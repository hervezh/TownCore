package com.silvarys;

import com.silvarys.gui.GUIHolder;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class TownBankGUIListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof GUIHolder.TownBank)) return;

        // ===== FULL PROTECTION =====
        InventoryAction action = event.getAction();

        // Block shift-click from any slot
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
            player.updateInventory();
            return;
        }

        // Block number key hotbar swaps
        if (event.getClick().name().equals("NUMBER_KEY")) {
            event.setCancelled(true);
            player.updateInventory();
            return;
        }

        // Block offhand swap (F key)
        if (event.getClick().name().equals("SWAP_OFFHAND")) {
            event.setCancelled(true);
            player.updateInventory();
            return;
        }

        // Block double-click collection
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

            switch (slot) {
                case 11 -> {
                    player.closeInventory();
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                    player.sendMessage("§eUse §f/town deposit <amount> §eto deposit money into your town bank.");
                }

                case 13 -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                    TownBankGUI.open(player);
                }

                case 15 -> {
                    player.closeInventory();
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                    player.sendMessage("§eUse §f/town withdraw <amount> §eto withdraw money from your town bank.");
                    player.sendMessage("§7Only the ruler or assistant can withdraw.");
                }

                case 22 -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                    TownGUI.open(player);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GUIHolder.TownBank)) return;

        for (int slot : event.getRawSlots()) {
            if (slot < event.getView().getTopInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }
}