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
package com.avrgaming.civcraft.command.admin;


import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.PlayerKickBan;
import com.avrgaming.civcraft.war.War;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class AdminWarCommand extends CommandBase {

    @Override
    public void init() {
        command = "/ad war";
        displayName = CivSettings.localize.localizedString("adcmd_war_name");

        register_sub("start", this::start_cmd, CivSettings.localize.localizedString("adcmd_war_startDesc"));
        register_sub("stop", this::stop_cmd, CivSettings.localize.localizedString("adcmd_war_stopDesc"));
        register_sub("resetstart", null, CivSettings.localize.localizedString("adcmd_war_resetstartDesc"));
        //commands.put("setlastwar", "takes a date of the form: DAY:MONTH:YEAR:HOUR:MIN (24 hour time)");
        register_sub("onlywarriors", this::onlywarriors_cmd, CivSettings.localize.localizedString("adcmd_war_onlywarriorsDesc"));
    }

    public void onlywarriors_cmd() {

        War.setOnlyWarriors(!War.isOnlyWarriors());

        if (War.isOnlyWarriors()) {

            for (Player player : Bukkit.getOnlinePlayers()) {
                Resident resident = CivGlobal.getResident(player);

                if (player.isOp()) {
                    CivMessage.send(sender, CivSettings.localize.localizedString("var_adcmd_war_onlywarriorsSkippedAdmin", player.getName()));
                    continue;
                }

                if (resident == null || !resident.hasTown() ||
                        !resident.getTown().getCiv().getDiplomacyManager().isAtWar()) {

                    TaskMaster.syncTask(new PlayerKickBan(player.getName(), true, false, CivSettings.localize.localizedString("adcmd_war_onlywarriorsKickMessage")));
                }
            }

            CivMessage.global(CivSettings.localize.localizedString("adcmd_war_onlywarriorsStart"));
        } else {
            CivMessage.global(CivSettings.localize.localizedString("adcmd_war_onlywarriorsEnd"));
        }
    }


    public void start_cmd() {

        War.setWarTime(true);
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_war_startSuccess"));
    }

    public void stop_cmd() {

        War.setWarTime(false);
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_war_stopSuccess"));
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
        if (!sender.isOp()) {
            throw new CivException(CivSettings.localize.localizedString("adcmd_NotAdmin"));
        }
    }

}
