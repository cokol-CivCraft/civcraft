package com.avrgaming.civcraft.endgame;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import org.bukkit.ChatColor;

import java.util.ArrayList;

public class EndConditionNotificationTask implements Runnable {

    @Override
    public void run() {

        for (EndGameCondition endCond : EndGameCondition.endConditions) {
            ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(endCond.getSessionKey());
            if (entries.size() == 0) {
                continue;
            }

            for (SessionEntry entry : entries) {
                Civilization civ = EndGameCondition.getCivFromSessionData(entry.value);
                if (civ != null) {
                    int daysLeft = endCond.getDaysToHold() - endCond.getDaysHeldFromSessionData(entry.value);
                    if (daysLeft == 0) {
                        CivMessage.global(CivSettings.localize.localizedString("var_cmd_civ_info_victory",
                                String.valueOf(ChatColor.AQUA) + ChatColor.BOLD + civ.getName() + ChatColor.WHITE, String.valueOf(ChatColor.LIGHT_PURPLE) + ChatColor.BOLD + endCond.getVictoryName() + ChatColor.WHITE));
                        break;
                    } else {
                        CivMessage.global(CivSettings.localize.localizedString("var_cmd_civ_info_daysTillVictoryNew",
                                String.valueOf(ChatColor.AQUA) + ChatColor.BOLD + civ.getName() + ChatColor.WHITE, String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + daysLeft + ChatColor.WHITE, String.valueOf(ChatColor.LIGHT_PURPLE) + ChatColor.BOLD + endCond.getVictoryName() + ChatColor.WHITE));
                    }
                }
            }
        }

    }

}
