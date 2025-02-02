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
import com.avrgaming.civcraft.object.ControlPoint;
import com.avrgaming.civcraft.object.Town;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.UUID;


public class ChichenItza extends Wonder {

    public ChichenItza(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    public ChichenItza(UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(uuid, nbt);
    }


    @Override
    protected void removeBuffs() {
        removeBuffFromCiv(this.getCiv(), "buff_chichen_itza_tower_hp");
        removeBuffFromCiv(this.getCiv(), "buff_chichen_itza_regen_rate");
        removeBuffFromTown(this.getTown(), "buff_chichen_itza_cp_bonus_hp");
        //This is where the Itza's buff to CP is removed
        for (ControlPoint controlPoint : this.getTown().getTownHall().getControlPoints().values()) {
            controlPoint.setMaxHitpoints((controlPoint.getMaxHitpoints() - (int) this.getTown().getBuffManager().getEffectiveDouble("buff_chichen_itza_cp_bonus_hp")));
        }
    }

    @Override
    protected void addBuffs() {
        addBuffToCiv(this.getCiv(), "buff_chichen_itza_tower_hp");
        addBuffToCiv(this.getCiv(), "buff_chichen_itza_regen_rate");
        addBuffToTown(this.getTown(), "buff_chichen_itza_cp_bonus_hp");
        //This is where the Itza's buff to CP applies
        for (ControlPoint controlPoint : this.getTown().getTownHall().getControlPoints().values()) {
            controlPoint.setMaxHitpoints((controlPoint.getMaxHitpoints() + (int) this.getTown().getBuffManager().getEffectiveDouble("buff_chichen_itza_cp_bonus_hp")));
        }
    }

    @Override
    public void onLoad() {
        if (this.isActive()) {
            addBuffs();
        }
    }

    @Override
    public void onComplete() {
        addBuffs();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeBuffs();
    }

}
