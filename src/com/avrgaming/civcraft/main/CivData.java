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
	public static final int WALL_SIGN = 68;
	public static final int SIGN = 63;
	public static final int CHEST = 54;
	public static final int TRAPPED_CHEST = 146;
	public static final int WOOD = 17;
	public static final int PLANKS = 5;
	public static final int LEAF = 18;
	
	
	public static final byte DATA_OAK = 0;
	public static final byte DATA_PINE = 1;
	public static final byte DATA_BIRCH = 2;
	public static final byte DATA_JUNGLE = 3;

	public static final int DIRT = 3;

	public static final int COARSE_DIRT = 1;
	public static final int PODZOL = 2;
	
	public static final int SALMON = 1;
	public static final int CLOWNFISH = 2;
	public static final int PUFFERFISH = 3;
	
	public static final int GOLD_ORE = 14;
	public static final int IRON_ORE = 15;
	public static final int IRON_INGOT = 265;
	public static final int GOLD_INGOT = 266;
	public static final int WATER = 9;
	public static final int WATER_RUNNING = 8;
	public static final int FENCE = 85;
	public static final int BEDROCK = 7;
	public static final int RAILROAD = 66;
	public static final int LAVA = 11;
	public static final int LAVA_RUNNING = 10;
	public static final int COBBLESTONE = 4;
	public static final int MOSS_STONE = 48;
	public static final int EMERALD = 388;
	public static final int DIAMOND = 264;
	public static final int GRAVEL = 13;
	public static final int AIR = 0;
	public static final int DISPENSER = 23;
	public static final int REDSTONE_DUST = 331;
	public static final int WHEAT = 59;
	public static final int SUGARCANE = 83;
	public static final int PUMPKIN_STEM = 104;
	public static final int MELON_STEM = 105;
	public static final int CARROTS = 141;
	public static final int POTATOES = 142;
	public static final int NETHERWART = 115;
	public static final int COCOAPOD = 127;
	public static final int REDMUSHROOM = 39;
	public static final int BROWNMUSHROOM = 40;
	public static final int FARMLAND = 60;
	public static final int MELON = 103;
	public static final int PUMPKIN = 86;
	public static final int PUBLISHED_BOOK = 387;
	public static final int ROTTEN_FLESH = 367;
	public static final int TORCH = 50;
	public static final int WATER_BUCKET = 326;
	public static final int EMPTY_BUCKET = 325;
	public static final int ENDER_PEARL = 368;
	public static final int ENDER_CHEST = 130;
	public static final int BEACON = 138;
	public static final int GUNPOWDER = 289;
	
	public static final int MONSTER_EGG = 383;
	public static final String BOOK_UNDERLINE = "�n";
	public static final String BOOK_BOLD = "�l";
	public static final String BOOK_ITALIC = "�o";
	public static final String BOOK_NORMAL = "�r";
	
	public static final byte DATA_SIGN_EAST = 0x5;
	public static final int DATA_SIGN_WEST = 0x4;
	public static final int DATA_SIGN_NORTH = 0x2;
	public static final int DATA_SIGN_SOUTH = 0x3;
	
	public static final int ITEMFRAME = 389;
	public static final int EMERALD_BLOCK = 133;
	public static final int GOLD_BLOCK = 41;
	public static final int DIAMOND_BLOCK = 57;
	public static final int REDSTONE_BLOCK = 152;
	public static final int LAPIS_BLOCK = 22;
	public static final int COAL_BLOCK = 173;
	public static final int WOOL = 35;
	public static final int SPONGE = 19;
	public static final int HAY_BALE = 170;
	public static final byte DATA_WOOL_BLACK = 0xF;
	public static final int COOKED_FISH = 350;
	public static final int OBSIDIAN = 49;
	public static final int NETHER_WART_BLOCK = 214;
	public static final int FIRE = 51;
	public static final int FISHING_ROD = 346;
	public static final int FISH_RAW = 349;
	public static final int MUTTON_RAW = 423;
	public static final int BREAD = 297;
	public static final int GLOWSTONE = 89;
	public static final int DYE = 351;
	public static final int REDSTONE_TORCH_OFF = 75;
	public static final int STONE_BRICK = 98;
	public static final int PRISMARINE = 168;
	public static final byte PRISMARINE_BRICKS = 0x1;
	public static final byte DARK_PRISMARINE = 0x2;
	public static final int ICE = 79;
	public static final int SNOW = 80;
	public static final int PACKED_ICE = 174;
	public static final int SANDSTONE = 24;
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
	public static final int BREAD_SEED = 295;
	public static final int CARROT_ITEM = 391;
	public static final int POTATO_ITEM = 392;
	
	public static final int LEATHER_HELMET = 298;
	public static final int LEATHER_CHESTPLATE = 299;
	public static final int LEATHER_LEGGINGS = 300;
	public static final int LEATHER_BOOTS = 301;

	public static final int IRON_HELMET = 306;
	public static final int IRON_CHESTPLATE = 307;
	public static final int IRON_LEGGINGS = 308;
	public static final int IRON_BOOTS = 309;
	
	public static final int DIAMOND_HELMET = 310;
	public static final int DIAMOND_CHESTPLATE = 311;
	public static final int DIAMOND_LEGGINGS = 312;
	public static final int DIAMOND_BOOTS = 313;
	
	public static final int GOLD_HELMET = 314;
	public static final int GOLD_CHESTPLATE = 315;
	public static final int GOLD_LEGGINGS = 316;
	public static final int GOLD_BOOTS = 317;
	
	public static final int CHAIN_HELMET = 302;
	public static final int CHAIN_CHESTPLATE = 303;
	public static final int CHAIN_LEGGINGS = 304;
	public static final int CHAIN_BOOTS = 305;
	public static final int WOOD_SWORD = 268;
	public static final int STONE_SWORD = 272;
	public static final int IRON_SWORD = 267;
	public static final int DIAMOND_SWORD = 276;
	public static final int GOLD_SWORD = 283;
	
	public static final int WOOD_AXE = 271;
	public static final int STONE_AXE = 275;
	public static final int IRON_AXE = 258;
	public static final int DIAMOND_AXE = 279;
	public static final int GOLD_AXE = 286;
	
	public static final int WOOD_SHOVEL = 269;
	public static final int STONE_SHOVEL = 273;
	public static final int IRON_SHOVEL = 256;
	public static final int DIAMOND_SHOVEL = 277;
	public static final int GOLD_SHOVEL = 284;
	
	public static final int WOOD_PICKAXE = 270;
	public static final int STONE_PICKAXE = 274;
	public static final int IRON_PICKAXE = 257;
	public static final int DIAMOND_PICKAXE = 278;
	public static final int GOLD_PICKAXE = 285;
	public static final byte DATA_WOOL_GREEN = 0x5;
	public static final Integer LADDER = 65;
	public static final int COAL = ItemManager.getId(Material.COAL);
	public static final int WOOD_DOOR = 64;
	public static final int IRON_DOOR = 71;
	public static final int SPRUCE_DOOR = 193;
	public static final int BIRCH_DOOR = 194;
	public static final int JUNGLE_DOOR = 195;
	public static final int ACACIA_DOOR = 196;
	public static final int DARK_OAK_DOOR = 197;
	public static final int NETHERRACK = 87;
	public static final int BOW = 261;
	public static final int ANVIL = 145;
	public static final int IRON_BLOCK = 42;
	public static final int COBWEB = 30;
	public static final int STONE = 1;
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
	public static final int GOLDEN_APPLE = 322;
	public static final int TNT = 46;
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
