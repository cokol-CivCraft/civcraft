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

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivTaskAbortException;
import com.avrgaming.civcraft.object.StructureChest;
import com.avrgaming.civcraft.structure.Windmill;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.MultiInventory;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Random;

public class WindmillPreProcessTask extends CivAsyncTask {

    private final ArrayList<ChunkSnapshot> snapshots;
    private final Windmill windmill;

    public WindmillPreProcessTask(Windmill windmill, ArrayList<ChunkSnapshot> snaphots) {
        this.snapshots = snaphots;
        this.windmill = windmill;
    }

    @Override
    public void run() {
        int plant_max = CivSettings.structureConfig.getInt("windmill.plant_max", 16);

        if (windmill.getCiv().hasTechnology("tech_machinery")) {
            plant_max *= 2;
        }

        /* Read in the source inventory's contents. Make sure we have seeds to plant. */
        MultiInventory source_inv = new MultiInventory();

        for (StructureChest src : windmill.getAllChestsById(0)) {
            try {
                this.syncLoadChunk(src.getCoord().getWorldname(), src.getCoord().getX(), src.getCoord().getZ());
                Inventory tmp;
                try {
                    tmp = this.getChestInventory(src.getCoord().getWorldname(), src.getCoord().getX(), src.getCoord().getY(), src.getCoord().getZ(), true);
                } catch (CivTaskAbortException e) {
                    //	e.printStackTrace();
                    return;
                }
                source_inv.addInventory(tmp);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }

        int breadCount = 0;
        int carrotCount = 0;
        int potatoCount = 0;
        for (ItemStack stack : source_inv.getContents()) {
            if (stack == null) {
                continue;
            }

            switch (stack.getType()) {
                case SEEDS -> breadCount += stack.getAmount();
                case CARROT_ITEM -> carrotCount += stack.getAmount();
                case POTATO_ITEM -> potatoCount += stack.getAmount();
                default -> {
                }
            }
        }

        /* If we've got nothing in the seed basket, nothing to plant! */
        if (breadCount == 0 && carrotCount == 0 && potatoCount == 0) {
            return;
        }

        /* Only try to plant as many crops as we have (or the max) */
        plant_max = Math.min((breadCount + carrotCount + potatoCount), plant_max);

        /* Read snapshots and find blocks that can be planted. */
        ArrayList<BlockCoord> blocks = new ArrayList<>();
        for (ChunkSnapshot snapshot : this.snapshots) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 255; y++) {


                        if (snapshot.getBlockType(x, y, z) == Material.SOIL) {
                            if (snapshot.getBlockType(x, y + 1, z) == Material.AIR) {
                                int blockx = (snapshot.getX() * 16) + x;
                                int blocky = y + 1;
                                int blockz = (snapshot.getZ() * 16) + z;

                                blocks.add(new BlockCoord(this.windmill.getCorner().getWorldname(),
                                        blockx, blocky, blockz));
                            }
                        }
                    }
                }
            }
        }

        ArrayList<BlockCoord> plantBlocks = new ArrayList<>();
        /* Select up to plant_max of these blocks to be planted. */
        Random rand = new Random();
        for (int i = 0; i < plant_max; i++) {
            if (blocks.isEmpty()) {
                break;
            }

            BlockCoord coord = blocks.get(rand.nextInt(blocks.size()));
            blocks.remove(coord);
            plantBlocks.add(coord);
        }

        // Fire off a sync task to complete the operation.
        TaskMaster.syncTask(new WindmillPostProcessSyncTask(windmill, plantBlocks,
                breadCount, carrotCount, potatoCount, source_inv));

    }

}
