package com.avrgaming.civcraft.siege;

import com.avrgaming.civcraft.camp.CampBlock;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.StructureBlock;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.TownHall;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.FireWorkTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.EntityProximity;
import com.avrgaming.civcraft.war.WarRegen;
import net.minecraft.server.v1_12_R1.EntityPlayer;
import org.bukkit.*;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

public class CannonProjectile {
    public Cannon cannon;
    public Location loc;
    private final Location startLoc;
    public Resident whoFired;
    public double speed = 1.0f;

    public static double yield;
    public static double playerDamage;
    public static double maxRange;
    public static int controlBlockHP;

    static {
        yield = CivSettings.warConfig.getDouble("cannon.yield", 7.0);
        playerDamage = CivSettings.warConfig.getDouble("cannon.player_damage", 50);
        maxRange = CivSettings.warConfig.getDouble("cannon.max_range", 256.0);
        controlBlockHP = CivSettings.warConfig.getInt("cannon.control_block_hp", 1);
    }

    public CannonProjectile(Cannon cannon, Location loc, Resident whoFired) {
        this.cannon = cannon;
        this.loc = loc;
        this.startLoc = loc.clone();
        this.whoFired = whoFired;
    }

    private void explodeBlock(Block b) {
        WarRegen.explodeThisBlock(b, Cannon.RESTORE_NAME);
        launchExplodeFirework(b.getLocation());
    }

    public static BlockCoord bcoord = new BlockCoord();

    public void onHit() {
        //launchExplodeFirework(loc);

        int radius = (int) yield;
        HashSet<Buildable> structuresHit = new HashSet<>();

        for (int x = -radius; x < radius; x++) {
            for (int z = -radius; z < radius; z++) {
                for (int y = -radius; y < radius; y++) {

                    Block b = loc.getBlock().getRelative(x, y, z);
                    if (b.getType() == Material.BEDROCK) {
                        continue;
                    }

                    if (loc.distance(b.getLocation()) <= yield) {
                        bcoord.setFromLocation(b.getLocation());
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

                            if (sb.getOwner() instanceof TownHall th) {
                                if (th.getControlPoints().containsKey(bcoord)) {
                                    continue;
                                }
                            }

                            if (!sb.getOwner().isDestroyed()) {
                                if (!structuresHit.contains(sb.getOwner())) {

                                    structuresHit.add(sb.getOwner());

                                    if (sb.getOwner() instanceof TownHall th) {

                                        if (th.getHitpoints() == 0) {
                                            explodeBlock(b);
                                        } else {
                                            th.onCannonDamage(cannon.getDamage(), this);

                                        }
                                    } else {
                                        Player player = null;
                                        try {
                                            player = CivGlobal.getPlayer(whoFired);
                                        } catch (CivException ignored) {
                                        }

                                        if (!sb.getCiv().getDiplomacyManager().atWarWith(whoFired.getCiv())) {
                                            if (player != null) {
                                                CivMessage.sendError(player, CivSettings.localize.localizedString("cannonProjectile_ErrorNotAtWar"));
                                                return;
                                            }
                                        }

                                        sb.getOwner().onDamage(cannon.getDamage(), b.getWorld(), player, sb.getCoord(), sb);
                                        CivMessage.sendCiv(sb.getCiv(), ChatColor.YELLOW + CivSettings.localize.localizedString("cannonProjectile_hitAnnounce", sb.getOwner().getDisplayName(),
                                                sb.getOwner().getCenterLocation().getX() + "," +
                                                        sb.getOwner().getCenterLocation().getY() + "," +
                                                        sb.getOwner().getCenterLocation().getZ())
                                                + " (" + sb.getOwner().getHitpoints() + "/" + sb.getOwner().getMaxHitPoints() + ")");
                                    }

                                    CivMessage.sendCiv(whoFired.getCiv(), ChatColor.GREEN + CivSettings.localize.localizedString("var_cannonProjectile_hitSuccess", sb.getOwner().getTown().getName(), sb.getOwner().getDisplayName()) +
                                            " (" + sb.getOwner().getHitpoints() + "/" + sb.getOwner().getMaxHitPoints() + ")");
                                }
                            } else {

                                if (!Cannon.cannonBlocks.containsKey(bcoord)) {
                                    explodeBlock(b);
                                }
                            }
                        }
                    }
                }
            }
        }

        /* Instantly kill any players caught in the blast. */
        LinkedList<Entity> players = EntityProximity.getNearbyEntities(null, loc, yield, EntityPlayer.class);
        for (Entity e : players) {
            Player player = (Player) e;
            player.damage(playerDamage);
            if (player.isDead()) {
                CivMessage.global(ChatColor.GRAY + CivSettings.localize.localizedString("var_cannonProjectile_userKilled", player.getName(), whoFired.getName()));
            }
        }
    }

    private void launchExplodeFirework(Location loc) {
        Random rand = new Random();
        int rand1 = rand.nextInt(100);

        if (rand1 > 90) {
            FireworkEffect fe = FireworkEffect.builder().withColor(Color.ORANGE).withColor(Color.YELLOW).flicker(true).with(Type.BURST).build();
            TaskMaster.syncTask(new FireWorkTask(fe, loc.getWorld(), loc, 3), 0);
        }
    }

    public boolean advance() {
        Vector dir = loc.getDirection();
        dir.add(new Vector(0.0f, -0.008, 0.0f)); //Apply 'gravity'
        loc.setDirection(dir);

        loc.add(dir.multiply(speed));
        loc.getWorld().createExplosion(loc, 0.0f, false);

        Block block = loc.getBlock();
        if (block.getType() != Material.AIR) {
            return true;
        }

        return loc.distance(startLoc) > maxRange;
    }

    public void fire() {

        TaskMaster.syncTask(new Runnable() {
            @Override
            public void run() {
                if (CannonProjectile.this.advance()) {
                    onHit();
                    return;
                }
                TaskMaster.syncTask(this, 1);
            }
        });
    }

}
