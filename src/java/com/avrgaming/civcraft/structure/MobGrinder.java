package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class MobGrinder extends Structure {
    private static final double T1_CHANCE = CivSettings.structureConfig.getDouble("mobGrinder.t1_chance", 0.5); //1%
    private static final double T2_CHANCE = CivSettings.structureConfig.getDouble("mobGrinder.t2_chance", 0.1); //2%
    private static final double T3_CHANCE = CivSettings.structureConfig.getDouble("mobGrinder.t3_chance", 0.05); //1%
    private static final double T4_CHANCE = CivSettings.structureConfig.getDouble("mobGrinder.t4_chance", 0.01); //0.25%
    private static final double PACK_CHANCE = CivSettings.structureConfig.getDouble("mobGrinder.pack_chance", 0.005); //0.10%
    private static final double BIGPACK_CHANCE = CivSettings.structureConfig.getDouble("mobGrinder.bigpack_chance", 0.001);
    private static final double HUGEPACK_CHANCE = CivSettings.structureConfig.getDouble("mobGrinder.hugepack_chance", 0.0005);

    public int skippedCounter = 0;
    public ReentrantLock lock = new ReentrantLock();

    public enum Crystal {
        T1,
        T2,
        T3,
        T4,
        PACK,
        BIGPACK,
        HUGEPACK
    }

    protected MobGrinder(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    public MobGrinder(UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(uuid, nbt);
    }

    public double getMineralChance(Crystal crystal) {
        double chance = switch (crystal) {
            case T1 -> T1_CHANCE;
            case T2 -> T2_CHANCE;
            case T3 -> T3_CHANCE;
            case T4 -> T4_CHANCE;
            case PACK -> PACK_CHANCE;
            case BIGPACK -> BIGPACK_CHANCE;
            case HUGEPACK -> HUGEPACK_CHANCE;
        };

        double increase = chance * this.getTown().getBuffManager().getEffectiveDouble(Buff.EXTRACTION);
        chance += increase;

        if (this.getTown().getGovernment().id.equals("gov_tribalism")) {
            chance *= CivSettings.structureConfig.getDouble("mobGrinder.tribalism_rate", 1.5);
        } else {
            chance *= CivSettings.structureConfig.getDouble("mobGrinder.penalty_rate", 0.8);
        }

        return chance;
    }

}
