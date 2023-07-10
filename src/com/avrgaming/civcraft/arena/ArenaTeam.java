package com.avrgaming.civcraft.arena;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.exception.InvalidObjectException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.SQLObject;
import org.bukkit.scoreboard.Team;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

public class ArenaTeam extends SQLObject implements Comparable<ArenaTeam> {

    public LinkedList<Resident> teamMembers = new LinkedList<>();
    private Resident leader;
    private int ladderPoints;
    private Arena currentArena;
    private Team team;
    private String teamColor;
    private Civilization teamCivilization;

    public static HashMap<String, ArenaTeam> arenaTeams = new HashMap<>();
    public static LinkedList<ArenaTeam> teamRankings = new LinkedList<>();

    public ArenaTeam(String name, Resident leader) throws InvalidNameException {
        this.setName(name);
        this.leader = leader;
        this.teamCivilization = leader.getCiv();
        teamMembers.add(leader);
    }

    public ArenaTeam(ResultSet rs) throws SQLException, InvalidNameException, InvalidObjectException, CivException {
        load(rs);
    }

    public static final String TABLE_NAME = "ARENA_TEAMS";

    public static void init() throws SQLException {
        if (!SQL.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME + " (" +
                    "`id` int(11) unsigned NOT NULL auto_increment," +
                    "`name` VARCHAR(64) NOT NULL," +
                    "`leader` mediumtext NULL," +
                    "`ladderPoints` int(11) DEFAULT 0," +
                    "`members` mediumtext NULL," +
                    "UNIQUE KEY (`name`), " +
                    "PRIMARY KEY (`id`)" + ")";

            SQL.makeTable(table_create);
            CivLog.info("Created " + TABLE_NAME + " table");
        } else {
            CivLog.info(TABLE_NAME + " table OK!");
        }
    }

    private void loadMembers(String memberList) {

        String[] members = memberList.split(",");
        for (String uuid : members) {
            Resident resident = CivGlobal.getResidentViaUUID(UUID.fromString(uuid));
            if (resident != null) {
                teamMembers.add(resident);
            }
        }
    }

    public String getMemberListSaveString() {
        StringBuilder out = new StringBuilder();

        for (Resident resident : teamMembers) {
            out.append(resident.getUUIDString()).append(",");
        }

        return out.toString();
    }

    @Override
    public void load(ResultSet rs) throws SQLException, InvalidNameException {
        this.setId(rs.getInt("id"));
        this.setName(rs.getString("name"));

        this.leader = CivGlobal.getResidentViaUUID(UUID.fromString(rs.getString("leader")));
        if (leader == null) {
            CivLog.error("Couldn't load leader for team:" + this.getName() + "(" + this.getId() + ")");
            return;
        }

        this.setLadderPoints(rs.getInt("ladderPoints"));
        loadMembers(rs.getString("members"));
        loadTeamCivilization();

        arenaTeams.put(this.getName(), this);
        teamRankings.add(this);
    }

    @Override
    public void save() {
        SQLUpdate.add(this);
    }

    @Override
    public void saveNow() throws SQLException {
        HashMap<String, Object> hashmap = new HashMap<>();
        hashmap.put("name", this.getName());
        hashmap.put("leader", this.leader.getUUIDString());
        hashmap.put("ladderPoints", this.getLadderPoints());
        hashmap.put("members", this.getMemberListSaveString());

        SQL.updateNamedObject(this, hashmap, TABLE_NAME);
    }

    @Override
    public void delete() throws SQLException {
        SQL.deleteNamedObject(this, TABLE_NAME);
    }

    public int getLadderPoints() {
        return ladderPoints;
    }

    public void setLadderPoints(int ladderPoints) {
        this.ladderPoints = ladderPoints;
    }

    public Resident getLeader() {
        return leader;
    }

    public Civilization getCivilization() {
        return teamCivilization;
    }

    public void loadTeamCivilization() {
        teamCivilization = getLeader().getCiv();
    }

    public void setLeader(Resident leader) {
        this.leader = leader;
    }

    public void addPoints(int points) {
        this.ladderPoints += points;
        Collections.sort(teamRankings);
        Collections.reverse(teamRankings); //Lazy method.
    }

    public void removePoints(int points) {
        this.ladderPoints -= points;
        Collections.sort(teamRankings);
        Collections.reverse(teamRankings); //Lazy method.
    }

    public static void createTeam(String name, Resident leader) throws CivException {
        try {
            if (arenaTeams.containsKey(name)) {
                throw new CivException(CivSettings.localize.localizedString("arena_teamExists"));
            }
            for (ArenaTeam at : arenaTeams.values()) {
                if (at.getCivilization() == leader.getCiv()) {
                    throw new CivException(CivSettings.localize.localizedString("arena_civhaveteam"));
                }
            }

            ArenaTeam team = new ArenaTeam(name, leader);
            team.save();

            arenaTeams.put(team.getName(), team);
            teamRankings.add(team);

            Collections.sort(teamRankings);
            CivMessage.sendSuccess(leader, CivSettings.localize.localizedString("arena_createTeamPrompt") + " " + name);
        } catch (InvalidNameException e) {
            throw new CivException(CivSettings.localize.localizedString("arena_createInvalid", name));
        }
    }

    public static void deleteTeam(String name) {
        ArenaTeam team = arenaTeams.get(name);
        if (team != null) {
            try {
                team.delete();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void addMember(String teamName, Resident member) throws CivException {
        ArenaTeam team = arenaTeams.get(teamName);
        if (team == null) {
            throw new CivException(CivSettings.localize.localizedString("arena_noTeamNamed") + " " + teamName);
        }

        try {
            int max_team_size = CivSettings.getInteger(CivSettings.arenaConfig, "max_team_size");

            if (team.teamMembers.size() >= max_team_size) {
                throw new CivException(CivSettings.localize.localizedString("var_arena_maxPlayers", max_team_size));
            }

        } catch (InvalidConfiguration e) {
            throw new CivException(CivSettings.localize.localizedString("internalException"));
        }

        team.teamMembers.add(member);
        team.save();
    }

    public static void removeMember(String teamName, Resident member) throws CivException {
        ArenaTeam team = arenaTeams.get(teamName);
        if (team == null) {
            throw new CivException(CivSettings.localize.localizedString("arena_noTeamNamed") + " " + teamName);
        }

        if (!team.teamMembers.contains(member)) {
            throw new CivException(CivSettings.localize.localizedString("arena_noteamMember") + " " + member);
        }

        team.teamMembers.remove(member);
        team.save();
    }

    public boolean hasMember(Resident resident) {
        for (Resident r : this.teamMembers) {
            if (r == resident) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInTeam(String teamName, Resident resident) throws CivException {
        ArenaTeam team = arenaTeams.get(teamName);
        if (team == null) {
            throw new CivException(CivSettings.localize.localizedString("arena_noTeamNamed") + " " + teamName);
        }

        return team.hasMember(resident);
    }

    public static ArenaTeam getTeamForResident(Resident resident) {
        for (ArenaTeam team : arenaTeams.values()) {
            if (team.hasMember(resident)) {
                return team;
            }
        }

        return null;
    }


    @Override
    public int compareTo(ArenaTeam otherTeam) {
        if (this.ladderPoints == otherTeam.getLadderPoints()) {
            return 0;
        } else if (this.ladderPoints > otherTeam.ladderPoints) {
            return 1;
        }

        return -1;
    }

    public void setCurrentArena(Arena arena) {
        this.currentArena = arena;
    }

    public Arena getCurrentArena() {
        return currentArena;
    }

    public Team getScoreboardTeam() {
        return team;
    }

    public void setScoreboardTeam(Team team) {
        this.team = team;
    }

    public String getTeamScoreboardName() {
        return getTeamColor() + this.getName();
    }

    public String getTeamColor() {
        return teamColor;
    }

    public void setTeamColor(String teamColor) {
        this.teamColor = teamColor;
    }
}
