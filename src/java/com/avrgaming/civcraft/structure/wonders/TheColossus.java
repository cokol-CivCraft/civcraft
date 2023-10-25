/*
 * AVRGAMING LLC
 * __________________
 *
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.structure.wonders;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.war.War;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

public class TheColossus extends Wonder {

    public TheColossus(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    public TheColossus(int id, UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(id, uuid, nbt);
    }

    private TreeMap<Double, Civilization> nearestCivs;

    private void calculateNearestCivilizations() {
        nearestCivs = CivGlobal.findNearestCivilizations(this.getTown());
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

    @Override
    protected void removeBuffs() {
        int i = 0;
        for (Entry<Double, Civilization> entry : nearestCivs.entrySet()) {
            this.removeBuffFromCiv(entry.getValue(), "debuff_colossus_leech_upkeep");
            i++;

            if (i > 3) {
                break;
            }
        }

        this.removeBuffFromTown(this.getTown(), "buff_colossus_reduce_upkeep");
        this.removeBuffFromTown(this.getTown(), "buff_colossus_coins_from_culture");
    }

    @Override
    protected void addBuffs() {
        calculateNearestCivilizations();

        int i = 0;
        for (Entry<Double, Civilization> entry : nearestCivs.entrySet()) {
            this.addBuffToCiv(entry.getValue(), "debuff_colossus_leech_upkeep");
            i++;

            if (i > 3) {
                break;
            }
        }

        this.addBuffToTown(this.getTown(), "buff_colossus_reduce_upkeep");
        this.addBuffToTown(this.getTown(), "buff_colossus_coins_from_culture");

    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!War.isWarTime()) {
            return;
        }
        for (Resident resident : this.getCiv().getOnlineResidents()) {
            Player player = resident.getPlayer();
            if (CivGlobal.getCultureChunk(player.getLocation()) != null && CivGlobal.getCultureChunk(player.getLocation()).getCiv().getDiplomacyManager().isHostileWith(resident)) {
                player.setHealth(player.getHealth() + 0.33);
            }
        }
    }

}
