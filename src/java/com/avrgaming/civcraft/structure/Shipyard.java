package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Shipyard extends WaterStructure {

    protected Shipyard(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    public Shipyard(ResultSet rs) throws SQLException, CivException {
        super(rs);
    }
}
