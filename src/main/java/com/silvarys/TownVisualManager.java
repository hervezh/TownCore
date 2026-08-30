package com.silvarys;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class TownVisualManager {

    private TownVisualManager() {
    }

    public static void showClaimBorder(Player player, Chunk chunk, JavaPlugin plugin) {
        if (player == null || chunk == null || plugin == null) return;

        World world = chunk.getWorld();

        int minX = chunk.getX() << 4;
        int minZ = chunk.getZ() << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        new BukkitRunnable() {
            int ticksRun = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (ticksRun >= 20 * 8) {
                    cancel();
                    return;
                }

                double y = Math.max(player.getLocation().getY() + 0.15, world.getHighestBlockYAt(player.getLocation()) + 0.2);

                for (int x = minX; x <= maxX; x++) {
                    spawnParticle(player, world, x + 0.5, y, minZ + 0.5);
                    spawnParticle(player, world, x + 0.5, y, maxZ + 0.5);
                }

                for (int z = minZ; z <= maxZ; z++) {
                    spawnParticle(player, world, minX + 0.5, y, z + 0.5);
                    spawnParticle(player, world, maxX + 0.5, y, z + 0.5);
                }

                ticksRun += 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private static void spawnParticle(Player player, World world, double x, double y, double z) {
        Location location = new Location(world, x, y, z);

        player.spawnParticle(
                Particle.HAPPY_VILLAGER,
                location,
                1,
                0,
                0,
                0,
                0
        );
    }
}