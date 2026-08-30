package com.silvarys;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

public class TutorialManager {

    public static void sendTutorial(Player player, int page) {
        player.sendMessage(" ");
        switch (page) {
            case 1 -> sendPage1(player);
            case 2 -> sendPage2(player);
            case 3 -> sendPage3(player);
            case 4 -> sendPage4(player);
            default -> sendPage1(player);
        }
        player.sendMessage(" ");
    }

    private static void sendPage1(Player player) {
        player.sendMessage(Component.text("  ✦ Getting Started: Founding ✦")
                .color(TextColor.color(0xFFD166))
                .decoration(TextDecoration.BOLD, true));
        player.sendMessage(Component.text("  To create a town, you need a ")
                .color(TextColor.color(0xAAAAAA))
                .append(Component.text("Town Core")
                        .color(TextColor.color(0x5555FF))
                        .decoration(TextDecoration.BOLD, true)));
        player.sendMessage(Component.text("  Craft it using: ")
                .color(TextColor.color(0x888888)));
        player.sendMessage(Component.text("  [ Gold | Gold | Gold ]")
                .color(TextColor.color(0xDAA520)));
        player.sendMessage(Component.text("  [ Gold | Diamond | Gold ]")
                .color(TextColor.color(0xDAA520)));
        player.sendMessage(Component.text("  [ Obsidian | Obsidian | Obsidian ]")
                .color(TextColor.color(0xDAA520)));
        player.sendMessage(Component.text("  Once placed, you must defend it for ")
                .color(TextColor.color(0xAAAAAA))
                .append(Component.text("5 minutes")
                        .color(TextColor.color(0xFFFFFF))));
        
        sendNavigation(player, 1, 4);
    }

    private static void sendPage2(Player player) {
        player.sendMessage(Component.text("  ✦ Claims & Protection ✦")
                .color(TextColor.color(0xf4a261))
                .decoration(TextDecoration.BOLD, true));
        player.sendMessage(Component.text("  Your town starts with 1 chunk. Use ")
                .color(TextColor.color(0xAAAAAA))
                .append(Component.text("/town claim")
                        .color(TextColor.color(0xFFFF55))));
        player.sendMessage(Component.text("  to expand your territory. More levels ")
                .color(TextColor.color(0xAAAAAA)));
        player.sendMessage(Component.text("  equal more claims!")
                .color(TextColor.color(0xAAAAAA)));
        player.sendMessage(Component.text("  Use ")
                .color(TextColor.color(0x888888))
                .append(Component.text("/town border")
                        .color(TextColor.color(0xFFFFFF)))
                .append(Component.text(" to see your land.")
                        .color(TextColor.color(0x888888))));

        sendNavigation(player, 2, 4);
    }

    private static void sendPage3(Player player) {
        player.sendMessage(Component.text("  ✦ Skills & Levels ✦")
                .color(TextColor.color(0x52b788))
                .decoration(TextDecoration.BOLD, true));
        player.sendMessage(Component.text("  Towns level up by performing actions:")
                .color(TextColor.color(0xAAAAAA)));
        player.sendMessage(Component.text("  • Mining, Woodcutting, Farming")
                .color(TextColor.color(0x7fb3d5)));
        player.sendMessage(Component.text("  • Combat (PvP & PvE)")
                .color(TextColor.color(0x7fb3d5)));
        player.sendMessage(Component.text("  Higher levels unlock ")
                .color(TextColor.color(0xAAAAAA))
                .append(Component.text("Income")
                        .color(TextColor.color(0x52b788)))
                .append(Component.text(" and ")
                        .color(TextColor.color(0xAAAAAA)))
                .append(Component.text("Upgrades")
                        .color(TextColor.color(0x52b788))));

        sendNavigation(player, 3, 4);
    }

    private static void sendPage4(Player player) {
        player.sendMessage(Component.text("  ✦ War & Diplomacy ✦")
                .color(TextColor.color(0xe63946))
                .decoration(TextDecoration.BOLD, true));
        player.sendMessage(Component.text("  War can be declared on ")
                .color(TextColor.color(0xAAAAAA))
                .append(Component.text("Enemy")
                        .color(TextColor.color(0xe63946)))
                .append(Component.text(" towns.")
                        .color(TextColor.color(0xAAAAAA))));
        player.sendMessage(Component.text("  During a war, your ")
                .color(TextColor.color(0xAAAAAA))
                .append(Component.text("Town Core")
                        .color(TextColor.color(0x5555FF)))
                .append(Component.text(" must be defended.")
                        .color(TextColor.color(0xAAAAAA))));
        player.sendMessage(Component.text("  It will teleport around to stay safe!")
                .color(TextColor.color(0xAAAAAA)));
        player.sendMessage(Component.text("  Losing a war results in ")
                .color(TextColor.color(0xAAAAAA))
                .append(Component.text("Occupation")
                        .color(TextColor.color(0xe63946)))
                .append(Component.text(" tax.")
                        .color(TextColor.color(0xAAAAAA))));

        sendNavigation(player, 4, 4);
    }

    private static void sendNavigation(Player player, int current, int total) {
        player.sendMessage(" ");
        Component nav = Component.text("  ");
        
        if (current > 1) {
            nav = nav.append(Component.text("« Prev")
                    .color(TextColor.color(0x55FF55))
                    .decoration(TextDecoration.BOLD, true)
                    .clickEvent(ClickEvent.runCommand("/town tutorial " + (current - 1)))
                    .hoverEvent(HoverEvent.showText(Component.text("§7Go to page " + (current - 1)))));
        } else {
            nav = nav.append(Component.text("« Prev")
                    .color(TextColor.color(0x555555)));
        }

        nav = nav.append(Component.text("  §8|  Page " + current + "/" + total + "  §8|  "));

        if (current < total) {
            nav = nav.append(Component.text("Next »")
                    .color(TextColor.color(0x55FF55))
                    .decoration(TextDecoration.BOLD, true)
                    .clickEvent(ClickEvent.runCommand("/town tutorial " + (current + 1)))
                    .hoverEvent(HoverEvent.showText(Component.text("§7Go to page " + (current + 1)))));
        } else {
            nav = nav.append(Component.text("Finish")
                    .color(TextColor.color(0xAAAAAA))
                    .clickEvent(ClickEvent.runCommand("/town help"))
                    .hoverEvent(HoverEvent.showText(Component.text("§7Return to Help Menu"))));
        }

        player.sendMessage(nav);
    }

    public static ItemStack getGuideBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        if (meta != null) {
            meta.setTitle("Town Core Starter Guide");
            meta.setAuthor("Town Core");

            List<Component> pages = new ArrayList<>();

            pages.add(Component.text("Welcome to Town Core!\n\n")
                    .color(TextColor.color(0x000000))
                    .append(Component.text("This guide will help you found your settlement, grow your town, and survive the chaos around it.\n\n")
                            .color(TextColor.color(0x333333)))
                    .append(Component.text("Type /town tutorial in chat for more help!")));

            pages.add(Component.text("Founding a Town\n\n")
                    .decoration(TextDecoration.BOLD, true)
                    .append(Component.text("1. Craft a Town Core.\n2. Place it in the wild.\n3. Defend it for 5 minutes.\n\n")
                            .decoration(TextDecoration.BOLD, false))
                    .append(Component.text("Be careful! If it's broken, you lose the core!")));

            pages.add(Component.text("Economy & Growth\n\n")
                    .decoration(TextDecoration.BOLD, true)
                    .append(Component.text("Your town needs money to survive upkeep. Deposit to your /town bank.\n\nLevel up by mining, woodcutting, and fighting to unlock upgrades!")));

            pages.add(Component.text("War & Siege\n\n")
                    .decoration(TextDecoration.BOLD, true)
                    .append(Component.text("Enemies can declare war. When a session starts, your core becomes vulnerable but will teleport to stay hidden. Defend it at all costs!")));

            meta.addPages(pages.toArray(new Component[0]));
            book.setItemMeta(meta);
        }

        return book;
    }
}
