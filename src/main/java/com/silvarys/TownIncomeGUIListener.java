package com.silvarys;

import com.silvarys.gui.GUIHolder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TownIncomeGUIListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof GUIHolder.TownIncome)) return;

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
        if (event.getRawSlot() >= 0 && event.getRawSlot() < event.getView().getTopInventory().getSize()) {
            event.setCancelled(true);
            player.updateInventory();

            // Only process if they actually clicked the top inventory
            if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
                return;
            }

            String townName = Main.playerTown.get(player.getUniqueId());
            if (townName == null) return;

            int slot = event.getRawSlot();
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            // Ignore category labels, filler, etc.
            if (clickedItem.getType() == Material.BARRIER
                    || clickedItem.getType() == Material.RED_STAINED_GLASS_PANE
                    || clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE) {
                return;
            }

            int posInRow = slot % 9;
            if (posInRow == 0 && slot < 45) {
                return;
            }

            // Collect All button
            if (slot == 49 && clickedItem.getType() == Material.HOPPER) {
                collectAll(player, townName);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                TownIncomeGUI.open(player);
                return;
            }

            // Income item slots (rows 0-4, positions 1-8)
            if (slot < 45 && posInRow >= 1) {
                collectItem(player, townName, slot);
            }
        }
    }

    private void collectAll(Player player, String townName) {
        Map<String, List<ItemStack>> catMap = Main.townIncome.get(townName);
        if (catMap == null) {
            player.sendMessage("§cThere's nothing to collect right now.");
            return;
        }

        boolean collectedAny = false;
        boolean fullInventory = false;

        for (List<ItemStack> items : catMap.values()) {
            Iterator<ItemStack> iterator = items.iterator();
            while (iterator.hasNext()) {
                ItemStack item = iterator.next();
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());

                if (leftover.isEmpty()) {
                    iterator.remove();
                    collectedAny = true;
                } else {
                    item.setAmount(leftover.get(0).getAmount());
                    collectedAny = true;
                    fullInventory = true;
                }
            }
        }

        if (collectedAny) {
            if (fullInventory) {
                player.sendMessage("§cYour inventory is full! Grabbed what we could.");
            } else {
                player.sendMessage("§aAll income collected!");
            }
        } else {
            player.sendMessage("§cNothing to collect, or your inventory is full.");
        }
    }

    private void collectItem(Player player, String townName, int slot) {
        int row = slot / 9;
        int index = (slot % 9) - 1; // Offset by 1 because slot 0 is the label

        String category = getCategoryByRow(row);
        if (category == null) return;

        Map<String, List<ItemStack>> catMap = Main.townIncome.get(townName);
        if (catMap == null) return;

        List<ItemStack> items = catMap.get(category);
        if (items == null || index < 0 || index >= items.size()) return;

        ItemStack itemToCollect = items.get(index);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(itemToCollect.clone());

        if (leftover.isEmpty()) {
            items.remove(index);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
        } else {
            itemToCollect.setAmount(leftover.get(0).getAmount());
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
            player.sendMessage("§cYour inventory is full! Only grabbed some.");
        }

        TownIncomeGUI.open(player);
    }

    private String getCategoryByRow(int row) {
        return switch (row) {
            case 0 -> "woodcutting";
            case 1 -> "mining";
            case 2 -> "pve";
            case 3 -> "farming";
            case 4 -> "cooking";
            default -> null;
        };
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GUIHolder.TownIncome)) return;

        for (int slot : event.getRawSlots()) {
            if (slot < event.getView().getTopInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
