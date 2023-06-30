package com.avrgaming.civcraft.config;

import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Resident;

public class ConfigTechPotion {
	public String name;
	public PotionEffectType effect;
	public Integer amp;
	public String require_tech;
	
	public static void loadConfig(FileConfiguration cfg, Map<String, ConfigTechPotion> techPotions) {
		techPotions.clear();
		List<Map<?, ?>> techs = cfg.getMapList("potions");
		for (Map<?, ?> confTech : techs) {
			ConfigTechPotion tech = new ConfigTechPotion();
			
			tech.name = (String)confTech.get("name");
			String effect = (String)confTech.get("effect");
			tech.effect = PotionEffectType.getByName(effect);
			tech.amp = (Integer)confTech.get("amp");
			tech.require_tech = (String)confTech.get("require_tech");			
			techPotions.put(""+effect+tech.amp, tech);
		}
		CivLog.info("Loaded "+techPotions.size()+" tech potions.");		
	}
	
	public boolean hasTechnology(Player player) {
		Resident resident = CivGlobal.getResident(player);
		if (resident == null || !resident.hasTown()) {
			return false;
		}
		
		if (!resident.getCiv().hasTechnology(require_tech)) {
			return false;
		}
		
		return true;
	}
}
