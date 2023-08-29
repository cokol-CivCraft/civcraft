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
import org.bukkit.ChatColor;
import org.bukkit.Material;

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

    private static String getHP() {
        String hpCFG;
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
        return switch (i) {
            case ACACIA_DOOR, DARK_OAK_DOOR, OAK_DOOR, BIRCH_DOOR, IRON_DOOR, JUNGLE_DOOR, SPRUCE_DOOR -> true;
            default -> false;
        };
    }

    public static String getStringForBar(TaskType type, double HP, int maxHP) {
        String s;
        String open = "<";
        String close = ">";
        int tenPercentOfMax = maxHP / 10;
        int sizeOfChars = (int) (HP / tenPercentOfMax);
        int emptyChars = 10 - sizeOfChars;
        switch (type) {
            case STRUCTURE -> {
                open = "❰";
                close = "❱";
            }
            case CONTROL -> {
                open = "⟪";
                close = "⟫";
            }
            case PLAYER -> {
                open = "﴾";
                close = "﴿";
            }
            default -> {
            }
            case TECH -> {
                open = "〖";
                close = "〗";
            }
            case STRUCTUREBUILD -> {
                open = "⧼";
                close = "⧽";
            }
            case WONDERBUILD -> {
                open = "⸨";
                close = "⸩";
            }
        }
        s = paintString(open, close, sizeOfChars, emptyChars);

        return s;
    }

    private static String paintString(String open, String close, int full, int empty) {
        StringBuilder s = new StringBuilder(ChatColor.GRAY + open);
        s.append(ChatColor.RED);
        for (; full > 0; full--) {
            s.append(getHP());
        }
        s.append(ChatColor.WHITE);
        for (; empty > 0; empty--) {
            s.append(getHP());
        }
        s.append(ChatColor.GRAY).append(close);
        return s.toString();
    }

    public static String getDisplayName(Material material) {
        return switch (material) {
            case GOLD_ORE -> "Gold Ore";
            case IRON_ORE -> "Iron Ore";
            case IRON_INGOT -> "Iron";
            case GOLD_INGOT -> "Gold";
            default -> "Unknown_Id";
        };
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

            if ((nextBs.getType() == Material.MELON &&
                    bs.getType() == Material.MELON_STEM) ||
                    (nextBs.getType() == Material.PUMPKIN &&
                            bs.getType() == Material.PUMPKIN_STEM)) {
                return false;
            }
        }
        return hasAir;
    }

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
        return switch (bs.getType()) {
            case WHEAT, CARROT, POTATO -> bs.getData() != 0x7;
            case NETHER_WART -> bs.getData() != 0x3;
            case COCOA -> (bs.getData() & 0xC) != 0x8;
            case MELON_STEM, PUMPKIN_STEM -> canGrowFromStem(bs);
            default -> false;
        };

    }
}
