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

import com.avrgaming.civcraft.cache.ArrowFiredCache;
import com.avrgaming.civcraft.cache.CannonFiredCache;
import com.avrgaming.civcraft.cache.CivCache;
import com.avrgaming.civcraft.camp.Camp;
import com.avrgaming.civcraft.camp.CampBlock;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.*;
import com.avrgaming.civcraft.permission.PlotPermissions;
import com.avrgaming.civcraft.structure.*;
import com.avrgaming.civcraft.structure.farm.FarmChunk;
import com.avrgaming.civcraft.structure.wonders.GrandShipIngermanland;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.FireWorkTask;
import com.avrgaming.civcraft.threading.tasks.StructureBlockHitEvent;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.ItemFrameStorage;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.civcraft.war.WarRegen;
import gpl.HorseModifier;
import net.minecraft.nbt.NBTTagCompound;
import org.bukkit.*;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class BlockListener implements Listener {

    /* Experimental, reuse the same object because it is single threaded. */
    public static ChunkCoord coord = new ChunkCoord("", 0, 0);
    public static BlockCoord bcoord = new BlockCoord("", 0, 0, 0);

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityTameEvent(EntityTameEvent event) {
        if (event.getEntity() instanceof Wolf) {
            if (event.getEntity().getName().contains("Direwolf")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onSlimeSplitEvent(SlimeSplitEvent event) {
        if (event.getEntity() == null) {
            return;
        }
        Slime slime = event.getEntity();
        if (slime.getName().contains("Brutal") ||
                slime.getName().contains("Elite") ||
                slime.getName().contains("Greater") ||
                slime.getName().contains("Lesser")) {
            slime.setSize(0);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockIgniteEvent(BlockIgniteEvent event) {
        //	CivLog.debug("block ignite event");

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block b = event.getBlock().getRelative(x, y, z);
                    bcoord.setFromLocation(b.getLocation());
                    StructureBlock sb = CivGlobal.getStructureBlock(bcoord);
                    if (sb != null) {
                        if (b.getType().isBurnable()) {
                            event.setCancelled(true);
                        }
                        return;
                    }

                    CampBlock cb = CivGlobal.getCampBlock(bcoord);
                    if (cb != null) {
                        event.setCancelled(true);
                        return;
                    }

                    StructureSign structSign = CivGlobal.getStructureSign(bcoord);
                    if (structSign != null) {
                        event.setCancelled(true);
                        return;
                    }

                    StructureChest structChest = CivGlobal.getStructureChest(bcoord);
                    if (structChest != null) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }


        coord.setFromLocation(event.getBlock().getLocation());
        TownChunk tc = CivGlobal.getTownChunk(coord);

        if (tc == null) {
            return;
        }

        if (!tc.perms.isFire()) {
            CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("fireDisabledInChunk"));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityBlockChange(EntityChangeBlockEvent event) {
        bcoord.setFromLocation(event.getBlock().getLocation());

        if (CivGlobal.getStructureBlock(bcoord) != null) {
            event.setCancelled(true);
            return;
        }

        if (CivGlobal.getCampBlock(bcoord) != null) {
            event.setCancelled(true);
        }

    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBurnEvent(BlockBurnEvent event) {
        bcoord.setFromLocation(event.getBlock().getLocation());

        if (CivGlobal.getStructureBlock(bcoord) != null) {
            event.setCancelled(true);
            return;
        }

        if (CivGlobal.getCampBlock(bcoord) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onProjectileHitEvent(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow) {
            ArrowFiredCache afc = CivCache.arrowsFired.get(event.getEntity().getUniqueId());
            if (afc != null) {
                afc.setHit(true);
            }
        }

        if (!(event.getEntity() instanceof Fireball)) {
            return;
        }
        CannonFiredCache cfc = CivCache.cannonBallsFired.get(event.getEntity().getUniqueId());
        if (cfc == null) {
            return;
        }

        cfc.setHit(true);

        FireworkEffect fe = FireworkEffect.builder().withColor(Color.RED).withColor(Color.BLACK).flicker(true).with(Type.BURST).build();

        Random rand = new Random();
        int spread = 30;
        for (int i = 0; i < 15; i++) {
            int x = rand.nextInt(spread) - spread / 2;
            int y = rand.nextInt(spread) - spread / 2;
            int z = rand.nextInt(spread) - spread / 2;


            Location loc = event.getEntity().getLocation();
            Location location = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ());
            location.add(x, y, z); // TODO: сокол помоги тут

            TaskMaster.syncTask(new FireWorkTask(fe, loc.getWorld(), loc, 5), rand.nextInt(30));
        }

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        /* Protect the Protected Item Frames! */
        if (event.getEntity() instanceof ItemFrame) {
            if (CivGlobal.getProtectedItemFrame(event.getEntity().getUniqueId()) != null) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getDamager() instanceof LightningStrike) {
//			CivLog.debug("onEntityDamageByEntityEvent LightningStrike: "+event.getDamager().getUniqueId());
            try {
                event.setDamage(CivSettings.getInteger(CivSettings.warConfig, "tesla_tower.damage"));
            } catch (InvalidConfiguration e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        event.getDamager();

        if (event.getDamager() instanceof Fireball) {
            CannonFiredCache cfc = CivCache.cannonBallsFired.get(event.getDamager().getUniqueId());
            if (cfc != null) {

                cfc.setHit(true);
                cfc.destroy(event.getDamager());
                Buildable whoFired = cfc.getWhoFired();
                if (whoFired.getConfigId().equals("s_cannontower")) {
                    event.setDamage(((CannonTower) whoFired).getDamage());
                } else if (whoFired.getConfigId().equals("s_cannonship")) {
                    event.setDamage(((CannonShip) whoFired).getDamage());
                } else if (whoFired.getConfigId().equals("w_grand_ship_ingermanland")) {
                    event.setDamage(((GrandShipIngermanland) whoFired).getCannonDamage());
                }
            }
        }

        if (!(event.getEntity() instanceof Player defender)) {
            return;
        }

        /* Only protect against players and entities that players can throw. */
        if (!CivSettings.playerEntityWeapons.contains(event.getDamager().getType())) {
            return;
        }

        coord.setFromLocation(event.getEntity().getLocation());
        TownChunk tc = CivGlobal.getTownChunk(coord);
        if (tc == null) {
            return;
        }
        boolean allowPVP = false;
        String denyMessage = "";

        Player attacker = null;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            LivingEntity shooter = (LivingEntity) ((Projectile) event.getDamager()).getShooter();
            if (shooter instanceof Player) {
                attacker = (Player) shooter;
            }
        }

        if (attacker == null) {
            /* Attacker wasnt a player or known projectile, allow it. */
            allowPVP = true;
        } else {
            switch (playersCanPVPHere(attacker, defender, tc)) {
                case ALLOWED -> allowPVP = true;
                case NOT_AT_WAR -> {
                    allowPVP = false;
                    denyMessage = CivSettings.localize.localizedString("var_pvpError1", defender.getName());
                }
                case NEUTRAL_IN_WARZONE -> {
                    allowPVP = false;
                    denyMessage = CivSettings.localize.localizedString("var_pvpError2", defender.getName());
                }
                case NON_PVP_ZONE -> {
                    allowPVP = false;
                    denyMessage = CivSettings.localize.localizedString("var_pvpError3", defender.getName());
                }
            }
        }

        if (!allowPVP) {
            CivMessage.sendError(attacker, denyMessage);
            event.setCancelled(true);
        }

    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void OnCreateSpawnEvent(CreatureSpawnEvent event) {

        if (event.getSpawnReason().equals(SpawnReason.BREEDING)) {
            Pasture pasture = Pasture.pastureChunks.get(new ChunkCoord(event.getEntity().getLocation()));
            if (pasture != null) {
                pasture.onBreed(event.getEntity());
            }
        }

        if (event.getEntityType() == EntityType.HORSE) {
            if (Stable.stableChunks.get(new ChunkCoord(event.getEntity().getLocation())) != null) {
                return;
            }

            if (event.getSpawnReason().equals(SpawnReason.DEFAULT)) {
                LivingEntity entity = event.getEntity();
                TaskMaster.syncTask(() -> {
                    if (entity != null) {
                        if (!HorseModifier.isCivCraftHorse(entity)) {
                            CivLog.warning("Removing a normally spawned horse.");
                            entity.remove();
                        }
                    }
                });
                return;
            }

            CivLog.warning("Canceling horse spawn reason:" + event.getSpawnReason().name());
            event.setCancelled(true);
        }

        coord.setFromLocation(event.getLocation());
        TownChunk tc = CivGlobal.getTownChunk(coord);
        if (tc == null) {
            return;
        }

        if (tc.perms.isMobs()) {
            return;
        }
        if (event.getSpawnReason().equals(SpawnReason.CUSTOM)) {
            return;
        }

        if (CivSettings.restrictedSpawns.containsKey(event.getEntityType())) {
            event.setCancelled(true);
        }
    }


    @EventHandler(priority = EventPriority.NORMAL)
    public void OnEntityExplodeEvent(EntityExplodeEvent event) {

        if (event.getEntity() == null) {
            return;
        }
        /* prevent ender dragons from breaking blocks. */
        if (event.getEntityType().equals(EntityType.ENDER_DRAGON)) {
            event.setCancelled(true);
        }

        for (Block block : event.blockList()) {
            bcoord.setFromLocation(block.getLocation());
            if (CivGlobal.getStructureBlock(bcoord) != null) {
                event.setCancelled(true);
                return;
            }

            if (CivGlobal.getCampBlock(bcoord) != null) {
                event.setCancelled(true);
                return;
            }

            if (CivGlobal.getStructureSign(bcoord) != null) {
                event.setCancelled(true);
                return;
            }

            if (CivGlobal.getStructureChest(bcoord) != null) {
                event.setCancelled(true);
                return;
            }

            coord.setFromLocation(block.getLocation());

            if (CivGlobal.getTownChunk(coord) == null) {
                continue;
            }
            event.setCancelled(true);
            return;
        }

    }

    private final BlockFace[] faces = new BlockFace[]{
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.SELF,
            BlockFace.UP
    };

    public BlockCoord generatesCobble(Material id, Block b) {
        Material mirrorID1 = (id == Material.WATER ? Material.LAVA : Material.WATER);
        Material mirrorID2 = (id == Material.WATER ? Material.LAVA : Material.WATER);
        Material mirrorID3 = (id == Material.WATER ? Material.LAVA : Material.WATER);
        Material mirrorID4 = (id == Material.WATER ? Material.LAVA : Material.WATER);
        for (BlockFace face : faces) {
            Block r = b.getRelative(face, 1);
            if (r.getType() == mirrorID1 || r.getType() == mirrorID2 ||
                    r.getType() == mirrorID3 || r.getType() == mirrorID4) {
                return new BlockCoord(r);
            }
        }

        return null;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void OnBlockFromToEvent(BlockFromToEvent event) {
        /* Disable cobblestone generators. */
        Material id = event.getBlock().getType();
        if (id != Material.WATER && id != Material.LAVA) {
            return;
        }
        Block b = event.getToBlock();
        bcoord.setFromLocation(b.getLocation());

        Material toid = b.getType();
        if (toid == Material.COBBLESTONE || toid == Material.OBSIDIAN) {
            BlockCoord other = generatesCobble(id, b);
            if (other != null && other.getBlock().getType() != Material.AIR) {
                other.getBlock().setType(Material.NETHERRACK);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void OnBlockFormEvent(BlockFormEvent event) {
        /* Disable cobblestone generators. */
        if (event.getNewState().getType() == Material.COBBLESTONE || event.getNewState().getType() == Material.OBSIDIAN) {
            event.getNewState().setData(new MaterialData(Material.NETHERRACK));
            return;
        }

        Chunk spreadChunk = event.getNewState().getChunk();
        coord.setX(spreadChunk.getX());
        coord.setZ(spreadChunk.getZ());
        coord.setWorldname(spreadChunk.getWorld().getName());

        TownChunk tc = CivGlobal.getTownChunk(coord);
        if (tc == null) {
            return;
        }

        if (!tc.perms.isFire()) {
            if (event.getNewState().getType() == Material.FIRE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void OnBlockPlaceEvent(BlockPlaceEvent event) {
        Resident resident = CivGlobal.getResident(event.getPlayer());

        if (resident == null) {
            event.setCancelled(true);
            return;
        }

        if (resident.isSBPermOverride()) {
            return;
        }

        bcoord.setFromLocation(event.getBlockAgainst().getLocation());
        StructureSign sign = CivGlobal.getStructureSign(bcoord);
        if (sign != null) {
            event.setCancelled(true);
            return;
        }

        bcoord.setFromLocation(event.getBlock().getLocation());
        StructureBlock sb = CivGlobal.getStructureBlock(bcoord);
        if (sb != null) {
            event.setCancelled(true);
            CivMessage.sendError(event.getPlayer(),
                    CivSettings.localize.localizedString("blockBreak_errorStructure") + " " + sb.getOwner().getDisplayName() + " " + CivSettings.localize.localizedString("blockBreak_errorOwnedBy") + " " + sb.getTown().getName());
            return;
        }

        CampBlock cb = CivGlobal.getCampBlock(bcoord);
        if (cb != null && !cb.canBreak(event.getPlayer().getName())) {
            event.setCancelled(true);
            CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("blockBreak_errorCamp1") + " " + cb.getCamp().getName() + " " + CivSettings.localize.localizedString("blockBreak_errorOwnedBy") + " " + cb.getCamp().getOwner().getName());
            return;
        }

        coord.setFromLocation(event.getBlock().getLocation());
        TownChunk tc = CivGlobal.getTownChunk(coord);
        if (CivSettings.blockPlaceExceptions.get(event.getBlock().getType()) != null) {
            return;
        }

        if (tc != null) {
            if (!tc.perms.hasPermission(PlotPermissions.Type.BUILD, resident)) {
                if (War.isWarTime() && resident.hasTown() &&
                        resident.getTown().getCiv().getDiplomacyManager().atWarWith(tc.getTown().getCiv())) {
                    if (WarRegen.canPlaceThisBlock(event.getBlock())) {
                        WarRegen.saveBlock(event.getBlock(), tc.getTown().getName(), true);
                        return;
                    } else {
                        CivMessage.sendErrorNoRepeat(event.getPlayer(), CivSettings.localize.localizedString("blockPlace_errorWar"));
                        event.setCancelled(true);
                        return;
                    }
                } else {
                    event.setCancelled(true);
                    CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("blockPlace_errorPermission") + " " + tc.getTown().getName());
                }
            }
        }

        /* Check if we're going to break too many structure blocks beneath a structure. */
        //LinkedList<StructureBlock> sbList = CivGlobal.getStructureBlocksAt(bcoord.getWorldname(), bcoord.getX(), bcoord.getZ());
        HashSet<Buildable> buildables = CivGlobal.getBuildablesAt(bcoord);
        if (buildables == null) {
            return;
        }
        for (Buildable buildable : buildables) {
            if (!buildable.validated) {
                buildable.validate(event.getPlayer());
                continue;
            }

            /* Building is validated, grab the layer and determine if this would set it over the limit. */
            BuildableLayer layer = buildable.layerValidPercentages.get(bcoord.getY());
            if (layer == null) {
                continue;
            }

            /* Update the layer. */
            layer.current += Buildable.getReinforcementValue(event.getBlockPlaced().getType());
            if (layer.current < 0) {
                layer.current = 0;
            }
            buildable.layerValidPercentages.put(bcoord.getY(), layer);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void OnBlockBreakEvent(BlockBreakEvent event) {
        Resident resident = CivGlobal.getResident(event.getPlayer());

        if (resident == null) {
            event.setCancelled(true);
            return;
        }

        if (resident.isSBPermOverride()) {
            return;
        }

        bcoord.setFromLocation(event.getBlock().getLocation());
        StructureBlock sb = CivGlobal.getStructureBlock(bcoord);

        if (sb != null) {
            event.setCancelled(true);
            TaskMaster.syncTask(new StructureBlockHitEvent(event.getPlayer().getName(), bcoord, sb, event.getBlock().getWorld()), 0);
            // TODO fix if any problems with that :/

            return;
        }

        if (CivGlobal.getProtectedBlock(bcoord) != null) {
            event.setCancelled(true);
            CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("blockBreak_errorProtected"));
            return;
        }

        CampBlock cb = CivGlobal.getCampBlock(bcoord);
        if (cb != null && !cb.canBreak(event.getPlayer().getName())) {
            ControlPoint cBlock = cb.getCamp().controlBlocks.get(bcoord);
            if (cBlock != null) {
                cb.getCamp().onDamage(1, event.getBlock().getWorld(), event.getPlayer(), bcoord, null);
                event.setCancelled(true);
            } else {
                event.setCancelled(true);
                CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("blockBreak_errorCamp1") + " " + cb.getCamp().getName() + " " + CivSettings.localize.localizedString("blockBreak_errorOwnedBy") + " " + cb.getCamp().getOwner().getName());
            }
            return;
        }

        if (CivGlobal.getStructureSign(bcoord) != null && !resident.isSBPermOverride()) {
            event.setCancelled(true);
            CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("blockBreak_errorStructureSign"));
            return;
        }

        StructureChest structChest = CivGlobal.getStructureChest(bcoord);
        if (structChest != null && !resident.isSBPermOverride()) {
            event.setCancelled(true);
            CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("blockBreak_errorStructureChests"));
            return;
        }

        coord.setFromLocation(event.getBlock().getLocation());

        TownChunk tc = CivGlobal.getTownChunk(coord);
        if (tc != null) {
            if (!tc.perms.hasPermission(PlotPermissions.Type.DESTROY, resident)) {
                event.setCancelled(true);

                if (War.isWarTime() && resident.hasTown() &&
                        resident.getTown().getCiv().getDiplomacyManager().atWarWith(tc.getTown().getCiv())) {
                    WarRegen.destroyThisBlock(event.getBlock(), tc.getTown());
                } else {
                    CivMessage.sendErrorNoRepeat(event.getPlayer(), CivSettings.localize.localizedString("blockBreak_errorPermission") + " " + tc.getTown().getName());
                }
            }
        }

        /* Check if we're going to break too many structure blocks beneath a structure. */
        //LinkedList<StructureBlock> sbList = CivGlobal.getStructureBlocksAt(bcoord.getWorldname(), bcoord.getX(), bcoord.getZ());
        HashSet<Buildable> buildables = CivGlobal.getBuildablesAt(bcoord);
        if (buildables != null) {
            for (Buildable buildable : buildables) {
                if (!buildable.validated) {
                    buildable.validate(event.getPlayer());
                    continue;
                }

                /* Building is validated, grab the layer and determine if this would set it over the limit. */
                BuildableLayer layer = buildable.layerValidPercentages.get(bcoord.getY());
                if (layer == null) {
                    continue;
                }

                double current = layer.current - Buildable.getReinforcementValue(event.getBlock().getType());
                if (current < 0) {
                    current = 0;
                }
                double percentValid = current / (double) layer.max;

                if (percentValid < Buildable.validPercentRequirement) {
                    CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("blockBreak_errorSupport") + " " + buildable.getDisplayName());
                    event.setCancelled(true);
                    return;
                }

                /* Update the layer. */
                layer.current = (int) current;
                buildable.layerValidPercentages.put(bcoord.getY(), layer);
            }
        }

    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void OnEntityInteractEvent(EntityInteractEvent event) {
        if (event.getBlock() != null) {
            if (CivSettings.switchItems.contains(event.getBlock().getType())) {
                coord.setFromLocation(event.getBlock().getLocation());
                TownChunk tc = CivGlobal.getTownChunk(coord);

                if (tc == null) {
                    return;
                }

                /* A non-player entity is trying to trigger something, if interact permission is
                 * off for others then disallow it.
                 */
                if (tc.perms.interact.isPermitOthers()) {
                    return;
                }

                if (event.getEntity() instanceof Player) {
                    CivMessage.sendErrorNoRepeat(event.getEntity(), CivSettings.localize.localizedString("blockUse_errorPermission"));
                }

                event.setCancelled(true);
            }
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void OnPlayerConsumeEvent(PlayerItemConsumeEvent event) {
        ItemStack stack = event.getItem();

        /* Disable notch apples */
        ItemStack stack1 = event.getItem();
        if (stack1.getType() == Material.GOLDEN_APPLE) {
            if (event.getItem().getDurability() == (short) 0x1) {
                CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("itemUse_errorGoldenApple"));
                event.setCancelled(true);
                return;
            }
        }

        if (stack.getType().equals(Material.POTION)) {
            if ((event.getItem().getDurability() & 0x000F) == 0xE) {
                event.setCancelled(true);
                CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("itemUse_errorInvisPotion"));
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockDispenseEvent(BlockDispenseEvent event) {
        if (event.getItem() == null) {
            return;
        }
        if (event.getItem().getType() == Material.POTION) {
            if ((event.getItem().getDurability() & 0x000F) == 0xE) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getItem().getType() == Material.INK_SACK) {
            //if (event.getItem().getDurability() == 15) {
            event.setCancelled(true);
            //}
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void OnPlayerInteractEvent(PlayerInteractEvent event) {
        if (CivGlobal.getResident(event.getPlayer()) == null) {
            event.setCancelled(true);
            return;
        }

        if (event.isCancelled()) {
            // Fix for bucket bug.
            if (event.getAction() == Action.RIGHT_CLICK_AIR) {
                Material item = event.getPlayer().getInventory().getItemInMainHand().getType();
                // block cheats for placing water/lava/fire/lighter use.
                if (item == Material.WATER_BUCKET ||
                        item == Material.LAVA_BUCKET ||
                        item == Material.FLINT_AND_STEEL ||
                        item == Material.WATER ||
                        item == Material.LAVA ||
                        item == Material.FIRE) {
                    event.setCancelled(true);
                }
            }
            return;
        }

        if (event.hasItem()) {

            if (event.getItem().getType() == Material.POTION) {
                if ((event.getItem().getDurability() & 0x000F) == 0xE) {
                    event.setCancelled(true);
                    CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("itemUse_errorInvisPotion"));
                    return;
                }
            }

            if (event.getItem().getType().equals(Material.INK_SAC) && event.getItem().getDurability() == 15) {
                Block clickedBlock = event.getClickedBlock();
                if (clickedBlock.getType() == Material.WHEAT ||
                        clickedBlock.getType() == Material.CARROT ||
                        clickedBlock.getType() == Material.POTATO) {
                    event.setCancelled(true);
                    CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("itemUse_errorBoneMeal"));
                    return;
                }
            }
        }

        Block soilBlock = event.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN);

        // prevent players trampling crops
        if ((event.getAction() == Action.PHYSICAL)) {
            if ((soilBlock.getType() == Material.FARMLAND) || (soilBlock.getType() == Material.WHEAT)) {
                //CivLog.debug("no crop cancel.");
                event.setCancelled(true);
                return;
            }
        }
        /*
         * Right clicking causes some dupe bugs for some reason with items that have "actions" such as swords.
         * It also causes block place events on top of signs. So we'll just only allow signs to work with left click.
         */
        boolean leftClick = event.getAction().equals(Action.LEFT_CLICK_AIR) || event.getAction().equals(Action.LEFT_CLICK_BLOCK);

        if (event.getClickedBlock() != null) {

            if (MarkerPlacementManager.isPlayerInPlacementMode(event.getPlayer())) {
                Block block;
                if (event.getBlockFace().equals(BlockFace.UP)) {
                    block = event.getClickedBlock().getRelative(event.getBlockFace());
                } else {
                    block = event.getClickedBlock();
                }

                try {
                    MarkerPlacementManager.setMarker(event.getPlayer(), block.getLocation());
                    CivMessage.send(event.getPlayer(), ChatColor.GREEN + CivSettings.localize.localizedString("itemUse_marked"));
                } catch (CivException e) {
                    CivMessage.send(event.getPlayer(), ChatColor.RED + e.getMessage());
                }

                event.setCancelled(true);
                return;
            }

            // Check for clicked structure signs.
            bcoord.setFromLocation(event.getClickedBlock().getLocation());
            StructureSign sign = CivGlobal.getStructureSign(bcoord);
            if (sign != null) {

                if (leftClick || sign.isAllowRightClick()) {
                    if (sign.getOwner() != null && sign.getOwner().isActive()) {
                        try {
                            sign.getOwner().processSignAction(event.getPlayer(), sign, event);
                            event.setCancelled(true);
                        } catch (CivException e) {
                            CivMessage.send(event.getPlayer(), ChatColor.RED + e.getMessage());
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
                return;
            }
            if (CivSettings.switchItems.contains(event.getClickedBlock().getType())) {
                OnPlayerSwitchEvent(event);
                if (event.isCancelled()) {
                    return;
                }
            }
        }

        if (!event.hasItem()) {
            return;
        }

        if (event.getItem() == null) {
            return;
        }
        if (CivSettings.restrictedItems.containsKey(event.getItem().getType())) {
            OnPlayerUseItem(event);
            event.isCancelled();
        }

    }

    public void OnPlayerBedEnterEvent(PlayerBedEnterEvent event) {

        Resident resident = CivGlobal.getResident(event.getPlayer().getName());

        if (resident == null) {
            event.setCancelled(true);
            return;
        }

        coord.setFromLocation(event.getPlayer().getLocation());
        Camp camp = CivGlobal.getCampFromChunk(coord);
        if (camp != null) {
            if (!camp.hasMember(event.getPlayer().getName())) {
                CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("bedUse_errorNotInCamp"));
                event.setCancelled(true);
            }
        }
    }

    public static void OnPlayerSwitchEvent(PlayerInteractEvent event) {

        if (event.getClickedBlock() == null) {
            return;
        }

        Resident resident = CivGlobal.getResident(event.getPlayer().getName());

        if (resident == null) {
            event.setCancelled(true);
            return;
        }

        bcoord.setFromLocation(event.getClickedBlock().getLocation());
        CampBlock cb = CivGlobal.getCampBlock(bcoord);
        if (cb != null && !resident.isPermOverride()) {
            if (!cb.getCamp().hasMember(resident.getName())) {
                CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("blockUse_errorNotInCamp"));
                event.setCancelled(true);
                return;
            }
        }

        coord.setFromLocation(event.getClickedBlock().getLocation());
        TownChunk tc = CivGlobal.getTownChunk(coord);

        if (tc == null) {
            return;
        }

        if (resident.hasTown()) {
            if (War.isWarTime()) {
                if (tc.getTown().getCiv().getDiplomacyManager().atWarWith(resident.getTown().getCiv())) {

                    switch (event.getClickedBlock().getType()) {
                        case OAK_DOOR, IRON_DOOR, SPRUCE_DOOR, BIRCH_DOOR, JUNGLE_DOOR, ACACIA_DOOR, DARK_OAK_DOOR, ACACIA_FENCE_GATE, BIRCH_FENCE_GATE, DARK_OAK_FENCE_GATE, FENCE_GATE, SPRUCE_FENCE_GATE, JUNGLE_FENCE_GATE -> {
                            return;
                        }
                        default -> {
                        }
                    }
                }
            }
        }

        event.getClickedBlock().getType();

        if (!tc.perms.hasPermission(PlotPermissions.Type.INTERACT, resident)) {
            event.setCancelled(true);

            if (War.isWarTime() && resident.hasTown() &&
                    resident.getTown().getCiv().getDiplomacyManager().atWarWith(tc.getTown().getCiv())) {
                WarRegen.destroyThisBlock(event.getClickedBlock(), tc.getTown());
            } else {
                CivMessage.sendErrorNoRepeat(event.getPlayer(), CivSettings.localize.localizedString("blockUse_errorGeneric") + " " + event.getClickedBlock().getType().toString());
            }
        }

    }

    private void OnPlayerUseItem(PlayerInteractEvent event) {
        Location loc = (event.getClickedBlock() == null) ?
                event.getPlayer().getLocation() :
                event.getClickedBlock().getLocation();

        ItemStack stack = event.getItem();

        coord.setFromLocation(event.getPlayer().getLocation());
        Camp camp = CivGlobal.getCampFromChunk(coord);
        if (camp != null) {
            if (!camp.hasMember(event.getPlayer().getName())) {
                CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("itemUse_errorCamp") + " " + stack.getType().toString());
                event.setCancelled(true);
                return;
            }
        }

        TownChunk tc = CivGlobal.getTownChunk(loc);
        if (tc == null) {
            return;
        }

        Resident resident = CivGlobal.getResident(event.getPlayer().getName());

        if (resident == null) {
            event.setCancelled(true);
        }

        if (!tc.perms.hasPermission(PlotPermissions.Type.ITEMUSE, resident)) {
            event.setCancelled(true);
            CivMessage.sendErrorNoRepeat(event.getPlayer(), CivSettings.localize.localizedString("itemUse_errorGeneric") + " " + stack.getType().toString() + " ");
        }

    }

    /*
     * Handles rotating of itemframes
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void OnPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {

        if (event.getRightClicked().getType().equals(EntityType.HORSE)) {
            if (!HorseModifier.isCivCraftHorse((LivingEntity) event.getRightClicked())) {
                CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("horseUse_invalidHorse"));
                event.setCancelled(true);
                event.getRightClicked().remove();
                return;
            }
        }

        ItemStack inHand = event.getPlayer().getInventory().getItemInMainHand();
        if (inHand != null) {

            boolean denyBreeding = false;
            switch (event.getRightClicked().getType()) {
                case COW, SHEEP, MUSHROOM_COW -> {
                    if (inHand.getType().equals(Material.WHEAT)) {
                        denyBreeding = true;
                    }
                }
                case PIG -> {
                    if (inHand.getType().equals(Material.CARROT)) {
                        denyBreeding = true;
                    }
                }
                case HORSE -> {
                    if (inHand.getType().equals(Material.GOLDEN_APPLE) ||
                            inHand.getType().equals(Material.GOLDEN_CARROT)) {
                        CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("itemUse_errorNoHorseBreeding"));
                        event.setCancelled(true);
                        return;
                    }
                }
                case CHICKEN -> {
                    if (inHand.getType().equals(Material.WHEAT_SEEDS) ||
                            inHand.getType().equals(Material.MELON_SEEDS) ||
                            inHand.getType().equals(Material.PUMPKIN_SEEDS)) {
                        denyBreeding = true;
                    }
                }
                case RABBIT -> {
                    if (inHand.getType().equals(Material.CARROT) ||
                            inHand.getType().equals(Material.GOLDEN_CARROT) ||
                            inHand.getType().equals(Material.YELLOW_FLOWER)) {
                        denyBreeding = true;
                    }
                }
                default -> {
                }
            }

            if (denyBreeding) {
                ChunkCoord coord = new ChunkCoord(event.getPlayer().getLocation());
                Pasture pasture = Pasture.pastureChunks.get(coord);

                if (pasture == null) {
                    CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("itemUse_errorNoWildBreeding"));
                    event.setCancelled(true);
                } else {
                    int loveTicks;
                    NBTTagCompound tag = new NBTTagCompound();
                    ((CraftEntity) event.getRightClicked()).getHandle().c(tag);
                    loveTicks = tag.getInt("InLove");

                    if (loveTicks == 0) {
                        if (!pasture.processMobBreed(event.getPlayer(), event.getRightClicked().getType())) {
                            event.setCancelled(true);
                        }
                    } else {
                        event.setCancelled(true);
                    }
                }

                return;
            }
        }
        if (!(event.getRightClicked() instanceof ItemFrame) && !(event.getRightClicked() instanceof Painting)) {
            return;
        }

        coord.setFromLocation(event.getPlayer().getLocation());
        TownChunk tc = CivGlobal.getTownChunk(coord);
        if (tc == null) {
            return;
        }

        Resident resident = CivGlobal.getResident(event.getPlayer().getName());
        if (resident == null) {
            return;
        }

        if (!tc.perms.hasPermission(PlotPermissions.Type.INTERACT, resident)) {
            event.setCancelled(true);
            CivMessage.sendErrorNoRepeat(event.getPlayer(), CivSettings.localize.localizedString("itemUse_errorPaintingOrFrame"));
        }

    }


    /*
     * Handles breaking of paintings and itemframes.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void OnHangingBreakByEntityEvent(HangingBreakByEntityEvent event) {
        //	CivLog.debug("hanging painting break event");

        ItemFrameStorage frameStore = CivGlobal.getProtectedItemFrame(event.getEntity().getUniqueId());
        if (frameStore != null) {
            if (event.getRemover() instanceof Player) {
                CivMessage.sendError(event.getRemover(), CivSettings.localize.localizedString("blockBreak_errorItemFrame"));
            }
            event.setCancelled(true);
            return;
        }

        if (event.getRemover() instanceof Player player) {

            coord.setFromLocation(player.getLocation());
            TownChunk tc = CivGlobal.getTownChunk(coord);

            if (tc == null) {
                return;
            }

            Resident resident = CivGlobal.getResident(player.getName());
            if (resident == null) {
                event.setCancelled(true);
            }

            if (!tc.perms.hasPermission(PlotPermissions.Type.DESTROY, resident)) {
                event.setCancelled(true);
                CivMessage.sendErrorNoRepeat(player, CivSettings.localize.localizedString("blockBreak_errorFramePermission"));
            }
        }


    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChunkUnloadEvent(ChunkUnloadEvent event) {
        Boolean persist = CivGlobal.isPersistChunk(event.getChunk());
        if (persist != null && persist) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChunkLoadEvent(ChunkLoadEvent event) {
        ChunkCoord coord = new ChunkCoord(event.getChunk());
        FarmChunk fc = CivGlobal.getFarmChunk(coord);
        if (fc == null) {
            return;
        }

        for (org.bukkit.entity.Entity ent : event.getChunk().getEntities()) {
            if (ent.getType().equals(EntityType.ZOMBIE)) {
                ent.remove();
            }
        }

        class AsyncTask extends CivAsyncTask {

            final FarmChunk fc;

            public AsyncTask(FarmChunk fc) {
                this.fc = fc;
            }

            @Override
            public void run() {
                if (fc.getMissedGrowthTicks() > 0) {
                    fc.processMissedGrowths(false, this);
                    fc.getFarm().saveMissedGrowths();
                }
            }

        }

        TaskMaster.syncTask(new AsyncTask(fc), 500);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {

        Pasture pasture = Pasture.pastureEntities.get(event.getEntity().getUniqueId());
        if (pasture != null) {
            pasture.onEntityDeath(event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockGrowEvent(BlockGrowEvent event) {
        bcoord.setFromLocation(event.getBlock().getLocation().add(0, -1, 0));
        if (CivGlobal.vanillaGrowthLocations.contains(bcoord)) {
            /* Allow vanilla growth on these plots. */
            return;
        }

        Block b = event.getBlock();

        if (Farm.isBlockControlled(b)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityBreakDoor(EntityBreakDoorEvent event) {
        bcoord.setFromLocation(event.getBlock().getLocation());
        StructureBlock sb = CivGlobal.getStructureBlock(bcoord);
        if (sb != null) {
            event.setCancelled(true);
        }

        CampBlock cb = CivGlobal.getCampBlock(bcoord);
        if (cb != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onCreatureSpawnEvent(CreatureSpawnEvent event) {
        if (War.isWarTime() && !event.getEntity().getType().equals(EntityType.HORSE)) {
            if (!event.getSpawnReason().equals(SpawnReason.BREEDING)) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getEntity().getType().equals(EntityType.CHICKEN)) {
            if (event.getSpawnReason().equals(SpawnReason.EGG)) {
                event.setCancelled(true);
                return;
            }
            NBTTagCompound compound = new NBTTagCompound();
            if (compound.getBoolean("IsChickenJockey")) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getEntity().getType().equals(EntityType.IRON_GOLEM) &&
                event.getSpawnReason().equals(SpawnReason.BUILD_IRONGOLEM)) {
            event.setCancelled(true);
            return;
        }
        switch (event.getEntityType()) {
            case ZOMBIE, ZOMBIE_VILLAGER, ZOMBIE_HORSE, SKELETON_HORSE, SKELETON, WITCH, ENDERMAN, SILVERFISH, OCELOT, WOLF, CREEPER, SPIDER, CAVE_SPIDER, BAT, STRAY, HUSK, EVOKER, VINDICATOR, VILLAGER ->
                    event.setCancelled(true);
        }

        if (event.getSpawnReason().equals(SpawnReason.SPAWNER)) {
            event.setCancelled(true);
        }
    }

    public boolean allowPistonAction(Location loc) {
        bcoord.setFromLocation(loc);
        StructureBlock sb = CivGlobal.getStructureBlock(bcoord);
        if (sb != null) {
            return false;
        }

        CampBlock cb = CivGlobal.getCampBlock(bcoord);
        if (cb != null) {
            return false;
        }

        /*
         * If we're next to an attached protected item frame. Disallow
         * we cannot break protected item frames.
         *
         * Only need to check blocks directly next to us.
         */
        BlockCoord bcoord2 = new BlockCoord(bcoord);
        bcoord2.setX(bcoord.getX() - 1);
        if (ItemFrameStorage.attachedBlockMap.containsKey(bcoord2)) {
            return false;
        }

        bcoord2.setX(bcoord.getX() + 1);
        if (ItemFrameStorage.attachedBlockMap.containsKey(bcoord2)) {
            return false;
        }

        bcoord2.setZ(bcoord.getZ() - 1);
        if (ItemFrameStorage.attachedBlockMap.containsKey(bcoord2)) {
            return false;
        }

        bcoord2.setZ(bcoord.getZ() + 1);
        if (ItemFrameStorage.attachedBlockMap.containsKey(bcoord2)) {
            return false;
        }

        coord.setFromLocation(loc);

        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPistonExtendEvent(BlockPistonExtendEvent event) {

        /* UGH. If we extend into 'air' it doesnt count them as blocks...
         * we need to check air to prevent breaking of item frames...
         */
        final int PISTON_EXTEND_LENGTH = 13;
        Block currentBlock = event.getBlock().getRelative(event.getDirection());
        for (int i = 0; i < PISTON_EXTEND_LENGTH; i++) {
            if (currentBlock.getType() == Material.AIR) {
                if (!allowPistonAction(currentBlock.getLocation())) {
                    event.setCancelled(true);
                    return;
                }
            }

            currentBlock = currentBlock.getRelative(event.getDirection());
        }

        if (War.isWarTime()) {
            event.setCancelled(true);
            return;
        }

        coord.setFromLocation(event.getBlock().getLocation());
        FarmChunk fc = CivGlobal.getFarmChunk(coord);
        if (fc == null) {
            event.setCancelled(true);

        }

        for (Block block : event.getBlocks()) {
            if (!allowPistonAction(block.getLocation())) {
                event.setCancelled(true);
                break;

            }
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPistonRetractEvent(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (!allowPistonAction(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProjectileLaunchEvent(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof ThrownPotion potion)) {
            return;
        }
        if (!(potion.getShooter() instanceof Player)) {
            //Get Ruffian type here and change damage type based on the potion thrown
            //Also change effect based on ruffian type
            String entityName = null;
            LivingEntity shooter = (LivingEntity) potion.getShooter();
            Witch witch = (Witch) shooter;

            if (!(witch.getTarget() instanceof Player)) {
                return;
            }
            if (potion.getShooter() instanceof LivingEntity) {
                entityName = shooter.getCustomName();
            }
            if (entityName != null && entityName.endsWith(" Ruffian")) {
                EntityInsentient nmsEntity = (EntityInsentient) ((CraftLivingEntity) shooter).getHandle();
                AttributeInstance attribute = nmsEntity.getAttributeInstance(GenericAttributes.ATTACK_DAMAGE);
                double damage = attribute.getValue();

                class RuffianProjectile {
                    final Location loc;
                    final Location target;
                    final org.bukkit.entity.Entity attacker;
                    final int speed = 1;
                    final double damage;
                    final int splash = 6;

                    public RuffianProjectile(Location loc, Location target, org.bukkit.entity.Entity attacker, double damage) {
                        this.loc = loc;
                        this.target = target;
                        this.attacker = attacker;
                        this.damage = damage;
                    }

                    public Vector getVectorBetween(Location to, Location from) {
                        Vector dir = new Vector();

                        dir.setX(to.getX() - from.getX());
                        dir.setY(to.getY() - from.getY());
                        dir.setZ(to.getZ() - from.getZ());

                        return dir;
                    }

                    public boolean advance() {
                        Vector dir = getVectorBetween(target, loc).normalize();
                        double distance = loc.distanceSquared(target);
                        dir.multiply(speed);

                        loc.add(dir);
                        loc.getWorld().createExplosion(loc, 0.0f, false);
                        distance = loc.distanceSquared(target);

                        if (distance < speed * 1.5) {
                            loc.setX(target.getX());
                            loc.setY(target.getY());
                            loc.setZ(target.getZ());
                            this.onHit();
                            return true;
                        }

                        return false;
                    }

                    public void onHit() {
                        int spread = 3;
                        int[][] offset = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
                        for (int i = 0; i < 4; i++) {
                            int x = offset[i][0] * spread;
                            int y = 0;
                            int z = offset[i][1] * spread;

                            Location location = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ());
                            location = location.add(x, y, z);

                            launchExplodeFirework(location);
                        }

                        launchExplodeFirework(loc);
                        //loc.getWorld().createExplosion(loc, 1.0f, true);
                        damagePlayers(loc, splash);
                        //setFireAt(loc, spread);
                    }

                    @SuppressWarnings("deprecation")
                    private void damagePlayers(Location loc, int radius) {
                        double x = loc.getX() + 0.5;
                        double y = loc.getY() + 0.5;
                        double z = loc.getZ() + 0.5;

                        CraftWorld craftWorld = (CraftWorld) attacker.getWorld();

                        AxisAlignedBB bb = AxisAlignedBB(x - (double) radius, y - (double) radius, z - (double) radius, x + (double) radius, y + (double) radius, z + (double) radius);

                        List<net.minecraft.server.v1_12_R1.Entity> entities = craftWorld.getHandle().getEntities(((CraftEntity) attacker).getHandle(), bb);

                        for (net.minecraft.server.v1_12_R1.Entity e : entities) {
                            if (e instanceof EntityPlayer) {
                                EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(attacker, ((EntityPlayer) e).getBukkitEntity(), DamageCause.ENTITY_ATTACK, damage);
                                Bukkit.getServer().getPluginManager().callEvent(event);
                                e.damageEntity(DamageSource.GENERIC, (float) event.getDamage());
                            }
                        }

                    }


                    private AxisAlignedBB AxisAlignedBB(double d, double e,
                                                        double f, double g, double h, double i) {
                        return new AxisAlignedBB(d, e, f, g, h, i);
//						return null;
                    }

                    private void launchExplodeFirework(Location loc) {
                        FireworkEffect fe = FireworkEffect.builder().withColor(Color.ORANGE).withColor(Color.YELLOW).flicker(true).with(Type.BURST).build();
                        TaskMaster.syncTask(new FireWorkTask(fe, loc.getWorld(), loc, 3), 0);
                    }
                }


                class SyncFollow implements Runnable {
                    public RuffianProjectile proj;

                    @Override
                    public void run() {

                        if (proj.advance()) {
                            proj = null;
                            return;
                        }
                        TaskMaster.syncTask(this, 1);
                    }
                }

                SyncFollow follow = new SyncFollow();
                follow.proj = new RuffianProjectile(shooter.getLocation(),
                        witch.getTarget().getLocation(), (org.bukkit.entity.Entity) potion.getShooter(), damage);
                TaskMaster.syncTask(follow);


                event.setCancelled(true);
            }
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPotionSplashEvent(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();
        if (!(potion.getShooter() instanceof Player attacker)) {
            return;
        }

        for (PotionEffect effect : potion.getEffects()) {
            if (effect.getType().equals(PotionEffectType.INVISIBILITY)) {
                event.setCancelled(true);
                return;
            }
        }

        boolean protect = false;
        for (PotionEffect effect : potion.getEffects()) {
            if (effect.getType().equals(PotionEffectType.BLINDNESS) ||
                    effect.getType().equals(PotionEffectType.CONFUSION) ||
                    effect.getType().equals(PotionEffectType.HARM) ||
                    effect.getType().equals(PotionEffectType.POISON) ||
                    effect.getType().equals(PotionEffectType.SLOW) ||
                    effect.getType().equals(PotionEffectType.SLOW_DIGGING) ||
                    effect.getType().equals(PotionEffectType.WEAKNESS) ||
                    effect.getType().equals(PotionEffectType.WITHER)) {

                protect = true;
                break;
            }
        }

        if (!protect) {
            return;
        }

        for (LivingEntity entity : event.getAffectedEntities()) {
            if (entity instanceof Player defender) {
                coord.setFromLocation(entity.getLocation());
                TownChunk tc = CivGlobal.getTownChunk(coord);
                if (tc == null) {
                    continue;
                }

                switch (playersCanPVPHere(attacker, defender, tc)) {
                    case ALLOWED -> {
                    }
                    case NOT_AT_WAR -> {
                        CivMessage.send(attacker, ChatColor.RED + CivSettings.localize.localizedString("var_itemUse_potionError1", defender.getName()));
                        event.setCancelled(true);
                        return;
                    }
                    case NEUTRAL_IN_WARZONE -> {
                        CivMessage.send(attacker, ChatColor.RED + CivSettings.localize.localizedString("var_itemUse_potionError2", defender.getName()));
                        event.setCancelled(true);
                        return;
                    }
                    case NON_PVP_ZONE -> {
                        CivMessage.send(attacker, ChatColor.RED + CivSettings.localize.localizedString("var_itemUse_potionError3", defender.getName()));
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockRedstoneEvent(BlockRedstoneEvent event) {

        bcoord.setFromLocation(event.getBlock().getLocation());

        CampBlock cb = CivGlobal.getCampBlock(bcoord);
        if (cb != null) {
            if (event.getBlock().getType() == Material.WOOD_DOOR ||
                    event.getBlock().getType() == Material.IRON_DOOR ||
                    event.getBlock().getType() == Material.SPRUCE_DOOR ||
                    event.getBlock().getType() == Material.BIRCH_DOOR ||
                    event.getBlock().getType() == Material.JUNGLE_DOOR ||
                    event.getBlock().getType() == Material.ACACIA_DOOR ||
                    event.getBlock().getType() == Material.DARK_OAK_DOOR) {
                event.setNewCurrent(0);
                return;
            }
        }

        if (War.isWarTime()) {
            event.setNewCurrent(0);
        }

    }

    private enum PVPDenyReason {
        ALLOWED,
        NON_PVP_ZONE,
        NOT_AT_WAR,
        NEUTRAL_IN_WARZONE
    }

    private PVPDenyReason playersCanPVPHere(Player attacker, Player defender, TownChunk tc) {
        Resident defenderResident = CivGlobal.getResident(defender);
        Resident attackerResident = CivGlobal.getResident(attacker);
        PVPDenyReason reason = PVPDenyReason.NON_PVP_ZONE;

        /* Outlaws can only pvp each other if they are declared at this location. */
        if (CivGlobal.isOutlawHere(defenderResident, tc) ||
                CivGlobal.isOutlawHere(attackerResident, tc)) {
            return PVPDenyReason.ALLOWED;
        }

        /*
         * If it is WarTime and the town we're in is at war, allow neutral players to be
         * targeted by anybody.
         */
        if (War.isWarTime()) {
            if (tc.getTown().getCiv().getDiplomacyManager().isAtWar()) {
                /*
                 * The defender is neutral if he is not in a town/civ, or not in his own civ AND not 'at war'
                 * with the attacker.
                 */
                if (!defenderResident.hasTown() || (!defenderResident.getTown().getCiv().getDiplomacyManager().atWarWith(tc.getTown().getCiv()) &&
                        defenderResident.getTown().getCiv() != tc.getTown().getCiv())) {
                    /* Allow neutral players to be hurt, but not hurt them back. */
                    return PVPDenyReason.ALLOWED;
                } else if (!attackerResident.hasTown() || (!attackerResident.getTown().getCiv().getDiplomacyManager().atWarWith(tc.getTown().getCiv()) &&
                        attackerResident.getTown().getCiv() != tc.getTown().getCiv())) {
                    reason = PVPDenyReason.NEUTRAL_IN_WARZONE;
                }
            }
        }

        if (defenderResident != null && defenderResident.hasTown()) {
            /*
             * If defenders are at war with attackers allow PVP. Location doesnt matter. Allies should be able to help
             * defend each other regardless of where they are currently located.
             */
            if (defenderResident.getTown().getCiv().getDiplomacyManager().atWarWith(attacker)) {
                //if (defenderResident.getTown().getCiv() == tc.getTown().getCiv() ||
                //	attackerResident.getTown().getCiv() == tc.getTown().getCiv()) {
                return PVPDenyReason.ALLOWED;
                //}
            } else if (reason.equals(PVPDenyReason.NON_PVP_ZONE)) {
                reason = PVPDenyReason.NOT_AT_WAR;
            }
        }

        return reason;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityPortalCreate(EntityCreatePortalEvent event) {
        event.setCancelled(true);
    }

}
