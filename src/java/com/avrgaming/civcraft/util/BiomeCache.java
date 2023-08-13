package com.avrgaming.civcraft.util;

import com.avrgaming.civcraft.database.SQLController;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.threading.TaskMaster;
import org.bukkit.Chunk;
import org.bukkit.block.Biome;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class BiomeCache {
    /*
     * We need to figure out which "biome" a chunk is when we create a culture chunk.
     * The problem is, this requires us to load the ENTIRE CHUNK to get at this little
     * snippet of data. (the biome at a particlar block in a chunk). This can cause us
     * to load literally gigabytes of extra data for "no reason" other than to find out
     * what the biome is. Hence this cache.
     *
     */
    public static HashMap<String, String> biomeCache = new HashMap<>();

    public static String TABLE_NAME = "CHUNK_BIOMES";

    public static void init() throws SQLException {
        CivLog.info("================= BiomeCache INIT ======================");
        if (!SQLController.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQLController.tb_prefix + TABLE_NAME + " (" +
                    "`key` varchar(64) NOT NULL," +
                    "`value` mediumtext," +
                    "PRIMARY KEY (`key`)" + ")";

            SQLController.makeTable(table_create);
            CivLog.info("Created " + TABLE_NAME + " table");
        } else {
            CivLog.info(TABLE_NAME + " table OK!");
        }

        Connection context = null;
        ResultSet rs = null;
        PreparedStatement ps = null;

        try {
            int count = 0;
            context = SQLController.getGameConnection();
            ps = context.prepareStatement("SELECT * FROM " + SQLController.tb_prefix + TABLE_NAME);
            rs = ps.executeQuery();

            while (rs.next()) {
                count++;
                String key = rs.getString("key");
                String value = rs.getString("value");
                biomeCache.put(key, value);
            }

            CivLog.info("Loaded " + count + " Biome Cache Entries");
        } finally {
            SQLController.close(rs, ps, context);
        }

        CivLog.info("==================================================");
    }

    public static void saveBiomeInfo(CultureChunk cc) {
        TaskMaster.asyncTask(() -> {
            Connection context = null;

            try {
                context = SQLController.getGameConnection();
                PreparedStatement ps = context.prepareStatement("INSERT INTO `" + SQLController.tb_prefix + TABLE_NAME + "` (`key`, `value`) VALUES (?, ?)" +
                        " ON DUPLICATE KEY UPDATE `value` = ?");
                ps.setString(1, cc.getChunkCoord().toString());
                ps.setString(2, cc.getBiome().name());
                ps.setString(3, cc.getBiome().name());

                int rs = ps.executeUpdate();
                if (rs == 0) {
                    CivLog.error("Couldn't update biome cache for key:" + cc.getChunkCoord().toString() + " with value: " + cc.getBiome().name());
                }

            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
            }
        }, 0);
    }

    public static Biome getBiome(CultureChunk cc) {
        if (biomeCache.containsKey(cc.getChunkCoord().toString())) {
            return Biome.valueOf(biomeCache.get(cc.getChunkCoord().toString()));
        }

        TaskMaster.syncTask(() -> {
            Chunk chunk = cc.getChunkCoord().getChunk();
            cc.setBiome(chunk.getWorld().getBiome((chunk.getX() * 16), (chunk.getZ() * 16)));
            BiomeCache.saveBiomeInfo(cc);
        });
        return Biome.HELL;

    }


}
