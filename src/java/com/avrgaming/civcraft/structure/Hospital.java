package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Hospital extends Structure {

    protected Hospital(Location center, String id, Town town)
            throws CivException {
        super(center, id, town);
    }

    public Hospital(ResultSet rs) throws SQLException, CivException {
        super(rs);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        for (Town t : this.getTown().getCiv().getTowns()) {
            for (Resident res : t.getResidents()) {
                try {
                    Player player = CivGlobal.getPlayer(res);

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

                    if (player.getFoodLevel() < 10.0) {
                        player.setFoodLevel(player.getFoodLevel() + 1);
                    }
                } catch (CivException e) {
                    //Player not online;
                }

            }
        }
    }
}
