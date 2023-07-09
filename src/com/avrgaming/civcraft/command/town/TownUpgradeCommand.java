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
package com.avrgaming.civcraft.command.town;

import org.apache.commons.lang.WordUtils;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigTownUpgrade;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class TownUpgradeCommand extends CommandBase {

	@SuppressWarnings("unused")
	@Override
	public void init() {
        command = "/town upgrade";
        displayName = CivSettings.localize.localizedString("cmd_town_upgrade_name");

        register_sub("list", this::list_cmd, CivSettings.localize.localizedString("cmd_town_upgrade_listDesc"));
        register_sub("purchased", this::purchased_cmd, CivSettings.localize.localizedString("cmd_town_upgrade_purchasedDesc"));
        register_sub("buy", this::buy_cmd, CivSettings.localize.localizedString("cmd_town_upgrade_buyDesc"));

    }

	@SuppressWarnings("unused")
	public void purchased_cmd() throws CivException {
		Town town = this.getSelectedTown();
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_upgrade_purchasedHeading"));

		StringBuilder out = new StringBuilder();
		for (ConfigTownUpgrade upgrade : town.getUpgrades().values()) {
			out.append(upgrade.name).append(", ");
		}

		CivMessage.send(sender, out.toString());
	}
	
	private void list_upgrades(String category, Town town) throws CivException {
		if (!ConfigTownUpgrade.categories.containsKey(category.toLowerCase()) && !category.equalsIgnoreCase("all")) {
			throw new CivException(CivSettings.localize.localizedString("var_cmd_town_upgrade_listnoCat",category));
		}
				
		for (ConfigTownUpgrade upgrade : CivSettings.townUpgrades.values()) {
			if (category.equalsIgnoreCase("all") || upgrade.category.equalsIgnoreCase(category)) {	
				if (upgrade.isAvailable(town)) {
					CivMessage.send(sender, upgrade.name+" "+CivColor.LightGray+CivSettings.localize.localizedString("Cost")+" "+CivColor.Yellow+upgrade.cost);
				}
			}
		}
	}

	@SuppressWarnings("unused")
	public void list_cmd() throws CivException {
		Town town = this.getSelectedTown();

		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_upgrade_listHeading"));

		if (args.length < 2) {
			CivMessage.send(sender, "- "+CivColor.Gold+CivSettings.localize.localizedString("cmd_town_upgrade_listAllHeading")+" "+
					CivColor.LightBlue+"("+ConfigTownUpgrade.getAvailableCategoryCount("all", town)+")");
			for (String category : ConfigTownUpgrade.categories.keySet()) {
				CivMessage.send(sender, "- "+CivColor.Gold+WordUtils.capitalize(category)+
						CivColor.LightBlue+" ("+ConfigTownUpgrade.getAvailableCategoryCount(category, town)+")");
			}
			return;
		}

		list_upgrades(args[1], town);

	}

	@SuppressWarnings("unused")
	public void buy_cmd() throws CivException {
		if (args.length < 2) {
			list_upgrades("all", getSelectedTown());
			CivMessage.send(sender, CivSettings.localize.localizedString("cmd_town_upgrade_buyHeading"));
			return;
		}

		Town town = this.getSelectedTown();

		StringBuilder combinedArgs = new StringBuilder();
		args = this.stripArgs(args, 1);
		for (String arg : args) {
			combinedArgs.append(arg).append(" ");
		}
		combinedArgs = new StringBuilder(combinedArgs.toString().trim());

		ConfigTownUpgrade upgrade = CivSettings.getUpgradeByNameRegex(town, combinedArgs.toString());
		if (upgrade == null) {
			throw new CivException(CivSettings.localize.localizedString("cmd_town_upgrade_buyInvalid")+" "+combinedArgs);
		}

		if (town.hasUpgrade(upgrade.id)) {
			throw new CivException(CivSettings.localize.localizedString("cmd_town_upgrade_buyOwned"));
		}

		//TODO make upgrades take time by using hammers.
		town.purchaseUpgrade(upgrade);
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_upgrade_buySuccess", upgrade.name));
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
	public void permissionCheck() throws CivException {
		this.validMayorAssistantLeader();
	}

}
