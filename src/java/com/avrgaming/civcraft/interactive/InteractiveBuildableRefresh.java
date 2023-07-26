package com.avrgaming.civcraft.interactive;

import java.io.IOException;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.CivColor;

public class InteractiveBuildableRefresh implements InteractiveResponse {

	String playerName;
	Buildable buildable;
	
	public InteractiveBuildableRefresh(Buildable buildable, String playerName) {
		this.playerName = playerName;
		this.buildable = buildable;
		displayMessage();
	}
	
	public void displayMessage() {
		Player player;
		try {
			player = CivGlobal.getPlayer(playerName);
		} catch (CivException e) {
			return;
		}
		
		CivMessage.sendHeading(player, CivSettings.localize.localizedString("interactive_refresh_Heading"));
		CivMessage.send(player, CivColor.LightGreen+CivColor.BOLD+CivSettings.localize.localizedString("var_interactive_refresh_prompt1",buildable.getDisplayName()));
		CivMessage.send(player, CivColor.LightGreen+CivColor.BOLD+CivSettings.localize.localizedString("interactive_refresh_prompt2"));
		CivMessage.send(player, CivColor.LightGreen+CivColor.BOLD+CivSettings.localize.localizedString("interactive_refresh_prompt3"));
		
	}
	
	
	@Override
	public void respond(String message, Resident resident) {
		resident.clearInteractiveMode();

		if (!message.equalsIgnoreCase("yes")) {
			CivMessage.send(resident, CivColor.LightGray+CivSettings.localize.localizedString("interactive_refresh_cancel"));
			return;
		}
		
		class SyncTask implements Runnable {
			Buildable buildable;
			Resident resident;
			
			public SyncTask(Buildable buildable, Resident resident) {
				this.buildable = buildable;
				this.resident = resident;
			}
			
			@Override
			public void run() {	
				try {
					try {
						buildable.repairFromTemplate();
						buildable.getTown().markLastBuildableRefeshAsNow();
						CivMessage.sendSuccess(resident, CivSettings.localize.localizedString("var_interactive_refresh_success",buildable.getDisplayName()));
					} catch (IOException e) {
						e.printStackTrace();
						throw new CivException(CivSettings.localize.localizedString("interactive_refresh_exception")+" "+buildable.getSavedTemplatePath()+" ?");
					} 
				} catch (CivException e) {
					CivMessage.sendError(resident, e.getMessage());
				}
			}
		}
		
		TaskMaster.syncTask(new SyncTask(buildable, resident));	
	}
}
