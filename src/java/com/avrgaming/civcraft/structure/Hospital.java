package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.UUID;

public class Hospital extends Structure {

    protected Hospital(Location center, String id, Town town)
            throws CivException {
        super(center, id, town);
    }

    public Hospital(int id, UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(id, uuid, nbt);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        for (Town town : this.getTown().getCiv().getTowns()) {
            for (Resident resident : town.getResidents()) {
                Player player;
                try {
                    player = CivGlobal.getPlayer(resident);
                } catch (CivException e) {
                    //Player not online;
                    continue;
                }

                if (player.isDead() || !player.isValid() || !player.isOnline()) {
                    continue;
                }
                if (player.getFoodLevel() >= 20) {
                    continue;
                }

                TownChunk tc = CivGlobal.getTownChunk(player.getLocation());
                if (tc == null || tc.getTown() != this.getTown()) {
                    continue;
                }

                if (player.getFoodLevel() < 19.0) {
                    player.setFoodLevel(player.getFoodLevel() + 1);
                }

            }
        }
    }
}
