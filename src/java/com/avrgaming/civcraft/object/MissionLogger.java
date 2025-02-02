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
package com.avrgaming.civcraft.object;

import com.avrgaming.civcraft.database.SQLController;
import com.avrgaming.civcraft.main.CivLog;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class MissionLogger {


    public static String TABLE_NAME = "MISSION_LOGS";

    public static void init() throws SQLException {
        if (!SQLController.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQLController.tb_prefix + TABLE_NAME + " (" +
                    "`uuid` VARCHAR(36) NOT NULL," +
                    "`town_uuid` VARCHAR(36)," +
                    "`target_uuid` VARCHAR(36)," +
                    "`time` long," +
                    "`playerName` mediumtext," +
                    "`missionName` mediumtext," +
                    "`result` mediumtext," +
                    "PRIMARY KEY (`uuid`)" + ")";

            SQLController.makeTable(table_create);
            CivLog.info("Created " + TABLE_NAME + " table");
        } else {
            CivLog.info(TABLE_NAME + " table OK!");
        }
    }


    public static void logMission(Town town, Town target, Resident resident, String missionName, String result) {
        HashMap<String, Object> hashmap = new HashMap<>();

        hashmap.put("town_uuid", town.getUUID().toString());
        hashmap.put("target_uuid", target.getUUID().toString());
        hashmap.put("time", new Date());
        hashmap.put("playerName", resident.getUUIDString());


        hashmap.put("missionName", missionName);
        hashmap.put("result", result);

        try {
            SQLController.insertNow(hashmap, TABLE_NAME);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<String> getMissionLogs(Town town) {
        ResultSet rs = null;
        PreparedStatement ps = null;


        ArrayList<String> out = new ArrayList<>();
        try {
            ps = SQLController.getGameConnection().prepareStatement("SELECT * FROM " + SQLController.tb_prefix + TABLE_NAME + " WHERE `town_uuid` = ?");
            ps.setString(1, town.getUUID().toString());
            rs = ps.executeQuery();

            SimpleDateFormat sdf = new SimpleDateFormat("M/dd h:mm:ss a z");
            while (rs.next()) {
                Town target = Town.getTownFromUUID(UUID.fromString(rs.getString("target_uuid")));
                if (target == null) {
                    continue;
                }
                out.add(
                        sdf.format(new Date(rs.getLong("time"))) +
                                " - " + rs.getString("playerName") +
                                ":" + target.getName() +
                                ":" + rs.getString("missionName") +
                                " -- " + rs.getString("result"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SQLController.close(rs, ps);
        }

        return out;

    }

}
