package com.silvarys;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public class WarListener implements Listener {

    private static final int MINING_TICKS_PER_PERCENT = 10;
    private static final long MINING_TASK_PERIOD_TICKS = 4L;
    private static final double MAX_CORE_DAMAGE_DISTANCE_SQUARED = 36.0;

    private static final Map<UUID, CoreMiningSession> miningSessions = new HashMap<>();

    private static class CoreMiningSession {
        String defenderTown;
        Location coreLocation;
        int miningTicks;
        BukkitTask task;

        long lastAction;
 
        CoreMiningSession(String defenderTown, Location coreLocation) {
            this.defenderTown = defenderTown;
            this.coreLocation = coreLocation;
            this.miningTicks = 0;
            this.lastAction = System.currentTimeMillis();
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (WarManager.isPlayerInActiveWarSession(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou cannot place blocks during an active war session!");
        }
    }

    @EventHandler
    public void onCoreBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block.getType() != Material.BEACON) return;

        String defenderTown = getDefenderTownCoreAt(block.getLocation());

        if (defenderTown == null) return;

        WarManager.War war = WarManager.getWarByDefenderTown(defenderTown);

        if (war == null || !war.activeSession) return;

        event.setCancelled(true);
        event.setDropItems(false);

        event.getPlayer().sendMessage("§cHold left click on the Town Core to damage it!");
    }

    @EventHandler
    public void onCoreDamage(BlockDamageEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block.getType() != Material.BEACON) return;

        String defenderTown = getDefenderTownCoreAt(block.getLocation());

        if (defenderTown == null) return;

        WarManager.War war = WarManager.getWarByDefenderTown(defenderTown);

        if (war == null || !war.activeSession) return;

        event.setCancelled(true);

        String playerTown = Main.playerTown.get(player.getUniqueId());

        if (playerTown == null) {
            player.sendMessage("§cYou must be in the attacking town to damage this Town Core!");
            return;
        }

        if (!playerTown.equalsIgnoreCase(war.attackerTown) && !war.attackerAllies.contains(playerTown)) {
            player.sendMessage("§cOnly attackers and their allies can damage the defender Town Core!");
            return;
        }

        if (war.isRevolt) {
            player.sendMessage("§cYou cannot damage the Town Core during a revolt! This battle is decided by kills within the revolting town's territory.");
            return;
        }

        startCoreMining(player, defenderTown, block.getLocation());
    }

    private void startCoreMining(Player player, String defenderTown, Location coreLocation) {
        UUID uuid = player.getUniqueId();

        CoreMiningSession existing = miningSessions.get(uuid);

        if (existing != null) {
            if (Main.sameBlock(existing.coreLocation, coreLocation)) {
                return;
            }

            stopMining(uuid);
        }

        CoreMiningSession session = new CoreMiningSession(defenderTown, coreLocation);
        miningSessions.put(uuid, session);

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(WarListener.class);

        player.sendMessage("§eDamaging Town Core... keep holding left click!");

        session.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player onlinePlayer = Bukkit.getPlayer(uuid);

            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                stopMining(uuid);
                return;
            }

            WarManager.War war = WarManager.getWarByDefenderTown(defenderTown);

            if (war == null || !war.activeSession) {
                stopMining(uuid);
                return;
            }

            Location currentCore = Main.townCoreLocation.get(defenderTown);

            if (currentCore == null || !Main.sameBlock(currentCore, coreLocation)) {
                stopMining(uuid);
                return;
            }

            if (coreLocation.getWorld() == null || !coreLocation.getWorld().equals(onlinePlayer.getWorld())) {
                stopMining(uuid);
                return;
            }

            if (onlinePlayer.getLocation().distanceSquared(coreLocation) > MAX_CORE_DAMAGE_DISTANCE_SQUARED) {
                onlinePlayer.sendMessage("§cYou moved too far away from the Town Core.");
                stopMining(uuid);
                return;
            }

            Block targetBlock = onlinePlayer.getTargetBlockExact(6);

            if (targetBlock == null || !Main.sameBlock(targetBlock.getLocation(), coreLocation)) {
                onlinePlayer.sendMessage("§cYou stopped damaging the Town Core.");
                stopMining(uuid);
                return;
            }

            if (System.currentTimeMillis() - session.lastAction > 1000L) {
                // Must be clicking (swinging arm) within the last second
                stopMining(uuid);
                return;
            }

            if (targetBlock.getType() != Material.BEACON) {
                stopMining(uuid);
                return;
            }

            session.miningTicks++;

            int requiredTicks = MINING_TICKS_PER_PERCENT;
            if (TownUpgradesManager.hasUpgrade(defenderTown, TownUpgradesManager.PERK_REINFORCED_CORE)) {
                requiredTicks = 30; // 3x slower
            }

            if (session.miningTicks >= requiredTicks) {
                session.miningTicks = 0;

                WarManager.damageCore(onlinePlayer, defenderTown, 1);

                int health = WarManager.getCoreHealthPercent(defenderTown);

                if (health > 0) {
                    onlinePlayer.sendMessage("§cTown Core Health: §f" + health + "%");
                }
            }

        }, 0L, MINING_TASK_PERIOD_TICKS);
    }

    private void stopMining(UUID uuid) {
        CoreMiningSession session = miningSessions.remove(uuid);

        if (session != null && session.task != null) {
            session.task.cancel();
        }
    }

    @EventHandler
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        CoreMiningSession session = miningSessions.get(player.getUniqueId());
        
        if (session != null) {
            session.lastAction = System.currentTimeMillis();
        }
    }
 
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopMining(event.getPlayer().getUniqueId());
    }
 
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        if (!WarManager.isPlayerInActiveWarSession(player)) {
            return;
        }

        // We want them to KEEP their gear, but DROP their potions.
        List<ItemStack> drops = event.getDrops();
        List<ItemStack> toKeep = new ArrayList<>();
        List<ItemStack> toDrop = new ArrayList<>();

        for (ItemStack item : drops) {
            if (item == null || item.getType() == Material.AIR) continue;

            if (isPotion(item.getType())) {
                toDrop.add(item);
            } else {
                toKeep.add(item);
            }
        }

        // Modify the death drops to only contain potions
        drops.clear();
        drops.addAll(toDrop);

        // Keep the rest in inventory
        event.setKeepInventory(true);
        event.getDrops().clear();
        event.getDrops().addAll(toDrop);
        
        // Remove potions from the saved inventory so they aren't duplicated
        player.getInventory().remove(Material.POTION);
        player.getInventory().remove(Material.SPLASH_POTION);
        player.getInventory().remove(Material.LINGERING_POTION);
        
        // Ensure all stacks are removed (remove(Material) only removes one stack)
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && isPotion(item.getType())) {
                player.getInventory().setItem(i, null);
            }
        }

        // Damage gear by 10%
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
                int max = item.getType().getMaxDurability();
                if (max > 0) {
                    int damageToAdd = (int) (max * 0.1);
                    int newDamage = damageable.getDamage() + damageToAdd;
                    
                    // Don't break the item, leave it at 1 HP if it would break
                    if (newDamage >= max) {
                        newDamage = max - 1;
                    }
                    
                    damageable.setDamage(newDamage);
                    item.setItemMeta(damageable);
                }
            }
        }

        player.sendMessage("§6§l[WAR] §r§7You kept your gear, but it was §6damaged by 10% §7and your potions were dropped!");

        // Award points for kills
        String victimTown = Main.playerTown.get(player.getUniqueId());
        WarManager.War war = WarManager.getWarByTown(victimTown);
        
        if (war != null && war.activeSession) {
            Player killer = player.getKiller();
            String killerTown = killer != null ? Main.playerTown.get(killer.getUniqueId()) : null;
            
            if (war.isRevolt) {
                // Revolt rules: Kills only count in the revolting town's claims (attackerTown)
                String deathTown = Main.getTownAt(player.getLocation());
                if (deathTown != null && deathTown.equalsIgnoreCase(war.attackerTown)) {
                    if (killerTown != null) {
                        boolean killerIsAttacker = killerTown.equalsIgnoreCase(war.attackerTown) || war.attackerAllies.contains(killerTown);
                        boolean killerIsDefender = killerTown.equalsIgnoreCase(war.defenderTown) || war.defenderAllies.contains(killerTown);
                        
                        if (killerIsAttacker) {
                            war.attackerPoints++;
                            Bukkit.broadcastMessage("§4§l[REVOLT] §f" + war.attackerTown + " §cearned a kill point! §8(§f" + player.getName() + "§8)");
                        } else if (killerIsDefender) {
                            war.defenderPoints++;
                            Bukkit.broadcastMessage("§4§l[REVOLT] §f" + war.defenderTown + " §cearned a kill point! §8(§f" + player.getName() + "§8)");
                        }
                    }
                }
            } else {
                // Normal war rules: Kills award points anywhere? 
                // Let's award points for kills in normal wars too to make them more dynamic.
                if (killerTown != null) {
                    boolean killerIsAttacker = killerTown.equalsIgnoreCase(war.attackerTown) || war.attackerAllies.contains(killerTown);
                    boolean killerIsDefender = killerTown.equalsIgnoreCase(war.defenderTown) || war.defenderAllies.contains(killerTown);
                    
                    if (killerIsAttacker) {
                        war.attackerPoints++;
                        Bukkit.broadcastMessage("§4§l[WAR] §f" + war.attackerTown + " §cearned a kill point! §8(§f" + player.getName() + "§8)");
                    } else if (killerIsDefender) {
                        war.defenderPoints++;
                        Bukkit.broadcastMessage("§4§l[WAR] §f" + war.defenderTown + " §cearned a kill point! §8(§f" + player.getName() + "§8)");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onWarBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        String townName = Main.getTownAt(loc);
        if (townName == null) return;

        WarManager.War war = WarManager.getWarByTown(townName);
        if (war != null && war.activeSession) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cBlock breaking is disabled in §f" + townName + " §cwhile it is at war!");
        }
    }

    @EventHandler
    public void onWarBlockPlace(BlockPlaceEvent event) {
        Location loc = event.getBlock().getLocation();
        String townName = Main.getTownAt(loc);
        if (townName == null) return;

        WarManager.War war = WarManager.getWarByTown(townName);
        if (war != null && war.activeSession) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cBlock placing is disabled in §f" + townName + " §cwhile it is at war!");
        }
    }

    private boolean isPotion(Material type) {
        return type == Material.POTION || 
               type == Material.SPLASH_POTION || 
               type == Material.LINGERING_POTION;
    }

    private String getDefenderTownCoreAt(Location location) {
        for (Map.Entry<String, Location> entry : Main.townCoreLocation.entrySet()) {
            String townName = entry.getKey();
            Location coreLocation = entry.getValue();

            if (!Main.sameBlock(coreLocation, location)) continue;

            WarManager.War war = WarManager.getWarByDefenderTown(townName);

            if (war != null && war.activeSession) {
                return townName;
            }
        }

        return null;
    }
}