package com.silvarys;

import com.silvarys.gui.GUIHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TownListGUI implements Listener {

    public static final String TITLE_PREFIX = "§8Town List - Page ";
    private static final int TOWNS_PER_PAGE = 28; // Using 4 rows for towns, 2 for border/nav

    public static void open(Player player, int page) {
        List<String> towns = new ArrayList<>(Main.townLevel.keySet());
        towns.sort(String.CASE_INSENSITIVE_ORDER);

        int totalPages = (int) Math.ceil((double) towns.size() / TOWNS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        if (page > totalPages) page = totalPages;
        if (page < 1) page = 1;

        GUIHolder.TownList holder = new GUIHolder.TownList(page);
        Inventory gui = Bukkit.createInventory(holder, 54, TITLE_PREFIX + page);
        holder.setInventory(gui);
        fillBorder(gui);

        int start = (page - 1) * TOWNS_PER_PAGE;
        int end = Math.min(start + TOWNS_PER_PAGE, towns.size());

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        int slotIndex = 0;
        for (int i = start; i < end; i++) {
            String townName = towns.get(i);
            gui.setItem(slots[slotIndex++], createTownHead(townName));
        }

        // Navigation
        if (page > 1) {
            gui.setItem(48, createItem(Material.ARROW, "§e§lPrevious Page", List.of("§7Back to page " + (page - 1))));
        }
        
        gui.setItem(49, createItem(Material.BARRIER, "§c§lClose", List.of("§7Return to town menu")));

        if (page < totalPages) {
            gui.setItem(50, createItem(Material.ARROW, "§e§lNext Page", List.of("§7Forward to page " + (page + 1))));
        }

        player.openInventory(gui);
    }

    private static ItemStack createTownHead(String townName) {
        UUID ownerUUID = Main.townOwner.get(townName);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            if (ownerUUID != null) {
                OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);
                meta.setOwningPlayer(owner);
            }

            meta.displayName(Component.text(townName)
                    .color(TextColor.color(0xFFAA00))
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(" "));
            lore.add(Component.text("§7Ruler: §f" + (ownerUUID != null ? Bukkit.getOfflinePlayer(ownerUUID).getName() : "Unknown")));
            lore.add(Component.text("§7Level: §e" + Main.townLevel.getOrDefault(townName, 1)));
            lore.add(Component.text("§7Members: §a" + Main.townMembers.getOrDefault(townName, new java.util.HashSet<>()).size()));
            lore.add(Component.text(" "));
            lore.add(Component.text("§eClick to view info!"));

            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            head.setItemMeta(meta);
        }

        return head;
    }

    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
            List<Component> loreComp = new ArrayList<>();
            for (String line : lore) {
                loreComp.add(Component.text(line).color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(loreComp);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void fillBorder(Inventory gui) {
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < gui.getSize(); i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, filler);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GUIHolder.TownList holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // ===== FULL PROTECTION =====
        InventoryAction action = event.getAction();

        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
            player.updateInventory();
            return;
        }

        if (event.getClick().name().equals("NUMBER_KEY")) {
            event.setCancelled(true);
            player.updateInventory();
            return;
        }

        if (event.getClick().name().equals("SWAP_OFFHAND")) {
            event.setCancelled(true);
            player.updateInventory();
            return;
        }

        if (action == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            player.updateInventory();
            return;
        }

        // Handle top inventory clicks
        int slot = event.getRawSlot();
        if (slot >= 0 && slot < event.getView().getTopInventory().getSize()) {
            event.setCancelled(true);
            player.updateInventory();

            if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
                return;
            }

            int page = holder.getPage();

            if (slot == 48 && page > 1) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                open(player, page - 1);
            } else if (slot == 50) {
                List<String> towns = new ArrayList<>(Main.townLevel.keySet());
                int totalPages = (int) Math.ceil((double) towns.size() / TOWNS_PER_PAGE);
                if (page < totalPages) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    open(player, page + 1);
                }
            } else if (slot == 49) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                TownGUI.open(player);
            } else if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = (SkullMeta) event.getCurrentItem().getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    String townName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.displayName());
                    // Remove formatting if any
                    townName = org.bukkit.ChatColor.stripColor(townName);
                    player.closeInventory();
                    player.performCommand("town info " + townName);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GUIHolder.TownList)) return;

        for (int slot : event.getRawSlots()) {
            if (slot < event.getView().getTopInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
