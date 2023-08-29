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

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.structure.Farm;
import com.avrgaming.civcraft.structure.farm.FarmChunk;
import com.avrgaming.civcraft.structure.farm.GrowBlock;
import com.avrgaming.civcraft.threading.sync.request.GrowRequest;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

public class SyncGrowTask implements Runnable {

    public static Queue<GrowRequest> requestQueue = new LinkedList<>();
    public static ReentrantLock lock;

    public static final int UPDATE_LIMIT = 200;

    public SyncGrowTask() {
        lock = new ReentrantLock();
    }

    @Override
    public void run() {
        if (!CivGlobal.growthEnabled) {
            return;
        }

        HashSet<FarmChunk> unloadedFarms = new HashSet<>();

        if (lock.tryLock()) {
            try {
                for (int i = 0; i < UPDATE_LIMIT; i++) {
                    GrowRequest request = requestQueue.poll();
                    if (request == null) {
                        return;
                    }

                    if (request.farmChunk == null) {
                        request.result = false;
                    } else if (!request.farmChunk.getChunk().isLoaded()) {
                        // This farm's chunk isn't loaded so we can't update
                        // the crops. Add the missed growths to the farms to
                        // process later.
                        unloadedFarms.add(request.farmChunk);
                        request.result = false;

                    } else {

                        for (GrowBlock growBlock : request.growBlocks) {
                            switch (growBlock.getMaterial()) {
                                case CARROT, WHEAT, POTATO -> {
                                    Block block = growBlock.bcoord.getBlock();
                                    if ((growBlock.data - 1) != block.getData()) {
                                        // replanted??
                                        continue;
                                    }
                                }
                            }

                            Block block1 = growBlock.bcoord.getBlock();
                            if (growBlock.spawn || block1.getType() == growBlock.getMaterial()) {
                                if (growBlock.spawn) {
                                    // Only allow block to change its type if its marked as spawnable.
                                    Block block = growBlock.bcoord.getBlock();
                                    block.setType(growBlock.getMaterial());
                                }
                                Block block = growBlock.bcoord.getBlock();
                                var data = block.getState();
                                data.setRawData((byte) growBlock.data);
                                data.update(true, false);
                                request.result = true;
                            }

                        }
                    }

                    request.finished = true;
                    request.condition.signalAll();
                }

                // increment any farms that were not loaded.
                for (FarmChunk fc : unloadedFarms) {
                    fc.incrementMissedGrowthTicks();
                    Farm farm = (Farm) fc.getStruct();
                    farm.saveMissedGrowths();
                }


            } finally {
                lock.unlock();
            }
        } else {
            CivLog.warning("SyncGrowTask: lock busy, retrying next tick.");
        }
    }
}

