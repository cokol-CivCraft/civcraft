package com.avrgaming.civcraft.structure.wonders;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Himeji extends Wonder {

    public Himeji(ResultSet rs) throws SQLException, CivException {
        super(rs);
    }

    public Himeji(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    @Override
    protected void removeBuffs() {
        this.removeBuffFromCiv(this.getCiv(), Buff.MOBILIZATION);
    }

    @Override
    protected void addBuffs() {
        this.addBuffToCiv(this.getCiv(), Buff.MOBILIZATION);
    }

}