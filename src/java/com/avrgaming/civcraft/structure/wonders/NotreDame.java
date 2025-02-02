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

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.UUID;

public class NotreDame extends Wonder {

    public NotreDame(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    public NotreDame(UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(uuid, nbt);
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
        this.removeBuffFromCiv(this.getCiv(), "buff_notre_dame_no_anarchy");
        this.removeBuffFromTown(this.getTown(), "buff_notre_dame_coins_from_peace");
        this.removeBuffFromTown(this.getTown(), "buff_notre_dame_extra_war_penalty");
    }

    @Override
    protected void addBuffs() {
        this.addBuffToCiv(this.getCiv(), "buff_notre_dame_no_anarchy");
        this.addBuffToTown(this.getTown(), "buff_notre_dame_coins_from_peace");
        this.addBuffToTown(this.getTown(), "buff_notre_dame_extra_war_penalty");

    }

    public void processPeaceTownCoins() {
        double totalCoins = 0;
        int peacefulTowns = 0;
        double coinsPerTown = this.getTown().getBuffManager().getEffectiveInt("buff_notre_dame_coins_from_peace");

        for (Civilization civ : Civilization.getCivs()) {
            if (civ.isAdminCiv()) {
                continue;
            }

            if (civ.getDiplomacyManager().isAtWar()) {
                continue;
            }
            peacefulTowns++;
            totalCoins += (coinsPerTown * civ.getTowns().size());
        }

        this.getTown().depositTaxed(totalCoins);
        CivMessage.sendTown(this.getTown(), CivSettings.localize.localizedString("var_NotreDame_generatedCoins", totalCoins, CivSettings.CURRENCY_NAME, peacefulTowns));

    }

}
