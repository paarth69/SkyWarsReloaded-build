package com.walrusone.skywarsreloaded.managers.spectatorItems;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemUtil {

    private ItemUtil() {}

    public static ItemStack named(Material material, String displayName) {
        ItemStack it = new ItemStack(material);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            it.setItemMeta(meta);
        }
        return it;
    }
}
