package com.avrgaming.civcraft.randomevents.components;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.randomevents.RandomEventComponent;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.Random;

public class PickRandomLocation extends RandomEventComponent {

    @Override
    public void process() {

        TaskMaster.syncTask(() -> {
            int max_x;
            int max_z;
            int min_x;
            int min_z;
            try {
                max_x = CivSettings.getInteger(CivSettings.randomEventsConfig, "max_x");
                max_z = CivSettings.getInteger(CivSettings.randomEventsConfig, "max_z");
                min_x = CivSettings.getInteger(CivSettings.randomEventsConfig, "min_x");
                min_z = CivSettings.getInteger(CivSettings.randomEventsConfig, "min_z");
            } catch (InvalidConfiguration e) {
                e.printStackTrace();
                return;
            }

            int range_x = max_x - min_x;
            int range_z = max_z - min_z;

            Random rand = new Random();
            int randX = rand.nextInt(range_x) - max_x;
            int randZ = rand.nextInt(range_z) - max_z;

            /* XXX only pick in "world" */
            World world = Bukkit.getWorld("world");
            int y = world.getHighestBlockYAt(randX, randZ);

            BlockCoord bcoord = new BlockCoord(world.getName(), randX, y, randZ);

            String varname = getString("varname");
            PickRandomLocation.this.getParent().componentVars.put(varname, bcoord.toString());

            sendMessage(CivSettings.localize.localizedString("var_re_pickRandom", bcoord.getX() + "," + bcoord.getY() + "," + bcoord.getZ()));
        });
    }
}
