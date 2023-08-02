package com.avrgaming.civcraft.threading.tasks;

import com.avrgaming.civcraft.threading.CivAsyncTask;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class UpdateBlockUnderAttack extends CivAsyncTask {
    private final Block simpleBlock;

    public UpdateBlockUnderAttack(Block simpleBlock) {
        this.simpleBlock = simpleBlock;
    }

    @Override
    public void run() {
        Material m = simpleBlock.getType();
        byte by = simpleBlock.getData();
        switch (simpleBlock.getType()) {
            case STEP, WOOD_STEP, STONE_SLAB2 -> {
                simpleBlock.setType(Material.PURPUR_SLAB);
                simpleBlock.setData(by);
            }
            case DOUBLE_STEP, WOOD_DOUBLE_STEP, DOUBLE_STONE_SLAB2 -> {
                simpleBlock.setType(Material.PURPUR_DOUBLE_SLAB);
                simpleBlock.setData(by);
            }
            case SANDSTONE, RED_SANDSTONE, ICE, PACKED_ICE, SNOW_BLOCK, GRASS, DIRT, NETHERRACK, GRAVEL, SAND, SOUL_SAND, SLIME_BLOCK, CLAY, CACTUS -> {
                simpleBlock.setType(Material.OBSIDIAN);
                simpleBlock.setData(by);
            }
            case LEAVES, LEAVES_2 -> {
                simpleBlock.setType(Material.SPONGE);
                simpleBlock.setData(by);
            }
            case ACACIA_STAIRS, BIRCH_WOOD_STAIRS, SANDSTONE_STAIRS, QUARTZ_STAIRS, WOOD_STAIRS, SPRUCE_WOOD_STAIRS, DARK_OAK_STAIRS, RED_SANDSTONE_STAIRS, JUNGLE_WOOD_STAIRS, PURPUR_STAIRS, BRICK_STAIRS, SMOOTH_STAIRS, NETHER_BRICK_STAIRS -> {
                simpleBlock.setType(Material.COBBLESTONE_STAIRS);
                simpleBlock.setData(by);
            }
        }
        synchronized (this) {
            try {
                wait(9);
            } catch (InterruptedException e) {
                e.getCause();
            }
            simpleBlock.setType(m);
            simpleBlock.setData(by);
        }

    }
}
