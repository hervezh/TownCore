package com.silvarys;

import com.silvarys.gui.GUIHolder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TownSettingsGUI {

    public static final String SETTINGS_MENU_TITLE = "§d§lTown Settings";

    private TownSettingsGUI() {
    }

    public static void open(Player player) {
        String townName = Main.playerTown.get(player.getUniqueId());

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        GUIHolder.TownSettings holder = new GUIHolder.TownSettings();
        Inventory gui = Bukkit.createInventory(holder, 27, SETTINGS_MENU_TITLE);
        holder.setInventory(gui);

        fillBorder(gui);

        boolean mobsEnabled = Main.townMobsEnabled.getOrDefault(townName, true);

        String chatFormat = Main.townChatFormat.getOrDefault(townName, "default");

        gui.setItem(4, createItem(
                Material.WRITABLE_BOOK,
                "§d§l" + townName + " Settings",
                List.of(
                        "§7Manage your town settings.",
                        " ",
                        "§eMobs: " + formatToggle(mobsEnabled),
                        "§eChat Format: §f" + capitalize(chatFormat)
                )
        ));

        gui.setItem(10, createItem(
                mobsEnabled ? Material.LIME_DYE : Material.GRAY_DYE,
                "§a§lHostile Mobs",
                List.of(
                        "§7Toggle hostile mob spawning.",
                        " ",
                        "§eCurrent: " + formatToggle(mobsEnabled),
                        " ",
                        "§7Requires §fPeacekeeper §7upgrade.",
                        "§7Click to toggle."
                )
        ));

        gui.setItem(14, createItem(
                Material.NAME_TAG,
                "§6§lChat Format",
                List.of(
                        "§7Change your town chat style.",
                        " ",
                        "§eCurrent: §f" + capitalize(chatFormat),
                        " ",
                        "§7Click to cycle:",
                        "§fDefault §7→ §fFancy §7→ §fSimple"
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
        ItemStack filler = createItem(Material.PURPLE_STAINED_GLASS_PANE, " ", List.of());

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

    private static String formatToggle(boolean enabled) {
        return enabled ? "§aON" : "§cOFF";
    }

    private static String capitalize(String input) {
        if (input == null || input.isEmpty()) return "Default";

        return Character.toUpperCase(input.charAt(0)) + input.substring(1).toLowerCase();
    }
}