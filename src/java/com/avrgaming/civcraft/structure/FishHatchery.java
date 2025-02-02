package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.SimpleBlock;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class FishHatchery extends Structure {
    public static final int MAX_CHANCE = CivSettings.structureConfig.getInt("fishery.tierMax", 10000);
    public static final double FISH_T0_RATE = CivSettings.structureConfig.getDouble("fishery.t0_rate", 1.0); //100%
    public static final double FISH_T1_RATE = CivSettings.structureConfig.getDouble("fishery.t1_rate", 0.5); //100%
    public static final double FISH_T2_RATE = CivSettings.structureConfig.getDouble("fishery.t2_rate", 0.05); //100%
    public static final double FISH_T3_RATE = CivSettings.structureConfig.getDouble("fishery.t3_rate", 0.01); //100%
    public static final double FISH_T4_RATE = CivSettings.structureConfig.getDouble("fishery.t4_rate", 0.001); //100%

    private int level = 1;
    private Biome biome = null;
    public int skippedCounter = 0;
    public ReentrantLock lock = new ReentrantLock();

    protected FishHatchery(Location center, String id, Town town) throws CivException {
        super(center, id, town);
        setLevel(town.saved_fish_hatchery_level);
    }

    public FishHatchery(UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(uuid, nbt);
    }

    public double getChance(double chance) {
        return this.modifyChance(chance);
    }

    private double modifyChance(Double chance) {
//		double increase = chance*this.getTown().getBuffManager().getEffectiveDouble(Buff.EXTRACTION);
//		chance += increase;
//		
//		try {
//			if (this.getTown().getGovernment().id.equals("gov_despotism")) {
//				chance *= CivSettings.getDouble(CivSettings.structureConfig, "quarry.despotism_rate");
//			} else if (this.getTown().getGovernment().id.equals("gov_theocracy") || this.getTown().getGovernment().id.equals("gov_monarchy")){
//				chance *= CivSettings.getDouble(CivSettings.structureConfig, "quarry.penalty_rate");
//			}
//		} catch (InvalidConfiguration e) {
//			e.printStackTrace();
//		}

        //No buffs at this time
        return chance;
    }

    @Override
    public void onPostBuild(BlockCoord absCoord, SimpleBlock commandBlock) {
        this.level = getTown().saved_fish_hatchery_level;
        this.setBiome(this.getCorner().getBlock().getBiome());
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    private StructureSign getSignFromSpecialId(int special_id) {
        for (StructureSign sign : getSigns()) {
            int id = Integer.parseInt(sign.getAction());
            if (id == special_id) {
                return sign;
            }
        }
        return null;
    }


    @Override
    public void updateSignText() {
        int count = 0;


        for (count = 0; count < level; count++) {
            StructureSign sign = getSignFromSpecialId(count);
            if (sign == null) {
                CivLog.error("sign from special id was null, id:" + count);
                return;
            }
            sign.setText(CivSettings.localize.localizedString("fishery_sign_pool") + "\n" + (count + 1));
            sign.update();
        }

        for (; count < getSigns().size(); count++) {
            StructureSign sign = getSignFromSpecialId(count);
            if (sign == null) {
                CivLog.error("sign from special id was null, id:" + count);
                return;
            }
            sign.setText(CivSettings.localize.localizedString("fishery_sign_poolOffline"));
            sign.update();
        }

    }

    @Override
    public void processSignAction(Player player, StructureSign sign, PlayerInteractEvent event) {
        int special_id = Integer.parseInt(sign.getAction());
        if (special_id < this.level) {
            CivMessage.send(player, ChatColor.GREEN + CivSettings.localize.localizedString("var_fishery_pool_msg_online", (special_id + 1)));

        } else {
            CivMessage.send(player, ChatColor.RED + CivSettings.localize.localizedString("var_fishery_pool_msg_offline", (special_id + 1)));
        }
    }

    public Biome getBiome() {
        if (biome != null) {
            return biome;
        }
        try {
            World world = Bukkit.getWorld("world");
            BlockCoord block = this.getCenterLocation();
            Chunk chunk = world.getChunkAt(block.getX(), block.getZ());
            ChunkCoord coord = new ChunkCoord(chunk);
            CultureChunk cc = new CultureChunk(this.getTown(), coord);
            biome = cc.getBiome();
            this.setBiome(cc.getBiome());
        } catch (IllegalStateException ignored) {

        } finally {
            biome = Biome.BIRCH_FOREST_HILLS;
        }
        return biome;
    }

    public void setBiome(Biome biome) {
        this.biome = biome;
    }

}