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
            case SMOOTH_STONE_SLAB, OAK_SLAB, STONE_SLAB -> {
                simpleBlock.setType(Material.PURPUR_SLAB);
                simpleBlock.setBlockData(simpleBlock.getBlockData());
            }
            case SANDSTONE, RED_SANDSTONE, ICE, PACKED_ICE, SNOW_BLOCK, GRASS, DIRT, NETHERRACK, GRAVEL, SAND, SOUL_SAND, SLIME_BLOCK, CLAY, CACTUS -> {
                simpleBlock.setType(Material.OBSIDIAN);
                simpleBlock.setBlockData(simpleBlock.getBlockData());
            }
            case OAK_LEAVES -> {
                simpleBlock.setType(Material.SPONGE);
                simpleBlock.setBlockData(simpleBlock.getBlockData());
            }
            case ACACIA_STAIRS, BIRCH_STAIRS, SANDSTONE_STAIRS, QUARTZ_STAIRS, OAK_STAIRS, SPRUCE_STAIRS, DARK_OAK_STAIRS, RED_SANDSTONE_STAIRS, JUNGLE_STAIRS, PURPUR_STAIRS, BRICK_STAIRS, STONE_STAIRS, NETHER_BRICK_STAIRS -> {
                simpleBlock.setType(Material.COBBLESTONE_STAIRS);
                simpleBlock.setBlockData(simpleBlock.getBlockData());
            }
        }
        synchronized (this) {
            try {
                wait(9);
            } catch (InterruptedException e) {
                e.getCause();
            }
            simpleBlock.setType(m);
            simpleBlock.setBlockData(simpleBlock.getBlockData());
        }

    }
}
