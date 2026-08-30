package com.silvarys;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class WarBossBarManager {

    private static final Map<String, BossBar> warBossBars = new HashMap<>();
    private static final Map<String, Integer> sessionSecondsLeft = new HashMap<>();

    public static void startWarBossBar(JavaPlugin plugin, WarManager.War war, int durationSeconds) {
        String warKey = WarManager.getWarKey(war.attackerTown, war.defenderTown);

        stopWarBossBar(warKey);

        sessionSecondsLeft.put(warKey, durationSeconds);

        BossBar bossBar = Bukkit.createBossBar(
                buildTitle(war, durationSeconds),
                BarColor.RED,
                BarStyle.SEGMENTED_10
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }

        warBossBars.put(warKey, bossBar);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!warBossBars.containsKey(warKey)) {
                    cancel();
                    return;
                }

                if (!war.activeSession) {
                    stopWarBossBar(warKey);
                    cancel();
                    return;
                }

                int secondsLeft = sessionSecondsLeft.getOrDefault(warKey, 0);

                if (secondsLeft <= 0) {
                    stopWarBossBar(warKey);
                    cancel();
                    return;
                }

                double progress = (double) secondsLeft / durationSeconds;
                bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
                bossBar.setTitle(buildTitle(war, secondsLeft));

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!bossBar.getPlayers().contains(player)) {
                        bossBar.addPlayer(player);
                    }
                }

                sessionSecondsLeft.put(warKey, secondsLeft - 1);
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private static String buildTitle(WarManager.War war, int secondsLeft) {
        String timeStr = formatTime(secondsLeft);

        String coreInfo = "Unknown";
        Location core = Main.townCoreLocation.get(war.defenderTown);

        if (core != null) {
            coreInfo = "X:" + core.getBlockX()
                    + " Y:" + core.getBlockY()
                    + " Z:" + core.getBlockZ();
        }

        int coreHealth = WarManager.getCoreHealthPercent(war.defenderTown);

        return "§c⚔ " + war.attackerTown + " " + war.attackerPoints
                + " §7vs §a" + war.defenderPoints + " " + war.defenderTown
                + " §7| §eCore: " + coreInfo
                + " §7| §cHealth: §f" + coreHealth + "%"
                + " §7| §f" + timeStr;
    }

    private static String formatTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;

        if (hours > 0) {
            return hours + "h " + minutes + "m " + secs + "s";
        }

        return minutes + "m " + secs + "s";
    }

    public static void showActiveBarsToPlayer(Player player) {
        for (BossBar bar : warBossBars.values()) {
            if (!bar.getPlayers().contains(player)) {
                bar.addPlayer(player);
            }
        }
    }

    public static void stopWarBossBar(String warKey) {
        BossBar bar = warBossBars.remove(warKey);

        if (bar != null) {
            bar.removeAll();
        }

        sessionSecondsLeft.remove(warKey);
    }

    public static void stopAllWarBossBars() {
        for (BossBar bar : warBossBars.values()) {
            bar.removeAll();
        }

        warBossBars.clear();
        sessionSecondsLeft.clear();
    }
}