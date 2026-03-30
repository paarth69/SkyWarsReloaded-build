package com.walrusone.skywarsreloaded.managers.chests;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

/**
 * Represents a single loot item that can appear in a chest.
 * Format: material,chance,amount[,name:valor][,lore:linea1|linea2][,enchants:enchant-level|...]
 */
public class LootItem {

    private final Material material;
    private final int chance;
    private final int minAmount;
    private final int maxAmount;
    private final String customName;
    private final List<String> lore;
    private final Map<Enchantment, Integer> enchantments;
    private final List<String> nbtComponents;

    private static final Random random = new Random();

    public LootItem(Material material, int chance, int minAmount, int maxAmount,
                    String customName, List<String> lore, Map<Enchantment, Integer> enchantments,
                    List<String> nbtComponents) {
        this.material = material;
        this.chance = chance;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.customName = customName;
        this.lore = lore != null ? lore : new ArrayList<>();
        this.enchantments = enchantments != null ? enchantments : new HashMap<>();
        this.nbtComponents = nbtComponents != null ? nbtComponents : new ArrayList<>();
    }

    /**
     * Parses a loot item from string format.
     * Format: material,chance,amount[,name:valor][,lore:linea1|linea2][,enchants:enchant-level|...][,nbt:component1|component2]
     * Examples:
     *   - bread,30,10-20
     *   - diamond_sword,10,1,enchants:sharpness-2|unbreaking-1
     *   - iron_helmet,50,1,name:&aCasco,lore:&7Linea1|&7Linea2
     *   - iron_sword,50,1,name:"&aEspada de Hierro",lore:"&7Una espada|&7muy buena"
     *   - splash_potion,45,1,nbt:potion_contents={potion:"minecraft:strong_healing"}
     *   - tipped_arrow,30,8,nbt:potion_contents={potion:"minecraft:poison"}
     *
     * Name and lore support quotes for spaces: name:"&aHello World",lore:"&7Line 1|&7Line 2"
     * NBT format: component_name={data} (use = instead of :)
     * Multiple NBT components separated by |
     * Note: nbt: must be the last parameter as it can contain commas and spaces.
     */
    public static LootItem parse(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        // Check if there's an NBT tag - it must be at the end and can contain commas
        String nbtData = null;
        String mainPart = line;
        int nbtIndex = line.indexOf(",nbt:");
        if (nbtIndex != -1) {
            nbtData = line.substring(nbtIndex + 5); // Skip ",nbt:"
            mainPart = line.substring(0, nbtIndex);
        }

        // Split by comma but respect quotes
        List<String> parts = splitRespectingQuotes(mainPart);
        if (parts.size() < 3) {
            return null;
        }

        // Parse material
        Material material = Material.matchMaterial(parts.get(0).trim().toUpperCase());
        if (material == null) {
            return null;
        }

        // Parse chance
        int chance;
        try {
            chance = Integer.parseInt(parts.get(1).trim());
        } catch (NumberFormatException e) {
            return null;
        }

        // Parse amount (can be "10" or "10-20")
        int minAmount;
        int maxAmount;
        String amountStr = parts.get(2).trim();
        if (amountStr.contains("-")) {
            String[] amountParts = amountStr.split("-");
            try {
                minAmount = Integer.parseInt(amountParts[0].trim());
                maxAmount = Integer.parseInt(amountParts[1].trim());
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            try {
                minAmount = maxAmount = Integer.parseInt(amountStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // Parse optional parameters
        String customName = null;
        List<String> lore = new ArrayList<>();
        Map<Enchantment, Integer> enchantments = new HashMap<>();
        List<String> nbtComponents = new ArrayList<>();

        for (int i = 3; i < parts.size(); i++) {
            String part = parts.get(i).trim();

            if (part.startsWith("name:")) {
                String nameValue = part.substring(5);
                // Remove surrounding quotes if present
                nameValue = removeQuotes(nameValue);
                customName = ChatColor.translateAlternateColorCodes('&', nameValue);
            } else if (part.startsWith("lore:")) {
                String loreStr = part.substring(5);
                // Remove surrounding quotes if present
                loreStr = removeQuotes(loreStr);
                for (String loreLine : loreStr.split("\\|")) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', loreLine));
                }
            } else if (part.startsWith("enchants:")) {
                String enchantsStr = part.substring(9);
                for (String enchantPart : enchantsStr.split("\\|")) {
                    String[] enchantData = enchantPart.split("-");
                    if (enchantData.length == 2) {
                        Enchantment enchant = getEnchantment(enchantData[0].trim());
                        if (enchant != null) {
                            try {
                                int level = Integer.parseInt(enchantData[1].trim());
                                enchantments.put(enchant, level);
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            }
        }

        // Parse NBT components (pipe-separated)
        if (nbtData != null && !nbtData.trim().isEmpty()) {
            for (String component : nbtData.split("\\|")) {
                String trimmed = component.trim();
                if (!trimmed.isEmpty()) {
                    nbtComponents.add(trimmed);
                }
            }
        }

        return new LootItem(material, chance, minAmount, maxAmount, customName, lore, enchantments, nbtComponents);
    }

    /**
     * Splits a string by comma, but respects quoted sections.
     * Example: 'a,b,"c,d",e' -> ['a', 'b', '"c,d"', 'e']
     */
    private static List<String> splitRespectingQuotes(String input) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        // Add the last part
        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }

    /**
     * Removes surrounding quotes from a string if present.
     */
    private static String removeQuotes(String input) {
        if (input != null && input.length() >= 2 && input.startsWith("\"") && input.endsWith("\"")) {
            return input.substring(1, input.length() - 1);
        }
        return input;
    }

    /**
     * Creates an ItemStack from this loot item with randomized amount.
     */
    public ItemStack createItemStack() {
        int amount = minAmount == maxAmount ? minAmount : random.nextInt(maxAmount - minAmount + 1) + minAmount;
        ItemStack item = new ItemStack(material, amount);

        // Apply NBT components first (before other modifications)
        if (!nbtComponents.isEmpty()) {
            item = applyNbtComponents(item);
        }

        // Apply enchantments
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            item.addUnsafeEnchantment(entry.getKey(), entry.getValue());
        }

        // Apply custom name and lore
        if (customName != null || !lore.isEmpty()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (customName != null) {
                    meta.setDisplayName(customName);
                }
                if (!lore.isEmpty()) {
                    meta.setLore(lore);
                }
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    /**
     * Applies NBT components to an ItemStack using Paper's ItemFactory.
     * Format for components: component_name={data}
     * Example: potion_contents={potion:"minecraft:strong_healing"}
     *
     * Uses the format: minecraft:item[component1=data1,component2=data2]
     */
    private ItemStack applyNbtComponents(ItemStack item) {
        try {
            // Build the item string with components
            // Format: minecraft:material[component1=data1,component2=data2]
            StringBuilder itemBuilder = new StringBuilder();
            itemBuilder.append("minecraft:").append(material.getKey().getKey());
            itemBuilder.append("[");

            boolean first = true;
            for (String component : nbtComponents) {
                if (!first) {
                    itemBuilder.append(",");
                }
                first = false;
                itemBuilder.append(component);
            }
            itemBuilder.append("]");

            String itemString = itemBuilder.toString();

            // Use Paper's ItemFactory to create item from string
            ItemStack modifiedItem = Bukkit.getItemFactory().createItemStack(itemString);
            if (modifiedItem != null) {
                modifiedItem.setAmount(item.getAmount());
                return modifiedItem;
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[SkyWarsReloaded] Failed to apply NBT components to item '"
                + material.getKey().getKey() + "': " + e.getMessage());
        }
        return item;
    }

    /**
     * Gets an enchantment by name using the modern Registry system.
     */
    private static Enchantment getEnchantment(String name) {
        String normalizedName = name.toLowerCase().replace(" ", "_");

        // Try direct registry lookup
        Enchantment enchant = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(normalizedName));
        if (enchant != null) {
            return enchant;
        }

        // Try common aliases
        String mappedName = switch (normalizedName) {
            case "protection" -> "protection";
            case "fire_protection" -> "fire_protection";
            case "feather_falling" -> "feather_falling";
            case "blast_protection" -> "blast_protection";
            case "projectile_protection" -> "projectile_protection";
            case "respiration" -> "respiration";
            case "aqua_affinity" -> "aqua_affinity";
            case "thorns" -> "thorns";
            case "sharpness" -> "sharpness";
            case "smite" -> "smite";
            case "bane_of_arthropods" -> "bane_of_arthropods";
            case "knockback" -> "knockback";
            case "fire_aspect" -> "fire_aspect";
            case "looting" -> "looting";
            case "efficiency" -> "efficiency";
            case "silk_touch" -> "silk_touch";
            case "unbreaking", "durability" -> "unbreaking";
            case "fortune" -> "fortune";
            case "power" -> "power";
            case "punch" -> "punch";
            case "flame" -> "flame";
            case "infinity" -> "infinity";
            case "luck_of_the_sea" -> "luck_of_the_sea";
            case "lure" -> "lure";
            case "mending" -> "mending";
            case "sweeping", "sweeping_edge" -> "sweeping_edge";
            case "depth_strider" -> "depth_strider";
            case "frost_walker" -> "frost_walker";
            case "soul_speed" -> "soul_speed";
            case "swift_sneak" -> "swift_sneak";
            case "riptide" -> "riptide";
            case "channeling" -> "channeling";
            case "impaling" -> "impaling";
            case "loyalty" -> "loyalty";
            case "multishot" -> "multishot";
            case "piercing" -> "piercing";
            case "quick_charge" -> "quick_charge";
            default -> null;
        };

        if (mappedName != null) {
            return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(mappedName));
        }

        return null;
    }

    // Getters

    public Material getMaterial() {
        return material;
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

    public String getCustomName() {
        return customName;
    }

    public List<String> getLore() {
        return lore;
    }

    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }

    public List<String> getNbtComponents() {
        return nbtComponents;
    }
}
