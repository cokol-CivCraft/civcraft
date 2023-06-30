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

import org.bukkit.ChatColor;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.command.civ.CivInfoCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigGovernment;
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.endgame.EndConditionDiplomacy;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Relation;
import com.avrgaming.civcraft.object.Relation.Status;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class AdminCivCommand extends CommandBase {

	@Override
	public void init() {
		command = "/ad civ";
		displayName = CivSettings.localize.localizedString("adcmd_civ_name");
		
		commands.put("disband", CivSettings.localize.localizedString("adcmd_civ_disbandDesc"));
		commands.put("addleader", CivSettings.localize.localizedString("adcmd_civ_addLeaderDesc"));
		commands.put("addadviser", CivSettings.localize.localizedString("adcmd_civ_addAdvisorDesc"));
		commands.put("rmleader", CivSettings.localize.localizedString("adcmd_civ_rmLeaderDesc"));
		commands.put("rmadviser", CivSettings.localize.localizedString("adcmd_civ_rmAdvisorDesc"));
		commands.put("givetech", CivSettings.localize.localizedString("adcmd_civ_giveTechDesc"));
		commands.put("beakerrate", CivSettings.localize.localizedString("adcmd_civ_beakerRateDesc"));
		commands.put("toggleadminciv", CivSettings.localize.localizedString("adcmd_civ_toggleadminCivDesc"));
		commands.put("alltech", CivSettings.localize.localizedString("adcmd_civ_alltechDesc"));
		commands.put("setrelation", CivSettings.localize.localizedString("adcmd_civ_setRelationDesc"));
		commands.put("info", CivSettings.localize.localizedString("adcmd_civ_infoDesc"));
		commands.put("merge", CivSettings.localize.localizedString("adcmd_civ_mergeDesc"));
		commands.put("setgov", CivSettings.localize.localizedString("adcmd_civ_setgovDesc"));
		commands.put("bankrupt", CivSettings.localize.localizedString("adcmd_civ_bankruptDesc"));
		commands.put("conquered", CivSettings.localize.localizedString("adcmd_civ_concqueredDesc"));
		commands.put("unconquer", CivSettings.localize.localizedString("adcmd_civ_unconquerDesc"));
		commands.put("liberate", CivSettings.localize.localizedString("adcmd_civ_liberateDesc"));
		commands.put("setvotes", CivSettings.localize.localizedString("adcmd_civ_setvotesDesc"));
		commands.put("rename", CivSettings.localize.localizedString("adcmd_civ_renameDesc"));
	}
	
	public void liberate_cmd() throws CivException {
		Civilization motherCiv = getNamedCiv(1);
		
		/* Liberate the civ. */
		for (Town t : CivGlobal.getTowns()) {
			if (t.getMotherCiv() == motherCiv) {
				t.changeCiv(motherCiv);
				t.setMotherCiv(null);
				t.save();
			}
		}
		
		motherCiv.setConquered(false);
		CivGlobal.removeConqueredCiv(motherCiv);
		CivGlobal.addCiv(motherCiv);
		motherCiv.save();
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_liberateSuccess")+" "+motherCiv.getName());
	}
	
	public void rename_cmd() throws CivException, InvalidNameException {
		Civilization civ = getNamedCiv(1);
		String name = getNamedString(2, CivSettings.localize.localizedString("adcmd_civ_newNamePrompt"));
		
		if (args.length < 3) {
			throw new CivException(CivSettings.localize.localizedString("adcmd_civ_renameUseUnderscores"));
		}
		
		civ.rename(name);
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_renameCivSuccess"));
	}
	
	public void setvotes_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		Integer votes = getNamedInteger(2);
		EndConditionDiplomacy.setVotes(civ, votes);
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_setVotesSuccess",civ.getName(),votes));
	}
	
	public void conquered_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		civ.setConquered(true);
		CivGlobal.removeCiv(civ);
		CivGlobal.addConqueredCiv(civ);
		civ.save();
		
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_conqueredSuccess"));
	}
	
	public void unconquer_cmd() throws CivException {
		String conquerCiv = this.getNamedString(1, "conquered civ");
		
		Civilization civ = CivGlobal.getConqueredCiv(conquerCiv);
		if (civ == null) {
			civ = CivGlobal.getCiv(conquerCiv);
		}
		
		if (civ == null) {
			throw new CivException (CivSettings.localize.localizedString("var_adcmd_civ_NoCivByThatNane",conquerCiv));
		}
		
		civ.setConquered(false);
		CivGlobal.removeConqueredCiv(civ);
		CivGlobal.addCiv(civ);
		civ.save();
		
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_unconquerSuccess"));
	}
	
	
	public void bankrupt_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		
		if (args.length < 3) {
			CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+CivSettings.localize.localizedString("adcmd_civ_bankruptConfirmPrompt"));
			CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_civ_bankruptConfirmCmd"));
		}
		
		civ.getTreasury().setBalance(0);
		
		for (Town town : civ.getTowns()) {
			town.getTreasury().setBalance(0);
			town.save();
			
			for (Resident resident : town.getResidents()) {
				resident.getTreasury().setBalance(0);
				resident.save();
			}
		}
		
		civ.save();
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_bankruptSuccess",civ.getName()));
	}
	
	public void setgov_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		
		if (args.length < 3) {
			throw new CivException(CivSettings.localize.localizedString("adcmd_civ_setgovPrompt"));
		}
		
		ConfigGovernment gov = CivSettings.governments.get(args[2]);
		if (gov == null) {
			throw new CivException(CivSettings.localize.localizedString("adcmd_civ_setGovInvalidGov")+" gov_monarchy, gov_depostism... etc");
		}
		// Remove any anarchy timers
		String key = "changegov_"+civ.getId();
		CivGlobal.getSessionDB().delete_all(key);
		
		civ.setGovernment(gov.id);
		CivMessage.global(CivSettings.localize.localizedString("var_adcmd_civ_setGovSuccessBroadcast",civ.getName(),CivSettings.governments.get(gov.id).displayName));
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_setGovSuccess"));
		
	}
	
	public void merge_cmd() throws CivException {
		Civilization oldciv = getNamedCiv(1);
		Civilization newciv = getNamedCiv(2);
		
		if (oldciv == newciv) {
			throw new CivException(CivSettings.localize.localizedString("adcmd_civ_mergeSameError"));
		}
		
		newciv.mergeInCiv(oldciv);
		CivMessage.global(CivSettings.localize.localizedString("var_adcmd_civ_mergeSuccess",oldciv.getName(),newciv.getName()));
	}
	
	public void info_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		
		CivInfoCommand cmd = new CivInfoCommand();	
		cmd.senderCivOverride = civ;
		cmd.onCommand(sender, null, "info", this.stripArgs(args, 2));	
	}
	
//	public void setmaster_cmd() throws CivException {
//		Civilization vassal = getNamedCiv(1);
//		Civilization master = getNamedCiv(2);
//		
//		if (vassal == master) {
//			throw new CivException("cannot make vassal and master the same");
//		}
//		
//		CivGlobal.setVassalState(master, vassal);
//		CivMessage.sendSuccess(sender, "Vassaled "+vassal.getName()+" to "+master.getName());
//		
//	}
	
	public void setmaster_cmd() {
		
	}
	
	public void setrelation_cmd() throws CivException {
		if (args.length < 4) {
			throw new CivException(CivSettings.localize.localizedString("Usage") +" [civ] [otherCiv] [NEUTRAL|HOSTILE|WAR|PEACE|ALLY]");
		}
		
		Civilization civ = getNamedCiv(1);
		Civilization otherCiv = getNamedCiv(2);
		
		Relation.Status status = Relation.Status.valueOf(args[3].toUpperCase());
		
		CivGlobal.setRelation(civ, otherCiv, status);
		if (status.equals(Status.WAR)) {
			CivGlobal.setAggressor(civ, otherCiv, civ);
			CivGlobal.setAggressor(otherCiv, civ, civ);
		}
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_setrelationSuccess",civ.getName(),otherCiv.getName(),status.name()));
		
	}
	
	public void alltech_cmd() throws CivException {
	
		Civilization civ = getNamedCiv(1);
		
		for (ConfigTech tech : CivSettings.techs.values()) {
			civ.addTech(tech);
		}
		
		civ.save();
		
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_alltechSuccess"));
	}
	
	public void toggleadminciv_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		
		civ.setAdminCiv(!civ.isAdminCiv());
		civ.save();
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_toggleAdminCivSuccess",civ.getName(),civ.isAdminCiv()));
	}
	
	public void beakerrate_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		Double amount = getNamedDouble(2);
		
		civ.setBaseBeakers(amount);
		civ.save();

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_beakerRateSuccess",civ.getName(),amount));
	}
	
	public void givetech_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		
		if (args.length < 3) {
			throw new CivException(CivSettings.localize.localizedString("adcmd_civ_giveTechPrompt"));
		}
		
		ConfigTech tech = CivSettings.techs.get(args[2]);
		if (tech == null) {
			throw new CivException(CivSettings.localize.localizedString("adcmd_civ_giveTechInvalid")+args[2]);
		}
		
		if (civ.hasTechnology(tech.id)) {
			throw new CivException(CivSettings.localize.localizedString("var_adcmd_civ_giveTechAlreadyhas",civ.getName(),tech.id));
		}
		
		civ.addTech(tech);
		civ.save();
		
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_giveTechSuccess",tech.name,civ.getName()));
		
	}
	
	public void rmadviser_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		Resident resident = getNamedResident(2);
		
		if (civ.getAdviserGroup().hasMember(resident)) {
			civ.getAdviserGroup().removeMember(resident);
			civ.save();
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_rmAdvisorSuccess",resident.getName(),civ.getName()));
		} else {
			CivMessage.sendError(sender, CivSettings.localize.localizedString("var_adcmd_civ_rmAdvisorNotInGroup",resident.getName(),civ.getName()));
		}
	}
	
	public void rmleader_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		Resident resident = getNamedResident(2);
		
		if (civ.getLeaderGroup().hasMember(resident)) {
			civ.getLeaderGroup().removeMember(resident);
			civ.save();
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_rmLeaderSuccess",resident.getName(),civ.getName()));
		} else {
			CivMessage.sendError(sender, CivSettings.localize.localizedString("var_adcmd_civ_rmLeaderNotInGroup",resident.getName(),civ.getName()));
		}
	}
	
	public void addadviser_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		Resident resident = getNamedResident(2);
		
		civ.getAdviserGroup().addMember(resident);
		civ.getAdviserGroup().save();
		civ.save();
		
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_addAdvisorSuccess",resident.getName(),civ.getName()));
	}

	public void addleader_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		Resident resident = getNamedResident(2);
		
		civ.getLeaderGroup().addMember(resident);
		civ.getLeaderGroup().save();
		civ.save();
		
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_addLeaderSuccess",resident.getName(),civ.getName()));
	}
	
	public void disband_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		
		CivMessage.sendCiv(civ, CivSettings.localize.localizedString("adcmd_civ_disbandAlert"));
		try {
			civ.delete();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_disbandSuccess"));
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
		//Admin is checked in parent command
	}

}
