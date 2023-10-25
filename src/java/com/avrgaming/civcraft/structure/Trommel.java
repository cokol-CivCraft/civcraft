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
package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.SimpleBlock;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class Trommel extends Structure {
    public static final int GRAVEL_MAX_CHANCE = CivSettings.structureConfig.getInt("trommel_gravel.max", 10000); //100%
    public static final int GRANITE_MAX_CHANCE = CivSettings.structureConfig.getInt("trommel_granite.max", 100000);
    public static final int DIORITE_MAX_CHANCE = CivSettings.structureConfig.getInt("trommel_diorite.max", 100000);
    public static final int ANDESITE_MAX_CHANCE = CivSettings.structureConfig.getInt("trommel_andesite.max", 100000);
    private static final double GRAVEL_REDSTONE_CHANCE = CivSettings.structureConfig.getDouble("trommel_gravel.redstone_chance", 0.04); //3%
    private static final double GRAVEL_IRON_CHANCE = CivSettings.structureConfig.getDouble("trommel_gravel.iron_chance", 0.06); //4%
    private static final double GRAVEL_GOLD_CHANCE = CivSettings.structureConfig.getDouble("trommel_gravel.gold_chance", 0.02); //2%
    private static final double GRAVEL_DIAMOND_CHANCE = CivSettings.structureConfig.getDouble("trommel_gravel.diamond_chance", 0.0050); //0.50%
    private static final double GRAVEL_EMERALD_CHANCE = CivSettings.structureConfig.getDouble("trommel_gravel.emerald_chance", 0.0020); //0.20%
    private static final double GRAVEL_CHROMIUM_CHANCE = CivSettings.structureConfig.getDouble("trommel_gravel.chromium_chance", 0.0008);
    private static final double GRANITE_DIRT_RATE = CivSettings.structureConfig.getDouble("trommel_granite.dirt_rate", 0.75); //100%
    private static final double GRANITE_POLISHED_RATE = CivSettings.structureConfig.getDouble("trommel_granite.polished_rate", 0.1); //10%
    private static final double GRANITE_REDSTONE_CHANCE = CivSettings.structureConfig.getDouble("trommel_granite.redstone_chance", 0.04);
    private static final double GRANITE_IRON_CHANCE = CivSettings.structureConfig.getDouble("trommel_granite.iron_chance", 0.06);
    private static final double GRANITE_GOLD_CHANCE = CivSettings.structureConfig.getDouble("trommel_granite.gold_chance", 0.02);
    private static final double GRANITE_TUNGSTEN_CHANCE = CivSettings.structureConfig.getDouble("trommel_granite.tungsten_chance", 0.0075);
    private static final double GRANITE_DIAMOND_CHANCE = CivSettings.structureConfig.getDouble("trommel_granite.diamond_chance", 0.0050);
    private static final double GRANITE_EMERALD_CHANCE = CivSettings.structureConfig.getDouble("trommel_granite.emerald_chance", 0.0020);
    private static final double GRANITE_CHROMIUM_CHANCE = CivSettings.structureConfig.getDouble("trommel_granite.chromium_chance", 0.0008);
    private static final double GRANITE_CRYSTAL_FRAGMENT_CHANCE = CivSettings.structureConfig.getDouble("trommel_granite.crystal_fragment_chance", 0.0002);
    private static final double GRANITE_REFINED_CHROMIUM_CHANCE = CivSettings.structureConfig.getDouble("trommel_granite.refined_chromium_chance", 0.00008);
    private static final double GRANITE_REFINED_TUNGSTEN_CHANCE = CivSettings.structureConfig.getDouble("trommel_granite.refined_tungsten_chance", 0.00004);
    private static final double GRANITE_CRYSTAL_CHANCE = CivSettings.structureConfig.getDouble("trommel_granite.crystal_chance", 0.00002);
    private static final double DIORITE_DIRT_RATE = CivSettings.structureConfig.getDouble("trommel_diorite.dirt_rate", 0.75); //100%
    private static final double DIORITE_POLISHED_RATE = CivSettings.structureConfig.getDouble("trommel_diorite.polished_rate", 0.1); //10%
    private static final double DIORITE_REDSTONE_CHANCE = CivSettings.structureConfig.getDouble("trommel_diorite.redstone_chance", 0.045);
    private static final double DIORITE_IRON_CHANCE = CivSettings.structureConfig.getDouble("trommel_diorite.iron_chance", 0.06);
    private static final double DIORITE_GOLD_CHANCE = CivSettings.structureConfig.getDouble("trommel_diorite.gold_chance", 0.025);
    private static final double DIORITE_TUNGSTEN_CHANCE = CivSettings.structureConfig.getDouble("trommel_diorite.tungsten_chance", 0.00080);
    private static final double DIORITE_DIAMOND_CHANCE = CivSettings.structureConfig.getDouble("trommel_diorite.diamond_chance", 0.0055);
    private static final double DIORITE_EMERALD_CHANCE = CivSettings.structureConfig.getDouble("trommel_diorite.emerald_chance", 0.0025);
    private static final double DIORITE_CHROMIUM_CHANCE = CivSettings.structureConfig.getDouble("trommel_diorite.chromium_chance", 0.00085);
    private static final double DIORITE_CRYSTAL_FRAGMENT_CHANCE = CivSettings.structureConfig.getDouble("trommel_diorite.crystal_fragment_chance", 0.00025);
    private static final double DIORITE_REFINED_CHROMIUM_CHANCE = CivSettings.structureConfig.getDouble("trommel_diorite.refined_chromium_chance", 0.00012);
    private static final double DIORITE_REFINED_TUNGSTEN_CHANCE = CivSettings.structureConfig.getDouble("trommel_diorite.refined_tungsten_chance", 0.00008);
    private static final double DIORITE_CRYSTAL_CHANCE = CivSettings.structureConfig.getDouble("trommel_diorite.crystal_chance", 0.00004);
    private static final double ANDESITE_DIRT_RATE = CivSettings.structureConfig.getDouble("trommel_andesite.dirt_rate", 0.75); //100%
    private static final double ANDESITE_POLISHED_RATE = CivSettings.structureConfig.getDouble("trommel_andesite.polished_rate", 0.1); //10%
    private static final double ANDESITE_REDSTONE_CHANCE = CivSettings.structureConfig.getDouble("trommel_andesite.redstone_chance", 0.05);
    private static final double ANDESITE_IRON_CHANCE = CivSettings.structureConfig.getDouble("trommel_andesite.iron_chance", 0.06);
    private static final double ANDESITE_GOLD_CHANCE = CivSettings.structureConfig.getDouble("trommel_andesite.gold_chance", 0.03);
    private static final double ANDESITE_TUNGSTEN_CHANCE = CivSettings.structureConfig.getDouble("trommel_andesite.tungsten_chance", 0.0085);
    private static final double ANDESITE_DIAMOND_CHANCE = CivSettings.structureConfig.getDouble("trommel_andesite.diamond_chance", 0.0060);
    private static final double ANDESITE_EMERALD_CHANCE = CivSettings.structureConfig.getDouble("trommel_andesite.emerald_chance", 0.0030);
    private static final double ANDESITE_CHROMIUM_CHANCE = CivSettings.structureConfig.getDouble("trommel_andesite.chromium_chance", 0.0009);
    private static final double ANDESITE_CRYSTAL_FRAGMENT_CHANCE = CivSettings.structureConfig.getDouble("trommel_andesite.crystal_fragment_chance", 0.0003);
    private static final double ANDESITE_REFINED_CHROMIUM_CHANCE = CivSettings.structureConfig.getDouble("trommel_andesite.refined_chromium_chance", 0.00015);
    private static final double ANDESITE_REFINED_TUNGSTEN_CHANCE = CivSettings.structureConfig.getDouble("trommel_andesite.refined_tungsten_chance", 0.00010);
    private static final double ANDESITE_CRYSTAL_CHANCE = CivSettings.structureConfig.getDouble("trommel_andesite.crystal_chance", 0.00005);
    private final HashMap<Mineral, Double> andesiteChance = new HashMap<>();
    private final HashMap<Mineral, Double> dioriteChance = new HashMap<>();
    private final HashMap<Mineral, Double> graniteChance = new HashMap<>();
    private final HashMap<Mineral, Double> gravelChance = new HashMap<>();
    private int level = 1;
    public int skippedCounter = 0;
    public ReentrantLock lock = new ReentrantLock();

    public enum Mineral {
        CRYSTAL,
        REFINED_TUNGSTEN,
        REFINED_CHROMIUM,
        CRYSTAL_FRAGMENT,
        CHROMIUM,
        EMERALD,
        DIAMOND,
        TUNGSTEN,
        GOLD,
        REDSTONE,
        IRON,
        POLISHED,
        DIRT
    }

    protected Trommel(Location center, String id, Town town) throws CivException {
        super(center, id, town);
        setLevel(town.saved_trommel_level);
    }

    public Trommel(UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(uuid, nbt);
    }

    public double getGravelChance(Mineral mineral) {
        HashMap<Mineral, Double> dd = getGravelChanceMap();
        return this.modifyChance(dd.get(mineral));
    }

    public double getGraniteChance(Mineral mineral) {
        HashMap<Mineral, Double> dd = getGraniteChanceMap();
        return this.modifyChance(dd.get(mineral));
    }

    public double getDioriteChance(Mineral mineral) {
        HashMap<Mineral, Double> dd = getDioriteChanceMap();
        return this.modifyChance(dd.get(mineral));
    }


    public double getAndesiteChance(Mineral mineral) {
        HashMap<Mineral, Double> dd = getAndesiteChanceMap();
        return this.modifyChance(dd.get(mineral));
    }

    private double modifyChance(Double chance) {
        double increase = chance * this.getTown().getBuffManager().getEffectiveDouble(Buff.EXTRACTION);
        chance += increase;
        chance *= this.getTown().getGovernment().trommel_rate;
        return chance;
    }

    @Override
    public void onPostBuild(BlockCoord absCoord, SimpleBlock commandBlock) {
        this.level = getTown().saved_trommel_level;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }


    public HashMap<Mineral, Double> getAndesiteChanceMap() {
        if (!andesiteChance.isEmpty()) {
            return andesiteChance;
        }
        andesiteChance.put(Mineral.CRYSTAL, ANDESITE_CRYSTAL_CHANCE);
        andesiteChance.put(Mineral.IRON, ANDESITE_IRON_CHANCE);
        andesiteChance.put(Mineral.GOLD, ANDESITE_GOLD_CHANCE);
        andesiteChance.put(Mineral.EMERALD, ANDESITE_EMERALD_CHANCE);
        andesiteChance.put(Mineral.DIAMOND, ANDESITE_DIAMOND_CHANCE);
        andesiteChance.put(Mineral.CHROMIUM, ANDESITE_CHROMIUM_CHANCE);
        andesiteChance.put(Mineral.CRYSTAL_FRAGMENT, ANDESITE_CRYSTAL_FRAGMENT_CHANCE);
        andesiteChance.put(Mineral.DIRT, ANDESITE_DIRT_RATE);
        andesiteChance.put(Mineral.POLISHED, ANDESITE_POLISHED_RATE);
        andesiteChance.put(Mineral.REDSTONE, ANDESITE_REDSTONE_CHANCE);
        andesiteChance.put(Mineral.TUNGSTEN, ANDESITE_TUNGSTEN_CHANCE);
        andesiteChance.put(Mineral.REFINED_TUNGSTEN, ANDESITE_REFINED_TUNGSTEN_CHANCE);
        andesiteChance.put(Mineral.REFINED_CHROMIUM, ANDESITE_REFINED_CHROMIUM_CHANCE);
        return andesiteChance;
    }

    public HashMap<Mineral, Double> getDioriteChanceMap() {
        if (!dioriteChance.isEmpty()) {
            return dioriteChance;
        }
        dioriteChance.put(Mineral.CRYSTAL, DIORITE_CRYSTAL_CHANCE);
        dioriteChance.put(Mineral.IRON, DIORITE_IRON_CHANCE);
        dioriteChance.put(Mineral.GOLD, DIORITE_GOLD_CHANCE);
        dioriteChance.put(Mineral.EMERALD, DIORITE_EMERALD_CHANCE);
        dioriteChance.put(Mineral.DIAMOND, DIORITE_DIAMOND_CHANCE);
        dioriteChance.put(Mineral.CHROMIUM, DIORITE_CHROMIUM_CHANCE);
        dioriteChance.put(Mineral.CRYSTAL_FRAGMENT, DIORITE_CRYSTAL_FRAGMENT_CHANCE);
        dioriteChance.put(Mineral.DIRT, DIORITE_DIRT_RATE);
        dioriteChance.put(Mineral.POLISHED, DIORITE_POLISHED_RATE);
        dioriteChance.put(Mineral.REDSTONE, DIORITE_REDSTONE_CHANCE);
        dioriteChance.put(Mineral.TUNGSTEN, DIORITE_TUNGSTEN_CHANCE);
        dioriteChance.put(Mineral.REFINED_TUNGSTEN, DIORITE_REFINED_TUNGSTEN_CHANCE);
        dioriteChance.put(Mineral.REFINED_CHROMIUM, DIORITE_REFINED_CHROMIUM_CHANCE);
        return dioriteChance;
    }

    public HashMap<Mineral, Double> getGraniteChanceMap() {
        if (!graniteChance.isEmpty()) {
            return graniteChance;
        }
        graniteChance.put(Mineral.CRYSTAL, GRANITE_CRYSTAL_CHANCE);
        graniteChance.put(Mineral.IRON, GRANITE_IRON_CHANCE);
        graniteChance.put(Mineral.GOLD, GRANITE_GOLD_CHANCE);
        graniteChance.put(Mineral.EMERALD, GRANITE_EMERALD_CHANCE);
        graniteChance.put(Mineral.DIAMOND, GRANITE_DIAMOND_CHANCE);
        graniteChance.put(Mineral.CHROMIUM, GRANITE_CHROMIUM_CHANCE);
        graniteChance.put(Mineral.CRYSTAL_FRAGMENT, GRANITE_CRYSTAL_FRAGMENT_CHANCE);
        graniteChance.put(Mineral.DIRT, GRANITE_DIRT_RATE);
        graniteChance.put(Mineral.POLISHED, GRANITE_POLISHED_RATE);
        graniteChance.put(Mineral.REDSTONE, GRANITE_REDSTONE_CHANCE);
        graniteChance.put(Mineral.TUNGSTEN, GRANITE_TUNGSTEN_CHANCE);
        graniteChance.put(Mineral.REFINED_TUNGSTEN, GRANITE_REFINED_TUNGSTEN_CHANCE);
        graniteChance.put(Mineral.REFINED_CHROMIUM, GRANITE_REFINED_CHROMIUM_CHANCE);
        return graniteChance;
    }

    public HashMap<Mineral, Double> getGravelChanceMap() {
        if (!gravelChance.isEmpty()) {
            return gravelChance;
        }
        gravelChance.put(Mineral.IRON, GRAVEL_IRON_CHANCE);
        gravelChance.put(Mineral.GOLD, GRAVEL_GOLD_CHANCE);
        gravelChance.put(Mineral.EMERALD, GRAVEL_EMERALD_CHANCE);
        gravelChance.put(Mineral.DIAMOND, GRAVEL_DIAMOND_CHANCE);
        gravelChance.put(Mineral.CHROMIUM, GRAVEL_CHROMIUM_CHANCE);
        gravelChance.put(Mineral.REDSTONE, GRAVEL_REDSTONE_CHANCE);
        return gravelChance;
    }

}
