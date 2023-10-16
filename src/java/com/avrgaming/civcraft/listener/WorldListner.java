package com.avrgaming.civcraft.listener;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class WorldListner implements Listener {
    public static void save_all() {
        for (Civilization civ : Civilization.getCivs()) {
            civ.save();
        }
        for (Town town : Town.getTowns()) {
            town.save();
        }
    }

    @EventHandler()
    public void onWorldSave(WorldSaveEvent event) {
        if (!event.getWorld().getName().equals("world")) {
            return;
        }
        CivLog.debug("World saving");
        save_all();

    }

    @EventHandler()
    public void onWorldUnload(WorldUnloadEvent event) {
        if (!event.getWorld().getName().equals("world")) {
            return;
        }
        CivLog.debug("World unloading");
        save_all();
    }
}
