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
package com.avrgaming.civcraft.threading.tasks;

import com.avrgaming.civcraft.config.ConfigTradeGood;
import com.avrgaming.civcraft.database.SQLController;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.TradeGood;
import com.avrgaming.civcraft.populators.TradeGoodPick;
import com.avrgaming.civcraft.populators.TradeGoodPopulator;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;

public class TradeGoodPostGenTask implements Runnable {

    String playerName;
    int start;

    public TradeGoodPostGenTask(String playerName, int start) {
        this.playerName = playerName;
        this.start = 0;
    }

    public void deleteAllTradeGoodiesFromDB() {
        String code = "TRUNCATE TABLE " + TradeGood.TABLE_NAME;
        try (PreparedStatement ps = SQLController.getGameConnection().prepareStatement(code)) {
            ps.execute();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }

    }
    @SuppressWarnings("BusyWait")
    @Override
    public void run() {
        CivLog.info("Generating/Clearing Trade goods...");
        CivLog.info("|- Organizing trade picks into a Queue.");

        deleteAllTradeGoodiesFromDB();

        /* Generate Trade Good Pillars. */
        Queue<TradeGoodPick> picksQueue = new LinkedList<>(CivGlobal.tradeGoodPreGenerator.goodPicks.values());

        int count = 0;
        int amount = 20;
        int totalSize = picksQueue.size();
        while (picksQueue.peek() != null) {
            CivLog.info("|- Placing/Picking Goods:" + count + "/" + totalSize + " current size:" + picksQueue.size());

            Queue<TradeGoodPick> processQueue = new LinkedList<>();
            for (int i = 0; i < amount; i++) {
                TradeGoodPick pick = picksQueue.poll();
                if (pick == null) {
                    break;
                }

                count++;
                processQueue.add(pick);
            }

            TaskMaster.syncTask(new SyncTradeGenTask(processQueue, amount));

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

    static class SyncTradeGenTask implements Runnable {
        public Queue<TradeGoodPick> picksQueue;
        public int amount;

        public SyncTradeGenTask(Queue<TradeGoodPick> picksQueue, int amount) {
            this.picksQueue = picksQueue;
            this.amount = amount;
        }

        @Override
        public void run() {
            World world = Bukkit.getWorld("world");
            BlockCoord bcoord2 = new BlockCoord();

            for (int i = 0; i < amount; i++) {
                TradeGoodPick pick = picksQueue.poll();
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

                /* try to detect already existing trade goods. */
                while (true) {
                    Block top = world.getBlockAt(bcoord2.getX(), bcoord2.getY(), bcoord2.getZ());

                    if (!top.getChunk().isLoaded()) {
                        top.getChunk().load();
                    }

                    if (top.getType() != Material.BEDROCK) {
                        break;
                    }
                    top.setType(Material.AIR);
                    top.setData((byte) 0, true);
                    bcoord2.setY(bcoord2.getY() - 1);
                    for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                        Block block = top.getRelative(face);
                        if (block.getType() == Material.WALL_SIGN) {
                            block.setType(Material.AIR);
                            top.setData((byte) 0, true);
                        }
                    }


                }

                centerY = world.getHighestBlockYAt(centerX, centerZ);

                // Determine if we should be a water good.
                ConfigTradeGood good;
                if (world.getBlockAt(centerX, centerY - 1, centerZ).getType() == Material.STATIONARY_WATER ||
                        world.getBlockAt(centerX, centerY - 1, centerZ).getType() == Material.WATER) {
                    good = pick.waterPick;
                } else {
                    good = pick.landPick;
                }

                // Randomly choose a land or water good.
                if (good == null) {
                    CivLog.warning("Could not find suitable good type during populate! aborting.");
                    continue;
                }

                // Create a copy and save it in the global hash table.
                BlockCoord bcoord = new BlockCoord(world.getName(), centerX, centerY, centerZ);
                TradeGoodPopulator.buildTradeGoodie(good, bcoord, world, true);

            }
        }
    }

}
