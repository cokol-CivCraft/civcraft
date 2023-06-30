package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Location;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;

public class MobGrinder extends Structure {
	private static final double T1_CHANCE = CivSettings.getDoubleStructure("mobGrinder.t1_chance"); //1%
	private static final double T2_CHANCE = CivSettings.getDoubleStructure("mobGrinder.t2_chance"); //2%
	private static final double T3_CHANCE = CivSettings.getDoubleStructure("mobGrinder.t3_chance"); //1%
	private static final double T4_CHANCE = CivSettings.getDoubleStructure("mobGrinder.t4_chance"); //0.25%
	private static final double PACK_CHANCE = CivSettings.getDoubleStructure("mobGrinder.pack_chance"); //0.10%
	private static final double BIGPACK_CHANCE = CivSettings.getDoubleStructure("mobGrinder.bigpack_chance");
	private static final double HUGEPACK_CHANCE = CivSettings.getDoubleStructure("mobGrinder.hugepack_chance");
	
	public int skippedCounter = 0;
	public ReentrantLock lock = new ReentrantLock();
	
	public enum Crystal {
		T1,
		T2,
		T3,
		T4,
		PACK,
		BIGPACK,
		HUGEPACK
	}
	
	protected MobGrinder(Location center, String id, Town town) throws CivException {
		super(center, id, town);	
	}
	
	public MobGrinder(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public String getDynmapDescription() {
		return null;
	}
	
	@Override
	public String getMarkerIconName() {
		return "minecart";
	}
	
	public double getMineralChance(Crystal crystal) {
		double chance = 0;
		switch (crystal) {
		case T1:
			chance = T1_CHANCE;
			break;
		case T2:
			chance = T2_CHANCE;
			break;
		case T3:
			chance = T3_CHANCE;
			break;
		case T4:
			chance = T4_CHANCE;
			break;
		case PACK:
			chance = PACK_CHANCE;
			break;
		case BIGPACK:
			chance = BIGPACK_CHANCE;
			break;
		case HUGEPACK:
			chance = HUGEPACK_CHANCE;
		}
		
		double increase = chance*this.getTown().getBuffManager().getEffectiveDouble(Buff.EXTRACTION);
		chance += increase;
		
		try {
			if (this.getTown().getGovernment().id.equals("gov_tribalism")) {
				chance *= CivSettings.getDouble(CivSettings.structureConfig, "mobGrinder.tribalism_rate");
			} else {
				chance *= CivSettings.getDouble(CivSettings.structureConfig, "mobGrinder.penalty_rate");
			}
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
		
		return chance;
	}

}
