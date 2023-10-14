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
package com.avrgaming.civcraft.sessiondb;

import com.avrgaming.civcraft.database.SQLController;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.NamedObject;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionAsyncRequest.Database;
import com.avrgaming.civcraft.sessiondb.SessionAsyncRequest.Operation;
import com.avrgaming.civcraft.structure.Buildable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class SessionDatabase {

    String tb_prefix;

    private final ConcurrentHashMap<String, ArrayList<SessionEntry>> cache = new ConcurrentHashMap<>();

    public SessionDatabase() {
        tb_prefix = SQLController.tb_prefix;
    }

    public static String TABLE_NAME = "SESSIONS";

    public static void init() throws SQLException {
        CivLog.info("================= SESSION DB INIT ======================");
        // Check/Build SessionDB tables
        if (!SQLController.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQLController.tb_prefix + TABLE_NAME + " (" +
                    "`request_id` int(11) unsigned NOT NULL auto_increment," +
                    "`key` mediumtext," +
                    "`value` mediumtext," +
                    "`town_id` VARCHAR(36)," +
                    "`civ_id` VARCHAR(36)," +
                    "`struct_id` VARCHAR(36)," +
                    "`time` long," +
                    "PRIMARY KEY (`request_id`)" + ")";

            SQLController.makeTable(table_create);
            CivLog.info("Created " + TABLE_NAME + " table");
        } else {
            CivLog.info(TABLE_NAME + " table OK!");
        }
        CivLog.info("==================================================");
    }

    public boolean add(String key, String value) {
        return add(key, value, NamedObject.NULL_UUID);
    }

    public boolean add(String key, String value, UUID civ_id) {
        return add(key, value, civ_id, NamedObject.NULL_UUID);
    }

    public boolean add(String key, String value, UUID civ_id, UUID town_id) {
        return add(key, value, civ_id, town_id, NamedObject.NULL_UUID);
    }

    public boolean add(String key, String value, UUID civ_id, UUID town_id, UUID struct_id) {
        SessionEntry se = new SessionEntry();
        se.key = key;
        se.value = value;
        se.civ_uuid = civ_id;
        se.town_uuid = town_id;
        se.struct_uuid = struct_id;
        se.time = System.currentTimeMillis();
        se.request_id = -1;

        // Add to cache map, then fire async add to DB.
        ArrayList<SessionEntry> entries = this.cache.get(key);
        if (entries == null) {
            entries = new ArrayList<>();
        }
        entries.add(se);

        //Fire async add to DB.
        //BukkitObjects.scheduleAsyncDelayedTask(new SessionDBAsyncOperation(Operation.ADD, Database.GAME, tb_prefix, se), 0);
        SessionAsyncRequest request = new SessionAsyncRequest(Operation.ADD, Database.GAME, tb_prefix, se);
        request.queue();
        return true;
    }

    public ArrayList<SessionEntry> lookup(String key) {
        ResultSet rs = null;
        PreparedStatement ps = null;
        ArrayList<SessionEntry> retList;

        try {
            // Lookup in cache first, then look in DB.
            retList = cache.get(key);
            if (retList != null) {
                return retList;
            }

            // Couldnt find in cache, attempt DB lookup.
            retList = new ArrayList<>();
            String code = "SELECT * FROM `" + tb_prefix + "SESSIONS` WHERE `key` = ?";

            try {
                ps = SQLController.getGameConnection().prepareStatement(code);
                ps.setString(1, key);

                rs = ps.executeQuery();

                while (rs.next()) {
                    SessionEntry se = new SessionEntry();
                    String line;

                    se.request_id = rs.getInt("request_id");

                    line = rs.getString("key");

                    if (line == null)
                        break;
                    else
                        se.key = line;

                    line = rs.getString("value");
                    if (line == null)
                        break;
                    else
                        se.value = line;

                    se.civ_uuid = UUID.fromString(rs.getString("civ_id"));
                    se.town_uuid = UUID.fromString(rs.getString("town_id"));
                    se.struct_uuid = UUID.fromString(rs.getString("struct_id"));

                    se.time = rs.getLong("time");

                    retList.add(se);
                }

            } catch (SQLException e) {
                CivLog.error("SQLController: select sql error " + e.getMessage() + " --> " + code);
            }

            // Add what we found to the cache.
            cache.put(key, retList);

            return retList;
        } finally {
            SQLController.close(rs, ps);
        }
    }

    /* debug function to test the session DB */
    public void test() {
        add("ThisTestKey", "ThisTestData", NamedObject.NULL_UUID, NamedObject.NULL_UUID, NamedObject.NULL_UUID);

        for (SessionEntry se : lookup("ThisTestKey")) {
            CivLog.info("GOT ME SOME:" + se.value);
        }

    }

    public boolean delete_all(String key) {
        SessionEntry se = new SessionEntry();
        se.key = key;

        // Remove all from the cache
        cache.remove(key);

        //Fire async delete all from DB.
        SessionAsyncRequest request = new SessionAsyncRequest(Operation.DELETE_ALL, Database.GAME, tb_prefix, se);
        request.queue();
        return true;
    }

    public boolean delete(int request_id, String key) {
        SessionEntry se = new SessionEntry();
        se.request_id = request_id;

        // Remove it from the cache as well
        ArrayList<SessionEntry> entries = cache.get(key);
        if (entries != null) {
            for (SessionEntry e : entries) {
                if (e.request_id == request_id) {
                    entries.remove(e);
                    break;
                }
            }
            // Go ahead and remove entire array if empty now
            if (entries.isEmpty()) {
                cache.remove(key);
            }
        }


        //Fire async delete reqid from DB.
        SessionAsyncRequest request = new SessionAsyncRequest(Operation.DELETE, Database.GAME, tb_prefix, se);
        request.queue();
        return true;
    }


    public boolean update(int request_id, String key, String newValue) {
        SessionEntry se = new SessionEntry();
        se.request_id = request_id;
        se.value = newValue;
        se.key = key;

        // Update cache as well.
        ArrayList<SessionEntry> entries = cache.get(key);
        if (entries != null) {
            for (SessionEntry e : entries) {
                if (e.request_id == request_id) {
                    e.value = newValue;
                }
            }
        } else {
            entries = new ArrayList<>();
            entries.add(se);
            cache.put(se.key, entries);
        }

        //Fire async to update DB.
        SessionAsyncRequest request = new SessionAsyncRequest(Operation.UPDATE, Database.GAME, tb_prefix, se);
        request.queue();
        return true;
    }


    public void deleteAllForTown(Town town) {
        /* XXX FIXME, we use this for sessiondb deletion when towns die. Need to make this waaay  better by using SQLController queries. */
//		class AsyncTask implements Runnable {
//			Town town;
//			
//			public AsyncTask(Town town) {
//				this.town = town;
//			}
//			
//			@Override
//			public void run() {
//				SessionDBAsyncOperation async;
//				async = new SessionDBAsyncOperation(Operation.DELETE, Database.GAME, tb_prefix, new SessionEntry());
//				
//				// Gather keys to remove
//				LinkedList<String> removedKeys = new LinkedList<String>();
//				for (String key : cache.keySet()) {
//					ArrayList<SessionEntry> entries = cache.get(key);
//					if (entries.size() == 0) {
//						/* Clear any empty keys. */
//						removedKeys.add(key);
//						continue;
//					}
//
//					/* Remove any individual SessionEntries that have the town id. */
//					int entriesRemoved = 0;
//					LinkedList<Integer> removedEntries = new LinkedList<Integer>();
//					int i = 0;
//					for (SessionEntry entry : entries) {
//						if (entry.town_id == town.getId()) {
//							removedEntries.add(i);
//							entriesRemoved++;
//						}
//						i++;
//					}
//					
//					/* If we've removed all of the entries, remove the key as well. */
//					if (entriesRemoved == entries.size()) {
//						removedKeys.add(key);
//						continue;
//					}
//					
//					/* Actually remove the entries, and save the entry list back in the cache */
//					for (Integer index : removedEntries) {
//						/* Run the operation on this entry. */
//						async.entry = entries.get(index);
//						async.run();
//						entries.remove(index);
//					}
//					cache.put(key, entries);
//					
//				}
//				
//				/* Remove the keys. */
//				for (String key : removedKeys) {
//					async.op = Operation.DELETE_ALL;
//					async.entry.key = key;
//					cache.remove(key);
//				}
//			}
//		}
//		
//		TaskMaster.asyncTask(new AsyncTask(town), 0);
    }

    public void deleteAllForBuildable(Buildable buildable) {
        /* TODO Make this better by using SQLController queries. */
//		class AsyncTask implements Runnable {
//			Buildable buildable;
//			
//			public AsyncTask(Buildable buildable) {
//				this.buildable = buildable;
//			}
//			
//			@Override
//			public void run() {
//				SessionDBAsyncOperation async;
//				async = new SessionDBAsyncOperation(Operation.DELETE, Database.GAME, tb_prefix, new SessionEntry());
//				
//				// Gather keys to remove
//				LinkedList<String> removedKeys = new LinkedList<String>();
//				for (String key : cache.keySet()) {
//					ArrayList<SessionEntry> entries = cache.get(key);
//					if (entries.size() == 0) {
//						/* Clear any empty keys. */
//						removedKeys.add(key);
//						continue;
//					}
//
//					/* Remove any individual SessionEntries that have the town id. */
//					int entriesRemoved = 0;
//					LinkedList<Integer> removedEntries = new LinkedList<Integer>();
//					int i = 0;
//					for (SessionEntry entry : entries) {
//						if (entry.struct_id == buildable.getId()) {
//							removedEntries.add(i);
//							entriesRemoved++;
//						}
//						i++;
//					}
//					
//					/* If we've removed all of the entries, remove the key as well. */
//					if (entriesRemoved == entries.size()) {
//						removedKeys.add(key);
//						continue;
//					}
//					
//					/* Actually remove the entries, and save the entry list back in the cache */
//					for (Integer index : removedEntries) {
//						/* Run the operation on this entry. */
//						async.entry = entries.get(index);
//						async.run();
//						entries.remove(index);
//					}
//					cache.put(key, entries);
//					
//				}
//				
//				/* Remove the keys. */
//				for (String key : removedKeys) {
//					async.op = Operation.DELETE_ALL;
//					async.entry.key = key;
//					cache.remove(key);
//				}
//			}
//		}
//		
//		TaskMaster.asyncTask(new AsyncTask(buildable), 0);
    }

}
