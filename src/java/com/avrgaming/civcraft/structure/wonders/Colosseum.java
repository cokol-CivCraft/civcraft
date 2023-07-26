package com.avrgaming.civcraft.structure.wonders;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Location;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;

public class Colosseum extends Wonder {

	public Colosseum(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	public Colosseum(Location center, String id, Town town) throws CivException {
		super(center, id, town);
	}

	@Override
	protected void removeBuffs() {
	}

	@Override
	protected void addBuffs() {		
	}

}
