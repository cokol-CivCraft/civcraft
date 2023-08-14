package com.avrgaming.civcraft.config;

import com.avrgaming.civcraft.main.CivLog;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;

public record ConfigHappinessState(
        int level,
        String name,
        ChatColor color,
        double amount,
        double beaker_rate,
        double coin_rate,
        double culture_rate,
        double hammer_rate
) {
    public static void loadConfig(FileConfiguration cfg, Map<Integer, ConfigHappinessState> happiness_states) {
        happiness_states.clear();
        List<Map<?, ?>> list = cfg.getMapList("happiness.states");
        for (Map<?, ?> cl : list) {

            ConfigHappinessState happy_level = new ConfigHappinessState(
                    (Integer) cl.get("level"),
                    (String) cl.get("name"),
                    ChatColor.valueOf((String) cl.get("color")),
                    (Double) cl.get("amount"),
                    (Double) cl.get("beaker_rate"),
                    (Double) cl.get("coin_rate"),
                    (Double) cl.get("culture_rate"),
                    (Double) cl.get("hammer_rate")
            );


            happiness_states.put(happy_level.level, happy_level);

        }
        CivLog.info("Loaded " + happiness_states.size() + " Happiness States.");
    }
}
