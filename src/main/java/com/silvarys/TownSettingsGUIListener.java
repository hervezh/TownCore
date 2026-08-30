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
import org.bukkit.plugin.java.JavaPlugin;

public class TownSettingsGUIListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof GUIHolder.TownSettings)) return;

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

            switch (slot) {
                case 10 -> toggleMobs(player);
                case 14 -> cycleChatFormat(player);
                case 22 -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                    TownGUI.open(player);
                }
            }
        }
    }

    private void toggleMobs(Player player) {
        String townName = getTown(player);

        if (townName == null) {
            return;
        }

        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly the ruler or assistant can change town settings!");
            player.closeInventory();
            return;
        }

        if (!TownUpgradesManager.hasUpgrade(townName, TownUpgradesManager.PERK_MOBS_TOGGLE)) {
            player.sendMessage("§cYour town needs the §fPeacekeeper §cupgrade to toggle hostile mob spawning!");
            player.sendMessage("§7Unlock it in §f/town upgrades§7.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.8f);
            return;
        }

        boolean current = Main.townMobsEnabled.getOrDefault(townName, true);
        boolean next = !current;

        Main.townMobsEnabled.put(townName, next);

        Main.logTownAction(townName, player.getName() + " set hostile mobs to " + (next ? "ON" : "OFF") + ".");
        saveTownData();

        player.sendMessage("§aHostile mobs are now " + formatToggle(next) + "§a.");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);

        TownSettingsGUI.open(player);
    }



    private void cycleChatFormat(Player player) {
        String townName = getTown(player);

        if (townName == null) {
            return;
        }

        if (!isRulerOrAssistant(player)) {
            player.sendMessage("§cOnly the ruler or assistant can change town settings!");
            player.closeInventory();
            return;
        }

        String current = Main.townChatFormat.getOrDefault(townName, "default").toLowerCase();
        String next;

        switch (current) {
            case "default" -> next = "fancy";
            case "fancy" -> next = "simple";
            default -> next = "default";
        }

        Main.townChatFormat.put(townName, next);

        Main.logTownAction(townName, player.getName() + " changed town chat format to " + next + ".");
        saveTownData();

        player.sendMessage("§aTown chat format is now §f" + capitalize(next) + "§a.");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);

        TownSettingsGUI.open(player);
    }

    private String getTown(Player player) {
        String townName = Main.playerTown.get(player.getUniqueId());

        if (townName == null) {
            player.sendMessage("§cYou don't have a town!");
            player.closeInventory();
        }

        return townName;
    }

    private boolean isRulerOrAssistant(Player player) {
        String role = Main.playerRole.get(player.getUniqueId());
        return role != null && (role.equalsIgnoreCase("ruler") || role.equalsIgnoreCase("assistant"));
    }

    private void saveTownData() {
        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(TownSettingsGUIListener.class);
        StorageManager.saveData(plugin);
    }

    private String formatToggle(boolean enabled) {
        return enabled ? "§aON" : "§cOFF";
    }

    private String capitalize(String input) {
        if (input == null || input.isEmpty()) {
            return "Default";
        }

        return Character.toUpperCase(input.charAt(0)) + input.substring(1).toLowerCase();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GUIHolder.TownSettings)) return;

        for (int slot : event.getRawSlots()) {
            if (slot < event.getView().getTopInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }
}