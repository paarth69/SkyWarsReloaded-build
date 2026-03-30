package com.walrusone.skywarsreloaded.managers.spectatorItems;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.game.GameMap;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public final class SwsrHooks {

    private SwsrHooks() {}

    public static boolean isSwsrPresent() {
        try {
            return SkyWarsReloaded.get() != null;
        } catch (Throwable t) {
            return false;
        }
    }

    public static GameMap getPlayerMap(Player p) {
        try {
            return com.walrusone.skywarsreloaded.managers.MatchManager.get().getPlayerMap(p);
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean isSpectator(Player p) {
        try {
            return SkyWarsReloaded.get().getPlayerManager().isSpectator(p);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void addSpectator(GameMap map, Player p) {
        try {
            SkyWarsReloaded.get().getPlayerManager().addSpectator(map, p);
        } catch (Throwable ignored) {}
    }

    public static void forceLeave(Player player) {
        try {
            Object pm = SkyWarsReloaded.get().getPlayerManager();
            for (String mName : new String[]{
                    "leaveGame",
                    "removePlayer",
                    "forceLeave",
                    "playerLeave"
            }) {
                try {
                    java.lang.reflect.Method m = pm.getClass().getMethod(mName, Player.class);
                    m.invoke(pm, player);
                    return;
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Throwable ignored) {}
    }


    public static void removeSpectator(Player p) {
        try {
            Object pm = SkyWarsReloaded.get().getPlayerManager();
            for (String name : new String[]{"removeSpectator", "leaveSpectator", "removePlayerSpectator"}) {
                try {
                    Method m = pm.getClass().getMethod(name, Player.class);
                    m.invoke(pm, p);
                    return;
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Throwable ignored) {}
    }
}
