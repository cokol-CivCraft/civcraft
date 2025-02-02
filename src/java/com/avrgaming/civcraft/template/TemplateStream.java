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
package com.avrgaming.civcraft.template;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.SimpleBlock;
import org.bukkit.Location;
import org.bukkit.Material;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TemplateStream {
    /*
     * TemplateStream
     *
     * Rather than load an entire template into memory, only request particular parts of a template at
     * any one time. This is useful for reducing total memory usage. Applications are as follows:
     * 		-) Structure floating validation
     * 		       We only want the bottom layer (aka the shape) of a template when attempting to
     *             perform structure floating validation.
     *      -) Post-Build/Reload tasks
     *             When the server starts, we run a the post build task which reloads the template
     *             for each structure in the server. All of the command blocks are found and re-applied
     *             at this time. There is no need to load the entire template. Just the command blocks.
     */

    /* source file we're going to be grabbing data from. */
    private String source;

    /* Sizes loaded from the top of the template file. */
    private int sizeX;
    private int sizeY;
    private int sizeZ;

    /*
     * Container that holds any/all simple block objects we've retrieved from the file.
     * Rather than clear and create new simple blocks, we'd rather overwrite old ones
     * that way we're not pressing the allocator/garbage collector all the time.
     */
    ArrayList<SimpleBlock> blocks = new ArrayList<>();
    private int currentBlockCount = 0;

    private File sourceFile = null;

    public TemplateStream(String filepath) throws IOException {
        this.setSource(filepath);
    }

    private SimpleBlock getSimpleBlockFromLine(String line) {
        String[] locTypeSplit = line.split(",");
        String location = locTypeSplit[0];
        String type = locTypeSplit[1];

        /* Parse out location */
        String[] locationSplit = location.split(":");
        int blockX = Integer.parseInt(locationSplit[0]);
        int blockY = Integer.parseInt(locationSplit[1]);
        int blockZ = Integer.parseInt(locationSplit[2]);

        /* Parse out type */
        String[] typeSplit = type.split(":");


        SimpleBlock block;
        if (currentBlockCount < blocks.size()) {
            /* Get an already allocated simple block. */
            block = blocks.get(currentBlockCount);
        } else {
            /* allocate a new one and add to cache. */
            block = new SimpleBlock(Material.getMaterial(Integer.parseInt(typeSplit[0])), Integer.parseInt(typeSplit[1]));
            blocks.add(block);
        }
        currentBlockCount++;

        block.x = blockX;
        block.y = blockY;
        block.z = blockZ;
        return block;
    }

    /*
     * Loads all blocks on y-layer y and returns them in a list.
     */
    public List<SimpleBlock> getBlocksForLayer(int y) throws IOException {
        if (y > sizeY) {
            throw new IllegalArgumentException();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile))) {
            LinkedList<SimpleBlock> returnBlocks = new LinkedList<>();
            /* Read past the starting size line. */
            reader.readLine();

            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                String[] locationSplit = line.split(",")[0].split(":");

                if (Integer.parseInt(locationSplit[1]) == y) {
                    returnBlocks.add(getSimpleBlockFromLine(line));
                }
            }
            return returnBlocks;
        }
    }

    /*
     * Loads entire template into simple blocks
     */
    @SuppressWarnings("unused")
    public List<SimpleBlock> getTemplateBlocks() throws IOException {

        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile))) {
            LinkedList<SimpleBlock> returnBlocks = new LinkedList<>();
            /* Read past the starting size line. */
            reader.readLine();

            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                returnBlocks.add(getSimpleBlockFromLine(line));
            }
            return returnBlocks;
        }
    }

    /*
     * builds the contents of the blocks array centered on our location.
     */
    public void debugBuildBlocksHere(Location location) {

        for (SimpleBlock block : blocks) {
            BlockCoord bcoord = new BlockCoord(location);
            bcoord.setX(bcoord.getX() + block.x);
            bcoord.setY(bcoord.getY() + block.y);
            bcoord.setZ(bcoord.getZ() + block.z);
            bcoord.getBlock().setType(block.getType());
            bcoord.getBlock().setData((byte) block.getData());
        }

    }


    public String getSource() {
        return source;
    }

    public void setSource(String filepath) throws IOException {
        this.source = filepath;
        sourceFile = new File(filepath);
        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile))) {
            // Read first line and get size.
            String line = reader.readLine();
            if (line == null) {
                throw new IOException(CivSettings.localize.localizedString("template_invalidFile") + " " + filepath);
            }

            String[] split = line.split(";");
            sizeX = Integer.parseInt(split[0]);
            sizeY = Integer.parseInt(split[1]);
            sizeZ = Integer.parseInt(split[2]);
        }
    }

    public int getSizeX() {
        return sizeX;
    }

    public void setSizeX(int sizeX) {
        this.sizeX = sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public void setSizeY(int sizeY) {
        this.sizeY = sizeY;
    }

    public int getSizeZ() {
        return sizeZ;
    }

    public void setSizeZ(int sizeZ) {
        this.sizeZ = sizeZ;
    }
}
