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
package com.avrgaming.civcraft.util;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.TownChunk;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class AsciiMap {

    private static final int width = 9;
    private static final int height = 40;

    public static List<String> getMapAsString(Location center) {
        ArrayList<String> out = new ArrayList<>();

        //	ChunkCoord[][] chunkmap = new ChunkCoord[width][height];
        ChunkCoord centerChunk = new ChunkCoord(center);

        /* Use the center to build a starting point. */
        ChunkCoord currentChunk = new ChunkCoord(center.getWorld().getName(),
                (centerChunk.getX() - (width / 2)),
                (centerChunk.getZ() - (height / 2)));

        int startX = currentChunk.getX();
        int startZ = currentChunk.getZ();

        out.add(CivMessage.buildTitle(CivSettings.localize.localizedString("Map")));

        //ChunkCoord currentChunk = new ChunkCoord(center);
        for (int x = 0; x < width; x++) {
            StringBuilder outRow = new StringBuilder("         ");
            for (int z = 0; z < height; z++) {
                String color = String.valueOf(ChatColor.WHITE);

                currentChunk = new ChunkCoord(center.getWorld().getName(),
                        startX + x, startZ + z);

                if (currentChunk.equals(centerChunk)) {
                    color = String.valueOf(ChatColor.YELLOW);
                }

                /* Try to see if there is a town chunk here.. */
                TownChunk tc = CivGlobal.getTownChunk(currentChunk);
                if (tc != null) {

                    if (color.equals(String.valueOf(ChatColor.WHITE))) {
                        if (tc.perms.getOwner() != null) {
                            color = String.valueOf(ChatColor.GREEN);
                        } else {
                            color = String.valueOf(ChatColor.RED);
                        }
                    }

                    outRow.append(color).append("T");
                } else {
                    outRow.append(color).append("-");
                }
            }
            out.add(outRow.toString());
        }


        return out;
    }

}
