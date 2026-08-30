package com.silvarys;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener implements Listener {

    public static Set<UUID> townChatPlayers = new HashSet<>();
    public static Set<UUID> allyChatPlayers = new HashSet<>();

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    @EventHandler(priority = EventPriority.LOWEST)
    public void onGlobalChatFormat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        String townName = Main.playerTown.get(player.getUniqueId());
        
        if (townName != null && !townChatPlayers.contains(player.getUniqueId()) && !allyChatPlayers.contains(player.getUniqueId())) {
            String color;
            if (WarManager.isOccupied(townName)) {
                color = "§8"; // Dark Gray for occupied towns
            } else {
                color = translateColors(Main.townTagColor.getOrDefault(townName, "§6"));
            }
            String tag = "§7[" + color + townName + "§7] ";
            event.setFormat(tag + event.getFormat());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String message = event.getMessage();
        String townName = Main.playerTown.get(uuid);

        if (townChatPlayers.contains(uuid)) {
            event.setCancelled(true);

            if (townName == null) {
                townChatPlayers.remove(uuid);
                player.sendMessage("§cYou are not in a town! Switched back to global chat.");
                return;
            }

            String color = translateColors(Main.townTagColor.getOrDefault(townName, "§6"));
            String format = color + "[" + townName + " Chat] "
                    + getTitlePrefix(uuid)
                    + "§f" + player.getName()
                    + "§7: §f" + message;

            for (UUID memberUUID : Main.townMembers.getOrDefault(townName, new HashSet<>())) {
                Player member = Bukkit.getPlayer(memberUUID);

                if (member != null && member.isOnline()) {
                    member.sendMessage(format);
                }
            }

            return;
        }

        if (allyChatPlayers.contains(uuid)) {
            event.setCancelled(true);

            if (townName == null) {
                allyChatPlayers.remove(uuid);
                player.sendMessage("§cYou are not in a town! Switched back to global chat.");
                return;
            }

            String color = translateColors(Main.townTagColor.getOrDefault(townName, "§6"));
            String format = color + "[Ally Chat] "
                    + getTitlePrefix(uuid)
                    + "§f" + player.getName()
                    + " §7(" + townName + ")"
                    + "§7: §f" + message;

            for (UUID memberUUID : Main.townMembers.getOrDefault(townName, new HashSet<>())) {
                Player member = Bukkit.getPlayer(memberUUID);

                if (member != null && member.isOnline()) {
                    member.sendMessage(format);
                }
            }

            for (String allyTown : Main.townAllies.getOrDefault(townName, new HashSet<>())) {
                for (UUID memberUUID : Main.townMembers.getOrDefault(allyTown, new HashSet<>())) {
                    Player member = Bukkit.getPlayer(memberUUID);

                    if (member != null
                            && member.isOnline()
                            && !Main.playerTown.getOrDefault(member.getUniqueId(), "").equals(townName)) {
                        member.sendMessage(format);
                    }
                }
            }
        }
    }

    private String getTitlePrefix(UUID uuid) {
        String title = Main.playerTownTitle.getOrDefault(uuid, "");

        if (title == null || title.isBlank()) {
            return "";
        }

        return "§7[" + translateColors(title) + "§7] ";
    }

    public static String translateColors(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String translated = input;

        Matcher matcher = HEX_PATTERN.matcher(translated);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");

            for (char c : hex.toCharArray()) {
                replacement.append("§").append(c);
            }

            matcher.appendReplacement(buffer, replacement.toString());
        }

        matcher.appendTail(buffer);
        translated = buffer.toString();

        return translated.replace("&", "§");
    }

    public static String stripColorCodes(String input) {
        if (input == null) {
            return "";
        }

        return input
                .replaceAll("&#[A-Fa-f0-9]{6}", "")
                .replaceAll("§x(§[A-Fa-f0-9]){6}", "")
                .replaceAll("[§&][0-9A-FK-ORa-fk-or]", "");
    }
}