package com.silvarys;

import com.silvarys.gui.GUIHolder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TownIncomeGUI {

    public static final String TITLE = "Town Income";

    /*
     * GUI Layout (54 slots, 6 rows):
     *
     * Each income category gets 1 row:
     *   Slot 0 = Category label item (icon showing what this row produces)
     *   Slots 1-8 = Income items (collectible) or empty
     *
     * Row 0 (slots 0-8):   Woodcutting
     * Row 1 (slots 9-17):  Mining
     * Row 2 (slots 18-26): PvE
     * Row 3 (slots 27-35): Farming
     * Row 4 (slots 36-44): Cooking
     * Row 5 (slots 45-53): Filler + Collect All button at slot 49
     */

    private static final String[] CATEGORIES = {"woodcutting", "mining", "pve", "farming", "cooking"};

    private static final Material[] CATEGORY_ICONS = {
            Material.OAK_LOG,
            Material.IRON_PICKAXE,
            Material.IRON_SWORD,
            Material.WHEAT,
            Material.CAMPFIRE
    };

    private static final String[] CATEGORY_NAMES = {
            "Woodcutting",
            "Mining",
            "PvE",
            "Farming",
            "Cooking"
    };

    private static final int[] CATEGORY_COLORS = {
            0x8B4513, // Brown for woodcutting
            0x808080, // Gray for mining
            0xCC3333, // Red for PvE
            0x55AA55, // Green for farming
            0xFF8800  // Orange for cooking
    };

    private static final String[][] CATEGORY_PRODUCES = {
            {"Oak Log", "Spruce Log", "Birch Log", "Jungle Log", "Acacia Log", "Dark Oak Log"},
            {"Coal Ore", "Iron Ore", "Copper Ore", "Gold Ore", "Redstone Ore", "Emerald Ore", "Lapis Ore", "Diamond Ore", "Deepslate"},
            {"Rotten Flesh", "Bone", "Gunpowder", "String", "Spider Eye", "Slime Ball"},
            {"Wheat", "Carrot", "Potato", "Beetroot"},
            {"Beef", "Porkchop", "Chicken", "Mutton", "Rabbit", "Cod", "Salmon"}
    };

    public static void open(Player player) {
        String townName = Main.playerTown.get(player.getUniqueId());
        if (townName == null) {
            player.sendMessage("§cYou need to be in a town first!");
            return;
        }

        GUIHolder.TownIncome holder = new GUIHolder.TownIncome();
        Inventory inventory = Bukkit.createInventory(holder, 54, Component.text(TITLE));
        holder.setInventory(inventory);

        Map<String, List<ItemStack>> townCatMap = Main.townIncome.get(townName);

        for (int row = 0; row < 5; row++) {
            String category = CATEGORIES[row];
            int taskLevel = TownLevelManager.getTaskLevel(townName, category);
            boolean unlocked = taskLevel >= 40;
            int labelSlot = row * 9;

            inventory.setItem(labelSlot, createCategoryLabel(row, taskLevel, unlocked));

            if (unlocked && townCatMap != null) {
                List<ItemStack> items = townCatMap.get(category);
                if (items != null) {
                    for (int i = 0; i < Math.min(items.size(), 8); i++) {
                        inventory.setItem(labelSlot + 1 + i, items.get(i).clone());
                    }
                }
            } else if (!unlocked) {
                for (int i = 1; i <= 8; i++) {
                    inventory.setItem(labelSlot + i, createLockedSlot(taskLevel));
                }
            }
        }

        // Bottom row - filler + Collect All
        ItemStack glassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassPane.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(Component.text(" "));
            glassPane.setItemMeta(glassMeta);
        }

        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, glassPane);
        }

        ItemStack collectAll = new ItemStack(Material.HOPPER);
        ItemMeta collectMeta = collectAll.getItemMeta();
        if (collectMeta != null) {
            collectMeta.displayName(Component.text("Collect All")
                    .color(TextColor.color(0x55FF55))
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            collectMeta.lore(List.of(Component.text("Grab everything at once!")
                    .color(TextColor.color(0xAAAAAA))
                    .decoration(TextDecoration.ITALIC, false)));
            collectAll.setItemMeta(collectMeta);
        }
        inventory.setItem(49, collectAll);

        player.openInventory(inventory);
    }

    private static ItemStack createCategoryLabel(int categoryIndex, int taskLevel, boolean unlocked) {
        Material icon;
        if (unlocked) {
            icon = CATEGORY_ICONS[categoryIndex];
        } else {
            icon = Material.BARRIER;
        }

        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = CATEGORY_NAMES[categoryIndex];
        int color = CATEGORY_COLORS[categoryIndex];

        if (unlocked) {
            meta.displayName(Component.text(name)
                    .color(TextColor.color(color))
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            meta.displayName(Component.text(name + " §c(Locked)")
                    .color(TextColor.color(0x888888))
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
        }

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        if (unlocked) {
            lore.add(Component.text("✔ Earning resources")
                    .color(TextColor.color(0x55FF55))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Skill Level " + taskLevel)
                    .color(TextColor.color(0xAAAAAA))
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("✘ Needs Level 40 to unlock")
                    .color(TextColor.color(0xFF5555))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Currently Level " + taskLevel)
                    .color(TextColor.color(0xAAAAAA))
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(Component.text("Can drop:")
                .color(TextColor.color(0xFFFF55))
                .decoration(TextDecoration.ITALIC, false));

        for (String produce : CATEGORY_PRODUCES[categoryIndex]) {
            lore.add(Component.text("  • " + produce)
                    .color(TextColor.color(0xBBBBBB))
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(Component.text("Generates 4 items every 4 hours")
                .color(TextColor.color(0x888888))
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createLockedSlot(int taskLevel) {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text("Not yet unlocked")
                .color(TextColor.color(0xFF5555))
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Get this skill to Level 40!")
                        .color(TextColor.color(0xAAAAAA))
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Currently Level " + taskLevel)
                        .color(TextColor.color(0x888888))
                        .decoration(TextDecoration.ITALIC, false)
        ));

        item.setItemMeta(meta);
        return item;
    }
}
