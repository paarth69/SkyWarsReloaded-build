package com.walrusone.skywarsreloaded.menus.gameoptions;

import com.google.common.collect.Lists;
import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.config.CustomChestType;
import com.walrusone.skywarsreloaded.enums.MatchState;
import com.walrusone.skywarsreloaded.enums.ScoreVar;
import com.walrusone.skywarsreloaded.enums.Vote;
import com.walrusone.skywarsreloaded.events.SkyWarsVoteEvent;
import com.walrusone.skywarsreloaded.game.GameMap;
import com.walrusone.skywarsreloaded.game.PlayerCard;
import com.walrusone.skywarsreloaded.managers.MatchManager;
import com.walrusone.skywarsreloaded.menus.IconMenu;
import com.walrusone.skywarsreloaded.menus.gameoptions.objects.CoordLoc;
import com.walrusone.skywarsreloaded.utilities.Messaging;
import com.walrusone.skywarsreloaded.utilities.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChestOption extends GameOption {

    private boolean useCustom;
    private List<CustomChestType> customTypes;
    private Map<Integer, Vote> slotToVote = new LinkedHashMap<>();
    private Map<Integer, String> slotToItemKey = new LinkedHashMap<>();
    private Map<Vote, Integer> voteToSlot = new LinkedHashMap<>();
    private int randomSlot;

    public ChestOption(GameMap gameMap, String key) {
        this.gameMap = gameMap;
        this.useCustom = SkyWarsReloaded.getCfg().isUseCustomChestTypes();

        if (useCustom) {
            customTypes = SkyWarsReloaded.getCfg().getCustomChestTypes();
            randomSlot = SkyWarsReloaded.getCfg().getChestVoteRandomSlot();

            // Build itemList and voteList from config (random is always first)
            itemList = new ArrayList<>();
            voteList = new ArrayList<>();
            itemList.add("chestrandom");
            voteList.add(Vote.CHESTRANDOM);

            for (CustomChestType ct : customTypes) {
                String itemKey = "chestcustom_" + ct.getId();
                itemList.add(itemKey);
                voteList.add(ct.getVote());
                slotToVote.put(ct.getSlot(), ct.getVote());
                slotToItemKey.put(ct.getSlot(), itemKey);
                voteToSlot.put(ct.getVote(), ct.getSlot());
            }

            createCustomMenu(key, new Messaging.MessageFormatter().format("menu.chest-voting-menu"));
        } else {
            itemList = Lists.newArrayList("chestrandom", "chestbasic", "chestnormal", "chestop", "chestscavenger");
            voteList = Lists.newArrayList(Vote.CHESTRANDOM, Vote.CHESTBASIC, Vote.CHESTNORMAL, Vote.CHESTOP, Vote.CHESTSCAVENGER);
            createMenu(key, new Messaging.MessageFormatter().format("menu.chest-voting-menu"));
        }
    }

    private void createCustomMenu(String key, String name) {
        this.key = key;
        ArrayList<Inventory> invs = new ArrayList<>();

        int menuSize = SkyWarsReloaded.getCfg().getChestVoteMenuSize();

        Inventory inv = Bukkit.createInventory(null, menuSize, new Messaging.MessageFormatter().format(name));
        inv.clear();

        // Place random item
        if (SkyWarsReloaded.getCfg().isRandomVoteEnabled()) {
            inv.setItem(randomSlot, SkyWarsReloaded.getIM().getItem("chestrandom"));
        }

        // Place custom chest type items
        for (CustomChestType ct : customTypes) {
            String itemKey = "chestcustom_" + ct.getId();
            ItemStack item = SkyWarsReloaded.getIM().getItem(itemKey);
            if (item != null) {
                inv.setItem(ct.getSlot(), item);
            }
        }

        invs.add(inv);

        SkyWarsReloaded.getIC().create(key, invs, event -> {
            String itemName = event.getName();
            if (itemName.equalsIgnoreCase(SkyWarsReloaded.getNMS().getItemName(SkyWarsReloaded.getIM().getItem("exitMenuItem")))) {
                new VotingMenu(event.getPlayer());
                return;
            }
            final GameMap gMap = MatchManager.get().getPlayerMap(event.getPlayer());
            if (!gMap.equals(gameMap)) {
                return;
            }
            if (gameMap.getMatchState() == MatchState.WAITINGSTART || gameMap.getMatchState().equals(MatchState.WAITINGLOBBY)) {
                int slot = event.getSlot();
                if (slot == randomSlot && SkyWarsReloaded.getCfg().isRandomVoteEnabled()) {
                    Vote cVote = Vote.CHESTRANDOM;
                    String type = new Messaging.MessageFormatter().format("items.chest-random");
                    finishEvent(gameMap, event.getPlayer(), cVote, type);
                } else if (slotToVote.containsKey(slot)) {
                    Vote cVote = slotToVote.get(slot);
                    CustomChestType ct = getCustomTypeByVote(cVote);
                    if (ct != null) {
                        String type = org.bukkit.ChatColor.translateAlternateColorCodes('&', ct.getDisplayName());
                        finishEvent(gameMap, event.getPlayer(), cVote, type);
                    }
                }
            }
        });
        iconMenu = SkyWarsReloaded.getIC().getMenu(key);
    }

    private IconMenu iconMenu;

    private CustomChestType getCustomTypeByVote(Vote vote) {
        if (customTypes == null) return null;
        for (CustomChestType ct : customTypes) {
            if (ct.getVote() == vote) return ct;
        }
        return null;
    }

    public List<CustomChestType> getCustomTypes() {
        return customTypes;
    }

    public boolean isUseCustom() {
        return useCustom;
    }

    public Map<Vote, Integer> getVoteToSlot() {
        return voteToSlot;
    }

    // --- Legacy hardcoded slot methods (used when useCustomChestTypes is false) ---

    protected void doSlotNine(Player player) {
        Vote cVote = Vote.CHESTRANDOM;
        String type = new Messaging.MessageFormatter().format("items.chest-random");
        finishEvent(gameMap, player, cVote, type);
    }

    protected void doSlotEleven(Player player) {
        Vote cVote = Vote.CHESTBASIC;
        String type = new Messaging.MessageFormatter().format("items.chest-basic");
        finishEvent(gameMap, player, cVote, type);
    }

    protected void doSlotThriteen(Player player) {
        Vote cVote = Vote.CHESTNORMAL;
        String type = new Messaging.MessageFormatter().format("items.chest-normal");
        finishEvent(gameMap, player, cVote, type);
    }

    protected void doSlotFifteen(Player player) {
        Vote cVote = Vote.CHESTOP;
        String type = new Messaging.MessageFormatter().format("items.chest-op");
        finishEvent(gameMap, player, cVote, type);
    }

    protected void doSlotSeventeen(Player player) {
        Vote cVote = Vote.CHESTSCAVENGER;
        String type = new Messaging.MessageFormatter().format("items.chest-scavenger");
        finishEvent(gameMap, player, cVote, type);
    }

    private void finishEvent(GameMap gameMap, Player player, Vote vote, String type) {
        if (vote != null) {
            setVote(player, vote);
            Bukkit.getPluginManager().callEvent(new SkyWarsVoteEvent(player, gameMap, vote));
            if (useCustom) {
                updateCustomVotes();
            } else {
                updateVotes();
            }
            Util.get().playSound(player, player.getLocation(), SkyWarsReloaded.getCfg().getConfirmeSelctionSound(), 1.0F, 1.0F);
            if (gameMap.getMatchState().equals(MatchState.WAITINGSTART) || gameMap.getMatchState().equals(MatchState.WAITINGLOBBY)) {
                new VotingMenu(player);
            }

            int votes = getVotes(false).getOrDefault(vote, 0);

            MatchManager.get().message(gameMap, new Messaging.MessageFormatter()
                    .setVariable("player", player.getName())
                    .setVariable("chest", type)
                    .setVariable("votes", votes+"").format("game.votechest"));
        }
    }

    private void updateCustomVotes() {
        HashMap<Vote, Integer> votes = getVotes(false);
        Inventory inv = iconMenu.getInventory(0);

        // Update random slot
        if (SkyWarsReloaded.getCfg().isRandomVoteEnabled()) {
            int randomVotes = votes.getOrDefault(Vote.CHESTRANDOM, 0);
            ItemStack item = SkyWarsReloaded.getIM().getItem("chestrandom");
            item.setAmount(randomVotes == 0 ? 1 : randomVotes);
            ItemMeta meta = item.getItemMeta();
            List<String> lores = meta.getLore();
            if (lores == null) lores = new ArrayList<>();
            lores.add(" ");
            lores.add(new Messaging.MessageFormatter().setVariable("number", "" + randomVotes).format("game.vote-display"));
            meta.setLore(lores);
            item.setItemMeta(meta);
            inv.setItem(randomSlot, item);
        }

        // Update custom type slots
        for (CustomChestType ct : customTypes) {
            Vote vote = ct.getVote();
            int voteCount = votes.getOrDefault(vote, 0);
            String itemKey = "chestcustom_" + ct.getId();
            ItemStack item = SkyWarsReloaded.getIM().getItem(itemKey);
            if (item != null) {
                item.setAmount(voteCount == 0 ? 1 : voteCount);
                ItemMeta meta = item.getItemMeta();
                List<String> lores = meta.getLore();
                if (lores == null) lores = new ArrayList<>();
                lores.add(" ");
                lores.add(new Messaging.MessageFormatter().setVariable("number", "" + voteCount).format("game.vote-display"));
                meta.setLore(lores);
                item.setItemMeta(meta);
                inv.setItem(ct.getSlot(), item);
            }
        }
        updateScoreboard();
    }

    public void restore() {
        if (useCustom) {
            restoreCustom();
        } else {
            super.restore();
        }
    }

    private void restoreCustom() {
        Inventory inv = iconMenu.getInventory(0);
        if (SkyWarsReloaded.getCfg().isRandomVoteEnabled()) {
            inv.setItem(randomSlot, SkyWarsReloaded.getIM().getItem("chestrandom"));
        }
        for (CustomChestType ct : customTypes) {
            String itemKey = "chestcustom_" + ct.getId();
            ItemStack item = SkyWarsReloaded.getIM().getItem(itemKey);
            if (item != null) {
                inv.setItem(ct.getSlot(), item);
            }
        }
        updateScoreboard();
    }

    public void setCard(PlayerCard pCard, Vote vote) {
        pCard.setChestVote(vote);
    }

    public Vote getVote(PlayerCard pCard) {
        return pCard.getVote("chest");
    }

    public Vote getRandomVote() {
        return Vote.getRandom("chest");
    }

    protected void updateScoreboard() {
        gameMap.setCurrentChest(getVoteString(getVoted()));
        gameMap.getGameBoard().updateScoreboardVar(ScoreVar.CHESTVOTE);
    }

    protected Vote getDefault() {
        return gameMap.getDefaultChestType();
    }


    public void completeOption() {
        Vote cVote = gameMap.getChestOption().getVoted();

        if (useCustom) {
            CustomChestType ct = getCustomTypeByVote(cVote);
            if (ct != null && !ct.getChestFile().isEmpty()) {
                // Use loot table name from config (without extension)
                String lootTableName = ct.getChestFile().replace(".yaml", "").replace(".yml", "");
                String centerLootTableName = ct.getCenterChestFile().replace(".yaml", "").replace(".yml", "");
                populateChestsWithLootTable(gameMap.getChests(), lootTableName);
                populateChestsWithLootTable(gameMap.getCenterChests(),
                    centerLootTableName.isEmpty() ? lootTableName : centerLootTableName);
            } else if (ct == null) {
                // Fallback for built-in votes (random resolved)
                String tableName = getTableNameFromVote(cVote);
                String centerTableName = getCenterTableNameFromVote(cVote);
                populateChestsWithLootTable(gameMap.getChests(), tableName);
                populateChestsWithLootTable(gameMap.getCenterChests(), centerTableName);
            } else {
                // Scavenger-like type with empty chestFile: clear chests
                clearChests(gameMap.getChests());
                clearChests(gameMap.getCenterChests());
            }
        } else {
            String tableName = getTableNameFromVote(cVote);
            String centerTableName = getCenterTableNameFromVote(cVote);
            populateChestsWithLootTable(gameMap.getChests(), tableName);
            populateChestsWithLootTable(gameMap.getCenterChests(), centerTableName);
        }

        if (SkyWarsReloaded.getCfg().isChestVoteEnabled() && gameMap.getTimer() < 5) {
            String optionValue;
            if (useCustom) {
                CustomChestType ct = getCustomTypeByVote(cVote);
                if (ct != null) {
                    optionValue = org.bukkit.ChatColor.translateAlternateColorCodes('&', ct.getDisplayName());
                } else {
                    String subOptionName = cVote.name().toLowerCase().replace("chest", "chest-");
                    optionValue = SkyWarsReloaded.getMessaging().getMessage("items." + subOptionName);
                }
            } else {
                String subOptionName = cVote.name().toLowerCase().replace("chest", "chest-");
                optionValue = SkyWarsReloaded.getMessaging().getMessage("items." + subOptionName);
            }

            MatchManager.get().message(
                    gameMap,
                    new Messaging.MessageFormatter().setVariable(
                            "type",
                            optionValue
                    ).format("game.vote-announcements.chests"));
        }
    }

    /**
     * Converts a Vote to a loot table name for normal chests.
     */
    private String getTableNameFromVote(Vote vote) {
        return switch (vote) {
            case CHESTBASIC -> "basicchest";
            case CHESTNORMAL -> "normalchest";
            case CHESTOP -> "opchest";
            case CHESTSCAVENGER -> ""; // Empty means clear chests
            default -> "basicchest"; // Default fallback
        };
    }

    /**
     * Converts a Vote to a loot table name for center chests.
     */
    private String getCenterTableNameFromVote(Vote vote) {
        return switch (vote) {
            case CHESTBASIC -> "basiccenterchest";
            case CHESTNORMAL -> "normalcenterchest";
            case CHESTOP -> "opcenterchest";
            case CHESTSCAVENGER -> ""; // Empty means clear chests
            default -> "basiccenterchest"; // Default fallback
        };
    }

    /**
     * Populates chests using the new loot table system.
     */
    private void populateChestsWithLootTable(ArrayList<CoordLoc> chests, String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            clearChests(chests);
            return;
        }

        org.bukkit.World mapWorld = gameMap.getCurrentWorld();
        for (CoordLoc eChest : chests) {
            int x = eChest.getX();
            int y = eChest.getY();
            int z = eChest.getZ();
            Location loc = new Location(mapWorld, x, y, z);
            if ((loc.getBlock().getState() instanceof Chest)) {
                Chest chest = (Chest) loc.getBlock().getState();
                org.bukkit.inventory.InventoryHolder ih = chest.getInventory().getHolder();
                if ((ih instanceof DoubleChest)) {
                    DoubleChest dc = (DoubleChest) ih;
                    SkyWarsReloaded.getCM().fillChest(dc, tableName);
                } else {
                    SkyWarsReloaded.getCM().fillChest(chest, tableName);
                }
            }
        }
    }

    private void clearChests(ArrayList<CoordLoc> chests) {
        org.bukkit.World mapWorld = gameMap.getCurrentWorld();
        for (CoordLoc eChest : chests) {
            Location loc = new Location(mapWorld, eChest.getX(), eChest.getY(), eChest.getZ());
            if ((loc.getBlock().getState() instanceof Chest)) {
                Chest chest = (Chest) loc.getBlock().getState();
                chest.getInventory().clear();
            }
        }
    }
}
