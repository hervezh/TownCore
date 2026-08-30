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
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class TownInfoGUI implements Listener {

    public static final String TITLE_PREFIX = "§8Town: ";

    public static void open(Player player, String townName) {
        GUIHolder.TownInfo holder = new GUIHolder.TownInfo(townName);
        Inventory gui = Bukkit.createInventory(holder, 27, TITLE_PREFIX + townName);
        holder.setInventory(gui);
        fillBorder(gui);

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
            lore.add(Component.text("§7Members: §a" + Main.townMembers.getOrDefault(townName, new HashSet<>()).size()));
            lore.add(Component.text("§7Claims: §b" + Main.townChunks.getOrDefault(townName, new HashSet<>()).size()));
            lore.add(Component.text(" "));
            lore.add(Component.text("§eClick to view full info in chat!"));

            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            head.setItemMeta(meta);
        }

        gui.setItem(13, head);

        // Stats Items
        gui.setItem(11, createStatItem(Material.GOLD_INGOT, "§e§lBank Balance", List.of("§7Balance: §a$" + String.format("%.2f", Main.townBank.getOrDefault(townName, 0.0)))));
        gui.setItem(15, createStatItem(Material.SHIELD, "§9§lDiplomacy", List.of(
                "§7Allies: §f" + Main.townAllies.getOrDefault(townName, new HashSet<>()).size(),
                "§7Enemies: §f" + Main.townEnemies.getOrDefault(townName, new HashSet<>()).size()
        )));

        gui.setItem(22, createStatItem(Material.ARROW, "§f§lBack", List.of("§7Return to list")));

        player.openInventory(gui);
    }

    private static ItemStack createStatItem(Material material, String name, List<String> lore) {
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
        ItemStack filler = createStatItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < gui.getSize(); i++) {
            if (i < 9 || i >= 18 || i % 9 == 0 || i % 9 == 8) {
                if (gui.getItem(i) == null) {
                    gui.setItem(i, filler);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GUIHolder.TownInfo holder)) return;
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

            String townName = holder.getTownName();

            if (slot == 13) {
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                TownCommand.sendTownInfoToChat(player, townName);
            } else if (slot == 22) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                TownListGUI.open(player, 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GUIHolder.TownInfo)) return;

        for (int slot : event.getRawSlots()) {
            if (slot < event.getView().getTopInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
