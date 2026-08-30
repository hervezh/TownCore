package com.silvarys;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class RadarManager {

    private static final Map<String, Long> lastAlertTime = new HashMap<>();
    private static final long ALERT_COOLDOWN = 30000; // 30 seconds cooldown per town

    public static void startTask(JavaPlugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (String townName : Main.townLevel.keySet()) {
                // Check if town has radar
                boolean hasL1 = TownUpgradesManager.hasUpgrade(townName, TownUpgradesManager.PERK_RADAR_L1);
                boolean hasL2 = TownUpgradesManager.hasUpgrade(townName, TownUpgradesManager.PERK_RADAR_L2);

                if (!hasL1 && !hasL2) continue;

                // Disable during war
                if (WarManager.getWarByTown(townName) != null && WarManager.getWarByTown(townName).activeSession) {
                    continue;
                }

                int range = hasL2 ? 100 : 50;
                Set<String> chunks = Main.townChunks.getOrDefault(townName, new HashSet<>());
                if (chunks.isEmpty()) continue;

                checkNearbyPlayers(townName, chunks, range, hasL2);
            }
        }, 100L, 100L); // Every 5 seconds
    }

    private static void checkNearbyPlayers(String townName, Set<String> townChunks, int range, boolean hasL2) {
        long now = System.currentTimeMillis();
        if (now - lastAlertTime.getOrDefault(townName, 0L) < ALERT_COOLDOWN) return;

        boolean detectedInside = false;
        boolean detectedOutside = false;

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Skip allies and members
            if (isAllied(player, townName)) continue;

            String currentTown = Main.getTownAt(player.getLocation());
            if (currentTown != null && currentTown.equalsIgnoreCase(townName)) {
                if (hasL2) {
                    detectedInside = true;
                    break; // Priority alert
                }
            } else {
                // Check distance to claims
                if (isNearClaims(player, townChunks, range)) {
                    detectedOutside = true;
                }
            }
        }

        if (detectedInside && hasL2) {
            broadcastRadarAlert(townName, "[" + townName + " Radar] An unallied player has been spotted inside your " + townName + "'s claims!", true);
            lastAlertTime.put(townName, now);
        } else if (detectedOutside) {
            broadcastRadarAlert(townName, "[" + townName + " Radar] An unallied player has been detected near " + townName, false);
            lastAlertTime.put(townName, now);
        }
    }

    private static boolean isAllied(Player player, String townName) {
        String playerTown = Main.playerTown.get(player.getUniqueId());
        if (playerTown == null) return false;
        if (playerTown.equalsIgnoreCase(townName)) return true;
        
        Set<String> allies = Main.townAllies.getOrDefault(townName, new HashSet<>());
        return allies.contains(playerTown);
    }

    private static boolean isNearClaims(Player player, Set<String> townChunks, int range) {
        Chunk playerChunk = player.getLocation().getChunk();
        int px = playerChunk.getX();
        int pz = playerChunk.getZ();
        
        // Range in chunks (approx)
        int chunkRange = (range / 16) + 1;

        for (String chunkKey : townChunks) {
            String[] parts = chunkKey.split(":");
            if (parts.length < 3) continue;
            
            // Check world if possible? Main.getChunkKey uses world:x:z
            // Let's assume same world for now or check it
            if (!parts[0].equals(player.getWorld().getName())) continue;

            int tx = Integer.parseInt(parts[1]);
            int tz = Integer.parseInt(parts[2]);

            if (Math.abs(px - tx) <= chunkRange && Math.abs(pz - tz) <= chunkRange) {
                // More precise check
                double distSq = getDistanceSqToChunk(player.getLocation(), tx, tz);
                if (distSq <= range * range) return true;
            }
        }
        return false;
    }

    private static double getDistanceSqToChunk(org.bukkit.Location loc, int tx, int tz) {
        int minX = tx << 4;
        int minZ = tz << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        double dx = Math.max(0, Math.max(minX - loc.getX(), loc.getX() - maxX));
        double dz = Math.max(0, Math.max(minZ - loc.getZ(), loc.getZ() - maxZ));

        return dx * dx + dz * dz;
    }

    private static void broadcastRadarAlert(String townName, String message, boolean inside) {
        Component comp = Component.text(message)
                .color(inside ? TextColor.color(0xFF5555) : TextColor.color(0xFFAA00))
                .decoration(TextDecoration.ITALIC, false);

        for (UUID memberUUID : Main.townMembers.getOrDefault(townName, new HashSet<>())) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && member.isOnline()) {
                member.sendMessage(comp);
            }
        }
    }
}
