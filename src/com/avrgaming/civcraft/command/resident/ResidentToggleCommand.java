/*************************************************************************
 * 
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
package com.avrgaming.civcraft.command.resident;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;

public class ResidentToggleCommand extends CommandBase {

	@Override
	public void init() {
		command = "/resident toggle";
		displayName = CivSettings.localize.localizedString("cmd_res_toggle_name");	
		
		commands.put("map", CivSettings.localize.localizedString("cmd_res_toggle_mapDesc"));
		commands.put("info", CivSettings.localize.localizedString("cmd_res_toggle_infoDesc"));
		commands.put("showtown", CivSettings.localize.localizedString("cmd_res_toggle_showtownDesc"));
		commands.put("showciv", CivSettings.localize.localizedString("cmd_res_toggle_showcivDesc"));
		commands.put("showscout", CivSettings.localize.localizedString("cmd_res_toggle_showscoutDesc"));
		commands.put("combatinfo", CivSettings.localize.localizedString("cmd_res_toggle_combatinfoDesc"));
		commands.put("itemdrops", CivSettings.localize.localizedString("cmd_res_toggle_itemdropsDesc"));
		commands.put("titles", CivSettings.localize.localizedString("cmd_res_toggle_titleAPIDesc"));
		
	}
	public void itemdrops_cmd() throws CivException {
		toggle();
	}
	
	public void map_cmd() throws CivException {
		toggle();
	}
	public void showtown_cmd() throws CivException {
		toggle();
	}
	
	public void showciv_cmd() throws CivException  {
		toggle();
	}
	
	public void showscout_cmd() throws CivException  {
		toggle();
	}
	
	public void info_cmd() throws CivException {
		toggle();
	}
	
	public void combatinfo_cmd() throws CivException {
		toggle();
	}
	
	public void titles_cmd() throws CivException {
		toggle();
	}
	
	private void toggle() throws CivException {
		Resident resident = getResident();
	
		boolean result;
		switch(args[0].toLowerCase()) {
		case "map":
			resident.setShowMap(!resident.isShowMap());
			result = resident.isShowMap();
			break;
		case "showtown":
			resident.setShowTown(!resident.isShowTown());
			result = resident.isShowTown();
			break;
		case "showciv":
			resident.setShowCiv(!resident.isShowCiv());
			result = resident.isShowCiv();
			break;
		case "showscout":
			resident.setShowScout(!resident.isShowScout());
			result = resident.isShowScout();
			break;
		case "info":
			resident.setShowInfo(!resident.isShowInfo());
			result = resident.isShowInfo();
			break;
		case "combatinfo":
			resident.setCombatInfo(!resident.isCombatInfo());
			result = resident.isCombatInfo();
			break;
		case "titles":
			resident.setTitleAPI(!resident.isTitleAPI());
			result = resident.isTitleAPI();
			break;
		case "itemdrops":
			resident.toggleItemMode();
			return;
		default:
			throw new CivException(CivSettings.localize.localizedString("cmd_unkownFlag")+" "+args[0]);
		}
		
		resident.save();
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_toggled")+" "+args[0]+" -> "+result);
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
