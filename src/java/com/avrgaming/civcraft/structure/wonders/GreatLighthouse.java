package com.avrgaming.civcraft.structure.wonders;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.UUID;

public class GreatLighthouse extends Wonder {

    public GreatLighthouse(UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(uuid, nbt);
    }

    public GreatLighthouse(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    @Override
    protected void addBuffs() {
        addBuffToTown(this.getTown(), "buff_great_lighthouse_tower_range");
        addBuffToCiv(this.getCiv(), "buff_great_lighthouse_trade_ship_income");
    }

    @Override
    protected void removeBuffs() {
        removeBuffFromTown(this.getTown(), "buff_great_lighthouse_tower_range");
        removeBuffFromCiv(this.getCiv(), "buff_great_lighthouse_trade_ship_income");
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

}
