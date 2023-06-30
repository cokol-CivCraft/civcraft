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
package com.avrgaming.civcraft.command.admin;

import java.sql.SQLException;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.camp.Camp;
import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.AlreadyRegisteredException;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;

public class AdminResCommand extends CommandBase {

	@Override
	public void init() {
		command = "/ad res";
		displayName = CivSettings.localize.localizedString("adcmd_res_Name");
		
		commands.put("settown", CivSettings.localize.localizedString("adcmd_res_setTownDesc"));
		commands.put("setcamp", CivSettings.localize.localizedString("adcmd_res_setCampDesc"));
		commands.put("cleartown", CivSettings.localize.localizedString("adcmd_res_clearTownDesc"));
		commands.put("enchant", CivSettings.localize.localizedString("adcmd_res_enchantDesc"));
		commands.put("rename", CivSettings.localize.localizedString("adcmd_res_renameDesc"));
	}
	
	public void rename_cmd() throws CivException {
		Resident resident = getNamedResident(1);
		String newName = getNamedString(2, CivSettings.localize.localizedString("adcmd_res_renamePrompt"));

		
		
		Resident newResident = CivGlobal.getResident(newName);
		if (newResident != null) {
			throw new CivException(CivSettings.localize.localizedString("var_adcmd_res_renameExists",newResident.getName(),resident.getName()));
		}
		
		/* Create a dummy resident to make sure name is valid. */
		try {
			new Resident(null, newName);
		} catch (InvalidNameException e1) {
			throw new CivException(CivSettings.localize.localizedString("adcmd_res_renameInvalid"));
		}
		
		/* Delete the old resident object. */
		try {
			resident.delete();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CivException(e.getMessage());
		}
		
		/* Remove resident from CivGlobal tables. */
		CivGlobal.removeResident(resident);
		
		/* Change the resident's name. */
		try {
			resident.setName(newName);
		} catch (InvalidNameException e) {
			e.printStackTrace();
			throw new CivException(CivSettings.localize.localizedString("internalCommandException")+" "+e.getMessage());
		}
		
		/* Resave resident to DB and global tables. */
		CivGlobal.addResident(resident);
		resident.save();
		
		CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_res_renameSuccess"));
	}
	
	public void enchant_cmd() throws CivException {
		Player player = getPlayer();
		String enchant = getNamedString(1, CivSettings.localize.localizedString("adcmd_res_enchantHeading"));
		int level = getNamedInteger(2);
		
		
		ItemStack stack = player.getInventory().getItemInMainHand();
		Enchantment ench = Enchantment.getByName(enchant);
		if (ench == null) {
			String out ="";
			for (Enchantment ench2 : Enchantment.values()) {
				out += ench2.getName()+",";
			}
			throw new CivException(CivSettings.localize.localizedString("var_adcmd_res_enchantInvalid1",enchant,out));
		}
		
		stack.addUnsafeEnchantment(ench, level);
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_res_enchantSuccess"));
	}
	
	public void cleartown_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException(CivSettings.localize.localizedString("EnterPlayerName"));
		}
				
		Resident resident = getNamedResident(1);
		
		if (resident.hasTown()) {
			resident.getTown().removeResident(resident);
		}
		
		resident.save();
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_res_cleartownSuccess",resident.getName()));

	}
	
	public void setcamp_cmd() throws CivException {		
		Resident resident = getNamedResident(1);
		Camp camp = getNamedCamp(2);

		if (resident.hasCamp()) {
			resident.getCamp().removeMember(resident);
		}		
		
		camp.addMember(resident);
		
		camp.save();
		resident.save();
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_res_setcampSuccess",resident.getName(),camp.getName()));
	}
	
	
	public void settown_cmd() throws CivException {
		
		if (args.length < 3) {
			throw new CivException(CivSettings.localize.localizedString("adcmd_res_settownPrompt"));
		}
		
		Resident resident = getNamedResident(1);

		Town town = getNamedTown(2);

		if (resident.hasTown()) {
			resident.getTown().removeResident(resident);
		}
		
		try {
			town.addResident(resident);
		} catch (AlreadyRegisteredException e) {
			e.printStackTrace();
			throw new CivException(CivSettings.localize.localizedString("adcmd_res_settownErrorInTown"));
		}
		
		town.save();
		resident.save();
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_res_setTownSuccess",resident.getName(),town.getName()));
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
		
	}

}
