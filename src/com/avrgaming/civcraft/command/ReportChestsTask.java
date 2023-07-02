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
package com.avrgaming.civcraft.command;

import java.util.LinkedList;
import java.util.Queue;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Furnace;
import org.bukkit.block.Hopper;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class ReportChestsTask implements Runnable {
	public Queue<ChunkCoord> coords = new LinkedList<ChunkCoord>();
	CommandSender sender;
	
	public ReportChestsTask(CommandSender sender, Queue<ChunkCoord> coords) {
		this.coords = coords;
		this.sender = sender;
	}

	private int countItem(Inventory inv, int id) {
		int total = 0;
		for (ItemStack stack : inv.all(ItemManager.getMaterial(id)).values()) {
			total += stack.getAmount();
		}
		
		return total;
	}
	
	@Override
	public void run() {
		ChunkCoord coord = coords.poll();
		if (coord == null) {
			CivMessage.send(sender, "Done.");
			return;
		}
		Chunk chunk = coord.getChunk();
		
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 256; y++) {
				for (int z = 0; z < 16; z++) {
					Block block = chunk.getBlock(x, y, z);
					Inventory inv = null;
					if (block.getState() instanceof Chest) {
						inv = ((Chest)block.getState()).getBlockInventory();
					} else if (block.getState() instanceof Furnace) {
						inv = ((Furnace)block.getState()).getInventory();
					} else if (block.getState() instanceof Hopper) {
						inv = ((Hopper)block.getState()).getInventory();
					}
					
					if (inv != null) {
						BlockCoord bcoord = new BlockCoord(coord.getWorldname(), (coord.getX() << 4)+x, 
								y, (coord.getZ() << 4)+z);

                        int diamondBlocks = countItem(inv, Material.DIAMOND_BLOCK.getId());
                        int diamonds = countItem(inv, Material.DIAMOND.getId());
                        int goldBlocks = countItem(inv, Material.GOLD_BLOCK.getId());
                        int gold = countItem(inv, Material.GOLD_INGOT.getId());
                        int emeraldBlocks = countItem(inv, Material.EMERALD_BLOCK.getId());
                        int emeralds = countItem(inv, Material.EMERALD.getId());
                        int diamondOre = countItem(inv, Material.DIAMOND_ORE.getId());
                        int goldOre = countItem(inv, Material.GOLD_ORE.getId());
                        int emeraldOre = countItem(inv, Material.EMERALD_ORE.getId());
						
						String out = block.getType().name()+": "+CivColor.LightPurple+bcoord+CivColor.White+" DB:"+diamondBlocks+" EB:"+emeraldBlocks+" GB:"+goldBlocks+" D:"+
								diamonds+" E:"+emeralds+" G:"+gold+" DO:"+diamondOre+" EO:"+emeraldOre+" GO:"+goldOre;
						if (diamondBlocks != 0 || diamonds != 0 || goldBlocks != 0 || gold != 0 || emeraldBlocks != 0 
								|| emeralds != 0 || diamondOre != 0 || goldOre != 0 || emeraldOre != 0) {	
							CivMessage.send(sender, out);
							CivLog.info("REPORT: "+out);
						}
						inv = null;
					}
				}
			}
		}
		
		for (Entity e : chunk.getEntities()) {
			Inventory inv = null;
			
			if (e.getType() == EntityType.MINECART_CHEST) {
				StorageMinecart chest = (StorageMinecart)e;
				inv = chest.getInventory();
			}
			
			if (e.getType() == EntityType.MINECART_HOPPER) {
				HopperMinecart chest = (HopperMinecart)e;
				inv = chest.getInventory();
			}
					
			if (inv != null) {
				BlockCoord bcoord = new BlockCoord(e.getLocation());

                int diamondBlocks = countItem(inv, Material.DIAMOND_BLOCK.getId());
                int diamonds = countItem(inv, Material.DIAMOND.getId());
                int goldBlocks = countItem(inv, Material.GOLD_BLOCK.getId());
                int gold = countItem(inv, Material.GOLD_INGOT.getId());
                int emeraldBlocks = countItem(inv, Material.EMERALD_BLOCK.getId());
                int emeralds = countItem(inv, Material.EMERALD.getId());
                int diamondOre = countItem(inv, Material.DIAMOND_ORE.getId());
                int goldOre = countItem(inv, Material.GOLD_ORE.getId());
                int emeraldOre = countItem(inv, Material.EMERALD_ORE.getId());
				
				String out =  e.getType().name()+": "+CivColor.LightPurple+bcoord+CivColor.White+" DB:"+diamondBlocks+" EB:"+emeraldBlocks+" GB:"+goldBlocks+" D:"+
						diamonds+" E:"+emeralds+" G:"+gold+" DO:"+diamondOre+" EO:"+emeraldOre+" GO:"+goldOre;
				if (diamondBlocks != 0 || diamonds != 0 || goldBlocks != 0 || gold != 0 || emeraldBlocks != 0 
						|| emeralds != 0 || diamondOre != 0 || goldOre != 0 || emeraldOre != 0) {
					CivMessage.send(sender, out);
					CivLog.info("REPORT: "+out);
				}
			}
		}
		
		TaskMaster.syncTask(new ReportChestsTask(sender, coords), 5);
	}
}
