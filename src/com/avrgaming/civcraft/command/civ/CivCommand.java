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
package com.avrgaming.civcraft.command.civ;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.TimeZone;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.endgame.EndConditionDiplomacy;
import com.avrgaming.civcraft.endgame.EndGameCondition;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Relation.Status;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.TownHall;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.war.War;

public class CivCommand extends CommandBase {

	@Override
	public void init() {
        command = "/civ";
        displayName = CivSettings.localize.localizedString("cmd_civ_name");

        register_sub("townlist", this::townlist_cmd, CivSettings.localize.localizedString("cmd_civ_townlistDesc"));
        register_sub("deposit", this::deposit_cmd, CivSettings.localize.localizedString("cmd_civ_depositDesc"));
        register_sub("withdraw", this::withdraw_cmd, CivSettings.localize.localizedString("cmd_civ_withdrawDesc"));
        register_sub("info", this::info_cmd, CivSettings.localize.localizedString("cmd_civ_infoDesc"));
        register_sub("show", this::show_cmd, CivSettings.localize.localizedString("cmd_civ_showDesc"));
        register_sub("list", this::list_cmd, CivSettings.localize.localizedString("cmd_civ_listDesc"));
        register_sub("research", this::research_cmd, CivSettings.localize.localizedString("cmd_civ_researchDesc"));
        register_sub("gov", this::gov_cmd, CivSettings.localize.localizedString("cmd_civ_govDesc"));
        register_sub("time", this::time_cmd, CivSettings.localize.localizedString("cmd_civ_timeDesc"));
        register_sub("set", this::set_cmd, CivSettings.localize.localizedString("cmd_civ_setDesc"));
        register_sub("group", this::group_cmd, CivSettings.localize.localizedString("cmd_civ_groupDesc"));
        register_sub("dip", this::dip_cmd, CivSettings.localize.localizedString("cmd_civ_dipDesc"));
        register_sub("victory", this::victory_cmd, CivSettings.localize.localizedString("cmd_civ_victoryDesc"));
        register_sub("vote", this::vote_cmd, CivSettings.localize.localizedString("cmd_civ_voteDesc"));
        register_sub("votes", this::votes_cmd, CivSettings.localize.localizedString("cmd_civ_votesDesc"));
        register_sub("top5", this::top5_cmd, CivSettings.localize.localizedString("cmd_civ_top5Desc"));
        register_sub("disbandtown", this::disbandtown_cmd, CivSettings.localize.localizedString("cmd_civ_disbandtownDesc"));
        register_sub("revolution", this::revolution_cmd, CivSettings.localize.localizedString("cmd_civ_revolutionDesc"));
        register_sub("claimleader", this::claimleader_cmd, CivSettings.localize.localizedString("cmd_civ_claimleaderDesc"));
        register_sub("motd", this::motd_cmd, CivSettings.localize.localizedString("cmd_civ_motdDesc"));
        register_sub("location", this::location_cmd, CivSettings.localize.localizedString("cmd_civ_locationDesc"));
        register_sub("v", this::v_cmd, null); // TODO: add some info about commands and shortcuts;
        register_sub("t", this::time_cmd, null);
        register_sub("r", this::research_cmd, null);
        register_sub("l", this::list_cmd, null);
        register_sub("s", this::show_cmd, null);
        register_sub("i", this::info_cmd, null);
        register_sub("w", this::w_cmd, null);
        register_sub("d", this::d_cmd, null);
    }

	public void location_cmd() throws CivException {
		Civilization civ = getSenderCiv();
	    Resident resident = getResident();
	    if (resident.getCiv() == civ) {
    		for (Town town : civ.getTowns())
    		{
    			String name = town.getName();
    			TownHall townhall = town.getTownHall();
	            if (townhall == null) {
	                    CivMessage.send(sender, CivColor.Rose+CivColor.BOLD+name+CivColor.RESET+CivColor.Gray+CivSettings.localize.localizedString("cmd_civ_locationMissingTownHall"));
	            } else {
	                    CivMessage.send(sender, CivColor.Rose+CivColor.BOLD+name+CivColor.LightPurple+" - "+CivSettings.localize.localizedString("cmd_civ_locationSuccess")+" "+townhall.getCorner());
	            }
    		}
	    }
	}


	public void motd_cmd() {
		CivMotdCommand cmd = new CivMotdCommand();
		cmd.onCommand(sender, null, "motd", this.stripArgs(args, 1));
	}


	public void claimleader_cmd() throws CivException {
		Civilization civ = getSenderCiv();
		Resident resident = getResident();

		if (!civ.areLeadersInactive()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_civ_claimleaderStillActive"));
		}

		civ.getLeaderGroup().addMember(resident);
		civ.getLeaderGroup().save();
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_claimLeaderSuccess", civ.getName()));
		CivMessage.sendCiv(civ, CivSettings.localize.localizedString("var_cmd_civ_claimLeaderBroadcast", resident.getName()));
	}


	public void votes_cmd() {

		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_votesHeading"));
		for (Civilization civ : CivGlobal.getCivs()) {
			Integer votes = EndConditionDiplomacy.getVotesFor(civ);
			if (votes != 0) {
				CivMessage.send(sender, CivColor.LightBlue +
						CivColor.BOLD + civ.getName() + CivColor.White + ": " +
						CivColor.LightPurple + CivColor.BOLD + votes + CivColor.White + " " + CivSettings.localize.localizedString("cmd_civ_votes"));
			}
		}
	}


	public void v_cmd() {
		victory_cmd();
	}
	
	public void victory_cmd() {
		Resident r;
		try {
			r = getResident();
		} catch (CivException e) {
			throw new RuntimeException(e);
		}
		CivMessage.sendHeading(r, CivSettings.localize.localizedString("cmd_civ_victoryHeading"));
		boolean anybody = false;

		for (EndGameCondition endCond : EndGameCondition.endConditions) {
			ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(endCond.getSessionKey());
			if (entries.size() == 0) {
				continue;
			}
			
			anybody = true;
			for (SessionEntry entry : entries) {
				Civilization civ = EndGameCondition.getCivFromSessionData(entry.value);
				int daysLeft = endCond.getDaysToHold() - endCond.getDaysHeldFromSessionData(entry.value);
				CivMessage.send(r, CivColor.LightBlue+CivColor.BOLD+civ.getName()+CivColor.White+": "+
				CivSettings.localize.localizedString("var_cmd_civ_victoryDays",(CivColor.Yellow+CivColor.BOLD+daysLeft+CivColor.White),(CivColor.LightPurple+CivColor.BOLD+endCond.getVictoryName()+CivColor.White)));
			}
		}
		
		if (!anybody) {
			CivMessage.send(r, CivColor.LightGray+CivSettings.localize.localizedString("cmd_civ_victoryNoOne"));
		}
		
	}


	public void revolution_cmd() throws CivException {
		Town town = getSelectedTown();

		if (War.isWarTime() || War.isWithinWarDeclareDays()) {
			throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_revolutionErrorWar1",War.getTimeDeclareDays()));
		}

		if (town.getMotherCiv() == null) {
			throw new CivException(CivSettings.localize.localizedString("cmd_civ_revolutionErrorNoMother"));
		}

		Civilization motherCiv = town.getMotherCiv();

		if (!motherCiv.getCapitolName().equals(town.getName())) {
			throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_revolutionErrorNotCapitol",motherCiv.getCapitolName()));
		}


		try {
			int revolution_cooldown = CivSettings.getInteger(CivSettings.civConfig, "civ.revolution_cooldown");

			Calendar cal = Calendar.getInstance();
			Calendar revCal = Calendar.getInstance();

			Date conquered = town.getMotherCiv().getConqueredDate();
			if (conquered == null) {
				throw new CivException(CivSettings.localize.localizedString("cmd_civ_revolutionErrorNoMother"));
			}

			revCal.setTime(town.getMotherCiv().getConqueredDate());
			revCal.add(Calendar.DAY_OF_MONTH, revolution_cooldown);

			if (!cal.after(revCal)) {
				throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_revolutionErrorTooSoon",revolution_cooldown));
			}

		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			throw new CivException(CivSettings.localize.localizedString("internalException"));
		}


		double revolutionFee = motherCiv.getRevolutionFee();

		if (args.length < 2 || !args[1].equalsIgnoreCase("yes")) {
			CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+CivSettings.localize.localizedString("var_cmd_civ_revolutionConfirm1",revolutionFee,CivSettings.CURRENCY_NAME));
			CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+CivSettings.localize.localizedString("cmd_civ_revolutionConfirm2"));
			CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+CivSettings.localize.localizedString("cmd_civ_revolutionConfirm3"));
			CivMessage.send(sender, CivColor.LightGreen+CivSettings.localize.localizedString("cmd_civ_revolutionConfirm4"));
			return;
		}

		if(!town.getTreasury().hasEnough(revolutionFee)) {
			throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_revolutionErrorTooPoor",revolutionFee,CivSettings.CURRENCY_NAME));
		}

		/* Starting a revolution! Give back all of our towns to us. */
		HashSet<String> warCivs = new HashSet<>();
		for (Town t : CivGlobal.getTowns()) {
			if (t.getMotherCiv() == motherCiv) {
				warCivs.add(t.getCiv().getName());
				t.changeCiv(motherCiv);
				t.setMotherCiv(null);
				t.save();
			}
		}

		for (String warCivName : warCivs) {
			Civilization civ = CivGlobal.getCiv(warCivName);
			if (civ != null) {
				CivGlobal.setRelation(civ, motherCiv, Status.WAR);
				/* THEY are the aggressor in a revolution. */
				CivGlobal.setAggressor(civ, motherCiv, civ);
			}
		}

		motherCiv.setConquered(false);
		CivGlobal.removeConqueredCiv(motherCiv);
		CivGlobal.addCiv(motherCiv);
		motherCiv.save();


		town.getTreasury().withdraw(revolutionFee);
		CivMessage.global(CivColor.Yellow+ChatColor.BOLD+CivSettings.localize.localizedString("var_cmd_civ_revolutionSuccess1",motherCiv.getName()));

	}


	public void disbandtown_cmd() throws CivException {
		this.validLeaderAdvisor();
		Town town = this.getNamedTown(1);

		if (town.getMotherCiv() != null) {
			throw new CivException(CivSettings.localize.localizedString("cmd_civ_disbandtownError"));
		}

		if (town.leaderWantsToDisband) {
			town.leaderWantsToDisband = false;
			CivMessage.send(sender, CivSettings.localize.localizedString("cmd_civ_disbandtownErrorLeader"));
			return;
		}

		town.leaderWantsToDisband = true;

		if (town.mayorWantsToDisband) {
			CivMessage.sendCiv(town.getCiv(), CivSettings.localize.localizedString("var_cmd_civ_disbandtownSuccess", town.getName()));
			town.disband();
		}

		CivMessage.send(sender, CivColor.Yellow + CivSettings.localize.localizedString("cmd_civ_disbandtownPrompt"));
	}


	public void top5_cmd() {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_top5Heading"));
//		TreeMap<Integer, Civilization> scores = new TreeMap<Integer, Civilization>();
//		
//		for (Civilization civ : CivGlobal.getCivs()) {
//			if (civ.isAdminCiv()) {
//				continue;
//			}
//			scores.put(civ.getScore(), civ);
//		}
		
		synchronized(CivGlobal.civilizationScores) {
			int i = 1;
			for (Integer score : CivGlobal.civilizationScores.descendingKeySet()) {
				CivMessage.send(sender, i+") "+CivColor.Gold+CivGlobal.civilizationScores.get(score).getName()+CivColor.White+" - "+score);
				i++;
				if (i > 5) {
					break;
				}
			}
		}
		
	}


	public void dip_cmd() {
		CivDiplomacyCommand cmd = new CivDiplomacyCommand();
		cmd.onCommand(sender, null, "dip", this.stripArgs(args, 1));
	}

	public void group_cmd() {
		CivGroupCommand cmd = new CivGroupCommand();
		cmd.onCommand(sender, null, "group", this.stripArgs(args, 1));
	}

	public void set_cmd() {
		CivSetCommand cmd = new CivSetCommand();
		cmd.onCommand(sender, null, "set", this.stripArgs(args, 1));
	}
	
	public void time_cmd() throws CivException {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_timeHeading"));
        Resident resident = getResident();
        ArrayList<String> out = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("M/dd h:mm:ss a z");
		
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone(resident.getTimezone()));
		sdf.setTimeZone(cal.getTimeZone());
		
		
		out.add(CivColor.Green+CivSettings.localize.localizedString("cmd_civ_timeServer")+" "+CivColor.LightGreen+sdf.format(cal.getTime()));
		
		cal.setTime(CivGlobal.getNextUpkeepDate());
		out.add(CivColor.Green+CivSettings.localize.localizedString("cmd_civ_timeUpkeep")+" "+CivColor.LightGreen+sdf.format(cal.getTime()));
		
		cal.setTime(CivGlobal.getNextHourlyTickDate());
		out.add(CivColor.Green+CivSettings.localize.localizedString("cmd_civ_timeHourly")+" "+CivColor.LightGreen+sdf.format(cal.getTime()));
		
		cal.setTime(CivGlobal.getNextRepoTime());
		out.add(CivColor.Green+CivSettings.localize.localizedString("cmd_civ_timeRepo")+" "+CivColor.LightGreen+sdf.format(cal.getTime()));
		
		if (War.isWarTime()) {
			out.add(CivColor.Yellow+CivSettings.localize.localizedString("cmd_civ_timeWarNow"));
			cal.setTime(War.getStart());
			out.add(CivColor.Yellow+CivSettings.localize.localizedString("cmd_civ_timeWarStarted")+" "+CivColor.LightGreen+sdf.format(cal.getTime()));
			
			cal.setTime(War.getEnd());
			out.add(CivColor.Yellow+CivSettings.localize.localizedString("cmd_civ_timeWarEnds")+" "+CivColor.LightGreen+sdf.format(cal.getTime()));
		} else {
			cal.setTime(War.getNextWarTime());
			out.add(CivColor.Green+CivSettings.localize.localizedString("cmd_civ_timeWarNext")+" "+CivColor.LightGreen+sdf.format(cal.getTime()));
		}
		
		Player player = null;
        try {
            player = getPlayer();
        } catch (CivException ignored) {
        }

		if (player == null || player.hasPermission(CivSettings.MINI_ADMIN) || player.isOp()) {
			cal.setTime(CivGlobal.getTodaysSpawnRegenDate());
			out.add(CivColor.LightPurple+CivSettings.localize.localizedString("cmd_civ_timeSpawnRegen")+" "+CivColor.LightGreen+sdf.format(cal.getTime()));
			
			cal.setTime(CivGlobal.getNextRandomEventTime());
			out.add(CivColor.LightPurple+CivSettings.localize.localizedString("cmd_civ_timeRandomEvent")+" "+CivColor.LightGreen+sdf.format(cal.getTime()));
		}
		
		CivMessage.send(sender, out);
	}

	public void gov_cmd() {
		CivGovCommand cmd = new CivGovCommand();
		cmd.onCommand(sender, null, "gov", this.stripArgs(args, 1));
	}
	
	public void research_cmd() {
		CivResearchCommand cmd = new CivResearchCommand();	
		cmd.onCommand(sender, null, "research", this.stripArgs(args, 1));	
	}

	public void list_cmd() throws CivException {
		if (args.length < 2) {
            StringBuilder out = new StringBuilder();
			CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_listHeading"));
			for (Civilization civ : CivGlobal.getCivs()) {
                out.append(civ.getName()).append(", ");
			}

            CivMessage.send(sender, out.toString());
			return;
		}
		
		Civilization civ = getNamedCiv(1);

        StringBuilder out = new StringBuilder();
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("var_cmd_civ_listtowns",args[1]));
		
		for (Town t : civ.getTowns()) {
            out.append(t.getName()).append(", ");
		}

        CivMessage.send(sender, out.toString());
	}

	public void show_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException(CivSettings.localize.localizedString("cmd_civ_showPrompt"));
		}

		Civilization civ = getNamedCiv(1);
		if (sender instanceof Player) {
			CivInfoCommand.show(sender, getResident(), civ);
		} else {
			CivInfoCommand.show(sender, null, civ);
		}
	}
	
	public void deposit_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException(CivSettings.localize.localizedString("cmd_civ_despositPrompt"));
		}
		
		Resident resident = getResident();
		Civilization civ = getSenderCiv();
		
		try {
            double amount = Double.parseDouble(args[1]);
			if (amount < 1) {
				throw new CivException(amount+" "+CivSettings.localize.localizedString("cmd_enterNumerError2"));
			}
			amount = Math.floor(amount);
			
			civ.depositFromResident(resident, Double.valueOf(args[1]));			
			
		} catch (NumberFormatException e) {
			throw new CivException(args[1]+" "+CivSettings.localize.localizedString("cmd_enterNumerError"));
		}

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("Deposited")+args[1]+" "+CivSettings.CURRENCY_NAME);
	}


	public void d_cmd() throws  CivException{
		if (args.length < 2) {
			throw new CivException(CivSettings.localize.localizedString("cmd_civ_despositPrompt"));
		} else {
			deposit_cmd();
		}
	}


	public void loc_cmd() {
		try {
			location_cmd();
		} catch (CivException e) {
			e.printStackTrace();
		}
	}

	public void withdraw_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException(CivSettings.localize.localizedString("cmd_civ_withdrawPrompt"));
		}
		
		Civilization civ = getSenderCiv();
		Resident resident = getResident();
		
		if (!civ.getLeaderGroup().hasMember(resident)) {
			throw new CivException(CivSettings.localize.localizedString("cmd_NeedHigherCivRank2"));
		}
		
		try {
            double amount = Double.parseDouble(args[1]);
			if (amount < 1) {
				throw new CivException(amount+" "+CivSettings.localize.localizedString("cmd_enterNumerError2"));
			}
			amount = Math.floor(amount);

            if (!civ.getTreasury().payTo(resident.getTreasury(), Double.parseDouble(args[1]))) {
                throw new CivException(CivSettings.localize.localizedString("cmd_civ_withdrawTooPoor"));
            }
		} catch (NumberFormatException e) {
			throw new CivException(args[1]+" "+CivSettings.localize.localizedString("cmd_enterNumerError"));
		}
		
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_withdrawSuccess",args[1],CivSettings.CURRENCY_NAME));
	}


	public void w_cmd() throws CivException{
		if (args.length < 2) {
			throw new CivException(CivSettings.localize.localizedString("cmd_civ_withdrawPrompt"));
		} else {
			withdraw_cmd();
		}
	}


	public void townlist_cmd() throws CivException {
		Civilization civ = getSenderCiv();

		CivMessage.sendHeading(sender, civ.getName()+" "+CivSettings.localize.localizedString("cmd_civ_townListHeading"));
		StringBuilder out = new StringBuilder();
		for (Town town : civ.getTowns()) {
			out.append(town.getName()).append(",");
		}
		CivMessage.send(sender, out.toString());
	}

	public void info_cmd() {
		CivInfoCommand cmd = new CivInfoCommand();
		cmd.onCommand(sender, null, "info", this.stripArgs(args, 1));
	}


	public void i_cmd() {
		info_cmd();
	}


	public void vote_cmd() {

		if (args.length < 2) {
			CivMessage.sendError(sender, CivSettings.localize.localizedString("cmd_civ_voteHeading"));
			return;
		}

		if (sender instanceof Player) {
			Player player = (Player) sender;
			Resident resident = CivGlobal.getResident(player);
			
			if (!resident.hasTown()) {
				CivMessage.sendError(sender, CivSettings.localize.localizedString("cmd_civ_voteNotInTown"));
				return;
			}
			
			Civilization civ = CivGlobal.getCiv(args[1]);
			if (civ == null) {
				CivMessage.sendError(sender, CivSettings.localize.localizedString("var_cmd_civ_voteInvalidCiv",args[1]));
				return;
			}
			
			if (!EndConditionDiplomacy.canPeopleVote()) {
				CivMessage.sendError(sender, CivSettings.localize.localizedString("cmd_civ_voteNoCouncil"));
				return;
			}
			
			EndConditionDiplomacy.addVote(civ, resident);
		} else {
		}
	}

    @Override
    public void doDefaultAction() {
        showHelp();
    }

	@Override
	public void showHelp() {
		this.showBasicHelp();
	}

    @Override
    public void permissionCheck() {

    }
	
}
