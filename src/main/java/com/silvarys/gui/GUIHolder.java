package com.silvarys.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Base marker class for all custom GUI holders in Town Core.
 * Using custom InventoryHolder subclasses lets us identify our GUIs
 * by type-checking instead of fragile title-string matching.
 */
public abstract class GUIHolder implements InventoryHolder {
    private Inventory inventory;

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    // --- Concrete marker subclasses ---

    /** Main town menu (with-town or no-town variant). */
    public static class TownMain extends GUIHolder {}

    /** Staff panel menu. */
    public static class TownStaff extends GUIHolder {}

    /** Town bank menu. */
    public static class TownBank extends GUIHolder {}

    /** Town settings menu. */
    public static class TownSettings extends GUIHolder {}

    /** Town upgrades menu. */
    public static class TownUpgrades extends GUIHolder {}

    /** Town income menu. */
    public static class TownIncome extends GUIHolder {}

    /** Town info menu. */
    public static class TownInfo extends GUIHolder {
        private final String townName;
        public TownInfo(String townName) { this.townName = townName; }
        public String getTownName() { return townName; }
    }

    /** Town list (paginated). */
    public static class TownList extends GUIHolder {
        private final int page;
        public TownList(int page) { this.page = page; }
        public int getPage() { return page; }
    }

    /** Town leaderboard / top GUI. */
    public static class TownTop extends GUIHolder {}
}
