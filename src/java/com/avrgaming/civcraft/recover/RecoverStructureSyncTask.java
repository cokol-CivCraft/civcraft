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
package com.avrgaming.civcraft.recover;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.util.SimpleBlock.Type;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.util.ArrayList;

public class RecoverStructureSyncTask implements Runnable {

    ArrayList<Structure> structures;
    CommandSender sender;


    public RecoverStructureSyncTask(CommandSender sender, ArrayList<Structure> structs) {
        this.structures = structs;
        this.sender = sender;
    }

    public void repairStructure(Structure struct) {
        // Repairs a structure, one block at a time. Does not bother repairing
        // command blocks since they will be re-populated in onLoad() anyway.

        // Template is already loaded.
        Template tpl;
        try {
            //tpl.load_template(struct.getSavedTemplatePath());
            tpl = Template.getTemplate(struct.getSavedTemplatePath(), null);
        } catch (IOException | CivException e) {
            e.printStackTrace();
            return;
        }

        Block cornerBlock = struct.getCorner().getBlock();
        for (int x = 0; x < tpl.size_x; x++) {
            for (int y = 0; y < tpl.size_y; y++) {
                for (int z = 0; z < tpl.size_z; z++) {
                    Block nextBlock = cornerBlock.getRelative(x, y, z);

//					if (RecoverStructuresAsyncTask.ignoreBlocks.contains(nextBlock.getTypeId())) {
//						continue;
//					}
//					
//					if (RecoverStructuresAsyncTask.ignoreBlocks.contains(tpl.blocks[x][y][z].getType())) {
//						continue;
//					}

                    if (tpl.blocks[x][y][z].specialType != Type.NORMAL) {
                        continue;
                    }

                    if (nextBlock.getType() != Material.BEDROCK) {
                        if (tpl.blocks[x][y][z].isAir()) {
                            continue;
                        }
                    }

                    try {
                        if (nextBlock.getType() != tpl.blocks[x][y][z].getType()) {
                            nextBlock.getState().setData(tpl.blocks[x][y][z].getMaterialData());
                        }
                    } catch (Exception e) {
                        CivLog.error(e.getMessage());
                    }
                }
            }
        }

    }

    @Override
    public void run() {
        for (Structure struct : this.structures) {
            CivMessage.send(sender, CivSettings.localize.localizedString("structureRepairStart") + " " + struct.getDisplayName() + " @ " + ChatColor.YELLOW + struct.getCorner());
            repairStructure(struct);
        }

        CivMessage.send(sender, CivSettings.localize.localizedString("structureRepairComplete"));
    }


}
