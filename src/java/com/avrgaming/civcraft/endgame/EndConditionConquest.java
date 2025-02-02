package com.avrgaming.civcraft.endgame;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.wonders.Wonder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class EndConditionConquest extends EndGameCondition {

    int daysAfterStart;
    double percentCaptured;
    double percentCapturedWithWonder;

    Date startDate = null;

    @Override
    public void onLoad() {
        daysAfterStart = Integer.parseInt(this.getString("days_after_start"));
        percentCaptured = Double.parseDouble(this.getString("percent_captured"));
        percentCapturedWithWonder = Double.parseDouble(this.getString("percent_captured_with_wonder"));
        getStartDate();
    }

    private void getStartDate() {
        String key = "endcondition:conquest:startdate";

        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(key);
        if (entries.isEmpty()) {
            /* Start date is now! */
            startDate = new Date();
            CivGlobal.getSessionDB().add(key, String.valueOf(startDate.getTime()));
        } else {
            long time = Long.parseLong(entries.get(0).value);
            startDate = new Date(time);
        }
    }

    private boolean isAfterStartupTime() {
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startDate);

        Calendar now = Calendar.getInstance();

        startCal.add(Calendar.DATE, daysAfterStart);

        return now.after(startCal);
    }

    @Override
    public String getSessionKey() {
        return "endgame:conquer";
    }

    @Override
    public boolean check(Civilization civ) {
        if (!isAfterStartupTime()) {
            return false;
        }

        boolean hasChichenItza = false;
        for (Town town : civ.getTowns()) {
            if (town.getMotherCiv() != null) {
                continue;
            }

            for (Wonder wonder : town.getWonders()) {
                if (wonder.isActive() && wonder.getConfigId().equals("w_chichen_itza")) {
                    hasChichenItza = true;
                    break;
                }
            }

            if (hasChichenItza) {
                break;
            }
        }

        if (!hasChichenItza) {
            if (civ.getPercentageConquered() < percentCaptured) {
                return false;
            }
        } else {
            if (civ.getPercentageConquered() < percentCapturedWithWonder) {
                return false;
            }
        }

        return !civ.isConquered();
    }

    @Override
    protected void onWarDefeat(Civilization civ) {
        this.onFailure(civ);
    }

}
