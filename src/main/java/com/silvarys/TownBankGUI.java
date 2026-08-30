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

public class TownBankGUI {

    public static final String BANK_MENU_TITLE = "§e§lTown Bank";

    private TownBankGUI() {
    }

    public static void open(Player player) {
        String townName = Main.playerTown.get(player.getUniqueId());

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            return;
        }

        GUIHolder.TownBank holder = new GUIHolder.TownBank();
        Inventory gui = Bukkit.createInventory(holder, 27, BANK_MENU_TITLE);
        holder.setInventory(gui);

        fillBorder(gui);

        double balance = Main.townBank.getOrDefault(townName, 0.0);
        double upkeep = Main.getDailyUpkeepCost();
        int survivalDays = Main.getTownSurvivalDays(townName);

        gui.setItem(4, createItem(
                Material.GOLD_BLOCK,
                "§e§l" + townName + " Bank",
                List.of(
                        "§7Your town bank overview.",
                        " ",
                        "§eBalance: §f$" + String.format("%.2f", balance),
                        "§eDaily Upkeep: §f$" + String.format("%.2f", upkeep),
                        "§eSurvival Time: §f" + formatSurvivalDays(survivalDays),
                        " ",
                        getBankWarningLine(survivalDays)
                )
        ));

        gui.setItem(11, createItem(
                Material.EMERALD,
                "§a§lDeposit Money",
                List.of(
                        "§7Add money to your town bank.",
                        " ",
                        "§7Use command:",
                        "§f/town deposit <amount>",
                        " ",
                        "§eExample:",
                        "§f/town deposit 500"
                )
        ));

        gui.setItem(13, createItem(
                Material.PAPER,
                "§6§lBank Status",
                List.of(
                        "§7Current bank health.",
                        " ",
                        "§eBalance: §f$" + String.format("%.2f", balance),
                        "§eUpkeep Cost: §f$" + String.format("%.2f", upkeep) + "/day",
                        "§eSafe For: §f" + formatSurvivalDays(survivalDays),
                        " ",
                        "§7Keep enough money in the bank",
                        "§7so your town can pay upkeep."
                )
        ));

        gui.setItem(15, createItem(
                Material.REDSTONE,
                "§c§lWithdraw Money",
                List.of(
                        "§7Take money from your town bank.",
                        " ",
                        "§7Only ruler/assistant can withdraw.",
                        "§7Use command:",
                        "§f/town withdraw <amount>",
                        " ",
                        "§eExample:",
                        "§f/town withdraw 250"
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
        ItemStack filler = createItem(Material.YELLOW_STAINED_GLASS_PANE, " ", List.of());

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
}