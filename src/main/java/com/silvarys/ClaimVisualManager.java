package com.silvarys;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class ClaimVisualManager {

    private static final int DISPLAY_SECONDS = 8;
    private static final int PARTICLE_STEP = 2;

    public static void showClaimBorder(Player player, Chunk chunk) {
        if (player == null || chunk == null || chunk.getWorld() == null) return;

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(ClaimVisualManager.class);
        World world = chunk.getWorld();

        int minX = chunk.getX() * 16;
        int minZ = chunk.getZ() * 16;
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (ticks >= DISPLAY_SECONDS * 20) {
                    cancel();
                    return;
                }

                double y = Math.max(player.getLocation().getY() + 0.2, world.getHighestBlockYAt(player.getLocation()) + 0.2);

                for (int x = minX; x <= maxX; x += PARTICLE_STEP) {
                    spawnParticle(player, new Location(world, x + 0.5, y, minZ + 0.5));
                    spawnParticle(player, new Location(world, x + 0.5, y, maxZ + 0.5));
                }

                for (int z = minZ; z <= maxZ; z += PARTICLE_STEP) {
                    spawnParticle(player, new Location(world, minX + 0.5, y, z + 0.5));
                    spawnParticle(player, new Location(world, maxX + 0.5, y, z + 0.5));
                }

                ticks += 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private static void spawnParticle(Player player, Location location) {
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
}