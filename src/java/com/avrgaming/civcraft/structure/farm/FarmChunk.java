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
package com.avrgaming.civcraft.structure.farm;

import com.avrgaming.civcraft.components.ActivateOnBiome;
import com.avrgaming.civcraft.components.Component;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidBlockLocation;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Farm;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.BlockSnapshot;
import com.avrgaming.civcraft.util.ChunkCoord;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class FarmChunk {
    private Town town;
    private Structure struct;
    private ChunkCoord coord;
    public ChunkSnapshot snapshot;

    /* Populated Asynchronously, Integer represents last data value at that location.. may or may not be useful. */
    public ArrayList<BlockCoord> cropLocationCache = new ArrayList<>();
    public ReentrantLock lock = new ReentrantLock();

    private ArrayList<BlockCoord> lastGrownCrops = new ArrayList<>();
    private LinkedList<GrowBlock> growBlocks;
    private Date lastGrowDate;
    private int lastGrowTickCount;
    private double lastChanceForLast;
    private int lastRandomInt;
    private int missedGrowthTicks;
    private int missedGrowthTicksStat;

    String biomeName;

    public FarmChunk(Chunk c, Town t, Structure struct) {
        this.town = t;
        this.struct = struct;
        this.coord = new ChunkCoord(c);
        biomeName = coord.getChunk().getBlock(8, 64, 8).getBiome().name();
    }

    public Chunk getChunk() {
        return this.coord.getChunk();
    }

    public Town getTown() {
        return town;
    }

    public void setTown(Town town) {
        this.town = town;
    }

    public Structure getStruct() {
        return struct;
    }

    public Farm getFarm() {
        return (Farm) struct;
    }

    public void setStruct(Structure struct) {
        this.struct = struct;
    }

    public boolean isHydrated(Block block) {
        Block beneath = block.getRelative(0, -1, 0);

        if (beneath != null) {
            if (beneath.getType() == Material.SOIL) {
                return beneath.getData() != 0x0;
            }
        }
        return false;
    }

    public int getLightLevel(Block block) {
        return block.getLightLevel();
    }

    public void spawnMelonOrPumpkin(BlockSnapshot bs, BlockCoord growMe, CivAsyncTask task) {
        //search for a free spot
        int[][] offset = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        BlockSnapshot freeBlock = null;
        BlockSnapshot nextBlock = null;

        int xOff = 0;
        int zOff = 0;

        Random rand = new Random();
        int randChance = rand.nextInt(10);
        if (randChance <= 7)
            return;

        int randInt = rand.nextInt(4);
        xOff = offset[randInt][0];
        zOff = offset[randInt][1];
        try {
            switch (randInt) {
                case 0:
                case 1:
                case 2:
                case 3:
                    nextBlock = bs.getRelative(xOff, 0, zOff);
                    break;
            }
        } catch (InvalidBlockLocation e) {
            // An invalid block location can occur if we try to grow 'off the chunk'
            // this kind of growth is not valid, simply continue onward.
            return;
        }

        if (nextBlock == null) {
            return;
        }

        if (nextBlock.getType() == Material.AIR) {
            freeBlock = nextBlock;
        }

        if ((nextBlock.getType() == Material.MELON_BLOCK &&
                bs.getType() == Material.MELON_STEM) ||
                (nextBlock.getType() == Material.PUMPKIN &&
                        bs.getType() == Material.PUMPKIN_STEM)) {
            return;
        }

        if (freeBlock == null) {
            return;
        }

        if (bs.getType() == Material.MELON_STEM) {
            addGrowBlock("world", growMe.getX() + xOff, growMe.getY(), growMe.getZ() + zOff, Material.MELON_BLOCK, 0x0, true);
        } else {
            addGrowBlock("world", growMe.getX() + xOff, growMe.getY(), growMe.getZ() + zOff, Material.PUMPKIN, 0x0, true);
        }
    }

    public void addGrowBlock(String world, int x, int y, int z, Material typeid, int data, boolean spawn) {
        if ((x > -64 && x < 64) && ((z > -64 && z < 64))) {
            CivLog.debug("Didn't grow in town " + this.town.getName() + ": " + x + " " + y + " " + z);
            //Don't grow in spawn, gosh
            return;
        }
        this.growBlocks.add(new GrowBlock(world, x, y, z, typeid, data, spawn));
    }

    public void growBlock(BlockSnapshot bs, BlockCoord growMe, CivAsyncTask task) {

        //XXX we are skipping hydration as I guess we dont seem to care.
        //XXX we also skip light level checks, as we dont really care about that either.
        switch (bs.getType()) {
            case WHEAT:
            case CARROT:
            case POTATO:
                if (bs.getData() < 0x7) {
                    addGrowBlock("world", growMe.getX(), growMe.getY(), growMe.getZ(), bs.getType(), bs.getData() + 0x1, false);
                }
                break;
            case NETHER_WARTS:
                if (bs.getData() < 0x3) {
                    addGrowBlock("world", growMe.getX(), growMe.getY(), growMe.getZ(), bs.getType(), bs.getData() + 0x1, false);
                }
                break;
            case MELON_STEM:
            case PUMPKIN_STEM:
                if (bs.getData() < 0x7) {
                    addGrowBlock("world", growMe.getX(), growMe.getY(), growMe.getZ(), bs.getType(), bs.getData() + 0x1, false);
                } else if (bs.getData() == 0x7) {
                    spawnMelonOrPumpkin(bs, growMe, task);
                }
                break;
            case COCOA:
                if (CivData.canCocoaGrow(bs)) {
                    addGrowBlock("world", growMe.getX(), growMe.getY(), growMe.getZ(), bs.getType(), CivData.getNextCocoaValue(bs), false);
                }
                break;
        }
    }

    public void processGrowth(CivAsyncTask task) throws InterruptedException {

        if (!this.getStruct().isActive()) {
            return;
        }

        if (this.snapshot == null) {
            return;
        }

        if (this.town == null) {
            return;
        }

        // Lets let a growth rate of 100% mean 1 crop grows every 10 ticks(1/2 second)
        // Over 100% means we do more than 1 crop, under 100% means we check that probability.
        // So for example, if we have a 120% growth rate, every 10 ticks 1 crop *always* grows,
        // and another has a 20% chance to grow.
        double effectiveGrowthRate = 1.0;
        try {
            effectiveGrowthRate = this.town.getGrowth().total / (double) 100;
        } catch (NullPointerException e) {
            e.printStackTrace();
            CivLog.debug("Farm at location" + this.getCoord() + " in town " + this.getTown().getName() + " Growth Error");
        }

        for (Component comp : this.getFarm().attachedComponents) {
            if (comp instanceof ActivateOnBiome) {
                ActivateOnBiome ab = (ActivateOnBiome) comp;
                if (ab.isValidBiome(biomeName)) {
                    effectiveGrowthRate *= ab.getValue();
                    break;
                }
            }
        }
        this.getFarm().setLastEffectiveGrowth(effectiveGrowthRate);

        int crops_per_growth_tick = CivSettings.getIntegerStructure("farm.grows_per_tick");
        int numberOfCropsToGrow = (int) (effectiveGrowthRate * crops_per_growth_tick); //Since this is a double, 1.0 means 100% so int cast is # of crops
        int chanceForLast = (int) (this.town.getGrowth().total % 100);

        this.lastGrownCrops.clear();
        this.lastGrowTickCount = numberOfCropsToGrow;
        this.lastChanceForLast = chanceForLast;
        Calendar c = Calendar.getInstance();
        this.lastGrowDate = c.getTime();
        this.growBlocks = new LinkedList<>();

        if (this.cropLocationCache.size() == 0) {
            return;
        }

        // Process number of crops that will grow this time. Select one at random
        Random rand = new Random();
        for (int i = 0; i < numberOfCropsToGrow; i++) {
            BlockCoord growMe = this.cropLocationCache.get(rand.nextInt(this.cropLocationCache.size()));

            int bsx = growMe.getX() % 16;
            int bsy = growMe.getY();
            int bsz = growMe.getZ() % 16;

            BlockSnapshot bs = new BlockSnapshot(bsx, bsy, bsz, snapshot);

            this.lastGrownCrops.add(growMe);
            growBlock(bs, growMe, task);
        }
        if (chanceForLast != 0) {
            int randInt = rand.nextInt(100);
            this.lastRandomInt = randInt;
            if (randInt < chanceForLast) {
                BlockCoord growMe = this.cropLocationCache.get(rand.nextInt(this.cropLocationCache.size()));
                int bsx = growMe.getX() % 16;
                int bsy = growMe.getY();
                int bsz = growMe.getZ() % 16;

                BlockSnapshot bs = new BlockSnapshot(bsx, bsy, bsz, snapshot);

                this.lastGrownCrops.add(growMe);
                growBlock(bs, growMe, task);
            }
        }

        task.growBlocks(this.growBlocks, this);
    }

    public void processMissedGrowths(boolean populate, CivAsyncTask task) {
        if (this.missedGrowthTicks > 0) {

            if (populate) {
                if (this.snapshot == null) {
                    this.snapshot = this.getChunk().getChunkSnapshot();
                }
                this.populateCropLocationCache();
            }

            for (int i = 0; i < this.missedGrowthTicks; i++) {
                try {
                    this.processGrowth(task);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }
            this.missedGrowthTicks = 0;
        }
    }

    public ChunkCoord getCoord() {
        return coord;
    }

    public void setCoord(ChunkCoord coord) {
        this.coord = coord;
    }

    public int getLastGrowTickCount() {
        return lastGrowTickCount;
    }

    public void setLastGrowTickCount(int lastGrowTickCount) {
        this.lastGrowTickCount = lastGrowTickCount;
    }

    public Date getLastGrowDate() {
        return lastGrowDate;
    }

    public void setLastGrowDate(Date lastGrowDate) {
        this.lastGrowDate = lastGrowDate;
    }

    public ArrayList<BlockCoord> getLastGrownCrops() {
        return lastGrownCrops;
    }

    public void setLastGrownCrops(ArrayList<BlockCoord> lastGrownCrops) {
        this.lastGrownCrops = lastGrownCrops;
    }

    public double getLastChanceForLast() {
        return lastChanceForLast;
    }

    public void setLastChanceForLast(double lastChanceForLast) {
        this.lastChanceForLast = lastChanceForLast;
    }

    public int getLastRandomInt() {
        return lastRandomInt;
    }

    public void setLastRandomInt(int lastRandomInt) {
        this.lastRandomInt = lastRandomInt;
    }

//	public void addToCropLocationCache(Block b) {
    //	this.cropLocationCache.put(new BlockCoord(b), (int) b.getData());
    //}

    public void populateCropLocationCache() {
        this.lock.lock();
        try {
            this.cropLocationCache.clear();
            BlockSnapshot bs = new BlockSnapshot();

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 256; y++) {

                        //Block nextBlock = this.struct.getCorner().getBlock().getChunk().getBlock(x, y, z);
                        //BlockCoord bcoord = new BlockCoord(nextBlock);
                        bs.setFromSnapshotLocation(x, y, z, snapshot);

                        if (CivData.canGrow(bs)) {
                            this.cropLocationCache.add(new BlockCoord(snapshot.getWorldName(),
                                    (snapshot.getX() << 4) + bs.getX(),
                                    (bs.getY()),
                                    (snapshot.getZ() << 4) + bs.getZ()));
                        }
                    }
                }
            }
        } finally {
            this.lock.unlock();
        }
    }

    public int getMissedGrowthTicks() {
        return missedGrowthTicks;
    }

    public void setMissedGrowthTicks(int missedGrowthTicks) {
        this.missedGrowthTicks = missedGrowthTicks;
    }

    public void incrementMissedGrowthTicks() {
        this.missedGrowthTicks++;
        this.missedGrowthTicksStat++;
    }

    public int getMissedGrowthTicksStat() {
        return missedGrowthTicksStat;
    }


}
