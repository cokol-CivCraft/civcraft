package com.avrgaming.civcraft.config;

import com.avrgaming.civcraft.main.CivLog;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;

public class ConfigHappinessState {
    public final int level;
    public final String name;
    public final ChatColor color;
    public final double amount;
    public final double beaker_rate;
    public final double coin_rate;
    public final double culture_rate;
    public final double hammer_rate;

    public ConfigHappinessState(int level, String name, ChatColor color, double amount, double beakerRate, double coinRate, double cultureRate, double hammerRate) {
        this.level = level;
        this.name = name;
        this.color = color;
        this.amount = amount;
        beaker_rate = beakerRate;
        coin_rate = coinRate;
        culture_rate = cultureRate;
        hammer_rate = hammerRate;
    }

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
