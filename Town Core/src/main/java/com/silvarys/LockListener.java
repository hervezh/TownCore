package com.silvarys;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class LockListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Material type = block.getType();

        boolean isDoor = isDoor(type);
        boolean isChest = isChest(type);

        if (!isDoor && !isChest) return;

        // Staff/OP bypass locked block access.
        // They can still shift-right-click to lock/unlock below.
        boolean canBypassLocks = LockManager.canBypassLocks(player);

        String townName = Main.playerTown.get(player.getUniqueId());

        if (townName == null && !canBypassLocks) {
            return;
        }

        // Check block is in player's town.
        // Staff/OP can manage locks even outside their own town.
        if (!canBypassLocks) {
            String chunkKey = Main.getChunkKey(block.getChunk());
            boolean inTown = Main.townChunks.getOrDefault(townName, new java.util.HashSet<>()).contains(chunkKey);

            if (!inTown) {
                return;
            }
        }

        // Shift click = lock/unlock toggle
        if (player.isSneaking()) {
            event.setCancelled(true);

            if (LockManager.isLocked(block.getLocation())) {
                String ownerUUIDStr = LockManager.getOwner(block.getLocation());
                String role = Main.playerRole.getOrDefault(player.getUniqueId(), "member");

                // Only owner, ruler, assistant, or staff bypass can unlock
                if (!canBypassLocks
                        && !player.getUniqueId().toString().equals(ownerUUIDStr)
                        && !role.equalsIgnoreCase("ruler")
                        && !role.equalsIgnoreCase("assistant")) {
                    player.sendMessage("§cYou cannot unlock this! It belongs to someone else.");
                    return;
                }

                LockManager.unlock(block.getLocation());

                player.sendActionBar(net.kyori.adventure.text.Component.text("Item is now unlocked!").color(net.kyori.adventure.text.format.TextColor.color(0x55FF55)));
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
                if (canBypassLocks && !player.getUniqueId().toString().equals(ownerUUIDStr)) {
                    player.sendMessage("§aBlock unlocked with staff bypass!");
                }
            } else {
                LockManager.lock(block.getLocation(), player.getUniqueId());
                player.sendActionBar(net.kyori.adventure.text.Component.text("Item is now locked!").color(net.kyori.adventure.text.format.TextColor.color(0x55FF55)));
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
            }

            return;
        }

        // Normal click — staff/OP bypass access checks
        if (canBypassLocks) {
            return;
        }

        if (LockManager.isLocked(block.getLocation())) {
            if (isDoor) {
                if (!LockManager.canOpenDoor(player, block.getLocation())) {
                    event.setCancelled(true);
                    player.sendMessage("§cThis door is locked!");
                }
            } else if (isChest) {
                if (!LockManager.canAccessChest(player, block.getLocation())) {
                    event.setCancelled(true);
                    String ownerUUIDStr = LockManager.getOwner(block.getLocation());
                    if (ownerUUIDStr != null) {
                        org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(ownerUUIDStr));
                        String ownerName = op.getName() != null ? op.getName() : "Unknown";
                        player.sendMessage("§cThis chest belongs to " + ownerName);
                    } else {
                        player.sendMessage("§cThis chest is locked!");
                    }
                }
            }
        }
    }

    // Remove lock when block is broken
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!LockManager.isLocked(block.getLocation())) {
            return;
        }

        if (LockManager.canBypassLocks(player)) {
            LockManager.unlock(block.getLocation());
            player.sendMessage("§aLocked block removed with staff bypass.");
            return;
        }

        String ownerUUIDStr = LockManager.getOwner(block.getLocation());
        String role = Main.playerRole.getOrDefault(player.getUniqueId(), "member");

        boolean canManage = player.getUniqueId().toString().equals(ownerUUIDStr) 
            || role.equalsIgnoreCase("ruler") 
            || role.equalsIgnoreCase("assistant");

        if (!canManage) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot break this locked block! It belongs to someone else.");
            return;
        }

        LockManager.unlock(block.getLocation());
    }

    private boolean isDoor(Material m) {
        String name = m.name();
        return name.contains("DOOR") || name.contains("GATE") || name.contains("TRAPDOOR");
    }

    private boolean isChest(Material m) {
        return m == Material.CHEST ||
                m == Material.TRAPPED_CHEST ||
                m == Material.BARREL ||
                m == Material.SHULKER_BOX ||
                m.name().endsWith("_SHULKER_BOX");
    }
}