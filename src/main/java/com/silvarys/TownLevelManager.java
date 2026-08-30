package com.silvarys;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class TownLevelManager {

    public static Map<String, Map<String, Integer>> townTaskXP = new HashMap<>();
    public static Map<String, Map<String, Integer>> townTaskLevel = new HashMap<>();

    public static final String[] TASKS = {
            "farming",
            "cooking",
            "mining",
            "woodcutting",
            "pvp",
            "pve",
            "building",
            "fishing",
            "smithing",
            "enchanting"
    };

    public static final int XP_PER_LEVEL = 1000;
    public static final int MINING_XP_PER_LEVEL = 500;
    public static final int WOODCUTTING_XP_PER_LEVEL = 500;
    public static final int ENCHANTING_XP_PER_LEVEL = 500;
    public static final int FISHING_XP_PER_LEVEL = 100;
    public static final int PVE_XP_PER_LEVEL = 250;
    public static final int PVP_XP_PER_LEVEL = 1000;

    public static void initTown(String townName) {
        townTaskXP.putIfAbsent(townName, new HashMap<>());
        townTaskLevel.putIfAbsent(townName, new HashMap<>());

        Map<String, Integer> xp = townTaskXP.get(townName);
        Map<String, Integer> levels = townTaskLevel.get(townName);

        for (String task : TASKS) {
            xp.putIfAbsent(task, 0);
            levels.putIfAbsent(task, 1);
        }
    }

    public static void addXP(String townName, String task, int amount) {
        if (townName == null || task == null || amount <= 0) return;

        if (!townTaskXP.containsKey(townName) || !townTaskLevel.containsKey(townName)) {
            initTown(townName);
        }

        ensureTaskExists(townName, task);

        Map<String, Integer> xp = townTaskXP.get(townName);
        Map<String, Integer> levels = townTaskLevel.get(townName);

        int currentLevel = levels.getOrDefault(task, 1);
        int xpNeeded = getRequiredXPForTask(task, currentLevel);
        int currentXP = xp.getOrDefault(task, 0) + amount;

        boolean taskLeveledUp = false;

        while (currentXP >= xpNeeded) {
            currentXP -= xpNeeded;
            currentLevel++;
            taskLeveledUp = true;
            xpNeeded = getRequiredXPForTask(task, currentLevel);
        }

        xp.put(task, currentXP);
        levels.put(task, currentLevel);

        townTaskXP.put(townName, xp);
        townTaskLevel.put(townName, levels);

        if (!taskLeveledUp) return;

        int oldTownLevel = Main.townLevel.getOrDefault(townName, 1);
        int newTownLevel = calculateTownLevel(townName);
        Main.townLevel.put(townName, newTownLevel);

        notifyTaskLevelUp(townName, task, currentLevel, currentXP, xpNeeded, newTownLevel);

        if (currentLevel == 40 && isIncomeTask(task)) {
            notifyIncomeUnlocked(townName, task);
        }

        /*
         * Only announce town level milestones every 10 levels.
         *
         * Example:
         * Level 2-9   = no global announcement
         * Level 10    = announcement
         * Level 11-19 = no global announcement
         * Level 20    = announcement
         */
        if (newTownLevel > oldTownLevel) {
            int oldMilestone = oldTownLevel / 10;
            int newMilestone = newTownLevel / 10;

            if (newMilestone > oldMilestone) {
                int milestoneLevel = newMilestone * 10;
                Main.onTownLevelUp(townName, milestoneLevel);
            }

            int currentTokens = Main.townUpgradeTokens.getOrDefault(townName, 0);
            int newTokens = Math.min(10, currentTokens + (newTownLevel - oldTownLevel));
            Main.townUpgradeTokens.put(townName, newTokens);

            if (newTokens > currentTokens) {
                Main.logTownAction(townName, "Town reached Level " + newTownLevel + " and earned " + (newTokens - currentTokens) + " upgrade tokens.");
            }
    }

    public static int calculateTownLevel(String townName) {
        if (!townTaskLevel.containsKey(townName)) return 1;

        initTown(townName);

        int total = 0;

        for (String task : TASKS) {
            total += townTaskLevel.get(townName).getOrDefault(task, 1);
        }

        return Math.max(1, total - TASKS.length + 1);
    }

    public static int getTaskXP(String townName, String task) {
        if (townName == null || task == null) return 0;

        if (!townTaskXP.containsKey(townName)) {
            initTown(townName);
        }

        ensureTaskExists(townName, task);

        return townTaskXP.getOrDefault(townName, new HashMap<>()).getOrDefault(task, 0);
    }

    public static int getTaskLevel(String townName, String task) {
        if (townName == null || task == null) return 1;

        if (!townTaskLevel.containsKey(townName)) {
            initTown(townName);
        }

        ensureTaskExists(townName, task);

        return townTaskLevel.getOrDefault(townName, new HashMap<>()).getOrDefault(task, 1);
    }

    public static int getRequiredXPForTask(String task, int level) {
        int baseXP = XP_PER_LEVEL;

        if (task != null) {
            baseXP = switch (task.toLowerCase()) {
                case "mining" -> MINING_XP_PER_LEVEL;
                case "woodcutting" -> WOODCUTTING_XP_PER_LEVEL;
                case "enchanting" -> ENCHANTING_XP_PER_LEVEL;
                case "fishing" -> FISHING_XP_PER_LEVEL;
                case "pve" -> PVE_XP_PER_LEVEL;
                case "pvp" -> PVP_XP_PER_LEVEL;
                default -> XP_PER_LEVEL;
            };
        }

        return baseXP + (Math.max(0, level - 1) * 50);
    }

    private static void ensureTaskExists(String townName, String task) {
        townTaskXP.putIfAbsent(townName, new HashMap<>());
        townTaskLevel.putIfAbsent(townName, new HashMap<>());

        townTaskXP.get(townName).putIfAbsent(task, 0);
        townTaskLevel.get(townName).putIfAbsent(task, 1);
    }

    private static void notifyTaskLevelUp(String townName, String task, int currentLevel, int currentXP, int xpNeeded, int townLevel) {
        for (UUID uuid : Main.townMembers.getOrDefault(townName, new HashSet<>())) {
            Player player = Bukkit.getPlayer(uuid);

            if (player == null) continue;

            player.sendMessage("§6§l⚑ Town Skill Level Up!");
            player.sendMessage("§e" + formatTaskName(task) + " §7reached §aLevel " + currentLevel);
            player.sendMessage("§7Town Level: §f" + townLevel);
            player.sendMessage("§7Progress: §f" + currentXP + "/" + xpNeeded + " XP");

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.35f);
        }
    }

    private static String formatTaskName(String task) {
        if (task == null || task.isEmpty()) return "Unknown";

        return switch (task.toLowerCase()) {
            case "pve" -> "PvE";
            case "pvp" -> "PvP";
            default -> {
                String[] words = task.replace("_", " ").split(" ");
                StringBuilder formatted = new StringBuilder();

                for (String word : words) {
                    if (word.isEmpty()) continue;

                    if (formatted.length() > 0) {
                        formatted.append(" ");
                    }

                    formatted.append(Character.toUpperCase(word.charAt(0)))
                            .append(word.substring(1).toLowerCase());
                }

                yield formatted.toString();
            }
        };
    }

    private static boolean isIncomeTask(String task) {
        if (task == null) return false;
        return switch (task.toLowerCase()) {
            case "woodcutting", "mining", "pve", "farming", "cooking" -> true;
            default -> false;
        };
    }

    private static void notifyIncomeUnlocked(String townName, String task) {
        for (UUID uuid : Main.townMembers.getOrDefault(townName, new HashSet<>())) {
            Player player = Bukkit.getPlayer(uuid);

            if (player == null) continue;

            player.sendMessage(" ");
            player.sendMessage("§6§l✦ Town Income Unlocked!");
            player.sendMessage("§eYour town can now earn §f" + formatTaskName(task) + " §eresources!");
            player.sendMessage("§7Type §f/town income §7to check it out.");
            player.sendMessage(" ");

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        }
    }
}