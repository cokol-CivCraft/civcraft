package com.avrgaming.civcraft.war;

import com.avrgaming.civcraft.camp.CampBlock;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.StructureBlock;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.TownHall;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.FireWorkTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Random;

public class WarListener implements Listener {

    public static final String RESTORE_NAME = "special:TNT";
    ChunkCoord coord = new ChunkCoord();

    public static int yield;
    public static double playerDamage;
    public static int structureDamage;

    static {
        try {
            yield = CivSettings.getInteger(CivSettings.warConfig, "tnt.yield");
            playerDamage = CivSettings.getDouble(CivSettings.warConfig, "tnt.player_damage");
            structureDamage = CivSettings.getInteger(CivSettings.warConfig, "tnt.structure_damage");
        } catch (InvalidConfiguration e) {
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!War.isWarTime()) {
            return;
        }

        coord.setFromLocation(event.getBlock().getLocation());
        CultureChunk cc = CivGlobal.getCultureChunk(coord);

        if (cc == null) {
            return;
        }

        if (!cc.getCiv().getDiplomacyManager().isAtWar()) {
            return;
        }
        switch (event.getBlock().getType()) {
            case DIRT:
            case GRASS:
            case SAND:
            case GRAVEL:
            case TORCH:
            case REDSTONE_TORCH_OFF:
            case REDSTONE_TORCH_ON:
            case REDSTONE:
            case TNT:
            case LADDER:
            case VINE:
            case IRON_BLOCK:
            case GOLD_BLOCK:
            case DIAMOND_BLOCK:
            case EMERALD_BLOCK:
                return;
            default:
                break;
        }
        if (!event.getBlock().getType().isSolid()) {
            return;
        }

        CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("war_mustUseTNT"));
        event.setCancelled(true);
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!War.isWarTime()) {
            return;
        }

        coord.setFromLocation(event.getBlock().getLocation());
        CultureChunk cc = CivGlobal.getCultureChunk(coord);

        if (cc == null) {
            return;
        }

        if (!cc.getCiv().getDiplomacyManager().isAtWar()) {
            return;
        }
        switch (event.getBlock().getType()) {
            case DIRT:
            case GRASS:
            case SAND:
            case GRAVEL:
            case TORCH:
            case REDSTONE_TORCH_OFF:
            case REDSTONE_TORCH_ON:
            case REDSTONE:
            case TNT:
            case LADDER:
            case VINE:
                if (event.getBlock().getLocation().subtract(0, 1, 0).getBlock().getType() != Material.AIR) {
                    return;
                }

                event.getBlock().getWorld().spawnFallingBlock(event.getBlock().getLocation(), event.getBlock().getType(), event.getBlock().getData());
                event.getBlock().setType(Material.AIR);

                return;
        }

        if (event.getBlock().getType().equals(Material.IRON_BLOCK) ||
                event.getBlock().getType().equals(Material.GOLD_BLOCK) ||
                event.getBlock().getType().equals(Material.DIAMOND_BLOCK) ||
                event.getBlock().getType().equals(Material.EMERALD_BLOCK)) {

            if (event.getBlock().getLocation().subtract(0, 1, 0).getBlock().getType() != Material.AIR) {
                return;
            }

            return;
        }

        CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("war_onlyBuildCertainBlocks"));
        CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("war_canAlsoPlaceBridgeBlocks"));
        event.setCancelled(true);
    }

    private void explodeBlock(Block b) {
        WarRegen.explodeThisBlock(b, WarListener.RESTORE_NAME);
        launchExplodeFirework(b.getLocation());
    }

    private void launchExplodeFirework(Location loc) {
        Random rand = new Random();
        int rand1 = rand.nextInt(100);

        if (rand1 > 90) {
            FireworkEffect fe = FireworkEffect.builder().withColor(Color.ORANGE).withColor(Color.YELLOW).flicker(true).with(Type.BURST).build();
            TaskMaster.syncTask(new FireWorkTask(fe, loc.getWorld(), loc, 3), 0);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {

        if (War.isWarTime()) {
            event.setCancelled(false);
        } else {
            event.setCancelled(true);
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        if (event.getEntity() == null) {
            return;
        }

        if (event.getEntityType().equals(EntityType.UNKNOWN)) {
            return;
        }

        if (event.getEntityType().equals(EntityType.PRIMED_TNT) ||
                event.getEntityType().equals(EntityType.MINECART_TNT) || event.getEntityType().equals(EntityType.CREEPER)) {

            HashSet<Buildable> structuresHit = new HashSet<>();

            for (int y = -yield; y <= yield; y++) {
                for (int x = -yield; x <= yield; x++) {
                    for (int z = -yield; z <= yield; z++) {
                        Location loc = event.getLocation().clone().add(new Vector(x, y, z));
                        Block b = loc.getBlock();
                        if (loc.distance(event.getLocation()) < yield) {

                            BlockCoord bcoord = new BlockCoord();
                            bcoord.setFromLocation(loc);
//							StructureBlock sb = CivGlobal.getStructureBlock(bcoord);
//							if (sb == null) {
//							WarRegen.saveBlock(loc.getBlock(), Cannon.RESTORE_NAME, false);
//							}
//							if (sb.getTown() != null) {
//							WarRegen.destroyThisBlock(loc.getBlock(), sb.getTown());
//							} else {
//							ItemManager.setTypeIdAndData(loc.getBlock(), CivData.AIR, 0, false);
//							}

                            StructureBlock sb = CivGlobal.getStructureBlock(bcoord);
                            CampBlock cb = CivGlobal.getCampBlock(bcoord);

                            if (sb == null && cb == null) {
                                explodeBlock(b);
                                continue;
                            }

                            if (sb != null) {

                                if (!sb.isDamageable()) {
                                    continue;
                                }

                                if (sb.getOwner() instanceof TownHall) {
                                    TownHall th = (TownHall) sb.getOwner();
                                    if (th.getControlPoints().containsKey(bcoord)) {
                                        continue;
                                    }
                                }

                                if (!sb.getOwner().isDestroyed()) {
                                    if (!structuresHit.contains(sb.getOwner())) {

                                        structuresHit.add(sb.getOwner());

                                        if (sb.getOwner() instanceof TownHall) {
                                            TownHall th = (TownHall) sb.getOwner();

                                            if (th.getHitpoints() == 0) {
                                                explodeBlock(b);
                                            } else {
                                                th.onTNTDamage(structureDamage);
                                            }
                                        } else {
                                            sb.getOwner().onDamage(structureDamage, b.getWorld(), null, sb.getCoord(), sb);
                                            CivMessage.sendCiv(sb.getCiv(), CivColor.Yellow + CivSettings.localize.localizedString("var_war_tntMsg", sb.getOwner().getDisplayName(), (
                                                            sb.getOwner().getCenterLocation().getX() + "," +
                                                                    sb.getOwner().getCenterLocation().getY() + "," +
                                                                    sb.getOwner().getCenterLocation().getZ() + ")"),
                                                    (sb.getOwner().getHitpoints() + "/" + sb.getOwner().getMaxHitPoints())));
                                        }
                                    }
                                } else {
                                    explodeBlock(b);
                                }
                            }
                        }
                    }
                }
            }
            event.setCancelled(true);
        }

    }

}

