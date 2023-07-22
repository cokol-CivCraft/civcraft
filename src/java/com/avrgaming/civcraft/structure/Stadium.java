package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Stadium extends Structure {

    protected Stadium(Location center, String id, Town town)
            throws CivException {
        super(center, id, town);
    }

    public Stadium(ResultSet rs) throws SQLException, CivException {
        super(rs);
    }

    @Override
    public String getMarkerIconName() {
        return "flower";
    }
}
