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

import com.avrgaming.civcraft.components.ProjectileCannonComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.UUID;

public class CannonTower extends Structure {

    ProjectileCannonComponent cannonComponent;

    protected CannonTower(Location center, String id, Town town) throws CivException {
        super(center, id, town);
        this.hitpoints = this.getMaxHitPoints();
    }

    public CannonTower(int id, UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(id, uuid, nbt);
    }

    @Override
    public void loadSettings() {
        super.loadSettings();
        cannonComponent = new ProjectileCannonComponent(this, this.getCenterLocation().getLocation());
        cannonComponent.createComponent(this);
    }

    public int getDamage() {
        double rate = 1;
        rate += this.getTown().getBuffManager().getEffectiveDouble(Buff.FIRE_BOMB);
        return (int) (cannonComponent.getDamage() * rate);
    }

    @Override
    public int getMaxHitPoints() {
        double rate = 1;
            rate += this.getTown().getBuffManager().getEffectiveDouble("buff_chichen_itza_tower_hp");
            rate += this.getTown().getBuffManager().getEffectiveDouble(Buff.BARRICADE);
        return (int) (info.max_hp * rate);
    }

//	public void setDamage(int damage) {
//		cannonComponent.setDamage(damage);
//	}


    public void setTurretLocation(BlockCoord absCoord) {
        cannonComponent.setTurretLocation(absCoord);
    }


    @Override
    public void onCheck() throws CivException {
        double build_distance = CivSettings.warConfig.getDouble("cannon_tower.build_distance", 115.0);

        for (Town town : this.getTown().getCiv().getTowns()) {
            for (Structure struct : town.getStructures()) {
                if (struct instanceof CannonTower) {
                    BlockCoord center = struct.getCenterLocation();
                    double distance = center.distance(this.getCenterLocation());
                    if (distance <= build_distance) {
                        throw new CivException(CivSettings.localize.localizedString("var_buildable_tooCloseToCannonTower", (center.getX() + "," + center.getY() + "," + center.getZ())));
                    }
                }
                if (struct instanceof CannonShip) {
                    BlockCoord center = struct.getCenterLocation();
                    double distance = center.distance(this.getCenterLocation());
                    if (distance <= build_distance) {
                        throw new CivException(CivSettings.localize.localizedString("var_buildable_tooCloseToCannonShip", (center.getX() + "," + center.getY() + "," + center.getZ())));
                    }
                }
            }
        }

    }

}
