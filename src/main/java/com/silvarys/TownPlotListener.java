package com.silvarys;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TownPlotListener implements Listener {

    private final Map<UUID, String> playerCurrentPlot = new HashMap<>();

    @EventHandler
    public void onPlotEntry(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        Location to = event.getTo();
        String chunkKey = Main.getChunkKey(to.getChunk());

        if (!Main.subdividedChunks.containsKey(chunkKey)) {
            if (playerCurrentPlot.containsKey(player.getUniqueId())) {
                playerCurrentPlot.remove(player.getUniqueId());
            }
            return;
        }

        int type = Main.subdividedChunks.get(chunkKey);
        int index = Main.getPlotIndex(to, type);
        String plotKey = chunkKey + ":" + index;

        String lastPlot = playerCurrentPlot.get(player.getUniqueId());
        if (plotKey.equals(lastPlot)) return;

        playerCurrentPlot.put(player.getUniqueId(), plotKey);
        Main.Plot plot = Main.townPlots.get(plotKey);

        if (plot == null) {
            // Uninitialized plot (default free)
            sendPlotMessage(player, "Unowned Plot", 0);
        } else {
            if (plot.owner == null) {
                sendPlotMessage(player, "Plot for Sale", plot.price);
            } else {
                sendPlotOwnerMessage(player, Main.getPlotOwnerName(plot));
            }
        }
    }

    private void sendPlotMessage(Player player, String title, double price) {
        Component message = Component.text(title + " ")
                .color(TextColor.color(0xFFFF55))
                .append(Component.text("($" + String.format("%.2f", price) + ")")
                        .color(TextColor.color(0x55FF55)));
        player.sendActionBar(message);
    }

    private void sendPlotOwnerMessage(Player player, String ownerName) {
        Component message = Component.text("Entering ")
                .color(TextColor.color(0xAAAAAA))
                .append(Component.text(ownerName + "'s Plot")
                        .color(TextColor.color(0x55FFFF))
                        .decoration(TextDecoration.BOLD, true));
        player.sendActionBar(message);
    }
}
