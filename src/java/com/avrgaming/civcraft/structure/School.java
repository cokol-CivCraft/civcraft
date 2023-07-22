package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.sql.SQLException;

public class School extends Structure {


    protected School(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    public School(ResultSet rs) throws SQLException, CivException {
        super(rs);
    }

    @Override
    public void loadSettings() {
        super.loadSettings();

    }

    @Override
    public String getMarkerIconName() {
        return "walk";
    }

}
