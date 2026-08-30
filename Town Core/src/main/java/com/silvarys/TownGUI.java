package com.silvarys;

import com.silvarys.gui.GUIHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TownGUI {

    public static final String MAIN_MENU_TITLE_SUFFIX = "'s Town";
    public static final String NO_TOWN_TITLE = "§2§lTown Menu";
    public static final String STAFF_MENU_TITLE = "§d§lStaff Panel";

    public static boolean isMainMenuTitle(String title) {
        return title.equals(NO_TOWN_TITLE) || title.endsWith(MAIN_MENU_TITLE_SUFFIX);
    }

    private TownGUI() {
    }

    public static void open(Player player) {
        String townName = Main.playerTown.get(player.getUniqueId());
        String guiTitle = (townName != null) ? "§2§l" + townName + MAIN_MENU_TITLE_SUFFIX : NO_TOWN_TITLE;
        GUIHolder.TownMain holder = new GUIHolder.TownMain();
        Inventory gui = Bukkit.createInventory(holder, 54, guiTitle);
        holder.setInventory(gui);

        fillBorder(gui);



        if (townName == null) {
            gui.setItem(13, createItem(
                    Material.BEACON,
                    "§6§lNo Town",
                    List.of(
                            "§7You are not in a town yet.",
                            " ",
                            "§ePlace a Town Core to create one.",
                            "§7Use §f/town help §7to view commands."
                    )
            ));

            gui.setItem(31, createItem(
                    Material.BOOK,
                    "§e§lCommand Help",
                    List.of(
                            "§7Click to view town commands.",
                            " ",
                            "§f/town help"
                    )
            ));

            player.openInventory(gui);
            return;
        }

        int level = Main.townLevel.getOrDefault(townName, 1);
        int claims = Main.townChunks.getOrDefault(townName, Collections.emptySet()).size();
        int maxClaims = Main.getMaxChunks(level);
        double bank = Main.townBank.getOrDefault(townName, 0.0);
        double upkeep = Main.getDailyUpkeepCost();
        int survivalDays = Main.getTownSurvivalDays(townName);
        String role = Main.playerRole.getOrDefault(player.getUniqueId(), "member");

        List<String> overviewLore = new ArrayList<>(List.of(
                "§7Your town overview.",
                " "
        ));

        if (WarManager.isOccupied(townName)) {
            overviewLore.add("§4§l⚑ OCCUPIED BY: §f" + WarManager.getOccupier(townName));
            overviewLore.add("§cIncome goes to the occupier.");
            overviewLore.add(" ");
        }

        overviewLore.addAll(List.of(
                "§eRole: §f" + capitalize(role),
                "§eLevel: §f" + level,
                "§eClaims: §f" + claims + "/" + maxClaims,
                "§eBank: §f$" + String.format("%.2f", bank),
                " ",
                "§7Click for town info."
        ));

        gui.setItem(4, createItem(
                Material.BEACON,
                "§6§l" + townName,
                overviewLore
        ));

        gui.setItem(10, createItem(
                Material.PLAYER_HEAD,
                "§9§lMembers",
                buildMembersLore(townName)
        ));

        gui.setItem(12, createItem(
                Material.GRASS_BLOCK,
                "§a§lClaims",
                List.of(
                        "§7View your claim usage.",
                        " ",
                        "§eClaims Used: §f" + claims + "/" + maxClaims,
                        "§eRemaining: §f" + Math.max(0, maxClaims - claims),
                        " ",
                        "§7Commands:",
                        "§f/town claim",
                        "§f/town unclaim",
                        "§f/town map",
                        "§f/town border"
                )
        ));

        gui.setItem(14, createItem(
                Material.GOLD_INGOT,
                "§e§lTown Bank",
                List.of(
                        "§7Open the town bank menu.",
                        " ",
                        "§eBalance: §f$" + String.format("%.2f", bank),
                        "§eDaily Upkeep: §f$" + String.format("%.2f", upkeep),
                        "§eSurvival Time: §f" + formatSurvivalDays(survivalDays),
                        " ",
                        getBankWarningLine(survivalDays),
                        " ",
                        "§7Click to open bank GUI."
                )
        ));

        gui.setItem(16, createItem(
                Material.NETHER_STAR,
                "§b§lLevel & Stats",
                List.of(
                        "§7View town progression.",
                        " ",
                        "§eTown Level: §f" + level,
                        "§eMax Claims: §f" + maxClaims,
                        " ",
                        "§7Click to run:",
                        "§f/town level"
                )
        ));

        gui.setItem(20, createItem(
                Material.SHIELD,
                "§c§lRelations",
                buildRelationsLore(townName)
        ));

        gui.setItem(22, createItem(
                Material.IRON_SWORD,
                "§4§lWar",
                List.of(
                        "§7View war information.",
                        " ",
                        "§7Commands:",
                        "§f/town warinfo",
                        "§f/town declarewar <town>",
                        "§f/town startwar",
                        "§f/town surrender"
                )
        ));

        gui.setItem(24, createItem(
                Material.WRITABLE_BOOK,
                "§d§lTown Settings",
                buildSettingsLore(townName)
        ));

        gui.setItem(28, createItem(
                Material.ENCHANTED_BOOK,
                "§d§lTown Upgrades",
                List.of(
                        "§7Spend tokens on town perks.",
                        " ",
                        "§eTokens: §f" + Main.townUpgradeTokens.getOrDefault(townName, 0),
                        " ",
                        "§7Click to open upgrades GUI."
                )
        ));

        gui.setItem(30, createItem(
                Material.ENDER_PEARL,
                "§a§lTown Spawn",
                List.of(
                        "§7Teleport to your town spawn.",
                        " ",
                        "§7Click to run:",
                        "§f/town spawn"
                )
        ));

        gui.setItem(32, createItem(
                Material.BOOK,
                "§e§lCommand Help",
                List.of(
                        "§7View all town commands.",
                        " ",
                        "§7Click to run:",
                        "§f/town help"
                )
        ));

        if (player.hasPermission("silvarys.staff")) {
            gui.setItem(40, createItem(
                    Material.COMMAND_BLOCK,
                    "§d§lStaff Panel",
                    List.of(
                            "§7Staff-only town tools.",
                            " ",
                            "§7Click to open staff commands.",
                            "§f/town help staff"
                    )
            ));
        }

        player.openInventory(gui);
    }

    public static void openStaff(Player player) {
        if (!player.hasPermission("silvarys.staff")) {
            player.sendMessage("§cYou don't have permission to open the staff town menu!");
            return;
        }

        GUIHolder.TownStaff holder = new GUIHolder.TownStaff();
        Inventory gui = Bukkit.createInventory(holder, 27, STAFF_MENU_TITLE);
        holder.setInventory(gui);

        fillBorder(gui);

        gui.setItem(10, createItem(
                Material.CHEST,
                "§e§lBackups",
                List.of(
                        "§7List town backups.",
                        " ",
                        "§7Click to run:",
                        "§f/town backups"
                )
        ));

        gui.setItem(12, createItem(
                Material.EMERALD,
                "§a§lSave Data",
                List.of(
                        "§7Manually save town data.",
                        " ",
                        "§7Click to run:",
                        "§f/town save"
                )
        ));

        gui.setItem(14, createItem(
                Material.REDSTONE,
                "§c§lReload Data",
                List.of(
                        "§7Reload town data from towns.yml.",
                        " ",
                        "§7Click to run:",
                        "§f/town reload"
                )
        ));

        gui.setItem(16, createItem(
                Material.NAME_TAG,
                "§d§lRename Requests",
                List.of(
                        "§7View pending rename requests.",
                        " ",
                        "§7Click to run:",
                        "§f/town renamerequest"
                )
        ));

        gui.setItem(22, createItem(
                Material.ARROW,
                "§f§lBack",
                List.of(
                        "§7Return to the main town menu."
                )
        ));

        player.openInventory(gui);
    }

    private static void fillBorder(Inventory gui) {
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());

        int size = gui.getSize();

        for (int i = 0; i < size; i++) {
            boolean topRow = i < 9;
            boolean bottomRow = i >= size - 9;
            boolean leftSide = i % 9 == 0;
            boolean rightSide = i % 9 == 8;

            if (topRow || bottomRow || leftSide || rightSide) {
                gui.setItem(i, filler);
            }
        }
    }

    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        meta.displayName(Component.text(name)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> loreComponents = new ArrayList<>();

        for (String line : lore) {
            loreComponents.add(Component.text(line)
                    .color(TextColor.color(0xDDDDDD))
                    .decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(loreComponents);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

        item.setItemMeta(meta);
        return item;
    }

    private static List<String> buildMembersLore(String townName) {
        List<String> lore = new ArrayList<>();

        UUID ownerUUID = Main.townOwner.get(townName);
        UUID assistantUUID = Main.townAssistant.get(townName);

        String ownerName = getPlayerName(ownerUUID);
        String assistantName = assistantUUID != null ? getPlayerName(assistantUUID) : "None";

        int members = Main.townMembers.getOrDefault(townName, Collections.emptySet()).size();

        lore.add("§7View town members.");
        lore.add(" ");
        lore.add("§cRuler: §f" + ownerName);
        lore.add("§6Assistant: §f" + assistantName);
        lore.add("§eMembers: §f" + members);
        lore.add(" ");
        lore.add("§7Commands:");
        lore.add("§f/town members");
        lore.add("§f/town online");
        lore.add("§f/town invite <player>");

        return lore;
    }

    private static List<String> buildRelationsLore(String townName) {
        List<String> lore = new ArrayList<>();

        int allies = Main.townAllies.getOrDefault(townName, Collections.emptySet()).size();
        int enemies = Main.townEnemies.getOrDefault(townName, Collections.emptySet()).size();
        int wars = Main.townWars.getOrDefault(townName, Collections.emptySet()).size();

        lore.add("§7View diplomacy information.");
        lore.add(" ");
        lore.add("§aAllies: §f" + allies);
        lore.add("§cEnemies: §f" + enemies);
        lore.add("§4Wars: §f" + wars);
        lore.add(" ");
        lore.add("§7Commands:");
        lore.add("§f/town relations");
        lore.add("§f/town ally <town>");
        lore.add("§f/town enemy <town>");

        return lore;
    }

    private static List<String> buildSettingsLore(String townName) {
        List<String> lore = new ArrayList<>();

        boolean mobsEnabled = Main.townMobsEnabled.getOrDefault(townName, true);
        boolean publicJoin = Main.townPublicJoin.getOrDefault(townName, false);

        String chatFormat = Main.townChatFormat.getOrDefault(townName, "default");

        lore.add("§7Open town settings.");
        lore.add(" ");
        lore.add("§eMobs: " + formatToggle(mobsEnabled));
        lore.add("§ePublic Join: " + formatToggle(publicJoin));
        lore.add("§eChat Format: §f" + capitalize(chatFormat));
        lore.add(" ");
        lore.add("§7Click to open settings GUI.");

        return lore;
    }

    private static String getPlayerName(UUID uuid) {
        if (uuid == null) return "Unknown";

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);

        if (offlinePlayer.getName() == null) {
            return "Unknown";
        }

        return offlinePlayer.getName();
    }

    private static String formatToggle(boolean enabled) {
        return enabled ? "§aON" : "§cOFF";
    }

    private static String formatSurvivalDays(int days) {
        if (days <= 0) {
            return "Less than 1 day";
        }

        return days + " day" + (days == 1 ? "" : "s");
    }

    private static String getBankWarningLine(int days) {
        if (days <= 0) {
            return "§cBank is critically low!";
        }

        if (days <= 2) {
            return "§cYour town needs money soon.";
        }

        if (days <= 7) {
            return "§eYour town should deposit more soon.";
        }

        return "§aYour town bank is healthy.";
    }

    private static String capitalize(String input) {
        if (input == null || input.isEmpty()) return input;

        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }
}