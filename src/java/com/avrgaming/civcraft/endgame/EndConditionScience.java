package com.avrgaming.civcraft.endgame;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.wonders.Wonder;

import java.util.ArrayList;

public class EndConditionScience extends EndGameCondition {

    String techname;

    @Override
    public void onLoad() {
        techname = this.getString("tech");
    }

    @Override
    public boolean check(Civilization civ) {

        if (!civ.hasTechnology(techname)) {
            return false;
        }

        if (civ.isAdminCiv()) {
            return false;
        }

        for (Town town : civ.getTowns()) {
            if (town.getMotherCiv() != null) {
                continue;
            }

            for (Wonder wonder : town.getWonders()) {
                if (wonder.isActive() && wonder.getConfigId().equals("w_greatlibrary")) {
                    return true;
                }
            }

        }

        return false;
    }

    @Override
    public boolean finalWinCheck(Civilization civ) {
        Civilization rival = getMostAccumulatedBeakers();
        if (rival != civ) {
            CivMessage.global(CivSettings.localize.localizedString("var_end_scienceError", civ.getName(), rival.getName()));
            return false;
        }

        return true;
    }

    public Civilization getMostAccumulatedBeakers() {
        double most = 0;
        Civilization mostCiv = null;

        for (Civilization civ : CivGlobal.getCivs()) {
            double beakers = getExtraBeakersInCiv(civ);
            if (beakers > most) {
                most = beakers;
                mostCiv = civ;
            }
        }

        return mostCiv;
    }

    @Override
    public String getSessionKey() {
        return "endgame:science";
    }

    @Override
    protected void onWarDefeat(Civilization civ) {
        /* remove any extra beakers we might have. */
        CivGlobal.getSessionDB().delete_all(getBeakerSessionKey(civ));
        civ.removeTech(techname);
        CivMessage.sendCiv(civ, CivSettings.localize.localizedString("end_scienceWarDefeat"));

        civ.save();
        this.onFailure(civ);
    }

    public static String getBeakerSessionKey(Civilization civ) {
        return "endgame:sciencebeakers:" + civ.getUUID();
    }

    public double getExtraBeakersInCiv(Civilization civ) {
        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getBeakerSessionKey(civ));
        if (entries.isEmpty()) {
            return 0;
        }
        return Double.parseDouble(entries.get(0).value);
    }

    public void addExtraBeakersToCiv(Civilization civ, double beakers) {
        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getBeakerSessionKey(civ));
        if (entries.isEmpty()) {
            CivGlobal.getSessionDB().add(getBeakerSessionKey(civ), String.valueOf(beakers), civ.getId(), 0, 0);
        } else {
            double current = Double.parseDouble(entries.get(0).value) + beakers;
            CivGlobal.getSessionDB().update(entries.get(0).request_id, entries.get(0).key, String.valueOf(current));
        }
    }

    public static Double getBeakersFor(Civilization civ) {
        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getBeakerSessionKey(civ));
        if (entries.isEmpty()) {
            return 0.0;
        }
        return Double.valueOf(entries.get(0).value);
    }

}
