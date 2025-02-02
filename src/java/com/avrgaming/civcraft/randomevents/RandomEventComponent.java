package com.avrgaming.civcraft.randomevents;

import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;

import java.util.HashMap;

public abstract class RandomEventComponent {
    private String name;
    private final HashMap<String, String> attributes = new HashMap<>();
    private RandomEvent parent;

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

    protected Town getParentTown() {
        return parent.getTown();
    }

    protected RandomEvent getParent() {
        return parent;
    }

    protected void sendMessage(String message) {
        CivMessage.sendTown(parent.getTown(), message);
        parent.savedMessages.add(message);
    }


    public void createComponent(RandomEvent parent) {
        this.parent = parent;
    }

    public abstract void process();

    public boolean onCheck() {
        return false;
    }

    public void onStart() {
    }

    public void onCleanup() {
    }

    public boolean requiresActivation() {
        return false;
    }

}
