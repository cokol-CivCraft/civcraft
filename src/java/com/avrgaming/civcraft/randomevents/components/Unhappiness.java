package com.avrgaming.civcraft.randomevents.components;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.randomevents.RandomEventComponent;

public class Unhappiness extends RandomEventComponent {

    public static String getKey(Town town) {
        return "randomevent:unhappiness:" + town.getUUID();
    }


    @Override
    public void process() {

        int unhappiness = Integer.parseInt(this.getString("value"));
        int duration = Integer.parseInt(this.getString("duration"));

        CivGlobal.getSessionDB().add(getKey(this.getParentTown()), unhappiness + ":" + duration, this.getParentTown().getCiv().getUUID(), this.getParentTown().getUUID());
        sendMessage(CivSettings.localize.localizedString("var_re_unhappiness1", unhappiness, duration));

    }

}
