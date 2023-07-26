package com.avrgaming.civcraft.config;

import com.avrgaming.civcraft.main.CivLog;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;

public class ConfigCultureBiomeInfo {
	public String name;
	public double coins;
	public double hammers;
	public double growth;
	public double happiness;
	public double beakers;
    public Double faith;
	
	public static void loadConfig(FileConfiguration cfg, Map<String, ConfigCultureBiomeInfo> culture_biomes) {
		culture_biomes.clear();
		List<Map<?, ?>> list = cfg.getMapList("culture_biomes");
		for (Map<?,?> cl : list) {

            ConfigCultureBiomeInfo biome = new ConfigCultureBiomeInfo();
            biome.name = (String) cl.get("name");
            biome.coins = (Double) cl.get("coins");
            biome.hammers = (Double) cl.get("hammers");
            biome.growth = (Double) cl.get("growth");
            biome.happiness = (Double) cl.get("happiness");
            biome.beakers = (Double) cl.get("beakers");
            biome.faith = (Double) cl.get("faith");
            if (biome.faith == null) {
                biome.faith = 0.0;
            }

            culture_biomes.put(biome.name, biome);
        }
		CivLog.info("Loaded "+culture_biomes.size()+" Culture Biomes.");		
	}
}
