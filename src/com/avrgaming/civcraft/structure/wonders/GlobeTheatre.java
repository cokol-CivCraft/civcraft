package com.avrgaming.civcraft.structure.wonders;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Location;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;

public class GlobeTheatre extends Wonder {

	public GlobeTheatre(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	public GlobeTheatre(Location center, String id, Town town) throws CivException {
		super(center, id, town);
	}

	@Override
	protected void removeBuffs() {
		removeBuffFromTown(this.getTown(), "buff_globe_theatre_culture_from_towns");
	}

	@Override
	protected void addBuffs() {		
		addBuffToTown(this.getTown(), "buff_globe_theatre_culture_from_towns");
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
