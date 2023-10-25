package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.UUID;

public class Lighthouse extends Structure {


    protected Lighthouse(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    public Lighthouse(int id, UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(id, uuid, nbt);
    }
}
