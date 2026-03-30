package com.walrusone.skywarsreloaded.commands.maps;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.commands.BaseCmd;
import com.walrusone.skywarsreloaded.managers.ChestManager;
import com.walrusone.skywarsreloaded.managers.chests.ChestLootTable;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class TestLootCmd extends BaseCmd {

    public TestLootCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "testloot";
        alias = new String[]{"tl"};
        argLength = 2;
    }

    @Override
    public boolean run(CommandSender sender, Player player, String[] args) {
        String lootTableName = args[1].toLowerCase();

        ChestManager chestManager = SkyWarsReloaded.getCM();
        if (chestManager == null) {
            sender.sendMessage("§cChestManager is not initialized!");
            return true;
        }

        ChestLootTable lootTable = chestManager.getLootTable(lootTableName);
        if (lootTable == null) {
            Set<String> available = chestManager.getAvailableLootTables();
            sender.sendMessage("§cLoot table '§e" + lootTableName + "§c' not found!");
            sender.sendMessage("§7Available loot tables: §f" + String.join(", ", available));
            return true;
        }

        // Create a test inventory and fill it with loot
        Inventory testInventory = SkyWarsReloaded.get().getServer().createInventory(null, 27, "§6Test Loot: §e" + lootTableName);

        // Fill the chest using the loot table
        lootTable.fillChest(testInventory);

        // Open the inventory for the player
        player.openInventory(testInventory);

        sender.sendMessage("§aShowing random loot from '§e" + lootTableName + "§a'");

        return true;
    }
}
