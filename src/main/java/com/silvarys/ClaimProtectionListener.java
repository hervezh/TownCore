package com.silvarys;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.entity.EnderPearl;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ClaimProtectionListener implements Listener {

    private String getChunkOwner(Chunk chunk) {
        String chunkKey = Main.getChunkKey(chunk);

        for (java.util.Map.Entry<String, java.util.Set<String>> entry : Main.townChunks.entrySet()) {
            if (entry.getValue().contains(chunkKey)) {
                return entry.getKey();
            }
        }

        return null;
    }

    private boolean isTownCoreChunk(Chunk chunk) {
        String chunkKey = Main.getChunkKey(chunk);
        for (org.bukkit.Location loc : Main.townCoreLocation.values()) {
            if (Main.getChunkKey(loc.getChunk()).equals(chunkKey)) return true;
        }
        return false;
    }

    private boolean canBypassClaimProtection(Player player) {
        return player.isOp()
                || player.hasPermission("silvarys.staff")
                || player.hasPermission("silvarys.town.bypass")
                || player.hasPermission("silvarys.town.claim.bypass");
    }

    private boolean isCreationChunk(Chunk chunk) {
        String chunkKey = Main.getChunkKey(chunk);

        for (Main.PendingTown pending : Main.pendingTowns.values()) {
            if (pending.coreLocation == null || pending.coreLocation.getWorld() == null) continue;

            String pendingChunk = Main.getChunkKey(pending.coreLocation.getChunk());

            if (pendingChunk.equals(chunkKey)) {
                return true;
            }
        }

        return false;
    }

    private Main.PendingTown getPendingTownInChunk(Chunk chunk) {
        String chunkKey = Main.getChunkKey(chunk);

        for (Main.PendingTown pending : Main.pendingTowns.values()) {
            if (pending.coreLocation == null || pending.coreLocation.getWorld() == null) continue;

            String pendingChunk = Main.getChunkKey(pending.coreLocation.getChunk());

            if (pendingChunk.equals(chunkKey)) {
                return pending;
            }
        }

        return null;
    }

    private boolean isMember(Player player, String townName) {
        String playerTown = Main.playerTown.get(player.getUniqueId());
        return townName.equals(playerTown);
    }

    private boolean isPendingTownCoreBlock(Block block) {
        for (Main.PendingTown pending : Main.pendingTowns.values()) {
            if (Main.sameBlock(pending.coreLocation, block.getLocation())) {
                return true;
            }
        }

        return false;
    }

    private boolean isPendingTownCoreBaseBlock(Block block) {
        for (Main.PendingTown pending : Main.pendingTowns.values()) {
            if (pending.coreLocation == null || pending.coreLocation.getWorld() == null) continue;
            if (block.getWorld() == null) continue;
            if (!pending.coreLocation.getWorld().getName().equals(block.getWorld().getName())) continue;

            int coreX = pending.coreLocation.getBlockX();
            int coreY = pending.coreLocation.getBlockY();
            int coreZ = pending.coreLocation.getBlockZ();

            int blockX = block.getX();
            int blockY = block.getY();
            int blockZ = block.getZ();

            boolean sameBaseLayer = blockY == coreY - 1;
            boolean withinBaseX = Math.abs(blockX - coreX) <= 1;
            boolean withinBaseZ = Math.abs(blockZ - coreZ) <= 1;

            if (sameBaseLayer && withinBaseX && withinBaseZ && block.getType() == Material.IRON_BLOCK) {
                return true;
            }
        }
        return false;
    }

    private boolean isRulerOrAssistant(Player player) {
        String role = Main.playerRole.getOrDefault(player.getUniqueId(), "member");
        return role.equals("ruler") || role.equals("assistant");
    }

    private boolean hasPlotAccess(Player player, Location loc) {
        String chunkKey = Main.getChunkKey(loc.getChunk());
        if (!Main.subdividedChunks.containsKey(chunkKey)) return true;

        int type = Main.subdividedChunks.get(chunkKey);
        int index = Main.getPlotIndex(loc, type);
        String plotKey = chunkKey + ":" + index;

        Main.Plot plot = Main.townPlots.get(plotKey);
        if (plot == null || plot.owner == null) {
            // Unowned plots can only be modified by town leaders
            return isRulerOrAssistant(player);
        }

        return plot.owner.equals(player.getUniqueId()) || isRulerOrAssistant(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Handle Ruined Town Cores
        String key = block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
        if (Main.ruinedCores.containsKey(key)) {
            String data = Main.ruinedCores.get(key);
            String[] parts = data.split(":", 2);
            String townName = parts[0];
            String date = parts.length > 1 ? parts[1] : "Unknown";

            ItemStack drop = new ItemStack(Material.BLACKSTONE);
            ItemMeta meta = drop.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("§c§lFallen " + townName + "'s tired town core")
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(List.of(
                        Component.text("§7Fell into ruin " + date)
                                .decoration(TextDecoration.ITALIC, false)
                ));
                meta.getPersistentDataContainer().set(Main.RUINED_CORE_KEY, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                drop.setItemMeta(meta);
            }

            block.setType(Material.AIR);
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
            Main.ruinedCores.remove(key);
            event.setCancelled(true);
            return;
        }

        Chunk chunk = block.getChunk();

        if (canBypassClaimProtection(player)) {
            return;
        }

        if (isTownCoreChunk(chunk)) {
            player.sendMessage("§cYou cannot break blocks in a Town Core chunk!");
            event.setCancelled(true);
            return;
        }

        if (isCreationChunk(chunk)) {
            if (block.getType() == Material.BEACON && isPendingTownCoreBlock(block)) {
                return;
            }

            if (isPendingTownCoreBaseBlock(block)) {
                event.setCancelled(true);
                player.sendMessage("§cYou cannot break the Town Core beacon base during town creation!");
                return;
            }

            event.setCancelled(true);
            player.sendMessage("§cThis chunk is locked during town creation!");
            return;
        }

        String townName = getChunkOwner(chunk);

        if (townName == null) {
            return;
        }

        if (!isMember(player, townName)) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot break blocks in §f" + townName + "§c's territory!");
            return;
        }

        if (!hasPlotAccess(player, block.getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cThis plot belongs to someone else!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();

        if (canBypassClaimProtection(player)) {
            return;
        }

        if (isTownCoreChunk(chunk)) {
            player.sendMessage("§cYou cannot place blocks in a Town Core chunk!");
            event.setCancelled(true);
            return;
        }

        if (isCreationChunk(chunk)) {
            event.setCancelled(true);
            player.sendMessage("§cThis chunk is locked during town creation!");
            return;
        }

        String townName = getChunkOwner(chunk);

        if (townName == null) {
            return;
        }

        if (!isMember(player, townName)) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot place blocks in §f" + townName + "§c's territory!");
            return;
        }

        if (!hasPlotAccess(player, block.getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cThis plot belongs to someone else!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }

        Block block = event.getClickedBlock();
        Material type = block.getType();
        Player player = event.getPlayer();

        // Priority: Town Core Beacon always opens Town GUI, even for OPs
        if (type == Material.BEACON) {
            if (isTownCoreBeacon(block)) {
                event.setCancelled(true);
                String coreTown = getTownCoreOwner(block);
                if (coreTown != null && (isMember(player, coreTown) || canBypassClaimProtection(player))) {
                    TownGUI.open(player);
                } else if (coreTown != null) {
                    player.sendMessage("§cThat's §f" + coreTown + "§c's Town Core — you can't use it.");
                }
                return;
            }

            if (isCreationChunk(block.getChunk())) {
                event.setCancelled(true);
                Main.PendingTown pending = getPendingTownInChunk(block.getChunk());
                if (pending != null) {
                    player.sendMessage("§e§lTown Core active! §r§eDefending §f" + pending.townName + "§e...");
                    player.sendMessage("§7You'll be able to use this once the town is set up.");
                }
                return;
            }
        }

        if (canBypassClaimProtection(player)) {
            return;
        }

        Chunk chunk = block.getChunk();
        String townName = getChunkOwner(chunk);

        if (townName == null) {
            return;
        }

        if (isMember(player, townName)) {
            if (!hasPlotAccess(player, block.getLocation())) {
                if (isInteractable(type)) {
                    event.setCancelled(true);
                    player.sendMessage("§cThis plot belongs to someone else!");
                }
            }
            return;
        }

        if (isAccessibleContainer(type)) {
            return;
        }

        if (isInteractable(type)) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot interact with §f" + townName + "§c's territory!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            return;
        }

        Player player = event.getPlayer();
        if (canBypassClaimProtection(player)) {
            return;
        }

        Location to = event.getTo();
        String townName = getChunkOwner(to.getChunk());

        if (townName != null && !isMember(player, townName)) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot enderpearl into or within §f" + townName + "§c's territory!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)) {
            return;
        }

        if (!(pearl.getShooter() instanceof Player player)) {
            return;
        }

        if (canBypassClaimProtection(player)) {
            return;
        }

        Chunk chunk = pearl.getLocation().getChunk();
        String townName = getChunkOwner(chunk);

        if (townName != null && !isMember(player, townName)) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot use enderpearls within §f" + townName + "§c's territory!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block ->
                getChunkOwner(block.getChunk()) != null || isCreationChunk(block.getChunk())
        );
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block ->
                getChunkOwner(block.getChunk()) != null || isCreationChunk(block.getChunk())
        );
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Monster)) {
            return;
        }

        Chunk chunk = event.getLocation().getChunk();
        String townName = getChunkOwner(chunk);

        if (townName == null) {
            return;
        }

        int level = Main.townLevel.getOrDefault(townName, 1);

        if (level < 5) {
            return;
        }

        boolean mobsEnabled = Main.townMobsEnabled.getOrDefault(townName, false);

        if (!mobsEnabled) {
            event.setCancelled(true);
        }
    }

    private boolean isAccessibleContainer(Material material) {
        return material == Material.CHEST ||
                material == Material.TRAPPED_CHEST ||
                material == Material.BARREL ||
                material == Material.ENDER_CHEST ||
                material == Material.SHULKER_BOX ||
                material.name().endsWith("_SHULKER_BOX");
    }

    private boolean isInteractable(Material material) {
        String name = material.name();

        return name.contains("DOOR") ||
                name.contains("GATE") ||
                name.contains("BUTTON") ||
                name.contains("LEVER") ||
                name.contains("TRAPDOOR") ||
                name.contains("ANVIL") ||
                material == Material.CRAFTING_TABLE ||
                material == Material.FURNACE ||
                material == Material.BLAST_FURNACE ||
                material == Material.SMOKER ||
                material == Material.ENCHANTING_TABLE ||
                material == Material.BREWING_STAND ||
                material == Material.BEACON ||
                material == Material.CAMPFIRE ||
                material == Material.SOUL_CAMPFIRE ||
                material == Material.LOOM ||
                material == Material.CARTOGRAPHY_TABLE ||
                material == Material.GRINDSTONE ||
                material == Material.STONECUTTER ||
                material == Material.COMPOSTER ||
                material == Material.LECTERN;
    }

    private boolean isTownCoreBeacon(Block block) {
        if (block == null || block.getType() != Material.BEACON) return false;

        org.bukkit.Location blockLoc = block.getLocation();

        for (org.bukkit.Location coreLoc : Main.townCoreLocation.values()) {
            if (Main.sameBlock(coreLoc, blockLoc)) {
                return true;
            }
        }

        return false;
    }

    private String getTownCoreOwner(Block block) {
        if (block == null) return null;

        org.bukkit.Location blockLoc = block.getLocation();

        for (java.util.Map.Entry<String, org.bukkit.Location> entry : Main.townCoreLocation.entrySet()) {
            if (Main.sameBlock(entry.getValue(), blockLoc)) {
                return entry.getKey();
            }
        }

        return null;
    }
}