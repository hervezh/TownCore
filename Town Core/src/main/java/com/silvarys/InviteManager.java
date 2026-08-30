package com.silvarys;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InviteManager {

    private static final long INVITE_EXPIRATION_MS = 5L * 60L * 1000L;
    private static final long INVITE_COOLDOWN_MS = 30L * 1000L;

    // Stores pending invites: target UUID -> town name
    private static final Map<UUID, String> pendingInvites = new HashMap<>();
    private static final Map<UUID, String> inviterName = new HashMap<>();
    private static final Map<UUID, Long> inviteCreatedAt = new HashMap<>();

    // Stores invite command cooldowns: inviter UUID -> last invite time
    private static final Map<UUID, Long> inviteCooldowns = new HashMap<>();

    public static boolean isOnInviteCooldown(Player inviter) {
        if (inviter.hasPermission("silvarys.town.invitecooldown.bypass") || inviter.hasPermission("silvarys.staff")) {
            return false;
        }

        UUID inviterUUID = inviter.getUniqueId();
        long now = System.currentTimeMillis();
        long lastInvite = inviteCooldowns.getOrDefault(inviterUUID, 0L);
        long timePassed = now - lastInvite;

        if (timePassed < INVITE_COOLDOWN_MS) {
            long timeLeft = INVITE_COOLDOWN_MS - timePassed;
            long secondsLeft = Math.max(1L, (long) Math.ceil(timeLeft / 1000.0));

            inviter.sendMessage("§cPlease wait §e" + secondsLeft + "s §cbefore sending another town invite.");
            return true;
        }

        return false;
    }

    public static void addInvite(UUID target, String townName, String inviter) {
        pendingInvites.put(target, townName);
        inviterName.put(target, inviter);
        inviteCreatedAt.put(target, System.currentTimeMillis());

        Player inviterPlayer = Bukkit.getPlayer(inviter);

        if (inviterPlayer != null) {
            inviteCooldowns.put(inviterPlayer.getUniqueId(), System.currentTimeMillis());
        }

        saveTownData();
    }

    public static boolean hasPendingInvite(UUID target) {
        if (!pendingInvites.containsKey(target)) {
            return false;
        }

        if (isInviteExpired(target)) {
            String expiredTown = pendingInvites.get(target);
            String invitedName = Bukkit.getOfflinePlayer(target).getName();

            removeInvite(target);

            Main.logTownAction(expiredTown, "Invite for " + (invitedName != null ? invitedName : target.toString()) + " expired.");
            saveTownData();

            return false;
        }

        return true;
    }

    public static String getPendingInviteTown(UUID target) {
        if (!hasPendingInvite(target)) {
            return null;
        }

        return pendingInvites.get(target);
    }

    public static String getInviterName(UUID target) {
        if (!hasPendingInvite(target)) {
            return "Unknown";
        }

        return inviterName.getOrDefault(target, "Unknown");
    }

    public static void showInvites(Player player) {
        UUID uuid = player.getUniqueId();

        if (!pendingInvites.containsKey(uuid)) {
            player.sendMessage("§7You do not have any pending town invites.");
            return;
        }

        if (isInviteExpired(uuid)) {
            String expiredTown = pendingInvites.get(uuid);
            removeInvite(uuid);

            Main.logTownAction(expiredTown, "Invite for " + player.getName() + " expired.");
            saveTownData();

            player.sendMessage("§cYour invite to §f" + expiredTown + " §chas expired.");
            return;
        }

        String townName = pendingInvites.get(uuid);
        String inviter = inviterName.getOrDefault(uuid, "Unknown");

        player.sendMessage("§6§l--- Pending Town Invite ---");
        player.sendMessage("§eTown: §f" + townName);
        player.sendMessage("§eInvited by: §f" + inviter);
        player.sendMessage("§eExpires in: §f" + getRemainingTime(uuid));
        player.sendMessage("§aUse §f/town accept §ato join.");
        player.sendMessage("§cUse §f/town deny §cto decline.");
    }

    public static void acceptInvite(Player player) {
        UUID uuid = player.getUniqueId();

        if (!pendingInvites.containsKey(uuid)) {
            player.sendMessage("§cYou don't have a pending invite!");
            return;
        }

        if (isInviteExpired(uuid)) {
            String expiredTown = pendingInvites.get(uuid);
            removeInvite(uuid);

            Main.logTownAction(expiredTown, "Invite for " + player.getName() + " expired.");
            saveTownData();

            player.sendMessage("§cYour invite to §f" + expiredTown + " §chas expired.");
            return;
        }

        String townName = pendingInvites.get(uuid);

        if (!Main.townLevel.containsKey(townName)) {
            removeInvite(uuid);
            saveTownData();

            player.sendMessage("§cThat town no longer exists!");
            return;
        }

        if (Main.playerTown.containsKey(uuid)) {
            removeInvite(uuid);
            saveTownData();

            player.sendMessage("§cYou are already in a town!");
            return;
        }

        removeInvite(uuid);

        Main.playerTown.put(uuid, townName);
        Main.playerRole.put(uuid, "member");
        Main.townMembers.putIfAbsent(townName, new java.util.HashSet<>());
        Main.townMembers.get(townName).add(uuid);

        Main.logTownAction(townName, player.getName() + " accepted a town invite and joined the town.");
        saveTownData();

        player.sendMessage("§aYou have joined §f" + townName + "§a!");

        for (UUID memberUUID : Main.townMembers.getOrDefault(townName, new java.util.HashSet<>())) {
            Player member = Bukkit.getPlayer(memberUUID);

            if (member != null && !member.equals(player)) {
                member.sendMessage("§a" + player.getName() + " §7has joined §f" + townName + "§7!");
            }
        }
    }

    public static void denyInvite(Player player) {
        UUID uuid = player.getUniqueId();

        if (!pendingInvites.containsKey(uuid)) {
            player.sendMessage("§cYou don't have a pending invite!");
            return;
        }

        String townName = pendingInvites.get(uuid);
        removeInvite(uuid);

        Main.logTownAction(townName, player.getName() + " declined a town invite.");
        saveTownData();

        player.sendMessage("§cYou declined the invite to §f" + townName + "§c!");
    }

    private static boolean isInviteExpired(UUID uuid) {
        Long createdAt = inviteCreatedAt.get(uuid);

        if (createdAt == null) {
            return true;
        }

        return System.currentTimeMillis() - createdAt >= INVITE_EXPIRATION_MS;
    }

    private static String getRemainingTime(UUID uuid) {
        Long createdAt = inviteCreatedAt.get(uuid);

        if (createdAt == null) {
            return "Expired";
        }

        long elapsed = System.currentTimeMillis() - createdAt;
        long remaining = Math.max(0, INVITE_EXPIRATION_MS - elapsed);

        long seconds = remaining / 1000L;
        long minutes = seconds / 60L;
        long leftoverSeconds = seconds % 60L;

        return minutes + "m " + leftoverSeconds + "s";
    }

    private static void removeInvite(UUID uuid) {
        pendingInvites.remove(uuid);
        inviterName.remove(uuid);
        inviteCreatedAt.remove(uuid);
    }

    private static void saveTownData() {
        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(InviteManager.class);
        StorageManager.saveData(plugin);
    }
}