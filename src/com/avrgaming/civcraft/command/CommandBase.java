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
package com.avrgaming.civcraft.command;

import com.avrgaming.civcraft.arena.ArenaTeam;
import com.avrgaming.civcraft.camp.Camp;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.util.CivColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class CommandBase implements CommandExecutor {
	
	private static final int MATCH_LIMIT = 5;

	protected HashMap<String, String> commands = new HashMap<>();
	
	protected String[] args;
	protected CommandSender sender;
	
	protected String command = "FIXME";
	protected String displayName = "FIXME";
	protected boolean sendUnknownToDefault = false;
	protected DecimalFormat df = new DecimalFormat();

	public Town senderTownOverride = null;
	public Civilization senderCivOverride = null;
	
	public abstract void init();
	
	/* Called when no arguments are passed. */
	public abstract void doDefaultAction() throws CivException;
	
	/* Called on syntax error. */
	public abstract void showHelp();
	
	/* Called before command is executed to check permissions. */
	public abstract void permissionCheck() throws CivException;
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		init();
		
		this.args = args;
		this.sender = sender;
		
		try {
			permissionCheck();
		} catch (CivException e1) {
			CivMessage.sendError(sender, e1.getMessage());
			return false;
		}
		
		doLogging();
		
		if (args.length == 0) {
			try {
				doDefaultAction();
			} catch (CivException e) {
				CivMessage.sendError(sender, e.getMessage());
			}
			return false;
		}
		
		if (args[0].equalsIgnoreCase("help")) {
			showHelp();
			return true;
		}
		
		for (String c : commands.keySet()) {
			if (!c.equalsIgnoreCase(args[0])) {
				continue;
			}
			try {
				Method method = this.getClass().getMethod(args[0].toLowerCase() + "_cmd");
				try {
					method.invoke(this);
					return true;
				} catch (IllegalAccessException | IllegalArgumentException e) {
					e.printStackTrace();
					CivMessage.sendError(sender, CivSettings.localize.localizedString("internalCommandException"));
				} catch (InvocationTargetException e) {
					if (e.getCause() instanceof CivException) {
						CivMessage.sendError(sender, e.getCause().getMessage());
					} else {
						CivMessage.sendError(sender, CivSettings.localize.localizedString("internalCommandException"));
						e.getCause().printStackTrace();
					}
				}
				return true;

			} catch (NoSuchMethodException e) {
				if (!sendUnknownToDefault) {
					CivMessage.sendError(sender, CivSettings.localize.localizedString("cmd_unknwonMethod") + " " + args[0]);
				}
				try {
					doDefaultAction();
				} catch (CivException e1) {
					CivMessage.sendError(sender, e.getMessage());
				}
				return false;
			}
		}
		
		if (sendUnknownToDefault) {
			try {
				doDefaultAction();
			} catch (CivException e) {
				CivMessage.sendError(sender, e.getMessage());
			}
			return false;
		}
		
		CivMessage.sendError(sender, CivSettings.localize.localizedString("cmd_unknownCommand")+" "+args[0]);
		return false;
	}
	
	
	public void doLogging() {	
	}
	
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> al = new ArrayList<>();
		al.add("sub1");
		al.add("barg");
		al.add("borg");
		return al;
	}

	public void showBasicHelp() {
		CivMessage.sendHeading(sender, displayName+" "+CivSettings.localize.localizedString("cmd_CommandHelpTitle"));
		for (String c : commands.keySet()) {
			String info = commands.get(c);
			if (info != null) {

				info = info.replace("[", CivColor.Yellow + "[");
				info = info.replace("]", "]" + CivColor.LightGray);
				info = info.replace("(", CivColor.Yellow + "(");
				info = info.replace(")", ")" + CivColor.LightGray);

				CivMessage.send(sender, CivColor.LightPurple + command + " " + c + CivColor.LightGray + " " + info);
			}
		}
	}
	
	public Resident getResident() throws CivException {
		Player player = getPlayer();
		Resident res = CivGlobal.getResident(player);
		if (res == null) {
			throw new CivException(CivSettings.localize.localizedString("var_Resident_CouldNotBeFound",player.getName()));
		}
		return res;
	}
	
	public Player getPlayer() throws CivException {
		if (sender instanceof Player) {
			return (Player)sender;
		}
		throw new CivException(CivSettings.localize.localizedString("cmd_MustBePlayer"));
	}
	
	public Town getSelectedTown() throws CivException {
		if (senderTownOverride != null) {
			return senderTownOverride;
		}
		
		if (sender instanceof Player) {
			Player player = (Player)sender;
			Resident res = CivGlobal.getResident(player);
			if (res != null && res.getTown() != null) {
				
				if (res.getSelectedTown() != null) {
					try {
						res.getSelectedTown().validateResidentSelect(res);
					} catch (CivException e) {
						CivMessage.send(player, CivColor.Yellow+CivSettings.localize.localizedString("var_cmd_townDeselectedInvalid",res.getSelectedTown().getName(),res.getTown().getName()));
						res.setSelectedTown(res.getTown());
						return res.getTown();
					}
					
					return res.getSelectedTown();
				} else {
					return res.getTown();
				}
			}
		}
		throw new CivException(CivSettings.localize.localizedString("cmd_notPartOfTown"));
	}
	
	public TownChunk getStandingTownChunk() throws CivException {
		Player player = getPlayer();
		
		TownChunk tc = CivGlobal.getTownChunk(player.getLocation());
		if (tc == null) {
			throw new CivException(CivSettings.localize.localizedString("cmd_plotNotOwned"));
		}
		return tc;
	}
	
	protected String[] stripArgs(String[] someArgs, int amount) {
		if (amount >= someArgs.length) {
			return new String[0];
		}
		
		
		String[] argsLeft = new String[someArgs.length - amount];
        System.arraycopy(someArgs, amount, argsLeft, 0, argsLeft.length);
		
		return argsLeft;
	}
	
	protected String combineArgs(String[] someArgs) {
        StringBuilder combined = new StringBuilder();
        for (String str : someArgs) {
            combined.append(str).append(" ");
        }
        combined = new StringBuilder(combined.toString().trim());
        return combined.toString();
    }
	
	public void validMayor() throws CivException {
		Player player = getPlayer();
		Town town = getSelectedTown();
		
		if (!town.playerIsInGroupName("mayors", player)) {
			throw new CivException(CivSettings.localize.localizedString("cmd_MustBeMayor"));
		}
		//if (this.)
	}
	
	public void validMayorAssistantLeader() throws CivException {
		Resident resident = getResident();
		Town town = getSelectedTown();
		Civilization civ;
		
		/* 
		 * If we're using a selected town that isn't ours validate based on the mother civ.
		 */
		if (town.getMotherCiv() != null) {
			civ = town.getMotherCiv();
		} else {
			civ = getSenderCiv();
		}
		
		if (town.getMayorGroup() == null || town.getAssistantGroup() == null || 
				civ.getLeaderGroup() == null) {
			throw new CivException(CivSettings.localize.localizedString("var_cmd_townOrCivMissingGroup1",town.getName(),civ.getName()));
		}
		
		if (!town.getMayorGroup().hasMember(resident) && !town.getAssistantGroup().hasMember(resident) &&
				!civ.getLeaderGroup().hasMember(resident)) {
			throw new CivException(CivSettings.localize.localizedString("cmd_NeedHigherTownOrCivRank"));
		}
	}
	
	public void validLeaderAdvisor() throws CivException {
		Resident res = getResident();
		Civilization civ = getSenderCiv();

		
		if (!civ.getLeaderGroup().hasMember(res) && !civ.getAdviserGroup().hasMember(res)) {
			throw new CivException(CivSettings.localize.localizedString("cmd_NeedHigherCivRank"));
		}
	}
	
	public void validLeader() throws CivException {
		Resident res = getResident();
		Civilization civ = getSenderCiv();
		
		if (!civ.getLeaderGroup().hasMember(res)) {
			throw new CivException(CivSettings.localize.localizedString("cmd_NeedHigherCivRank2"));
		}
	}
	
	public void validPlotOwner() throws CivException {
		Resident resident = getResident();
		TownChunk tc = getStandingTownChunk();
		
		if (tc.perms.getOwner() == null) {
			validMayorAssistantLeader();
			if (tc.getTown() != resident.getTown()) {
				throw new CivException(CivSettings.localize.localizedString("cmd_validPlotOwnerFalse"));
			}
		} else {
			if (resident != tc.perms.getOwner()) {
				throw new CivException(CivSettings.localize.localizedString("cmd_validPlotOwnerFalse2"));
			}
		}	
	}
	
	public Civilization getSenderCiv() throws CivException {
		
		if (this.senderCivOverride != null) {
			return this.senderCivOverride;
		}
		
		Resident resident = getResident();
		
		if (resident.getTown() == null) {
			throw new CivException(CivSettings.localize.localizedString("cmd_getSenderCivNoCiv"));
		}
				
		if (resident.getTown().getCiv() == null) {
			//This should never happen but....
			throw new CivException(CivSettings.localize.localizedString("cmd_getSenderCivNoCiv"));
		}
		
		return resident.getTown().getCiv();
	}

	protected Double getNamedDouble(int index) throws CivException {
		if (args.length < (index+1)) {
			throw new CivException(CivSettings.localize.localizedString("cmd_enterNumber"));
		}
		
		try {
			return Double.valueOf(args[index]);
		} catch (NumberFormatException e) {
			throw new CivException(args[index]+" "+CivSettings.localize.localizedString("cmd_enterNumerError"));
		}
		
	}
	
	protected Integer getNamedInteger(int index) throws CivException {
		if (args.length < (index+1)) {
			throw new CivException(CivSettings.localize.localizedString("cmd_enterNumber"));
		}
		
		try {
			return Integer.valueOf(args[index]);
		} catch (NumberFormatException e) {
			throw new CivException(args[index]+" "+CivSettings.localize.localizedString("cmd_enterNumerError2"));
		}
		
	}
	
	protected Resident getNamedResident(int index) throws CivException {
		if (args.length < (index+1)) {
			throw new CivException(CivSettings.localize.localizedString("EnterResidentName"));
		}
		
		String name = args[index].toLowerCase();
		name = name.replace("%", "(\\w*)");

        ArrayList<Resident> potentialMatches = new ArrayList<>();
		for (Resident resident : CivGlobal.getResidents()) {
			String str = resident.getName().toLowerCase();
			try {
				if (str.matches(name)) {
					potentialMatches.add(resident);
				}
			} catch (Exception e) {
				throw new CivException(CivSettings.localize.localizedString("cmd_invalidPattern"));
			}
			
			if (potentialMatches.size() > MATCH_LIMIT) {
				throw new CivException(CivSettings.localize.localizedString("cmd_TooManyResults"));
			}
		}
		
		if (potentialMatches.size() == 0) {
			throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults"));
		}
		
		if (potentialMatches.size() != 1) {
			CivMessage.send(sender, CivColor.LightPurple+ChatColor.UNDERLINE+CivSettings.localize.localizedString("cmd_NameMoreThan1"));
			CivMessage.send(sender, " ");
            StringBuilder out = new StringBuilder();
			for (Resident resident : potentialMatches) {
                out.append(resident.getName()).append(", ");
			}
		
			CivMessage.send(sender, CivColor.LightBlue+ChatColor.ITALIC+out);
			throw new CivException(CivSettings.localize.localizedString("cmd_NameMoreThan2"));
		}
		
		return potentialMatches.get(0);
	}
	
	protected Civilization getNamedCiv(int index) throws CivException {
		if (args.length < (index+1)) {
			throw new CivException(CivSettings.localize.localizedString("EnterCivName"));
		}
		
		String name = args[index].toLowerCase();
		name = name.replace("%", "(\\w*)");

        ArrayList<Civilization> potentialMatches = new ArrayList<>();
		for (Civilization civ : CivGlobal.getCivs()) {
			String str = civ.getName().toLowerCase();
			try {
				if (str.matches(name)) {
					potentialMatches.add(civ);
				}
			} catch (Exception e) {
				throw new CivException(CivSettings.localize.localizedString("cmd_invalidPattern"));
			}
			
			if (potentialMatches.size() > MATCH_LIMIT) {
				throw new CivException(CivSettings.localize.localizedString("cmd_TooManyResults"));
			}
		}
		
		if (potentialMatches.size() == 0) {
			throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults")+" '"+args[index]+"'");
		}
		
		if (potentialMatches.size() != 1) {
			CivMessage.send(sender, CivColor.LightPurple+ChatColor.UNDERLINE+CivSettings.localize.localizedString("cmd_NameMoreThan1"));
			CivMessage.send(sender, " ");
            StringBuilder out = new StringBuilder();
			for (Civilization civ : potentialMatches) {
                out.append(civ.getName()).append(", ");
			}
		
			CivMessage.send(sender, CivColor.LightBlue+ChatColor.ITALIC+out);
			throw new CivException(CivSettings.localize.localizedString("cmd_NameMoreThan2"));
		}
		
		return potentialMatches.get(0);
	}
	
	protected Civilization getNamedCapturedCiv(int index) throws CivException {
		if (args.length < (index + 1)) {
			throw new CivException(CivSettings.localize.localizedString("EnterCivName"));
		}

		String name = args[index].toLowerCase().replace("%", "(\\w*)");

		ArrayList<Civilization> potentialMatches = new ArrayList<>();
		for (Civilization civ : CivGlobal.getConqueredCivs()) {
			String str = civ.getName().toLowerCase();
			try {
				if (str.matches(name)) {
					potentialMatches.add(civ);
				}
			} catch (Exception e) {
				throw new CivException(CivSettings.localize.localizedString("cmd_invalidPattern"));
			}
			
			if (potentialMatches.size() > MATCH_LIMIT) {
				throw new CivException(CivSettings.localize.localizedString("cmd_TooManyResults"));
			}
		}
		
		if (potentialMatches.size() == 0) {
			throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults")+" '"+args[index]+"'");
		}
		
		if (potentialMatches.size() != 1) {
			CivMessage.send(sender, CivColor.LightPurple+ChatColor.UNDERLINE+CivSettings.localize.localizedString("cmd_NameMoreThan1"));
			CivMessage.send(sender, " ");
            StringBuilder out = new StringBuilder();
			for (Civilization civ : potentialMatches) {
                out.append(civ.getName()).append(", ");
			}
		
			CivMessage.send(sender, CivColor.LightBlue+ChatColor.ITALIC+out);
			throw new CivException(CivSettings.localize.localizedString("cmd_NameMoreThan2"));
		}
		
		return potentialMatches.get(0);
	}
//	protected Town getNamedTown(int index) throws CivException {
//		if (args.length < (index+1)) {
//			throw new CivException("Enter a town name");
//		}
//		
//		Town town = CivGlobal.getTown(args[index]);
//		if (town == null) {
//			throw new CivException("No town named:"+args[index]);
//		}
//		
//		return town;
//	}
	
	protected Town getNamedTown(int index) throws CivException {
		if (args.length < (index+1)) {
			throw new CivException(CivSettings.localize.localizedString("EnterTownName"));
		}
		
		String name = args[index].toLowerCase();
		name = name.replace("%", "(\\w*)");

        ArrayList<Town> potentialMatches = new ArrayList<>();
		for (Town town : CivGlobal.getTowns()) {
			String str = town.getName().toLowerCase();
			try {
				if (str.matches(name)) {
					potentialMatches.add(town);
				}
			} catch (Exception e) {
				throw new CivException(CivSettings.localize.localizedString("cmd_invalidPattern"));
			}
			
			if (potentialMatches.size() > MATCH_LIMIT) {
				throw new CivException(CivSettings.localize.localizedString("cmd_TooManyResults"));
			}
		}
		
		if (potentialMatches.size() == 0) {
			throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults"));
		}
		
		if (potentialMatches.size() != 1) {
			CivMessage.send(sender, CivColor.LightPurple+ChatColor.UNDERLINE+CivSettings.localize.localizedString("cmd_NameMoreThan1"));
			CivMessage.send(sender, " ");
            StringBuilder out = new StringBuilder();
			for (Town town : potentialMatches) {
                out.append(town.getName()).append(", ");
			}
		
			CivMessage.send(sender, CivColor.LightBlue+ChatColor.ITALIC+out);
			throw new CivException(CivSettings.localize.localizedString("cmd_NameMoreThan2"));
		}
		
		return potentialMatches.get(0);
	}
	
	public String getNamedString(int index, String message) throws CivException {
		if (args.length < (index+1)) {
			throw new CivException(message);
		}
		
		return args[index];
	}
	
	@SuppressWarnings("deprecation")
	protected OfflinePlayer getNamedOfflinePlayer(int index) throws CivException {
		if (args.length < (index+1)) {
			throw new CivException(CivSettings.localize.localizedString("EnterPlayerName"));
		}
		
		OfflinePlayer offplayer = Bukkit.getOfflinePlayer(args[index]);
		if (offplayer == null) {
			throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults")+" "+args[index]);
		}
		
		return offplayer;
	}
	
	public String makeInfoString(HashMap<String, String> kvs, String lowColor, String highColor) {

        StringBuilder out = new StringBuilder();
		for (String key : kvs.keySet()) {
            out.append(lowColor).append(key).append(": ").append(highColor).append(kvs.get(key)).append(" ");
		}

        return out.toString();
	}
	
	protected PermissionGroup getNamedPermissionGroup(Town town, int index) throws CivException {
		if (args.length < (index+1)) {
			throw new CivException(CivSettings.localize.localizedString("EnterGroupName"));
		}
		
		PermissionGroup grp = CivGlobal.getPermissionGroupFromName(town, args[index]);
		if (grp == null) {
			throw new CivException(CivSettings.localize.localizedString("var_cmd_NameNoResults",args[index],town.getName()));
		}
		
		return grp;
	}
	
	protected void validCampOwner() throws CivException {
		Resident resident = getResident();
		
		if (!resident.hasCamp()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotInCamp"));
		}
		
		if (resident.getCamp().getOwner() != resident) {
			throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotOwner")+" ("+resident.getCamp().getOwnerName()+")");
		}
	}
	
	protected Camp getCurrentCamp() throws CivException {
		Resident resident = getResident();
		
		if (!resident.hasCamp()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotInCamp"));
		}
		
		return resident.getCamp();
	}
	
	protected Camp getNamedCamp(int index) throws CivException {
		if (args.length < (index+1)) {
			throw new CivException(CivSettings.localize.localizedString("EnterCampName"));
		}
		
		String name = args[index].toLowerCase();
		name = name.replace("%", "(\\w*)");

        ArrayList<Camp> potentialMatches = new ArrayList<>();
		for (Camp camp : CivGlobal.getCamps()) {
			String str = camp.getName().toLowerCase();
			try {
				if (str.matches(name)) {
					potentialMatches.add(camp);
				}
			} catch (Exception e) {
				throw new CivException(CivSettings.localize.localizedString("cmd_invalidPattern"));
			}
			
			if (potentialMatches.size() > MATCH_LIMIT) {
				throw new CivException(CivSettings.localize.localizedString("cmd_TooManyResults"));
			}
		}
		
		if (potentialMatches.size() == 0) {
			throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults"));
		}
		
		
		if (potentialMatches.size() != 1) {
			CivMessage.send(sender, CivColor.LightPurple+ChatColor.UNDERLINE+CivSettings.localize.localizedString("cmd_NameMoreThan1"));
			CivMessage.send(sender, " ");
            StringBuilder out = new StringBuilder();
			for (Camp camp : potentialMatches) {
                out.append(camp.getName()).append(", ");
			}
		
			CivMessage.send(sender, CivColor.LightBlue+ChatColor.ITALIC+out);
			throw new CivException(CivSettings.localize.localizedString("cmd_NameMoreThan2"));
		}
		
		return potentialMatches.get(0);
	}
	
	protected ArenaTeam getNamedTeam(int index) throws CivException {
		if (args.length < (index+1)) {
			throw new CivException(CivSettings.localize.localizedString("EnterTeamName"));
		}
		
		String name = args[index].toLowerCase();
		name = name.replace("%", "(\\w*)");

        ArrayList<ArenaTeam> potentialMatches = new ArrayList<>();
		for (ArenaTeam team : ArenaTeam.arenaTeams.values()) {
			String str = team.getName().toLowerCase();
			try {
				if (str.matches(name)) {
					potentialMatches.add(team);
				}
			} catch (Exception e) {
				throw new CivException(CivSettings.localize.localizedString("cmd_invalidPattern"));
			}
			
			if (potentialMatches.size() > MATCH_LIMIT) {
				throw new CivException(CivSettings.localize.localizedString("cmd_TooManyResults"));
			}
		}
		
		if (potentialMatches.size() == 0) {
			throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults"));
		}
		
		if (potentialMatches.size() != 1) {
			CivMessage.send(sender, CivColor.LightPurple+ChatColor.UNDERLINE+CivSettings.localize.localizedString("cmd_NameMoreThan1"));
			CivMessage.send(sender, " ");
            StringBuilder out = new StringBuilder();
			for (ArenaTeam team : potentialMatches) {
                out.append(team.getName()).append(", ");
			}
		
			CivMessage.send(sender, CivColor.LightBlue+ChatColor.ITALIC+out);
			throw new CivException(CivSettings.localize.localizedString("cmd_NameMoreThan2"));
		}
		
		return potentialMatches.get(0);
	}
	
}
