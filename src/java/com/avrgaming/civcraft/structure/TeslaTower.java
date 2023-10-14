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

import com.avrgaming.civcraft.components.ProjectileLightningComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TeslaTower extends Structure {

    ProjectileLightningComponent teslaComponent;

    protected TeslaTower(Location center, String id, Town town)
            throws CivException {
        super(center, id, town);
        this.hitpoints = this.getMaxHitPoints();
    }

    protected TeslaTower(ResultSet rs) throws SQLException, CivException {
        super(rs);
    }

    @Override
    public void loadSettings() {
        super.loadSettings();
        teslaComponent = new ProjectileLightningComponent(this, this.getCenterLocation().getLocation());
        teslaComponent.createComponent(this);
    }

    public int getDamage() {
        double rate = 1;
//		rate += this.getTown().getBuffManager().getEffectiveDouble(Buff.FIRE_BOMB);
        return (int) (teslaComponent.getDamage() * rate);
    }

    @Override
    public int getMaxHitPoints() {
        double rate = 1;
            rate += this.getTown().getBuffManager().getEffectiveDouble("buff_chichen_itza_tower_hp");
            rate += this.getTown().getBuffManager().getEffectiveDouble(Buff.BARRICADE);
        return (int) (info.max_hp * rate);
    }


    public void setTurretLocation(BlockCoord absCoord) {
        teslaComponent.setTurretLocation(absCoord);
    }


    @Override
    public void onCheck() throws CivException {
        double build_distance = CivSettings.warConfig.getDouble("tesla_tower.build_distance", 120.0);

        for (Town town : this.getTown().getCiv().getTowns()) {
            for (Structure struct : town.getStructures()) {
                if (struct instanceof TeslaTower) {
                    BlockCoord center = struct.getCenterLocation();
                    double distance = center.distance(this.getCenterLocation());
                    if (distance <= build_distance) {
                        throw new CivException(CivSettings.localize.localizedString("var_buildable_tooCloseToTeslaTower", (center.getX() + "," + center.getY() + "," + center.getZ())));
                    }
                }
//					if (struct instanceof CannonTower) {
//						BlockCoord center = struct.getCenterLocation();
//						double distance = center.distance(this.getCenterLocation());
//						if (distance <= build_distance) {
//							throw new CivException(CivSettings.localize.localizedString("var_buildable_tooCloseToCannonShip",(center.getX()+","+center.getY()+","+center.getZ())));
//						}
//					}
                for (Town town : this.getTown().getCiv().getTowns()) {
                    for (Structure struct : town.getStructures()) {
                        if (struct instanceof TeslaTower) {
                            BlockCoord center = struct.getCenterLocation();
                            double distance = center.distance(this.getCenterLocation());
                            if (distance < build_distance) {
                                throw new CivException(CivSettings.localize.localizedString("var_buildable_tooCloseToTeslaTower", (center.getX() + "," + center.getY() + "," + center.getZ())));
                            }
                        }
                    }
                }
            }

    }

}
