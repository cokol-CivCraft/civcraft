/*************************************************************************
 * 
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

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import com.avrgaming.civcraft.structure.Buildable;

public class SimpleBlock {
	
	//public static final int SIGN = 1;
	//public static final int CHEST = 2;
	//public static final int SIGN_LITERAL = 3;
	
	public enum Type {
		NORMAL,
		COMMAND,
		LITERAL,
	}
	
	private Material material;
	private byte data;
	//public int special = 0;
//	public int special_id = -1;
	public int x;
	public int y;
	public int z;
	
	public Type specialType;
	public String command; 
	public String[] message = new String[4];
	public String worldname;
	public Buildable buildable;
	public Map<String, String> keyvalues = new HashMap<String, String>();
	
	/**
	 * Construct the block with its type.
	 *
	 * @param block
	 */
	    public SimpleBlock(Block block) {
	        this.x = block.getX();
	        this.y = block.getY();
	        this.z = block.getZ();
	        this.worldname = block.getWorld().getName();
            this.material = block.getType();
	        this.data = ItemManager.getData(block);
	        this.specialType = Type.NORMAL;
	    }
	    
	    public SimpleBlock(String hash, Material material, byte data) {
		    String[] split = hash.split(",");
			this.worldname = split[0];
			this.x = Integer.parseInt(split[1]);
			this.y = Integer.parseInt(split[2]);
			this.z = Integer.parseInt(split[3]);
			this.material = material;
			this.data = data;
	        this.specialType = Type.NORMAL;
	    }
	
	public String getKey() {
		return this.worldname+","+this.x+","+this.y+","+this.z;
	}
	
	public static String getKeyFromBlockCoord(BlockCoord coord) {
		return coord.getWorldname()+","+coord.getX()+","+coord.getY()+","+coord.getZ();
	}
	    
	/**
	 * Construct the block with its type and data.
	 *
	 * @param material
	 * @param data
	 */
	public SimpleBlock(Material material, int data) {
		this.material = material;
		this.data = (byte) data;
		this.specialType = Type.NORMAL;

	}

	public Material getType() {
		return material;
	}
	
	/**
	 * @param material the type to set
	 */
	public void setType(Material material) {
	    this.material = material;
	}
	@SuppressWarnings("unused")
	public void setTypeAndData(Material type, int data) {
		this.material =  type;
		this.data = (byte) data;
	}
	/**
	 * @return the data
	 */
	public int getData() {
	    return data;
	}
	
	/**
	 * @param data the data to set
	 */
	public void setData(int data) {
	    this.data = (byte) data;
	}
	
	/**
	 * Returns true if it's air.
	 *
	 * @return if air
	 */
	public boolean isAir() {
	    return material == Material.AIR;
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

}
