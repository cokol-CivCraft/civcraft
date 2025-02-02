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
package com.avrgaming.global.scores;

import com.avrgaming.civcraft.database.SQLController;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ScoreManager {

    public static String TOWN_TABLE_NAME = "SCORES_TOWNS";
    public static String CIV_TABLE_NAME = "SCORES_CIVS";

    public static void init() throws SQLException {
        CivLog.info("================= SCORE_TOWN INIT ======================");

        // Check/Build SessionDB tables
        if (!SQLController.hasGlobalTable(TOWN_TABLE_NAME)) {
            String table_create = "CREATE TABLE " + TOWN_TABLE_NAME + " (" +
                    "`local_uuid` VARCHAR(36)," +
                    "`local_name` mediumtext," +
                    "`local_civ_name` mediumtext," +
                    "`points` int(11)," +
                    "PRIMARY KEY (`local_uuid`)" + ")";

            SQLController.makeGlobalTable(table_create);
            CivLog.info("Created " + TOWN_TABLE_NAME + " table");
        } else {
            CivLog.info(TOWN_TABLE_NAME + " table OK!");
        }

        CivLog.info("==================================================");

        CivLog.info("================= SCORE_CIV INIT ======================");

        // Check/Build SessionDB tables
        if (!SQLController.hasGlobalTable(CIV_TABLE_NAME)) {
            String table_create = "CREATE TABLE " + CIV_TABLE_NAME + " (" +
                    "`local_uuid` VARCHAR(36)," +
                    "`local_name` mediumtext," +
                    "`local_capitol_name` mediumtext," +
                    "`points` int(11)," +
                    "PRIMARY KEY (`local_uuid`)" + ")";

            SQLController.makeGlobalTable(table_create);
            CivLog.info("Created " + CIV_TABLE_NAME + " table");
        }

        CivLog.info("==================================================");
    }

    public static void UpdateScore(Civilization civ, int points) throws SQLException {
        PreparedStatement s = null;

        try {
            String query = "INSERT INTO `" + CIV_TABLE_NAME + "` (`local_uuid`, `local_name`, `local_capitol_name`, `points`) " +
                    "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE `local_name`=?, `local_capitol_name`=?, `points`=?";
            s = SQLController.getGameConnection().prepareStatement(query);

            s.setString(1, civ.getUUID().toString());
            s.setString(2, civ.getName());
            s.setString(3, civ.getCapitolName());
            s.setInt(4, points);

            s.setString(5, civ.getName());
            s.setString(6, civ.getCapitolName());
            s.setInt(7, points);

            if (s.executeUpdate() == 0) {
                throw new SQLException("Could not execute SQLController code:" + query);
            }

        } finally {
            SQLController.close(null, s);
        }
    }

    public static void UpdateScore(Town town, int points) throws SQLException {
        PreparedStatement s = null;

        try {
            String query = "INSERT INTO `" + TOWN_TABLE_NAME + "` (`local_uuid`, `local_name`, `local_civ_name`, `points`) " +
                    "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE `local_name`=?, `local_civ_name`=?, `points`=?";
            s = SQLController.getGameConnection().prepareStatement(query);

            s.setString(1, town.getUUID().toString());
            s.setString(2, town.getName());
            s.setString(3, town.getCiv().getName());
            s.setInt(4, points);

            s.setString(5, town.getName());
            s.setString(6, town.getCiv().getName());
            s.setInt(7, points);

            if (s.executeUpdate() == 0) {
                throw new SQLException("Could not execute SQLController code:" + query);
            }

        } finally {
            SQLController.close(null, s);
        }
    }
}
