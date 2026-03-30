package com.walrusone.skywarsreloaded.managers.chests;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

/**
 * Manages loading and using chest loot tables from YAML configuration files.
 * Handles the new category-based loot system.
 */
public class ChestLootTable {

    private final String fileName;
    private final List<ChestCategory> categories;
    private int totalChance;

    private final List<Integer> randomSlots;
    private final List<Integer> randomDoubleSlots;

    private static final Random random = new Random();

    public ChestLootTable(String fileName) {
        this.fileName = fileName;
        this.categories = new ArrayList<>();
        this.totalChance = 0;

        // Initialize slot lists for randomization
        this.randomSlots = new ArrayList<>();
        this.randomDoubleSlots = new ArrayList<>();
        for (int i = 0; i < 27; i++) {
            randomSlots.add(i);
        }
        for (int i = 0; i < 54; i++) {
            randomDoubleSlots.add(i);
        }

        load();
    }

    /**
     * Loads the loot table from the YAML file.
     */
    public void load() {
        categories.clear();
        totalChance = 0;

        File file = new File(SkyWarsReloaded.get().getDataFolder(), "chests/" + fileName);

        if (!file.exists()) {
            // Try to save from resources
            File chestDir = new File(SkyWarsReloaded.get().getDataFolder(), "chests");
            if (!chestDir.exists()) {
                chestDir.mkdirs();
            }
            try {
                SkyWarsReloaded.get().saveResource("chests/" + fileName, false);
            } catch (Exception e) {
                SkyWarsReloaded.get().getLogger().log(Level.WARNING,
                    "Could not save default chest file: chests/" + fileName);
                return;
            }
        }

        if (!file.exists()) {
            SkyWarsReloaded.get().getLogger().log(Level.WARNING,
                "Chest loot file not found: chests/" + fileName);
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection categoriesSection = config.getConfigurationSection("categories");

        if (categoriesSection == null) {
            SkyWarsReloaded.get().getLogger().log(Level.WARNING,
                "No 'categories' section found in: chests/" + fileName);
            return;
        }

        for (String categoryName : categoriesSection.getKeys(false)) {
            ConfigurationSection catSection = categoriesSection.getConfigurationSection(categoryName);
            if (catSection == null) continue;

            ChestCategory category = parseCategory(categoryName, catSection);
            if (category != null) {
                categories.add(category);
                totalChance += category.getChance();
            }
        }

        SkyWarsReloaded.get().getLogger().log(Level.INFO,
            "Loaded " + categories.size() + " categories from chests/" + fileName);
    }

    /**
     * Parses a category from a configuration section.
     */
    private ChestCategory parseCategory(String name, ConfigurationSection section) {
        int chance = section.getInt("chance", 100);

        // Parse amount range (e.g., "6-12" or just "6")
        String amountStr = section.getString("amount", "6");
        int minAmount;
        int maxAmount;

        if (amountStr.contains("-")) {
            String[] parts = amountStr.split("-");
            try {
                minAmount = Integer.parseInt(parts[0].trim());
                maxAmount = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                minAmount = maxAmount = 6;
            }
        } else {
            try {
                minAmount = maxAmount = Integer.parseInt(amountStr.trim());
            } catch (NumberFormatException e) {
                minAmount = maxAmount = 6;
            }
        }

        // Parse item types
        List<ChestItemType> itemTypes = new ArrayList<>();
        ConfigurationSection itemsSection = section.getConfigurationSection("items");

        if (itemsSection != null) {
            for (String typeName : itemsSection.getKeys(false)) {
                ConfigurationSection typeSection = itemsSection.getConfigurationSection(typeName);
                if (typeSection == null) continue;

                ChestItemType itemType = parseItemType(typeName, typeSection);
                if (itemType != null) {
                    itemTypes.add(itemType);
                }
            }
        }

        return new ChestCategory(name, chance, minAmount, maxAmount, itemTypes);
    }

    /**
     * Parses an item type from a configuration section.
     */
    private ChestItemType parseItemType(String name, ConfigurationSection section) {
        int percentage = section.getInt("percentage", 25);
        List<String> lootStrings = section.getStringList("loot");

        List<LootItem> lootItems = new ArrayList<>();
        for (String lootStr : lootStrings) {
            LootItem item = LootItem.parse(lootStr);
            if (item != null) {
                lootItems.add(item);
            } else {
                SkyWarsReloaded.get().getLogger().log(Level.WARNING,
                    "Failed to parse loot item: " + lootStr + " in " + fileName);
            }
        }

        return new ChestItemType(name, percentage, lootItems);
    }

    /**
     * Selects a random category based on weighted chance.
     *
     * @return The selected category, or null if no categories exist
     */
    public ChestCategory selectCategory() {
        if (categories.isEmpty() || totalChance <= 0) {
            return null;
        }

        int roll = random.nextInt(totalChance) + 1;
        int cumulative = 0;

        for (ChestCategory category : categories) {
            cumulative += category.getChance();
            if (roll <= cumulative) {
                return category;
            }
        }

        // Fallback to last category
        return categories.get(categories.size() - 1);
    }

    /**
     * Fills a chest with random loot based on the loaded categories.
     *
     * @param chest The chest to fill (Chest or DoubleChest)
     */
    public void fillChest(Object chest) {
        Inventory inventory = null;
        boolean isDouble = false;

        if (chest instanceof Chest) {
            inventory = ((Chest) chest).getInventory();
        } else if (chest instanceof DoubleChest) {
            inventory = ((DoubleChest) chest).getInventory();
            isDouble = true;
        }

        if (inventory == null) {
            return;
        }

        inventory.clear();

        // Select a category
        ChestCategory category = selectCategory();
        if (category == null) {
            return;
        }

        // Generate items from the category
        List<ItemStack> items = category.generateItems();

        // Shuffle slot positions
        List<Integer> slots = isDouble ? new ArrayList<>(randomDoubleSlots) : new ArrayList<>(randomSlots);
        Collections.shuffle(slots);

        // Place items in random slots
        int slotIndex = 0;
        int maxSlots = inventory.getSize();

        for (ItemStack item : items) {
            if (slotIndex >= maxSlots || slotIndex >= slots.size()) {
                break;
            }
            inventory.setItem(slots.get(slotIndex), item);
            slotIndex++;
        }
    }

    /**
     * Fills an inventory directly with random loot.
     * This is useful for testing or custom inventories.
     *
     * @param inventory The inventory to fill
     */
    public void fillChest(Inventory inventory) {
        if (inventory == null) {
            return;
        }

        inventory.clear();

        ChestCategory category = selectCategory();
        if (category == null) {
            return;
        }

        List<ItemStack> items = category.generateItems();

        List<Integer> slots = new ArrayList<>(randomSlots);
        Collections.shuffle(slots);

        int slotIndex = 0;
        int maxSlots = inventory.getSize();

        for (ItemStack item : items) {
            if (slotIndex >= maxSlots || slotIndex >= slots.size()) {
                break;
            }
            inventory.setItem(slots.get(slotIndex), item);
            slotIndex++;
        }
    }

    /**
     * Fills a crate inventory with random loot.
     *
     * @param inventory The inventory to fill
     * @param maxItems Maximum number of items to add
     */
    public void fillCrate(Inventory inventory, int maxItems) {
        if (inventory == null) {
            return;
        }

        inventory.clear();

        ChestCategory category = selectCategory();
        if (category == null) {
            return;
        }

        List<ItemStack> items = category.generateItems();

        List<Integer> slots = new ArrayList<>(randomSlots);
        Collections.shuffle(slots);

        int slotIndex = 0;
        int added = 0;

        for (ItemStack item : items) {
            if (added >= maxItems || slotIndex >= slots.size() || slotIndex >= inventory.getSize()) {
                break;
            }
            inventory.setItem(slots.get(slotIndex), item);
            slotIndex++;
            added++;
        }
    }

    // Getters

    public String getFileName() {
        return fileName;
    }

    public List<ChestCategory> getCategories() {
        return categories;
    }

    public int getTotalChance() {
        return totalChance;
    }
}
