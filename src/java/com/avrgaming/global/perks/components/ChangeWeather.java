package com.avrgaming.global.perks.components;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.interactive.InteractiveConfirmWeatherChange;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;

public class ChangeWeather extends PerkComponent {

	@Override
	public void onActivate(Resident resident) {
		Player player;
		try {
			player = CivGlobal.getPlayer(resident);
		} catch (CivException e) {
			return;
		}
		if (!player.getWorld().isThundering() && !player.getWorld().hasStorm()) {
			CivMessage.sendError(resident, CivSettings.localize.localizedString("weather_isSunny"));
			return;
		}
		
		CivMessage.sendHeading(resident, CivSettings.localize.localizedString("weather_heading"));
		CivMessage.send(resident, CivColor.Green+CivSettings.localize.localizedString("weather_confirmPrompt"));
		CivMessage.send(resident, CivColor.LightGray+CivSettings.localize.localizedString("weather_confirmPrompt2"));
		resident.setInteractiveMode(new InteractiveConfirmWeatherChange(this));
	}
}
