package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.UUID;

public class Museum extends Structure {

    protected Museum(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    public Museum(UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(uuid, nbt);
    }

    @Override
    public void loadSettings() {
        super.loadSettings();
    }

    @Override
    public void onLoad() {
        if (this.isActive()) {
            addBuffs();
        }
    }

    @Override
    public void onComplete() {
        addBuffs();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeBuffs();
    }

    protected void removeBuffs() {
        this.removeBuffFromTown(this.getTown(), "buff_art_appreciation");
    }

    protected void addBuffs() {
        this.addBuffToTown(this.getTown(), "buff_art_appreciation");

    }

    protected void addBuffToTown(Town town, String id) {
        try {
            town.getBuffManager().addBuff(id, id, this.getDisplayName() + " in " + this.getTown().getName());
        } catch (CivException e) {
            e.printStackTrace();
        }
    }

    protected void removeBuffFromTown(Town town, String id) {
        town.getBuffManager().removeBuff(id);
    }
}
