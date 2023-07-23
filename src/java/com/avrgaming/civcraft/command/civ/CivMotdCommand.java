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
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import org.bukkit.ChatColor;

public class CivMotdCommand extends CommandBase {

    @Override
    public void init() {
        command = "/civ motd";
        displayName = CivSettings.localize.localizedString("cmd_civ_motd_name");

        register_sub("set", this::set_cmd, CivSettings.localize.localizedString("cmd_civ_motd_setDesc"));
        register_sub("remove", this::remove_cmd, CivSettings.localize.localizedString("cmd_civ_motd_removeDesc"));
    }

    public void set_cmd() throws CivException {
        Resident resident = getResident();
        Civilization civ = getSenderCiv();

        if (!civ.getLeaderGroup().hasMember(resident) && !civ.getAdviserGroup().hasMember(resident)) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_motd_notallowed"));
        }

        if (args.length < 2) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_motd_setPrompt"));
        }

        String motd = combineArgs(this.stripArgs(args, 1));
        civ.setMotd(motd);
        civ.save();

        CivMessage.sendCiv(civ, "MOTD:" + " " + motd);
    }

    public void remove_cmd() throws CivException {
        Resident resident = getResident();
        Civilization civ = getSenderCiv();

        if (!civ.getLeaderGroup().hasMember(resident) && !civ.getAdviserGroup().hasMember(resident)) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_motd_notallowed"));
        }
        civ.setMotd(null);
        civ.save();
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_civ_motd_removeSuccess"));
    }

    @Override
    public void doDefaultAction() throws CivException {
        showHelp();
        Resident resident = getResident();
        Civilization civ = getSenderCiv();
        if (civ.MOTD() != null) {
            CivMessage.send(resident, ChatColor.LIGHT_PURPLE + "[Civ MOTD] " + ChatColor.WHITE + resident.getCiv().MOTD());
        } else {
            CivMessage.send(resident, ChatColor.LIGHT_PURPLE + "[Civ MOTD] " + ChatColor.WHITE + CivSettings.localize.localizedString("cmd_civ_motd_noneSet"));
        }

    }

    @Override
    public void showHelp() {
        showBasicHelp();
    }

    @Override
    public void permissionCheck() {

    }

}
