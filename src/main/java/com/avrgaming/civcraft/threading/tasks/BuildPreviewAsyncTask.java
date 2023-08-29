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


import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;


public class BuildPreviewAsyncTask extends BukkitRunnable {
    /*
     * This task slow-builds a struct block-by-block based on the
     * town's hammer rate. This task is per-structure building and will
     * use the CivAsynTask interface to send synchronous requests to the main
     * thread to build individual blocks.
     */

    public final Template tpl;
    public final Block centerBlock;
    public final UUID playerUUID;
    private final int blocksPerStep;
    public final int period;
    private final Resident resident;
    private int block_num = 0;

    public BuildPreviewAsyncTask(Template t, Block center, UUID playerUUID) {
        tpl = t;
        centerBlock = center;
        this.playerUUID = playerUUID;
        resident = CivGlobal.getResidentViaUUID(playerUUID);
        this.blocksPerStep = 3;
        this.period = 3;
    }

    public Player getPlayer() throws CivException {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) {
            throw new CivException("Player offline");
        }
        return player;
    }

    @Override
    public void run() {
        for (int i = 0; i < blocksPerStep; i++) {
            int y = block_num / tpl.size_x / tpl.size_z;
            int z = block_num / tpl.size_x % tpl.size_z;
            int x = block_num % tpl.size_x;

            if (y >= tpl.size_y) {
                this.cancel();
                return;
            }

            Block block = centerBlock.getRelative(x, y, z);

            try {
                ItemManager.sendBlockChange(getPlayer(), block.getLocation(), tpl.blocks[x][y][z].getMaterialData());
            } catch (CivException e) {
                this.cancel();
                return;
            }
            resident.previewUndo.put(new BlockCoord(block.getLocation()), new SimpleBlock(block.getState().getData()));
            block_num++;
        }
    }


}