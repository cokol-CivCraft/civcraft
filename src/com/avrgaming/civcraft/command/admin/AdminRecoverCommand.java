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

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.recover.RecoverStructuresAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;

public class AdminRecoverCommand extends CommandBase {

	@Override
	public void init() {
		command = "/ad recover";
		displayName = CivSettings.localize.localizedString("adcmd_recover_Name");
		
		commands.put("structures", CivSettings.localize.localizedString("adcmd_recover_structuresDesc"));
		commands.put("listbroken", CivSettings.localize.localizedString("adcmd_recover_listBrokenDesc"));
		
		commands.put("listorphantowns", CivSettings.localize.localizedString("adcmd_recover_listOrphanTownDesc"));
		commands.put("listorphancivs", CivSettings.localize.localizedString("adcmd_recover_listOrphanCivsDesc"));
		
		commands.put("listorphanleaders", CivSettings.localize.localizedString("adcmd_recover_listOrphanLeadersDesc"));
		commands.put("fixleaders", CivSettings.localize.localizedString("adcmd_recover_fixLeadersDesc"));
		
		commands.put("listorphanmayors", CivSettings.localize.localizedString("adcmd_recover_listOrphanMayorsDesc"));
		commands.put("fixmayors", CivSettings.localize.localizedString("admcd_recover_fixmayorsDesc"));
		
		commands.put("forcesaveresidents", CivSettings.localize.localizedString("adcmd_recover_forceSaveResDesc"));
		commands.put("forcesavetowns", CivSettings.localize.localizedString("adcmd_recover_forceSaveTownsDesc"));
		commands.put("forcesavecivs", CivSettings.localize.localizedString("adcmd_recover_forceSaveCivsDesc"));
		
		commands.put("listdefunctcivs", CivSettings.localize.localizedString("adcmd_recover_listDefunctCivsDesc"));
		commands.put("killdefunctcivs", CivSettings.localize.localizedString("admcd_recover_killDefunctCivsDesc"));
		
		commands.put("listdefuncttowns", CivSettings.localize.localizedString("adcmd_recover_listDefunctTownsDesc"));
		commands.put("killdefuncttowns", CivSettings.localize.localizedString("adcmd_recover_killdefunctTownsDesc"));
		
		commands.put("listnocaptials", CivSettings.localize.localizedString("adcmd_recover_listNoCapitolsDesc"));
		commands.put("cleannocapitols", CivSettings.localize.localizedString("adcmd_recover_cleanNoCapitolsDesc"));
		
		commands.put("fixtownresidents", CivSettings.localize.localizedString("adcmd_recover_fixTownResidenstDesc"));

	}
	
	public void fixtownresidents_cmd() {
		for (Resident resident : CivGlobal.getResidents()) {
			if (resident.debugTown != null && !resident.debugTown.equals("")) {
				Town town = CivGlobal.getTown(resident.debugTown);
				if (town == null) {
					CivLog.error( CivSettings.localize.localizedString("var_admcd_recover_FixTownError1",resident.debugTown,resident.getName()));
					continue;
				}
				
				resident.setTown(town);
				try {
					resident.saveNow();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	public void listnocapitols_cmd() {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_recover_ListNoCapitolHeading"));
		for (Civilization civ : CivGlobal.getCivs()) {
			
			Town town = CivGlobal.getTown(civ.getCapitolName());
			if (town == null) {
				CivMessage.send(sender, civ.getName());
			}
		}
	}
	
	public void cleannocapitols_cmd() {
		for (Civilization civ : CivGlobal.getCivs()) {
			
			Town town = CivGlobal.getTown(civ.getCapitolName());
			if (town == null) {
				CivMessage.send(sender, CivSettings.localize.localizedString("Deleting")+" "+civ.getName());
				try {
					civ.delete();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void listdefunctcivs_cmd() {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_recover_ListNoCapitolHeading"));
		for (Civilization civ : CivGlobal.getCivs()) {
			if (civ.getLeaderGroup() == null) {
				CivMessage.send(sender, civ.getName());
			}
		}
	}
	
	public void killdefunctcivs_cmd() {
		for (Civilization civ : CivGlobal.getCivs()) {
			if (civ.getLeaderGroup() == null) {
				CivMessage.send(sender, CivSettings.localize.localizedString("Deleting")+" "+civ.getName());
				try {
					civ.delete();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void listdefuncttowns_cmd() {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_recover_listDefunctTownsHeading"));
		for (Town town : CivGlobal.getTowns()) {
			if (town.getMayorGroup() == null) {
				CivMessage.send(sender, town.getName());
			}
		}
	}
	
	public void killdefuncttowns_cmd() {
		for (Town town : CivGlobal.getTowns()) {
			if (town.getMayorGroup() == null) {
				CivMessage.send(sender, CivSettings.localize.localizedString("Deleting")+" "+town.getName());
				try {
					town.delete();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				
			}
		}
	}
	
	public void forcesaveresidents_cmd() throws SQLException {
		for (Resident resident : CivGlobal.getResidents()) {
			resident.saveNow();
		}
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_recover_forcesaveResSuccss",CivGlobal.getResidents().size()));
	}
	
	public void forcesavetowns_cmd() throws SQLException {
		for (Town town : CivGlobal.getTowns()) {
			town.saveNow();
		}
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_recover_forceSaveTownsSuccess",CivGlobal.getTowns().size()));
	}
	
	public void forcesavecivs_cmd() throws SQLException {
		for (Civilization civ : CivGlobal.getCivs()) {
			civ.saveNow();
		}
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_recover_forceSaveCivsSuccess",CivGlobal.getCivs().size()));
	}
	
	public void listorphanmayors_cmd() {
		for (Civilization civ : CivGlobal.getCivs()) {
			Town capitol = civ.getTown(civ.getCapitolName());
			if (capitol == null) {
				continue;
			}
			
			Resident leader = civ.getLeader();
			if (leader == null) {
				continue;
			}
			
			CivMessage.send(sender, CivSettings.localize.localizedString("Broken")+" "+leader.getName()+" "+CivSettings.localize.localizedString("inCiv")+" "+civ.getName()+" "+CivSettings.localize.localizedString("inCapitol")+" "+capitol.getName());
			
		}
		
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("Finished"));
	}
	
	public void fixmayors_cmd() {
		
		for (Civilization civ : CivGlobal.getCivs()) {
			Town capitol = civ.getTown(civ.getCapitolName());
			if (capitol == null) {
				continue;
			}
			
			Resident leader = civ.getLeader();
			if (leader == null) {
				continue;
			}
			
			if (capitol.getMayorGroup() == null) {
				CivMessage.send(sender, CivSettings.localize.localizedString("var_adcmd_recover_fixMayorsError",capitol.getName()));
				continue;
			}
			
			capitol.getMayorGroup().addMember(leader);
			try {
				capitol.getMayorGroup().saveNow();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			CivMessage.send(sender, CivSettings.localize.localizedString("Fixed")+" "+leader.getName()+" "+CivSettings.localize.localizedString("inCiv")+" "+civ.getName()+" "+CivSettings.localize.localizedString("inCapitol")+" "+capitol.getName());
			
		}
		
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("Finished"));
		
	}

	public void fixleaders_cmd() {
		
		for (Civilization civ : CivGlobal.getCivs()) {
			Resident res = civ.getLeader();
			if (res == null) {
				continue;
			}
			
			if (!res.hasTown()) {
				Town capitol = civ.getTown(civ.getCapitolName());
				if (capitol == null) {
					CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_recover_fixLeadersNoCap")+" "+civ.getName());
					continue;
				}
				res.setTown(capitol);
				try {
					res.saveNow();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_recover_FixLeaders1")+" "+civ.getName()+" "+CivSettings.localize.localizedString("Leader")+" "+res.getName());
			}
			
			if (!civ.getLeaderGroup().hasMember(res)) {
				civ.getLeaderGroup().addMember(res);
				try {
					civ.getLeaderGroup().saveNow();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			
		}
	}
	
	public void listorphanleaders_cmd() {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_recover_listOrphanLeadersHeading"));
		
		for (Civilization civ : CivGlobal.getCivs()) {
			Resident res = civ.getLeader();
			if (res == null) {
				continue;
			}
			
			if (!res.hasTown()) {
				Town capitol = civ.getTown(civ.getCapitolName());
				if (capitol == null) {
					CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_recover_fixLeadersNoCap")+" "+civ.getName());
					continue;
				}
				
				CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_recover_listOrphanLeadersBroken")+civ.getName()+" "+CivSettings.localize.localizedString("Leader")+" "+res.getName());
			}
			
		}
		
	}
	
	public void listorphantowns_cmd() {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_recover_listOrphanTownsHeading"));
		
		for (Town town : CivGlobal.orphanTowns) {
			CivMessage.send(sender, town.getName());
		}
	}
	
	public void listorphancivs_cmd() {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_recover_listOrphanCivsHeading"));
		
		for (Civilization civ : CivGlobal.orphanCivs) {
			CivMessage.send(sender, civ.getName()+ " capitol:"+civ.getCapitolName());
		}
		
	}
	
	public void listbroken_cmd() {
		CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_recover_listbrokenStart"));
		TaskMaster.syncTask(new RecoverStructuresAsyncTask(sender, true), 0);
	}
	
	public void structures_cmd() {
		CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_recover_structuresStart"));
		TaskMaster.syncTask(new RecoverStructuresAsyncTask(sender, false), 0);
		
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
		//Permissions checked in /ad command above.
	}

	
	
}
