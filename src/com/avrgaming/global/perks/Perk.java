package com.avrgaming.global.perks;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigPerk;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.global.perks.components.CustomPersonalTemplate;
import com.avrgaming.global.perks.components.PerkComponent;

import java.util.HashMap;
import java.util.List;

public class Perk {

    public static HashMap<String, Perk> staticPerks = new HashMap<>();

    private String ident;
    private final HashMap<ComponentsNames, PerkComponent> components = new HashMap<>();
    public ConfigPerk configPerk;
    public int count;
    public String provider;

    public Perk(ConfigPerk config) {
        this.configPerk = config;
        this.ident = config.id;
        this.count = 1;
        buildComponents();
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

    public void setIdent(String ident) {
        this.ident = ident;
    }

    public enum ComponentsNames {
        ChangeWeather(com.avrgaming.global.perks.components.ChangeWeather.class),
        CustomPersonalTemplate(CustomPersonalTemplate.class),
        CustomTemplate(com.avrgaming.global.perks.components.CustomTemplate.class),
        RenameCivOrTown(com.avrgaming.global.perks.components.RenameCivOrTown.class);
        private final Class<? extends PerkComponent> component;

        ComponentsNames(Class<? extends PerkComponent> component) {
            this.component = component;
        }

        public Class<? extends PerkComponent> getComponent() {
            return component;
        }
    }

    private void buildComponents() {
        List<HashMap<String, String>> compInfoList = this.configPerk.components;
        if (compInfoList == null) {
            return;
        }
        for (HashMap<String, String> compInfo : compInfoList) {
            try {

                ComponentsNames component_class = ComponentsNames.valueOf(compInfo.get("name"));
                PerkComponent perkCompClass = component_class.getComponent().newInstance();
                perkCompClass.setName(compInfo.get("name"));
                perkCompClass.setParent(this);

                for (String key : compInfo.keySet()) {
                    perkCompClass.setAttribute(key, compInfo.get(key));
                }

                perkCompClass.createComponent();
                this.components.put(component_class, perkCompClass);
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void onActivate(Resident resident) {
        for (PerkComponent perk : this.components.values()) {
            perk.onActivate(resident);
        }
    }

    public String getDisplayName() {
        return configPerk.display_name;
    }

    public PerkComponent getComponent(ComponentsNames key) {
        return this.components.get(key);
    }

}
