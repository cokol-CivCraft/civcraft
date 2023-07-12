package com.avrgaming.global.perks;

import com.avrgaming.civcraft.config.ConfigPerk;

public class Perk {
    private final String ident;
    public ConfigPerk configPerk;
    public final String theme;
    public final String template;

    public Perk(ConfigPerk config) {
        this.configPerk = config;
        this.ident = config.id;
        this.theme = config.theme;
        this.template = config.template;
    }

    public String getIdent() {
        return ident;
    }

    public String getDisplayName() {
        return configPerk.display_name;
    }

}
