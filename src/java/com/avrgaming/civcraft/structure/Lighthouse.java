package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Lighthouse extends Structure {


    protected Lighthouse(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    public Lighthouse(ResultSet rs) throws SQLException, CivException {
        super(rs);
    }
}
