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
package com.avrgaming.civcraft.command.camp;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.camp.Camp;
import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.questions.JoinCampResponse;
import com.avrgaming.civcraft.util.CivColor;

public class CampCommand extends CommandBase {
	public static final long INVITE_TIMEOUT = 30000; //30 seconds

	@Override
	public void init() {
		command = "/camp";
		displayName = CivSettings.localize.localizedString("Camp");
		
		commands.put("undo", CivSettings.localize.localizedString("cmd_camp_undoDesc"));
		commands.put("add", CivSettings.localize.localizedString("cmd_camp_addDesc"));
		commands.put("remove", CivSettings.localize.localizedString("cmd_camp_removeDesc"));
		commands.put("leave", CivSettings.localize.localizedString("cmd_camp_leaveDesc"));
		commands.put("setowner", CivSettings.localize.localizedString("cmd_camp_setownerDesc"));
		commands.put("info", CivSettings.localize.localizedString("cmd_camp_infoDesc"));
		commands.put("disband", CivSettings.localize.localizedString("cmd_camp_disbandDesc"));
		commands.put("upgrade", CivSettings.localize.localizedString("cmd_camp_upgradeDesc"));
		commands.put("refresh", CivSettings.localize.localizedString("cmd_camp_refreshDesc"));
		commands.put("location", CivSettings.localize.localizedString("cmd_camp_locationDesc"));
	}
	
	public void location_cmd() throws CivException {
		Resident resident = getResident();
		
		if (!resident.hasCamp()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotInCamp"));
		}
		Camp camp = resident.getCamp();

        if (camp != null) 
        {
                CivMessage.send(sender, "");
                CivMessage.send(sender, CivColor.LightGreen+CivColor.BOLD+CivSettings.localize.localizedString("cmd_camp_locationSuccess")+" "+CivColor.LightPurple+camp.getCorner());
                CivMessage.send(sender, "");
        }
    }
	
	
	public void refresh_cmd() throws CivException {
		Resident resident = getResident();
		
		if (!resident.hasCamp()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotInCamp"));
		}
		
		Camp camp = resident.getCamp();
		if (camp.getOwner() != resident) {
			throw new CivException(CivSettings.localize.localizedString("cmd_camp_refreshNotOwner"));
		}
		
		if (camp.isDestroyed())
		{
			throw new CivException(CivSettings.localize.localizedString("cmd_camp_refreshDestroyed"));
		}
		
		try {
			camp.repairFromTemplate();
		} catch (IOException e) {
		} catch (CivException e) {
			e.printStackTrace();
		}
		camp.reprocessCommandSigns();
		CivMessage.send(sender, CivSettings.localize.localizedString("cmd_camp_refreshSuccess"));
	}
	
	public void upgrade_cmd() {
		CampUpgradeCommand cmd = new CampUpgradeCommand();	
		cmd.onCommand(sender, null, "camp", this.stripArgs(args, 1));
	}
	
	public void info_cmd() throws CivException {
		Camp camp = this.getCurrentCamp();
		SimpleDateFormat sdf = new SimpleDateFormat("M/dd h:mm:ss a z");

		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("var_camp_infoHeading",camp.getName()));
		HashMap<String,String> info = new HashMap<String, String>();
		info.put(CivSettings.localize.localizedString("Owner"), camp.getOwnerName());
		info.put(CivSettings.localize.localizedString("Members"), ""+camp.getMembers().size());
		info.put(CivSettings.localize.localizedString("NextRaid"), ""+sdf.format(camp.getNextRaidDate()));
		CivMessage.send(sender, this.makeInfoString(info, CivColor.Green, CivColor.LightGreen));
		
		info.clear();
		info.put(CivSettings.localize.localizedString("cmd_camp_infoFireLeft"), ""+camp.getFirepoints());
		info.put(CivSettings.localize.localizedString("cmd_camp_infoLonghouseLevel"), ""+camp.getLonghouseLevel()+""+camp.getLonghouseCountString());
		CivMessage.send(sender, this.makeInfoString(info, CivColor.Green, CivColor.LightGreen));

		info.clear();
		info.put(CivSettings.localize.localizedString("Members"), camp.getMembersString());
		CivMessage.send(sender, this.makeInfoString(info, CivColor.Green, CivColor.LightGreen));
	}
	
	public void remove_cmd() throws CivException {
		this.validCampOwner();
		Camp camp = getCurrentCamp();
		Resident resident = getNamedResident(1);
		
		if (!resident.hasCamp() || resident.getCamp() != camp) {
			throw new CivException(CivSettings.localize.localizedString("var_cmd_camp_removeNotInCamp",resident.getName()));
		}
		
		if (resident.getCamp().getOwner() == resident) {
			throw new CivException(CivSettings.localize.localizedString("cmd_camp_removeErrorOwner"));
		}
		
		camp.removeMember(resident);
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_camp_removeSuccess",resident.getName()));
	}
	
	public void add_cmd() throws CivException {
		this.validCampOwner();
		Camp camp = this.getCurrentCamp();
		Resident resident = getNamedResident(1);
		Player player = getPlayer();
		
		if (resident.hasCamp()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_camp_addInCamp"));
		}
		
		if (resident.hasTown()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_camp_addInTown"));
		}
		
		JoinCampResponse join = new JoinCampResponse();
		join.camp = camp;
		join.resident = resident;
		join.sender = player;
		
		CivGlobal.questionPlayer(player, CivGlobal.getPlayer(resident), 
				CivSettings.localize.localizedString("var_cmd_camp_addInvite",player.getName(),camp.getName()),
				INVITE_TIMEOUT, join);
		
		CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_cmd_camp_addSuccess",resident.getName()));
	}
	
	public void setowner_cmd() throws CivException {
		this.validCampOwner();
		Camp camp = getCurrentCamp();
		Resident newLeader = getNamedResident(1);
		
		if (!camp.hasMember(newLeader.getName())) {
			throw new CivException(CivSettings.localize.localizedString("var_cmd_camp_removeNotInCamp",newLeader.getName()));
		}
		
		camp.setOwner(newLeader);
		camp.save();
		
		Player player = CivGlobal.getPlayer(newLeader);
		CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_cmd_camp_setownerMsg",camp.getName()));
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_camp_setownerSuccess",newLeader.getName()));
		
	}
	
	public void leave_cmd() throws CivException {
		Resident resident = getResident();
		
		if (!resident.hasCamp()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotInCamp"));
		}
		
		Camp camp = resident.getCamp();
		if (camp.getOwner() == resident) {
			throw new CivException(CivSettings.localize.localizedString("cmd_camp_leaveOwner"));
		}
		
		camp.removeMember(resident);
		camp.save();
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_camp_leaveSuccess",camp.getName()));
	}
	
	public void new_cmd() throws CivException {

	}
	
	public void disband_cmd() throws CivException {
		Resident resident = getResident();
		this.validCampOwner();
		Camp camp = this.getCurrentCamp();
		
		if (!resident.hasCamp()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotInCamp"));
		}

		camp.disband();
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_camp_disbandSuccess"));
	}
	
	public void undo_cmd() throws CivException {
		Resident resident = getResident();
		
		if (!resident.hasCamp()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotInCamp"));
		}
		
		Camp camp = resident.getCamp();
		if (camp.getOwner() != resident) {
			throw new CivException(CivSettings.localize.localizedString("cmd_camp_undoNotOwner"));
		}
		
		if (!camp.isUndoable()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_camp_undoTooLate"));
		}
		
		LoreCraftableMaterial campMat = LoreCraftableMaterial.getCraftMaterialFromId("mat_found_camp");
		if (campMat == null) {
			throw new CivException(CivSettings.localize.localizedString("cmd_camp_undoError"));
		}
		
		ItemStack newStack = LoreCraftableMaterial.spawn(campMat);
		Player player = CivGlobal.getPlayer(resident);
		HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(newStack);
		for (ItemStack stack : leftovers.values()) {
			player.getWorld().dropItem(player.getLocation(), stack);
			CivMessage.send(player, CivColor.LightGray+CivSettings.localize.localizedString("cmd_camp_undoFullInven"));
		}
		
		camp.undo();
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_camp_undoSuccess"));
		
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
