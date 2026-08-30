package com.silvarys;

import com.silvarys.gui.GUIHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TownUpgradesGUI implements Listener {

    public static final String UPGRADES_MENU_TITLE = "§d§lTown Upgrades";

    public static void open(Player player) {
        String townName = Main.playerTown.get(player.getUniqueId());
        if (townName == null) return;

        GUIHolder.TownUpgrades holder = new GUIHolder.TownUpgrades();
        Inventory gui = Bukkit.createInventory(holder, 45, UPGRADES_MENU_TITLE);
        holder.setInventory(gui);
        fillBorder(gui);

        int tokens = Main.townUpgradeTokens.getOrDefault(townName, 0);
        Set<String> upgrades = Main.townUpgrades.getOrDefault(townName, new HashSet<>());

        gui.setItem(4, createItem(Material.NETHER_STAR, "§d§lUpgrade Tokens", List.of(
                "§7Tokens available: §f" + tokens + " / 10",
                " ",
                "§7Earn 1 token every time your town levels up."
        )));

        // --- ROW 2: ECONOMY & UTILITY ---
        // Path Speed
        gui.setItem(10, createUpgradeItem(Material.DIRT_PATH, "§e§lPath Speed", List.of(
                "§7Gain Speed I when walking on",
                "§7cobblestone or dirt paths in town.",
                " ",
                "§7Cost: §f1 Token"
        ), upgrades.contains(TownUpgradesManager.PERK_SPEED_PATH)));

        // No Hunger
        gui.setItem(12, createUpgradeItem(Material.COOKED_BEEF, "§a§lEternal Saturation", List.of(
                "§7No hunger loss inside town claims.",
                "§7(Disabled during active war sessions)",
                " ",
                "§7Cost: §f1 Token"
        ), upgrades.contains(TownUpgradesManager.PERK_NO_HUNGER)));

        // Crop Growth
        gui.setItem(14, createUpgradeItem(Material.WHEAT, "§6§lFertile Land", List.of(
                "§7Doubles the growth speed of crops",
                "§7inside town claims.",
                " ",
                "§7Cost: §f1 Token"
        ), upgrades.contains(TownUpgradesManager.PERK_CROP_GROWTH)));

        // Mobs Toggle
        gui.setItem(16, createUpgradeItem(Material.ZOMBIE_HEAD, "§c§lPeacekeeper", List.of(
                "§7Unlocks the ability to toggle",
                "§7hostile mob spawning in town.",
                " ",
                "§7Cost: §f2 Tokens"
        ), upgrades.contains(TownUpgradesManager.PERK_MOBS_TOGGLE)));

        // --- ROW 3: BUILDING & EXPLORATION ---
        // Builder's Spirit
        gui.setItem(19, createUpgradeItem(Material.GOLDEN_PICKAXE, "§b§lBuilder's Spirit", List.of(
                "§7Grants Haste I to all members",
                "§7while inside town borders.",
                "§7(Disabled during active war sessions)",
                " ",
                "§7Cost: §f2 Tokens"
        ), upgrades.contains(TownUpgradesManager.PERK_HASTE_BUILDER)));

        // Night Owl
        gui.setItem(21, createUpgradeItem(Material.ENDER_EYE, "§9§lNight Owl", List.of(
                "§7Permanent Night Vision for all",
                "§7members inside town claims.",
                "§7(Disabled during active war sessions)",
                " ",
                "§7Cost: §f1 Token"
        ), upgrades.contains(TownUpgradesManager.PERK_NIGHT_VISION)));

        // Fireproof
        gui.setItem(23, createUpgradeItem(Material.BLAZE_POWDER, "§6§lFireproof", List.of(
                "§7Prevents fire from spreading or",
                "§7burning blocks inside town borders.",
                " ",
                "§7Cost: §f1 Token"
        ), upgrades.contains(TownUpgradesManager.PERK_FIREPROOF)));

        // Waterman
        gui.setItem(25, createUpgradeItem(Material.HEART_OF_THE_SEA, "§3§lWaterman", List.of(
                "§7Permanent Water Breathing while",
                "§7swimming in town claims.",
                "§7(Disabled during active war sessions)",
                " ",
                "§7Cost: §f1 Token"
        ), upgrades.contains(TownUpgradesManager.PERK_WATER_BREATHING)));

        // --- ROW 4: SURVIVAL ---
        // Graceful Landing
        gui.setItem(29, createUpgradeItem(Material.FEATHER, "§f§lGraceful Landing", List.of(
                "§7Reduces all fall damage by 50%",
                "§7while inside town claims.",
                "§7(Disabled during active war sessions)",
                " ",
                "§7Cost: §f2 Tokens"
        ), upgrades.contains(TownUpgradesManager.PERK_HALF_FALL_DAMAGE)));

        // Life Aura
        gui.setItem(33, createUpgradeItem(Material.GHAST_TEAR, "§d§lLife Aura", List.of(
                "§7Grants Regeneration I when your",
                "§7health drops below 5 hearts.",
                "§7(Disabled during active war sessions)",
                " ",
                "§7Cost: §f3 Tokens"
        ), upgrades.contains(TownUpgradesManager.PERK_REGEN_LOW_HEALTH)));

        // Reinforced Core
        gui.setItem(35, createUpgradeItem(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, "§4§lReinforced Core", List.of(
                "§7Triples the clicks required for",
                "§7attackers to damage your core.",
                " ",
                "§c§l(!) Single War Session only",
                " ",
                "§7Cost: §f5 Tokens"
        ), upgrades.contains(TownUpgradesManager.PERK_REINFORCED_CORE)));

        // Radar L1
        gui.setItem(31, createUpgradeItem(Material.RECOVERY_COMPASS, "§6§lRadar (Level 1)", List.of(
                "§7Detects unallied players within",
                "§750 blocks of your town claims.",
                " ",
                "§7Cost: §f2 Tokens"
        ), upgrades.contains(TownUpgradesManager.PERK_RADAR_L1)));

        // Radar L2
        gui.setItem(32, createUpgradeItem(Material.COMPASS, "§d§lRadar (Level 2)", List.of(
                "§7Upgrades range to 100 blocks.",
                "§7Alerts when unallied players",
                "§7enter your town claims.",
                " ",
                "§7Requires: §fRadar Level 1",
                "§7Cost: §f3 Tokens"
        ), upgrades.contains(TownUpgradesManager.PERK_RADAR_L2)));

        gui.setItem(40, createItem(Material.ARROW, "§f§lBack", List.of("§7Return to the main town menu.")));

        player.openInventory(gui);
    }

    private static ItemStack createUpgradeItem(Material material, String name, List<String> lore, boolean unlocked) {
        List<String> finalLore = new ArrayList<>(lore);
        finalLore.add(" ");
        if (unlocked) {
            finalLore.add("§a§lUNLOCKED");
        } else {
            finalLore.add("§eClick to unlock!");
        }

        ItemStack item = createItem(unlocked ? Material.ENCHANTED_BOOK : material, name, finalLore);
        if (unlocked) {
            item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING, 1);
        }
        return item;
    }

    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(Component.text(line).color(TextColor.color(0xDDDDDD)).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(loreComponents);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private static void fillBorder(Inventory gui) {
        ItemStack filler = createItem(Material.PURPLE_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < gui.getSize(); i++) {
            if (i < 9 || i >= gui.getSize() - 9 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, filler);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof GUIHolder.TownUpgrades)) return;

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

            String townName = Main.playerTown.get(player.getUniqueId());
            if (townName == null) return;

            if (slot == 40) {
                TownGUI.open(player);
                return;
            }

            String upgrade = null;
            int cost = 0;

            switch (slot) {
                case 10 -> { upgrade = TownUpgradesManager.PERK_SPEED_PATH; cost = 1; }
                case 12 -> { upgrade = TownUpgradesManager.PERK_NO_HUNGER; cost = 1; }
                case 14 -> { upgrade = TownUpgradesManager.PERK_CROP_GROWTH; cost = 1; }
                case 16 -> { upgrade = TownUpgradesManager.PERK_MOBS_TOGGLE; cost = 2; }
                case 19 -> { upgrade = TownUpgradesManager.PERK_HASTE_BUILDER; cost = 2; }
                case 21 -> { upgrade = TownUpgradesManager.PERK_NIGHT_VISION; cost = 1; }
                case 23 -> { upgrade = TownUpgradesManager.PERK_FIREPROOF; cost = 1; }
                case 25 -> { upgrade = TownUpgradesManager.PERK_WATER_BREATHING; cost = 1; }
                case 29 -> { upgrade = TownUpgradesManager.PERK_HALF_FALL_DAMAGE; cost = 2; }
                case 33 -> { upgrade = TownUpgradesManager.PERK_REGEN_LOW_HEALTH; cost = 3; }
                case 35 -> { upgrade = TownUpgradesManager.PERK_REINFORCED_CORE; cost = 5; }
                case 31 -> { upgrade = TownUpgradesManager.PERK_RADAR_L1; cost = 2; }
                case 32 -> { upgrade = TownUpgradesManager.PERK_RADAR_L2; cost = 3; }
            }

            if (upgrade != null) {
                java.util.Set<String> upgrades = Main.townUpgrades.getOrDefault(townName, new java.util.HashSet<>());
                if (upgrades.contains(upgrade)) {
                    player.sendMessage("§cYour town already has this upgrade!");
                    return;
                }

                if (upgrade.equals(TownUpgradesManager.PERK_RADAR_L2) && !upgrades.contains(TownUpgradesManager.PERK_RADAR_L1)) {
                    player.sendMessage("§cYou must unlock Radar Level 1 first!");
                    return;
                }

                int tokens = Main.townUpgradeTokens.getOrDefault(townName, 0);
                if (tokens < cost) {
                    player.sendMessage("§cYour town does not have enough upgrade tokens! (" + tokens + "/" + cost + ")");
                    return;
                }

                Main.townUpgradeTokens.put(townName, tokens - cost);
                upgrades.add(upgrade);
                Main.townUpgrades.put(townName, upgrades);

                Main.logTownAction(townName, player.getName() + " purchased upgrade: " + upgrade);
                StorageManager.saveData(JavaPlugin.getProvidingPlugin(TownUpgradesGUI.class));

                player.sendMessage("§aSuccessfully unlocked upgrade: §f" + upgrade.replace("_", " ").toUpperCase());
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                open(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GUIHolder.TownUpgrades)) return;

        for (int slot : event.getRawSlots()) {
            if (slot < event.getView().getTopInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
