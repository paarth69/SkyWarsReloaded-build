package com.walrusone.skywarsreloaded.utilities.placeholders;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.enums.LeaderType;
import com.walrusone.skywarsreloaded.enums.MatchState;
import com.walrusone.skywarsreloaded.game.GameMap;
import com.walrusone.skywarsreloaded.managers.Leaderboard;
import com.walrusone.skywarsreloaded.managers.PlayerStat;
import com.walrusone.skywarsreloaded.utilities.Messaging;
import com.walrusone.skywarsreloaded.utilities.Util;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.List;

public class SWRPlaceholderAPI extends PlaceholderExpansion {

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "sw";
    }

    @Override
    public String getAuthor() {
        return "Devmart";
    }

    @Override
    public String getVersion() {
        return SkyWarsReloaded.get().getDescription().getVersion();
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player p, String identifier) {
        if (identifier == null) return "";
        String lowerIdent = identifier.toLowerCase();

        // Arena-specific placeholders (don't require player)
        if (lowerIdent.startsWith("arena_status_")) {
            String arenaName = identifier.substring(13); // Fixed length to avoid issues
            GameMap map = findArena(arenaName);
            if (map == null) return "ARENA_NOT_FOUND:" + arenaName;
            return getStatusString(map);
        }

        if (lowerIdent.startsWith("arena_players_")) {
            String arenaName = identifier.substring(14); // Fixed length to avoid issues
            GameMap map = findArena(arenaName);
            if (map == null) return "ARENA_NOT_FOUND:" + arenaName;
            try {
                return String.valueOf(map.getAlivePlayers().size());
            } catch (Exception e) {
                return "ERR_ALIVE";
            }
        }

        if (lowerIdent.equalsIgnoreCase("playing_solo")) {
            int count = 0;
            try {
                if (SkyWarsReloaded.getGameMapMgr() != null) {
                    for (GameMap map : SkyWarsReloaded.getGameMapMgr().getMapsCopy()) {
                        if (map.getTeamSize() == 1 && map.getMatchState() == MatchState.PLAYING) {
                            count += map.getAlivePlayers().size();
                        }
                    }
                }
            } catch (Exception ignored) {}
            return String.valueOf(count);
        }

        if (lowerIdent.equalsIgnoreCase("playing_teams")) {
            int count = 0;
            try {
                if (SkyWarsReloaded.getGameMapMgr() != null) {
                    for (GameMap map : SkyWarsReloaded.getGameMapMgr().getMapsCopy()) {
                        if (map.getTeamSize() > 1 && map.getMatchState() == MatchState.PLAYING) {
                            count += map.getAlivePlayers().size();
                        }
                    }
                }
            } catch (Exception ignored) {}
            return String.valueOf(count);
        }

        if (p == null) {
            return "";
        }

        PlayerStat stat = SkyWarsReloaded.get().getPlayerStat(p);
        if (identifier.equalsIgnoreCase("wins")) {
            return "" + stat.getWins();
        } else if (identifier.equalsIgnoreCase("losses")) {
            return "" + stat.getLosses();
        } else if (identifier.equalsIgnoreCase("kills")) {
            return "" + stat.getKills();
        } else if (identifier.equalsIgnoreCase("deaths")) {
            return "" + stat.getDeaths();
        } else if (identifier.equalsIgnoreCase("xp")) {
            return "" + stat.getXp();
        } else if (identifier.equalsIgnoreCase("games_played") ||
                identifier.equalsIgnoreCase("games")) {
            return "" + (stat.getWins() + stat.getLosses());
        } else if (identifier.equalsIgnoreCase("level")) {
            return "" + Util.get().getPlayerLevel(p, false);
        } else if (identifier.equalsIgnoreCase("kill_death")) {
            double stat1 = (double) stat.getKills() / (double) stat.getDeaths();
            return String.format("%1$,.2f", stat1);
        } else if (identifier.equalsIgnoreCase("win_loss")) {
            double stat1 = (double) stat.getWins() / (double) stat.getLosses();
            return String.format("%1$,.2f", stat1);
        }
        else {
            return null;
        }

    }

    private String getStatusString(GameMap map) {
        MatchState state = map.getMatchState();
        Messaging.MessageFormatter formatter = new Messaging.MessageFormatter();
        switch (state) {
            case WAITINGLOBBY:
            case WAITINGSTART:
                if (map.getPlayerCount() >= map.getMinTeams() || map.getForceStart()) {
                    return formatter.format("matchstate.starting");
                } else {
                    return formatter.format("matchstate.waiting");
                }
            case PLAYING:
                return formatter.format("matchstate.playing");
            case ENDING:
                return formatter.format("matchstate.ending");
            case OFFLINE:
            default:
                return formatter.format("matchstate.offline");
        }
    }

    /**
     * Finds an arena by internal name or display name
     */
    private GameMap findArena(String arenaName) {
        if (arenaName == null || arenaName.isEmpty()) return null;
        
        // First try by internal name
        GameMap map = SkyWarsReloaded.getGameMapMgr().getMap(arenaName);
        if (map != null) return map;
        
        // Then try by display name
        map = SkyWarsReloaded.getGameMapMgr().getMapByDisplayName(arenaName);
        if (map != null) return map;
        
        // Try case-insensitive search on all maps
        for (GameMap gMap : SkyWarsReloaded.getGameMapMgr().getMapsCopy()) {
            if (gMap == null) continue;
            // Check internal name
            if (gMap.getName() != null && gMap.getName().equalsIgnoreCase(arenaName)) {
                return gMap;
            }
            // Check display name with colors stripped
            String displayName = gMap.getDisplayName();
            if (displayName != null) {
                String stripped = org.bukkit.ChatColor.stripColor(
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName)
                );
                if (stripped.equalsIgnoreCase(arenaName)) {
                    return gMap;
                }
            }
        }
        
        return null;
    }

    public static String getLeaderBoardVariable(String var, @Nullable LeaderType type) {
        String[] parts = var.split("_");
        if (SkyWarsReloaded.getLB() != null) {
            List<Leaderboard.LeaderData> topList = SkyWarsReloaded.getLB().getTopList(type);

            if (topList != null && Util.get().isInteger(parts[1])) {
                int playerLeaderboardRank;
                try {
                    playerLeaderboardRank = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    return "INVALID-INPUT-(NAN)";
                }

                int playerLeaderboardIndex = playerLeaderboardRank - 1;
                if (playerLeaderboardIndex < 0) {
                    return "INVALID-INPUT-(RANK<1)";
                }

                if (topList.size() > playerLeaderboardIndex) {
                    String firstPartLower = parts[0].toLowerCase();

                    switch (firstPartLower) {
                        case "wins":
                            return "" + topList.get(playerLeaderboardIndex).getWins();
                        case "losses":
                            return "" + topList.get(playerLeaderboardIndex).getLoses();
                        case "kills":
                            return "" + topList.get(playerLeaderboardIndex).getKills();
                        case "deaths":
                            return "" + topList.get(playerLeaderboardIndex).getDeaths();
                        case "xp":
                            return "" + topList.get(playerLeaderboardIndex).getXp();
                        case "player":
                            return "" + topList.get(playerLeaderboardIndex).getName();
                        case "games_played":
                            return "" + (topList.get(playerLeaderboardIndex).getLoses() + topList.get(playerLeaderboardIndex).getWins());
                        case "kill_death": {
                            double stat = (double) topList.get(playerLeaderboardIndex).getKills() / (double) topList.get(playerLeaderboardIndex).getDeaths();
                            return String.format("%1$,.2f", stat);
                        }
                        case "win_loss": {
                            double stat = (double) topList.get(playerLeaderboardIndex).getWins() / (double) topList.get(playerLeaderboardIndex).getLoses();
                            return String.format("%1$,.2f", stat);
                        }
                    }
                }
            }
        }
        return "NO DATA";
    }
}
