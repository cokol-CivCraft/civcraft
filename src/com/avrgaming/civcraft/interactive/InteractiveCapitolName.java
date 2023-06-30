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

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.town.TownCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.CivColor;

public class InteractiveCapitolName implements InteractiveResponse {

	@Override
	public void respond(String message, Resident resident) {
		
		Player player;
		try {
			player = CivGlobal.getPlayer(resident);
		} catch (CivException e) {
			return;
		}

		if (message.equalsIgnoreCase("cancel")) {
			CivMessage.send(player, CivSettings.localize.localizedString("interactive_capitol_cancel"));
			resident.clearInteractiveMode();
			return;
		}
		
		if (!StringUtils.isAlpha(message) || !StringUtils.isAsciiPrintable(message)) {
			CivMessage.send(player, CivColor.Rose+ChatColor.BOLD+CivSettings.localize.localizedString("interactive_capitol_invalidname"));
			return;
		}
		
		message = message.replace(" ", "_");
		message = message.replace("\"", "");
		message = message.replace("\'", "");
		
		resident.desiredCapitolName = message;
		CivMessage.send(player, CivColor.LightGreen+CivSettings.localize.localizedString("var_interactive_capitol_confirm1",CivColor.Yellow+resident.desiredCivName+CivColor.LightGreen,CivColor.Yellow+resident.desiredCapitolName+CivColor.LightGreen));
		CivMessage.sendHeading(player, CivSettings.localize.localizedString("interactive_capitol_confirmSurvey"));
		
		class SyncTask implements Runnable {
			String playerName;
			
			
			public SyncTask(String name) {
				this.playerName = name;
			}

			@Override
			public void run() {
				Player player;
				try {
					player = CivGlobal.getPlayer(playerName);
				} catch (CivException e) {
					return;
				}
				
				Resident resident = CivGlobal.getResident(playerName);
				if (resident == null) {
					return;
				}
				
				CivMessage.send(player, TownCommand.survey(player.getLocation()));
				CivMessage.send(player, "");
				CivMessage.send(player, CivColor.LightGreen+ChatColor.BOLD+CivSettings.localize.localizedString("interactive_capitol_confirmPrompt"));
				resident.setInteractiveMode(new InteractiveConfirmCivCreation());				
			}
		
		}

		TaskMaster.syncTask(new SyncTask(resident.getName())); 

		return;
	}

}
