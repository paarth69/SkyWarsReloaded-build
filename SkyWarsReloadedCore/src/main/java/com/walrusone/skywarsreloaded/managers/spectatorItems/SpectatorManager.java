package com.walrusone.skywarsreloaded.managers.spectatorItems;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.game.GameMap;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class SpectatorManager {

    private final Map<UUID, SpectateSnapshot> snapshots = new HashMap<>();
    private final Map<UUID, String> spectatingMap = new HashMap<>();
    private final Map<UUID, Integer> flySpeedIndex = new HashMap<>();

    private final Map<UUID, Float> desiredFlySpeed = new HashMap<>();

    public SpectatorManager() {
    }

    public boolean isInSwSpec(Player p) {
        return snapshots.containsKey(p.getUniqueId());
    }

    public String getSpectatingMapName(Player p) {
        return spectatingMap.get(p.getUniqueId());
    }

    public void enterSpectate(Player p, GameMap map) {
        if (isInSwSpec(p)) return;

        snapshots.put(p.getUniqueId(), SpectateSnapshot.capture(p));
        spectatingMap.put(p.getUniqueId(), map != null ? map.getName() : null);

        SwsrHooks.removeSpectator(p);

        flySpeedIndex.put(p.getUniqueId(), 0);
        desiredFlySpeed.put(p.getUniqueId(), 0.2f);

        applyFullSpectatorState(p);

        if (map != null && map.getCurrentWorld() != null) {
            String msg = ChatColor.translateAlternateColorCodes('&',
                    "&3" + p.getName() + " &fis now spectating this match!");

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getWorld().equals(map.getCurrentWorld())) {
                    online.sendMessage(msg);
                }
                if (isInSwSpec(online)) {
                    online.showPlayer(SkyWarsReloaded.get(), p);
                    p.showPlayer(SkyWarsReloaded.get(), online);
                } else {
                    online.hidePlayer(SkyWarsReloaded.get(), p);
                }
            }
            try {
                p.setCollidable(false);
            } catch (Throwable ignored) {}
        }
        sendSpeedActionBar(p, flySpeedIndex.get(p.getUniqueId()), getEntries());
    }

    public void exitSpectate(Player p, boolean removeFromSWR) {
        if (removeFromSWR) {
            String mapName = spectatingMap.get(p.getUniqueId());
            GameMap map = mapName != null ? SkyWarsReloaded.getGameMapMgr().getMap(mapName) : null;
            if (map != null && map.getCurrentWorld() != null) {
                String msg = ChatColor.translateAlternateColorCodes('&',
                        "&3" + p.getName() + " &fis no longer spectating this match!");
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.getWorld().equals(map.getCurrentWorld())) {
                        online.sendMessage(msg);
                    }
                }
            }
            SwsrHooks.removeSpectator(p);
        }

        SpectateSnapshot snap = snapshots.remove(p.getUniqueId());
        spectatingMap.remove(p.getUniqueId());
        flySpeedIndex.remove(p.getUniqueId());
        desiredFlySpeed.remove(p.getUniqueId());

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.showPlayer(SkyWarsReloaded.get(), p);
        }
        try {
            p.setCollidable(true);
        } catch (Throwable ignored) {}

        if (snap != null) {
            snap.restore(p);
        }
    }

    private void applyFullSpectatorState(Player p) {
        p.setGameMode(GameMode.ADVENTURE);

        p.setAllowFlight(true);
        p.setFlying(true);

        if (!p.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
             p.removePotionEffect(PotionEffectType.INVISIBILITY);
        }

        giveSpectatorItems(p);
    }


    private void ensureSpectatorState(Player p) {
        if (p.getGameMode() != GameMode.ADVENTURE) p.setGameMode(GameMode.ADVENTURE);

        if (!p.getAllowFlight()) p.setAllowFlight(true);
        if (!p.isFlying()) p.setFlying(true);

        Float want = desiredFlySpeed.get(p.getUniqueId());
        if (want != null) {
            if (Math.abs(p.getFlySpeed() - want) > 0.001f) {
                p.setFlySpeed(want);
            }
        }

        ItemStack slot0 = p.getInventory().getItem(0);
        ItemStack slot4 = p.getInventory().getItem(4);
        ItemStack slot8 = p.getInventory().getItem(8);

        boolean missing =
                slot0 == null || slot0.getType() != Material.RECOVERY_COMPASS ||
                        slot4 == null || slot4.getType() != Material.SUGAR ||
                        slot8 == null || slot8.getType() != Material.REDSTONE;

        if (missing) {
            giveSpectatorItems(p);
        }
    }

    private void giveSpectatorItems(Player p) {
        p.getInventory().clear();

        p.getInventory().setItem(0, ItemUtil.named(Material.RECOVERY_COMPASS, "&3Teleporter &7(Right-Click)"));
        p.getInventory().setItem(4, ItemUtil.named(Material.SUGAR, "&3Change Fly Speed &7(Right-Click)"));
        p.getInventory().setItem(8, ItemUtil.named(Material.REDSTONE, "&cLeave &7(Right-Click)"));

        p.updateInventory();
    }

    private List<String> getEntries() {
        List<String> entries = SkyWarsReloaded.get().getConfig().getStringList("spectator-changespeed-types");
        return entries == null ? Collections.emptyList() : entries;
    }

    public void cycleFlySpeed(Player p) {
        List<String> entries = getEntries();
        if (entries.isEmpty()) return;

        if (!p.getAllowFlight()) p.setAllowFlight(true);
        if (!p.isFlying()) p.setFlying(true);

        int idx = flySpeedIndex.getOrDefault(p.getUniqueId(), 0);
        idx = (idx + 1) % entries.size();
        flySpeedIndex.put(p.getUniqueId(), idx);

        String colored = ChatColor.translateAlternateColorCodes('&', entries.get(idx));
        String stripped = ChatColor.stripColor(colored);

        String[] split = stripped.split(":");
        if (split.length != 2) return;

        float shownSpeed;
        try {
            shownSpeed = Float.parseFloat(split[1]);
        } catch (NumberFormatException ex) {
            return;
        }

        float mc = Math.min(1.0f, Math.max(0.1f, shownSpeed / 3.0f));

        desiredFlySpeed.put(p.getUniqueId(), mc);
        p.setFlySpeed(mc);

        sendSpeedActionBar(p, idx, entries);
    }

    private void sendSpeedActionBar(Player p, int active, List<String> entries) {
        if (entries == null || entries.isEmpty()) return;

        StringBuilder bar = new StringBuilder();

        for (int i = 0; i < entries.size(); i++) {
            String colored = ChatColor.translateAlternateColorCodes('&', entries.get(i));
            String label = colored.split(":")[0];
            String plain = ChatColor.stripColor(label);

            boolean isSelected = (i == active);

            if (isSelected && i != 0) {
                bar.append(ChatColor.DARK_AQUA).append(ChatColor.BOLD).append(plain);
            } else {
                bar.append(ChatColor.WHITE).append(plain);
            }

            if (i < entries.size() - 1) {
                bar.append(ChatColor.WHITE).append(" / ");
            }
        }

        com.walrusone.skywarsreloaded.SkyWarsReloaded.getNMS().sendActionBar(p, bar.toString());
    }

    public void forceCleanup(Player p) {
        if (!isInSwSpec(p)) return;

        SpectateSnapshot snap = snapshots.remove(p.getUniqueId());
        spectatingMap.remove(p.getUniqueId());
        flySpeedIndex.remove(p.getUniqueId());
        desiredFlySpeed.remove(p.getUniqueId());

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.showPlayer(SkyWarsReloaded.get(), p);
        }
        try {
            p.setCollidable(true);
        } catch (Throwable ignored) {}


        if (snap != null) {
            snap.restore(p);
        }
    }

    public void startEnforcerTask() {
        Bukkit.getScheduler().runTaskTimer(SkyWarsReloaded.get(), () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {

                GameMap map = SwsrHooks.getPlayerMap(p);
                boolean inArena = map != null;
                boolean fakeSpec = isInSwSpec(p);

                if (inArena && !fakeSpec && SwsrHooks.isSpectator(p)) {
                    enterSpectate(p, map);
                    continue;
                }

                if (fakeSpec && inArena) {
                    ensureSpectatorState(p);
                    continue;
                }

                if (fakeSpec && !inArena) {
                    forceCleanup(p);
                }
            }
        }, 1L, 5L);
    }



    private static class SpectateSnapshot {
        private final GameMode gameMode;
        private final ItemStack[] contents;
        private final boolean allowFlight;
        private final float flySpeed;

        private SpectateSnapshot(Player p) {
            this.gameMode = p.getGameMode();
            this.contents = p.getInventory().getContents();
            this.allowFlight = p.getAllowFlight();
            this.flySpeed = p.getFlySpeed();
        }

        static SpectateSnapshot capture(Player p) {
            return new SpectateSnapshot(p);
        }

        void restore(Player p) {
            p.removePotionEffect(PotionEffectType.INVISIBILITY);
            p.setGameMode(gameMode);
            p.setAllowFlight(allowFlight);
            p.setFlying(false);
            p.setFlySpeed(flySpeed);
            p.getInventory().setContents(contents);
            p.updateInventory();
        }
    }
}
