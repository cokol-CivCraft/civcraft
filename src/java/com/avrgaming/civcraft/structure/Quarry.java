package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.SimpleBlock;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;

public class Quarry extends Structure {
    public static final int MAX_CHANCE = CivSettings.getIntegerStructure("quarry.max");
    public static final String NO_DIRT = CivSettings.getStringStructure("quarry.nodirt_buff");
    private static final double COBBLESTONE_RATE = CivSettings.getDoubleStructure("quarry.cobblestone_rate"); //100%
    private static final double OTHER_RATE = CivSettings.getDoubleStructure("quarry.other_rate"); //10%
    private static final double COAL_RATE = CivSettings.getDoubleStructure("quarry.coal_rate"); //10%
    private static final double REDSTONE_CHANCE = CivSettings.getDoubleStructure("quarry.redstone_chance");
    private static final double IRON_CHANCE = CivSettings.getDoubleStructure("quarry.iron_chance");
    private static final double GOLD_CHANCE = CivSettings.getDoubleStructure("quarry.gold_chance");
    private static final double TUNGSTEN_CHANCE = CivSettings.getDoubleStructure("quarry.tungsten_chance");
    private static final double RARE_CHANCE = CivSettings.getDoubleStructure("quarry.rare_chance");

    private int level = 1;
    public int skippedCounter = 0;
    public ReentrantLock lock = new ReentrantLock();

    public enum Mineral {
        RARE, TUNGSTEN, GOLD, REDSTONE, IRON, COAL, OTHER, COBBLESTONE
    }

    protected Quarry(Location center, String id, Town town) throws CivException {
        super(center, id, town);
        setLevel(town.saved_quarry_level);
    }

    public Quarry(ResultSet rs) throws SQLException, CivException {
        super(rs);
    }

    public double getChance(Mineral mineral) {
        double chance = switch (mineral) {
            case RARE -> RARE_CHANCE;
            case TUNGSTEN -> TUNGSTEN_CHANCE;
            case GOLD -> GOLD_CHANCE;
            case IRON -> IRON_CHANCE;
            case REDSTONE -> REDSTONE_CHANCE;
            case COAL -> COAL_RATE;
            case OTHER -> OTHER_RATE;
            case COBBLESTONE -> COBBLESTONE_RATE;
        };
        return this.modifyChance(chance) * MAX_CHANCE;
    }

    private double modifyChance(Double chance) {
        double increase = chance * this.getTown().getBuffManager().getEffectiveDouble(Buff.EXTRACTION);
        chance += increase;
        chance *= this.getTown().getGovernment().trommel_rate;
        return chance;
    }

    @Override
    public void onPostBuild(BlockCoord absCoord, SimpleBlock commandBlock) {
        this.level = getTown().saved_quarry_level;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

}
