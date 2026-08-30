package com.silvarys;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UpkeepManager {

    private static final double UPKEEP_PER_MINUTE = Main.DAILY_UPKEEP_COST / 1440.0;
    private static final int LOW_BANK_WARNING_DAYS = 3;

    public static void startUpkeepTimer(JavaPlugin plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                processUpkeep();
            }
        }.runTaskTimer(plugin, 20L * 60, 20L * 60); // Run every minute
    }

    private static void processUpkeep() {
        List<String> townsToFall = new ArrayList<>();
        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(UpkeepManager.class);

        for (String townName : new HashSet<>(Main.townLevel.keySet())) {
            double balance = Main.townBank.getOrDefault(townName, 0.0);

            if (balance >= UPKEEP_PER_MINUTE) {
                double newBalance = balance - UPKEEP_PER_MINUTE;
                Main.townBank.put(townName, newBalance);

                // No message every minute to avoid spam, but we check for low funds
                if (newBalance < getThreeDayWarningAmount()) {
                    // Only warn once every 6 hours to avoid spamming rulers
                    long lastWarn = Main.lastUpkeepWarning.getOrDefault(townName, 0L);
                    if (System.currentTimeMillis() - lastWarn > 6 * 3600000) {
                        notifyLowFundsWarning(townName, newBalance);
                        Main.lastUpkeepWarning.put(townName, System.currentTimeMillis());
                    }
                }

            } else {
                townsToFall.add(townName);
            }
        }

        for (String townName : townsToFall) {
            fallTown(townName);
        }

        StorageManager.saveData(plugin);
    }

    public static void checkTownFundsWarning(String townName) {
        if (townName == null || !Main.townLevel.containsKey(townName)) return;

        double balance = Main.townBank.getOrDefault(townName, 0.0);

        if (balance < getThreeDayWarningAmount()) {
            notifyLowFundsWarning(townName, balance);
        }
    }

    private static void notifyLowFundsWarning(String townName, double balance) {
        double safeAmount = getThreeDayWarningAmount();
        double needed = Math.max(0, safeAmount - balance);

        for (UUID uuid : Main.townMembers.getOrDefault(townName, new HashSet<>())) {
            Player player = Bukkit.getPlayer(uuid);

            if (player == null) continue;

            String role = Main.playerRole.getOrDefault(uuid, "member");

            if (!role.equalsIgnoreCase("ruler") && !role.equalsIgnoreCase("assistant")) {
                continue;
            }

            player.sendMessage("§c§l⚠ Town Bank Warning");
            player.sendMessage("§c" + townName + " §7has less than §f" + LOW_BANK_WARNING_DAYS + " days §7of upkeep money.");
            player.sendMessage("§7Current Bank: §f$" + String.format("%.2f", balance));
            player.sendMessage("§7Safe Amount: §f$" + String.format("%.2f", safeAmount));
            player.sendMessage("§7Needed: §f$" + String.format("%.2f", needed));
            player.sendMessage("§eDeposit money with §f/town deposit <amount>");
        }
    }

    private static double getThreeDayWarningAmount() {
        return Main.DAILY_UPKEEP_COST * LOW_BANK_WARNING_DAYS;
    }

    private static void notifyTownMembers(String townName, String... messages) {
        for (UUID uuid : Main.townMembers.getOrDefault(townName, new HashSet<>())) {
            Player player = Bukkit.getPlayer(uuid);

            if (player == null) continue;

            for (String message : messages) {
                player.sendMessage(message);
            }
        }
    }

    private static void fallTown(String townName) {
        if (townName == null || !Main.townLevel.containsKey(townName)) return;

        Location coreLocation = Main.townCoreLocation.get(townName);

        notifyTownMembers(
                townName,
                "§4§l[Town Fallen]",
                "§c" + townName + " §7has fallen because it could not pay upkeep.",
                "§7All claims, relations, wars, locked blocks, and town data have been removed."
        );

        removeTownCoreBlock(townName, coreLocation);
        removePlayersFromTown(townName);
        removeTownRelations(townName);
        removeTownWars(townName);
        removePendingRenameData(townName);
        removeRelatedPendingTownCreations(townName);
        removeTownData(townName);

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(UpkeepManager.class);
        StorageManager.deleteTownBackups(plugin, townName);

        Bukkit.broadcastMessage("§4§l⚠ A town has fallen! §r§c" + townName + " §chas fallen due to lack of funds.");

        StorageManager.saveData(JavaPlugin.getProvidingPlugin(UpkeepManager.class));
    }

    private static void removeTownCoreBlock(String townName, Location coreLocation) {
        if (coreLocation == null || coreLocation.getWorld() == null) return;
 
        Block coreBlock = coreLocation.getBlock();
 
        if (coreBlock.getType() == Material.BEACON) {
            coreBlock.setType(Material.BLACKSTONE);
            
            String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String key = coreLocation.getWorld().getName() + ":" + coreLocation.getBlockX() + ":" + coreLocation.getBlockY() + ":" + coreLocation.getBlockZ();
            Main.ruinedCores.put(key, townName + ":" + date);

            // Turn base into Cobbled Deepslate
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Block baseBlock = coreLocation.clone().add(x, -1, z).getBlock();
                    if (baseBlock.getType() == Material.IRON_BLOCK) {
                        baseBlock.setType(Material.COBBLED_DEEPSLATE);
                    }
                }
            }
        }
    }

    private static void removePlayersFromTown(String townName) {
        for (UUID uuid : new HashSet<>(Main.townMembers.getOrDefault(townName, new HashSet<>()))) {
            Player player = Bukkit.getPlayer(uuid);

            Main.playerTown.remove(uuid);
            Main.playerRole.remove(uuid);
            ChatListener.townChatPlayers.remove(uuid);
            ChatListener.allyChatPlayers.remove(uuid);

            if (player != null) {
                player.sendMessage("§7You are no longer in a town because §c" + townName + " §7has fallen.");
            }
        }
    }

    private static void removeTownRelations(String townName) {
        for (java.util.Set<String> allies : Main.townAllies.values()) {
            allies.remove(townName);
        }

        for (java.util.Set<String> enemies : Main.townEnemies.values()) {
            enemies.remove(townName);
        }

        for (java.util.Set<String> wars : Main.townWars.values()) {
            wars.remove(townName);
        }
    }

    private static void removeTownWars(String townName) {
        WarManager.War war = WarManager.getWarByTown(townName);

        while (war != null) {
            WarManager.removeWar(war);
            war = WarManager.getWarByTown(townName);
        }
    }

    private static void removePendingRenameData(String townName) {
        Main.pendingRenames.remove(townName);
        Main.pendingRenameRequester.remove(townName);

        List<String> requestsToRemove = new ArrayList<>();

        for (Map.Entry<String, String> entry : Main.pendingRenames.entrySet()) {
            String oldName = entry.getKey();
            String requestedNewName = entry.getValue();

            if (oldName.equalsIgnoreCase(townName) || requestedNewName.equalsIgnoreCase(townName)) {
                requestsToRemove.add(oldName);
            }
        }

        for (String oldName : requestsToRemove) {
            Main.pendingRenames.remove(oldName);
            Main.pendingRenameRequester.remove(oldName);
        }
    }

    private static void removeRelatedPendingTownCreations(String townName) {
        List<String> pendingKeysToRemove = new ArrayList<>();

        for (Map.Entry<String, Main.PendingTown> entry : Main.pendingTowns.entrySet()) {
            Main.PendingTown pending = entry.getValue();

            if (pending == null || pending.townName == null) continue;

            if (pending.townName.equalsIgnoreCase(townName)) {
                if (pending.task != null) pending.task.cancel();
                if (pending.countdownTask != null) pending.countdownTask.cancel();

                Location pendingCoreLocation = pending.coreLocation;

                if (pendingCoreLocation != null && pendingCoreLocation.getWorld() != null) {
                    Block block = pendingCoreLocation.getBlock();

                    if (block.getType() == Material.BEACON) {
                        block.setType(Material.AIR);
                    }
                }

                pendingKeysToRemove.add(entry.getKey());
            }
        }

        for (String key : pendingKeysToRemove) {
            Main.pendingTowns.remove(key);
        }
    }

    private static void removeTownData(String townName) {
        Main.townLevel.remove(townName);
        Main.townBank.remove(townName);
        Main.townChunks.remove(townName);
        Main.townOwner.remove(townName);
        Main.townAssistant.remove(townName);
        Main.townMembers.remove(townName);
        Main.townAllies.remove(townName);
        Main.townWars.remove(townName);
        Main.townEnemies.remove(townName);
        Main.townSpawn.remove(townName);
        Main.townCoreLocation.remove(townName);
        Main.townSpawnCooldown.remove(townName);
        Main.townRenameUsed.remove(townName);
        Main.townMotd.remove(townName);
        Main.lockedBlocks.remove(townName);

        TownLevelManager.townTaskXP.remove(townName);
        TownLevelManager.townTaskLevel.remove(townName);
    }
}