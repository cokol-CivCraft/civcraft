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
package com.avrgaming.civcraft.structure.wonders;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.TreeMap;
import java.util.UUID;

public class TheGreatPyramid extends Wonder {
    public TheGreatPyramid(Location center, String id, Town town)throws CivException {
        super(center, id, town);
    }

    public TheGreatPyramid(UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(uuid, nbt);
    }

    private Civilization calculateNearestCivilization() {
        TreeMap<Double, Civilization> civMaps = CivGlobal.findNearestCivilizations(this.getTown());
        if (!civMaps.isEmpty()) {
            return civMaps.firstEntry().getValue();
        }
        return null;
    }

    @Override
    protected void addBuffs() {
        addBuffToTown(this.getTown(), "buff_pyramid_cottage_consume");
        addBuffToTown(this.getTown(), "buff_pyramid_cottage_bonus");
        addBuffToCiv(this.getCiv(), "buff_pyramid_culture");
        addBuffToTown(this.getTown(), "buff_pyramid_leech");
        Civilization nearest = calculateNearestCivilization();
        if (nearest != null) {
            addBuffToCiv(nearest, "debuff_pyramid_leech");
        }
    }

    @Override
    protected void removeBuffs() {
        removeBuffFromTown(this.getTown(), "buff_pyramid_cottage_consume");
        removeBuffFromTown(this.getTown(), "buff_pyramid_cottage_bonus");
        removeBuffFromCiv(this.getCiv(), "buff_pyramid_culture");
        removeBuffFromTown(this.getTown(), "buff_pyramid_leech");
        Civilization nearest = calculateNearestCivilization();
        if (nearest != null) {
            removeBuffFromCiv(nearest, "debuff_pyramid_leech");
        }
    }

    @Override
    public void onLoad() {
        if (this.isActive()) {
            addBuffs();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeBuffs();
    }

    @Override
    public void onComplete() {
        addBuffs();
    }

}
