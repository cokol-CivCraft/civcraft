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

import com.avrgaming.civcraft.camp.CampHourlyTick;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.CultureProcessAsyncTask;
import com.avrgaming.civcraft.threading.timers.SyncTradeTimer;

import javax.annotation.Nonnull;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class HourlyTickEvent implements EventInterface {

    @Override
    public void process() {
        CivLog.info("TimerEvent: Hourly -------------------------------------");
        TaskMaster.asyncTask("cultureProcess", new CultureProcessAsyncTask(), 0);
        TaskMaster.asyncTask("EffectEventTimer", new EffectEventTimer(), 0);
        TaskMaster.syncTask(new SyncTradeTimer(), 0);
        TaskMaster.syncTask(new CampHourlyTick(), 0);
        CivLog.info("TimerEvent: Hourly Finished -----------------------------");
    }

    @Nonnull
    @Override
    public Calendar getNextDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("M/dd h:mm:ss a z");
        Calendar cal = EventTimer.getCalendarInServerTimeZone();

        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.add(Calendar.SECOND, CivSettings.civConfig.getInt("global.hourly_tick", 3600));
        sdf.setTimeZone(cal.getTimeZone());
        return cal;
    }

}
