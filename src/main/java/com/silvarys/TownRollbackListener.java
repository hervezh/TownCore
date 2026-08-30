package com.silvarys;

import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Map;
import java.util.Set;

public class TownRollbackListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (TownRollbackManager.isRollbackInProgress()) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();

        String townName = getTownAtChunk(block.getChunk());

        if (townName == null) return;

        TownRollbackManager.recordBlockBreak(townName, player, block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (TownRollbackManager.isRollbackInProgress()) return;

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        String townName = getTownAtChunk(block.getChunk());

        if (townName == null) return;

        TownRollbackManager.recordBlockPlace(townName, player, block);
    }

    private String getTownAtChunk(Chunk chunk) {
        String chunkKey = Main.getChunkKey(chunk);

        for (Map.Entry<String, Set<String>> entry : Main.townChunks.entrySet()) {
            if (entry.getValue().contains(chunkKey)) {
                return entry.getKey();
            }
        }

        return null;
    }
}