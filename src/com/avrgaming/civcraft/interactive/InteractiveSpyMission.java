package com.avrgaming.civcraft.interactive;

import java.text.DecimalFormat;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

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
import com.avrgaming.civcraft.util.CivColor;

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
		
		CivMessage.sendHeading(player, CivSettings.localize.localizedString("interactive_spy_heading")+" "+mission.name);
		
		double failChance = MissionBook.getMissionFailChance(mission, target);
		double compChance = MissionBook.getMissionCompromiseChance(mission, target);
		DecimalFormat df = new DecimalFormat();
		
		String successChance = df.format((1 - failChance)*100)+"%";
		String compromiseChance = df.format(compChance)+"%";
		String length = "";
		
		int mins = mission.length / 60;
		int seconds = mission.length % 60;
		if (mins > 0) {
			length += CivSettings.localize.localizedString("var_interactive_spy_mins",mins);
			if (seconds > 0) {
				length += " & ";
			}
		}
		
		if (seconds > 0) {
			length += CivSettings.localize.localizedString("var_interactive_spy_seconds",seconds);
		}
		
		CivMessage.send(player, CivColor.Green+CivColor.BOLD+CivSettings.localize.localizedString("var_interactive_spy_prompt1",CivColor.LightGreen+successChance+CivColor.Green+CivColor.BOLD));
		CivMessage.send(player, CivColor.Green+CivColor.BOLD+CivSettings.localize.localizedString("var_interactive_spy_prompt2",CivColor.LightGreen+compromiseChance+CivColor.Green+CivColor.BOLD));
		CivMessage.send(player, CivColor.Green+CivColor.BOLD+CivSettings.localize.localizedString("var_interactive_spy_prompt3",CivColor.Yellow+mission.cost+CivColor.Green+CivColor.BOLD,CivSettings.CURRENCY_NAME));
		CivMessage.send(player, CivColor.Green+CivColor.BOLD+CivSettings.localize.localizedString("var_interactive_spy_prompt4",CivColor.Yellow+length+CivColor.Green+CivColor.BOLD));
		CivMessage.send(player, CivColor.Green+CivColor.BOLD+CivSettings.localize.localizedString("interactive_spy_prompt5"));
		CivMessage.send(player, CivColor.Green+CivColor.BOLD+CivSettings.localize.localizedString("interactive_spy_prompt6"));
		CivMessage.send(player, CivColor.Green+ChatColor.BOLD+CivSettings.localize.localizedString("interactive_spy_prompt7"));
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
		
		if(!TaskMaster.hasTask("missiondelay:"+playerName)) {
			TaskMaster.asyncTask("missiondelay:"+playerName, (new EspionageMissionTask(mission, playerName, playerLocation, target, mission.length)), 0);
		} else {
			CivMessage.sendError(player, CivSettings.localize.localizedString("interactive_spy_waiting"));
			return;
		}
	}
}
