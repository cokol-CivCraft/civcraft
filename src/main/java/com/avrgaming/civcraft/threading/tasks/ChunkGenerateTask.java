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
package com.avrgaming.civcraft.threading.tasks;


import com.avrgaming.civcraft.threading.TaskMaster;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;

public class ChunkGenerateTask implements Runnable {

    final int startX;
    final int startZ;
    final int stopX;
    final int stopZ;

    public ChunkGenerateTask(int startx, int startz, int stopx, int stopz) {
        this.startX = startx;
        this.startZ = startz;
        this.stopX = stopx;
        this.stopZ = stopz;
    }

    @Override
    public void run() {

        int maxgen = 10;
        int i = 0;

        for (int x = startX; x <= stopX; x++) {
            for (int z = startZ; z <= stopZ; z++) {
                i++;

                Chunk chunk = Bukkit.getWorld("world").getChunkAt(x, z);
                chunk.load(true);

                chunk.unload(true);

                if (i > maxgen) {
                    TaskMaster.syncTask(new ChunkGenerateTask(x, z, stopX, stopZ));
                    return;
                }

            }
        }


    }


}
