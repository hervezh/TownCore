package com.silvarys;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TownPlaceholderExpansion extends PlaceholderExpansion {

    @Override
    public String getIdentifier() {
        return "silvarys";
    }

    @Override
    public String getAuthor() {
        return "Town Core";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params) {
        if (offlinePlayer == null || params == null) return "";

        UUID uuid = offlinePlayer.getUniqueId();
        String townName = Main.playerTown.get(uuid);
        String placeholder = params.toLowerCase();

        switch (placeholder) {
            case "town":
                return townName != null ? townName : "No Town";

            case "town_raw":
                return townName != null ? townName : "";

            case "has_town":
                return townName != null ? "Yes" : "No";

            case "role":
                if (townName == null) return "No Town";
                return formatRole(Main.playerRole.getOrDefault(uuid, "member"));

            case "role_raw":
                if (townName == null) return "";
                return Main.playerRole.getOrDefault(uuid, "member").toLowerCase();

            case "title": {
                if (townName == null) return "";
                String title = Main.playerTownTitle.getOrDefault(uuid, "");
                return title == null || title.isBlank() ? "No Title" : ChatListener.translateColors(title);
            }

            case "title_raw": {
                if (townName == null) return "";
                return Main.playerTownTitle.getOrDefault(uuid, "");
            }

            case "title_formatted": {
                if (townName == null) return "";
                String title = Main.playerTownTitle.getOrDefault(uuid, "");
                if (title == null || title.isBlank()) return "";
                return "§7[" + ChatListener.translateColors(title) + "§7]";
            }

            case "last_online":
                return Main.formatLastOnline(uuid);

            case "town_level":
                if (townName == null) return "0";
                return String.valueOf(Main.townLevel.getOrDefault(townName, 1));

            case "town_bank":
                if (townName == null) return "0";
                return String.format("%.2f", Main.townBank.getOrDefault(townName, 0.0));

            case "town_claims":
                if (townName == null) return "0";
                return String.valueOf(Main.townChunks.getOrDefault(townName, Collections.emptySet()).size());

            case "town_max_claims":
                if (townName == null) return "0";
                return String.valueOf(Main.getMaxChunks(Main.townLevel.getOrDefault(townName, 1)));

            case "town_claims_formatted": {
                if (townName == null) return "0/0";
                int level = Main.townLevel.getOrDefault(townName, 1);
                int claims = Main.townChunks.getOrDefault(townName, Collections.emptySet()).size();
                int maxClaims = Main.getMaxChunks(level);
                return claims + "/" + maxClaims;
            }

            case "town_members":
                if (townName == null) return "0";
                return String.valueOf(Main.townMembers.getOrDefault(townName, Collections.emptySet()).size());

            case "town_online":
                if (townName == null) return "0";
                return String.valueOf(getOnlineTownMembers(townName));

            case "town_owner":
                if (townName == null) return "";
                return getTownOwnerName(townName);

            case "town_assistant":
                if (townName == null) return "";
                return getTownAssistantName(townName);

            case "town_allies_count":
                if (townName == null) return "0";
                return String.valueOf(Main.townAllies.getOrDefault(townName, Collections.emptySet()).size());

            case "town_enemies_count":
                if (townName == null) return "0";
                return String.valueOf(Main.townEnemies.getOrDefault(townName, Collections.emptySet()).size());

            case "town_wars_count":
                if (townName == null) return "0";
                return String.valueOf(Main.townWars.getOrDefault(townName, Collections.emptySet()).size());

            case "town_relation_counts": {
                if (townName == null) return "A:0 E:0 W:0";
                int allies = Main.townAllies.getOrDefault(townName, Collections.emptySet()).size();
                int enemies = Main.townEnemies.getOrDefault(townName, Collections.emptySet()).size();
                int wars = Main.townWars.getOrDefault(townName, Collections.emptySet()).size();
                return "A:" + allies + " E:" + enemies + " W:" + wars;
            }

            case "town_allies":
                if (townName == null) return "None";
                return formatTownSet(Main.townAllies.getOrDefault(townName, Collections.emptySet()));

            case "town_enemies":
                if (townName == null) return "None";
                return formatTownSet(Main.townEnemies.getOrDefault(townName, Collections.emptySet()));

            case "town_wars":
                if (townName == null) return "None";
                return formatTownSet(Main.townWars.getOrDefault(townName, Collections.emptySet()));

            case "town_mobs": {
                if (townName == null) return "";
                int townLevel = Main.townLevel.getOrDefault(townName, 1);
                if (townLevel < 5) return "Locked";
                boolean mobsEnabled = Main.townMobsEnabled.getOrDefault(townName, true);
                return mobsEnabled ? "Enabled" : "Disabled";
            }

            case "tab_prefix": {
                if (townName == null) return "§7[No Town]";

                String role = Main.playerRole.getOrDefault(uuid, "member").toLowerCase();
                String title = Main.playerTownTitle.getOrDefault(uuid, "");
                String titlePart = title == null || title.isBlank()
                        ? ""
                        : " §7[" + ChatListener.translateColors(title) + "§7]";

                switch (role) {
                    case "ruler":
                        return "§6[♛ " + townName + "]" + titlePart;
                    case "assistant":
                        return "§e[✦ " + townName + "]" + titlePart;
                    default:
                        return "§a[" + townName + "]" + titlePart;
                }
            }

            case "tab_suffix": {
                if (townName == null) return "";
                int suffixLevel = Main.townLevel.getOrDefault(townName, 1);
                return "§7Lv." + suffixLevel;
            }

            case "territory": {
                if (!(offlinePlayer instanceof Player onlinePlayer)) return "Wilderness";
                String chunkKey = Main.getChunkKey(onlinePlayer.getLocation().getChunk());

                for (Map.Entry<String, Set<String>> entry : Main.townChunks.entrySet()) {
                    if (entry.getValue().contains(chunkKey)) {
                        return entry.getKey();
                    }
                }

                return "Wilderness";
            }

            case "territory_relation": {
                if (!(offlinePlayer instanceof Player onlinePlayer)) return "Wilderness";

                String chunkKey = Main.getChunkKey(onlinePlayer.getLocation().getChunk());
                String territoryTown = null;

                for (Map.Entry<String, Set<String>> entry : Main.townChunks.entrySet()) {
                    if (entry.getValue().contains(chunkKey)) {
                        territoryTown = entry.getKey();
                        break;
                    }
                }

                if (territoryTown == null) return "§7Wilderness";
                if (townName == null) return "§fNeutral";
                if (territoryTown.equals(townName)) return "§aYour Town";
                if (Main.townAllies.getOrDefault(townName, Collections.emptySet()).contains(territoryTown)) return "§bAlly";
                if (Main.townEnemies.getOrDefault(townName, Collections.emptySet()).contains(territoryTown)) return "§cEnemy";
                if (Main.townWars.getOrDefault(townName, Collections.emptySet()).contains(territoryTown)) return "§4At War";
                return "§fNeutral";
            }

            case "in_war_session": {
                if (townName == null) return "false";
                WarManager.War war = WarManager.getWarByTown(townName);
                return war != null && war.activeSession ? "true" : "false";
            }

            default:
                return null;
        }
    }

    private int getOnlineTownMembers(String townName) {
        int online = 0;

        for (UUID memberUUID : Main.townMembers.getOrDefault(townName, Collections.emptySet())) {
            Player member = Bukkit.getPlayer(memberUUID);

            if (member != null && member.isOnline()) {
                online++;
            }
        }

        return online;
    }

    private String getTownOwnerName(String townName) {
        UUID ownerUUID = Main.townOwner.get(townName);

        if (ownerUUID == null) {
            return "Unknown";
        }

        String name = Bukkit.getOfflinePlayer(ownerUUID).getName();
        return name != null ? name : "Unknown";
    }

    private String getTownAssistantName(String townName) {
        UUID assistantUUID = Main.townAssistant.get(townName);

        if (assistantUUID == null) {
            return "None";
        }

        String name = Bukkit.getOfflinePlayer(assistantUUID).getName();
        return name != null ? name : "Unknown";
    }

    private String formatRole(String role) {
        if (role == null || role.isEmpty()) {
            return "Member";
        }

        switch (role.toLowerCase()) {
            case "ruler":
                return "Ruler";
            case "assistant":
                return "Assistant";
            default:
                return "Member";
        }
    }

    private String formatTownSet(Set<String> towns) {
        if (towns == null || towns.isEmpty()) {
            return "None";
        }

        return String.join(", ", towns);
    }
}