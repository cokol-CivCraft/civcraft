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
package com.avrgaming.civcraft.interactive;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.questions.TownNewRequest;
import com.avrgaming.civcraft.util.CivColor;

public class InteractiveConfirmTownCreation implements InteractiveResponse {

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
			CivMessage.send(player, CivSettings.localize.localizedString("interactive_town_cancel"));
			return;
		}
		
		if (resident.desiredTownName == null) {
			CivMessage.send(player, CivColor.Rose+CivSettings.localize.localizedString("interactive_town_createError"));
			return;
		}
		
		TownNewRequest join = new TownNewRequest();		
		join.resident = resident;
		join.civ = resident.getCiv();
		try {
			CivGlobal.questionLeaders(player, resident.getCiv(), CivSettings.localize.localizedString("var_interactive_town_alert",player.getName(),resident.desiredTownName,(player.getLocation().getBlockX()+","+player.getLocation().getBlockY()+","+player.getLocation().getBlockZ())),
					30*1000, join);
		} catch (CivException e) {
			CivMessage.sendError(player, e.getMessage());
			return;
		}
		
		CivMessage.send(player, CivColor.Yellow+CivSettings.localize.localizedString("interactive_town_request"));
//		CivGlobal.questionPlayer(player, CivGlobal.getPlayer(newResident), 
//				"Would you like to join the town of "+town.getName()+"?",
//				INVITE_TIMEOUT, join);
		
//		TaskMaster.syncTask(new FoundTownSync(resident));
	}

}
