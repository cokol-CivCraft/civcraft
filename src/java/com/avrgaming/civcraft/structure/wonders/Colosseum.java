package com.avrgaming.civcraft.structure.wonders;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.UUID;

public class Colosseum extends Wonder {

    public Colosseum(UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(uuid, nbt);
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
