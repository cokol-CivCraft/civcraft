package com.avrgaming.global.perks.components;

import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.global.perks.Perk;

import java.util.HashMap;


public class PerkComponent {

    private final HashMap<String, String> attributes = new HashMap<>();
    private Perk parent;

    public String getString(String key) {
        return attributes.get(key);
    }

    public void setAttribute(String key, String value) {
        attributes.put(key, value);
    }

    public Perk getParent() {
        return parent;
    }

    public void setParent(Perk parent) {
        this.parent = parent;
    }

    public void onActivate(Resident resident) {
    }

}
