package com.avrgaming.civcraft.command.admin;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigPerk;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;
import org.bukkit.configuration.InvalidConfigurationException;

import java.io.IOException;

public class AdminPerkCommand extends CommandBase {

	@Override
	public void init() {
		command = "/ad perk";
		displayName = CivSettings.localize.localizedString("adcmd_perk_name");

		register_sub("list", this::list_cmd, CivSettings.localize.localizedString("adcmd_perk_listDesc"));
		register_sub("reload", this::reload_cmd, CivSettings.localize.localizedString("adcmd_perk_reloadDesc"));
	}

	@SuppressWarnings("unused")
	public void list_cmd() {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_perk_listHeading"));
		for (ConfigPerk perk : CivSettings.perks.values()) {
			CivMessage.send(sender, CivColor.Green + perk.display_name + CivColor.LightGreen + " id:" + CivColor.Rose + perk.id);
		}
		CivMessage.send(sender, CivColor.LightGray + CivSettings.localize.localizedString("adcmd_perk_listingSuccess"));
	}

	@SuppressWarnings("unused")
	public void reload_cmd() {
		try {
			CivSettings.reloadPerks();
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}
	}


	@Override
	public void doDefaultAction() {
		showHelp();
	}

	@Override
	public void showHelp() {
		showBasicHelp();
	}

    @Override
    public void permissionCheck() {
        // TODO Auto-generated method stub

    }

}
