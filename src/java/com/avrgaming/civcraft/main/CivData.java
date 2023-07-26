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
package com.avrgaming.civcraft.main;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidBlockLocation;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.util.BlockSnapshot;
import com.avrgaming.civcraft.util.CivColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

public class CivData {

    public static final int COARSE_DIRT = 1;
    public static final int PODZOL = 2;

    public static final int SALMON = 1;
    public static final int CLOWNFISH = 2;
    public static final int PUFFERFISH = 3;

    public static final byte DATA_SIGN_EAST = 0x5;
    public static final int DATA_SIGN_WEST = 0x4;
    public static final int DATA_SIGN_NORTH = 0x2;
    public static final int DATA_SIGN_SOUTH = 0x3;

    public static final byte DATA_WOOL_BLACK = 0xF;
    public static final byte PRISMARINE_BRICKS = 0x1;
    public static final byte DARK_PRISMARINE = 0x2;
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

    public static final byte DATA_WOOL_GREEN = 0x5;
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
    private static final String hp = "❤";
    private static String hpCFG;

    private static String getHP() {
        try {
            hpCFG = CivSettings.getString(CivSettings.civConfig, "global.health");
        } catch (InvalidConfiguration e) {
            hpCFG = hp;
            e.printStackTrace();
        }
        return hpCFG;
    }

    public enum TaskType {
        STRUCTURE, CONTROL, PLAYER, TECH, WONDERBUILD, STRUCTUREBUILD, NULL
    }

    public static boolean isDoor(Material i) {
        switch (i) {
            case ACACIA_DOOR:
            case DARK_OAK_DOOR:
            case WOOD_DOOR:
            case BIRCH_DOOR:
            case IRON_DOOR:
            case JUNGLE_DOOR:
            case SPRUCE_DOOR:
                return true;
            default:
                return false;
        }
    }public static String getStringForBar(TaskType type, double HP, int maxHP) {
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
                open = "⟪";
                close = "⟫";
                break;
            case PLAYER:
                open = "﴾";
                close = "﴿";
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
                open = "⸨";
                close = "⸩";
                break;
        }
        s = paintString(open, close, sizeOfChars, emptyChars);

        return s;
    }

    private static String paintString(String open, String close, int full, int empty) {
        StringBuilder s = new StringBuilder(CivColor.LightGray + open);
        s.append(CivColor.Rose);
        for (; full > 0; full--) {
            s.append(getHP());
        }
        s.append(CivColor.White);
        for (; empty > 0; empty--) {
            s.append(getHP());
        }
        s.append(CivColor.LightGray).append(close);
        return s.toString();
    }

    public static String getDisplayName(Material material) {
        switch (material) {
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
        int[][] offset = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
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


            if (nextBs.getType() == Material.AIR) {
                hasAir = true;
            }

            if ((nextBs.getType() == Material.MELON_BLOCK &&
                    bs.getType() == Material.MELON_STEM) ||
                    (nextBs.getType() == Material.PUMPKIN &&
                            bs.getType() == Material.PUMPKIN_STEM)) {
                return false;
            }
        }
        return hasAir;
    }

    public static boolean canGrowMushroom(BlockState blockState) {
        int[][] offset = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        boolean hasAir = false;
        for (int i = 0; i < 4; i++) {
            Block nextBlock = blockState.getBlock().getRelative(offset[i][0], 0, offset[i][1]);
            if (nextBlock.getType() == Material.AIR) {
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
		return (byte) (bs.getData() & 0xC) != 0x8;
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
        switch (bs.getType()) {
            case WHEAT:
            case CARROT:
            case POTATO:
                return bs.getData() != 0x7;

            case NETHER_WARTS:
                return bs.getData() != 0x3;

            case COCOA:
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
        switch (data) {
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

        switch (data) {
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


        System.out.println("Warning, unknown sign post direction:" + data);
        return CHEST_SOUTH;
    }

}
