package com.avrgaming.civcraft.structure.wonders;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.UUID;

public class Himeji extends Wonder {

    public Himeji(UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(uuid, nbt);
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