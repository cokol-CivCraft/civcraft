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

import com.avrgaming.civcraft.structure.Buildable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;

import java.util.HashMap;
import java.util.Map;

public class SimpleBlock {
    public enum Type {
        NORMAL,
        COMMAND,
        LITERAL,
    }

    private final MaterialData material_data;
    public int x;
    public int y;
    public int z;

    public Type specialType;
    public String command;
    public String[] message = new String[4];
    public String worldname;
    public Buildable buildable;
    public Map<String, String> keyvalues = new HashMap<>();

    public SimpleBlock(Block block) {
        this.x = block.getX();
        this.y = block.getY();
        this.z = block.getZ();
        this.worldname = block.getWorld().getName();
        this.material_data = block.getState().getData();
        this.specialType = Type.NORMAL;
    }

    public SimpleBlock(String hash, Material material, byte data) {
        String[] split = hash.split(",");
        this.worldname = split[0];
        this.x = Integer.parseInt(split[1]);
        this.y = Integer.parseInt(split[2]);
        this.z = Integer.parseInt(split[3]);
        this.material_data = material.getNewData(data);
        this.specialType = Type.NORMAL;
    }

    public String getKey() {
        return this.worldname + "," + this.x + "," + this.y + "," + this.z;
    }

    public static String getKeyFromBlockCoord(BlockCoord coord) {
        return coord.getWorldname() + "," + coord.getX() + "," + coord.getY() + "," + coord.getZ();
    }

    public SimpleBlock(Material material, int data) {
        this(material.getNewData((byte) data));
    }

    public SimpleBlock(MaterialData material_data) {
        this.material_data = material_data;
        this.specialType = Type.NORMAL;

    }

    public Material getType() {
        return material_data.getItemType();
    }

    public int getData() {
        return material_data.getData();
    }

    public MaterialData getMaterialData() {
        return material_data;
    }

    public boolean isAir() {
        return getType() == Material.AIR;
    }

    public String getKeyValueString() {
        StringBuilder out = new StringBuilder();

        for (String key : keyvalues.keySet()) {
            out.append(key).append(":").append(keyvalues.get(key)).append(",");
        }

        return out.toString();
    }

    public Location getLocation() {
        return new Location(Bukkit.getWorld(this.worldname), this.x, this.y, this.z);
    }

    public void setTo(BlockCoord coord) {
        this.setTo(coord, false);
    }

    public void setTo(BlockCoord coord, boolean applyPhysics) {
        this.setTo(coord.getBlock(), applyPhysics);
    }

    public void setTo(Block block) {
        this.setTo(block, false);
    }

    public void setTo(Block block, boolean applyPhysics) {
        BlockState state = block.getState();
        state.setType(this.getType());
        state.setData(this.getMaterialData());
        state.update(true, applyPhysics);
    }

}
