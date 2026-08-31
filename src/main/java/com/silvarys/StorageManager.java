package com.silvarys;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import org.bukkit.inventory.ItemStack;

public class StorageManager {

    private static final int MAX_BACKUPS = 3;

    public static void saveData(JavaPlugin plugin) {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        saveGlobalData(plugin);

        for (String town : Main.townLevel.keySet()) {
            saveTownData(plugin, town);
        }
    }

    // used for backup snapshots - everything in one file so it's easy to restore
    public static void saveDataToSingleFile(JavaPlugin plugin, File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("players", null);
        config.set("towns", null);
        config.set("lockedBlocks", null);
        config.set("pendingRenames", null);
        config.set("subdivisions", null);
        config.set("plots", null);
        writePlayersToConfig(config);
        for (String town : Main.townLevel.keySet()) {
            writeTownToConfig(config, "towns." + town, town);
        }
        writeGlobalExtrasToConfig(config);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveGlobalData(JavaPlugin plugin) {
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) dataDir.mkdirs();

        File file = new File(dataDir, "global.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("players", null);
        config.set("lockedBlocks", null);
        config.set("pendingRenames", null);
        config.set("subdivisions", null);
        config.set("plots", null);
        config.set("occupation", null);
        config.set("ruinedCores", null);

        writePlayersToConfig(config);
        writeGlobalExtrasToConfig(config);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveTownData(JavaPlugin plugin, String town) {
        File townDir = getTownFolder(plugin, town);
        if (!townDir.exists()) townDir.mkdirs();

        File file = new File(townDir, "town.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("town", null);
        writeTownToConfig(config, "town", town);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writePlayersToConfig(FileConfiguration config) {
        for (UUID uuid : Main.playerTown.keySet()) {
            String path = "players." + uuid;
            config.set(path + ".town", Main.playerTown.get(uuid));
            config.set(path + ".role", Main.playerRole.getOrDefault(uuid, "member"));
            config.set(path + ".title", Main.playerTownTitle.getOrDefault(uuid, ""));
            config.set(path + ".lastOnline", Main.playerLastOnline.getOrDefault(uuid, 0L));
        }
    }

    private static void writeTownToConfig(FileConfiguration config, String path, String town) {
        config.set(path + ".level", Main.townLevel.getOrDefault(town, 1));
        config.set(path + ".bank", Main.townBank.getOrDefault(town, 0.0));
        config.set(path + ".renameUsed", Main.townRenameUsed.getOrDefault(town, false));
        config.set(path + ".motd", Main.townMotd.getOrDefault(town, "Welcome to " + town + "!"));
        config.set(path + ".mobsEnabled", Main.townMobsEnabled.getOrDefault(town, true));
        config.set(path + ".publicJoin", Main.townPublicJoin.getOrDefault(town, false));
        config.set(path + ".foundingDate", Main.townFoundingDate.getOrDefault(town, "Unknown"));
        config.set(path + ".chatFormat", Main.townChatFormat.getOrDefault(town, "default"));
        config.set(path + ".auditLogs", new ArrayList<>(Main.townAuditLogs.getOrDefault(town, new ArrayList<>())));
        config.set(path + ".upgradeTokens", Main.townUpgradeTokens.getOrDefault(town, 0));
        config.set(path + ".upgrades", new ArrayList<>(Main.townUpgrades.getOrDefault(town, new HashSet<>())));

        UUID owner = Main.townOwner.get(town);
        UUID assistant = Main.townAssistant.get(town);
        config.set(path + ".owner", owner != null ? owner.toString() : null);
        config.set(path + ".assistant", assistant != null ? assistant.toString() : null);

        config.set(path + ".chunks", new ArrayList<>(Main.townChunks.getOrDefault(town, new HashSet<>())));
        config.set(path + ".allies", new ArrayList<>(Main.townAllies.getOrDefault(town, new HashSet<>())));
        config.set(path + ".wars", new ArrayList<>(Main.townWars.getOrDefault(town, new HashSet<>())));
        config.set(path + ".enemies", new ArrayList<>(Main.townEnemies.getOrDefault(town, new HashSet<>())));
        config.set(path + ".lockedBlocks", new ArrayList<>(Main.lockedBlocks.getOrDefault(town, new HashSet<>())));

        List<String> members = new ArrayList<>();
        for (UUID uuid : Main.townMembers.getOrDefault(town, new HashSet<>())) {
            members.add(uuid.toString());
        }
        config.set(path + ".members", members);

        Location spawn = Main.townSpawn.get(town);
        if (spawn != null && spawn.getWorld() != null) {
            config.set(path + ".spawn.world", spawn.getWorld().getName());
            config.set(path + ".spawn.x", spawn.getX());
            config.set(path + ".spawn.y", spawn.getY());
            config.set(path + ".spawn.z", spawn.getZ());
            config.set(path + ".spawn.yaw", spawn.getYaw());
            config.set(path + ".spawn.pitch", spawn.getPitch());
        }

        Location core = Main.townCoreLocation.get(town);
        if (core != null && core.getWorld() != null) {
            config.set(path + ".core.world", core.getWorld().getName());
            config.set(path + ".core.x", core.getX());
            config.set(path + ".core.y", core.getY());
            config.set(path + ".core.z", core.getZ());
        }

        Long cooldown = Main.townSpawnCooldown.get(town);
        if (cooldown != null) config.set(path + ".spawnCooldown", cooldown);

        Long lastIncome = Main.townLastIncomeTime.get(town);
        if (lastIncome != null) config.set(path + ".lastIncomeTime", lastIncome);

        Map<String, List<org.bukkit.inventory.ItemStack>> incomeMap = Main.townIncome.get(town);
        if (incomeMap != null) {
            for (Map.Entry<String, List<org.bukkit.inventory.ItemStack>> catEntry : incomeMap.entrySet()) {
                config.set(path + ".income." + catEntry.getKey(), catEntry.getValue());
            }
        }

        Map<String, Integer> taskXP = TownLevelManager.townTaskXP.get(town);
        Map<String, Integer> taskLevel = TownLevelManager.townTaskLevel.get(town);
        if (taskXP != null) {
            for (String task : TownLevelManager.TASKS) {
                config.set(path + ".tasks." + task + ".xp", taskXP.getOrDefault(task, 0));
            }
        }
        if (taskLevel != null) {
            for (String task : TownLevelManager.TASKS) {
                config.set(path + ".tasks." + task + ".level", taskLevel.getOrDefault(task, 1));
            }
        }

        config.set(path + ".tagColor", Main.townTagColor.getOrDefault(town, "§6"));
        config.set(path + ".shieldExpiry", Main.townShieldExpiry.getOrDefault(town, 0L));
        config.set(path + ".coreHealth", WarManager.getCoreHealthPercent(town));

        Location originalCore = WarManager.getOriginalCoreLocation(town);
        if (originalCore != null && originalCore.getWorld() != null) {
            config.set(path + ".originalCore.world", originalCore.getWorld().getName());
            config.set(path + ".originalCore.x", originalCore.getX());
            config.set(path + ".originalCore.y", originalCore.getY());
            config.set(path + ".originalCore.z", originalCore.getZ());
        }
    }

    private static void writeGlobalExtrasToConfig(FileConfiguration config) {
        for (Map.Entry<String, String> entry : WarManager.getOccupiedTowns().entrySet()) {
            config.set("occupation." + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : Main.pendingRenames.entrySet()) {
            config.set("pendingRenames." + entry.getKey() + ".newName", entry.getValue());
            config.set("pendingRenames." + entry.getKey() + ".requester", Main.pendingRenameRequester.getOrDefault(entry.getKey(), "Unknown"));
        }
        List<String> lockedEntries = LockManager.lockedBlockOwners.entrySet().stream()
                .map(e -> e.getKey() + "==" + e.getValue()).collect(Collectors.toList());
        config.set("lockedBlocks", lockedEntries);
        config.set("subdivisions", Main.subdividedChunks);
        config.set("ruinedCores", new ArrayList<>(Main.ruinedCores.entrySet().stream()
                .map(e -> e.getKey() + "==" + e.getValue()).collect(Collectors.toList())));
        Map<String, Map<String, Object>> plotsMap = new HashMap<>();
        for (Map.Entry<String, Main.Plot> entry : Main.townPlots.entrySet()) {
            Map<String, Object> plotData = new HashMap<>();
            plotData.put("owner", entry.getValue().owner != null ? entry.getValue().owner.toString() : null);
            plotData.put("price", entry.getValue().price);
            plotsMap.put(entry.getKey(), plotData);
        }
        config.set("plots", plotsMap);
    }

    private static File getTownFolder(JavaPlugin plugin, String townName) {
        return new File(new File(plugin.getDataFolder(), "data" + File.separator + "towns"), townName);
    }

    public static void loadData(JavaPlugin plugin) {
        // Check for legacy towns.yml and migrate if needed
        File legacyFile = new File(plugin.getDataFolder(), "towns.yml");
        File globalFile = new File(new File(plugin.getDataFolder(), "data"), "global.yml");

        if (legacyFile.exists() && !globalFile.exists()) {
            plugin.getLogger().info("Migrating legacy towns.yml to per-town folder structure...");
            loadFromLegacyFile(plugin, legacyFile);
            saveData(plugin); // Save in new format
            legacyFile.renameTo(new File(plugin.getDataFolder(), "towns.yml.migrated"));
            plugin.getLogger().info("Migration complete. Old file renamed to towns.yml.migrated");
            return;
        }

        clearAllData();

        // Load global data
        if (globalFile.exists()) {
            loadGlobalData(plugin, globalFile);
        }

        // Load each town from its folder
        File townsDir = new File(plugin.getDataFolder(), "data" + File.separator + "towns");
        if (townsDir.exists() && townsDir.isDirectory()) {
            File[] townDirs = townsDir.listFiles(File::isDirectory);
            if (townDirs != null) {
                for (File townDir : townDirs) {
                    File townFile = new File(townDir, "town.yml");
                    if (townFile.exists()) {
                        loadTownFromFile(plugin, townDir.getName(), townFile);
                    }
                }
            }
        }

        plugin.getLogger().info("Town data loaded from per-town folders.");
    }

    /** Load from legacy single-file format (used for migration and backup restore). */
    public static void loadFromLegacyFile(JavaPlugin plugin, File file) {
        clearAllData();
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        loadPlayersFromConfig(plugin, config);
        loadTownsFromConfig(plugin, config, "towns");
        loadGlobalExtrasFromConfig(config);
        plugin.getLogger().info("Town data loaded from " + file.getName() + ".");
    }

    private static void clearAllData() {
        Main.playerTown.clear(); Main.playerRole.clear();
        Main.playerTownTitle.clear(); Main.playerLastOnline.clear();
        Main.townChunks.clear(); Main.townLevel.clear();
        Main.townBank.clear(); Main.townOwner.clear();
        Main.townAssistant.clear(); Main.townMembers.clear();
        Main.townAllies.clear(); Main.townWars.clear();
        Main.townEnemies.clear(); Main.townSpawn.clear();
        Main.townCoreLocation.clear(); Main.townSpawnCooldown.clear();
        Main.townRenameUsed.clear(); Main.pendingRenames.clear();
        Main.pendingRenameRequester.clear(); Main.townMotd.clear();
        Main.lockedBlocks.clear(); Main.townMobsEnabled.clear();
        Main.townPublicJoin.clear(); Main.townFoundingDate.clear();
        Main.townUpgradeTokens.clear(); Main.townUpgrades.clear();
        Main.subdividedChunks.clear(); Main.ruinedCores.clear();
        Main.townPlots.clear(); Main.townChatFormat.clear();
        Main.townAuditLogs.clear(); Main.townIncome.clear();
        Main.townLastIncomeTime.clear();
        LockManager.lockedBlockOwners.clear();
        TownLevelManager.townTaskXP.clear();
        TownLevelManager.townTaskLevel.clear();
    }

    private static void loadGlobalData(JavaPlugin plugin, File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        loadPlayersFromConfig(plugin, config);
        loadGlobalExtrasFromConfig(config);
    }

    private static void loadPlayersFromConfig(JavaPlugin plugin, FileConfiguration config) {
        if (config.contains("players") && config.getConfigurationSection("players") != null) {
            for (String uuidString : config.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    String path = "players." + uuidString;
                    String town = config.getString(path + ".town");
                    if (town != null) Main.playerTown.put(uuid, town);
                    Main.playerRole.put(uuid, config.getString(path + ".role", "member"));
                    Main.playerTownTitle.put(uuid, config.getString(path + ".title", ""));
                    Main.playerLastOnline.put(uuid, config.getLong(path + ".lastOnline", 0L));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    private static void loadTownFromFile(JavaPlugin plugin, String town, File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        loadSingleTown(plugin, town, config, "town");
    }

    private static void loadTownsFromConfig(JavaPlugin plugin, FileConfiguration config, String section) {
        if (config.contains(section) && config.getConfigurationSection(section) != null) {
            for (String town : config.getConfigurationSection(section).getKeys(false)) {
                loadSingleTown(plugin, town, config, section + "." + town);
            }
        }
    }

    private static void loadSingleTown(JavaPlugin plugin, String town, FileConfiguration config, String path) {
        Main.townLevel.put(town, config.getInt(path + ".level", 1));
        Main.townBank.put(town, config.getDouble(path + ".bank", 0.0));
        Main.townRenameUsed.put(town, config.getBoolean(path + ".renameUsed", false));
        Main.townMotd.put(town, config.getString(path + ".motd", "Welcome to " + town + "!"));
        Main.townTagColor.put(town, config.getString(path + ".tagColor", "§6"));
        Main.townShieldExpiry.put(town, config.getLong(path + ".shieldExpiry", 0L));
        Main.townMobsEnabled.put(town, config.getBoolean(path + ".mobsEnabled", true));
        Main.townPublicJoin.put(town, config.getBoolean(path + ".publicJoin", false));
        Main.townFoundingDate.put(town, config.getString(path + ".foundingDate", "Unknown"));
        Main.townChatFormat.put(town, config.getString(path + ".chatFormat", "default"));
        Main.townAuditLogs.put(town, new ArrayList<>(config.getStringList(path + ".auditLogs")));
        Main.townUpgradeTokens.put(town, config.getInt(path + ".upgradeTokens", 0));
        Main.townUpgrades.put(town, new HashSet<>(config.getStringList(path + ".upgrades")));

        try {
            String owner = config.getString(path + ".owner");
            String assistant = config.getString(path + ".assistant");
            if (owner != null) Main.townOwner.put(town, UUID.fromString(owner));
            if (assistant != null) Main.townAssistant.put(town, UUID.fromString(assistant));
        } catch (IllegalArgumentException ignored) {}

        Main.townChunks.put(town, new HashSet<>(config.getStringList(path + ".chunks")));
        Main.townAllies.put(town, new HashSet<>(config.getStringList(path + ".allies")));
        Main.townWars.put(town, new HashSet<>(config.getStringList(path + ".wars")));
        Main.townEnemies.put(town, new HashSet<>(config.getStringList(path + ".enemies")));
        Main.lockedBlocks.put(town, new HashSet<>(config.getStringList(path + ".lockedBlocks")));

        Set<UUID> members = new HashSet<>();
        for (String uuidString : config.getStringList(path + ".members")) {
            try { members.add(UUID.fromString(uuidString)); } catch (IllegalArgumentException ignored) {}
        }
        Main.townMembers.put(town, members);
        for (UUID memberUUID : members) {
            Main.playerTown.putIfAbsent(memberUUID, town);
            Main.playerRole.putIfAbsent(memberUUID, "member");
            Main.playerTownTitle.putIfAbsent(memberUUID, "");
            Main.playerLastOnline.putIfAbsent(memberUUID, 0L);
        }

        if (config.contains(path + ".spawn.world")) {
            String world = config.getString(path + ".spawn.world");
            if (world != null && plugin.getServer().getWorld(world) != null) {
                Main.townSpawn.put(town, new Location(plugin.getServer().getWorld(world),
                        config.getDouble(path + ".spawn.x"), config.getDouble(path + ".spawn.y"),
                        config.getDouble(path + ".spawn.z"), (float) config.getDouble(path + ".spawn.yaw"),
                        (float) config.getDouble(path + ".spawn.pitch")));
            }
        }

        if (config.contains(path + ".core.world")) {
            String world = config.getString(path + ".core.world");
            if (world != null && plugin.getServer().getWorld(world) != null) {
                Main.townCoreLocation.put(town, new Location(plugin.getServer().getWorld(world),
                        config.getDouble(path + ".core.x"), config.getDouble(path + ".core.y"),
                        config.getDouble(path + ".core.z")));
            }
        }

        if (config.contains(path + ".spawnCooldown")) Main.townSpawnCooldown.put(town, config.getLong(path + ".spawnCooldown"));
        if (config.contains(path + ".lastIncomeTime")) Main.townLastIncomeTime.put(town, config.getLong(path + ".lastIncomeTime"));

        if (config.contains(path + ".income") && config.getConfigurationSection(path + ".income") != null) {
            Map<String, List<org.bukkit.inventory.ItemStack>> incomeMap = new HashMap<>();
            for (String category : config.getConfigurationSection(path + ".income").getKeys(false)) {
                List<?> rawList = config.getList(path + ".income." + category);
                if (rawList != null) {
                    List<org.bukkit.inventory.ItemStack> items = new ArrayList<>();
                    for (Object obj : rawList) {
                        if (obj instanceof org.bukkit.inventory.ItemStack) items.add((org.bukkit.inventory.ItemStack) obj);
                    }
                    incomeMap.put(category, items);
                }
            }
            Main.townIncome.put(town, incomeMap);
        }

        Map<String, Integer> taskXP = new HashMap<>();
        Map<String, Integer> taskLevel = new HashMap<>();
        for (String task : TownLevelManager.TASKS) {
            taskXP.put(task, config.getInt(path + ".tasks." + task + ".xp", 0));
            taskLevel.put(task, config.getInt(path + ".tasks." + task + ".level", 1));
        }
        TownLevelManager.townTaskXP.put(town, taskXP);
        TownLevelManager.townTaskLevel.put(town, taskLevel);

        if (config.contains(path + ".coreHealth")) WarManager.setCoreHealthPercent(town, config.getInt(path + ".coreHealth", 100));
        if (config.contains(path + ".originalCore.world")) {
            String world = config.getString(path + ".originalCore.world");
            if (world != null && plugin.getServer().getWorld(world) != null) {
                WarManager.getOriginalCoreLocations().put(town, new Location(plugin.getServer().getWorld(world),
                        config.getDouble(path + ".originalCore.x"), config.getDouble(path + ".originalCore.y"),
                        config.getDouble(path + ".originalCore.z")));
            }
        }
    }

    private static void loadGlobalExtrasFromConfig(FileConfiguration config) {
        if (config.contains("occupation") && config.getConfigurationSection("occupation") != null) {
            Map<String, String> occ = new HashMap<>();
            for (String def : config.getConfigurationSection("occupation").getKeys(false)) {
                String atk = config.getString("occupation." + def);
                if (atk != null) occ.put(def, atk);
            }
            WarManager.setOccupiedTowns(occ);
        }
        if (config.contains("pendingRenames") && config.getConfigurationSection("pendingRenames") != null) {
            for (String oldName : config.getConfigurationSection("pendingRenames").getKeys(false)) {
                String newName = config.getString("pendingRenames." + oldName + ".newName");
                String requester = config.getString("pendingRenames." + oldName + ".requester", "Unknown");
                if (newName != null && !newName.isEmpty()) {
                    Main.pendingRenames.put(oldName, newName);
                    Main.pendingRenameRequester.put(oldName, requester);
                }
            }
        }
        if (config.contains("lockedBlocks")) {
            for (String entry : config.getStringList("lockedBlocks")) {
                String[] parts = entry.split("==", 2);
                if (parts.length == 2) LockManager.lockedBlockOwners.put(parts[0], parts[1]);
            }
        }
        if (config.contains("subdivisions") && config.getConfigurationSection("subdivisions") != null) {
            for (String key : config.getConfigurationSection("subdivisions").getKeys(false)) {
                Main.subdividedChunks.put(key, config.getInt("subdivisions." + key));
            }
        }
        if (config.contains("plots") && config.getConfigurationSection("plots") != null) {
            for (String key : config.getConfigurationSection("plots").getKeys(false)) {
                String ownerStr = config.getString("plots." + key + ".owner");
                UUID owner = (ownerStr != null && !ownerStr.equals("null")) ? UUID.fromString(ownerStr) : null;
                Main.townPlots.put(key, new Main.Plot(owner, config.getDouble("plots." + key + ".price")));
            }
        }
        if (config.contains("ruinedCores")) {
            for (String entry : config.getStringList("ruinedCores")) {
                String[] parts = entry.split("==", 2);
                if (parts.length == 2) Main.ruinedCores.put(parts[0], parts[1]);
            }
        }
    }
    public static String createBackup(JavaPlugin plugin) {
        return createBackup(plugin, "global");
    }

    public static String createAutoBackup(JavaPlugin plugin) {
        File backupFolder = new File(new File(plugin.getDataFolder(), "backups"), "automatic");
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }

        saveData(plugin);

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String backupName = "auto-backup-" + timestamp + ".yml";

        // Create a combined backup file
        File backupFile = new File(backupFolder, backupName);
        saveDataToSingleFile(plugin, backupFile);
        plugin.getLogger().info("Created automatic 12-hour backup: " + backupName);

        // Keep only the newest auto backup (limit = 1)
        cleanupOldAutoBackups(plugin, 1);

        return backupName;
    }

    public static String createBackup(JavaPlugin plugin, String townName) {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        File backupFolder = new File(plugin.getDataFolder(), "backups");
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }

        File townBackupFolder = new File(backupFolder, townName.equalsIgnoreCase("global") ? "global" : "towns");
        if (!townBackupFolder.exists()) {
            townBackupFolder.mkdirs();
        }

        saveData(plugin);

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String backupName = (townName.equalsIgnoreCase("global") ? "towns-backup-" : townName + "-") + timestamp + ".yml";

        File backupFile = new File(townBackupFolder, backupName);
        saveDataToSingleFile(plugin, backupFile);
        plugin.getLogger().info("Created " + townName + " backup: " + backupName);

        cleanupOldBackups(plugin, townName, MAX_BACKUPS);

        return backupName;
    }

    public static void deleteTownBackups(JavaPlugin plugin, String townName) {
        // Delete backup files
        File townBackupFolder = new File(new File(plugin.getDataFolder(), "backups"), "towns");
        if (townBackupFolder.exists()) {
            File[] files = townBackupFolder.listFiles((dir, name) -> name.startsWith(townName + "-") && name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    if (file.delete()) {
                        plugin.getLogger().info("Deleted backup for fallen town " + townName + ": " + file.getName());
                    }
                }
            }
        }

        // Delete the town's data folder
        deleteTownFolder(plugin, townName);
    }

    /** Deletes the entire data folder for a town. Called when a town is disbanded or falls. */
    public static void deleteTownFolder(JavaPlugin plugin, String townName) {
        File townDir = getTownFolder(plugin, townName);
        if (townDir.exists()) {
            deleteDirectoryRecursive(townDir);
            plugin.getLogger().info("Deleted data folder for town: " + townName);
        }
    }

    /** Renames a town's data folder. Called when a town is renamed. */
    public static void renameTownFolder(JavaPlugin plugin, String oldName, String newName) {
        File oldDir = getTownFolder(plugin, oldName);
        File newDir = getTownFolder(plugin, newName);
        if (oldDir.exists()) {
            if (oldDir.renameTo(newDir)) {
                plugin.getLogger().info("Renamed town folder: " + oldName + " -> " + newName);
            } else {
                plugin.getLogger().warning("Failed to rename town folder: " + oldName + " -> " + newName);
            }
        }
    }

    private static void deleteDirectoryRecursive(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectoryRecursive(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }


    public static List<String> listBackups(JavaPlugin plugin) {
        return listBackups(plugin, "global");
    }

    public static List<String> listBackups(JavaPlugin plugin, String townName) {
        String folderName;
        if (townName.equalsIgnoreCase("global")) folderName = "global";
        else if (townName.equalsIgnoreCase("automatic")) folderName = "automatic";
        else folderName = "towns";
        
        File backupFolder = new File(new File(plugin.getDataFolder(), "backups"), folderName);

        if (!backupFolder.exists()) {
            return new ArrayList<>();
        }

        File[] files = backupFolder.listFiles((dir, name) -> 
            (townName.equalsIgnoreCase("global") ? name.startsWith("towns-backup-") : 
             townName.equalsIgnoreCase("automatic") ? name.startsWith("auto-backup-") :
             name.startsWith(townName + "-")) && name.endsWith(".yml")
        );

        if (files == null) {
            return new ArrayList<>();
        }

        List<String> backups = new ArrayList<>();
        for (File file : files) {
            backups.add(file.getName());
        }

        backups.sort(Collections.reverseOrder());
        return backups;
    }

    public static boolean backupExists(JavaPlugin plugin, String backupName) {
        if (backupName == null) return false;

        if (backupName.contains("/") || backupName.contains("\\") || backupName.contains("..")) {
            return false;
        }

        String folderName;
        if (backupName.startsWith("towns-backup-")) folderName = "global";
        else if (backupName.startsWith("auto-backup-")) folderName = "automatic";
        else folderName = "towns";
        File backupFolder = new File(new File(plugin.getDataFolder(), "backups"), folderName);
        File backupFile = new File(backupFolder, backupName);

        return backupFile.exists() && backupFile.isFile();
    }

    public static boolean restoreBackup(JavaPlugin plugin, String backupName) {
        if (backupName == null) return false;

        if (backupName.contains("/") || backupName.contains("\\") || backupName.contains("..")) {
            return false;
        }

        String folderName;
        if (backupName.startsWith("towns-backup-")) folderName = "global";
        else if (backupName.startsWith("auto-backup-")) folderName = "automatic";
        else folderName = "towns";
        File backupFolder = new File(new File(plugin.getDataFolder(), "backups"), folderName);
        File backupFile = new File(backupFolder, backupName);

        if (!backupFile.exists() || !backupFile.isFile()) {
            return false;
        }

        String emergencyName = createBackup(plugin, "global");

        if (emergencyName == null) {
            plugin.getLogger().warning("Restore cancelled because emergency backup failed.");
            return false;
        }

        // Load from single-file backup format
        loadFromLegacyFile(plugin, backupFile);
        // Re-save into per-town folder structure
        saveData(plugin);
        plugin.getLogger().info("Restored town backup: " + backupName);
        return true;
    }

    private static void cleanupOldBackups(JavaPlugin plugin, String townName, int maxBackups) {
        File backupFolder = new File(new File(plugin.getDataFolder(), "backups"), townName.equalsIgnoreCase("global") ? "global" : "towns");

        if (!backupFolder.exists()) {
            return;
        }

        File[] files = backupFolder.listFiles((dir, name) -> 
            (townName.equalsIgnoreCase("global") ? name.startsWith("towns-backup-") : name.startsWith(townName + "-")) && name.endsWith(".yml")
        );

        if (files == null || files.length <= maxBackups) {
            return;
        }

        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

        for (int i = maxBackups; i < files.length; i++) {
            File oldBackup = files[i];

            if (oldBackup.delete()) {
                plugin.getLogger().info("Deleted old " + townName + " backup: " + oldBackup.getName());
            }
        }
    }

    private static void cleanupOldAutoBackups(JavaPlugin plugin, int maxBackups) {
        File backupFolder = new File(new File(plugin.getDataFolder(), "backups"), "automatic");
        if (!backupFolder.exists()) return;

        File[] files = backupFolder.listFiles((dir, name) -> name.startsWith("auto-backup-") && name.endsWith(".yml"));
        if (files == null || files.length <= maxBackups) return;

        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

        for (int i = maxBackups; i < files.length; i++) {
            if (files[i].delete()) {
                plugin.getLogger().info("Deleted old automatic backup: " + files[i].getName());
            }
        }
    }
}