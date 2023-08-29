/*
 * AVRGAMING LLC
 * __________________
 *
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.listener;

import com.avrgaming.civcraft.camp.Camp;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigTechPotion;
import com.avrgaming.civcraft.items.units.UnitItemMaterial;
import com.avrgaming.civcraft.items.units.UnitMaterial;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.structure.Capitol;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.PlayerChunkNotifyAsyncTask;
import com.avrgaming.civcraft.threading.tasks.PlayerLoginAsyncTask;
import com.avrgaming.civcraft.threading.timers.PlayerLocationCacheUpdate;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.civcraft.war.WarStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

public class PlayerListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            String name;
            boolean rare = false;
            ItemStack item = event.getItem().getItemStack();
            if (item.getItemMeta().hasDisplayName()) {
                name = item.getItemMeta().getDisplayName();
                rare = true;
            } else {
                name = item.getType().name().replace("_", " ").toLowerCase();
            }

            Resident resident = CivGlobal.getResident(player);
            if (resident.getItemMode().equals("all")) {
                CivMessage.send(player, ChatColor.GREEN + CivSettings.localize.localizedString("var_customItem_Pickup", String.valueOf(ChatColor.LIGHT_PURPLE) + event.getItem().getItemStack().getAmount(), name), item);
            } else if (resident.getItemMode().equals("rare") && rare) {
                CivMessage.send(player, ChatColor.GREEN + CivSettings.localize.localizedString("var_customItem_Pickup", String.valueOf(ChatColor.LIGHT_PURPLE) + event.getItem().getItemStack().getAmount(), name), item);
            }
        }
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogin(PlayerLoginEvent event) {
        CivLog.info("Scheduling on player login task for player:" + event.getPlayer().getName());
        TaskMaster.asyncTask("onPlayerLogin-" + event.getPlayer().getName(), new PlayerLoginAsyncTask(event.getPlayer().getUniqueId()), 0);

        CivGlobal.playerFirstLoginMap.put(event.getPlayer().getName(), new Date());
        PlayerLocationCacheUpdate.playerQueue.add(event.getPlayer().getName());
//		MobSpawnerTimer.playerQueue.add((event.getPlayer().getName()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerTeleportEvent(PlayerTeleportEvent event) {
        //Handle Teleportation Things here!
        if (!event.getCause().equals(TeleportCause.COMMAND) && !event.getCause().equals(TeleportCause.PLUGIN)) {
            return;
        }
        CivLog.info("[TELEPORT]" + " " + event.getPlayer().getName() + " " + "to:" + event.getTo().getBlockX() + "," + event.getTo().getBlockY() + "," + event.getTo().getBlockZ() +
                " " + "from:" + event.getFrom().getBlockX() + "," + event.getFrom().getBlockY() + "," + event.getFrom().getBlockZ());
        Player player = event.getPlayer();
        if (player.isOp()) {
            return;
        }
        CultureChunk cc = CivGlobal.getCultureChunk(new ChunkCoord(event.getTo()));
        Camp toCamp = CivGlobal.getCampFromChunk(new ChunkCoord(event.getTo()));
        Resident resident = CivGlobal.getResident(player);
        if (cc != null && cc.getCiv() != resident.getCiv() && !cc.getCiv().isAdminCiv()) {
            event.setTo(event.getFrom());
            if (!event.isCancelled()) {
                CivLog.debug("Cancelled Event " + event.getEventName() + " with cause: " + event.getCause());
                event.setCancelled(true);
                CivMessage.send(resident, ChatColor.DARK_RED + CivSettings.localize.localizedString("teleportDeniedPrefix") + " " + ChatColor.WHITE + CivSettings.localize.localizedString("var_teleportDeniedCiv", ChatColor.DARK_GREEN + cc.getCiv().getName() + ChatColor.WHITE));
                return;
            }
        }

        if (toCamp != null && toCamp != resident.getCamp()) {
            /*
             * Deny telportation into Civ if not allied.
             */
            event.setTo(event.getFrom());
            if (!event.isCancelled()) {
                CivLog.debug("Cancelled Event " + event.getEventName() + " with cause: " + event.getCause());
                event.setCancelled(true);
                CivMessage.send(resident, ChatColor.DARK_RED + CivSettings.localize.localizedString("teleportDeniedPrefix") + " " + ChatColor.WHITE + CivSettings.localize.localizedString("var_teleportDeniedCamp", ChatColor.DARK_GREEN + toCamp.getName() + ChatColor.WHITE));
            }

        }

//				if (War.isWarTime()) {
//
//					if (toCamp != null && toCamp == resident.getCamp()) {
//						return;
//					}
//					if (cc != null && (cc.getCiv() == resident.getCiv() || cc.getCiv().isAdminCiv())) {
//						return;
//					}
//
//					event.setTo(event.getFrom());
//					if (!event.isCancelled())
//					{
//					event.setCancelled(true);
//						CivMessage.send(resident, CivColor.Red+"[Denied] "+CivColor.White+"You're not allowed to Teleport during War unless you are teleporting to your own Civ or Camp");
//					}
//				}
    }

    private void setModifiedMovementSpeed(Player player) {
        /* Change move speed based on armor. */
        double speed;
        Resident resident = CivGlobal.getResident(player);
        if (resident != null) {
            speed = resident.getWalkingModifier();
//            if (resident.isOnRoad()) {
//                if (player.getVehicle() != null && player.getVehicle().getType().equals(EntityType.HORSE)) {
//                    Vector vec = player.getVehicle().getVelocity();
//                    double yComp = vec.getY();
//
//                    vec.multiply(Road.ROAD_HORSE_SPEED);
//                    vec.setY(yComp); /* Do not multiply y velocity. */
//
//                    player.getVehicle().setVelocity(vec);
//                } else {
//                    speed *= Road.ROAD_PLAYER_SPEED;
//                }
//            }
        } else {
            speed = CivSettings.normal_speed;
        }

        player.setWalkSpeed((float) Math.min(1.0f, speed));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerMove(PlayerMoveEvent event) {
        /*
         * Abort if we havn't really moved
         */
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY()) {
            return;
        }
        if (!CivGlobal.speedChunks) {
            /* Get the Modified Speed for the player. */
            setModifiedMovementSpeed(event.getPlayer());
        }

        ChunkCoord fromChunk = new ChunkCoord(event.getFrom());
        ChunkCoord toChunk = new ChunkCoord(event.getTo());

        // Haven't moved chunks.
        if (fromChunk.equals(toChunk)) {
            return;
        }
        if (CivGlobal.speedChunks) {
            /* Get the Modified Speed for the player. */
            setModifiedMovementSpeed(event.getPlayer());
        }

        TaskMaster.asyncTask(PlayerChunkNotifyAsyncTask.class.getSimpleName(),
                new PlayerChunkNotifyAsyncTask(event.getFrom(), event.getTo(), event.getPlayer().getName()), 0);

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Resident resident = CivGlobal.getResident(player);
        if (resident == null || !resident.hasTown()) {
            return;
        }

        if (War.isWarTime() && !resident.isInsideArena()) {
            if (resident.getTown().getCiv().getDiplomacyManager().isAtWar()) {
                //TownHall townhall = resident.getTown().getTownHall();
                Capitol capitol = resident.getCiv().getCapitolStructure();
                if (capitol != null) {
                    BlockCoord respawn = capitol.getRandomRespawnPoint();
                    if (respawn != null) {
                        //PlayerReviveTask reviveTask = new PlayerReviveTask(player, townhall.getRespawnTime(), townhall, event.getRespawnLocation());
                        resident.setLastKilledTime(new Date());
                        event.setRespawnLocation(respawn.getCenteredLocation());
                        CivMessage.send(player, ChatColor.GRAY + CivSettings.localize.localizedString("playerListen_repawnInWarRoom"));

                        //TaskMaster.asyncTask("", reviveTask, 0);
                    }
                }
            }
        }

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Resident resident = CivGlobal.getResident(event.getPlayer());
        if (resident != null) {
            if (resident.previewUndo != null) {
                resident.previewUndo.clear();
            }
            resident.clearInteractiveMode();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            //Unit.removeUnit(((Player)event.getEntity()));
            boolean keepInventory = Boolean.parseBoolean(Bukkit.getWorld("world").getGameRuleValue("keepInventory"));
            if (!keepInventory) {
                ArrayList<ItemStack> stacksToRemove = new ArrayList<>();
                for (ItemStack stack : event.getDrops()) {
                    if (stack != null) {
                        //CustomItemStack is = new CustomItemStack(stack);
                        LoreMaterial material = LoreMaterial.getMaterial(stack);
                        if (material != null) {
                            material.onPlayerDeath(event, stack);
                            if (material instanceof UnitMaterial) {
                                stacksToRemove.add(stack);
                                continue;
                            }

                            if (material instanceof UnitItemMaterial) {
                                stacksToRemove.add(stack);
                            }
                        }
                    }
                }

                for (ItemStack stack : stacksToRemove) {
                    event.getDrops().remove(stack);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (War.isWarTime()) {
            if (event.getEntity().getKiller() != null) {
                WarStats.incrementPlayerKills(event.getEntity().getKiller().getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPortalCreate(PortalCreateEvent event) {
        event.setCancelled(true);
    }

//	@EventHandler(priority = EventPriority.NORMAL)
//	public void OnCraftItemEvent(CraftItemEvent event) {
//		if (event.getInventory() == null) {
//			return;
//		}
//		
//		ItemStack resultStack = event.getInventory().getResult();
//		if (resultStack == null) {
//			return;
//		}
//		
//		if (CivSettings.techItems == null) {
//			CivLog.error("tech items null???");
//			return;
//		}
//
//		//XXX Replaced via materials system.
////		ConfigTechItem item = CivSettings.techItems.get(resultStack.getTypeId());
////		if (item != null) {
////			Resident resident = CivGlobal.getResident(event.getWhoClicked().getName());
////			if (resident != null && resident.hasTown()) {
////				if (resident.getCiv().hasTechnology(item.require_tech)) {
////					return;
////				}
////			}	
////			event.setCancelled(true);
////			CivMessage.sendError((Player)event.getWhoClicked(), "You do not have the required technology to craft a "+item.name);
////		}
//	}

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPortalEvent(PlayerPortalEvent event) {
        if (event.getCause().equals(TeleportCause.END_PORTAL)) {
            event.setCancelled(true);
            CivMessage.sendErrorNoRepeat(event.getPlayer(), CivSettings.localize.localizedString("playerListen_endDisabled"));
            return;
        }

        if (event.getCause().equals(TeleportCause.NETHER_PORTAL)) {
            event.setCancelled(true);
            CivMessage.sendErrorNoRepeat(event.getPlayer(), CivSettings.localize.localizedString("playerListen_netherDisabled"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        // THIS EVENT IS NOT RUN IN OFFLINE MODE
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void OnPlayerBucketEmptyEvent(PlayerBucketEmptyEvent event) {
        Resident resident = CivGlobal.getResident(event.getPlayer());

        if (resident == null) {
            event.setCancelled(true);
            return;
        }

        ChunkCoord coord = new ChunkCoord(event.getBlockClicked().getLocation());
        CultureChunk cc = CivGlobal.getCultureChunk(coord);
        if (cc != null) {
            if (event.getBucket().equals(Material.LAVA_BUCKET) ||
                    event.getBucket().equals(Material.LAVA)) {

                if (!resident.hasTown() || (resident.getTown().getCiv() != cc.getCiv())) {
                    CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("playerListen_placeLavaDenied"));
                    event.setCancelled(true);
                }

            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void OnBrewEvent(BrewEvent event) {
        /* Hardcoded disables based on ingredients used. */
        if (event.getContents().contains(Material.SPIDER_EYE) ||
                event.getContents().contains(Material.GOLDEN_CARROT) ||
                event.getContents().contains(Material.GHAST_TEAR) ||
                event.getContents().contains(Material.FERMENTED_SPIDER_EYE) ||
                event.getContents().contains(Material.BLAZE_POWDER) ||
                event.getContents().contains(Material.GUNPOWDER)) {
            event.setCancelled(true);
        }

        if (event.getContents().contains(Material.POTION)) {
            ItemStack potion = event.getContents().getItem(event.getContents().first(Material.POTION));

            if (potion.getDurability() == CivData.MUNDANE_POTION_DATA ||
                    potion.getDurability() == CivData.MUNDANE_POTION_EXT_DATA ||
                    potion.getDurability() == CivData.THICK_POTION_DATA) {
                event.setCancelled(true);
            }
        }
    }

    private boolean isPotionDisabled(PotionEffect type) {
        return !type.getType().equals(PotionEffectType.SPEED) &&
                !type.getType().equals(PotionEffectType.FIRE_RESISTANCE) &&
                !type.getType().equals(PotionEffectType.HEAL) &&
                (!type.getType().equals(PotionEffectType.SPEED) || type.getAmplifier() != 2);
//added speed II
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPotionSplash(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();

        if (!(potion.getShooter() instanceof Player)) {
            return;
        }
        for (PotionEffect effect : event.getPotion().getEffects()) {
            if (isPotionDisabled(effect)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack stack = event.getItem();
        if (stack.getType() == Material.GOLDEN_APPLE) {
            CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("itemUse_errorGoldenApple"));
            event.setCancelled(true);
            return;
        }

        if (event.getItem().getType().equals(Material.POTION)) {
            PotionMeta meta = (PotionMeta) event.getItem().getItemMeta();
            for (PotionEffect effect : meta.getCustomEffects()) {
                String name = effect.getType().getName();
                int amp = effect.getAmplifier();
                ConfigTechPotion pot = CivSettings.techPotions.get(name + amp);
                if (pot != null) {
                    if (!pot.hasTechnology(event.getPlayer())) {
                        CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("var_playerListen_potionNoTech", pot.name));
                        event.setCancelled(true);
                        return;
                    }
                    if (pot.hasTechnology(event.getPlayer())) {
                        event.setCancelled(false);
                        return;
                    }
                } else {
                    CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("playerListen_denyUse"));
                    event.setCancelled(true);
                    return;
                }
            }


        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpenEvent(InventoryOpenEvent event) {
        if (event.getInventory() instanceof DoubleChestInventory doubleInv) {

            Chest leftChest = (Chest) doubleInv.getHolder().getLeftSide();
            /*Generate a new player 'switch' event for the left and right chests. */
            PlayerInteractEvent interactLeft = new PlayerInteractEvent((Player) event.getPlayer(), Action.RIGHT_CLICK_BLOCK, null, leftChest.getBlock(), null);
            BlockListener.OnPlayerSwitchEvent(interactLeft);

            if (interactLeft.isCancelled()) {
                event.setCancelled(true);
                return;
            }

            Chest rightChest = (Chest) doubleInv.getHolder().getRightSide();
            PlayerInteractEvent interactRight = new PlayerInteractEvent((Player) event.getPlayer(), Action.RIGHT_CLICK_BLOCK, null, rightChest.getBlock(), null);
            BlockListener.OnPlayerSwitchEvent(interactRight);

            if (interactRight.isCancelled()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamageByEntityMonitor(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player attacker;
        Player defender;
        String damage;

        if (event.getEntity() instanceof Player) {
            defender = (Player) event.getEntity();
        } else {
            defender = null;
        }

        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Arrow arrow) {
            if (arrow.getShooter() instanceof Player) {
                attacker = (Player) arrow.getShooter();
            } else {
                attacker = null;
            }
        } else {
            attacker = null;
        }

        if (attacker == null && defender == null) {
            return;
        }

        damage = new DecimalFormat("#.#").format(event.getDamage());

        if (defender != null) {
            Resident defenderResident = CivGlobal.getResident(defender);
            if (defenderResident.isCombatInfo()) {
                if (attacker != null) {
                    CivMessage.send(defender, ChatColor.GRAY + "   " + CivSettings.localize.localizedString("playerListen_combatHeading") + " " + CivSettings.localize.localizedString("var_playerListen_combatDefend", ChatColor.RED + attacker.getName() + ChatColor.GRAY, ChatColor.RED + damage + ChatColor.GRAY));
                } else {
                    String entityName = null;

                    if (event.getDamager() instanceof LivingEntity) {
                        entityName = event.getDamager().getCustomName();
                    }

                    if (entityName == null) {
                        entityName = event.getDamager().getType().toString();
                    }

                    CivMessage.send(defender, ChatColor.GRAY + "   " + CivSettings.localize.localizedString("playerListen_combatHeading") + " " + CivSettings.localize.localizedString("var_playerListen_combatDefend", ChatColor.LIGHT_PURPLE + entityName + ChatColor.GRAY, ChatColor.RED + damage + ChatColor.GRAY));
                }
            }
        }

        if (attacker != null) {
            Resident attackerResident = CivGlobal.getResident(attacker);
            if (attackerResident.isCombatInfo()) {
                if (defender != null) {
                    CivMessage.send(attacker, ChatColor.GRAY + "   " + CivSettings.localize.localizedString("playerListen_combatHeading") + " " + CivSettings.localize.localizedString("var_playerListen_attack", ChatColor.RED + defender.getName() + ChatColor.GRAY, ChatColor.GREEN + damage + ChatColor.GRAY));
                    CivMessage.sendActionBar(attacker, CivData.getStringForBar(CivData.TaskType.PLAYER, defender.getHealth(), (int) defender.getMaxHealth()));
                } else {
                    String entityName = null;

                    if (event.getEntity() instanceof LivingEntity) {
                        entityName = event.getEntity().getCustomName();
                    }

                    if (entityName == null) {
                        entityName = event.getEntity().getType().toString();
                    }

                    CivMessage.send(attacker, ChatColor.GRAY + "   " + CivSettings.localize.localizedString("playerListen_combatHeading") + " " + CivSettings.localize.localizedString("var_playerListen_attack", ChatColor.LIGHT_PURPLE + entityName + ChatColor.GRAY, ChatColor.GREEN + damage + ChatColor.GRAY));
                }
            }
        }
    }
}
