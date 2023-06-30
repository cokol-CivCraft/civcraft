
package com.avrgaming.civcraft.populators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

import org.bukkit.Bukkit;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMobSpawner;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.util.ChunkCoord;

public class MobSpawnerPreGenerate {

    private int chunks_min;
    private int chunks_max;
    private int chunks_x;
    private int chunks_z;
    private int seed;
    private String worldName;
    
    public Map<ChunkCoord, MobSpawnerPick> spawnerPicks = new HashMap<ChunkCoord, MobSpawnerPick>();
    
    public MobSpawnerPreGenerate() {
        
    }
    
    private TreeSet<ConfigMobSpawner> getValidMobSpawners(int x, int z, Map<String, ConfigMobSpawner> spawners) {
    	TreeSet<ConfigMobSpawner> validGoods = new TreeSet<ConfigMobSpawner>();
        for (ConfigMobSpawner spawner : spawners.values()) {
            validGoods.add(spawner);
        }
        return validGoods;
    }
    
    /*
     * Pre-generate the locations of the trade spawners so that we can
     * validate their positions relative to each other. Once generated
     * save results to a file, and load if that file exists.
     */
    public void preGenerate() {
        try {
            chunks_min = CivSettings.getInteger(CivSettings.spawnersConfig, "generation.chunks_min");
            chunks_max = CivSettings.getInteger(CivSettings.spawnersConfig, "generation.chunks_max");
            chunks_x = CivSettings.getInteger(CivSettings.spawnersConfig, "generation.chunks_x");
            chunks_z = CivSettings.getInteger(CivSettings.spawnersConfig, "generation.chunks_z");
            seed = CivSettings.getInteger(CivSettings.spawnersConfig, "generation.seed");
            this.worldName = Bukkit.getWorlds().get(0).getName();   
            
        } catch (InvalidConfiguration e) {
            e.printStackTrace();
        }
        
        Random rand = new Random();
        rand.setSeed(seed);
        CivLog.info("Generating Mob Spawner Locations.");
        for (int x = -chunks_x; x < chunks_x; x += chunks_min ) {
            for (int z = -chunks_z; z < chunks_z; z += chunks_min) {
                int diff = chunks_max - chunks_min;
                int randX = x;
                int randZ = z;
                
                if (diff > 0) {
                    if (rand.nextBoolean()) {
                        randX += rand.nextInt(diff);
                    } else {
                        randX -= rand.nextInt(diff);
                    }
                    
                    if (rand.nextBoolean()) {
                        randZ += rand.nextInt(diff);
                    } else {
                        randZ -= rand.nextInt(diff);
                    }
                }
                
                
                ChunkCoord cCoord = new ChunkCoord(worldName, randX, randZ);
                pickFromCoord(cCoord);
            }
        }
        
        CivLog.info("Done.");
        
        
    }
    private ConfigMobSpawner pickFromSet(TreeSet<ConfigMobSpawner> set, int rand) {
        
        //Find the lowest rarity that qualifies in our list.
        double lowest_rarity = Double.MAX_VALUE;
        for (ConfigMobSpawner spawner : set) {
            if (rand < (spawner.rarity*100)) {
                if (spawner.rarity < lowest_rarity) {
                    lowest_rarity = spawner.rarity;
                }
            }
        }
        
        // Filter out all but the lowest rarity that qualifies
        ArrayList<ConfigMobSpawner> pickList = new ArrayList<ConfigMobSpawner>();
        for (ConfigMobSpawner spawner : set) {
            if (spawner.rarity == lowest_rarity) {
                pickList.add(spawner);
            }
        }
        
        // Pick a random spawner from this list.
        Random random = new Random();
        
        return pickList.get(random.nextInt(pickList.size()));
        
    }
    
    private void pickFromCoord(ChunkCoord cCoord) {
        TreeSet<ConfigMobSpawner> validLandSpawners;
        TreeSet<ConfigMobSpawner> validWaterSpawners;
        MobSpawnerPick pick = new MobSpawnerPick();

        validLandSpawners = this.getValidMobSpawners(cCoord.getX(), cCoord.getZ(), CivSettings.landSpawners);
        validWaterSpawners =  this.getValidMobSpawners(cCoord.getX(), cCoord.getZ(), CivSettings.waterSpawners);
    
        pick.chunkCoord = cCoord;
        
        Random random = new Random();
        int rand = random.nextInt(100);

        pick.landPick = pickFromSet(validLandSpawners, rand);
        pick.waterPick = pickFromSet(validWaterSpawners, rand);
        
        /*
         * Do not allow two of the same spawners within
         * 4 chunks of each other.
         */
//        for (int x = -4; x < 4; x++) {
//            for (int z = -4; z < 4; z++) {
//                ChunkCoord n = new ChunkCoord(cCoord.getWorldname(), cCoord.getX(), cCoord.getZ());
//                n.setX(n.getX()+x);
//                n.setZ(n.getZ()+z);
//                
//                MobSpawnerPick nearby = spawnerPicks.get(n);
//                if (nearby == null) {
//                    continue;
//                }
//                
//                if (nearby.landPick == pick.landPick) {
//                    if (validLandSpawners.size() <= 1) {
//                        /* Dont generate anything here. */
//                        return;
//                    } else {
//                        while (nearby.landPick == pick.landPick) {
//                            rand = random.nextInt(100);
//                            pick.landPick = pickFromSet(validLandSpawners, rand);
//                        }
//                    }
//                }
//                
//                if (nearby.waterPick == pick.waterPick) {
//                    if (validWaterSpawners.size() <= 1) {
//                        /* Dont generate anything here. */
//                        return;
//                    } else {
//                        while (nearby.waterPick == pick.waterPick) {
//                            rand = random.nextInt(100);
//                            pick.waterPick = pickFromSet(validWaterSpawners, rand);
//                        }
//                    }
//                }
//            }
//        }
        
        
        this.spawnerPicks.put(cCoord, pick);
    }
    
    
}
