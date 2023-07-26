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
package com.avrgaming.civcraft.threading.tasks;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivTaskAbortException;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.StructureChest;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.Trommel;
import com.avrgaming.civcraft.structure.Trommel.Mineral;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.sync.request.UpdateInventoryRequest.Action;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.MultiInventory;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class TrommelAsyncTask extends CivAsyncTask {

    Trommel trommel;
    private static final int GRAVEL_RATE = 1; //0.10%

    public static HashSet<String> debugTowns = new HashSet<>();

    public static void debug(Trommel trommel, String msg) {
        if (debugTowns.contains(trommel.getTown().getName())) {
            CivLog.warning("TrommelDebug:" + trommel.getTown().getName() + ":" + msg);
        }
    }

    public TrommelAsyncTask(Structure trommel) {
        this.trommel = (Trommel) trommel;
    }

    public void processTrommelUpdate() {
        if (!trommel.isActive()) {
            debug(trommel, "trommel inactive...");
            return;
        }

        debug(trommel, "Processing trommel...");
        // Grab each CivChest object we'll require.
        ArrayList<StructureChest> sources = trommel.getAllChestsById(1);
        ArrayList<StructureChest> destinations = trommel.getAllChestsById(2);

        if (sources.size() != 2 || destinations.size() != 2) {
            CivLog.error("Bad chests for trommel in town:" + trommel.getTown().getName() + " sources:" + sources.size() + " dests:" + destinations.size());
            return;
        }

        // Make sure the chunk is loaded before continuing. Also, add get chest and add it to inventory.
        MultiInventory source_inv = new MultiInventory();
        MultiInventory dest_inv = new MultiInventory();

        try {
            for (StructureChest src : sources) {
                //this.syncLoadChunk(src.getCoord().getWorldname(), src.getCoord().getX(), src.getCoord().getZ());
                Inventory tmp;
                try {
                    tmp = this.getChestInventory(src.getCoord().getWorldname(), src.getCoord().getX(), src.getCoord().getY(), src.getCoord().getZ(), false);
                } catch (CivTaskAbortException e) {
                    //e.printStackTrace();
                    CivLog.warning("Trommel:" + e.getMessage());
                    return;
                }
                if (tmp == null) {
                    trommel.skippedCounter++;
                    return;
                }
                source_inv.addInventory(tmp);
            }

            boolean full = true;
            for (StructureChest dst : destinations) {
                //this.syncLoadChunk(dst.getCoord().getWorldname(), dst.getCoord().getX(), dst.getCoord().getZ());
                Inventory tmp;
                try {
                    tmp = this.getChestInventory(dst.getCoord().getWorldname(), dst.getCoord().getX(), dst.getCoord().getY(), dst.getCoord().getZ(), false);
                } catch (CivTaskAbortException e) {
                    //e.printStackTrace();
                    CivLog.warning("Trommel:" + e.getMessage());
                    return;
                }
                if (tmp == null) {
                    trommel.skippedCounter++;
                    return;
                }
                dest_inv.addInventory(tmp);

                for (ItemStack stack : tmp.getContents()) {
                    if (stack == null) {
                        full = false;
                        break;
                    }
                }
            }

            if (full) {
                /* Trommel destination chest is full, stop processing. */
                return;
            }

        } catch (InterruptedException e) {
            return;
        }

        debug(trommel, "Processing trommel:" + trommel.skippedCounter + 1);
        ItemStack[] contents = source_inv.getContents();
        for (int i = 0; i < trommel.skippedCounter + 1; i++) {

            for (ItemStack stack : contents) {
                if (stack == null) {
                    continue;
                }

                if (stack.getTypeId() == Material.COBBLESTONE.getId()) {
                    try {
                        this.updateInventory(Action.REMOVE, source_inv, new ItemStack(Material.COBBLESTONE.getId(), 1, (short) 0));
                    } catch (InterruptedException e) {
                        return;
                    }

                    // Attempt to get special resources
                    Random rand = new Random();
                    int randMax = Trommel.GRAVEL_MAX_CHANCE;
                    int rand1 = rand.nextInt(randMax);
                    ItemStack newItem;

                    if (rand1 < ((int) ((trommel.getGravelChance(Mineral.CHROMIUM)) * randMax))) {
                        newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_chromium_ore"));
                    } else if (rand1 < ((int) ((trommel.getGravelChance(Mineral.EMERALD)) * randMax))) {
                        newItem = new ItemStack(Material.EMERALD.getId(), 1, (short) 0);
                    } else if (rand1 < ((int) ((trommel.getGravelChance(Mineral.DIAMOND)) * randMax))) {
                        newItem = new ItemStack(Material.DIAMOND.getId(), 1, (short) 0);

                    } else if (rand1 < ((int) ((trommel.getGravelChance(Mineral.GOLD)) * randMax))) {
                        newItem = new ItemStack(Material.GOLD_INGOT.getId(), 1, (short) 0);

                    } else if (rand1 < ((int) ((trommel.getGravelChance(Mineral.REDSTONE)) * randMax))) {
                        newItem = new ItemStack(Material.REDSTONE.getId(), 1, (short) 0);

                    } else if (rand1 < ((int) ((trommel.getGravelChance(Mineral.IRON)) * randMax))) {
                        newItem = new ItemStack(Material.IRON_INGOT.getId(), 1, (short) 0);

                    } else {
                        newItem = new ItemStack(Material.GRAVEL.getId(), GRAVEL_RATE, (short) 0);
                    }

                    //Try to add the new item to the dest chest, if we cant, oh well.
                    try {
                        debug(trommel, "Updating inventory:" + newItem);
                        this.updateInventory(Action.ADD, dest_inv, newItem);
                    } catch (InterruptedException e) {
                        return;
                    }
                    break;
                }
                if (stack.getTypeId() == Material.STONE.getId()) {

                    if (this.trommel.getLevel() >= 2 && ItemManager.getData(stack) ==
                            new MaterialData(Material.STONE.getId(), (byte) CivData.GRANITE).getData()) {
                        try {
                            this.updateInventory(Action.REMOVE, source_inv, new ItemStack(Material.STONE.getId(), 1, (short) CivData.GRANITE));
                        } catch (InterruptedException e) {
                            return;
                        }

                        // Attempt to get special resources
                        Random rand = new Random();
                        int randMax = Trommel.GRANITE_MAX_CHANCE;
                        int rand1 = rand.nextInt(randMax);
                        ItemStack newItem;

                        if (rand1 < ((int) ((trommel.getGraniteChance(Mineral.CRYSTAL)) * randMax))) {
                            int rand2 = rand.nextInt(randMax);
                            if (rand2 < (randMax / 10)) {
                                int rand3 = rand.nextInt(randMax);
                                if (rand3 < (randMax / 2)) {
                                    newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_ionic_crystal_2"));
                                } else {
                                    newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_metallic_crystal_2"));
                                }
                            } else if (rand2 < (randMax / 2)) {
                                newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_ionic_crystal_1"));
                            } else {
                                newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_metallic_crystal_1"));
                            }
                        } else if (rand1 < ((int) ((trommel.getGraniteChance(Mineral.REFINED_CHROMIUM)) * randMax))) {
                            newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_forged_chromium"));
                        } else if (rand1 < ((int) ((trommel.getGraniteChance(Mineral.REFINED_TUNGSTEN)) * randMax))) {
                            newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_forged_tungsten"));
                        } else if (rand1 < ((int) ((trommel.getGraniteChance(Mineral.CRYSTAL_FRAGMENT)) * randMax))) {
                            int rand2 = rand.nextInt(randMax);
                            if (rand2 < (randMax / 10)) {
                                int rand3 = rand.nextInt(randMax);
                                if (rand3 < (randMax / 2)) {
                                    newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_ionic_crystal_fragment_2"));
                                } else {
                                    newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_metallic_crystal_fragment_2"));
                                }
                            } else if (rand2 < (randMax / 2)) {
                                newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_ionic_crystal_fragment_1"));
                            } else {
                                newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_metallic_crystal_fragment_1"));
                            }
                        } else if (rand1 < ((int) ((trommel.getGraniteChance(Mineral.CHROMIUM)) * randMax))) {
                            newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_chromium_ore"));
                        } else if (rand1 < ((int) ((trommel.getGraniteChance(Mineral.EMERALD)) * randMax))) {
                            newItem = new ItemStack(Material.EMERALD.getId(), 1, (short) 0);
                        } else if (rand1 < ((int) ((trommel.getGraniteChance(Mineral.DIAMOND)) * randMax))) {
                            newItem = new ItemStack(Material.DIAMOND.getId(), 1, (short) 0);
                        } else if (rand1 < ((int) ((trommel.getGraniteChance(Mineral.TUNGSTEN)) * randMax))) {
                            newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_tungsten_ore"));
                        } else if (rand1 < ((int) ((trommel.getGraniteChance(Mineral.GOLD)) * randMax))) {
                            newItem = new ItemStack(Material.GOLD_INGOT.getId(), 1, (short) 0);
                        } else if (rand1 < ((int) ((trommel.getGraniteChance(Mineral.REDSTONE)) * randMax))) {
                            newItem = new ItemStack(Material.REDSTONE.getId(), 1, (short) 0);
                        } else if (rand1 < ((int) ((trommel.getGraniteChance(Mineral.IRON)) * randMax))) {
                            newItem = new ItemStack(Material.IRON_INGOT.getId(), 1, (short) 0);
                        } else if (rand1 < ((int) ((trommel.getGraniteChance(Mineral.POLISHED)) * randMax))) {
                            newItem = new ItemStack(Material.STONE.getId(), 1, (short) CivData.POLISHED_GRANITE);
                        } else if (rand1 < ((int) ((trommel.getGraniteChance(Mineral.DIRT)) * randMax))) {
                            newItem = new ItemStack(Material.DIRT.getId(), 1, (short) 0);
                        } else {
                            newItem = new ItemStack(Material.GRAVEL.getId(), GRAVEL_RATE, (short) 0);
                        }

                        //Try to add the new item to the dest chest, if we cant, oh well.
                        try {
                            debug(trommel, "Updating inventory:" + newItem);
                            this.updateInventory(Action.ADD, dest_inv, newItem);
                            trommel.getCorner().getBlock().getWorld().playSound(trommel.getCenterLocation().getLocation(), Sound.ENTITY_MINECART_RIDING, 1.2f, 1.2f);

                        } catch (InterruptedException e) {
                            return;
                        }
                        break;
                    }
                    if (this.trommel.getLevel() >= 3 && ItemManager.getData(stack) ==
                            new MaterialData(Material.STONE.getId(), (byte) CivData.DIORITE).getData()) {
                        try {
                            this.updateInventory(Action.REMOVE, source_inv, new ItemStack(Material.STONE.getId(), 1, (short) CivData.DIORITE));
                        } catch (InterruptedException e) {
                            return;
                        }
                        // Attempt to get special resources
                        Random rand = new Random();
                        int randMax = Trommel.DIORITE_MAX_CHANCE;
                        int rand1 = rand.nextInt(randMax);
                        ItemStack newItem;

                        if (rand1 < ((int) ((trommel.getDioriteChance(Mineral.CRYSTAL)) * randMax))) {
                            int rand2 = rand.nextInt(randMax);
                            if (rand2 < (randMax / 10)) {
                                int rand3 = rand.nextInt(randMax);
                                if (rand3 < (randMax / 2)) {
                                    newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_ionic_crystal_2"));
                                } else {
                                    newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_metallic_crystal_2"));
                                }
                            } else if (rand2 < (randMax / 2)) {
                                newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_ionic_crystal_1"));
                            } else {
                                newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_metallic_crystal_1"));
                            }
                        } else if (rand1 < ((int) ((trommel.getDioriteChance(Mineral.REFINED_CHROMIUM)) * randMax))) {
                            newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_forged_chromium"));
                        } else if (rand1 < ((int) ((trommel.getDioriteChance(Mineral.REFINED_TUNGSTEN)) * randMax))) {
                            newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_forged_tungsten"));
                        } else if (rand1 < ((int) ((trommel.getDioriteChance(Mineral.CRYSTAL_FRAGMENT)) * randMax))) {
                            int rand2 = rand.nextInt(randMax);
                            if (rand2 < (randMax / 10)) {
                                int rand3 = rand.nextInt(randMax);
                                if (rand3 < (randMax / 2)) {
                                    newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_ionic_crystal_fragment_2"));
                                } else {
                                    newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_metallic_crystal_fragment_2"));
                                }
                            } else if (rand2 < (randMax / 2)) {
                                newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_ionic_crystal_fragment_1"));
                            } else {
                                newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_metallic_crystal_fragment_1"));
                            }
                        } else if (rand1 < ((int) ((trommel.getDioriteChance(Mineral.CHROMIUM)) * randMax))) {
                            newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_chromium_ore"));
                        } else if (rand1 < ((int) ((trommel.getDioriteChance(Mineral.EMERALD)) * randMax))) {
                            newItem = new ItemStack(Material.EMERALD.getId(), 1, (short) 0);
                        } else if (rand1 < ((int) ((trommel.getDioriteChance(Mineral.DIAMOND)) * randMax))) {
                            newItem = new ItemStack(Material.DIAMOND.getId(), 1, (short) 0);
                        } else if (rand1 < ((int) ((trommel.getDioriteChance(Mineral.TUNGSTEN)) * randMax))) {
                            newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_tungsten_ore"));
                        } else if (rand1 < ((int) ((trommel.getDioriteChance(Mineral.GOLD)) * randMax))) {
                            newItem = new ItemStack(Material.GOLD_INGOT.getId(), 1, (short) 0);
                        } else if (rand1 < ((int) ((trommel.getDioriteChance(Mineral.REDSTONE)) * randMax))) {
                            newItem = new ItemStack(Material.REDSTONE.getId(), 1, (short) 0);
                        } else if (rand1 < ((int) ((trommel.getDioriteChance(Mineral.IRON)) * randMax))) {
                            newItem = new ItemStack(Material.IRON_INGOT.getId(), 1, (short) 0);
                        } else if (rand1 < ((int) ((trommel.getDioriteChance(Mineral.POLISHED)) * randMax))) {
                            newItem = new ItemStack(Material.STONE.getId(), 1, (short) CivData.POLISHED_DIORITE);
                        } else if (rand1 < ((int) ((trommel.getDioriteChance(Mineral.DIRT)) * randMax))) {
                            newItem = new ItemStack(Material.DIRT.getId(), 1, (short) 0);
                        } else {
                            newItem = new ItemStack(Material.GRAVEL.getId(), GRAVEL_RATE, (short) 0);
                        }

                        //Try to add the new item to the dest chest, if we cant, oh well.
                        try {
                            debug(trommel, "Updating inventory:" + newItem);
                            this.updateInventory(Action.ADD, dest_inv, newItem);
                            trommel.getCorner().getBlock().getWorld().playSound(trommel.getCenterLocation().getLocation(), Sound.ENTITY_MINECART_RIDING, 1.2f, 1.2f);

                        } catch (InterruptedException e) {
                            return;
                        }
                        break;
                    }
                    if (this.trommel.getLevel() >= 4 && ItemManager.getData(stack) ==
                            new MaterialData(Material.STONE.getId(), (byte) CivData.ANDESITE).getData()) {
                        try {
                            this.updateInventory(Action.REMOVE, source_inv, new ItemStack(Material.STONE.getId(), 1, (short) CivData.ANDESITE));
                        } catch (InterruptedException e) {
                            return;
                        }
                        // Attempt to get special resources
                        Random rand = new Random();
                        int randMax = Trommel.ANDESITE_MAX_CHANCE;
                        int rand1 = rand.nextInt(randMax);
                        ItemStack newItem;

                        if (rand1 < ((int) ((trommel.getAndesiteChance(Mineral.CRYSTAL)) * randMax))) {
                            int rand2 = rand.nextInt(randMax);
                            if (rand2 < (randMax / 10)) {
                                int rand3 = rand.nextInt(randMax);
                                if (rand3 < (randMax / 2)) {
                                    newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_ionic_crystal_2"));
                                } else {
                                    newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_metallic_crystal_2"));
                                }
                            } else if (rand2 < (randMax / 2)) {
                                newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_ionic_crystal_1"));
                            } else {
                                newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_metallic_crystal_1"));
                            }
                        } else if (rand1 < ((int) ((trommel.getAndesiteChance(Mineral.REFINED_CHROMIUM)) * randMax))) {
                            newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_forged_chromium"));
                        } else if (rand1 < ((int) ((trommel.getAndesiteChance(Mineral.REFINED_TUNGSTEN)) * randMax))) {
                            newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_forged_tungsten"));
                        } else if (rand1 < ((int) ((trommel.getAndesiteChance(Mineral.CRYSTAL_FRAGMENT)) * randMax))) {
                            int rand2 = rand.nextInt(randMax);
                            if (rand2 < (randMax / 10)) {
                                int rand3 = rand.nextInt(randMax);
                                if (rand3 < (randMax / 2)) {
                                    newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_ionic_crystal_fragment_2"));
                                } else {
                                    newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_metallic_crystal_fragment_2"));
                                }
                            } else if (rand2 < (randMax / 2)) {
                                newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_ionic_crystal_fragment_1"));
                            } else {
                                newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_metallic_crystal_fragment_1"));
                            }
                        } else if (rand1 < ((int) ((trommel.getAndesiteChance(Mineral.CHROMIUM)) * randMax))) {
                            newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_chromium_ore"));
                        } else if (rand1 < ((int) ((trommel.getAndesiteChance(Mineral.EMERALD)) * randMax))) {
                            newItem = new ItemStack(Material.EMERALD.getId(), 1, (short) 0);
                        } else if (rand1 < ((int) ((trommel.getAndesiteChance(Mineral.DIAMOND)) * randMax))) {
                            newItem = new ItemStack(Material.DIAMOND.getId(), 1, (short) 0);
                        } else if (rand1 < ((int) ((trommel.getAndesiteChance(Mineral.TUNGSTEN)) * randMax))) {
                            newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_tungsten_ore"));
                        } else if (rand1 < ((int) ((trommel.getAndesiteChance(Mineral.GOLD)) * randMax))) {
                            newItem = new ItemStack(Material.GOLD_INGOT.getId(), 1, (short) 0);
                        } else if (rand1 < ((int) ((trommel.getAndesiteChance(Mineral.REDSTONE)) * randMax))) {
                            newItem = new ItemStack(Material.REDSTONE.getId(), 1, (short) 0);
                        } else if (rand1 < ((int) ((trommel.getAndesiteChance(Mineral.IRON)) * randMax))) {
                            newItem = new ItemStack(Material.IRON_INGOT.getId(), 1, (short) 0);
                        } else if (rand1 < ((int) ((trommel.getAndesiteChance(Mineral.POLISHED)) * randMax))) {
                            newItem = new ItemStack(Material.STONE.getId(), 1, (short) CivData.POLISHED_ANDESITE);
                        } else if (rand1 < ((int) ((trommel.getAndesiteChance(Mineral.DIRT)) * randMax))) {
                            newItem = new ItemStack(Material.DIRT.getId(), 1, (short) 0);
                        } else {
                            newItem = new ItemStack(Material.GRAVEL.getId(), GRAVEL_RATE, (short) 0);
                        }

                        //Try to add the new item to the dest chest, if we cant, oh well.
                        try {
                            debug(trommel, "Updating inventory:" + newItem);
                            this.updateInventory(Action.ADD, dest_inv, newItem);
                            trommel.getCenterLocation().getBlock().getWorld().playSound(trommel.getCenterLocation().getLocation(), Sound.ENTITY_MINECART_RIDING, 1.2f, 1.2f);

                        } catch (InterruptedException e) {
                            return;
                        }
                        break;
                    }
                }
            }
        }
        trommel.skippedCounter = 0;
    }


    @Override
    public void run() {
        if (this.trommel.lock.tryLock()) {
            try {
                try {
                    if (this.trommel.getTown().getGovernment().id.equals("gov_theocracy") || this.trommel.getTown().getGovernment().id.equals("gov_monarchy")) {
                        Random rand = new Random();
                        int randMax = 100;
                        int rand1 = rand.nextInt(randMax);
                        double chance = CivSettings.getDouble(CivSettings.structureConfig, "trommel.penalty_rate") * 100;
                        if (rand1 < chance) {
                            processTrommelUpdate();
                            debug(this.trommel, "Not penalized");
                        } else {

                            debug(this.trommel, "Skip Due to Penalty");
                        }
                    } else {
                        processTrommelUpdate();
                        if (this.trommel.getTown().getGovernment().id.equals("gov_despotism")) {
                            debug(this.trommel, "Doing Bonus");
                            processTrommelUpdate();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } finally {
                this.trommel.lock.unlock();
            }
        } else {
            debug(this.trommel, "Failed to get lock while trying to start task, aborting.");
        }
    }

}
