package com.avrgaming.civcraft.interactive;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMission;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.units.MissionBook;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.EspionageMissionTask;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;

public class InteractiveSpyMission implements InteractiveResponse {

    public ConfigMission mission;
    public String playerName;
    public Location playerLocation;
    public Town target;

    public InteractiveSpyMission(ConfigMission mission, String playerName, Location playerLocation, Town target) {
        this.mission = mission;
        this.playerName = playerName;
        this.playerLocation = playerLocation;
        this.target = target;
        displayQuestion();
    }

    public void displayQuestion() {
        Player player;
        try {
            player = CivGlobal.getPlayer(playerName);
        } catch (CivException e) {
            return;
        }

        CivMessage.sendHeading(player, CivSettings.localize.localizedString("interactive_spy_heading") + " " + mission.name);

        double failChance = MissionBook.getMissionFailChance(mission, target);
        double compChance = MissionBook.getMissionCompromiseChance(mission, target);
        DecimalFormat df = new DecimalFormat();

        String successChance = df.format((1 - failChance) * 100) + "%";
        String compromiseChance = df.format(compChance) + "%";
        String length = "";

        int mins = mission.length / 60;
        int seconds = mission.length % 60;
        if (mins > 0) {
            length += CivSettings.localize.localizedString("var_interactive_spy_mins", mins);
            if (seconds > 0) {
                length += " & ";
            }
        }

        if (seconds > 0) {
            length += CivSettings.localize.localizedString("var_interactive_spy_seconds", seconds);
        }

        CivMessage.send(player, String.valueOf(ChatColor.DARK_GREEN) + ChatColor.BOLD + CivSettings.localize.localizedString("var_interactive_spy_prompt1", ChatColor.GREEN + successChance + ChatColor.DARK_GREEN + ChatColor.BOLD));
        CivMessage.send(player, String.valueOf(ChatColor.DARK_GREEN) + ChatColor.BOLD + CivSettings.localize.localizedString("var_interactive_spy_prompt2", ChatColor.GREEN + compromiseChance + ChatColor.DARK_GREEN + ChatColor.BOLD));
        CivMessage.send(player, String.valueOf(ChatColor.DARK_GREEN) + ChatColor.BOLD + CivSettings.localize.localizedString("var_interactive_spy_prompt3", String.valueOf(ChatColor.YELLOW) + mission.cost + ChatColor.DARK_GREEN + ChatColor.BOLD, CivSettings.CURRENCY_NAME));
        CivMessage.send(player, String.valueOf(ChatColor.DARK_GREEN) + ChatColor.BOLD + CivSettings.localize.localizedString("var_interactive_spy_prompt4", ChatColor.YELLOW + length + ChatColor.DARK_GREEN + ChatColor.BOLD));
        CivMessage.send(player, String.valueOf(ChatColor.DARK_GREEN) + ChatColor.BOLD + CivSettings.localize.localizedString("interactive_spy_prompt5"));
        CivMessage.send(player, String.valueOf(ChatColor.DARK_GREEN) + ChatColor.BOLD + CivSettings.localize.localizedString("interactive_spy_prompt6"));
        CivMessage.send(player, String.valueOf(ChatColor.DARK_GREEN) + ChatColor.BOLD + CivSettings.localize.localizedString("interactive_spy_prompt7"));
    }


    @Override
    public void respond(String message, Resident resident) {
        Player player;
        try {
            player = CivGlobal.getPlayer(resident);
        } catch (CivException e) {
            return;
        }
        resident.clearInteractiveMode();

        if (!message.equalsIgnoreCase("yes")) {
            CivMessage.sendError(player, CivSettings.localize.localizedString("interactive_spy_aborted"));
            return;
        }

        if (!TaskMaster.hasTask("missiondelay:" + playerName)) {
            TaskMaster.asyncTask("missiondelay:" + playerName, (new EspionageMissionTask(mission, playerName, target, mission.length)), 0);
        } else {
            CivMessage.sendError(player, CivSettings.localize.localizedString("interactive_spy_waiting"));
        }
    }
}
