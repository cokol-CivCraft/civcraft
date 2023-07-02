package com.avrgaming.global.perks.components;

import java.sql.SQLException;
import java.util.HashMap;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.global.perks.NotVerifiedException;
import com.avrgaming.global.perks.Perk;


public class PerkComponent {
	
	private final HashMap<String, String> attributes = new HashMap<>();
    private String name;
	private Perk parent;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public String getString(String key) {
		return attributes.get(key);
	}
	
	public double getDouble(String key) {
        return Double.parseDouble(attributes.get(key));
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
	
	public void markAsUsed(Resident resident) {
        this.getParent().count--;
        if (this.getParent().count <= 0) {
            resident.perks.remove(this.getParent().getIdent());
        }

        CivGlobal.perkManager.markAsUsed(resident, this.getParent());
    }
	
	public void onActivate(Resident resident) {}
	public void createComponent() {}

}
