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
package com.avrgaming.civcraft.threading.sync;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.structure.MetaStructure;
import com.avrgaming.civcraft.util.SimpleBlock;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.material.MaterialData;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;


public class SyncBuildUpdateTask implements Runnable {

    public static int UPDATE_LIMIT = Integer.MAX_VALUE;

    //	public static BlockingQueue<SimpleBlock> updateBlocks = new ArrayBlockingQueue<SimpleBlock>(QUEUE_SIZE);
    private static final Queue<SimpleBlock> updateBlocks = new LinkedList<>();


    public static ReentrantLock buildBlockLock = new ReentrantLock();

    public static void queueSimpleBlock(Queue<SimpleBlock> sbList) {
        buildBlockLock.lock();
        try {
            updateBlocks.addAll(sbList);
        } finally {
            buildBlockLock.unlock();
        }
    }

    public SyncBuildUpdateTask() {
    }

    /*
     * Runs once, per tick and changes the blocks represented by SimpleBlock
     * up to UPDATE_LIMIT times.
     */
    @Override
    public void run() {

        if (!buildBlockLock.tryLock()) {
            CivLog.warning("Couldn't get sync build update lock, skipping until next tick.");
        }
        try {

            for (int i = 0; i < UPDATE_LIMIT; i++) {
                SimpleBlock next = updateBlocks.poll();
                if (next == null) {
                    break;
                }
                Block block = Bukkit.getWorld(next.worldname).getBlockAt(next.x, next.y, next.z);
                next.setTo(block);

                /* Handle Special Blocks */
                switch (next.specialType) {
                    case COMMAND -> {
                        BlockState state = next.getLocation().getBlock().getState();
                        state.setType(Material.AIR);
                        state.update(true, false);
                    }
                    case LITERAL -> {
                        if (block.getState() instanceof Sign s) {

                            for (int j = 0; j < 4; j++) {
                                s.setLine(j, next.message[j]);
                            }

                            s.update();
                        } else {
                            block.getState().setData(new MaterialData(Material.AIR));
                        }
                    }
                    case NORMAL -> {
                    }
                }

                if (next.buildable instanceof MetaStructure structure) {
                    structure.savedBlockCount++;
                }
            }
        } finally {
            buildBlockLock.unlock();
        }
    }
}
