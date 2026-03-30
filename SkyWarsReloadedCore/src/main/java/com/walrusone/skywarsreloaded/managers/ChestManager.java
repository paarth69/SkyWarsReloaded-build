package com.walrusone.skywarsreloaded.managers;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.managers.chests.ChestLootTable;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.Inventory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Manages chest loot tables using the category-based system.
 * All loot tables are loaded from the chests/ directory.
 */
public class ChestManager {

    private final Map<String, ChestLootTable> lootTables = new HashMap<>();

    public ChestManager() {
        loadLootTables();
    }

    /**
     * Loads all YAML files from the chests/ directory.
     */
    private void loadLootTables() {
        File chestsDir = new File(SkyWarsReloaded.get().getDataFolder(), "chests");

        if (!chestsDir.exists()) {
            chestsDir.mkdirs();
            saveDefaultFiles();
        }

        File[] files = chestsDir.listFiles((dir, name) -> name.endsWith(".yaml") || name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                String tableName = fileName.replace(".yaml", "").replace(".yml", "");
                ChestLootTable table = new ChestLootTable(fileName);
                lootTables.put(tableName, table);
                SkyWarsReloaded.get().getLogger().log(Level.INFO, "Loaded loot table: " + fileName);
            }
        }

        if (lootTables.isEmpty()) {
            SkyWarsReloaded.get().getLogger().log(Level.WARNING, "No loot tables found in chests/ directory!");
        }
    }

    /**
     * Saves default loot table files from resources.
     */
    private void saveDefaultFiles() {
        String[] defaultFiles = {
            "chests/basicchest.yml",
            "chests/basiccenterchest.yml",
            "chests/normalchest.yml",
            "chests/normalcenterchest.yml",
            "chests/opchest.yml",
            "chests/opcenterchest.yml"
        };

        for (String resourcePath : defaultFiles) {
            try {
                SkyWarsReloaded.get().saveResource(resourcePath, false);
            } catch (Exception e) {
                // File doesn't exist in resources, skip
            }
        }
    }

    /**
     * Reloads all loot tables.
     */
    public void reload() {
        lootTables.clear();
        loadLootTables();
    }

    /**
     * Gets a loot table by name (without extension).
     *
     * @param name The loot table name
     * @return The loot table, or null if not found
     */
    public ChestLootTable getLootTable(String name) {
        return lootTables.get(name);
    }

    /**
     * Gets all loaded loot table names.
     *
     * @return Map of loot table names to their tables
     */
    public Map<String, ChestLootTable> getLootTables() {
        return lootTables;
    }

    /**
     * Fills a chest using a specific loot table.
     *
     * @param chest The chest to fill (Chest or DoubleChest)
     * @param tableName The name of the loot table (without extension)
     */
    public void fillChest(Object chest, String tableName) {
        ChestLootTable table = lootTables.get(tableName);
        if (table != null) {
            table.fillChest(chest);
        } else {
            SkyWarsReloaded.get().getLogger().log(Level.WARNING,
                "Loot table not found: " + tableName + ". Clearing chest.");
            clearChest(chest);
        }
    }

    /**
     * Fills a crate inventory using a specific loot table.
     *
     * @param inventory The inventory to fill
     * @param tableName The name of the loot table
     * @param maxItems Maximum number of items
     */
    public void fillCrate(Inventory inventory, String tableName, int maxItems) {
        ChestLootTable table = lootTables.get(tableName);
        if (table != null) {
            table.fillCrate(inventory, maxItems);
        } else {
            // Fallback to default crate table
            table = lootTables.get("crates");
            if (table != null) {
                table.fillCrate(inventory, maxItems);
            } else if (!lootTables.isEmpty()) {
                // Use first available table as last resort
                lootTables.values().iterator().next().fillCrate(inventory, maxItems);
            }
        }
    }

    /**
     * Fills a crate using the default crates loot table.
     *
     * @param inventory The inventory to fill
     * @param maxItems Maximum number of items
     */
    public void fillCrate(Inventory inventory, int maxItems) {
        fillCrate(inventory, "crates", maxItems);
    }

    /**
     * Clears a chest inventory.
     *
     * @param chest The chest to clear
     */
    public void clearChest(Object chest) {
        Inventory inventory = null;
        if (chest instanceof Chest) {
            inventory = ((Chest) chest).getInventory();
        } else if (chest instanceof DoubleChest) {
            inventory = ((DoubleChest) chest).getInventory();
        }
        if (inventory != null) {
            inventory.clear();
        }
    }

    /**
     * Checks if a loot table exists.
     *
     * @param name The loot table name
     * @return true if the table exists
     */
    public boolean hasLootTable(String name) {
        return lootTables.containsKey(name);
    }

    /**
     * Gets all available loot table names.
     *
     * @return Set of loot table names
     */
    public Set<String> getAvailableLootTables() {
        return lootTables.keySet();
    }
}
