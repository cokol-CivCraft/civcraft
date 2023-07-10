package com.avrgaming.civcraft.threading.tasks;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;
import org.bukkit.entity.Player;

public class PlayerArmorSpeedAsyncTask implements Runnable {

    Player player;

    public PlayerArmorSpeedAsyncTask(Player player) {
        this.player = player;
    }

    @Override
    public void run() {
        doArmorSpeedCheck();
    }

    public void doArmorSpeedCheck() {
        Resident resident = CivGlobal.getResident(this.player);
        resident.calculateWalkingModifier(this.player);
    }

}
