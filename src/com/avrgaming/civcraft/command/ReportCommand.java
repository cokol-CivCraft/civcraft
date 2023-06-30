package com.avrgaming.civcraft.command;

import org.bukkit.ChatColor;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.interactive.InteractiveReportPlayer;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.global.reports.ReportManager;

public class ReportCommand extends CommandBase {

	@Override
	public void init() {
		command = "/report";
		displayName = CivSettings.localize.localizedString("cmd_reprot_Name");
		
		commands.put("player", CivSettings.localize.localizedString("cmd_report_playerDesc"));
	}

	public void player_cmd() throws CivException {
		Resident resident = getResident();
		Resident reportedResident = getNamedResident(1);
		
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_report_Heading"));
		CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+CivSettings.localize.localizedString("cmd_report_1")+" "+reportedResident.getName());
		CivMessage.send(sender, " ");
		CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+CivSettings.localize.localizedString("cmd_report_2")+" "+CivColor.LightGreen+ChatColor.BOLD+ReportManager.getReportTypes());
		CivMessage.send(sender, " ");
		CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+ CivSettings.localize.localizedString("cmd_report_3")+
				CivSettings.localize.localizedString("cmd_report_4"));
		CivMessage.send(sender, CivColor.LightGray+ChatColor.BOLD+CivSettings.localize.localizedString("cmd_report_5"));
		resident.setInteractiveMode(new InteractiveReportPlayer(reportedResident.getName()));
	}
	
	@Override
	public void doDefaultAction() throws CivException {
		showHelp();
	}

	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() throws CivException {
		
	}

}
