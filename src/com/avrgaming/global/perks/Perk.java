package com.avrgaming.global.perks;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigPerk;

import java.util.HashMap;

public class Perk {

    public static HashMap<String, Perk> staticPerks = new HashMap<>();

    private final String ident;
    public ConfigPerk configPerk;
    public int count;
    public String provider;
    public final String theme;
    public final String template;

    public Perk(ConfigPerk config) {
        this.configPerk = config;
        this.ident = config.id;
        this.theme = config.theme;
        this.template = config.template;
        this.count = 1;
    }

    public static void init() {
        for (ConfigPerk configPerk : CivSettings.perks.values()) {
            Perk p = new Perk(configPerk);
            staticPerks.put(p.getIdent(), p);
        }
    }

    public String getIdent() {
        return ident;
    }

    public String getDisplayName() {
        return configPerk.display_name;
    }

}
