package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Location;

import com.avrgaming.civcraft.components.ProjectileCannonComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;

public class CannonShip extends WaterStructure {

	ProjectileCannonComponent cannonComponent;
	
	protected CannonShip(Location center, String id, Town town)
			throws CivException {
		super(center, id, town);
	}
	
	protected CannonShip(ResultSet rs) throws SQLException, CivException {
		super(rs);
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
		return (int)(cannonComponent.getDamage()*rate);
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
		try {
			double build_distance = CivSettings.getDouble(CivSettings.warConfig, "cannon_tower.build_distance");
			
			for (Town town : this.getTown().getCiv().getTowns()) {
				for (Structure struct : town.getStructures()) {
					if (struct instanceof CannonTower) {
						BlockCoord center = struct.getCenterLocation();
						double distance = center.distance(this.getCenterLocation());
						if (distance <= build_distance) {
							throw new CivException(CivSettings.localize.localizedString("var_buildable_tooCloseToCannonTower",(center.getX()+","+center.getY()+","+center.getZ())));
						}
					}
					if (struct instanceof CannonShip) {
						BlockCoord center = struct.getCenterLocation();
						double distance = center.distance(this.getCenterLocation());
						if (distance <= build_distance) {
							throw new CivException(CivSettings.localize.localizedString("var_buildable_tooCloseToCannonShip",(center.getX()+","+center.getY()+","+center.getZ())));
						}
					}
				}
			}
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			throw new CivException(e.getMessage());
		}
		
	}
	
}
