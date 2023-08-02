package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class MetaStructure extends Buildable {
    public static MetaStructure newStructOrWonder(ResultSet rs) throws CivException, SQLException {
        ConfigBuildableInfo info = CivSettings.wonders.get(rs.getString("type_id"));
        if (info == null) {
            info = CivSettings.structures.get(rs.getString("type_id"));
        }
        if (info.isWonder) {
            return Wonder.newWonder(rs);
        } else {
            return Structure.newStructure(rs);
        }
    }

    public static MetaStructure newStructOrWonder(Location center, ConfigBuildableInfo info, Town town) throws CivException {
        if (info.isWonder) {
            return Wonder.newWonder(center, info.id, town);
        } else {
            return Structure.newStructure(center, info.id, town);
        }
    }
}
