package com.silvarys;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TownIncomeManager {

    private static final long INCOME_INTERVAL_MS = 4L * 60 * 60 * 1000; // 4 hours

    private static final Material[] WOODCUTTING_ITEMS = {
            Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG,
            Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG
    };

    private static final Material[] MINING_ITEMS = {
            Material.COAL_ORE, Material.IRON_ORE, Material.COPPER_ORE, Material.GOLD_ORE,
            Material.REDSTONE_ORE, Material.EMERALD_ORE, Material.LAPIS_ORE, Material.DIAMOND_ORE,
            Material.DEEPSLATE
    };

    private static final Material[] PVE_ITEMS = {
            Material.ROTTEN_FLESH, Material.BONE, Material.GUNPOWDER,
            Material.STRING, Material.SPIDER_EYE, Material.SLIME_BALL
    };

    private static final Material[] FARMING_ITEMS = {
            Material.WHEAT, Material.CARROT, Material.POTATO, Material.BEETROOT
    };

    private static final Material[] COOKING_ITEMS = {
            Material.BEEF, Material.PORKCHOP, Material.CHICKEN,
            Material.MUTTON, Material.RABBIT, Material.COD, Material.SALMON
    };

    public static void startIncomeTimer(JavaPlugin plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                for (String townName : Main.townLevel.keySet()) {
                    long lastTime = Main.townLastIncomeTime.getOrDefault(townName, now);
                    
                    if (now - lastTime >= INCOME_INTERVAL_MS) {
                        generateIncome(townName);
                        Main.townLastIncomeTime.put(townName, now);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L * 60L, 20L * 60L); // Check every minute
    }

    public static void generateIncome(String townName) {
        // If this town is occupied, their income goes to the occupier
        String recipient = townName;
        if (WarManager.isOccupied(townName)) {
            String occupier = WarManager.getOccupier(townName);
            if (occupier != null) {  // Fix for null occupier
                recipient = occupier;
            }
        }

        Random random = new Random();

        if (TownLevelManager.getTaskLevel(townName, "woodcutting") >= 40) {
            Material mat = WOODCUTTING_ITEMS[random.nextInt(WOODCUTTING_ITEMS.length)];
            addIncomeItem(recipient, "woodcutting", new ItemStack(mat, 4));
        }

        if (TownLevelManager.getTaskLevel(townName, "mining") >= 40) {
            Material mat = MINING_ITEMS[random.nextInt(MINING_ITEMS.length)];
            addIncomeItem(recipient, "mining", new ItemStack(mat, 4));
        }

        if (TownLevelManager.getTaskLevel(townName, "pve") >= 40) {
            Material mat = PVE_ITEMS[random.nextInt(PVE_ITEMS.length)];
            addIncomeItem(recipient, "pve", new ItemStack(mat, 4));
        }

        if (TownLevelManager.getTaskLevel(townName, "farming") >= 40) {
            Material mat = FARMING_ITEMS[random.nextInt(FARMING_ITEMS.length)];
            addIncomeItem(recipient, "farming", new ItemStack(mat, 4));
        }

        if (TownLevelManager.getTaskLevel(townName, "cooking") >= 40) {
            Material mat = COOKING_ITEMS[random.nextInt(COOKING_ITEMS.length)];
            addIncomeItem(recipient, "cooking", new ItemStack(mat, 4));
        }
    }

    private static void addIncomeItem(String townName, String category, ItemStack item) {
        Main.townIncome.putIfAbsent(townName, new HashMap<>());
        Map<String, List<ItemStack>> townCatMap = Main.townIncome.get(townName);
        
        townCatMap.putIfAbsent(category, new ArrayList<>());
        List<ItemStack> items = townCatMap.get(category);

        for (ItemStack existing : items) {
            if (existing == null) continue;  // Fix for null ItemStack in list
            if (existing.isSimilar(item)) {
                int space = existing.getMaxStackSize() - existing.getAmount();
                if (space > 0) {
                    int toAdd = Math.min(space, item.getAmount());
                    existing.setAmount(existing.getAmount() + toAdd);
                    item.setAmount(item.getAmount() - toAdd);
                }
            }
            if (item.getAmount() <= 0) break;
        }

        if (item.getAmount() > 0) {
            if (items.size() < 8) {
                items.add(item);
            } else {
                // Fix for silent item loss - log when storage is full
                Bukkit.getLogger().warning("[TownCore] Income item dropped for " + townName 
                    + ": storage full. Item: " + item.getType() + " x" + item.getAmount());
            }
        }
    }
}
