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
package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.UUID;

public class WaterStructure extends Structure {

    public static int WATER_LEVEL = 62;
    public static int TOLERANCE = 20;

    public WaterStructure(int id, UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(id, uuid, nbt);
    }

    protected WaterStructure(Location center, String id, Town town)
            throws CivException {
        super(center, id, town);
    }

    @Override
    protected Location repositionCenter(Location center, BlockFace dir, double x_size, double z_size) {
        Location loc = new Location(center.getWorld(),
                center.getX(), center.getY(), center.getZ(),
                center.getYaw(), center.getPitch());

        // Reposition tile improvements
        if (this.isTileImprovement()) {
            // just put the center at 0,0 of this chunk?
            loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
            //loc = center.getChunk().getBlock(arg0, arg1, arg2)
        } else {
            switch (dir) {
                case EAST -> {
                    loc.setZ(loc.getZ() - (z_size / 2));
                    loc.setX(loc.getX() + SHIFT_OUT);
                }
                case WEST -> {
                    loc.setZ(loc.getZ() - (z_size / 2));
                    loc.setX(loc.getX() - (SHIFT_OUT + x_size));
                }
                case NORTH -> {
                    loc.setX(loc.getX() - (x_size / 2));
                    loc.setZ(loc.getZ() - (SHIFT_OUT + z_size));
                }
                case SOUTH -> {
                    loc.setX(loc.getX() - (x_size / 2));
                    loc.setZ(loc.getZ() + SHIFT_OUT);
                }
            }
        }

        if (this.getTemplateYShift() != 0) {
            // Y-Shift based on the config, this allows templates to be built underground.
            loc.setY(WATER_LEVEL + this.getTemplateYShift());
        }

        return loc;
    }

    @Override
    protected void checkBlockPermissionsAndRestrictions(Player player, Block centerBlock, int regionX, int regionY, int regionZ, Location savedLocation) throws CivException {
        super.checkBlockPermissionsAndRestrictions(player, centerBlock, regionX, regionY, regionZ, savedLocation);

        if ((player.getLocation().getBlockY() - WATER_LEVEL) > TOLERANCE) {
            throw new CivException(CivSettings.localize.localizedString("buildable_Water_notValidWaterSpot"));
        }

    }

}
