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

public class CannonShip extends WaterStructure {

    ProjectileCannonComponent cannonComponent;

    protected CannonShip(Location center, String id, Town town)
            throws CivException {
        super(center, id, town);
    }

    public CannonShip(UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(uuid, nbt);
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

//	public void setDamage(int damage) {
//		cannonComponent.setDamage(damage);
//	}


    public void setTurretLocation(BlockCoord absCoord) {
        cannonComponent.setTurretLocation(absCoord);
    }


//	@Override
//	public void fire(Location turretLoc, Location playerLoc) {
//		turretLoc = adjustTurretLocation(turretLoc, playerLoc);
//		Vector dir = getVectorBetween(playerLoc, turretLoc);
//		
//		Fireball fb = turretLoc.getWorld().spawn(turretLoc, Fireball.class);
//		fb.setDirection(dir);
//		// NOTE cannon does not like it when the dir is normalized or when velocity is set.
//		fb.setYield((float)yield);
//		CivCache.cannonBallsFired.put(fb.getUniqueId(), new CannonFiredCache(this, playerLoc, fb));
//	}

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
