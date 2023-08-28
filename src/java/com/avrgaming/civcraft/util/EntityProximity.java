package com.avrgaming.civcraft.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;

import java.util.LinkedList;

public class EntityProximity {


    /**
     * Use a NMS method to grab an axis aligned bounding box around an area to
     * determine which entities are within this radius.
     * Optionally provide an entity that is exempt from these checks.
     * Also optionally provide a filter so we can only capture specific types of entities.
     */
    public static LinkedList<Entity> getNearbyEntities(Entity exempt, Location loc, double radius, Class<?> filter) {
        LinkedList<Entity> entities = new LinkedList<>();

        double x = loc.getX() + 0.5;
        double y = loc.getY() + 0.5;
        double z = loc.getZ() + 0.5;

        BoundingBox bb = new BoundingBox(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);

        for (Entity e : loc.getWorld().getNearbyEntities(bb).stream().filter(entity -> entity == exempt).toList()) {
            if (filter == null || (filter.isInstance(e))) {
                entities.add(e);
            }
        }

        return entities;
    }

}
