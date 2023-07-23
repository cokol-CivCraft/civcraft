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
package com.avrgaming.civcraft.command.civ;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigGovernment;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import org.bukkit.ChatColor;

import java.util.ArrayList;

public class CivGovCommand extends CommandBase {

    @Override
    public void init() {
        command = "/civ gov";
        displayName = CivSettings.localize.localizedString("cmd_civ_gov_name");

        register_sub("info", this::info_cmd, CivSettings.localize.localizedString("cmd_civ_gov_infoDesc"));
        register_sub("change", this::change_cmd, CivSettings.localize.localizedString("cmd_civ_gov_changeDesc"));
        register_sub("list", this::list_cmd, CivSettings.localize.localizedString("cmd_civ_gov_listDesc"));
    }


    public void change_cmd() throws CivException {
        Civilization civ = getSenderCiv();

        if (args.length < 2) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_gov_changePrompt"));
        }

        ConfigGovernment gov = ConfigGovernment.getGovernmentFromName(args[1]);
        if (gov == null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_gov_changeInvalid") + " " + args[1]);
        }

        if (!gov.isAvailable(civ)) {
            throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_gov_changeNotHere", gov.displayName));
        }

        civ.changeGovernment(civ, gov, false);
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_civ_gov_changeSuccess"));
    }


    public void list_cmd() throws CivException {
        Civilization civ = getSenderCiv();

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_gov_listHeading"));
        ArrayList<ConfigGovernment> govs = ConfigGovernment.getAvailableGovernments(civ);

        for (ConfigGovernment gov : govs) {
            if (gov == civ.getGovernment()) {
                CivMessage.send(sender, ChatColor.GOLD + gov.displayName + " " + "(" + CivSettings.localize.localizedString("currentGovernment") + ")");
            } else {
                CivMessage.send(sender, ChatColor.DARK_GREEN + gov.displayName);
            }
        }

    }


    public void info_cmd() throws CivException {
        Civilization civ = getSenderCiv();

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_gov_infoHading") + " " + civ.getGovernment().displayName);
        CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("cmd_civ_gov_infoTrade") + " " + ChatColor.GREEN + civ.getGovernment().trade_rate +
                ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("cmd_civ_gov_infoCottage") + " " + ChatColor.GREEN + civ.getGovernment().cottage_rate);
        CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("cmd_civ_gov_infoUpkeep") + " " + ChatColor.GREEN + civ.getGovernment().upkeep_rate +
                ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("cmd_civ_gov_infoGrowth") + " " + ChatColor.GREEN + civ.getGovernment().growth_rate);
        CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("cmd_civ_gov_infoHammer") + " " + ChatColor.GREEN + civ.getGovernment().hammer_rate +
                ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("cmd_civ_gov_infoBeaker") + " " + ChatColor.GREEN + civ.getGovernment().beaker_rate);
        CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("cmd_civ_gov_infoCulture") + " " + ChatColor.GREEN + civ.getGovernment().culture_rate +
                ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("cmd_civ_gov_infoMaxTax") + " " + ChatColor.GREEN + civ.getGovernment().maximum_tax_rate);

    }

    @Override
    public void doDefaultAction() {
        showHelp();
    }

    @Override
    public void showHelp() {
        showBasicHelp();
    }

    @Override
    public void permissionCheck() throws CivException {
        validLeaderAdvisor();
    }

}
