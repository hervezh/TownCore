package com.silvarys;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main extends JavaPlugin implements Listener {

    public static Economy economy = null;

    public static Map<UUID, String> playerTown = new HashMap<>();
    public static Map<UUID, String> playerRole = new HashMap<>();

    public static Map<String, Set<String>> townChunks = new HashMap<>();
    public static Map<String, Integer> townLevel = new HashMap<>();
    public static Map<String, Double> townBank = new HashMap<>();
    public static Map<String, UUID> townOwner = new HashMap<>();
    public static Map<String, UUID> townAssistant = new HashMap<>();
    public static Map<String, Set<UUID>> townMembers = new HashMap<>();
    public static Map<String, Set<String>> townAllies = new HashMap<>();
    public static Map<String, Set<String>> townWars = new HashMap<>();
    public static Map<String, Set<String>> townEnemies = new HashMap<>();
    public static Map<String, Location> townSpawn = new HashMap<>();
    public static Map<String, Location> townCoreLocation = new HashMap<>();
    public static Map<String, PendingTown> pendingTowns = new HashMap<>();
    public static Map<String, Long> townSpawnCooldown = new HashMap<>();
    public static Map<String, Boolean> townRenameUsed = new HashMap<>();
    public static Map<String, String> pendingRenames = new HashMap<>();
    public static Map<String, String> pendingRenameRequester = new HashMap<>();
    public static Map<String, String> townTagColor = new HashMap<>();
    public static Map<String, Long> townShieldExpiry = new HashMap<>();
    public static Map<String, String> townMotd = new HashMap<>();
    public static Map<String, Set<String>> lockedBlocks = new HashMap<>();
    public static Map<String, String> ruinedCores = new HashMap<>(); // "world:x:y:z" -> "TownName:Date"
    public static Map<String, Long> lastUpkeepWarning = new HashMap<>();

    public static Map<String, Set<String>> pendingAllyRequests = new HashMap<>();

    public static Map<String, Boolean> townMobsEnabled = new HashMap<>();
    public static Map<String, Boolean> townPublicJoin = new HashMap<>();
    public static Map<String, String> townFoundingDate = new HashMap<>();
    public static Map<String, Integer> townUpgradeTokens = new HashMap<>();
    public static Map<String, Set<String>> townUpgrades = new HashMap<>();

    public static class Plot {
        public UUID owner;
        public double price;

        public Plot(UUID owner, double price) {
            this.owner = owner;
            this.price = price;
        }
    }

    public static Map<String, Integer> subdividedChunks = new HashMap<>(); // chunkKey -> type (4 or 16)
    public static Map<String, Plot> townPlots = new HashMap<>(); // "chunkKey:index" -> Plot

    public static Map<String, String> townChatFormat = new HashMap<>();
    public static Map<String, List<String>> townAuditLogs = new HashMap<>();
    public static Map<String, Map<String, List<ItemStack>>> townIncome = new HashMap<>();
    public static Map<String, Long> townLastIncomeTime = new HashMap<>();

    public static Map<UUID, String> playerTownTitle = new HashMap<>();
    public static Map<UUID, Long> playerLastOnline = new HashMap<>();

    public static NamespacedKey TOWN_CORE_KEY;
    public static NamespacedKey RUINED_CORE_KEY;

    public static final double DAILY_UPKEEP_COST = 250.0;

    private static final int MIN_DISTANCE_FROM_TOWN_CLAIMS_CHUNKS = 5;
    private static final int MAX_AUDIT_LOG_ENTRIES = 50;

    private static final DateTimeFormatter AUDIT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private BukkitTask autoSaveTask;

    public static class PendingTown {
        public UUID owner;
        public String townName;
        public Location coreLocation;
        public BukkitTask task;
        public BukkitTask countdownTask;
        public TextDisplay timerDisplay;

        public PendingTown(UUID owner, String townName, Location coreLocation, BukkitTask task, BukkitTask countdownTask, TextDisplay timerDisplay) {
            this.owner = owner;
            this.townName = townName;
            this.coreLocation = coreLocation;
            this.task = task;
            this.countdownTask = countdownTask;
            this.timerDisplay = timerDisplay;
        }
    }

    public static NamespacedKey GUIDE_BOOK_KEY;

    @Override
    public void onEnable() {
        TOWN_CORE_KEY = new NamespacedKey(this, "town_core");
        RUINED_CORE_KEY = new NamespacedKey(this, "ruined_core");
        GUIDE_BOOK_KEY = new NamespacedKey(this, "guide_book");

        // register all the listeners
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new WarListener(), this);
        getServer().getPluginManager().registerEvents(new TownLevelListener(), this);
        getServer().getPluginManager().registerEvents(new TownRollbackListener(), this);
        getServer().getPluginManager().registerEvents(new ChunkEnterListener(), this);
        getServer().getPluginManager().registerEvents(new ClaimProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new ChatListener(), this);
        getServer().getPluginManager().registerEvents(new LoginListener(this), this);
        getServer().getPluginManager().registerEvents(new TownGUIListener(), this);
        getServer().getPluginManager().registerEvents(new TownBankGUIListener(), this);
        getServer().getPluginManager().registerEvents(new TownSettingsGUIListener(), this);
        getServer().getPluginManager().registerEvents(new TownIncomeGUIListener(), this);
        getServer().getPluginManager().registerEvents(new TownUpgradesManager(), this);
        getServer().getPluginManager().registerEvents(new TownUpgradesGUI(), this);
        getServer().getPluginManager().registerEvents(new TownListGUI(), this);
        getServer().getPluginManager().registerEvents(new TownTopGUI(), this);
        getServer().getPluginManager().registerEvents(new TownInfoGUI(), this);
        getServer().getPluginManager().registerEvents(new TownPlotListener(), this);

        if (getCommand("town") != null) {
            getCommand("town").setExecutor(new TownCommand());
        }

        if (!setupEconomy()) {
            getLogger().warning("Vault not found - economy stuff won't work.");
        } else {
            getLogger().info("Hooked into Vault economy.");
        }

        registerTownCoreRecipe();

        StorageManager.loadData(this);

        registerPlaceholderAPI();

        startAutoSaveTimer();
        startAutoBackupTimer();

        // start all the background tasks
        UpkeepManager.startUpkeepTimer(this);
        WarManager.startAutomaticWarScheduler(this);
        TownIncomeManager.startIncomeTimer(this);
        TownUpgradesManager.startPerkTask(this);
        RadarManager.startTask(this);
        WarManager.startWarTicker(this);

        getLogger().info("TownCore loaded.");
    }

    @Override
    public void onDisable() {
        // save last-online time for all currently logged in players
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerLastOnline.put(player.getUniqueId(), System.currentTimeMillis());
        }

        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }

        cleanupPendingTowns();

        WarBossBarManager.stopAllWarBossBars();
        WarManager.shutdownWarSystem();

        StorageManager.saveData(this);
        getLogger().info("TownCore disabled, data saved.");
    }

    private void registerPlaceholderAPI() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("PlaceholderAPI not found! TAB placeholders are disabled.");
            return;
        }

        new TownPlaceholderExpansion().register();
        getLogger().info("Registered Town Core PlaceholderAPI placeholders.");
    }

    private void startAutoSaveTimer() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }

        // every 5 minutes, save everything
        autoSaveTask = new BukkitRunnable() {
            @Override
            public void run() {
                StorageManager.saveData(Main.this);
                getLogger().info("[TownCore] auto-saved.");
            }
        }.runTaskTimer(this, 20L * 300L, 20L * 300L);
    }

    private void startAutoBackupTimer() {
        // backup twice a day just in case
        new BukkitRunnable() {
            @Override
            public void run() {
                StorageManager.createAutoBackup(Main.this);
            }
        }.runTaskTimer(this, 20L * 60L * 60L * 12L, 20L * 60L * 60L * 12L); // 12 hours
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;

        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) return false;

        economy = rsp.getProvider();
        return economy != null;
    }

    private void registerTownCoreRecipe() {
        ItemStack townCore = getTownCoreItem();

        ShapedRecipe recipe = new ShapedRecipe(TOWN_CORE_KEY, townCore);
        recipe.shape("GGG", "GDG", "OOO");
        recipe.setIngredient('G', Material.GOLD_BLOCK);
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('O', Material.OBSIDIAN);

        getServer().addRecipe(recipe);
    }

    public static ItemStack getTownCoreItem() {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        meta.displayName(Component.text("Town Core")
                .color(TextColor.color(0x5555FF))
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        meta.getPersistentDataContainer().set(TOWN_CORE_KEY, PersistentDataType.BYTE, (byte) 1);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        item.setItemMeta(meta);
        return item;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTownCorePlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        ItemStack item = event.getItemInHand();

        if (item.getType() != Material.BEACON) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        Byte isTownCore = meta.getPersistentDataContainer().get(TOWN_CORE_KEY, PersistentDataType.BYTE);
        if (isTownCore == null || isTownCore != 1) return;

        if (event.isCancelled() || !event.canBuild()) {
            player.sendMessage("§cYou cannot place a Town Core in a protected area!");
            event.setCancelled(true);
            return;
        }

        if (playerTown.containsKey(uuid)) {
            player.sendMessage("§cYou are already in a town!");
            event.setCancelled(true);
            return;
        }

        for (PendingTown pending : pendingTowns.values()) {
            if (pending.owner.equals(uuid)) {
                player.sendMessage("§cYou already have a town creation in progress!");
                event.setCancelled(true);
                return;
            }
        }

        Block block = event.getBlock();

        if (!isValidTownCorePlacement(player, block)) {
            event.setCancelled(true);
            return;
        }

        // Clean the chunk of player-placed blocks before placing the core
        cleanChunk(block.getChunk());

        block.setType(Material.BEACON);
        createTownCoreBeaconBase(block);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (block.getType() == Material.BEACON) {
                playTownCorePlaceEffect(block);
            }
        }, 1L);

        Location coreLocation = block.getLocation();
        String coreKey = getLocationKey(coreLocation);
        String townName = player.getName() + "'s Town";

        int[] secondsLeft = {300};
        TextDisplay timerDisplay = spawnTownCoreTimerDisplay(coreLocation, secondsLeft[0]);

        BukkitTask countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                Player owner = Bukkit.getPlayer(uuid);
                PendingTown pending = pendingTowns.get(coreKey);

                if (pending == null) {
                    cancel();
                    return;
                }

                if (secondsLeft[0] <= 0) {
                    removeTownCoreTimerDisplay(pending);
                    cancel();
                    return;
                }

                updateTownCoreTimerDisplay(pending.timerDisplay, secondsLeft[0]);

                // Visual Feedback Particles
                Location center = coreLocation.clone().add(0.5, 0.5, 0.5);
                World world = coreLocation.getWorld();
                if (world != null) {
                    if (secondsLeft[0] >= 210) { // 70% - 100% (300s to 210s)
                        world.spawnParticle(org.bukkit.Particle.DUST, center, 8, 0.4, 0.4, 0.4, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(0, 255, 0), 1.0f));
                        world.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, center, 2, 0.4, 0.4, 0.4, 0.02);
                    } else if (secondsLeft[0] >= 90) { // 30% - 70% (210s to 90s)
                        world.spawnParticle(org.bukkit.Particle.DUST, center, 10, 0.4, 0.4, 0.4, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(255, 255, 0), 1.0f));
                        world.spawnParticle(org.bukkit.Particle.CRIT, center, 4, 0.4, 0.4, 0.4, 0.05);
                    } else { // 0% - 30% (90s to 0s)
                        world.spawnParticle(org.bukkit.Particle.DUST, center, 12, 0.3, 0.3, 0.3, 0, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(255, 0, 0), 1.2f));
                        world.spawnParticle(org.bukkit.Particle.FLAME, center, 6, 0.3, 0.3, 0.3, 0.05);
                        world.spawnParticle(org.bukkit.Particle.SMOKE, center, 10, 0.3, 0.6, 0.3, 0.02);
                    }
                }

                if (secondsLeft[0] == 300 || secondsLeft[0] == 240
                        || secondsLeft[0] == 180 || secondsLeft[0] == 120
                        || secondsLeft[0] == 60 || secondsLeft[0] <= 30) {

                    String timeStr = secondsLeft[0] >= 60
                            ? (secondsLeft[0] / 60) + "m " + (secondsLeft[0] % 60) + "s"
                            : secondsLeft[0] + "s";

                    if (owner != null) {
                        owner.sendMessage("§e⏱ Town Core: §f" + timeStr + " §eremaining!");
                    }

                    if (secondsLeft[0] <= 10) {
                        Bukkit.broadcastMessage("§6§l" + townName
                                + " §ewill be founded in §f" + secondsLeft[0] + "s§e!");
                    }
                }

                secondsLeft[0]--;
            }
        }.runTaskTimer(this, 0L, 20L);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(this, () -> {
            PendingTown pending = pendingTowns.remove(coreKey);
            if (pending == null) return;

            if (pending.countdownTask != null) {
                pending.countdownTask.cancel();
            }

            removeTownCoreTimerDisplay(pending);

            Block coreBlock = pending.coreLocation.getBlock();

            if (coreBlock.getType() != Material.BEACON) {
                Player owner = Bukkit.getPlayer(pending.owner);

                if (owner != null) {
                    owner.sendMessage("§cYour Town Core was destroyed. Town creation cancelled.");
                }

                return;
            }

            if (playerTown.containsKey(pending.owner)) {
                Player owner = Bukkit.getPlayer(pending.owner);

                if (owner != null) {
                    owner.sendMessage("§cTown creation cancelled because you are already in a town.");
                }

                return;
            }

            createTown(pending.owner, pending.townName, pending.coreLocation);

        }, 20L * 300L);

        pendingTowns.put(coreKey, new PendingTown(uuid, townName, coreLocation, task, countdownTask, timerDisplay));

        player.sendMessage("§e§lTown Core placed!");
        player.sendMessage("§eDefend it for §f5 minutes§e to create your town.");
        player.sendMessage("§cIf it is destroyed, town creation is cancelled and the core is lost.");

        Bukkit.broadcastMessage("§6§l" + player.getName()
                + " §eis trying to found §e" + townName
                + "§6! Defend the core for 5 minutes!");
    }

    private TextDisplay spawnTownCoreTimerDisplay(Location coreLocation, int secondsLeft) {
        if (coreLocation == null || coreLocation.getWorld() == null) return null;

        Location displayLocation = coreLocation.clone().add(0.5, 2.15, 0.5);
        TextDisplay display = coreLocation.getWorld().spawn(displayLocation, TextDisplay.class);

        display.setBillboard(Display.Billboard.CENTER);
        display.setShadowed(true);
        display.setSeeThrough(false);
        display.setDefaultBackground(false);
        display.setBackgroundColor(Color.fromARGB(120, 0, 0, 0));
        display.setAlignment(TextDisplay.TextAlignment.CENTER);
        display.setLineWidth(180);
        display.setPersistent(false);
        display.setInvulnerable(true);
        display.setGravity(false);

        updateTownCoreTimerDisplay(display, secondsLeft);
        return display;
    }

    private void updateTownCoreTimerDisplay(TextDisplay display, int secondsLeft) {
        if (display == null || display.isDead()) return;

        int timerColor = secondsLeft <= 30 ? 0xFF5555 : secondsLeft <= 60 ? 0xFFAA00 : 0x55FFFF;

        display.text(Component.text("Town Core")
                .color(TextColor.color(0xFFD166))
                .decoration(TextDecoration.BOLD, true)
                .append(Component.newline())
                .append(Component.text(formatTownCoreTime(secondsLeft) + " left")
                        .color(TextColor.color(timerColor))
                        .decoration(TextDecoration.BOLD, false)));
    }

    private String formatTownCoreTime(int secondsLeft) {
        int minutes = secondsLeft / 60;
        int seconds = secondsLeft % 60;

        if (minutes <= 0) {
            return seconds + "s";
        }

        return minutes + "m " + seconds + "s";
    }

    private void cancelPendingTown(PendingTown pending) {
        if (pending == null) return;

        if (pending.task != null) pending.task.cancel();
        if (pending.countdownTask != null) pending.countdownTask.cancel();

        removeTownCoreTimerDisplay(pending);
    }

    private void removeTownCoreTimerDisplay(PendingTown pending) {
        if (pending == null || pending.timerDisplay == null) return;

        if (!pending.timerDisplay.isDead()) {
            pending.timerDisplay.remove();
        }

        pending.timerDisplay = null;
    }

    private void cleanupPendingTowns() {
        for (PendingTown pending : pendingTowns.values()) {
            cancelPendingTown(pending);
        }

        pendingTowns.clear();
    }

    private boolean isValidTownCorePlacement(Player player, Block block) {
        Location location = block.getLocation();

        if (block.isLiquid()
                || block.getType() == Material.WATER
                || block.getType() == Material.LAVA
                || block.getRelative(0, -1, 0).isLiquid()) {
            player.sendMessage("§cYou cannot place a Town Core in water or lava!");
            return false;
        }

        if (!hasClearSkyAbove(block)) {
            player.sendMessage("§cYour Town Core needs clear sky above it for the beacon to activate!");
            return false;
        }

        if (!hasEnoughBeaconBaseSpace(block)) {
            player.sendMessage("§cNot enough space for the Town Core beacon base!");
            player.sendMessage("§7Make sure the 3x3 area under the beacon is not blocked by liquids, bedrock, barriers, or another Town Core.");
            return false;
        }

        if (isInsideSpawnProtection(location)) {
            player.sendMessage("§cYou cannot place a Town Core inside spawn protection!");
            return false;
        }

        String claimedTown = getTownAtChunk(location.getChunk());
        if (claimedTown != null) {
            player.sendMessage("§cYou cannot place a Town Core inside another town's claimed land!");
            player.sendMessage("§7This chunk is claimed by §f" + claimedTown + "§7.");
            return false;
        }



        ClaimDistanceResult nearestClaim = getNearestTownClaimDistance(location.getChunk());
        if (nearestClaim != null && nearestClaim.distanceChunks < MIN_DISTANCE_FROM_TOWN_CLAIMS_CHUNKS) {
            player.sendMessage("§cYou are too close to another town's claimed land!");
            player.sendMessage("§7You must be at least §f" + MIN_DISTANCE_FROM_TOWN_CLAIMS_CHUNKS + " chunks §7away from town claims.");
            player.sendMessage("§7Nearest claim: §f" + nearestClaim.distanceChunks + " chunks §7away from §f" + nearestClaim.townName + "§7.");
            return false;
        }

        for (PendingTown pending : pendingTowns.values()) {
            Location pendingLocation = pending.coreLocation;
            if (pendingLocation == null || pendingLocation.getWorld() == null) continue;
            if (location.getWorld() == null) continue;
            if (!pendingLocation.getWorld().getName().equals(location.getWorld().getName())) continue;

            int pendingDistance = getChunkDistance(location.getChunk(), pendingLocation.getChunk());
            if (pendingDistance < MIN_DISTANCE_FROM_TOWN_CLAIMS_CHUNKS) {
                player.sendMessage("§cYou are too close to a pending town creation!");
                player.sendMessage("§7You must be at least §f" + MIN_DISTANCE_FROM_TOWN_CLAIMS_CHUNKS + " chunks §7away.");
                player.sendMessage("§7Pending core: §f" + pendingDistance + " chunks §7away.");
                return false;
            }
        }

        return true;
    }

    private void cleanChunk(Chunk chunk) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = (chunk.getX() << 4) + x;
                int worldZ = (chunk.getZ() << 4) + z;
                int highestY = chunk.getWorld().getHighestBlockYAt(worldX, worldZ);
                
                for (int y = highestY; y >= chunk.getWorld().getMinHeight(); y--) {
                    Block b = chunk.getBlock(x, y, z);
                    Material type = b.getType();
                    
                    if (type.isAir()) continue;
                    
                    // If it's not a natural block, break it
                    if (!isNaturalBlock(type)) {
                        b.setType(Material.AIR);
                    }
                }
            }
        }
    }

    private boolean isNaturalBlock(Material type) {
        String name = type.name();
        return name.contains("GRASS") || name.contains("DIRT") || name.contains("STONE") || 
               name.contains("DEEPSLATE") || name.contains("ORE") || name.contains("LOG") || 
               name.contains("LEAVES") || name.contains("SAPLING") || name.contains("FLOWER") ||
               name.contains("SAND") || name.contains("GRAVEL") || name.contains("SNOW") ||
               name.contains("ICE") || name.contains("WATER") || name.contains("LAVA") ||
               name.contains("MUSHROOM") || name.contains("VINE") || name.contains("MOSS") ||
               name.contains("LICHEN") || name.contains("BUSH") || name.contains("FERN") ||
               name.contains("BAMBOO") || name.contains("CAVE") || name.contains("AMETHYST") ||
               type == Material.BEDROCK || type == Material.TUFF || type == Material.CALCITE ||
               type == Material.OBSIDIAN || type == Material.MAGMA_BLOCK || type == Material.CLAY ||
               type == Material.DRIPSTONE_BLOCK || type == Material.POINTED_DRIPSTONE ||
               type == Material.COAL_BLOCK || type == Material.PUMPKIN || type == Material.MELON;
    }

    private boolean hasClearSkyAbove(Block block) {
        World world = block.getWorld();
        int x = block.getX();
        int z = block.getZ();

        for (int y = block.getY() + 1; y < world.getMaxHeight(); y++) {
            Block above = world.getBlockAt(x, y, z);

            if (!above.getType().isAir()) {
                return false;
            }
        }

        return true;
    }

    private boolean hasEnoughBeaconBaseSpace(Block beaconBlock) {
        Location loc = beaconBlock.getLocation();

        if (loc.getBlockY() <= beaconBlock.getWorld().getMinHeight()) {
            return false;
        }

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block baseBlock = loc.clone().add(x, -1, z).getBlock();

                if (!canReplaceWithBeaconBase(baseBlock)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean canReplaceWithBeaconBase(Block block) {
        Material type = block.getType();

        if (block.isLiquid()) return false;
        if (type == Material.BEDROCK) return false;
        if (type == Material.BARRIER) return false;
        if (type == Material.COMMAND_BLOCK) return false;
        if (type == Material.CHAIN_COMMAND_BLOCK) return false;
        if (type == Material.REPEATING_COMMAND_BLOCK) return false;
        if (type == Material.STRUCTURE_BLOCK) return false;
        if (type == Material.END_PORTAL_FRAME) return false;
        if (type == Material.END_PORTAL) return false;
        if (type == Material.NETHER_PORTAL) return false;
        if (type == Material.BEACON) return false;

        return true;
    }

    private boolean isInsideSpawnProtection(Location location) {
        World world = location.getWorld();

        if (world == null) return false;

        int spawnRadius = Bukkit.getSpawnRadius();

        if (spawnRadius <= 0) return false;

        Location spawn = world.getSpawnLocation();

        if (!spawn.getWorld().getName().equals(world.getName())) return false;

        return Math.abs(location.getBlockX() - spawn.getBlockX()) <= spawnRadius
                && Math.abs(location.getBlockZ() - spawn.getBlockZ()) <= spawnRadius;
    }

    public static String getTownAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return getTownAtChunk(loc.getChunk());
    }

    public static String getTownAtChunk(Chunk chunk) {
        String chunkKey = getChunkKey(chunk);

        for (Map.Entry<String, Set<String>> entry : townChunks.entrySet()) {
            if (entry.getValue().contains(chunkKey)) {
                return entry.getKey();
            }
        }

        return null;
    }

    private ClaimDistanceResult getNearestTownClaimDistance(Chunk targetChunk) {
        ClaimDistanceResult nearest = null;

        for (Map.Entry<String, Set<String>> entry : townChunks.entrySet()) {
            String townName = entry.getKey();

            for (String claimKey : entry.getValue()) {
                ParsedChunkKey parsed = parseChunkKey(claimKey);

                if (parsed == null) continue;
                if (!parsed.worldName.equals(targetChunk.getWorld().getName())) continue;

                int distance = getChunkDistance(
                        targetChunk.getX(),
                        targetChunk.getZ(),
                        parsed.chunkX,
                        parsed.chunkZ
                );

                if (nearest == null || distance < nearest.distanceChunks) {
                    nearest = new ClaimDistanceResult(townName, distance);
                }
            }
        }

        return nearest;
    }

    private int getChunkDistance(Chunk chunkA, Chunk chunkB) {
        return getChunkDistance(chunkA.getX(), chunkA.getZ(), chunkB.getX(), chunkB.getZ());
    }

    private int getChunkDistance(int chunkAX, int chunkAZ, int chunkBX, int chunkBZ) {
        int dx = Math.abs(chunkAX - chunkBX);
        int dz = Math.abs(chunkAZ - chunkBZ);

        return Math.max(dx, dz);
    }

    private ParsedChunkKey parseChunkKey(String chunkKey) {
        try {
            String[] worldSplit = chunkKey.split(":");
            if (worldSplit.length != 2) return null;

            String worldName = worldSplit[0];

            String[] coordSplit = worldSplit[1].split(",");
            if (coordSplit.length != 2) return null;

            int chunkX = Integer.parseInt(coordSplit[0]);
            int chunkZ = Integer.parseInt(coordSplit[1]);

            return new ParsedChunkKey(worldName, chunkX, chunkZ);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static class ParsedChunkKey {
        private final String worldName;
        private final int chunkX;
        private final int chunkZ;

        private ParsedChunkKey(String worldName, int chunkX, int chunkZ) {
            this.worldName = worldName;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }
    }

    private static class ClaimDistanceResult {
        private final String townName;
        private final int distanceChunks;

        private ClaimDistanceResult(String townName, int distanceChunks) {
            this.townName = townName;
            this.distanceChunks = distanceChunks;
        }
    }

    private void createTownCoreBeaconBase(Block beaconBlock) {
        Location loc = beaconBlock.getLocation();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block baseBlock = loc.clone().add(x, -1, z).getBlock();
                baseBlock.setType(Material.IRON_BLOCK);
            }
        }
    }

    private void playTownCorePlaceEffect(Block block) {
        Location lightningLocation = block.getLocation().add(0.5, 0, 0.5);
        Location particleLocation = block.getLocation().add(0.5, 1.2, 0.5);

        block.getWorld().strikeLightningEffect(lightningLocation);

        block.getWorld().spawnParticle(
                Particle.END_ROD,
                particleLocation,
                80,
                0.5,
                0.8,
                0.5,
                0.03
        );

        block.getWorld().spawnParticle(
                Particle.FLAME,
                particleLocation,
                80,
                0.5,
                0.8,
                0.5,
                0.03
        );

        block.getWorld().playSound(
                block.getLocation(),
                Sound.BLOCK_BEACON_ACTIVATE,
                1.0f,
                1.0f
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTownCoreBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (isTownCoreBaseBlock(block)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cThe Town Core base is protected and cannot be broken!");
            return;
        }

        if (block.getType() != Material.BEACON) return;

        String key = getLocationKey(block.getLocation());

        if (pendingTowns.containsKey(key)) {
            PendingTown pending = pendingTowns.remove(key);

            cancelPendingTown(pending);

            event.setDropItems(false);

            Player breaker = event.getPlayer();
            Player owner = Bukkit.getPlayer(pending.owner);

            breaker.sendMessage("§cYou destroyed the Town Core. Town creation cancelled!");

            if (owner != null && !owner.equals(breaker)) {
                owner.sendMessage("§cYour Town Core was destroyed! Town creation cancelled.");
            }

            Bukkit.broadcastMessage("§c§l⚠ A town failed to form! The Town Core was destroyed by §f"
                    + breaker.getName() + "§c!");
            return;
        }

        for (Map.Entry<String, Location> entry : townCoreLocation.entrySet()) {
            String townName = entry.getKey();
            Location coreLocation = entry.getValue();

            if (!sameBlock(coreLocation, block.getLocation())) continue;

            WarManager.War war = WarManager.getWarByDefenderTown(townName);

            if (war != null && war.activeSession) {
                event.setCancelled(true);
                event.setDropItems(false);
                event.getPlayer().sendMessage("§cHold left click on the Town Core to damage it!");
                return;
            }

            event.setCancelled(true);
            event.getPlayer().sendMessage("§cThis Town Core is protected and cannot be broken!");
            return;
        }
    }

    private boolean isTownCoreBaseBlock(Block block) {
        if (block == null || block.getType() != Material.IRON_BLOCK) {
            return false;
        }

        Location blockLocation = block.getLocation();

        for (PendingTown pending : pendingTowns.values()) {
            if (isBlockPartOfCoreBase(blockLocation, pending.coreLocation)) {
                return true;
            }
        }

        for (Location coreLocation : townCoreLocation.values()) {
            if (isBlockPartOfCoreBase(blockLocation, coreLocation)) {
                return true;
            }
        }

        return false;
    }

    private boolean isBlockPartOfCoreBase(Location blockLocation, Location coreLocation) {
        if (blockLocation == null || coreLocation == null) return false;
        if (blockLocation.getWorld() == null || coreLocation.getWorld() == null) return false;

        if (!blockLocation.getWorld().getName().equals(coreLocation.getWorld().getName())) {
            return false;
        }

        int dx = Math.abs(blockLocation.getBlockX() - coreLocation.getBlockX());
        int dz = Math.abs(blockLocation.getBlockZ() - coreLocation.getBlockZ());
        int dy = blockLocation.getBlockY() - coreLocation.getBlockY();

        return dx <= 1 && dz <= 1 && dy == -1;
    }

    public static void createTown(UUID uuid, String townName, Location coreLocation) {
        Player player = Bukkit.getPlayer(uuid);

        Chunk chunk = coreLocation.getChunk();
        String chunkKey = getChunkKey(chunk);

        playerTown.put(uuid, townName);
        playerRole.put(uuid, "ruler");
        playerTownTitle.put(uuid, "");
        playerLastOnline.put(uuid, System.currentTimeMillis());

        townOwner.put(townName, uuid);
        townLevel.put(townName, 1);
        townBank.put(townName, 0.0);
        townAssistant.put(townName, null);
        townAllies.put(townName, new HashSet<>());
        townWars.put(townName, new HashSet<>());
        townEnemies.put(townName, new HashSet<>());
        pendingAllyRequests.put(townName, new HashSet<>());
        townRenameUsed.put(townName, false);
        townMotd.put(townName, "Welcome to " + townName + "!");
        
        String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        townFoundingDate.put(townName, date);
        
        townPublicJoin.put(townName, false);
        townMobsEnabled.put(townName, true);
        townShieldExpiry.put(townName, 0L);

        townChatFormat.put(townName, "default");
        townAuditLogs.put(townName, new ArrayList<>());
        lockedBlocks.put(townName, new HashSet<>());

        Set<String> chunks = new HashSet<>();
        chunks.add(chunkKey);
        townChunks.put(townName, chunks);

        Set<UUID> members = new HashSet<>();
        members.add(uuid);
        townMembers.put(townName, members);

        townSpawn.put(townName, coreLocation);
        townCoreLocation.put(townName, coreLocation);

        TownLevelManager.initTown(townName);

        logTownAction(townName, "Town was founded by " + (player != null ? player.getName() : "Unknown") + ".");
        StorageManager.saveData(JavaPlugin.getProvidingPlugin(Main.class));

        if (player != null) {
            player.sendMessage("§a§lTown Created! §r§aWelcome to " + townName + "!");
            player.sendMessage("§aYou are now the Ruler of this town.");
            player.sendMessage("§aYour first chunk has been claimed.");
            player.sendMessage("§aYour Town Core is now protected and cannot be broken!");
        }

        Bukkit.broadcastMessage("§a§l" + townName + " §ahas been founded by §f"
                + (player != null ? player.getName() : "Unknown") + "§a!");
    }

    public static void onTownLevelUp(String townName, int newLevel) {
        for (UUID uuid : townMembers.getOrDefault(townName, new HashSet<>())) {
            Player player = Bukkit.getPlayer(uuid);

            if (player != null) {
                player.giveExp(100);
                player.sendMessage("§a§lTown Level Up! §r§f" + townName
                        + " §ais now level §f" + newLevel + "§a!");
                player.sendMessage("§aYou received §f100 XP §afor your town's progress!");
            }
        }

        logTownAction(townName, "Town reached level " + newLevel + ".");
        StorageManager.saveData(JavaPlugin.getProvidingPlugin(Main.class));

        Bukkit.broadcastMessage("§a§l⚑ " + townName
                + " §ahas reached level §f" + newLevel + "§a!");

        if (newLevel % 10 == 0) {
            Bukkit.broadcastMessage(" ");
            Bukkit.broadcastMessage("§6§l✦ TOWN MILESTONE ✦");
            Bukkit.broadcastMessage("§e§l" + townName + " §6has reached §f§lLevel " + newLevel + "§6!");
            Bukkit.broadcastMessage("§7This town has achieved a major milestone.");
            Bukkit.broadcastMessage(" ");
        }
    }

    public static void logTownAction(String townName, String message) {
        if (townName == null || message == null || message.isBlank()) {
            return;
        }

        townAuditLogs.putIfAbsent(townName, new ArrayList<>());

        List<String> logs = townAuditLogs.get(townName);
        String time = LocalDateTime.now().format(AUDIT_TIME_FORMATTER);

        logs.add("[" + time + "] " + message);

        while (logs.size() > MAX_AUDIT_LOG_ENTRIES) {
            logs.remove(0);
        }
    }

    public static double getDailyUpkeepCost() {
        return DAILY_UPKEEP_COST;
    }

    public static int getTownSurvivalDays(String townName) {
        if (townName == null) {
            return 0;
        }

        double balance = townBank.getOrDefault(townName, 0.0);

        if (balance <= 0 || DAILY_UPKEEP_COST <= 0) {
            return 0;
        }

        return (int) Math.floor(balance / DAILY_UPKEEP_COST);
    }

    public static String getTownBankStatus(String townName) {
        if (townName == null) {
            return "No Town";
        }

        double balance = townBank.getOrDefault(townName, 0.0);
        int survivalDays = getTownSurvivalDays(townName);

        return "$" + String.format("%.2f", balance)
                + " | Upkeep: $" + String.format("%.2f", DAILY_UPKEEP_COST)
                + "/day | Survives: " + survivalDays + " day" + (survivalDays == 1 ? "" : "s");
    }

    public static String getPlayerTownTitle(UUID uuid) {
        if (uuid == null) {
            return "";
        }

        return playerTownTitle.getOrDefault(uuid, "");
    }

    public static String getPlayerTownTitleOrDefault(UUID uuid) {
        String title = getPlayerTownTitle(uuid);

        if (title == null || title.isBlank()) {
            return "No Title";
        }

        return title;
    }

    public static long getPlayerLastOnline(UUID uuid) {
        if (uuid == null) {
            return 0L;
        }

        return playerLastOnline.getOrDefault(uuid, 0L);
    }

    public static String formatLastOnline(UUID uuid) {
        if (uuid == null) {
            return "Unknown";
        }

        Player onlinePlayer = Bukkit.getPlayer(uuid);

        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            return "Online now";
        }

        long lastOnline = playerLastOnline.getOrDefault(uuid, 0L);

        if (lastOnline <= 0L) {
            return "Unknown";
        }

        long diff = System.currentTimeMillis() - lastOnline;

        long seconds = diff / 1000L;
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        long days = hours / 24L;

        if (days > 0) {
            return days + " day" + (days == 1 ? "" : "s") + " ago";
        }

        if (hours > 0) {
            return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        }

        if (minutes > 0) {
            return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        }

        return "Just now";
    }

    public static String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + "," + chunk.getZ();
    }

    public static String getLocationKey(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return "unknown";
        }

        return loc.getWorld().getName()
                + ":" + loc.getBlockX()
                + "," + loc.getBlockY()
                + "," + loc.getBlockZ();
    }

    public static boolean sameBlock(Location a, Location b) {
        if (a == null || b == null) return false;
        if (a.getWorld() == null || b.getWorld() == null) return false;

        return a.getWorld().getName().equals(b.getWorld().getName())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    public static int getMaxChunks(int level) {
        return Math.min(level * 10, 1000);
    }

    public static int getPlotIndex(Location loc, int type) {
        int rx = loc.getBlockX() & 15;
        int rz = loc.getBlockZ() & 15;

        if (type == 4) {
            return (rx < 8 ? 0 : 1) + (rz < 8 ? 0 : 2);
        } else if (type == 16) {
            return (rx / 4) + (rz / 4) * 4;
        }
        return -1;
    }

    public static String getPlotOwnerName(Plot plot) {
        if (plot == null || plot.owner == null) return "None";
        OfflinePlayer op = Bukkit.getOfflinePlayer(plot.owner);
        return op.getName() != null ? op.getName() : "Unknown";
    }
}