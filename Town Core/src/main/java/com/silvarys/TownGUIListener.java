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

public class TownGUIListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        boolean isMain = event.getView().getTopInventory().getHolder() instanceof GUIHolder.TownMain;
        boolean isStaff = event.getView().getTopInventory().getHolder() instanceof GUIHolder.TownStaff;
        if (!isMain && !isStaff) return;

        // ===== FULL PROTECTION: Cancel ALL exploitable actions =====
        InventoryAction action = event.getAction();

        // Block shift-click from ANY inventory slot (prevents moving items into GUI)
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

        // Block all clicks on the top (GUI) inventory
        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
            event.setCancelled(true);
            player.updateInventory();

            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
                if (isMain) {
                    handleMainMenuClick(player, event);
                } else {
                    handleStaffMenuClick(player, event.getRawSlot());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        boolean isMain = event.getView().getTopInventory().getHolder() instanceof GUIHolder.TownMain;
        boolean isStaff = event.getView().getTopInventory().getHolder() instanceof GUIHolder.TownStaff;
        if (!isMain && !isStaff) return;

        // Cancel drag if ANY slot touches the top inventory
        for (int slot : event.getRawSlots()) {
            if (slot < event.getView().getTopInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void handleMainMenuClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();

        switch (slot) {
            case 4 -> runCommand(player, "town info");
            case 10 -> runCommand(player, "town members");

            case 12 -> {
                if (event.isRightClick()) {
                    runCommand(player, "town border");
                } else {
                    runCommand(player, "town claims");
                }
            }

            case 14 -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                TownBankGUI.open(player);
            }

            case 16 -> runCommand(player, "town level");
            case 20 -> runCommand(player, "town relations");
            case 22 -> runCommand(player, "town warinfo");

            case 24 -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                TownSettingsGUI.open(player);
            }

            case 28 -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                TownUpgradesGUI.open(player);
            }

            case 30 -> runCommand(player, "town spawn");
            case 31, 32 -> runCommand(player, "town help");

            case 40 -> {
                if (player.hasPermission("silvarys.staff")) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                    TownGUI.openStaff(player);
                } else {
                    player.closeInventory();
                    player.sendMessage("§cYou don't have permission to open the staff menu!");
                }
            }

            default -> {
                // Border/filler slots do nothing.
            }
        }
    }

    private void handleStaffMenuClick(Player player, int slot) {
        if (!player.hasPermission("silvarys.staff")) {
            player.closeInventory();
            player.sendMessage("§cYou don't have permission to use the staff menu!");
            return;
        }

        switch (slot) {
            case 10 -> runCommand(player, "town backups");
            case 12 -> runCommand(player, "town save");
            case 14 -> runCommand(player, "town reload");
            case 16 -> runCommand(player, "town renamerequest");

            case 22 -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                TownGUI.open(player);
            }

            default -> {
                // Border/filler slots do nothing.
            }
        }
    }

    private void runCommand(Player player, String command) {
        player.closeInventory();
        player.performCommand(command);
    }
}