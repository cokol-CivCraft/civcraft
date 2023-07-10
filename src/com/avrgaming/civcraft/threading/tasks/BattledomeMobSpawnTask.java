package com.avrgaming.civcraft.threading.tasks;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.structure.wonders.Battledome;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.threading.CivAsyncTask;

import java.util.HashSet;

public class BattledomeMobSpawnTask extends CivAsyncTask {

    Battledome battledome;

    public static HashSet<String> debugTowns = new HashSet<>();

    public static void debug(Battledome battledome, String msg) {
        if (debugTowns.contains(battledome.getTown().getName())) {
            CivLog.warning("BattledomeDebug:" + battledome.getTown().getName() + ":" + msg);
        }
    }

    public BattledomeMobSpawnTask(Wonder battledome) {
        this.battledome = (Battledome) battledome;
    }

    public void processBattledomeSpawn() {
        if (!battledome.isActive()) {
            debug(battledome, "Battledome inactive...");
            return;
        }
//		World world = Bukkit.getWorld("world");

//        world.spawnCreature(pLoc, EntityType.SKELETON);

        debug(battledome, "Processing Battledome...");
    }

    @Override
    public void run() {
        if (this.battledome.lock.tryLock()) {
            try {
                try {
                    processBattledomeSpawn();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } finally {
                this.battledome.lock.unlock();
            }
        } else {
            debug(this.battledome, "Failed to get lock while trying to start task, aborting.");
        }
    }

}
