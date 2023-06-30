package com.avrgaming.civcraft.command.admin;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.bukkit.configuration.InvalidConfigurationException;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigPerk;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;

public class AdminPerkCommand extends CommandBase {

	@Override
	public void init() {
		command = "/ad perk";
		displayName = CivSettings.localize.localizedString("adcmd_perk_name");
		
		commands.put("list", CivSettings.localize.localizedString("adcmd_perk_listDesc"));
		commands.put("reload", CivSettings.localize.localizedString("adcmd_perk_reloadDesc"));
	}

	public void list_cmd() {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_perk_listHeading"));
		for (ConfigPerk perk : CivSettings.perks.values()) {
			CivMessage.send(sender, CivColor.Green+perk.display_name+CivColor.LightGreen+" id:"+CivColor.Rose+perk.id);
		}
		CivMessage.send(sender, CivColor.LightGray+CivSettings.localize.localizedString("adcmd_perk_listingSuccess"));
	}
	
	public void reload_cmd() throws FileNotFoundException, IOException, InvalidConfigurationException, InvalidConfiguration
	{
		CivSettings.reloadPerks();
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
		// TODO Auto-generated method stub
		
	}

}
