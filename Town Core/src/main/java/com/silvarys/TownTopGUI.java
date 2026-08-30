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

import java.util.*;

public class TownTopGUI implements Listener {

    public static final String TITLE = "§6§lTown Leaderboard";

    public static void open(Player player) {
        GUIHolder.TownTop holder = new GUIHolder.TownTop();
        Inventory gui = Bukkit.createInventory(holder, 54, TITLE);
        holder.setInventory(gui);
        fillDecorations(gui);

        List<String> towns = new ArrayList<>(Main.townLevel.keySet());
        towns.sort((t1, t2) -> {
            int lvl1 = Main.townLevel.getOrDefault(t1, 1);
            int lvl2 = Main.townLevel.getOrDefault(t2, 1);
            if (lvl1 != lvl2) return Integer.compare(lvl2, lvl1);
            
            double bank1 = Main.townBank.getOrDefault(t1, 0.0);
            double bank2 = Main.townBank.getOrDefault(t2, 0.0);
            return Double.compare(bank2, bank1);
        });

        // Podium Slots
        int[] slots = {13, 21, 23, 30, 31, 32, 39, 40, 41};
        String[] colors = {"§6§l", "§7§l", "§c§l", "§f§l", "§f§l", "§f§l", "§f§l", "§f§l", "§f§l"};
        
        for (int i = 0; i < Math.min(towns.size(), slots.length); i++) {
            String townName = towns.get(i);
            gui.setItem(slots[i], createRankHead(townName, i + 1, colors[i]));
        }

        gui.setItem(49, createItem(Material.ARROW, "§f§lBack", List.of("§7Return to town menu")));

        player.openInventory(gui);
    }

    private static ItemStack createRankHead(String townName, int rank, String rankColor) {
        UUID ownerUUID = Main.townOwner.get(townName);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            if (ownerUUID != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(ownerUUID));
            }

            meta.displayName(Component.text(rankColor + "#" + rank + " " + townName)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(" "));
            lore.add(Component.text("§7Level: §e" + Main.townLevel.getOrDefault(townName, 1)));
            lore.add(Component.text("§7Bank: §a$" + String.format("%.2f", Main.townBank.getOrDefault(townName, 0.0))));
            lore.add(Component.text("§7Members: §f" + Main.townMembers.getOrDefault(townName, new HashSet<>()).size()));
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

    private static void fillDecorations(Inventory gui) {
        ItemStack gold = createItem(Material.YELLOW_STAINED_GLASS_PANE, " ", List.of());
        ItemStack silver = createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ", List.of());
        ItemStack bronze = createItem(Material.ORANGE_STAINED_GLASS_PANE, " ", List.of());
        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());

        for (int i = 0; i < 54; i++) {
            gui.setItem(i, bg);
        }

        // Podium accents
        gui.setItem(22, gold);
        gui.setItem(30, silver); // Overwritten later by heads if rank exists
        gui.setItem(32, bronze);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GUIHolder.TownTop)) return;
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

            if (slot == 49) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                TownGUI.open(player);
            } else if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = (SkullMeta) event.getCurrentItem().getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    String displayName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.displayName());
                    // displayName looks like "#1 TownName"
                    String townName = org.bukkit.ChatColor.stripColor(displayName).substring(displayName.indexOf(" ") + 1);
                    player.closeInventory();
                    player.performCommand("town info " + townName);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GUIHolder.TownTop)) return;

        for (int slot : event.getRawSlots()) {
            if (slot < event.getView().getTopInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
