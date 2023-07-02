package com.avrgaming.civcraft.threading.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.avrgaming.civcraft.config.ConfigMobSpawner;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.MobSpawner;
import com.avrgaming.civcraft.populators.MobSpawnerPick;
import com.avrgaming.civcraft.populators.MobSpawnerPopulator;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.ItemManager;

public class MobSpawnerPostGenTask implements Runnable {

    String playerName;
    int start;
    
    public MobSpawnerPostGenTask(String playerName, int start) {
        this.playerName = playerName;
        this.start = 0;
    }
    
    public void deleteAllMobSpawnersFromDB() {
        /* Delete all existing trade goods from DB. */
        Connection conn = null;
        PreparedStatement ps = null;
        try {
        try {
            conn = SQL.getGameConnection();
            String code = "TRUNCATE TABLE "+MobSpawner.TABLE_NAME;
            ps = conn.prepareStatement(code);
            ps.execute();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        } finally {
            if (conn != null) {
                try {
                    conn.close();
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
    
    @Override
    public void run() {
        CivLog.info("Generating/Clearing Mob Spawners...");
        CivLog.info("|- Organizing trade picks into a Queue.");

        deleteAllMobSpawnersFromDB();

        /* Generate Trade Good Pillars. */
        Queue<MobSpawnerPick> picksQueue = new LinkedList<>();
        picksQueue.addAll(CivGlobal.mobSpawnerPreGenerator.spawnerPicks.values());

        int count = 0;
        int amount = 20;
        int totalSize = picksQueue.size();
        while (picksQueue.peek() != null) {
            CivLog.info("|- Placing/Picking spawners:" + count + "/" + totalSize + " current size:" + picksQueue.size());

            Queue<MobSpawnerPick> processQueue = new LinkedList<>();
            for (int i = 0; i < amount; i++) {
                MobSpawnerPick pick = picksQueue.poll();
                if (pick == null) {
                    break;
                }
                
                count++;
                processQueue.add(pick);
            }
            
            TaskMaster.syncTask(new SyncMopSpawnerGenTask(processQueue, amount));
            
            try {
                while (processQueue.peek() != null) {
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                return;
            }
        }
        
        
        CivLog.info("Finished!");
    }

    static class SyncMopSpawnerGenTask implements Runnable {
        public Queue<MobSpawnerPick> picksQueue;
        public int amount;

        public SyncMopSpawnerGenTask(Queue<MobSpawnerPick> picksQueue, int amount) {
            this.picksQueue = picksQueue;
            this.amount = amount;
        }

        @Override
        public void run() {
            World world = Bukkit.getWorld("world");
            BlockCoord bcoord2 = new BlockCoord();

            for(int i = 0; i < amount; i++) {
                MobSpawnerPick pick = picksQueue.poll();
                if (pick == null) {
                    return;
                }
                
                ChunkCoord coord = pick.chunkCoord;
                Chunk chunk = world.getChunkAt(coord.getX(), coord.getZ());
                
                int centerX = (chunk.getX() << 4) + 8;
                int centerZ = (chunk.getZ() << 4) + 8;
                int centerY = world.getHighestBlockYAt(centerX, centerZ);
                
                
                
                bcoord2.setWorldname("world");
                bcoord2.setX(centerX);
                bcoord2.setY(centerY - 1);
                bcoord2.setZ(centerZ);
                
                /* try to detect already existing mob spawners. */
                while(true) {
                    Block top = world.getBlockAt(bcoord2.getX(), bcoord2.getY(), bcoord2.getZ());

                    if (!top.getChunk().isLoaded()) {
                        top.getChunk().load();
                    }

                    if (top.getType() != Material.BEDROCK) {
                        break;
                    }
                    top.setType(Material.AIR);
                    ItemManager.setData(top, 0, true);
                    bcoord2.setY(bcoord2.getY() - 1);
                    for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                        Block block = top.getRelative(face);
                        if (block.getType() == Material.WALL_SIGN) {
                            block.setType(Material.AIR);
                            block.setData((byte) 0);
                        }
                    }

                }

                centerY = world.getHighestBlockYAt(centerX, centerZ);

                // Determine if we should be a water good.
                ConfigMobSpawner good;
                if (world.getBlockAt(centerX, centerY - 1, centerZ).getType() == Material.STATIONARY_WATER ||
                        world.getBlockAt(centerX, centerY - 1, centerZ).getType() == Material.WATER) {
                    good = pick.waterPick;
                } else {
                    good = pick.landPick;
                }

                // Randomly choose a land or water good.
                if (good == null) {
                    System.out.println("Could not find suitable mob spawner type during populate! aborting.");
                    continue;
                }
                
                // Create a copy and save it in the global hash table.
                BlockCoord bcoord = new BlockCoord(world.getName(), centerX, centerY, centerZ);
                MobSpawnerPopulator.buildMobSpawner(good, bcoord, world, true);
                
            }
        }
    }
    
}
