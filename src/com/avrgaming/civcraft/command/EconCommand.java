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
package com.avrgaming.civcraft.command;

import java.sql.SQLException;

import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;

public class EconCommand extends CommandBase {

	@Override
	public void init() {
		command = "/econ";
		displayName = CivSettings.localize.localizedString("cmd_econ_Name");

		commands.put("add", CivSettings.localize.localizedString("cmd_econ_addDesc"));
		commands.put("give", CivSettings.localize.localizedString("cmd_econ_giveDesc"));
		commands.put("set", CivSettings.localize.localizedString("cmd_econ_setDesc"));
		commands.put("sub",CivSettings.localize.localizedString("cmd_econ_subDesc"));
		
		commands.put("addtown", CivSettings.localize.localizedString("cmd_econ_addtownDesc"));
		commands.put("settown", CivSettings.localize.localizedString("cmd_econ_settownDesc"));
		commands.put("subtown", CivSettings.localize.localizedString("cmd_econ_subtownDesc"));
		
		commands.put("addciv", CivSettings.localize.localizedString("cmd_econ_addcivDesc"));
		commands.put("setciv", CivSettings.localize.localizedString("cmd_econ_setcivDesc"));
		commands.put("subciv", CivSettings.localize.localizedString("cmd_econ_subcivDesc"));
		
		commands.put("setdebt", CivSettings.localize.localizedString("cmd_econ_setdebtDesc"));
		commands.put("setdebttown", CivSettings.localize.localizedString("cmd_econ_setdebttownDesc"));
		commands.put("setdebtciv", CivSettings.localize.localizedString("cmd_econ_setdebtcivDesc"));
		
		commands.put("clearalldebt", CivSettings.localize.localizedString("cmd_econ_clearAllDebtDesc"));
		
	}
	
	public void clearalldebt_cmd() throws CivException {
		validEcon();
		
		for (Civilization civ : CivGlobal.getCivs()) {
			civ.getTreasury().setDebt(0);
			try {
				civ.saveNow();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		for (Town town : CivGlobal.getTowns()) {
			town.getTreasury().setDebt(0);
			try {
				town.saveNow();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		for (Resident res : CivGlobal.getResidents()) {
			res.getTreasury().setDebt(0);
			try {
				res.saveNow();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		CivMessage.send(sender, CivSettings.localize.localizedString("cmd_econ_clearedAllDebtSuccess"));
	}
	
	
	public void setdebtciv_cmd() throws CivException {
		validEcon();
		
		Civilization civ = getNamedCiv(1);
		Double amount = getNamedDouble(2);
		civ.getTreasury().setDebt(amount);
		civ.save();
		
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("SetSuccess"));
	}
	
	public void setdebttown_cmd() throws CivException {
		validEcon();
		
		Town town = getNamedTown(1);
		Double amount = getNamedDouble(2);
		town.getTreasury().setDebt(amount);
		town.save();
		
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("SetSuccess"));
	}
	
	public void setdebt_cmd() throws CivException {
		validEcon();
		
		Resident resident = getNamedResident(1);
		Double amount = getNamedDouble(2);
		resident.getTreasury().setDebt(amount);
		resident.save();
		
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("SetSuccess"));
	}
	
	private void validEcon() throws CivException {
		// Allow Console commands to manipulate the economy.
		if(!(sender instanceof ConsoleCommandSender))
		{
			if (!getPlayer().isOp() || !getPlayer().hasPermission(CivSettings.ECON)) {
				throw new CivException(CivSettings.localize.localizedString("cmd_MustBeOP"));
			}
		}
	}
	
	public void add_cmd() throws CivException {
		validEcon();
		
		if (args.length < 3) {
			throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
		}
		
		Resident resident = getNamedResident(1);

		try {
			
			Double amount = Double.valueOf(args[2]);
			resident.getTreasury().deposit(amount);
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_added",args[2],CivSettings.CURRENCY_NAME,args[1]));
			
		} catch (NumberFormatException e) {
			throw new CivException(args[2]+" "+CivSettings.localize.localizedString("cmd_enterNumerError"));
		}
	}
	
	public void give_cmd() throws CivException {
		validEcon();
		
		if (args.length < 3) {
			throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
		}
		
		Resident resident = getNamedResident(1);

		try {
			
			Double amount = Double.valueOf(args[2]);
			resident.getTreasury().deposit(amount);
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_give",args[2],CivSettings.CURRENCY_NAME,args[1]));
			
		} catch (NumberFormatException e) {
			throw new CivException(args[2]+" "+CivSettings.localize.localizedString("cmd_enterNumerError"));
		}
	}
	
	public void set_cmd() throws CivException {
		validEcon();
		
		if (args.length < 3) {
			throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
		}
		
		Resident resident = getNamedResident(1);

		try {
			
			Double amount = Double.valueOf(args[2]);
			resident.getTreasury().setBalance(amount);
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_set",args[1],args[2],CivSettings.CURRENCY_NAME));
			
		} catch (NumberFormatException e) {
			throw new CivException(args[2]+" "+CivSettings.localize.localizedString("cmd_enterNumerError"));
		}
	}
	
	public void sub_cmd() throws CivException {
		validEcon();
		
		if (args.length < 3) {
			throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
		}
		
		Resident resident = getNamedResident(1);

		try {
			
			Double amount = Double.valueOf(args[2]);
			resident.getTreasury().withdraw(amount);
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_Subtracted",args[2],CivSettings.CURRENCY_NAME,args[1]));
			
		} catch (NumberFormatException e) {
			throw new CivException(args[2]+" "+CivSettings.localize.localizedString("cmd_enterNumerError"));
		}
	}
	
	public void addtown_cmd() throws CivException {
		validEcon();
		
		if (args.length < 3) {
			throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
		}
		
		Town town = getNamedTown(1);
		
		try {
			
			Double amount = Double.valueOf(args[2]);
			town.getTreasury().deposit(amount);
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_added",args[2],CivSettings.CURRENCY_NAME,args[1]));
			
		} catch (NumberFormatException e) {
			throw new CivException(args[2]+" "+CivSettings.localize.localizedString("cmd_enterNumerError"));
		}
	}
	
	public void settown_cmd() throws CivException {
		validEcon();
		
		if (args.length < 3) {
			throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
		}
		
		Town town = getNamedTown(1);

		try {
			
			Double amount = Double.valueOf(args[2]);
			town.getTreasury().setBalance(amount);
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_set",args[1],args[2],CivSettings.CURRENCY_NAME));
			
		} catch (NumberFormatException e) {
			throw new CivException(args[2]+" "+CivSettings.localize.localizedString("cmd_enterNumerError"));
		}
	}
	
	public void subtown_cmd() throws CivException {
		validEcon();
		
		if (args.length < 3) {
			throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
		}
		
		Town town = getNamedTown(1);

		try {
			
			Double amount = Double.valueOf(args[2]);
			town.getTreasury().withdraw(amount);
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_Subtracted",args[2],CivSettings.CURRENCY_NAME,args[1]));
			
		} catch (NumberFormatException e) {
			throw new CivException(args[2]+" "+CivSettings.localize.localizedString("cmd_enterNumerError"));
		}
	}

	public void addciv_cmd() throws CivException {
		validEcon();
		
		if (args.length < 3) {
			throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
		}
		
		Civilization civ = getNamedCiv(1);
		
		try {
			
			Double amount = Double.valueOf(args[2]);
			civ.getTreasury().deposit(amount);
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_added",args[2],CivSettings.CURRENCY_NAME,args[1]));
			
		} catch (NumberFormatException e) {
			throw new CivException(args[2]+" "+CivSettings.localize.localizedString("cmd_enterNumerError"));
		}
	}
	
	public void setciv_cmd() throws CivException {
		validEcon();
		
		if (args.length < 3) {
			throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
		}
		
		Civilization civ = getNamedCiv(1);
		
		try {
			
			Double amount = Double.valueOf(args[2]);
			civ.getTreasury().setBalance(amount);
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_set",args[1],args[2],CivSettings.CURRENCY_NAME));
			
		} catch (NumberFormatException e) {
			throw new CivException(args[2]+" "+CivSettings.localize.localizedString("cmd_enterNumerError"));
		}
	}
	
	public void subciv_cmd() throws CivException {
		validEcon();
		
		if (args.length < 3) {
			throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
		}
		
		Civilization civ = getNamedCiv(1);
		
		try {
			
			Double amount = Double.valueOf(args[2]);
			civ.getTreasury().withdraw(amount);
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_Subtracted",args[2],CivSettings.CURRENCY_NAME,args[1]));
			
		} catch (NumberFormatException e) {
			throw new CivException(args[2]+" "+CivSettings.localize.localizedString("cmd_enterNumerError"));
		}
	}
	
	@Override
	public void doDefaultAction() throws CivException {
		Player player = getPlayer();
		Resident resident = CivGlobal.getResident(player);
		
		if (resident == null) {
			return;
		}
		
		CivMessage.sendSuccess(player, resident.getTreasury().getBalance()+" "+CivSettings.CURRENCY_NAME);
		
	}

	@Override
	public void showHelp() {
		Player player;
		try {
			player = getPlayer();
		} catch (CivException e) {
			e.printStackTrace();
			return;
		}
		
		if (!player.isOp() && !player.hasPermission(CivSettings.ECON)) {
			return;
		}
		
		showBasicHelp();
		
	}

	@Override
	public void permissionCheck() throws CivException {
		
	}

}
