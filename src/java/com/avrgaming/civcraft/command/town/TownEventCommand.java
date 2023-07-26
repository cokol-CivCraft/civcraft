package com.avrgaming.civcraft.command.town;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.randomevents.RandomEvent;
import com.avrgaming.civcraft.util.CivColor;

import java.text.SimpleDateFormat;

public class TownEventCommand extends CommandBase {

    @Override
    public void init() {
        command = "/town event";
        displayName = CivSettings.localize.localizedString("cmd_town_event_name");

        register_sub("show", this::show_cmd, CivSettings.localize.localizedString("cmd_town_event_showDesc"));
        register_sub("activate", this::activate_cmd, CivSettings.localize.localizedString("cmd_town_event_activateDesc"));
    }

    public void activate_cmd() throws CivException {
        Town town = getSelectedTown();
        RandomEvent event = town.getActiveEvent();

        if (event == null) {
            CivMessage.sendError(sender, CivSettings.localize.localizedString("cmd_town_event_activateNone"));
        } else {
            event.activate();
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_town_event_activateSuccess"));
        }
    }

    public void show_cmd() throws CivException {
        Town town = getSelectedTown();
        RandomEvent event = town.getActiveEvent();

        if (event == null) {
            CivMessage.sendError(sender, CivSettings.localize.localizedString("cmd_town_event_activateNone"));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("M/dd h:mm:ss a z");

            CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_event_showCurrent") + " " + event.configRandomEvent.name);
            CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_town_event_showStarted") + " " + CivColor.LightGreen + sdf.format(event.getStartDate()));
            CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_town_event_showEnd") + " " + CivColor.LightGreen + sdf.format(event.getEndDate()));
            if (event.isActive()) {
                CivMessage.send(sender, CivColor.LightGray + CivSettings.localize.localizedString("cmd_town_event_showActive"));
            } else {
                CivMessage.send(sender, CivColor.Yellow + CivSettings.localize.localizedString("cmd_town_event_showInactive"));
            }
            CivMessage.send(sender, CivColor.Green + "-- " + CivSettings.localize.localizedString("cmd_town_event_showMessageHeading") + " ---");
            CivMessage.send(sender, CivColor.LightGray + event.getMessages());
        }
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
    public void permissionCheck() {

    }

}