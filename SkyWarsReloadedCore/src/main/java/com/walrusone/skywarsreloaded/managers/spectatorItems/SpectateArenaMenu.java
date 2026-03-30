package com.walrusone.skywarsreloaded.managers.spectatorItems;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.enums.GameType;
import com.walrusone.skywarsreloaded.enums.MatchState;
import com.walrusone.skywarsreloaded.game.GameMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public final class SpectateArenaMenu implements CommandExecutor, Listener {

    private final SpectatorManager spectatorManager;

    private final Map<UUID, Map<Integer, GameMap>> selections = new HashMap<>();

    public SpectateArenaMenu(SpectatorManager spectatorManager) {
        this.spectatorManager = spectatorManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("swspectategui.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        openMenu(player);
        return true;
    }

    private void openMenu(Player player) {
        String title = ChatColor.translateAlternateColorCodes('&',
                SkyWarsReloaded.get().getConfig().getString("spectator-menu-title", "&3&lSPECTATE"));

        Inventory inv = Bukkit.createInventory(player, 54, title);
        Map<Integer, GameMap> mapBySlot = new HashMap<>();

        List<GameMap> running = SkyWarsReloaded.getGameMapMgr()
                .getPlayableArenas(GameType.ALL)
                .stream()
                .filter(m -> m.getMatchState() == MatchState.PLAYING)
                .collect(Collectors.toList());

        int slot = 0;
        for (GameMap map : running) {
            if (slot >= inv.getSize()) break;
            inv.setItem(slot, createArenaIcon(map));
            mapBySlot.put(slot, map);
            slot++;
        }

        selections.put(player.getUniqueId(), mapBySlot);
        player.openInventory(inv);
    }

    private ItemStack createArenaIcon(GameMap map) {
        ItemStack item = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String nameFmt = SkyWarsReloaded.get().getConfig().getString("spectator-arena-item-name", "&f{display-name}");
        String name = ChatColor.translateAlternateColorCodes('&',
                nameFmt.replace("{display-name}", safe(map.getDisplayName())));

        int alive = 0;
        try { alive = map.getAlivePlayers().size(); } catch (Throwable ignored) {}
        int max = 0;
        try { max = map.getMaxPlayers(); } catch (Throwable ignored) {}

        List<String> loreFmt = SkyWarsReloaded.get().getConfig().getStringList("spectator-arena-item-lore");
        List<String> lore = new ArrayList<>();
        
        if (loreFmt == null || loreFmt.isEmpty()) {
            lore.add(ChatColor.GRAY + "Players: " + ChatColor.WHITE + alive + "/" + max);
            lore.add(ChatColor.YELLOW + "Click to spectate!");
        } else {
            for (String l : loreFmt) {
                lore.add(ChatColor.translateAlternateColorCodes('&',
                        l.replace("{players}", alive + "/" + max)));
            }
        }

        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String safe(String s) {
        return s == null ? "Arena" : s;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        String title = ChatColor.translateAlternateColorCodes('&',
                SkyWarsReloaded.get().getConfig().getString("spectator-menu-title", "&3&lSPECTATE"));

        if (!e.getView().getTitle().equals(title)) return;

        e.setCancelled(true);

        Map<Integer, GameMap> map = selections.get(p.getUniqueId());
        if (map == null) return;

        GameMap chosen = map.get(e.getSlot());
        if (chosen == null) return;

        if (chosen.getMatchState() != MatchState.PLAYING) {
            p.sendMessage(ChatColor.RED + "That match is no longer running.");
            return;
        }

        p.closeInventory();

        SwsrHooks.addSpectator(chosen, p);
        spectatorManager.enterSpectate(p, chosen);
    }
}
