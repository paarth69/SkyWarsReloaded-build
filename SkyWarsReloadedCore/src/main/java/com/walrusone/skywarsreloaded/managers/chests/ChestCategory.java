package com.walrusone.skywarsreloaded.managers.chests;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Represents a chest category with a chance to be selected and item types.
 * When a chest is filled, first a category is selected based on chance,
 * then items are selected from each type based on their percentage.
 */
public class ChestCategory {

    private final String name;
    private final int chance;
    private final int minAmount;
    private final int maxAmount;
    private final List<ChestItemType> itemTypes;

    private static final Random random = new Random();

    public ChestCategory(String name, int chance, int minAmount, int maxAmount, List<ChestItemType> itemTypes) {
        this.name = name;
        this.chance = chance;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.itemTypes = itemTypes != null ? itemTypes : new ArrayList<>();
    }

    /**
     * Generates a random amount of items based on the min/max range.
     *
     * @return Random amount between minAmount and maxAmount (inclusive)
     */
    public int rollAmount() {
        if (minAmount == maxAmount) {
            return minAmount;
        }
        return random.nextInt(maxAmount - minAmount + 1) + minAmount;
    }

    /**
     * Generates all items for this category.
     * Rolls the total amount, then distributes items across all types based on their percentages.
     * Each type is guaranteed at least 1 item. Percentages are normalized if they don't sum to 100.
     *
     * @return List of ItemStacks to fill the chest with
     */
    public List<ItemStack> generateItems() {
        List<ItemStack> items = new ArrayList<>();

        if (itemTypes.isEmpty()) {
            return items;
        }

        int totalAmount = rollAmount();

        // Calculate total percentage sum
        int totalPercentage = 0;
        for (ChestItemType type : itemTypes) {
            totalPercentage += type.getPercentage();
        }

        // First pass: guarantee minimum 1 item per type
        int guaranteedItems = itemTypes.size();
        int remainingItems = Math.max(0, totalAmount - guaranteedItems);

        // Distribute items: each type gets 1 guaranteed + proportional share of remaining
        int itemsAssigned = 0;
        for (int i = 0; i < itemTypes.size(); i++) {
            ChestItemType type = itemTypes.get(i);
            int itemCount;

            if (i == itemTypes.size() - 1) {
                // Last type gets all remaining to ensure exact total
                itemCount = totalAmount - itemsAssigned;
            } else {
                // 1 guaranteed + proportional share of remaining items
                int proportionalShare = 0;
                if (remainingItems > 0 && totalPercentage > 0) {
                    proportionalShare = (int) Math.round((double) type.getPercentage() / totalPercentage * remainingItems);
                }
                itemCount = 1 + proportionalShare;
                itemsAssigned += itemCount;
            }

            // Ensure at least 1 item per type
            itemCount = Math.max(1, itemCount);
            items.addAll(type.selectItems(itemCount));
        }

        return items;
    }

    // Getters

    public String getName() {
        return name;
    }

    public int getChance() {
        return chance;
    }

    public int getMinAmount() {
        return minAmount;
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    public List<ChestItemType> getItemTypes() {
        return itemTypes;
    }
}
