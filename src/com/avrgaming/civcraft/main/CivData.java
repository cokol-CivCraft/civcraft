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
package com.avrgaming.civcraft.main;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.util.CivColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import com.avrgaming.civcraft.exception.InvalidBlockLocation;
import com.avrgaming.civcraft.util.BlockSnapshot;
import com.avrgaming.civcraft.util.ItemManager;

public class CivData {
	//TODO make this an enum
	public static final int WALL_SIGN = Material.WALL_SIGN.getId();
	public static final int SIGN = Material.SIGN_POST.getId();
	public static final int CHEST = Material.CHEST.getId();
	public static final int WOOD = Material.LOG.getId();
	public static final int PLANKS = Material.WOOD.getId();
	public static final int LEAF = Material.LEAVES.getId();

	public static final int DIRT = Material.DIRT.getId();

	public static final int COARSE_DIRT = 1;
	public static final int PODZOL = 2;
	
	public static final int SALMON = 1;
	public static final int CLOWNFISH = 2;
	public static final int PUFFERFISH = 3;
	
	public static final int GOLD_ORE = Material.GOLD_ORE.getId();
	public static final int IRON_ORE = Material.IRON_ORE.getId();
	public static final int IRON_INGOT = Material.IRON_INGOT.getId();
	public static final int GOLD_INGOT = Material.GOLD_INGOT.getId();
	public static final int WATER = Material.STATIONARY_WATER.getId();
	public static final int WATER_RUNNING = Material.WATER.getId();
	public static final int FENCE = Material.FENCE.getId();
	public static final int BEDROCK = Material.BEDROCK.getId();
	public static final int LAVA = Material.STATIONARY_LAVA.getId();
	public static final int LAVA_RUNNING = Material.LAVA.getId();
	public static final int COBBLESTONE = Material.COBBLESTONE.getId();
	public static final int MOSS_STONE = Material.MOSSY_COBBLESTONE.getId();
	public static final int EMERALD = Material.EMERALD.getId();
	public static final int DIAMOND = Material.DIAMOND.getId();
	public static final int GRAVEL = Material.GRAVEL.getId();
	public static final int AIR = Material.AIR.getId();
	public static final int REDSTONE_DUST = Material.REDSTONE.getId();
	public static final int WHEAT = Material.WHEAT.getId();
	public static final int PUMPKIN_STEM = Material.PUMPKIN_STEM.getId();
	public static final int MELON_STEM = Material.MELON_STEM.getId();
	public static final int CARROTS = Material.CARROT.getId();
	public static final int POTATOES =Material.POTATO.getId();
	public static final int NETHERWART = Material.NETHER_WARTS.getId();
	public static final int COCOAPOD = Material.COCOA.getId();
	public static final int FARMLAND = Material.SOIL.getId();
	public static final int MELON = Material.MELON_BLOCK.getId();
	public static final int PUMPKIN = Material.PUMPKIN.getId();
	public static final int ROTTEN_FLESH = Material.ROTTEN_FLESH.getId();
	public static final int ENDER_CHEST = Material.ENDER_CHEST.getId();
	public static final int BEACON = Material.BEACON.getId();
	public static final int GUNPOWDER = Material.SULPHUR.getId();
	
	public static final byte DATA_SIGN_EAST = 0x5;
	public static final int DATA_SIGN_WEST = 0x4;
	public static final int DATA_SIGN_NORTH = 0x2;
	public static final int DATA_SIGN_SOUTH = 0x3;

	public static final int EMERALD_BLOCK = Material.EMERALD_BLOCK.getId();
	public static final int GOLD_BLOCK = Material.GOLD_BLOCK.getId();
	public static final int DIAMOND_BLOCK = Material.DIAMOND_BLOCK.getId();
	public static final int REDSTONE_BLOCK = Material.REDSTONE_BLOCK.getId();
	public static final int LAPIS_BLOCK = Material.LAPIS_BLOCK.getId();
	public static final int COAL_BLOCK = Material.COAL_BLOCK.getId();
	public static final int WOOL = Material.WOOL.getId();
	public static final int SPONGE = Material.SPONGE.getId();
	public static final int HAY_BALE = Material.HAY_BLOCK.getId();
	public static final byte DATA_WOOL_BLACK = 0xF;
	public static final int OBSIDIAN = Material.OBSIDIAN.getId();
	public static final int FIRE = Material.FIRE.getId();
	public static final int FISHING_ROD = Material.FISHING_ROD.getId();
	public static final int FISH_RAW = Material.RAW_FISH.getId();
	public static final int BREAD = Material.BREAD.getId();
	public static final int REDSTONE_TORCH_OFF = Material.REDSTONE_TORCH_OFF.getId();
	public static final int STONE_BRICK = Material.SMOOTH_BRICK.getId();
	public static final int PRISMARINE = Material.PRISMARINE.getId();
	public static final byte PRISMARINE_BRICKS = 0x1;
	public static final byte DARK_PRISMARINE = 0x2;
	public static final int SNOW = Material.SNOW_BLOCK.getId();
	public static final int PACKED_ICE = Material.PACKED_ICE.getId();
	public static final int SANDSTONE = Material.SANDSTONE.getId();
	public static final byte CHISELED_SANDSTONE = 0x1;
	public static final byte SMOOTH_SANDSTONE = 0x2;
	
	public static final byte CHEST_NORTH = 0x2;
	public static final byte CHEST_SOUTH = 0x3;
	public static final byte CHEST_WEST = 0x4;
	public static final byte CHEST_EAST = 0x5;
	
	public static final byte SIGNPOST_NORTH = 0x8;
	public static final byte SIGNPOST_SOUTH = 0x0;
	public static final byte SIGNPOST_WEST = 0x4;
	public static final byte SIGNPOST_EAST = 0xC;
	public static final int BREAD_SEED = Material.SEEDS.getId();
	public static final int CARROT_ITEM = Material.CARROT_ITEM.getId();
	public static final int POTATO_ITEM = Material.POTATO_ITEM.getId();
	
	public static final int LEATHER_HELMET = Material.LEATHER_HELMET.getId();
	public static final int LEATHER_CHESTPLATE = Material.LEATHER_CHESTPLATE.getId();
	public static final int LEATHER_LEGGINGS = Material.LEATHER_LEGGINGS.getId();
	public static final int LEATHER_BOOTS = Material.LEATHER_BOOTS.getId();

	public static final int IRON_HELMET = Material.IRON_HELMET.getId();
	public static final int IRON_CHESTPLATE = Material.IRON_CHESTPLATE.getId();
	public static final int IRON_LEGGINGS = Material.IRON_LEGGINGS.getId();
	public static final int IRON_BOOTS = Material.IRON_BOOTS.getId();
	
	public static final int DIAMOND_HELMET = Material.DIAMOND_HELMET.getId();
	public static final int DIAMOND_CHESTPLATE = Material.DIAMOND_CHESTPLATE.getId();
	public static final int DIAMOND_LEGGINGS = Material.DIAMOND_LEGGINGS.getId();
	public static final int DIAMOND_BOOTS = Material.DIAMOND_BOOTS.getId();
	
	public static final int GOLD_HELMET = Material.GOLD_HELMET.getId();
	public static final int GOLD_CHESTPLATE = Material.GOLD_CHESTPLATE.getId();
	public static final int GOLD_LEGGINGS = Material.GOLD_LEGGINGS.getId();
	public static final int GOLD_BOOTS = Material.GOLD_BOOTS.getId();
	
	public static final int CHAIN_HELMET = Material.CHAINMAIL_HELMET.getId();
	public static final int CHAIN_CHESTPLATE = Material.CHAINMAIL_CHESTPLATE.getId();
	public static final int CHAIN_LEGGINGS = Material.CHAINMAIL_LEGGINGS.getId();
	public static final int CHAIN_BOOTS = Material.CHAINMAIL_BOOTS.getId();
	public static final int WOOD_SWORD = Material.WOOD_SWORD.getId();
	public static final int STONE_SWORD = Material.STONE_SWORD.getId();
	public static final int IRON_SWORD = Material.IRON_SWORD.getId();
	public static final int DIAMOND_SWORD = Material.DIAMOND_SWORD.getId();
	public static final int GOLD_SWORD = Material.GOLD_SWORD.getId();
	
	public static final int WOOD_AXE = Material.WOOD_AXE.getId();
	public static final int STONE_AXE = Material.STONE_AXE.getId();
	public static final int IRON_AXE = Material.IRON_AXE.getId();
	public static final int DIAMOND_AXE = Material.DIAMOND_AXE.getId();
	public static final int GOLD_AXE = Material.GOLD_AXE.getId();
	
	public static final int WOOD_SHOVEL = Material.WOOD_SPADE.getId();
	public static final int STONE_SHOVEL = Material.STONE_SPADE.getId();
	public static final int IRON_SHOVEL = Material.IRON_SPADE.getId();
	public static final int DIAMOND_SHOVEL = Material.DIAMOND_SPADE.getId();
	public static final int GOLD_SHOVEL = Material.GOLD_SPADE.getId();
	
	public static final int WOOD_PICKAXE = Material.WOOD_PICKAXE.getId();
	public static final int STONE_PICKAXE = Material.STONE_PICKAXE.getId();
	public static final int IRON_PICKAXE = Material.IRON_PICKAXE.getId();
	public static final int DIAMOND_PICKAXE = Material.DIAMOND_PICKAXE.getId();
	public static final int GOLD_PICKAXE = Material.GOLD_PICKAXE.getId();
	public static final byte DATA_WOOL_GREEN = 0x5;
	public static final int COAL = Material.COAL.getId();
	public static final int WOOD_DOOR = Material.WOOD_DOOR.getId();
	public static final int IRON_DOOR = Material.IRON_DOOR.getId();
	public static final int SPRUCE_DOOR = Material.SPRUCE_DOOR.getId();
	public static final int BIRCH_DOOR = Material.BIRCH_DOOR.getId();
	public static final int JUNGLE_DOOR = Material.JUNGLE_DOOR.getId();
	public static final int ACACIA_DOOR = Material.ACACIA_DOOR.getId();
	public static final int DARK_OAK_DOOR = Material.DARK_OAK_DOOR.getId();
	public static final int NETHERRACK = Material.NETHERRACK.getId();
	public static final int BOW = Material.BOW.getId();
	public static final int IRON_BLOCK = Material.IRON_BLOCK.getId();
	public static final int COBWEB = Material.WEB.getId();
	public static final int STONE = Material.STONE.getId();
	public static final int GRANITE = 1;
	public static final int POLISHED_GRANITE = 2;
	public static final int DIORITE = 3;
	public static final int POLISHED_DIORITE = 4;
	public static final int ANDESITE = 5;
	public static final int POLISHED_ANDESITE = 6;
	
	
	public static final short MUNDANE_POTION_DATA = 8192;
	public static final short MUNDANE_POTION_EXT_DATA = 64;
	public static final short THICK_POTION_DATA = 32;
	public static final short DATA_WOOL_RED = 14;
	public static final int DATA_WOOL_WHITE = 0;
	public static final int GOLDEN_APPLE = Material.GOLDEN_APPLE.getId();
	public static final int TNT = Material.TNT.getId();
	private static final String hp = "❤";
	private static String hpCFG;

	private static String getHP() {
		try {
			hpCFG = CivSettings.getString(CivSettings.civConfig, "global.health");
		} catch (InvalidConfiguration e){
			hpCFG = hp;
			e.printStackTrace();
		}
		return hpCFG;
	}
	public enum TaskType {
		STRUCTURE, CONTROL, PLAYER, TECH, WONDERBUILD, STRUCTUREBUILD, NULL
	}

	public static String getStringForBar(TaskType type, double HP, int maxHP) {
		String s;
		String open = "<";
		String close = ">";
		int tenPercentOfMax = maxHP / 10;
		int sizeOfChars = (int) (HP / tenPercentOfMax);
		int emptyChars = 10 - sizeOfChars;
		switch (type) {
			case STRUCTURE:
				open = "❰";
				close = "❱";
				break;
			case CONTROL:
				open ="⟪";
				close ="⟫";
				break;
			case PLAYER:
				open ="﴾";
				close ="﴿";
				break;
			case NULL:
			default:
				break;
			case TECH:
				open = "〖";
				close = "〗";
				break;
			case STRUCTUREBUILD:
				open = "⧼";
				close = "⧽";
				break;
			case WONDERBUILD:
				open ="⸨";
				close ="⸩";
				break;
		}
		s = paintString(open, close, sizeOfChars, emptyChars);

		return s;
	}
	private static String paintString(String open, String close, int full, int empty) {
		String s = CivColor.LightGray + open;
		s += CivColor.Rose;
		for (; full > 0 ; full--) {
			s += getHP();
		}
		s += CivColor.White;
		for (; empty > 0 ; empty--) {
			s += getHP();
		}
		s += CivColor.LightGray + close;
		return s;
	}

    public static String getDisplayName(int id) {
		switch (id) {
			case GOLD_ORE:
				return "Gold Ore";
			case IRON_ORE:
				return "Iron Ore";
			case IRON_INGOT:
				return "Iron";
			case GOLD_INGOT:
				return "Gold";
			default:
				return "Unknown_Id";
		}
	}
	
	
	public static boolean canGrowFromStem(BlockSnapshot bs) {
		int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
		boolean hasAir = false;
		for (int i = 0; i < 4; i++) {
			BlockSnapshot nextBs;
			try {
				nextBs = bs.getRelative(offset[i][0], 0, offset[i][1]);
			} catch (InvalidBlockLocation e) {
				/* 
				 * The block is on the edge of this farm plot. 
				 * it _could_ grow but lets not say it can to be safe.
				 */
				return false;
			}
			//Block nextBlock = blockState.getBlock().getRelative(offset[i][0], 0, offset[i][1]);
			//int nextType = snapshot.getBlockData(arg0, arg1, arg2)
			
			
			if (nextBs.getTypeId() == CivData.AIR) {
				hasAir = true;
			}
			
			if ((nextBs.getTypeId() == CivData.MELON && 
					bs.getTypeId() == CivData.MELON_STEM) ||
					(nextBs.getTypeId() == CivData.PUMPKIN &&
							bs.getTypeId() == CivData.PUMPKIN_STEM)) {
				return false;
			}
		}
		return hasAir;
	}

	public static boolean canGrowMushroom(BlockState blockState) {
		int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
		boolean hasAir = false;
		for (int i = 0; i < 4; i++) {
			Block nextBlock = blockState.getBlock().getRelative(offset[i][0], 0, offset[i][1]);
			if (nextBlock.getTypeId() == CivData.AIR) {
				hasAir = true;
			}
		}
		return hasAir;
	}
	
//	public static boolean canGrowSugarcane(Block blockState) {
//		int total = 1; //include our block
//		Block nextBlock = blockState.getBlock();
//		// Get # of sugarcanes above us
//		//Using a for loop to prevent possible infinite loop
//		for (int i = 0; i <= Farm.MAX_SUGARCANE_HEIGHT; i++) {
//			nextBlock = nextBlock.getRelative(0, 1, 0);
//			if (nextBlock.getTypeId() == CivData.SUGARCANE) {
//				total++;
//			} else {
//				break;
//			}
//		}
//		
//		nextBlock = blockState.getBlock();
//		// Get # of sugarcanes below us
//		for (int i = 0; i <= Farm.MAX_SUGARCANE_HEIGHT; i++) {
//			nextBlock = nextBlock.getRelative(0, -1, 0);
//			if (nextBlock.getTypeId() == CivData.SUGARCANE) {
//				total++;
//			} else {
//				break;
//			}
//		}
//		
//		// Compare total+1 with max height.
//		if (total < Farm.MAX_SUGARCANE_HEIGHT) {
//			return true;
//		}
//
//		return false;
//	}
	
	public static boolean canCocoaGrow(BlockSnapshot bs) {
		byte bits = (byte) (bs.getData() & 0xC);
		if (bits == 0x8)
			return false;
		return true;
	}
	
	public static byte getNextCocoaValue(BlockSnapshot bs) {
		byte bits = (byte) (bs.getData() & 0xC);
		if (bits == 0x0)
			return 0x4;
		else if (bits == 0x4)
			return 0x8;
		else
			return 0x8;
	}
	
	public static boolean canGrow(BlockSnapshot bs) {
		switch (bs.getTypeId()) {
		case WHEAT:
		case CARROTS:		
		case POTATOES:		
			if (bs.getData() == 0x7) {
				return false;
			}
			return true;
		
		case NETHERWART:
			if (bs.getData() == 0x3) {
				return false;
			}
			return true;
		
		case COCOAPOD:
			return canCocoaGrow(bs);
		
		case MELON_STEM:
		case PUMPKIN_STEM:
			return canGrowFromStem(bs);
		
		//case REDMUSHROOM:
		//case BROWNMUSHROOM:
		//	return canGrowMushroom(blockState);
			
		//case SUGARCANE:	
	//		return canGrowSugarcane(bs);
		}
		
		return false;
	}

	public static byte convertSignDataToDoorDirectionData(byte data) {
		switch(data) {
		case SIGNPOST_NORTH:
			return 0x1;
		case SIGNPOST_SOUTH:
			return 0x3;
		case SIGNPOST_EAST:
			return 0x2;
		case SIGNPOST_WEST:
			return 0x0;
		}
		
		return 0x0;
	}

	public static byte convertSignDataToChestData(byte data) {
		/* Chests are 
		 * 0x2: Facing north (for ladders and signs, attached to the north side of a block)
		 * 0x3: Facing south
		 * 0x4: Facing west
		 * 0x5: Facing east
		 */
		
		/* Signposts are
		 * 0x0: south
			0x4: west
			0x8: north
			0xC: east
		 */
		
		switch(data) {
		case SIGNPOST_NORTH:
			return CHEST_NORTH;
		case SIGNPOST_SOUTH:
			return CHEST_SOUTH;
		case SIGNPOST_EAST:
			return CHEST_EAST;
		case SIGNPOST_WEST:
			return CHEST_WEST;
		}
		
		
//		switch (data) {
//		case 0x0:
//			return 0x3;
//		case 0x4:
//			return 0x4;
//		case 0x8:
//			return 0x2;
//		case 0xC:
//			return 0x5;
//		}
		
		
		System.out.println("Warning, unknown sign post direction:"+data);
		return CHEST_SOUTH;
	}
	
}
