/*
 * AVRGAMING LLC
 * __________________
 *
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,TICE:  All information contained herein is, and remains
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

import com.avrgaming.civcraft.arena.ArenaTeam;
import com.avrgaming.civcraft.camp.Camp;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMarketItem;
import com.avrgaming.civcraft.event.EventTimer;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.*;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.randomevents.RandomEvent;
import com.avrgaming.civcraft.sessiondb.SessionDatabase;
import com.avrgaming.civcraft.structure.MetaStructure;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BiomeCache;
import com.avrgaming.global.scores.ScoreManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

public class SQLController {

    public static String hostname = "";
    public static String port = "";
    public static String db_name = "";
    public static String username = "";
    public static String password = "";
    public static String tb_prefix = "";

    private static String dsn = "";

    public static Integer min_conns;
    public static Integer max_conns;
    public static Integer parts;
    //	public static Connection context = null;
    public static ConnectionPool gameDatabase;

    public static void initialize() {
        CivLog.heading("Initializing SQLController");

        SQLController.hostname = CivSettings.plugin.getConfig().getString("mysql.hostname");
        SQLController.port = CivSettings.plugin.getConfig().getString("mysql.port");
        SQLController.db_name = CivSettings.plugin.getConfig().getString("mysql.database");
        SQLController.username = CivSettings.plugin.getConfig().getString("mysql.username");
        SQLController.password = CivSettings.plugin.getConfig().getString("mysql.password");
        SQLController.tb_prefix = CivSettings.plugin.getConfig().getString("mysql.table_prefix");
        SQLController.dsn = "jdbc:mysql://" + hostname + ":" + port + "/" + tb_prefix + db_name + "?enabledTLSProtocols=TLSv1.2&useSSL=false";
        SQLController.min_conns = Integer.valueOf(CivSettings.plugin.getConfig().getString("mysql.min_conns"));
        SQLController.max_conns = Integer.valueOf(CivSettings.plugin.getConfig().getString("mysql.max_conns"));
        SQLController.parts = Integer.valueOf(CivSettings.plugin.getConfig().getString("mysql.parts"));

        CivLog.info("\t Using " + SQLController.hostname + ":" + SQLController.port + " user:" + SQLController.username + " DB:" + SQLController.db_name);

        CivLog.info("\t Building Connection Pool for GAME database.");
        gameDatabase = new ConnectionPool(SQLController.dsn, SQLController.username, SQLController.password);
        CivLog.info("\t Connected to GAME database");

        CivLog.heading("Initializing SQLController Finished");
    }


    public static void initCivObjectTables() throws SQLException {
        CivLog.heading("Building Civ Object Tables.");

        SessionDatabase.init();
        BiomeCache.init();
        Civilization.init();
        Town.init();
        Resident.init();
        Relation.init();
        TownChunk.init();
        Structure.init();
        Wonder.init();
//        WallBlock.init();
//        RoadBlock.init();
        PermissionGroup.init();
        TradeGood.init();
        ProtectedBlock.init();
        BonusGoodie.init();
        MissionLogger.init();
        EventTimer.init();
        Camp.init();
        ConfigMarketItem.init();
        RandomEvent.init();
        ArenaTeam.init();
        StructureSign.init();

        CivLog.heading("Building Global Tables!!");
        ScoreManager.init();

        CivLog.info("----- Done Building Tables ----");

    }

    public static Connection getGameConnection() {
        return gameDatabase.getConnection();
    }

    public static boolean hasTable(String name) throws SQLException {
        ResultSet result = null;

        try {
            DatabaseMetaData dbm = getGameConnection().getMetaData();
            String[] types = {"TABLE"};

            result = dbm.getTables(null, null, SQLController.tb_prefix + name, types);
            return result.next();
        } finally {
            SQLController.close(result, null);
        }
    }

    public static boolean hasGlobalTable(String name) throws SQLException {
        ResultSet result = null;

        try {
            DatabaseMetaData dbm = getGameConnection().getMetaData();
            String[] types = {"TABLE"};

            result = dbm.getTables(null, null, name, types);
            return result.next();

        } finally {
            SQLController.close(result, null);
        }
    }

    public static boolean hasColumn(String tablename, String columnName) throws SQLException {
        ResultSet result = null;

        try {
            DatabaseMetaData dbm = getGameConnection().getMetaData();
            result = dbm.getColumns(null, null, SQLController.tb_prefix + tablename, columnName);
            return result.next();
        } finally {
            SQLController.close(result, null);
        }
    }

    public static void addColumn(String tablename, String columnDef) throws SQLException {
        PreparedStatement ps = null;

        try {
            String table_alter = "ALTER TABLE " + SQLController.tb_prefix + tablename + " ADD " + columnDef;
            ps = getGameConnection().prepareStatement(table_alter);
            ps.execute();
            CivLog.info("\tADDED:" + columnDef);
        } finally {
            SQLController.close(null, ps);
        }

    }

    public static boolean hasGlobalColumn(String tablename, String columnName) throws SQLException {
        ResultSet rs = null;

        try {
            DatabaseMetaData dbm = getGameConnection().getMetaData();
            rs = dbm.getColumns(null, null, tablename, columnName);

            try {
                return rs.next();
            } finally {
                rs.close();
            }

        } finally {
            SQLController.close(rs, null);
        }
    }

    public static void addGlobalColumn(String tablename, String columnDef) throws SQLException {
        PreparedStatement ps = null;

        try {
            String table_alter = "ALTER TABLE " + tablename + " ADD " + columnDef;
            ps = SQLController.getGameConnection().prepareStatement(table_alter);
            ps.execute();
            CivLog.info("\tADDED GLOBAL:" + columnDef);
        } finally {
            SQLController.close(null, ps);
        }
    }

    public static void updateNamedObjectAsync(SQLObject obj, HashMap<String, Object> hashmap, String tablename) {
        TaskMaster.asyncTask("", () -> {
            try {
                updateNamedObject(obj, hashmap, tablename);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, 0);
    }

    public static void updateNamedObject(SQLObject obj, HashMap<String, Object> hashmap, String tablename) throws SQLException {
        if (obj.isDeleted()) {
            return;
        }

        if (obj.getId() == 0) {
            if (obj instanceof MetaStructure || obj instanceof Town || obj instanceof Civilization) {
                obj.setUUID(UUID.randomUUID());
                hashmap.put("uuid", obj.getUUID().toString());
            }
            obj.setId(SQLController.insertNow(hashmap, tablename));
        } else {
            if (obj instanceof MetaStructure || obj instanceof Town || obj instanceof Civilization) {
                hashmap.put("uuid", obj.getUUID().toString());
            }
            SQLController.update(obj.getId(), hashmap, tablename);
        }
    }

    public static void update(int id, HashMap<String, Object> hashmap, String tablename) throws SQLException {
        hashmap.put("id", id);
        update(hashmap, "id", tablename);
    }


    public static void update(HashMap<String, Object> hashmap, String keyname, String tablename) throws SQLException {
        PreparedStatement ps = null;

        try {
            StringBuilder sql = new StringBuilder("UPDATE `" + SQLController.tb_prefix + tablename + "` SET ");
            String where = " WHERE `" + keyname + "` = ?;";
            ArrayList<Object> values = new ArrayList<>();

            Object keyValue = hashmap.get(keyname);
            hashmap.remove(keyname);

            Iterator<String> keyIter = hashmap.keySet().iterator();
            while (keyIter.hasNext()) {
                String key = keyIter.next();

                sql.append("`").append(key).append("` = ?");
                sql.append(keyIter.hasNext() ? ", " : " ");
                values.add(hashmap.get(key));
            }

            sql.append(where);

            ps = SQLController.getGameConnection().prepareStatement(sql.toString());

            int i = 1;
            for (Object value : values) {
                if (value instanceof String) {
                    ps.setString(i, (String) value);
                } else if (value instanceof Integer) {
                    ps.setInt(i, (Integer) value);
                } else if (value instanceof Boolean) {
                    ps.setBoolean(i, (Boolean) value);
                } else if (value instanceof Double) {
                    ps.setDouble(i, (Double) value);
                } else if (value instanceof Float) {
                    ps.setFloat(i, (Float) value);
                } else if (value instanceof Long) {
                    ps.setLong(i, (Long) value);
                } else {
                    ps.setObject(i, value);
                }
                i++;
            }

            ps.setObject(i, keyValue);

            if (ps.executeUpdate() == 0) {
                insertNow(hashmap, tablename);
            }
        } finally {
            SQLController.close(null, ps);
        }
    }

    public static void insert(HashMap<String, Object> hashmap, String tablename) {
        TaskMaster.asyncTask(new SQLInsertTask(hashmap, tablename), 0);
    }

    public static int insertNow(HashMap<String, Object> hashmap, String tablename) throws SQLException {
        ResultSet rs = null;
        PreparedStatement ps = null;

        try {
            String sql = "INSERT INTO " + SQLController.tb_prefix + tablename + " ";
            StringBuilder keycodes = new StringBuilder("(");
            StringBuilder valuecodes = new StringBuilder(" VALUES ( ");
            ArrayList<Object> values = new ArrayList<>();

            Iterator<String> keyIter = hashmap.keySet().iterator();
            while (keyIter.hasNext()) {
                String key = keyIter.next();

                keycodes.append(key);
                keycodes.append(keyIter.hasNext() ? "," : ")");

                valuecodes.append("?");
                valuecodes.append(keyIter.hasNext() ? "," : ")");

                values.add(hashmap.get(key));
            }

            sql += keycodes;
            sql += valuecodes;

            ps = SQLController.getGameConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            int i = 1;
            for (Object value : values) {
                if (value instanceof String) {
                    ps.setString(i, (String) value);
                } else if (value instanceof Integer) {
                    ps.setInt(i, (Integer) value);
                } else if (value instanceof Boolean) {
                    ps.setBoolean(i, (Boolean) value);
                } else if (value instanceof Double) {
                    ps.setDouble(i, (Double) value);
                } else if (value instanceof Float) {
                    ps.setFloat(i, (Float) value);
                } else if (value instanceof Long) {
                    ps.setLong(i, (Long) value);
                } else {
                    ps.setObject(i, value);
                }
                i++;
            }

            ps.execute();
            int id = 0;
            rs = ps.getGeneratedKeys();

            if (rs.next()) {
                id = rs.getInt(1);
            }

            if (id == 0) {
                String name = (String) hashmap.get("name");
                if (name == null) {
                    name = "Unknown";
                }

                CivLog.error("SQLController ERROR: Saving an SQLObject returned a 0 ID! Name:" + name + " Table:" + tablename);
            }
            return id;

        } finally {
            SQLController.close(rs, ps);
        }
    }


    public static void deleteNamedObject(SQLObject obj, String tablename) throws SQLException {
        PreparedStatement ps = null;

        try {
            String sql = "DELETE FROM " + SQLController.tb_prefix + tablename + " WHERE `id` = ?";
            ps = SQLController.getGameConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, obj.getId());
            ps.execute();
            ps.close();
            obj.setDeleted(true);
        } finally {
            SQLController.close(null, ps);
        }
    }

    public static void deleteByName(String name, String tablename) throws SQLException {
        PreparedStatement ps = null;

        try {
            String sql = "DELETE FROM " + SQLController.tb_prefix + tablename + " WHERE `name` = ?";
            ps = SQLController.getGameConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.execute();
            ps.close();
        } finally {
            SQLController.close(null, ps);
        }
    }

    public static void makeCol(String colname, String type, String TABLE_NAME) throws SQLException {
        if (!SQLController.hasColumn(TABLE_NAME, colname)) {
            CivLog.info("\tCouldn't find " + colname + " column for " + TABLE_NAME);
            SQLController.addColumn(TABLE_NAME, "`" + colname + "` " + type);
        }
    }

    public static void makeTable(String table_create) throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = SQLController.getGameConnection().prepareStatement(table_create);
            ps.execute();
        } finally {
            SQLController.close(null, ps);
        }

    }

    public static void makeGlobalTable(String table_create) throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = getGameConnection().prepareStatement(table_create);
            ps.execute();
        } finally {
            SQLController.close(null, ps);
        }
    }

    public static void close(ResultSet rs, PreparedStatement ps) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}
