package com.avrgaming.civcraft.threading.timers;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;

import java.util.LinkedList;

public class ReduceExposureTimer implements Runnable {

    @Override
    public void run() {

        LinkedList<String> playersToReduce = new LinkedList<>();

        for (Resident resident : CivGlobal.getResidents()) {
            if (!resident.isPerformingMission() && resident.getSpyExposure() > 0) {
                playersToReduce.add(resident.getName());
            }
        }


        class SyncTask implements Runnable {

            public final LinkedList<String> playersToReduce;

            public SyncTask(LinkedList<String> list) {
                playersToReduce = list;
            }

            @Override
            public void run() {
                for (String name : playersToReduce) {
                    Resident resident = CivGlobal.getResident(name);
                    if (resident.getSpyExposure() <= 5) {
                        resident.setSpyExposure(0.0);
                    } else {
                        resident.setSpyExposure(resident.getSpyExposure() - 5);
                    }
                }

            }

        }

        TaskMaster.syncTask(new SyncTask(playersToReduce));

    }

}
