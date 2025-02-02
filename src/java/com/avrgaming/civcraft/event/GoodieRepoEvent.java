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
package com.avrgaming.civcraft.event;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.TaskMaster;

import javax.annotation.Nonnull;
import java.util.Calendar;

public class GoodieRepoEvent implements EventInterface {

    public static void repoProcess() {
        class SyncTask implements Runnable {
            @Override
            public void run() {

                for (Town town : Town.getTowns()) {
                    for (BonusGoodie goodie : town.getBonusGoodies()) {
                        town.removeGoodie(goodie);
                    }
                }

                for (BonusGoodie goodie : CivGlobal.getBonusGoodies()) {
                    try {
                        goodie.replenish();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        }

        TaskMaster.syncTask(new SyncTask());
    }

    @Override
    public void process() {
        CivLog.info("TimerEvent: GoodieRepo -------------------------------------");
        repoProcess();
        CivMessage.globalTitle(CivSettings.localize.localizedString("goodieRepoBroadcastTitle"), "");
        CivMessage.global(CivSettings.localize.localizedString("goodieRepoBroadcast"));
    }

    @Nonnull
    @Override
    public Calendar getNextDate() {
        Calendar cal = EventTimer.getCalendarInServerTimeZone();
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, CivSettings.goodsConfig.getInt("trade_goodie_repo_hour", 14));
        cal.add(Calendar.DATE, CivSettings.goodsConfig.getInt("trade_goodie_repo_day", 7));
        return cal;
    }

}
