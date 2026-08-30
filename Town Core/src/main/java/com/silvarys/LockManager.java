package com.silvarys;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LockManager {

    // Stores locked block location key -> owner UUID string
    // Format: "world:x:y:z" -> "uuid"
    public static Map<String, String> lockedBlockOwners = new HashMap<>();

    public static String getBlockKey(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return "unknown";
        }

        return loc.getWorld().getName()
                + ":" + loc.getBlockX()
                + ":" + loc.getBlockY()
                + ":" + loc.getBlockZ();
    }

    public static boolean isLocked(Location loc) {
        return lockedBlockOwners.containsKey(getBlockKey(loc));
    }

    public static String getOwner(Location loc) {
        return lockedBlockOwners.get(getBlockKey(loc));
    }

    public static void lock(Location loc, UUID owner) {
        if (loc == null || owner == null) {
            return;
        }

        lockedBlockOwners.put(getBlockKey(loc), owner.toString());
    }

    public static void unlock(Location loc) {
        lockedBlockOwners.remove(getBlockKey(loc));
    }

    public static boolean canBypassLocks(Player player) {
        if (player == null) {
            return false;
        }

        return player.isOp()
                || player.hasPermission("silvarys.staff")
                || player.hasPermission("silvarys.lock.bypass")
                || player.hasPermission("silvarys.town.lock.bypass");
    }

    public static boolean canAccessChest(Player player, Location loc) {
        if (player == null || loc == null) {
            return false;
        }

        if (canBypassLocks(player)) {
            return true;
        }

        String key = getBlockKey(loc);

        if (!lockedBlockOwners.containsKey(key)) {
            return true;
        }

        String ownerUUIDStr = lockedBlockOwners.get(key);

        // Owner of the lock can access
        if (player.getUniqueId().toString().equals(ownerUUIDStr)) {
            return true;
        }

        // People outside the locker's town can access chests (if they bypass claim protection)
        UUID ownerUUID = UUID.fromString(ownerUUIDStr);
        String ownerTown = Main.playerTown.get(ownerUUID);
        String playerTown = Main.playerTown.get(player.getUniqueId());

        if (ownerTown != null && !ownerTown.equals(playerTown)) {
            return true;
        }

        return false;
    }

    public static boolean canOpenDoor(Player player, Location loc) {
        if (player == null || loc == null) {
            return false;
        }

        if (canBypassLocks(player)) {
            return true;
        }

        String key = getBlockKey(loc);

        if (!lockedBlockOwners.containsKey(key)) {
            return true;
        }

        // NOBODY can open a locked door till it's unlocked
        return false;
    }
}