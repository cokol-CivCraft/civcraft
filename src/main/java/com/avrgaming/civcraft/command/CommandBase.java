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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class CommandBase implements TabExecutor {

    private static final int MATCH_LIMIT = 5;

    private final HashMap<String, SubCommandRecord> commands = new HashMap<>();

    protected void register_sub(String literal, SubCommandFunction function, String description) {
        commands.put(literal, new SubCommandRecord(description, function));
    }

    @FunctionalInterface
    public interface SubCommandFunction {
        void run() throws CivException;
    }

    private record SubCommandRecord(String description, SubCommandFunction command) {
    }


    protected String[] args;
    protected CommandSender sender;

    public String command = "FIXME";
    protected String displayName = "FIXME";
    protected boolean sendUnknownToDefault = false;
    protected final DecimalFormat df = new DecimalFormat();

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

            SubCommandRecord comm = commands.get(args[0]);
            if (comm == null) {
                if (!sendUnknownToDefault) {
                    CivMessage.sendError(sender, CivSettings.localize.localizedString("cmd_unknwonMethod") + " " + args[0]);
                }
                try {
                    doDefaultAction();
                } catch (CivException e1) {
                    CivMessage.sendError(sender, e1.getMessage());
                }
                return false;
            }
            try {
                comm.command().run();
            } catch (IllegalArgumentException | CivException e) {
                e.printStackTrace();
                CivMessage.sendError(sender, CivSettings.localize.localizedString("internalCommandException"));
            }
            return true;
        }

        if (sendUnknownToDefault) {
            try {
                doDefaultAction();
            } catch (CivException e) {
                CivMessage.sendError(sender, e.getMessage());
            }
            return false;
        }

        CivMessage.sendError(sender, CivSettings.localize.localizedString("cmd_unknownCommand") + " " + args[0]);
        return false;
    }


    public void doLogging() {
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>(commands.keySet());
    }

    public void showBasicHelp() {
        CivMessage.sendHeading(sender, displayName + " " + CivSettings.localize.localizedString("cmd_CommandHelpTitle"));
        for (String c : commands.keySet()) {
            SubCommandRecord info = commands.get(c);
            if (info.description() != null) {
                String text = info.description().
                        replace("[", ChatColor.YELLOW + "[").
                        replace("]", "]" + ChatColor.GRAY).
                        replace("(", ChatColor.YELLOW + "(").
                        replace(")", ")" + ChatColor.GRAY);

                CivMessage.send(sender, ChatColor.LIGHT_PURPLE + command + " " + c + ChatColor.GRAY + " " + text);
            }
        }
    }

    public Resident getResident() throws CivException {
        Player player = getPlayer();
        Resident res = CivGlobal.getResident(player);
        if (res == null) {
            throw new CivException(CivSettings.localize.localizedString("var_Resident_CouldNotBeFound", player.getName()));
        }
        return res;
    }

    public Player getPlayer() throws CivException {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        throw new CivException(CivSettings.localize.localizedString("cmd_MustBePlayer"));
    }

    public Town getSelectedTown() throws CivException {
        if (senderTownOverride != null) {
            return senderTownOverride;
        }

        if (!(sender instanceof Player player)) {
            throw new CivException(CivSettings.localize.localizedString("cmd_notPartOfTown"));
        }
        Resident res = CivGlobal.getResident(player);
        if (res == null || res.getTown() == null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_notPartOfTown"));
        }
        if (res.getSelectedTown() == null) {
            return res.getTown();
        }
        try {
            res.getSelectedTown().validateResidentSelect(res);
        } catch (CivException e) {
            CivMessage.send(player, ChatColor.YELLOW + CivSettings.localize.localizedString("var_cmd_townDeselectedInvalid", res.getSelectedTown().getName(), res.getTown().getName()));
            res.setSelectedTown(res.getTown());
            return res.getTown();
        }
        return res.getSelectedTown();
    }

    public TownChunk getStandingTownChunk() throws CivException {
        TownChunk tc = CivGlobal.getTownChunk(getPlayer().getLocation());
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
        return combined.toString().trim();
    }

    public void validMayor() throws CivException {
        if (!getSelectedTown().playerIsInGroupName("mayors", getPlayer())) {
            throw new CivException(CivSettings.localize.localizedString("cmd_MustBeMayor"));
        }
    }

    public void validMayorAssistantLeader() throws CivException {
        Resident resident = getResident();
        Town town = getSelectedTown();
        /*
         * If we're using a selected town that isn't ours validate based on the mother civ.
         */

        Civilization civ = town.getMotherCiv() != null ? town.getMotherCiv() : getSenderCiv();
        if (town.getMayorGroup() == null || town.getAssistantGroup() == null || civ.getLeaderGroup() == null) {
            throw new CivException(CivSettings.localize.localizedString("var_cmd_townOrCivMissingGroup1", town.getName(), civ.getName()));
        }
        if (!town.getMayorGroup().hasMember(resident) && !town.getAssistantGroup().hasMember(resident) && !civ.getLeaderGroup().hasMember(resident)) {
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
        if (!getSenderCiv().getLeaderGroup().hasMember(getResident())) {
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
        if (args.length < (index + 1)) {
            throw new CivException(CivSettings.localize.localizedString("cmd_enterNumber"));
        }

        try {
            return Double.valueOf(args[index]);
        } catch (NumberFormatException e) {
            throw new CivException(args[index] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
        }

    }

    protected Integer getNamedInteger(int index) throws CivException {
        if (args.length < (index + 1)) {
            throw new CivException(CivSettings.localize.localizedString("cmd_enterNumber"));
        }

        try {
            return Integer.valueOf(args[index]);
        } catch (NumberFormatException e) {
            throw new CivException(args[index] + " " + CivSettings.localize.localizedString("cmd_enterNumerError2"));
        }

    }

    protected Resident getNamedResident(int index) throws CivException {
        if (args.length < (index + 1)) {
            throw new CivException(CivSettings.localize.localizedString("EnterResidentName"));
        }
        String name = args[index].toLowerCase().replace("%", "(\\w*)");
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

        if (potentialMatches.isEmpty()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults"));
        }

        if (potentialMatches.size() != 1) {
            CivMessage.send(sender, String.valueOf(ChatColor.LIGHT_PURPLE) + ChatColor.UNDERLINE + CivSettings.localize.localizedString("cmd_NameMoreThan1"));
            CivMessage.send(sender, " ");
            StringBuilder out = new StringBuilder();
            for (Resident resident : potentialMatches) {
                out.append(resident.getName()).append(", ");
            }

            CivMessage.send(sender, String.valueOf(ChatColor.AQUA) + ChatColor.ITALIC + out);
            throw new CivException(CivSettings.localize.localizedString("cmd_NameMoreThan2"));
        }

        return potentialMatches.get(0);
    }

    protected Civilization getNamedCiv(int index) throws CivException {
        if (args.length < (index + 1)) {
            throw new CivException(CivSettings.localize.localizedString("EnterCivName"));
        }
        String name = args[index].toLowerCase().replace("%", "(\\w*)");
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

        if (potentialMatches.isEmpty()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults") + " '" + args[index] + "'");
        }

        if (potentialMatches.size() != 1) {
            CivMessage.send(sender, String.valueOf(ChatColor.LIGHT_PURPLE) + ChatColor.UNDERLINE + CivSettings.localize.localizedString("cmd_NameMoreThan1"));
            CivMessage.send(sender, " ");
            StringBuilder out = new StringBuilder();
            for (Civilization civ : potentialMatches) {
                out.append(civ.getName()).append(", ");
            }

            CivMessage.send(sender, String.valueOf(ChatColor.AQUA) + ChatColor.ITALIC + out);
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

        if (potentialMatches.isEmpty()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults") + " '" + args[index] + "'");
        }

        if (potentialMatches.size() != 1) {
            CivMessage.send(sender, String.valueOf(ChatColor.LIGHT_PURPLE) + ChatColor.UNDERLINE + CivSettings.localize.localizedString("cmd_NameMoreThan1"));
            CivMessage.send(sender, " ");
            StringBuilder out = new StringBuilder();
            for (Civilization civ : potentialMatches) {
                out.append(civ.getName()).append(", ");
            }

            CivMessage.send(sender, String.valueOf(ChatColor.AQUA) + ChatColor.ITALIC + out);
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
        if (args.length < (index + 1)) {
            throw new CivException(CivSettings.localize.localizedString("EnterTownName"));
        }

        String name = args[index].toLowerCase().replace("%", "(\\w*)");

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

        if (potentialMatches.isEmpty()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults"));
        }

        if (potentialMatches.size() != 1) {
            CivMessage.send(sender, String.valueOf(ChatColor.LIGHT_PURPLE) + ChatColor.UNDERLINE + CivSettings.localize.localizedString("cmd_NameMoreThan1"));
            CivMessage.send(sender, " ");
            StringBuilder out = new StringBuilder();
            for (Town town : potentialMatches) {
                out.append(town.getName()).append(", ");
            }

            CivMessage.send(sender, String.valueOf(ChatColor.AQUA) + ChatColor.ITALIC + out);
            throw new CivException(CivSettings.localize.localizedString("cmd_NameMoreThan2"));
        }

        return potentialMatches.get(0);
    }

    public String getNamedString(int index, String message) throws CivException {
        if (args.length < (index + 1)) {
            throw new CivException(message);
        }

        return args[index];
    }

    @SuppressWarnings("deprecation")
    protected OfflinePlayer getNamedOfflinePlayer(int index) throws CivException {
        if (args.length < (index + 1)) {
            throw new CivException(CivSettings.localize.localizedString("EnterPlayerName"));
        }

        OfflinePlayer offplayer = Bukkit.getOfflinePlayer(args[index]);
        if (offplayer == null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults") + " " + args[index]);
        }

        return offplayer;
    }

    public String makeInfoString(HashMap<String, String> kvs, ChatColor lowColor, ChatColor highColor) {

        StringBuilder out = new StringBuilder();
        for (String key : kvs.keySet()) {
            out.append(lowColor).append(key).append(": ").append(highColor).append(kvs.get(key)).append(" ");
        }

        return out.toString();
    }

    protected PermissionGroup getNamedPermissionGroup(Town town, int index) throws CivException {
        if (args.length < (index + 1)) {
            throw new CivException(CivSettings.localize.localizedString("EnterGroupName"));
        }

        PermissionGroup grp = CivGlobal.getPermissionGroupFromName(town, args[index]);
        if (grp == null) {
            throw new CivException(CivSettings.localize.localizedString("var_cmd_NameNoResults", args[index], town.getName()));
        }

        return grp;
    }

    protected void validCampOwner() throws CivException {
        Resident resident = getResident();

        if (!resident.hasCamp()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotInCamp"));
        }

        if (resident.getCamp().getOwner() != resident) {
            throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotOwner") + " (" + resident.getCamp().getOwnerName() + ")");
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
        if (args.length < (index + 1)) {
            throw new CivException(CivSettings.localize.localizedString("EnterCampName"));
        }
        String name = args[index].toLowerCase().replace("%", "(\\w*)");
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

        if (potentialMatches.isEmpty()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults"));
        }


        if (potentialMatches.size() != 1) {
            CivMessage.send(sender, String.valueOf(ChatColor.LIGHT_PURPLE) + ChatColor.UNDERLINE + CivSettings.localize.localizedString("cmd_NameMoreThan1"));
            CivMessage.send(sender, " ");
            StringBuilder out = new StringBuilder();
            for (Camp camp : potentialMatches) {
                out.append(camp.getName()).append(", ");
            }

            CivMessage.send(sender, String.valueOf(ChatColor.AQUA) + ChatColor.ITALIC + out);
            throw new CivException(CivSettings.localize.localizedString("cmd_NameMoreThan2"));
        }

        return potentialMatches.get(0);
    }

    protected ArenaTeam getNamedTeam(int index) throws CivException {
        if (args.length < (index + 1)) {
            throw new CivException(CivSettings.localize.localizedString("EnterTeamName"));
        }
        String name = args[index].toLowerCase().replace("%", "(\\w*)");
        ArrayList<ArenaTeam> potentialMatches = new ArrayList<>();
        for (ArenaTeam team : ArenaTeam.arenaTeams.values()) {
            if (team.getName().toLowerCase().matches(name)) {
                potentialMatches.add(team);
            }
            if (potentialMatches.size() > MATCH_LIMIT) {
                throw new CivException(CivSettings.localize.localizedString("cmd_TooManyResults"));
            }
        }

        if (potentialMatches.isEmpty()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults"));
        }

        if (potentialMatches.size() != 1) {
            CivMessage.send(sender, String.valueOf(ChatColor.LIGHT_PURPLE) + ChatColor.UNDERLINE + CivSettings.localize.localizedString("cmd_NameMoreThan1"));
            CivMessage.send(sender, " ");
            StringBuilder out = new StringBuilder();
            for (ArenaTeam team : potentialMatches) {
                out.append(team.getName()).append(", ");
            }

            CivMessage.send(sender, String.valueOf(ChatColor.AQUA) + ChatColor.ITALIC + out);
            throw new CivException(CivSettings.localize.localizedString("cmd_NameMoreThan2"));
        }

        return potentialMatches.get(0);
    }

}
