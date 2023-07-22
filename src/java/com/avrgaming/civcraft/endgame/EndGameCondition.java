package com.avrgaming.civcraft.endgame;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.util.CivColor;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class EndGameCondition {

    public static ArrayList<EndGameCondition> endConditions = new ArrayList<>();

    private String id;
    private String victoryName;
    public HashMap<String, String> attributes = new HashMap<>();

    public EndGameCondition() {
    }

    public static void init() {
        for (ConfigEndCondition configEnd : CivSettings.endConditions.values()) {
            String className = "com.avrgaming.civcraft.endgame." + configEnd.className;
            Class<?> someClass;

            try {
                someClass = Class.forName(className);
                EndGameCondition endCompClass;
                endCompClass = (EndGameCondition) someClass.newInstance();
                endCompClass.setId(configEnd.id);
                endCompClass.setVictoryName(configEnd.victoryName);
                endCompClass.attributes = configEnd.attributes;

                endCompClass.onLoad();
                endConditions.add(endCompClass);
            } catch (InstantiationException | ClassNotFoundException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /* Called on start up to load any data in. */
    public abstract void onLoad();

    /* Returns true if the civilization given has met an end-game condition. */
    public abstract boolean check(Civilization civ);

    public abstract String getSessionKey();

    public void onVictoryReset(Civilization civ) {
    }

    /* Do one last check to see if it's ok to win.
     * Science and diplomatic victories require you to have the most
     * beakers/votes so this is needed.
     *
     * Returns true if its ok to win.
     */
    public boolean finalWinCheck(Civilization civ) {
        return true;
    }

    public void onSuccess(Civilization civ) {
        this.checkForWin(civ);
    }

    public void onFailure(Civilization civ) {
        for (SessionEntry entry : CivGlobal.getSessionDB().lookup(getSessionKey())) {
            if (civ == EndGameCondition.getCivFromSessionData(entry.value)) {
                CivMessage.global(CivSettings.localize.localizedString("var_end_warLoss", CivColor.LightBlue + CivColor.BOLD + civ.getName() + CivColor.White, CivColor.LightPurple + CivColor.BOLD + this.victoryName + CivColor.White));
                CivGlobal.getSessionDB().delete(entry.request_id, entry.key);
                onVictoryReset(civ);
                return;
            }
        }

        CivLog.error("Couldn't find civilization:" + civ.getName() + " with id:" + civ.getId() + " to fail end condition:" + this.victoryName);
    }


    public String getString(String key) {
        return attributes.get(key);
    }

    public double getDouble(String key) {
        return Double.parseDouble(attributes.get(key));
    }

    public void setAttribute(String key, String value) {
        attributes.put(key, value);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVictoryName() {
        return victoryName;
    }

    public void setVictoryName(String victoryName) {
        this.victoryName = victoryName;
    }

    /*
     * Returns true if this civ is currently awaiting a 2 week countdown after
     * meeting winning conditions.
     */
    public boolean isActive(Civilization civ) {
        return CivGlobal.getSessionDB().lookup(getSessionKey()).size() != 0;
    }

    public int getDaysLeft(Civilization civ) {
        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getSessionKey());
        if (entries.size() == 0) {
            return -1;
        }

        int daysToHold = getDaysToHold();
        int daysHeld = Integer.parseInt(entries.get(0).value);

        return daysToHold - daysHeld;
    }

    public int getDaysToHold() {
        return Integer.parseInt(this.getString("days_held"));
    }

    public void checkForWin(Civilization civ) {
        /* All win conditions are met, now check for time left. */
        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getSessionKey());

        int daysToHold = getDaysToHold();

        if (entries.size() == 0) {
            /* No entry yet, first time we hit the win condition, save entry. */
            civ.sessionAdd(getSessionKey(), getSessionData(civ, 0));
            civ.winConditionWarning(this, daysToHold);
            return;
        }
        /* Entries exists, check if enough days have passed. */
        for (SessionEntry entry : entries) {
            /* this function only checks non-conquered civs. should be good enough for vic cond. */
            if (EndGameCondition.getCivFromSessionData(entry.value) != civ) {
                continue;
            }

            int daysHeld = this.getDaysHeldFromSessionData(entry.value) + 1;

            if (daysHeld < daysToHold) {
                civ.winConditionWarning(this, daysToHold - daysHeld);
            } else {
                if (this.finalWinCheck(civ)) {
                    civ.declareAsWinner(this);
                }
            }

            CivGlobal.getSessionDB().update(entries.get(0).request_id, entries.get(0).key, getSessionData(civ, daysHeld));
        }


    }

    public String getSessionData(Civilization civ, Integer daysHeld) {
        return civ.getId() + ":" + daysHeld;
    }

    public static Civilization getCivFromSessionData(String data) {
        return CivGlobal.getCivFromId(Integer.parseInt(data.split(":")[0]));
    }

    public Integer getDaysHeldFromSessionData(String data) {
        return Integer.valueOf(data.split(":")[1]);
    }

    public static void onCivilizationWarDefeat(Civilization civ) {
        for (EndGameCondition end : endConditions) {
            end.onWarDefeat(civ);
        }
    }

    public static EndGameCondition getEndCondition(String name) {
        for (EndGameCondition cond : endConditions) {
            if (cond.getId().equals(name)) {
                return cond;
            }
        }
        return null;
    }

    protected abstract void onWarDefeat(Civilization civ);

}
