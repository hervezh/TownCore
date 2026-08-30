package com.silvarys;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.Set;

public class TownBorderManager {

    private static final int DISPLAY_SECONDS = 12;
    private static final int PARTICLE_STEP = 2;
    private static final int MAX_CLAIMS_TO_SHOW = 150;

    private TownBorderManager() {
    }

    public static void showTownBorders(Player player, String townName, JavaPlugin plugin) {
        if (player == null || townName == null || plugin == null) {
            return;
        }

        Set<String> claims = Main.townChunks.getOrDefault(townName, Collections.emptySet());

        if (claims.isEmpty()) {
            player.sendMessage("§cYour town has no claims to show!");
            return;
        }

        if (claims.size() > MAX_CLAIMS_TO_SHOW) {
            player.sendMessage("§cYour town has too many claims to preview at once!");
            player.sendMessage("§7Claims: §f" + claims.size() + "§7/§f" + MAX_CLAIMS_TO_SHOW);
            player.sendMessage("§7Try standing near your claims and use §f/town map §7instead.");
            return;
        }

        player.sendMessage("§aShowing town borders for §f" + townName + "§a.");
        player.sendMessage("§7The border preview will disappear in §f" + DISPLAY_SECONDS + " seconds§7.");

        new BukkitRunnable() {
            private int ticksPassed = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (ticksPassed >= DISPLAY_SECONDS * 20) {
                    cancel();
                    return;
                }

                for (String claimKey : claims) {
                    ParsedChunk parsed = parseChunkKey(claimKey);

                    if (parsed == null) {
                        continue;
                    }

                    World world = Bukkit.getWorld(parsed.worldName);

                    if (world == null) {
                        continue;
                    }

                    if (!player.getWorld().getName().equals(world.getName())) {
                        continue;
                    }

                    showChunkOuterBorders(player, world, parsed.chunkX, parsed.chunkZ, claims);
                }

                ticksPassed += 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private static void showChunkOuterBorders(Player player, World world, int chunkX, int chunkZ, Set<String> claims) {
        String worldName = world.getName();

        boolean northIsTown = claims.contains(worldName + ":" + chunkX + "," + (chunkZ - 1));
        boolean southIsTown = claims.contains(worldName + ":" + chunkX + "," + (chunkZ + 1));
        boolean westIsTown = claims.contains(worldName + ":" + (chunkX - 1) + "," + chunkZ);
        boolean eastIsTown = claims.contains(worldName + ":" + (chunkX + 1) + "," + chunkZ);

        Chunk chunk = world.getChunkAt(chunkX, chunkZ);

        int minX = chunk.getX() << 4;
        int minZ = chunk.getZ() << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        if (!northIsTown) {
            drawLine(player, world, minX, minZ, maxX, minZ);
        }

        if (!southIsTown) {
            drawLine(player, world, minX, maxZ, maxX, maxZ);
        }

        if (!westIsTown) {
            drawLine(player, world, minX, minZ, minX, maxZ);
        }

        if (!eastIsTown) {
            drawLine(player, world, maxX, minZ, maxX, maxZ);
        }
    }

    private static void drawLine(Player player, World world, int x1, int z1, int x2, int z2) {
        int dx = Integer.compare(x2, x1);
        int dz = Integer.compare(z2, z1);

        int x = x1;
        int z = z1;

        while (true) {
            spawnBorderParticle(player, world, x, z);

            if (x == x2 && z == z2) {
                break;
            }

            for (int i = 0; i < PARTICLE_STEP; i++) {
                if (x != x2) {
                    x += dx;
                }

                if (z != z2) {
                    z += dz;
                }

                if (x == x2 && z == z2) {
                    break;
                }
            }
        }
    }

    private static void spawnBorderParticle(Player player, World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z) + 1;

        Location location = new Location(world, x + 0.5, y + 0.15, z + 0.5);

        player.spawnParticle(
                Particle.HAPPY_VILLAGER,
                location,
                2,
                0.05,
                0.05,
                0.05,
                0.01
        );
    }

    private static ParsedChunk parseChunkKey(String chunkKey) {
        try {
            String[] worldSplit = chunkKey.split(":");

            if (worldSplit.length != 2) {
                return null;
            }

            String worldName = worldSplit[0];

            String[] coordSplit = worldSplit[1].split(",");

            if (coordSplit.length != 2) {
                return null;
            }

            int chunkX = Integer.parseInt(coordSplit[0]);
            int chunkZ = Integer.parseInt(coordSplit[1]);

            return new ParsedChunk(worldName, chunkX, chunkZ);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static class ParsedChunk {
        private final String worldName;
        private final int chunkX;
        private final int chunkZ;

        private ParsedChunk(String worldName, int chunkX, int chunkZ) {
            this.worldName = worldName;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }
    }
}