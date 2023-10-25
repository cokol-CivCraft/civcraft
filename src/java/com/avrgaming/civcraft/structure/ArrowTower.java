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

import com.avrgaming.civcraft.components.ProjectileArrowComponent;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.UUID;

public class ArrowTower extends Structure {

    ProjectileArrowComponent arrowComponent;

    protected ArrowTower(Location center, String id, Town town)
            throws CivException {
        super(center, id, town);
        this.hitpoints = this.getMaxHitPoints();
    }

    public ArrowTower(int id, UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(id, uuid, nbt);
    }

    @Override
    public void loadSettings() {
        super.loadSettings();
        arrowComponent = new ProjectileArrowComponent(this, this.getCenterLocation().getLocation());
        arrowComponent.createComponent(this);
    }

    /**
     * @return the damage
     */
    public int getDamage() {
        double rate = 1;
        rate += this.getTown().getBuffManager().getEffectiveDouble(Buff.FIRE_BOMB);
        return (int) (arrowComponent.getDamage() * rate);
    }

    @Override
    public int getMaxHitPoints() {
        double rate = 1;
            rate += this.getTown().getBuffManager().getEffectiveDouble("buff_chichen_itza_tower_hp");
            rate += this.getTown().getBuffManager().getEffectiveDouble(Buff.BARRICADE);
        return (int) (info.max_hp * rate);
    }

    /**
     * @param power the power to set
     */
    public void setPower(double power) {
        arrowComponent.setPower(power);
    }

    public void setTurretLocation(BlockCoord absCoord) {
        arrowComponent.setTurretLocation(absCoord);
    }

}
