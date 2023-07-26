package com.avrgaming.civcraft.command.team;

import com.avrgaming.civcraft.arena.Arena;
import com.avrgaming.civcraft.arena.ArenaManager;
import com.avrgaming.civcraft.arena.ArenaTeam;
import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.questions.JoinTeamResponse;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class TeamCommand extends CommandBase {

    @Override
    public void init() {
        command = "/team";
        displayName = CivSettings.localize.localizedString("cmd_team_name");

        register_sub("info", this::info_cmd, CivSettings.localize.localizedString("cmd_team_infoDesc"));
        register_sub("show", this::show_cmd, CivSettings.localize.localizedString("cmd_team_showDesc"));
        register_sub("create", this::create_cmd, CivSettings.localize.localizedString("cmd_team_createDesc"));
        register_sub("leave", this::leave_cmd, CivSettings.localize.localizedString("cmd_team_leaveDesc"));
        register_sub("disband", this::disband_cmd, CivSettings.localize.localizedString("cmd_team_disbandDesc"));
        register_sub("add", this::add_cmd, CivSettings.localize.localizedString("cmd_team_addDesc"));
        register_sub("remove", this::remove_cmd, CivSettings.localize.localizedString("cmd_team_removeDesc"));
        register_sub("changeleader", this::changeleader_cmd, CivSettings.localize.localizedString("cmd_team_changeleaderDesc"));
        register_sub("arena", this::arena_cmd, CivSettings.localize.localizedString("cmd_team_arenaDesc"));
        register_sub("top5", this::top5_cmd, CivSettings.localize.localizedString("cmd_team_top5Desc"));
        register_sub("top10", this::top10_cmd, CivSettings.localize.localizedString("cmd_team_top10Desc"));
        register_sub("list", this::list_cmd, CivSettings.localize.localizedString("cmd_team_listDesc"));
        register_sub("surrender", this::surrender_cmd, CivSettings.localize.localizedString("cmd_team_surrenderDesc"));
    }

    public void surrender_cmd() throws CivException {
        Resident resident = getResident();

        if (!resident.hasTeam()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_team_surrenderNotInTeam"));
        }

        if (!resident.isTeamLeader()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_team_NotLeader"));
        }

        ArenaTeam team = resident.getTeam();
        Arena arena = team.getCurrentArena();

        if (arena == null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_team_surrenderNotInMatch"));
        }

        ArenaTeam otherTeam = null;
        for (ArenaTeam t : arena.getTeams()) {
            if (t != team) {
                otherTeam = t;
                break;
            }
        }

        if (otherTeam == null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_team_surrenderNoOpposition"));
        }

        ArenaManager.declareVictor(arena, team, otherTeam);
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_team_surrenderSuccess"));

    }

    public void arena_cmd() throws CivException {
        Resident resident = getResident();

        if (!resident.hasTeam()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_team_arenaNotInTeam"));
        }

        if (!resident.isTeamLeader()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_team_NotLeader"));
        }

        ArenaTeam team = resident.getTeam();

        if (team.getCurrentArena() != null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_team_arenaInArena"));
        }

        for (ArenaTeam t : ArenaManager.teamQueue) {
            if (t == team) {
                ArenaManager.teamQueue.remove(t);
                CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_team_arenaLeft"));
                return;
            }
        }

        ArenaManager.addTeamToQueue(team);
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_team_arenaAdded"));
    }


    public void list_cmd() {
        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_team_ListHeading"));
        StringBuilder out = new StringBuilder();

        for (ArenaTeam team : ArenaTeam.arenaTeams.values()) {
            out.append(team.getName()).append(", ");
        }

        CivMessage.send(sender, out.toString());
    }


    public void top5_cmd() {
        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_team_top5Heading"));

        for (int i = 0; ((i < 5) && (i < ArenaTeam.teamRankings.size())); i++) {
            ArenaTeam team = ArenaTeam.teamRankings.get(i);
            CivMessage.send(sender, ChatColor.DARK_GREEN + team.getName() + ": " + ChatColor.GREEN + team.getLadderPoints());
        }
    }

    public void top10_cmd() {
        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_team_top10Heading"));

        for (int i = 0; ((i < 10) && (i < ArenaTeam.teamRankings.size())); i++) {
            ArenaTeam team = ArenaTeam.teamRankings.get(i);
            CivMessage.send(sender, ChatColor.DARK_GREEN + team.getName() + ": " + ChatColor.GREEN + team.getLadderPoints());
        }
    }

    public void printTeamInfo(ArenaTeam team) {
        CivMessage.sendHeading(sender, "Team " + team.getName());
        CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("cmd_team_printPoints") + " " + ChatColor.GREEN + team.getLadderPoints() +
                ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("Leader") + " " + ChatColor.GREEN + team.getLeader().getName());
        CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Members") + " " + ChatColor.GREEN + team.getMemberListSaveString());
    }

    public void info_cmd() throws CivException {
        Resident resident = getResident();

        if (!resident.hasTeam()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_team_arenaNotInTeam"));
        }

        ArenaTeam team = resident.getTeam();
        printTeamInfo(team);
    }

    public void show_cmd() throws CivException {
        ArenaTeam team = getNamedTeam(1);
        printTeamInfo(team);
    }

    public void create_cmd() throws CivException {
        String teamName = getNamedString(1, CivSettings.localize.localizedString("cmd_team_createPrompt"));
        Resident resident = getResident();

        if (resident.isProtected()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_team_createProtected"));
        }

        if (resident.hasTeam()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_team_createHasTeam"));
        }


        ArenaTeam.createTeam(teamName, resident);
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_team_createSuccess"));
    }

    public void leave_cmd() throws CivException {
        Resident resident = getResident();

        if (!resident.hasTeam()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_team_arenaNotInTeam"));
        }

        if (resident.isTeamLeader()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_team_leaveIsLeader"));
        }

        ArenaTeam team = resident.getTeam();

        if (team.getCurrentArena() != null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_team_arenaInArena"));
        }

        ArenaTeam.removeMember(team.getName(), resident);
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_team_leaveSuccess", team.getName()));
        CivMessage.sendTeam(team, CivSettings.localize.localizedString("var_cmd_team_leftMessage", resident.getName()));
    }

    public void disband_cmd() throws CivException {
        Resident resident = getResident();

        if (!resident.isTeamLeader()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_team_NotLeader"));
        }

        if (resident.getTeam().getCurrentArena() != null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_team_arenaInArena"));
        }

        String teamName = resident.getTeam().getName();
        ArenaTeam.deleteTeam(teamName);
        ArenaTeam.arenaTeams.remove(teamName);
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_team_disbandSuccess", teamName));
    }

    public void add_cmd() throws CivException {
        Resident resident = getResident();
        Resident member = getNamedResident(1);

        if (!resident.isTeamLeader()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_team_NotLeader"));
        }

        if (member.hasTeam()) {
            throw new CivException(CivSettings.localize.localizedString("var_cmd_team_addHasTeam", member.getName()));
        }

        if (resident.getTeam().getCurrentArena() != null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_team_arenaInArena"));
        }

        try {
            Player player = CivGlobal.getPlayer(member);

            if (member.isProtected()) {
                throw new CivException(CivSettings.localize.localizedString("var_cmd_team_addProtected", player.getName()));
            }

            ArenaTeam team = resident.getTeam();
            JoinTeamResponse join = new JoinTeamResponse();
            join.team = team;
            join.resident = member;
            join.sender = (Player) sender;

            CivGlobal.questionPlayer(CivGlobal.getPlayer(resident), player,
                    CivSettings.localize.localizedString("var_cmd_team_addRequest", team.getName()),
                    30000, join);

        } catch (CivException e) {
            throw new CivException(e.getMessage());
        }

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_team_addInvite", member.getName()));
    }

    public void remove_cmd() throws CivException {
        Resident resident = getResident();
        Resident member = getNamedResident(1);

        if (!resident.isTeamLeader()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_team_NotLeader"));
        }

        if (resident.getTeam().getCurrentArena() != null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_team_arenaInArena"));
        }

        ArenaTeam.removeMember(resident.getTeam().getName(), member);
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_team_removeSuccess", member.getName()));
        CivMessage.sendTeam(resident.getTeam(), CivSettings.localize.localizedString("var_cmd_team_leftMessage", member.getName()));

    }

    public void changeleader_cmd() throws CivException {
        Resident resident = getResident();
        Resident member = getNamedResident(1);

        if (!resident.isTeamLeader()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_team_NotLeader"));
        }

        ArenaTeam team = resident.getTeam();

        if (team.getCurrentArena() != null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_team_arenaInArena"));
        }

        if (!team.hasMember(member)) {
            throw new CivException(CivSettings.localize.localizedString("var_cmd_team_changeleaderNotInTeam", member.getName()));
        }

        team.setLeader(member);
        team.save();

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_team_changeleaderSuccess1", member.getName()));
        CivMessage.sendSuccess(member, CivSettings.localize.localizedString("var_cmd_team_changeleaderSuccess2", team.getName()));
        CivMessage.sendTeam(team, CivSettings.localize.localizedString("var_cmd_team_changeleaderSuccess3", resident.getName(), member.getName()));

    }

    @Override
    public void doDefaultAction() {
        showHelp();

    }

    @Override
    public void showHelp() {
        showBasicHelp();
    }

    @Override
    public void permissionCheck() {

    }


}
