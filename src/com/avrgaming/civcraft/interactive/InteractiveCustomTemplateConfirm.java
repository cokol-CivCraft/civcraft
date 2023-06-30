package com.avrgaming.civcraft.interactive;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.global.perks.Perk;
import com.avrgaming.global.perks.components.CustomTemplate;

public class InteractiveCustomTemplateConfirm implements InteractiveResponse {

	String playerName;
	CustomTemplate customTemplate;
	
	public InteractiveCustomTemplateConfirm(String playerName, CustomTemplate customTemplate) {
		this.playerName = playerName;
		this.customTemplate = customTemplate;
		displayQuestion();
	}
	
	public void displayQuestion() {
		Player player;
		try {
			player = CivGlobal.getPlayer(playerName);
		} catch (CivException e) {
			return;
		}
		
		Resident resident = CivGlobal.getResident(player);
		Town town = resident.getTown();
		Perk perk = customTemplate.getParent();
		
		CivMessage.sendHeading(player, CivSettings.localize.localizedString("interactive_template_heading"));
		CivMessage.send(player, CivColor.LightGreen+CivColor.BOLD+CivSettings.localize.localizedString("var_interactive_template_bind1",perk.getDisplayName(),town.getName()));
		CivMessage.send(player, CivColor.LightGreen+CivColor.BOLD+CivSettings.localize.localizedString("interactive_template_bind2"));
		CivMessage.send(player, CivColor.LightGreen+CivColor.BOLD+CivSettings.localize.localizedString("interactive_template_bind3"));
		CivMessage.send(player, CivColor.LightGreen+CivColor.BOLD+CivSettings.localize.localizedString("interactive_template_bind4"));
		CivMessage.send(player, CivColor.LightGreen+ChatColor.BOLD+CivSettings.localize.localizedString("interactive_template_bind5"));
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
			CivMessage.sendError(player, CivSettings.localize.localizedString("interactive_template_cancel"));
			return;
		}
		
		customTemplate.bindTemplateToTown(resident.getTown(), resident);
		customTemplate.markAsUsed(resident);
		CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_interactive_template_success",customTemplate.getParent().getDisplayName(),resident.getTown().getName()));
	}
}
