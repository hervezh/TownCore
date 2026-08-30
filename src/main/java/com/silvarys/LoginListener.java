package com.silvarys;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.persistence.PersistentDataType;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class LoginListener implements Listener {

    private final JavaPlugin plugin;

    public LoginListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Main.playerLastOnline.put(uuid, System.currentTimeMillis());

        if (!player.getPersistentDataContainer().has(Main.GUIDE_BOOK_KEY, org.bukkit.persistence.PersistentDataType.BYTE)) {
            player.getInventory().addItem(TutorialManager.getGuideBook());
            player.getPersistentDataContainer().set(Main.GUIDE_BOOK_KEY, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            player.sendMessage("§aWelcome to Town Core! §7You've been given a §fStarter Guide§7.");
        }

        String townName = Main.playerTown.get(uuid);

        if (townName == null) {
            WarBossBarManager.showActiveBarsToPlayer(player);
            return;
        }

        sendTownLoginMessage(player, townName);
        WarBossBarManager.showActiveBarsToPlayer(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Main.playerLastOnline.put(uuid, System.currentTimeMillis());

        String townName = Main.playerTown.get(uuid);

        if (townName != null) {
            Main.logTownAction(townName, player.getName() + " logged out.");
        }

        StorageManager.saveData(plugin);
    }

    private void sendTownLoginMessage(Player joinedPlayer, String townName) {
        Set<UUID> members = Main.townMembers.getOrDefault(townName, Collections.emptySet());

        String role = Main.playerRole.getOrDefault(joinedPlayer.getUniqueId(), "member");
        String title = Main.playerTownTitle.getOrDefault(joinedPlayer.getUniqueId(), "");

        String titlePart = title == null || title.isBlank()
                ? ""
                : " §7[" + title + "§7]";

        String rolePart = switch (role.toLowerCase()) {
            case "ruler" -> "§6Ruler";
            case "assistant" -> "§eAssistant";
            default -> "§aMember";
        };

        String message = "§2§l[Town] §r§f" + joinedPlayer.getName()
                + titlePart
                + " §7has logged on. §8("
                + rolePart
                + "§8)";

        for (UUID memberUUID : members) {
            Player member = Bukkit.getPlayer(memberUUID);

            if (member == null || !member.isOnline()) {
                continue;
            }

            member.sendMessage(message);
            member.playSound(member.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.4f);
        }

        Main.logTownAction(townName, joinedPlayer.getName() + " logged in.");
    }

    private String getPlayerName(UUID uuid) {
        if (uuid == null) {
            return "Unknown";
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
    }
}