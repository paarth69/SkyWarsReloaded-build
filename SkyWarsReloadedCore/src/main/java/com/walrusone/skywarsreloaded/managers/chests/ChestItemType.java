package com.walrusone.skywarsreloaded.managers.chests;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Represents a type of items within a chest category.
 * Each type has a percentage of the total items and a list of possible loot items.
 */
public class ChestItemType {

    private final String name;
    private final int percentage;
    private final List<LootItem> lootItems;

    private static final Random random = new Random();

    public ChestItemType(String name, int percentage, List<LootItem> lootItems) {
        this.name = name;
        this.percentage = percentage;
        this.lootItems = lootItems != null ? lootItems : new ArrayList<>();
    }

    /**
     * Selects random items from this type based on the given count.
     * Uses weighted random selection where 'chance' determines relative rarity.
     * Always guarantees exactly 'count' items will be selected.
     *
     * @param count The number of items to select
     * @return List of selected ItemStacks
     */
    public List<ItemStack> selectItems(int count) {
        List<ItemStack> selected = new ArrayList<>();

        if (lootItems.isEmpty() || count <= 0) {
            return selected;
        }

        // Calculate total weight (sum of all chances)
        int totalWeight = 0;
        for (LootItem item : lootItems) {
            totalWeight += item.getChance();
        }

        if (totalWeight <= 0) {
            // Fallback: if all chances are 0, select randomly with equal weight
            for (int i = 0; i < count; i++) {
                LootItem lootItem = lootItems.get(random.nextInt(lootItems.size()));
                selected.add(lootItem.createItemStack());
            }
            return selected;
        }

        // Select 'count' items using weighted random selection
        for (int i = 0; i < count; i++) {
            int roll = random.nextInt(totalWeight) + 1;
            int cumulative = 0;

            for (LootItem lootItem : lootItems) {
                cumulative += lootItem.getChance();
                if (roll <= cumulative) {
                    selected.add(lootItem.createItemStack());
                    break;
                }
            }
        }

        return selected;
    }

    /**
     * Calculates how many items this type should contribute based on the total amount.
     *
     * @param totalAmount The total number of items for the chest
     * @return The number of items this type should contribute
     */
    public int calculateItemCount(int totalAmount) {
        return (int) Math.round((double) totalAmount * percentage / 100.0);
    }

    // Getters

    public String getName() {
        return name;
    }

    public int getPercentage() {
        return percentage;
    }

    public List<LootItem> getLootItems() {
        return lootItems;
    }
}
