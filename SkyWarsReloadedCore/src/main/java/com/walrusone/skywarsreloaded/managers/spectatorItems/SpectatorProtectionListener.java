package com.walrusone.skywarsreloaded.managers.spectatorItems;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.entity.PlayerDeathEvent;
import com.walrusone.skywarsreloaded.game.GameMap;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.GameMode;
import java.util.List;
import org.bukkit.ChatColor;
import com.walrusone.skywarsreloaded.managers.PlayerManager;
import com.walrusone.skywarsreloaded.enums.PlayerRemoveReason;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import com.walrusone.skywarsreloaded.menus.gameoptions.objects.CoordLoc;

public class SpectatorProtectionListener implements Listener {

    private final SpectatorManager spectatorManager;
    private final TeleporterMenu teleporterMenu;

    public SpectatorProtectionListener(SpectatorManager sm, TeleporterMenu tm) {
        this.spectatorManager = sm;
        this.teleporterMenu = tm;
    }

    private boolean isProtected(Player p) {
        if (spectatorManager.isInSwSpec(p)) return true;
        
        GameMap map = SwsrHooks.getPlayerMap(p);
        if (map != null && map.getMatchState() == com.walrusone.skywarsreloaded.enums.MatchState.ENDING) {
            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onAnyDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if (isProtected(p)) {
                e.setCancelled(true);
                p.setFireTicks(0);
                p.setRemainingAir(p.getMaximumAir());
                
                if (e.getCause() == EntityDamageEvent.DamageCause.VOID) {
                    GameMap map = SwsrHooks.getPlayerMap(p);
                    if (map != null) {
                        CoordLoc specSpawn = map.getSpectateSpawn();
                        if (specSpawn != null) {
                            p.teleport(new Location(map.getCurrentWorld(), specSpawn.getX() + 0.5, specSpawn.getY(), specSpawn.getZ() + 0.5));
                        } else {
                            CoordLoc lobby = map.getWaitingLobbySpawn();
                            if (lobby != null) {
                                p.teleport(new Location(map.getCurrentWorld(), lobby.getX() + 0.5, lobby.getY(), lobby.getZ() + 0.5));
                            } else if (!map.getSpawnLocations().isEmpty()) {
                                List<CoordLoc> locs = map.getSpawnLocations().values().iterator().next();
                                if (locs != null && !locs.isEmpty()) {
                                    CoordLoc first = locs.get(0);
                                    p.teleport(new Location(map.getCurrentWorld(), first.getX() + 0.5, first.getY(), first.getZ() + 0.5));
                                }
                            }
                        }
                    }
                }

                if (e.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION || e.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                    p.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSpecDealDamage(EntityDamageByEntityEvent e) {

        if (e.getDamager() instanceof Player) {
            Player p = (Player) e.getDamager();
            if (isProtected(p)) {
                e.setCancelled(true);
                return;
            }
        }

        if (e.getDamager() instanceof Projectile) {
            Projectile proj = (Projectile) e.getDamager();
            if (proj.getShooter() instanceof Player) {
                Player p = (Player) proj.getShooter();
                if (isProtected(p)) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();

        GameMap map = SwsrHooks.getPlayerMap(p);
        if (map == null) return;

        Bukkit.getScheduler().runTask(SkyWarsReloaded.get(), () -> {

            SwsrHooks.forceLeave(p);

            Location loc = p.getLocation();

            Bukkit.getScheduler().runTaskLater(SkyWarsReloaded.get(), () -> {
                p.teleport(loc);

                if (!spectatorManager.isInSwSpec(p)) {
                    spectatorManager.enterSpectate(p, map);
                }
            }, 1L);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onGamemodeChange(PlayerGameModeChangeEvent e) {
        Player p = e.getPlayer();

        if (e.getNewGameMode() != GameMode.SPECTATOR) return;

        GameMap map = SwsrHooks.getPlayerMap(p);
        if (map == null) return;

        e.setCancelled(true);

        Bukkit.getScheduler().runTask(SkyWarsReloaded.get(), () -> {
            if (!spectatorManager.isInSwSpec(p)) {
                spectatorManager.enterSpectate(p, map);
            }
        });
    }


    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        if (spectatorManager.isInSwSpec(e.getPlayer())) {
            spectatorManager.forceCleanup(e.getPlayer());
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isProtected(p)) return;

        if (e.getHand() != EquipmentSlot.HAND) return;

        switch (e.getAction()) {
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                break;
            default:
                e.setCancelled(true);
                return;
        }

        if (e.getItem() == null) {
            e.setCancelled(true);
            return;
        }

        Material m = e.getItem().getType();

        if (m == Material.RECOVERY_COMPASS) {
            e.setCancelled(true);
            teleporterMenu.open(p);
            return;
        }

        if (m == Material.SUGAR) {
            e.setCancelled(true);
            spectatorManager.cycleFlySpeed(p);
            return;
        }

        if (m == Material.REDSTONE) {
            e.setCancelled(true);

            Bukkit.getScheduler().runTaskLater(SkyWarsReloaded.get(), () -> {
                SkyWarsReloaded.get().getPlayerManager().removePlayer(p, PlayerRemoveReason.PLAYER_QUIT_GAME, null, true);
            }, 1L);

            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInvClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            Player p = (Player) e.getWhoClicked();
            if (isProtected(p)) e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInvDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            Player p = (Player) e.getWhoClicked();
            if (isProtected(p)) e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrop(PlayerDropItemEvent e) {
        if (isProtected(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if (isProtected(p)) e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!isProtected(p)) return;

        if (!p.getAllowFlight()) p.setAllowFlight(true);
        if (!p.isFlying()) p.setFlying(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (isProtected(e.getPlayer())) spectatorManager.forceCleanup(e.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent e) {
        if (isProtected(e.getPlayer())) spectatorManager.forceCleanup(e.getPlayer());
    }
    @EventHandler
    public void onMobTarget(EntityTargetLivingEntityEvent e) {
        if (e.getTarget() instanceof Player) {
            Player p = (Player) e.getTarget();
            if (isProtected(p)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent e) {
        for (LivingEntity entity : e.getAffectedEntities()) {
            if (entity instanceof Player) {
                Player p = (Player) entity;
                if (isProtected(p)) {
                    e.setIntensity(entity, 0);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if (isProtected(p)) {
                if (p.getFoodLevel() >= 20) {
                    e.setCancelled(true);
                    return;
                }
                e.setCancelled(true);
                p.setFoodLevel(20);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAirChange(EntityAirChangeEvent e) {
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if (isProtected(p)) {
                if (p.getRemainingAir() >= p.getMaximumAir()) {
                    e.setCancelled(true);
                    return;
                }
                e.setCancelled(true);
                p.setRemainingAir(p.getMaximumAir());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCombust(EntityCombustEvent e) {
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if (isProtected(p)) {
                if (p.getFireTicks() <= 0) {
                    e.setCancelled(true);
                    return;
                }
                e.setCancelled(true);
                p.setFireTicks(0);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent e) {
        if (isProtected(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (isProtected(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onHangingBreak(HangingBreakByEntityEvent e) {
        if (e.getRemover() instanceof Player && isProtected((Player) e.getRemover())) {
            e.setCancelled(true);
        }
    }
}
