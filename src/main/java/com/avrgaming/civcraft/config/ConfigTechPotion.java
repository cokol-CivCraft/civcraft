package com.avrgaming.civcraft.config;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Resident;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

public class ConfigTechPotion {
    public final String name;
    public final PotionEffectType effect;
    public final Integer amp;
    public final String require_tech;

    public ConfigTechPotion(String name, PotionEffectType effect, Integer amp, String requireTech) {
        this.name = name;
        this.effect = effect;
        this.amp = amp;
        require_tech = requireTech;
    }

    public static void loadConfig(FileConfiguration cfg, Map<String, ConfigTechPotion> techPotions) {
        techPotions.clear();
        for (Map<?, ?> confTech : cfg.getMapList("potions")) {
            ConfigTechPotion tech = new ConfigTechPotion(
                    (String) confTech.get("name"),
                    PotionEffectType.getByName((String) confTech.get("effect")),
                    (Integer) confTech.get("amp"),
                    (String) confTech.get("require_tech")
            );
            techPotions.put(tech.effect.getName() + tech.amp, tech);
        }
        CivLog.info("Loaded " + techPotions.size() + " tech potions.");
    }

    public boolean hasTechnology(Player player) {
        Resident resident = CivGlobal.getResident(player);
        if (resident == null || !resident.hasTown()) {
            return false;
        }

        return resident.getCiv().hasTechnology(require_tech);
    }
}
