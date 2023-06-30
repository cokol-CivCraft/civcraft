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
package com.avrgaming.civcraft.command.town;

import java.sql.SQLException;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.util.CivColor;

public class TownGroupCommand extends CommandBase {

	@Override
	public void init() {
		command = "/town group";
		displayName = CivSettings.localize.localizedString("cmd_town_group_name");
		
		commands.put("new", CivSettings.localize.localizedString("cmd_town_group_newDesc"));
		commands.put("delete", CivSettings.localize.localizedString("cmd_town_group_deleteDesc"));
		commands.put("remove", CivSettings.localize.localizedString("cmd_town_group_removeDesc"));
		commands.put("add", CivSettings.localize.localizedString("cmd_town_group_addDesc"));
		commands.put("info", CivSettings.localize.localizedString("cmd_town_group_infoDesc"));
	}
	
	public void delete_cmd() throws CivException {
		Town town = getSelectedTown();
		PermissionGroup grp = this.getNamedPermissionGroup(town, 1);
		
		try {			
			if (grp.getMemberCount() > 0) {
				throw new CivException(CivSettings.localize.localizedString("cmd_town_group_deleteNotEmpty"));
			}
			
			if (town.isProtectedGroup(grp)) {
				throw new CivException(CivSettings.localize.localizedString("cmd_town_group_deleteProtected"));
			}
			
			town.removeGroup(grp);
			town.save();
			grp.delete();
			
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CivException(CivSettings.localize.localizedString("internalDatabaseException"));
		}

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_town_group_deleteSuccess")+" "+args[1]);
	}
	
	public void new_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException(CivSettings.localize.localizedString("cmd_town_group_newPrompt"));
		}
		
		Town town = getSelectedTown();	
		if (town.hasGroupNamed(args[1])) {
			throw new CivException(CivSettings.localize.localizedString("cmd_town_group_newExists")+" "+args[1]);
		}
		
		if (PermissionGroup.isProtectedGroupName(args[1])) {
			throw new CivException(CivSettings.localize.localizedString("cmd_town_group_newProtected"));
		}
		
		try {
			PermissionGroup grp = new PermissionGroup(town, args[1]);
			
			grp.save();
			town.addGroup(grp);
			town.save();
			
		} catch (InvalidNameException e) {
			throw new CivException(CivSettings.localize.localizedString("cmd_town_group_newInvalidName"));
		}

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_group_newSuccess",args[1]));
	}
	
	public void remove_cmd() throws CivException {
		Town town = getSelectedTown();
		Resident commandSenderResidnet = getResident();
		Resident oldMember = getNamedResident(1);
		PermissionGroup grp = getNamedPermissionGroup(town, 2);
				
		if (grp == town.getMayorGroup()) {
			if(!grp.hasMember(commandSenderResidnet)) {
				throw new CivException(CivSettings.localize.localizedString("cmd_town_group_removeOnlyMayor"));
			} 
			
			if (grp.getMemberCount() == 1) {
				throw new CivException(CivSettings.localize.localizedString("cmd_town_group_removeOneMayor"));
			}
		}
		
		grp.removeMember(oldMember);
		grp.save();
		
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_group_removeSuccess1",oldMember.getName(),grp.getName(),town.getName()));
		
		try {
			Player newPlayer = CivGlobal.getPlayer(oldMember);
			CivMessage.send(newPlayer, CivColor.Rose+CivSettings.localize.localizedString("var_cmd_town_group_removeAlert",grp.getName(),grp.getTown().getName()));
		} catch (CivException e) {
			/* player not online. forget the exception*/
		}
	}
	
	public void add_cmd() throws CivException {
		Town town = getSelectedTown();
		Resident commandSenderResident = getResident();
		Resident newMember = getNamedResident(1);
		PermissionGroup grp = this.getNamedPermissionGroup(town, 2);
								
		if (grp == town.getMayorGroup() && !grp.hasMember(commandSenderResident)) {
			
			PermissionGroup leaderGrp = town.getCiv().getLeaderGroup();
			if (leaderGrp == null) {
				throw new CivException(CivSettings.localize.localizedString("cmd_town_group_addOddError")+" "+town.getCiv());
			}
			
			if (!leaderGrp.hasMember(commandSenderResident)) {
				throw new CivException(CivSettings.localize.localizedString("cmd_town_group_addOnlyMayor"));
			}
		}
		
		if (grp.isProtectedGroup() && !newMember.hasTown()) {
			throw new CivException(CivSettings.localize.localizedString("var_cmd_town_group_addNotInTown",newMember.getName()));
		}
		
		if (grp.isTownProtectedGroup() && newMember.getTown() != grp.getTown()) {
			throw new CivException(CivSettings.localize.localizedString("var_cmd_town_group_addError1",newMember.getName(),newMember.getTown().getName(),grp.getTown().getName()));
		}
		
		if (grp.isCivProtectedGroup() && newMember.getCiv() != grp.getCiv()) {
			throw new CivException(CivSettings.localize.localizedString("var_cmd_town_group_addError3",newMember.getName(),newMember.getCiv().getName(),grp.getCiv().getName()));
		}
		
		grp.addMember(newMember);
		grp.save();
		
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_group_addSuccess1",newMember.getName(),grp.getName(),town.getName()));

		try {
			Player newPlayer = CivGlobal.getPlayer(newMember);
			CivMessage.sendSuccess(newPlayer, CivSettings.localize.localizedString("var_cmd_town_group_addAlert",grp.getName(),grp.getTown().getName()));
		} catch (CivException e) {
			/* player not online. forget the exception*/
		}
	}
	
	public void info_cmd() throws CivException {
		Town town = getSelectedTown();
		
		if (args.length >= 2) {
			PermissionGroup grp = town.getGroupByName(args[1]);
			if (grp == null) {
				throw new CivException(CivSettings.localize.localizedString("var_cmd_town_group_infoInvalid",town.getName(),args[1]));
			}
			
			CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_group_infoHeading")+"("+town.getName()+"):"+args[1]);
			
			String residents = "";
			for (Resident res : grp.getMemberList()) {
				residents += res.getName() + " ";
			}
			CivMessage.send(sender, residents);
			
		} else {
			CivMessage.sendHeading(sender, town.getName()+" "+CivSettings.localize.localizedString("cmd_town_group_infoHeading2"));

			for (PermissionGroup grp : town.getGroups()) {
				CivMessage.send(sender, CivSettings.localize.localizedString("var_cmd_civ_group_listGroup",grp.getName()+CivColor.LightGray,grp.getMemberCount()));
			}		
		}
	}
	
	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() throws CivException {
		this.validMayorAssistantLeader();
		return;
	}

	@Override
	public void doDefaultAction() {
		showHelp();
	}

}
