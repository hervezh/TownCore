package com.silvarys;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

public class TownUpgradesManager implements Listener {

    public static final String PERK_SPEED_PATH = "speed_path";
    public static final String PERK_NO_HUNGER = "no_hunger";
    public static final String PERK_CROP_GROWTH = "crop_growth";
    public static final String PERK_MOBS_TOGGLE = "mobs_toggle";
    public static final String PERK_HASTE_BUILDER = "haste_builder";
    public static final String PERK_NIGHT_VISION = "night_vision";
    public static final String PERK_FIREPROOF = "fireproof";
    public static final String PERK_HALF_FALL_DAMAGE = "half_fall_damage";
    public static final String PERK_REGEN_LOW_HEALTH = "regen_low_health";
    public static final String PERK_WATER_BREATHING = "water_breathing";
    public static final String PERK_REINFORCED_CORE = "war_reinforced_core";
    public static final String PERK_RADAR_L1 = "radar_l1";
    public static final String PERK_RADAR_L2 = "radar_l2";

    public static void startPerkTask(JavaPlugin plugin) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                String townName = Main.playerTown.get(player.getUniqueId());
                if (townName == null) continue;

                Set<String> upgrades = Main.townUpgrades.get(townName);
                if (upgrades == null) continue;

                // Check if player is in their own town
                String currentTerritory = Main.getTownAt(player.getLocation());
                if (currentTerritory == null || !currentTerritory.equalsIgnoreCase(townName)) continue;

                // War check for most perks
                boolean inActiveWar = WarManager.getWarByTown(townName) != null && WarManager.getWarByTown(townName).activeSession;

                // Path Speed
                if (upgrades.contains(PERK_SPEED_PATH)) {
                    Block block = player.getLocation().getBlock().getRelative(0, -1, 0);
                    Material type = block.getType();
                    if (type == Material.COBBLESTONE || type == Material.DIRT_PATH) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, false, false, true));
                    }
                }

                if (inActiveWar) continue; // Following perks are off during war

                // Builder's Spirit (Haste I)
                if (upgrades.contains(PERK_HASTE_BUILDER)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, 0, false, false, true));
                }

                // Night Owl (Night Vision)
                if (upgrades.contains(PERK_NIGHT_VISION)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, false, false, true));
                }

                // Waterman (Water Breathing)
                if (upgrades.contains(PERK_WATER_BREATHING)) {
                    if (player.getLocation().getBlock().getType() == Material.WATER) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 40, 0, false, false, true));
                    }
                }

                // Life Aura (Regen I when low)
                if (upgrades.contains(PERK_REGEN_LOW_HEALTH)) {
                    if (player.getHealth() <= 10.0) { // 5 hearts
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, false, false, true));
                    }
                }
            }
        }, 20L, 20L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHungerLoss(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        String townName = Main.playerTown.get(player.getUniqueId());
        if (townName == null) return;

        Set<String> upgrades = Main.townUpgrades.get(townName);
        if (upgrades == null || !upgrades.contains(PERK_NO_HUNGER)) return;

        // Check if player is in their own town
        String currentTerritory = Main.getTownAt(player.getLocation());
        if (currentTerritory == null || !currentTerritory.equalsIgnoreCase(townName)) return;

        // Disabled when a war is on
        if (WarManager.getWarByTown(townName) != null && WarManager.getWarByTown(townName).activeSession) return;

        event.setCancelled(true);
        player.setFoodLevel(20);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCropGrow(BlockGrowEvent event) {
        String townName = Main.getTownAt(event.getBlock().getLocation());
        if (townName == null) return;

        if (!hasUpgrade(townName, PERK_CROP_GROWTH)) return;

        // 50% chance to advance it another stage immediately
        if (Math.random() < 0.5) {
            if (event.getNewState().getBlockData() instanceof org.bukkit.block.data.Ageable ageable) {
                if (ageable.getAge() < ageable.getMaximumAge()) {
                    ageable.setAge(ageable.getAge() + 1);
                    event.getNewState().setBlockData(ageable);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFallDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL) return;

        String townName = Main.getTownAt(player.getLocation());
        if (townName == null) return;

        // Must be in your own town
        String playerTown = Main.playerTown.get(player.getUniqueId());
        if (!townName.equalsIgnoreCase(playerTown)) return;

        if (!hasUpgrade(townName, PERK_HALF_FALL_DAMAGE)) return;

        // Off during war
        if (WarManager.getWarByTown(townName) != null && WarManager.getWarByTown(townName).activeSession) return;

        event.setDamage(event.getDamage() * 0.5);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFireSpread(org.bukkit.event.block.BlockSpreadEvent event) {
        if (event.getSource().getType() != Material.FIRE) return;

        String townName = Main.getTownAt(event.getBlock().getLocation());
        if (townName == null) return;

        if (hasUpgrade(townName, PERK_FIREPROOF)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBurn(org.bukkit.event.block.BlockBurnEvent event) {
        String townName = Main.getTownAt(event.getBlock().getLocation());
        if (townName == null) return;

        if (hasUpgrade(townName, PERK_FIREPROOF)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFireIgnite(org.bukkit.event.block.BlockIgniteEvent event) {
        if (event.getCause() == org.bukkit.event.block.BlockIgniteEvent.IgniteCause.SPREAD) {
            String townName = Main.getTownAt(event.getBlock().getLocation());
            if (townName != null && hasUpgrade(townName, PERK_FIREPROOF)) {
                event.setCancelled(true);
            }
        }
    }

    public static boolean hasUpgrade(String townName, String upgrade) {
        Set<String> upgrades = Main.townUpgrades.get(townName);
        return upgrades != null && upgrades.contains(upgrade);
    }
}
