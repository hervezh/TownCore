package com.silvarys;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.HashMap;
import java.util.Map;

public class TownLevelListener implements Listener {

    private static final Map<Long, Long> blockPlaceTime = new HashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        String town = Main.playerTown.get(player.getUniqueId());

        if (town == null) return;

        long key = getBlockKey(event.getBlock());
        blockPlaceTime.put(key, System.currentTimeMillis());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String town = Main.playerTown.get(player.getUniqueId());

        if (town == null) return;

        Block block = event.getBlock();
        long key = getBlockKey(block);

        Long placeTime = blockPlaceTime.get(key);

        if (placeTime != null) {
            long elapsed = System.currentTimeMillis() - placeTime;

            if (elapsed >= 5 * 60 * 1000L) {
                TownLevelManager.addXP(town, "building", 1);
            }

            blockPlaceTime.remove(key);
            return;
        }

        Material type = block.getType();

        if (isOre(type)) {
            TownLevelManager.addXP(town, "mining", 1);
            return;
        }

        if (isLog(type)) {
            TownLevelManager.addXP(town, "woodcutting", 1);
            return;
        }

        if (isMatureCrop(type)) {
            TownLevelManager.addXP(town, "farming", 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerHitPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Player)) return;

        String town = Main.playerTown.get(player.getUniqueId());

        if (town == null) return;

        TownLevelManager.addXP(town, "pvp", 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player player)) return;
        if (event.getEntity() instanceof Player) return;

        String town = Main.playerTown.get(player.getUniqueId());

        if (town == null) return;

        TownLevelManager.addXP(town, "pve", 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        String town = Main.playerTown.get(player.getUniqueId());

        if (town == null) return;

        if (isFood(event.getItemType())) {
            TownLevelManager.addXP(town, "cooking", 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String town = Main.playerTown.get(player.getUniqueId());

        if (town == null) return;

        Material result = event.getRecipe().getResult().getType();

        if (isDiamondOrIronGear(result)) {
            TownLevelManager.addXP(town, "smithing", 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = event.getPlayer();
        String town = Main.playerTown.get(player.getUniqueId());

        if (town == null) return;

        TownLevelManager.addXP(town, "fishing", 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        String town = Main.playerTown.get(player.getUniqueId());

        if (town == null) return;

        TownLevelManager.addXP(town, "enchanting", 1);
    }

    private long getBlockKey(Block block) {
        return ((long) block.getWorld().getName().hashCode() << 32)
                ^ ((long) block.getX() & 0xFFFFFL) << 20
                ^ ((long) block.getY() & 0xFFFL) << 8
                ^ ((long) block.getZ() & 0xFFFFFL);
    }

    private boolean isOre(Material m) {
        return m == Material.COAL_ORE || m == Material.DEEPSLATE_COAL_ORE ||
                m == Material.IRON_ORE || m == Material.DEEPSLATE_IRON_ORE ||
                m == Material.GOLD_ORE || m == Material.DEEPSLATE_GOLD_ORE ||
                m == Material.DIAMOND_ORE || m == Material.DEEPSLATE_DIAMOND_ORE ||
                m == Material.EMERALD_ORE || m == Material.DEEPSLATE_EMERALD_ORE ||
                m == Material.LAPIS_ORE || m == Material.DEEPSLATE_LAPIS_ORE ||
                m == Material.REDSTONE_ORE || m == Material.DEEPSLATE_REDSTONE_ORE ||
                m == Material.COPPER_ORE || m == Material.DEEPSLATE_COPPER_ORE ||
                m == Material.NETHER_GOLD_ORE || m == Material.NETHER_QUARTZ_ORE ||
                m == Material.ANCIENT_DEBRIS;
    }

    private boolean isLog(Material m) {
        String name = m.name();

        return name.endsWith("_LOG") ||
                name.endsWith("_WOOD") ||
                name.endsWith("_STEM") ||
                name.endsWith("_HYPHAE");
    }

    private boolean isMatureCrop(Material m) {
        return m == Material.WHEAT || m == Material.CARROTS ||
                m == Material.POTATOES || m == Material.BEETROOTS ||
                m == Material.MELON || m == Material.PUMPKIN ||
                m == Material.SUGAR_CANE || m == Material.BAMBOO;
    }

    private boolean isFood(Material m) {
        return m == Material.COOKED_BEEF || m == Material.COOKED_CHICKEN ||
                m == Material.COOKED_PORKCHOP || m == Material.COOKED_MUTTON ||
                m == Material.COOKED_RABBIT || m == Material.COOKED_SALMON ||
                m == Material.COOKED_COD || m == Material.BREAD ||
                m == Material.BAKED_POTATO;
    }

    private boolean isDiamondOrIronGear(Material m) {
        String name = m.name();

        return (name.startsWith("DIAMOND_") || name.startsWith("IRON_")) &&
                (name.endsWith("_SWORD") || name.endsWith("_AXE") ||
                        name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") ||
                        name.endsWith("_HOE") || name.endsWith("_HELMET") ||
                        name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") ||
                        name.endsWith("_BOOTS"));
    }
}