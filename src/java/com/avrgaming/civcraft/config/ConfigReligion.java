package com.avrgaming.civcraft.config;

import com.avrgaming.civcraft.main.CivLog;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;

public class ConfigReligion {
    /**
     * #
     * #Язычество --- сток
     * #Атеизм --- /t cfaith (?)
     * #Буддизм --- культура и религия + счастье
     * #Ислам --- чет военное ??
     * #Православие --- +цены в банк и -несчастье
     * #Католицизм --- -потребление коттеджей
     * #Протестантизм --- молотки
     * #
     **/
    public String id;
    public String displayName;
    public int requireFaith;
    public ReligionType relType;
    public double tax_rate;


    public enum ReligionType {
        WAR, ECONOMY, SCIENTIFIC, CULTURAL, PRODUCTION, HAPPINESS, NONE
    }

    public static ReligionType getRTfromString(String s) {
        for (ReligionType rt : ReligionType.values()) {
            if (rt.toString().equals(s.toUpperCase())) {
                return rt;
            }
        }
        return null;
    }

    public static void loadConfig(FileConfiguration cfg, Map<String, ConfigReligion> religion_map) {
        religion_map.clear();
        List<Map<?, ?>> techs = cfg.getMapList("religions");
        for (Map<?, ?> level : techs) {
            ConfigReligion rel = new ConfigReligion();

            rel.id = (String) level.get("id");
            rel.displayName = (String) level.get("displayName");
            rel.requireFaith = (Integer) level.get("required_points");
            rel.relType = getRTfromString((String) level.get("religion_type")); // TODO: make more ,__, and add commands
            rel.tax_rate = (Double) level.get("income_rate");

            religion_map.put(rel.id, rel);
        }
        CivLog.info("Loaded " + religion_map.size() + " religions.");
    }

}
