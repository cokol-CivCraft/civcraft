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
package com.avrgaming.civcraft.cache;

import com.avrgaming.civcraft.components.PlayerProximityComponent;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.FireWorkTask;
import com.avrgaming.civcraft.util.BlockCoord;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.LinkedList;
import java.util.Queue;

public class CannonExplosionProjectile {
    Location loc;
    Location target;
    int speed = 6;
    int damage = 40;
    int splash = 30;
    PlayerProximityComponent proximityComponent;

    public CannonExplosionProjectile(Buildable buildable, Location target) {
        proximityComponent = new PlayerProximityComponent();
        proximityComponent.createComponent(buildable);
        proximityComponent.setCenter(new BlockCoord(target));
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
        dir.multiply(speed);

        loc.add(dir);
        loc.getWorld().createExplosion(loc, 0.0f, false);
        double distance = loc.distanceSquared(target);
        BlockCoord center = proximityComponent.getCenter();
        center.setFromLocation(loc);

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
            //loc.getWorld().createExplosion(location, 1.0f, true);
            setFireAt(location, spread);
        }

        launchExplodeFirework(loc);
        //loc.getWorld().createExplosion(loc, 1.0f, true);
        damagePlayers(loc, splash);
        setFireAt(loc, spread);
    }

    private void damagePlayers(Location loc, int radius) {
        TaskMaster.asyncTask(() -> {
            Queue<Player> playerList = new LinkedList<>();
            Queue<Double> damageList = new LinkedList<>();

            //PlayerLocationCache.lock.lock();
            for (PlayerLocationCache pc : PlayerLocationCache.getCache()) {
                if (!(pc.getCoord().distanceSquared(new BlockCoord(target)) < radius)) {
                    continue;
                }
                try {
                    playerList.add(CivGlobal.getPlayer(pc.getName()));
                    damageList.add((double) damage);
                } catch (CivException e) {
                    //player offline
                }

            }

            TaskMaster.syncTask(() -> {
                Player player = playerList.poll();
                Double damage = damageList.poll();

                while (player != null && damage != null) {
                    player.damage(damage);

                    player = playerList.poll();
                    damage = damageList.poll();
                }
            });

        }, 0);
    }

    private void setFireAt(Location loc, int radius) {
        //Set the entire area on fire.
        for (int x = -radius; x < radius; x++) {
            for (int y = -3; y < 3; y++) {
                for (int z = -radius; z < radius; z++) {
                    Block block = loc.getWorld().getBlockAt(loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z);
                    if (block.getType() == Material.AIR) {
                        block.setType(Material.FIRE);
                    }
                }
            }
        }
    }

    private void launchExplodeFirework(Location loc) {
        FireworkEffect fe = FireworkEffect.builder().withColor(Color.ORANGE).withColor(Color.YELLOW).flicker(true).with(Type.BURST).build();
        TaskMaster.syncTask(new FireWorkTask(fe, loc.getWorld(), loc, 3), 0);
    }


    public void setLocation(Location turretLoc) {
        this.loc = turretLoc;
    }

    public void setTargetLocation(Location location) {
        this.target = location;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public void setSplash(int splash) {
        this.splash = splash;
    }

}
