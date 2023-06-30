package com.avrgaming.civcraft.config;

import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;

public class ConfigMobSpawner implements Comparable<ConfigMobSpawner> {
    public String id;
    public String name;
    public boolean water;
    public Double rarity = null;
    
    public static void loadConfig(FileConfiguration cfg, Map<String, ConfigMobSpawner> spawners,
            Map<String, ConfigMobSpawner> landSpawners, Map<String, ConfigMobSpawner> waterSpawners){
    	spawners.clear();
        List<Map<?, ?>> land_spawners = cfg.getMapList("land_spawners");
        for (Map<?, ?> g : land_spawners) {
            ConfigMobSpawner good = new ConfigMobSpawner();
            good.id = (String)g.get("id");
            good.name = (String)g.get("name");
            good.water = false;
            good.rarity = ((Double)g.get("rarity"));
            if (good.rarity == null) {
                good.rarity = 1.0;
            }
            landSpawners.put(good.id, good);
            spawners.put(good.id, good);
        }
        
        List<Map<?, ?>> water_spawners = cfg.getMapList("water_spawners");
        for (Map<?, ?> g : water_spawners) {
            ConfigMobSpawner good = new ConfigMobSpawner();
            good.id = (String)g.get("id");
            good.name = (String)g.get("name");
            good.water = true;
            good.rarity = ((Double)g.get("rarity"));
            if (good.rarity == null) {
                good.rarity = 1.0;
            }

            
            waterSpawners.put(good.id, good);
            spawners.put(good.id, good);
        }
        
        CivLog.info("Loaded "+spawners.size()+" Mob Spawner Types.");
    }

    @Override
    public int compareTo(ConfigMobSpawner otherSpawner) {
        
        if (this.rarity < otherSpawner.rarity) {
            // A lower rarity should go first.
            return 1;
        } else if (this.rarity == otherSpawner.rarity) {
            return 0;
        }
        return -1;
    }
    
}
