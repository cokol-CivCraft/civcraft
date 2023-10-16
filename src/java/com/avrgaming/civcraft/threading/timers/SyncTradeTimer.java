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
package com.avrgaming.civcraft.threading.timers;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TradeGood;
import org.bukkit.ChatColor;

import java.text.DecimalFormat;

public class SyncTradeTimer implements Runnable {

    public SyncTradeTimer() {
    }

    public void processTownsTradePayments(Town town) {

        //goodies = town.getEffectiveBonusGoodies();

        //double payment = TradeGood.getTownTradePayment(town, goodies);
        double payment = TradeGood.getTownTradePayment(town);
        DecimalFormat df = new DecimalFormat();

        if (payment > 0.0) {

            double taxesPaid = payment * town.getDepositCiv().getIncomeTaxRate();
            if (taxesPaid > 0) {
                CivMessage.sendTown(town, ChatColor.GREEN + CivSettings.localize.localizedString("var_syncTrade_payout", (ChatColor.YELLOW + df.format(payment) + ChatColor.GREEN + " " + CivSettings.CURRENCY_NAME),
                        CivSettings.localize.localizedString("var_cottage_grew_taxes", (df.format(Math.floor(taxesPaid)) + " " + CivSettings.CURRENCY_NAME), town.getDepositCiv().getName())));
            } else {
                CivMessage.sendTown(town, ChatColor.GREEN + CivSettings.localize.localizedString("var_syncTrade_payout", (ChatColor.YELLOW + df.format(payment) + ChatColor.GREEN + " " + CivSettings.CURRENCY_NAME), ""));
            }

            town.getTreasury().deposit(payment - taxesPaid);
            town.getDepositCiv().taxPayment(town, taxesPaid);
        }
    }

    @Override
    public void run() {
        if (!CivGlobal.tradeEnabled) {
            return;
        }

        CivGlobal.checkForDuplicateGoodies();

        for (Town town : Town.getTowns()) {
            try {
                processTownsTradePayments(town);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
