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
package com.avrgaming.civcraft.command.resident;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;

public class ResidentToggleCommand extends CommandBase {

	@SuppressWarnings("unused")
	@Override
	public void init() {
        command = "/resident toggle";
        displayName = CivSettings.localize.localizedString("cmd_res_toggle_name");

        register_sub("map", this::map_cmd, CivSettings.localize.localizedString("cmd_res_toggle_mapDesc"));
        register_sub("info", this::info_cmd, CivSettings.localize.localizedString("cmd_res_toggle_infoDesc"));
        register_sub("showtown", this::showtown_cmd, CivSettings.localize.localizedString("cmd_res_toggle_showtownDesc"));
        register_sub("showciv", this::showciv_cmd, CivSettings.localize.localizedString("cmd_res_toggle_showcivDesc"));
        register_sub("showscout", this::showscout_cmd, CivSettings.localize.localizedString("cmd_res_toggle_showscoutDesc"));
        register_sub("combatinfo", this::combatinfo_cmd, CivSettings.localize.localizedString("cmd_res_toggle_combatinfoDesc"));
        register_sub("itemdrops", this::itemdrops_cmd, CivSettings.localize.localizedString("cmd_res_toggle_itemdropsDesc"));
        register_sub("titles", this::titles_cmd, CivSettings.localize.localizedString("cmd_res_toggle_titleAPIDesc"));

    }

	@SuppressWarnings("unused")
	public void itemdrops_cmd() throws CivException {
		toggle();
	}

	@SuppressWarnings("unused")
	public void map_cmd() throws CivException {
		toggle();
	}

	@SuppressWarnings("unused")
	public void showtown_cmd() throws CivException {
		toggle();
	}

	@SuppressWarnings("unused")
	public void showciv_cmd() throws CivException  {
		toggle();
	}

	@SuppressWarnings("unused")
	public void showscout_cmd() throws CivException  {
		toggle();
	}

	@SuppressWarnings("unused")
	public void info_cmd() throws CivException {
		toggle();
	}

	@SuppressWarnings("unused")
	public void combatinfo_cmd() throws CivException {
		toggle();
	}

	@SuppressWarnings("unused")
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

	@SuppressWarnings("unused")
	@Override
	public void doDefaultAction() {
		showHelp();
	}

	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@SuppressWarnings("unused")
	@Override
	public void permissionCheck() {

	}

}
