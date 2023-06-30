package com.avrgaming.global.perks.components;


import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.interactive.InteractiveRenameCivOrTown;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;

public class RenameCivOrTown extends PerkComponent {

	@Override
	public void onActivate(Resident resident) {
		
		if (!resident.hasTown()) {
			CivMessage.sendError(resident, CivSettings.localize.localizedString("RenameCivOrTown_NotResident"));
			return;
		}
		
		resident.setInteractiveMode(new InteractiveRenameCivOrTown(resident, this));
	}
	
}
