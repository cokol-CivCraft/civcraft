package com.avrgaming.civcraft.util;

import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.UUID;

public class EntityUtil {

    public static Entity getEntity(World world, UUID uuid) {

        for (Entity ent : world.getEntities()) {
            if (ent.getUniqueId().equals(uuid)) {
                return ent;
            }
        }

        return null;
    }

}
