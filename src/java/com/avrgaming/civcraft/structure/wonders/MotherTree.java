package com.avrgaming.civcraft.structure.wonders;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.UUID;

public class MotherTree extends Wonder {

    public MotherTree(UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(uuid, nbt);
    }

    public MotherTree(Location center, String id, Town town)
            throws CivException {
        super(center, id, town);
    }

    @Override
    protected void addBuffs() {
        addBuffToCiv(this.getCiv(), "buff_mother_tree_growth");
        addBuffToCiv(this.getCiv(), "buff_mother_tree_tile_improvement_cost");
        addBuffToTown(this.getTown(), "buff_mother_tree_tile_improvement_bonus");
    }

    @Override
    protected void removeBuffs() {
        removeBuffFromCiv(this.getCiv(), "buff_mother_tree_growth");
        removeBuffFromCiv(this.getCiv(), "buff_mother_tree_tile_improvement_cost");
        removeBuffFromTown(this.getTown(), "buff_mother_tree_tile_improvement_bonus");
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
