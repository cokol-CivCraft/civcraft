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

import com.avrgaming.civcraft.database.SQLController;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.randomevents.RandomEventTimer;
import com.avrgaming.civcraft.threading.TaskMaster;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;

public class EventTimer {

    private Calendar next;
    private Calendar last;
    private String name;
    private EventInterface eventFunction;
    /* Number of seconds this event repeats. */

    public static HashMap<String, EventTimer> timers = new HashMap<>();

    public static String TABLE_NAME = "TIMERS";

    public static void init() throws SQLException {
        if (!SQLController.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQLController.tb_prefix + TABLE_NAME + " (" +
                    "`name` VARCHAR(64) NOT NULL," +
                    "`nextEvent` long," +
                    "`lastEvent` long," +
                    "PRIMARY KEY (`name`)" + ")";

            SQLController.makeTable(table_create);
            CivLog.info("Created " + TABLE_NAME + " table");
        } else {
            CivLog.info(TABLE_NAME + " table OK!");
        }
    }


    public static void loadGlobalEvents() {
//		TestEvent test = new TestEvent();
//		try {
//			new EventTimer("test", test, test.getNextDate());
//		} catch (InvalidConfiguration e1) {
//			e1.printStackTrace();
//		}

        /* Setup daily upkeep event. */
        DailyEvent upkeepEvent = new DailyEvent();
        new EventTimer("daily", upkeepEvent, upkeepEvent.getNextDate());

        /* Setup Hourly tick event. */
        HourlyTickEvent hourlyTickEvent = new HourlyTickEvent();
        new EventTimer("hourly", hourlyTickEvent, hourlyTickEvent.getNextDate());

        /* Setup war event. */
        WarEvent WarEvent = new WarEvent();
        new EventTimer("war", WarEvent, WarEvent.getNextDate());

        /* Setup disable Teleport event. */
        DisableTeleportEvent DisableTeleportEvent = new DisableTeleportEvent();
        new EventTimer("disabletp", DisableTeleportEvent, DisableTeleportEvent.getNextDate());

        /* Setup repo event. */
        GoodieRepoEvent repoEvent = new GoodieRepoEvent();
        new EventTimer("repo-goodies", repoEvent, repoEvent.getNextDate());

        /* Setup random event timer. */
        RandomEventTimer randEvent = new RandomEventTimer();
        new EventTimer("random", randEvent, randEvent.getNextDate());

    }


    public EventTimer(String name, EventInterface eventFunction, Calendar start) {
        try {
            this.load(name, eventFunction, start);
        } catch (SQLException e) {
            e.printStackTrace();
        }
//		this.name = name;
//		this.eventFunction = eventFunction;
//		this.peroid = peroid;
//		this.next = start;
//		register();
    }

    public void load(String timerName, EventInterface eventFunction, Calendar start) throws SQLException {
        ResultSet rs = null;
        PreparedStatement ps = null;

        try {
            String query = "SELECT * FROM `" + SQLController.tb_prefix + TABLE_NAME + "` WHERE `name` = ?";
            ps = SQLController.getGameConnection().prepareStatement(query);
            ps.setString(1, timerName);
            rs = ps.executeQuery();

            this.name = timerName;
            this.eventFunction = eventFunction;

            if (rs.next()) {
                this.last = EventTimer.getCalendarInServerTimeZone();
                this.last.setTimeInMillis(rs.getLong("lastEvent"));

                this.next = EventTimer.getCalendarInServerTimeZone();
                this.next.setTimeInMillis(rs.getLong("nextEvent"));
            } else {
                this.last = EventTimer.getCalendarInServerTimeZone();
                this.last.setTimeInMillis(0);

                this.next = start;
                this.save();
            }
            register();
        } finally {
            SQLController.close(rs, ps);
        }
    }

    private void register() {
        timers.put(this.name, this);
    }

    public void save() {
        class SaveLater implements Runnable {
            final EventTimer timer;

            SaveLater(EventTimer timer) {
                this.timer = timer;
            }

            public void run() {
                try {
                    timer.saveNow();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        TaskMaster.asyncTask(new SaveLater(this), 0);
    }

    public void saveNow() throws SQLException {
        PreparedStatement ps = null;

        try {
            String query = "INSERT INTO `" + SQLController.tb_prefix + TABLE_NAME + "` (`name`, `nextEvent`, `lastEvent`) " +
                    "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `nextEvent`=?, `lastEvent`=?";
            ps = SQLController.getGameConnection().prepareStatement(query);

            ps.setString(1, this.name);
            ps.setLong(2, next.getTime().getTime());
            ps.setLong(3, last.getTime().getTime());
            ps.setLong(4, next.getTime().getTime());
            ps.setLong(5, last.getTime().getTime());

            int rs = ps.executeUpdate();
            if (rs == 0) {
                throw new SQLException("Could not execute SQLController code:" + query);
            }
        } finally {
            SQLController.close(null, ps);
        }

    }

    public Calendar getNext() {
        return next;
    }

    public Calendar getLast() {
        return last;
    }

    public void setNext(Calendar next2) {
        this.next = next2;
    }

    public void setLast(Calendar last) {
        this.last = last;
    }

    public EventInterface getEventFunction() {
        return eventFunction;
    }

    public void setEventFunction(EventInterface eventFunction) {
        this.eventFunction = eventFunction;
    }

    public String getName() {
        return name;
    }

    public static Calendar getCalendarInServerTimeZone() {
        // This doesnt fucking work. IDK why. But when I "Add" time to a calendar after setting the time zone
        // I get really strange results... Example:
        // 11/25 9:36:31 PM PST, setting to 0 seconds
        // gets set to 11/25 7:36:00 PM PST
        // FFS WHY?!

        //try {
        //cal.setTimeZone(TimeZone.getTimeZone(CivSettings.getStringBase("server_timezone")));
        //} catch (InvalidConfiguration e) {
        //	e.printStackTrace();
        //}

        return Calendar.getInstance();
    }

}
