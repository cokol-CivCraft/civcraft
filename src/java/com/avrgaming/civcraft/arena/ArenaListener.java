package com.avrgaming.civcraft.arena;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.DateUtil;
import com.avrgaming.civcraft.util.TimeTools;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Date;

public class ArenaListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockIgniteEvent(BlockIgniteEvent event) {
        if (ArenaManager.activeArenas.containsKey(event.getBlock().getWorld().getName())) {
            event.setCancelled(true);
        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        Resident resident = CivGlobal.getResident(event.getPlayer());
        if (resident == null) {
            return;
        }

        if (resident.isInsideArena()) {
            if (resident.getCurrentArena() != null) {

                CivMessage.sendArena(resident.getCurrentArena(), CivSettings.localize.localizedString("var_arena_playerJoined", event.getPlayer().getName()));

                TaskMaster.syncTask(() -> {
                    try {
                        Resident resident1 = CivGlobal.getResident(event.getPlayer().getName());
                        CivGlobal.getPlayer(resident1).setScoreboard(resident1.getCurrentArena().getScoreboard(resident1.getTeam().getName()));
                    } catch (CivException ignored) {
                    }
                });
            } else {

                event.getPlayer().getInventory().clear();
                TaskMaster.syncTask(() -> {
                    Resident resident12 = CivGlobal.getResident(event.getPlayer().getName());

                    /* Player is rejoining but the arena is no longer active. Return home. */
                    resident12.teleportHome();
                    resident12.restoreInventory();
                    resident12.setInsideArena(false);
                    resident12.save();

                    CivMessage.send(resident12, CivColor.LightGray + CivSettings.localize.localizedString("arena_destroyedTeleport"));
                }, 10);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        String worldName = event.getPlayer().getWorld().getName();
        if (!ArenaManager.activeArenas.containsKey(worldName)) {
            return;
        }

        /* Player is leaving an active arena. Let everyone know. */
        CivMessage.sendArena(ArenaManager.activeArenas.get(worldName), CivSettings.localize.localizedString("var_arena_playerLeft", event.getPlayer().getName()));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        String worldName = event.getBlock().getWorld().getName();
        if (!ArenaManager.activeArenas.containsKey(worldName)) {
            return;
        }

        Resident resident = CivGlobal.getResident(event.getPlayer());
        if (resident.isSBPermOverride()) {
            return;
        }

        event.setCancelled(true);
    }


    public static BlockCoord bcoord = new BlockCoord();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreakEvent(BlockBreakEvent event) {
        String worldName = event.getBlock().getWorld().getName();
        if (!ArenaManager.activeArenas.containsKey(worldName)) {
            return;
        }

        Resident resident = CivGlobal.getResident(event.getPlayer());
        if (resident.isSBPermOverride()) {
            return;
        }

        bcoord.setFromLocation(event.getBlock().getLocation());
        ArenaControlBlock acb = ArenaManager.arenaControlBlocks.get(bcoord);
        if (acb != null) {
            acb.onBreak(resident);
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Resident resident = CivGlobal.getResident(event.getPlayer());
        resident.increaseRespawnTime();

        if (!resident.isInsideArena()) {
            return;
        }

        Arena arena = resident.getCurrentArena();
        if (arena == null) {
            return;
        }

        Location loc = arena.getRespawnLocation(resident);
        if (loc != null) {
            CivMessage.send(resident, CivColor.LightGray + CivSettings.localize.localizedString("arena_respawned"));
            World world = Bukkit.getWorld(arena.getInstanceName());
            loc.setWorld(world);

            resident.setLastKilledTime(new Date());
            event.setRespawnLocation(loc);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Resident resident = CivGlobal.getResident(event.getPlayer());

        if (!resident.isInsideArena()) {
            return;
        }

        Arena arena = resident.getCurrentArena();
        if (arena == null) {
            return;
        }

        if (!event.hasBlock()) {
            return;
        }

        BlockCoord bcoord = new BlockCoord(event.getClickedBlock().getLocation());
        if (ArenaManager.chests.containsKey(bcoord)) {
            if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                Player player;
                try {
                    player = CivGlobal.getPlayer(resident);
                    player.closeInventory();
                } catch (CivException ignored) {
                }

                TaskMaster.syncTask(() -> {
                    try {
                        CivGlobal.getPlayer(resident).openInventory(arena.getInventory(resident));
                    } catch (CivException ignored) {
                    }

                }, 0);
                event.setCancelled(true);
                return;
            }
        }


        /* Did we click on a respawn sign. */
        if (!ArenaManager.respawnSigns.containsKey(bcoord)) {
            return;
        }

        TaskMaster.syncTask(new Runnable() {
            @Override
            public void run() {
                if (!DateUtil.isAfterSeconds(resident.getLastKilledTime(), resident.getRespawnTimeArena())) {
                    long secondsLeft = resident.getRespawnTimeArena() - (new Date().getTime() - resident.getLastKilledTime().getTime()) / 1000;
                    CivMessage.sendError(resident, CivSettings.localize.localizedString("var_arena_respawningIn", secondsLeft));
                    TaskMaster.syncTask(this, TimeTools.toTicks(1));
                    return;
                }
                BlockCoord revive = arena.getRandomReviveLocation(resident);
                if (revive == null) {
                    return;
                }
                Location loc = revive.getCenteredLocation();
                World world = Bukkit.getWorld(arena.getInstanceName());
                loc.setWorld(world);
                CivMessage.send(resident, CivColor.LightGray + CivSettings.localize.localizedString("arena_revived"));

                try {
                    CivGlobal.getPlayer(resident).teleport(loc);
                } catch (CivException e) {
                    e.printStackTrace();
                }

            }
        });
    }

}
