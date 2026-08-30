package com.silvarys;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChunkEnterListener implements Listener {

    private static final Map<UUID, String> lastChunkTown = new HashMap<>();
    private static final Map<UUID, Long> lastTitleTime = new HashMap<>();

    private static final long TITLE_COOLDOWN_MS = 2500L;

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;

        Player player = event.getPlayer();
        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = Main.getChunkKey(chunk);

        String townInChunk = getTownAtChunk(chunkKey);
        String lastTown = lastChunkTown.get(player.getUniqueId());

        if (townInChunk != null && !townInChunk.equals(lastTown)) {
            lastChunkTown.put(player.getUniqueId(), townInChunk);
            sendTownEntryMessage(player, townInChunk);
            return;
        }

        if (townInChunk == null && lastTown != null) {
            lastChunkTown.remove(player.getUniqueId());
            sendWildernessMessage(player);
        }
    }

    private void sendTownEntryMessage(Player player, String townName) {
        String playerTown = Main.playerTown.get(player.getUniqueId());
        RelationInfo relation = getRelationInfo(playerTown, townName);
        String rulerName = getRulerName(townName);

        Component title = Component.text("Entering " + townName)
                .color(relation.color)
                .decoration(TextDecoration.BOLD, true);

        Component subtitle = Component.text("Ruler: ")
                .color(TextColor.color(0xAAAAAA))
                .append(Component.text(rulerName)
                        .color(TextColor.color(0xFFFFFF)))
                .append(Component.text(" • Relation: ")
                        .color(TextColor.color(0xAAAAAA)))
                .append(Component.text(relation.name)
                        .color(relation.color));

        sendTitleIfReady(player, title, subtitle);

        player.sendActionBar(
                Component.text("⚑ " + townName + " ")
                        .color(relation.color)
                        .decoration(TextDecoration.BOLD, true)
                        .append(Component.text("• Ruler: ")
                                .color(TextColor.color(0xAAAAAA))
                                .decoration(TextDecoration.BOLD, false))
                        .append(Component.text(rulerName)
                                .color(TextColor.color(0xFFFFFF))
                                .decoration(TextDecoration.BOLD, false))
                        .append(Component.text(" • " + relation.name)
                                .color(relation.color)
                                .decoration(TextDecoration.BOLD, false))
        );

        player.playSound(player.getLocation(), relation.sound, 0.35f, relation.pitch);
    }

    private void sendWildernessMessage(Player player) {
        Component title = Component.text("Entering Wilderness")
                .color(TextColor.color(0xAAAAAA))
                .decoration(TextDecoration.BOLD, true);

        Component subtitle = Component.text("Unclaimed land")
                .color(TextColor.color(0xDDDDDD));

        sendTitleIfReady(player, title, subtitle);

        player.sendActionBar(Component.text("Wilderness")
                .color(TextColor.color(0xAAAAAA)));

        player.playSound(player.getLocation(), Sound.BLOCK_GRASS_STEP, 0.25f, 1.0f);
    }

    private void sendTitleIfReady(Player player, Component title, Component subtitle) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = lastTitleTime.getOrDefault(uuid, 0L);

        if (now - last < TITLE_COOLDOWN_MS) {
            return;
        }

        lastTitleTime.put(uuid, now);

        player.showTitle(Title.title(
                title,
                subtitle,
                Title.Times.times(
                        Duration.ofMillis(250),
                        Duration.ofMillis(1400),
                        Duration.ofMillis(500)
                )
        ));
    }

    private RelationInfo getRelationInfo(String playerTown, String enteredTown) {
        if (playerTown == null) {
            return new RelationInfo("Neutral", TextColor.color(0xFFD700), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f);
        }

        if (playerTown.equalsIgnoreCase(enteredTown)) {
            return new RelationInfo("Home", TextColor.color(0x52B788), Sound.ENTITY_PLAYER_LEVELUP, 1.4f);
        }

        if (Main.townWars.getOrDefault(playerTown, Collections.emptySet()).contains(enteredTown)) {
            return new RelationInfo("At War", TextColor.color(0x8B0000), Sound.ENTITY_WITHER_SPAWN, 0.8f);
        }

        if (Main.townAllies.getOrDefault(playerTown, Collections.emptySet()).contains(enteredTown)) {
            return new RelationInfo("Ally", TextColor.color(0x48CAE4), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f);
        }

        if (Main.townEnemies.getOrDefault(playerTown, Collections.emptySet()).contains(enteredTown)) {
            return new RelationInfo("Enemy", TextColor.color(0xE63946), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f);
        }

        if (Main.townEnemies.getOrDefault(enteredTown, Collections.emptySet()).contains(playerTown)) {
            return new RelationInfo("Hostile", TextColor.color(0xFF6B35), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f);
        }

        return new RelationInfo("Neutral", TextColor.color(0xFFD700), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f);
    }

    private String getTownAtChunk(String chunkKey) {
        for (Map.Entry<String, java.util.Set<String>> entry : Main.townChunks.entrySet()) {
            if (entry.getValue().contains(chunkKey)) {
                return entry.getKey();
            }
        }

        return null;
    }

    private String getRulerName(String townName) {
        UUID rulerUUID = Main.townOwner.get(townName);

        if (rulerUUID == null) {
            return "Unknown";
        }

        String name = Bukkit.getOfflinePlayer(rulerUUID).getName();

        return name != null ? name : "Unknown";
    }

    private static class RelationInfo {
        private final String name;
        private final TextColor color;
        private final Sound sound;
        private final float pitch;

        private RelationInfo(String name, TextColor color, Sound sound, float pitch) {
            this.name = name;
            this.color = color;
            this.sound = sound;
            this.pitch = pitch;
        }
    }
}