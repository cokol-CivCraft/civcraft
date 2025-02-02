package com.avrgaming.civcraft.endgame;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.wonders.Wonder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

public class EndConditionDiplomacy extends EndGameCondition {

    public static int vote_cooldown_hours;

    @Override
    public void onLoad() {
        vote_cooldown_hours = Integer.parseInt(this.getString("vote_cooldown_hours"));
    }

    @Override
    public boolean check(Civilization civ) {

        boolean hasCouncil = false;
        for (Town town : civ.getTowns()) {
            if (town.getMotherCiv() != null) {
                continue;
            }

            for (Wonder wonder : town.getWonders()) {
                if (wonder.isActive()) {
                    if (wonder.getConfigId().equals("w_council_of_eight")) {
                        hasCouncil = true;
                        break;
                    }
                }
            }

            if (hasCouncil) {
                break;
            }
        }

        if (!hasCouncil) {
            return false;
        }

        if (civ.isAdminCiv()) {
            return false;
        }

        return !civ.isConquered();
    }

    @Override
    public String getSessionKey() {
        return "endgame:diplomacy";
    }

    @Override
    protected void onWarDefeat(Civilization civ) {
        for (Town town : civ.getTowns()) {
            if (town.getMotherCiv() != null) {
                continue;
            }

            for (Wonder wonder : town.getWonders()) {
                if (wonder.getConfigId().equals("w_council_of_eight")) {
                    if (wonder.isActive()) {
                        wonder.fancyDestroyStructureBlocks();
                        wonder.getTown().removeWonder(wonder);
                        try {
                            wonder.delete();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                }
            }
        }

        deleteAllVotes(civ);
        this.onFailure(civ);
    }

    @Override
    public void onVictoryReset(Civilization civ) {
        deleteAllVotes(civ);
    }

    public static boolean canPeopleVote() {
        for (Wonder wonder : CivGlobal.getWonders()) {
            if (wonder.isActive() && wonder.getConfigId().equals("w_council_of_eight")) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean finalWinCheck(Civilization civ) {
        Integer votes = getVotesFor(civ);

        for (Civilization otherCiv : Civilization.getCivs()) {
            if (otherCiv == civ) {
                continue;
            }

            Integer otherVotes = getVotesFor(otherCiv);
            if (otherVotes > votes) {
                CivMessage.global(CivSettings.localize.localizedString("var_end_diplomacyError", civ.getName(), otherCiv.getName()));
                return false;
            }
        }

        return true;
    }

    public static String getVoteSessionKey(Civilization civ) {
        return "endgame:diplomacyvote:" + civ.getUUID();
    }

    public static void deleteAllVotes(Civilization civ) {
        CivGlobal.getSessionDB().delete_all(getVoteSessionKey(civ));
    }

    public static void addVote(Civilization civ, Resident resident) {
        /* validate that we can vote. */
        if (!canVoteNow(resident)) {
            return;
        }

        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getVoteSessionKey(civ));
        if (entries.isEmpty()) {
            CivGlobal.getSessionDB().add(getVoteSessionKey(civ), "" + 1, civ.getUUID());
        } else {
            int votes = Integer.parseInt(entries.get(0).value);
            votes++;
            CivGlobal.getSessionDB().update(entries.get(0).request_id, entries.get(0).key, String.valueOf(votes));
        }

        CivMessage.sendSuccess(resident, CivSettings.localize.localizedString("var_end_diplomacyAddVote", civ.getName()));
    }

    public static void setVotes(Civilization civ, Integer votes) {
        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getVoteSessionKey(civ));
        if (entries.isEmpty()) {
            CivGlobal.getSessionDB().add(getVoteSessionKey(civ), String.valueOf(votes), civ.getUUID());
        } else {
            CivGlobal.getSessionDB().update(entries.get(0).request_id, entries.get(0).key, String.valueOf(votes));
        }
    }

    public static Integer getVotesFor(Civilization civ) {
        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getVoteSessionKey(civ));
        if (entries.isEmpty()) {
            return 0;
        }

        return Integer.valueOf(entries.get(0).value);
    }

    private static boolean canVoteNow(Resident resident) {
        String key = "endgame:residentvote:" + resident.getName();
        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(key);
        if (entries.isEmpty()) {
            CivGlobal.getSessionDB().add(key, String.valueOf((new Date()).getTime()));
            return true;
        } else {
            Date then = new Date(Long.parseLong(entries.get(0).value));
            Date now = new Date();
            if (now.getTime() > (then.getTime() + ((long) vote_cooldown_hours * 60 * 60 * 1000))) {
                CivGlobal.getSessionDB().update(entries.get(0).request_id, entries.get(0).key, String.valueOf(now.getTime()));
                return true;
            }
        }

        CivMessage.sendError(resident, CivSettings.localize.localizedString("end_diplomacy24hours"));
        return false;
    }

}
