package com.walrusone.skywarsreloaded.managers.spectatorItems;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.SkullMeta;
import com.walrusone.skywarsreloaded.game.GameMap;

import java.util.*;

public class TeleporterMenu implements Listener {

    private final SpectatorManager spectatorManager;
    private final Map<UUID, Map<Integer, UUID>> targets = new HashMap<>();

    public TeleporterMenu(SpectatorManager spectatorManager) {
        this.spectatorManager = spectatorManager;
    }

    public void open(Player spectator) {
        String mapName = spectatorManager.getSpectatingMapName(spectator);

        List<Player> validTargets = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(spectator)) continue;
            if (spectatorManager.isInSwSpec(p)) continue;

            if (mapName != null) {
                GameMap m = SwsrHooks.getPlayerMap(p);
                if (m == null || !mapName.equalsIgnoreCase(m.getName())) continue;
            }

            validTargets.add(p);
        }

        int rows = Math.min(2, Math.max(1, (validTargets.size() + 8) / 9));
        Inventory inv = Bukkit.createInventory(spectator, rows * 9, ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "TELEPORTER");

        Map<Integer, UUID> slotMap = new HashMap<>();
        int slot = 0;

        for (Player p : validTargets) {
            ItemStack head = SkyWarsReloaded.getNMS().getBlankPlayerHead();
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                SkyWarsReloaded.getNMS().updateSkull(meta, p);
                meta.setDisplayName(ChatColor.WHITE + p.getName());
                head.setItemMeta(meta);
            }

            inv.setItem(slot, head);
            slotMap.put(slot, p.getUniqueId());
            slot++;
        }

        targets.put(spectator.getUniqueId(), slotMap);
        spectator.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player spectator = (Player) e.getWhoClicked();
        if (!e.getView().getTitle().contains("TELEPORTER")) return;

        e.setCancelled(true);

        Map<Integer, UUID> map = targets.get(spectator.getUniqueId());
        if (map == null) return;

        UUID targetId = map.get(e.getSlot());
        if (targetId == null) return;

        Player target = Bukkit.getPlayer(targetId);
        if (target == null) return;

        Bukkit.getScheduler().runTask(SkyWarsReloaded.get(), () -> {
            spectator.teleport(target.getLocation().add(0, 0.5, 0));
        });

        spectator.closeInventory();
    }
}
