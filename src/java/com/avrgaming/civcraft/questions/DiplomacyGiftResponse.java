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
package com.avrgaming.civcraft.questions;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import org.bukkit.ChatColor;

public class DiplomacyGiftResponse implements QuestionResponseInterface {

    public Object giftedObject;
    public Civilization fromCiv;
    public Civilization toCiv;

    @Override
    public void processResponse(String param) {
        if (!param.equalsIgnoreCase("accept")) {
            CivMessage.sendCiv(fromCiv, ChatColor.GRAY + CivSettings.localize.localizedString("var_RequestDecline", toCiv.getName()));
            return;
        }

        if (giftedObject instanceof Town town) {

            if (!toCiv.getTreasury().hasEnough(town.getGiftCost())) {
                CivMessage.sendCiv(toCiv, ChatColor.RED + CivSettings.localize.localizedString("var_diplomacy_gift_ErrorTooPoor", town.getName(), town.getGiftCost(), CivSettings.CURRENCY_NAME));
                CivMessage.sendCiv(fromCiv, ChatColor.RED + CivSettings.localize.localizedString("var_diplomacy_gift_ErrorTooPoor2", toCiv.getName(), town.getName(), town.getGiftCost(), CivSettings.CURRENCY_NAME));
                return;
            }

            toCiv.getTreasury().withdraw(town.getGiftCost());
            town.changeCiv(toCiv);
            CivMessage.sendCiv(fromCiv, ChatColor.GRAY + CivSettings.localize.localizedString("var_diplomacy_gift_accept", toCiv.getName(), town.getName()));
        } else if (giftedObject instanceof Civilization) {
            int coins = fromCiv.getMergeCost();

            if (!toCiv.getTreasury().hasEnough(coins)) {
                CivMessage.sendCiv(toCiv, ChatColor.RED + CivSettings.localize.localizedString("var_diplomacy_merge_ErrorTooPoor", fromCiv.getName(), coins, CivSettings.CURRENCY_NAME));
                CivMessage.sendCiv(fromCiv, ChatColor.RED + CivSettings.localize.localizedString("var_diplomacy_merge_ErrorTooPoor2", toCiv.getName(), fromCiv.getName(), coins, CivSettings.CURRENCY_NAME));
                return;
            }

            toCiv.getTreasury().withdraw(coins);
            CivMessage.sendCiv(fromCiv, ChatColor.YELLOW + CivSettings.localize.localizedString("var_diplomacy_merge_offerAccepted", toCiv.getName()));
            toCiv.mergeInCiv(fromCiv);
            CivMessage.global(CivSettings.localize.localizedString("var_diplomacy_merge_SuccessAlert1", fromCiv.getName(), toCiv.getName()));
        } else {
            CivLog.error(CivSettings.localize.localizedString("diplomacy_merge_UnexpectedError") + " " + giftedObject);
        }
    }

    @Override
    public void processResponse(String response, Resident responder) {
        processResponse(response);
    }
}
