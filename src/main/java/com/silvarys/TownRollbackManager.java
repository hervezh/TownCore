package com.silvarys;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

public class TownRollbackManager {

    private static final long MAX_ROLLBACK_HISTORY_MS = 24L * 60 * 60 * 1000L;

    private static final List<RollbackEntry> rollbackHistory = new ArrayList<>();

    private static boolean rollbackInProgress = false;

    private enum ChangeType {
        BLOCK_BREAK,
        BLOCK_PLACE
    }

    private static class RollbackEntry {
        String townName;
        UUID playerUUID;
        String playerName;
        long timestamp;
        ChangeType changeType;

        String worldName;
        int x;
        int y;
        int z;

        BlockState oldState;
        ItemStack[] oldContainerContents;

        RollbackEntry(
                String townName,
                UUID playerUUID,
                String playerName,
                long timestamp,
                ChangeType changeType,
                Location location,
                BlockState oldState,
                ItemStack[] oldContainerContents
        ) {
            this.townName = townName;
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.timestamp = timestamp;
            this.changeType = changeType;

            this.worldName = location.getWorld() == null ? null : location.getWorld().getName();
            this.x = location.getBlockX();
            this.y = location.getBlockY();
            this.z = location.getBlockZ();

            this.oldState = oldState;
            this.oldContainerContents = oldContainerContents;
        }

        Location getLocation() {
            if (worldName == null) return null;

            World world = Bukkit.getWorld(worldName);

            if (world == null) return null;

            return new Location(world, x, y, z);
        }
    }

    public static boolean isRollbackInProgress() {
        return rollbackInProgress;
    }

    public static void recordBlockBreak(String townName, Player player, Block block) {
        if (rollbackInProgress) return;
        if (townName == null || player == null || block == null) return;
        if (block.getType() == Material.AIR) return;

        BlockState oldState = block.getState();
        ItemStack[] oldContents = getContainerContents(oldState);

        rollbackHistory.add(new RollbackEntry(
                townName,
                player.getUniqueId(),
                player.getName(),
                System.currentTimeMillis(),
                ChangeType.BLOCK_BREAK,
                block.getLocation(),
                oldState,
                oldContents
        ));

        cleanupOldHistory();
    }

    public static void recordBlockPlace(String townName, Player player, Block block) {
        if (rollbackInProgress) return;
        if (townName == null || player == null || block == null) return;
        if (block.getType() == Material.AIR) return;

        rollbackHistory.add(new RollbackEntry(
                townName,
                player.getUniqueId(),
                player.getName(),
                System.currentTimeMillis(),
                ChangeType.BLOCK_PLACE,
                block.getLocation(),
                null,
                null
        ));

        cleanupOldHistory();
    }

    public static int rollbackTown(String townName, int minutes) {
        if (townName == null || minutes <= 0) return 0;

        long cutoff = System.currentTimeMillis() - (minutes * 60L * 1000L);
        int rolledBack = 0;

        rollbackInProgress = true;

        try {
            ListIterator<RollbackEntry> iterator = rollbackHistory.listIterator(rollbackHistory.size());

            while (iterator.hasPrevious()) {
                RollbackEntry entry = iterator.previous();

                if (!entry.townName.equalsIgnoreCase(townName)) continue;
                if (entry.timestamp < cutoff) continue;

                if (rollbackEntry(entry)) {
                    rolledBack++;
                    iterator.remove();
                }
            }
        } finally {
            rollbackInProgress = false;
        }

        return rolledBack;
    }

    public static int rollbackPlayer(String playerName, int minutes) {
        if (playerName == null || minutes <= 0) return 0;

        long cutoff = System.currentTimeMillis() - (minutes * 60L * 1000L);
        int rolledBack = 0;

        rollbackInProgress = true;

        try {
            ListIterator<RollbackEntry> iterator = rollbackHistory.listIterator(rollbackHistory.size());

            while (iterator.hasPrevious()) {
                RollbackEntry entry = iterator.previous();

                if (entry.playerName == null || !entry.playerName.equalsIgnoreCase(playerName)) continue;
                if (entry.timestamp < cutoff) continue;

                if (rollbackEntry(entry)) {
                    rolledBack++;
                    iterator.remove();
                }
            }
        } finally {
            rollbackInProgress = false;
        }

        return rolledBack;
    }

    public static int getHistorySize() {
        cleanupOldHistory();
        return rollbackHistory.size();
    }

    public static int getTownHistorySize(String townName) {
        if (townName == null) return 0;

        cleanupOldHistory();

        int count = 0;

        for (RollbackEntry entry : rollbackHistory) {
            if (entry.townName.equalsIgnoreCase(townName)) {
                count++;
            }
        }

        return count;
    }

    private static boolean rollbackEntry(RollbackEntry entry) {
        Location location = entry.getLocation();

        if (location == null || location.getWorld() == null) {
            return false;
        }

        Block block = location.getBlock();

        if (entry.changeType == ChangeType.BLOCK_BREAK) {
            restoreBrokenBlock(block, entry);
            return true;
        }

        if (entry.changeType == ChangeType.BLOCK_PLACE) {
            removePlacedBlock(block);
            return true;
        }

        return false;
    }

    private static void restoreBrokenBlock(Block block, RollbackEntry entry) {
        if (entry.oldState == null) return;

        entry.oldState.update(true, false);

        if (entry.oldContainerContents != null) {
            BlockState state = block.getState();

            if (state instanceof Container container) {
                Inventory inventory = container.getSnapshotInventory();
                inventory.setContents(copyContents(entry.oldContainerContents));
                container.update(true, false);
            }
        }
    }

    private static void removePlacedBlock(Block block) {
        if (block == null || block.getType() == Material.AIR) return;

        BlockState state = block.getState();

        if (state instanceof Container container) {
            Inventory inventory = container.getSnapshotInventory();

            for (ItemStack item : inventory.getContents()) {
                if (item == null || item.getType() == Material.AIR) continue;

                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), item.clone());
            }

            inventory.clear();
            container.update(true, false);
        }

        block.setType(Material.AIR, false);
    }

    private static ItemStack[] getContainerContents(BlockState state) {
        if (!(state instanceof Container container)) {
            return null;
        }

        return copyContents(container.getSnapshotInventory().getContents());
    }

    private static ItemStack[] copyContents(ItemStack[] contents) {
        if (contents == null) return null;

        ItemStack[] copy = new ItemStack[contents.length];

        for (int i = 0; i < contents.length; i++) {
            copy[i] = contents[i] == null ? null : contents[i].clone();
        }

        return copy;
    }

    private static void cleanupOldHistory() {
        long cutoff = System.currentTimeMillis() - MAX_ROLLBACK_HISTORY_MS;

        Iterator<RollbackEntry> iterator = rollbackHistory.iterator();

        while (iterator.hasNext()) {
            RollbackEntry entry = iterator.next();

            if (entry.timestamp < cutoff) {
                iterator.remove();
            }
        }
    }
}