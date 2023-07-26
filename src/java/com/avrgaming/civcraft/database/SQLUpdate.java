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
package com.avrgaming.civcraft.database;

import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.object.SQLObject;

import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SQLUpdate implements Runnable {

//	public static final int QUEUE_SIZE = 4096;
//	public static final int UPDATE_LIMIT = 50;
//	public static ReentrantLock lock = new ReentrantLock();

    private static final ConcurrentLinkedQueue<SQLObject> saveObjects = new ConcurrentLinkedQueue<>();
    public static ConcurrentHashMap<String, Integer> statSaveRequests = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Integer> statSaveCompletions = new ConcurrentHashMap<>();

    public static void add(SQLObject obj) {

        statSaveRequests.put(obj.getClass().getSimpleName(), 1 + Optional.ofNullable(statSaveRequests.get(obj.getClass().getSimpleName())).orElse(0));

        saveObjects.add(obj);
    }

    public static void save() {
        for (SQLObject obj : saveObjects) {
            if (obj == null) {
                continue;
            }
            try {
                obj.saveNow();
                statSaveCompletions.put(obj.getClass().getSimpleName(), 1 + Optional.ofNullable(statSaveCompletions.get(obj.getClass().getSimpleName())).orElse(0));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    @SuppressWarnings("BusyWait")
    @Override
    public void run() {
        while (true) {
            try {
                if (CivCraft.isDisable) {
                    break;
                }
                SQLObject obj = saveObjects.poll();
                if (obj == null) {
                    if (saveObjects.isEmpty()) {
                        Thread.sleep(500);
                    }
                    continue;
                }

                obj.saveNow();

                statSaveCompletions.put(obj.getClass().getSimpleName(), 1 + Optional.ofNullable(statSaveCompletions.get(obj.getClass().getSimpleName())).orElse(0));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
