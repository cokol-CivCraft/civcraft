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
package com.avrgaming.civcraft.interactive;

import com.avrgaming.civcraft.command.town.TownCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class InteractiveTownName implements InteractiveResponse {

    @Override
    public void respond(String message, Resident resident) {

        Player player;
        try {
            player = CivGlobal.getPlayer(resident);
        } catch (CivException e) {
            return;
        }

        if (message.equalsIgnoreCase("cancel")) {
            CivMessage.send(player, CivSettings.localize.localizedString("interactive_town_cancelled"));
            resident.clearInteractiveMode();
            return;
        }

        if (!StringUtils.isAlpha(message) || !StringUtils.isAsciiPrintable(message)) {
            CivMessage.send(player, String.valueOf(ChatColor.RED) + ChatColor.BOLD + CivSettings.localize.localizedString("interactive_town_nameInvalid"));
            return;
        }

        resident.desiredTownName = message.replace(" ", "_").replace("\"", "").replace("'", "");
        CivMessage.send(player, ChatColor.GREEN + CivSettings.localize.localizedString("var_interactive_town_confirmName", ChatColor.YELLOW + resident.desiredTownName + ChatColor.GREEN));

        class SyncTask implements Runnable {
            final Resident resident;

            public SyncTask(Resident resident) {
                this.resident = resident;
            }


            @Override
            public void run() {
                Player player;
                try {
                    player = CivGlobal.getPlayer(resident);
                } catch (CivException e) {
                    return;
                }

                CivMessage.sendHeading(player, CivSettings.localize.localizedString("interactive_town_surveyResults"));
                CivMessage.send(player, TownCommand.survey(player.getLocation(), 1));

                if (resident.getCiv().getCapitolTownHallLocation() == null) {
                    CivMessage.sendError(player, CivSettings.localize.localizedString("interactive_town_noCapitol"));
                    resident.clearInteractiveMode();
                    return;
                }

                CivMessage.send(player, String.valueOf(ChatColor.GREEN) + ChatColor.BOLD + CivSettings.localize.localizedString("interactive_town_confirm"));

                resident.setInteractiveMode(new InteractiveConfirmTownCreation());
            }
        }

        TaskMaster.syncTask(new SyncTask(resident));


    }

}
