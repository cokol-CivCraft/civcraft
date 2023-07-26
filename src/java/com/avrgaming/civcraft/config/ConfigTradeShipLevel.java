package com.avrgaming.civcraft.config;

import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;

public class ConfigTradeShipLevel {
	public int level;	/* Current level number */
	public int upgradeTrade; /* Number of redstone this mine consumes */
	public int maxTrade; /* Number of times that consumes must be met to level up */
	public double culture; /* hammers generated each time hour */
	
	public static void loadConfig(FileConfiguration cfg, Map<Integer, ConfigTradeShipLevel> levels) {
		levels.clear();
		List<Map<?, ?>> tradeship_levels = cfg.getMapList("tradeship_levels");
		for (Map<?, ?> level : tradeship_levels) {
			ConfigTradeShipLevel tradeship_level = new ConfigTradeShipLevel();
			tradeship_level.level = (Integer)level.get("level");
			tradeship_level.culture = (Double)level.get("culture");
			tradeship_level.upgradeTrade = (Integer)level.get("upgradeTrade");
			tradeship_level.maxTrade = (Integer)level.get("maxTrade"); 
			levels.put(tradeship_level.level, tradeship_level);
		}
		CivLog.info("Loaded "+levels.size()+" trade ship levels.");
	}
}