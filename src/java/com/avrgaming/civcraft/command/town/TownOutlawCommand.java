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
package com.avrgaming.civcraft.command.town;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.TownAddOutlawTask;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TownOutlawCommand extends CommandBase {

    @Override
    public void init() {
        command = "/town outlaw";
        displayName = CivSettings.localize.localizedString("cmd_town_outlaw_name");

        register_sub("add", this::add_cmd, CivSettings.localize.localizedString("cmd_town_outlaw_addDesc"));
        register_sub("remove", this::remove_cmd, CivSettings.localize.localizedString("cmd_town_outlaw_removeDesc"));
        register_sub("list", this::list_cmd, CivSettings.localize.localizedString("cmd_town_outlaw_listDesc"));
        register_sub("addall", this::addall_cmd, CivSettings.localize.localizedString("cmd_town_outlaw_addallDesc"));
        register_sub("removeall", this::removeall_cmd, CivSettings.localize.localizedString("cmd_town_outlaw_removeallDesc"));
        register_sub("addallciv", this::addallciv_cmd, CivSettings.localize.localizedString("cmd_town_outlaw_addallcivDesc"));
        register_sub("removeallciv", this::removeallciv_cmd, CivSettings.localize.localizedString("cmd_town_outlaw_removeallcivDesc"));
    }

    public void addall_cmd() throws CivException {
        Town town = getSelectedTown();
        Town targetTown = getNamedTown(1);

        for (Resident resident : targetTown.getResidents()) {

            try {
                Player player = CivGlobal.getPlayer(args[1]);
                CivMessage.send(player, String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_town_outlaw_addAllAlert1", town.getName()));
            } catch (CivException ignored) {
            }
            TaskMaster.asyncTask(new TownAddOutlawTask(resident.getName(), town), 1000);
        }
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_outlaw_addallalert3", args[1]));
    }

    public void removeall_cmd() throws CivException {
        Town town = getSelectedTown();
        Town targetTown = getNamedTown(1);

        for (Resident resident : targetTown.getResidents()) {
            town.removeOutlaw(resident.getName());
        }
    }

    public void addallciv_cmd() throws CivException {
        Town town = getSelectedTown();
        Civilization targetCiv = getNamedCiv(1);

        for (Town targetTown : targetCiv.getTowns()) {
            for (Resident resident : targetTown.getResidents()) {

                try {
                    Player player = CivGlobal.getPlayer(args[1]);
                    CivMessage.send(player, String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_town_outlaw_addAllAlert1", town.getName()));
                } catch (CivException ignored) {
                }
                TaskMaster.asyncTask(new TownAddOutlawTask(resident.getName(), town), 1000);
            }
        }
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_outlaw_addallalert3", args[1]));
    }

    public void removeallciv_cmd() throws CivException {
        Town town = getSelectedTown();
        Civilization targetCiv = getNamedCiv(1);

        for (Town targetTown : targetCiv.getTowns()) {
            for (Resident resident : targetTown.getResidents()) {
                town.removeOutlaw(resident.getName());
            }
        }
    }

    public void add_cmd() throws CivException {
        Town town = getSelectedTown();

        if (args.length < 2) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_outlaw_addPrompt"));
        }

        Resident resident = getNamedResident(1);
        if (resident.getTown() == town) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_outlaw_addError"));
        }

        try {
            Player player = CivGlobal.getPlayer(args[1]);
            CivMessage.send(player, String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_town_outlaw_addAllAlert1", town.getName()));
        } catch (CivException ignored) {
        }

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_outlaw_addallalert3", args[1]));
        TaskMaster.asyncTask(new TownAddOutlawTask(args[1], town), 1000);
    }

    public void remove_cmd() throws CivException {
        Town town = getSelectedTown();

        if (args.length < 2) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_outlaw_removePrompt"));
        }

        town.removeOutlaw(args[1]);

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_outlaw_removeSuccess", args[1]));
    }

    public void list_cmd() throws CivException {
        Town town = getSelectedTown();

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_outlaw_listHeading"));

        StringBuilder out = new StringBuilder();
        for (String outlaw : town.outlaws) {
            if (outlaw.length() >= 2) {
                Resident res = CivGlobal.getResidentViaUUID(UUID.fromString(outlaw));
                out.append(res.getName()).append(",");
            }
        }

        CivMessage.send(sender, out.toString());

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
        validMayorAssistantLeader();
    }

}
