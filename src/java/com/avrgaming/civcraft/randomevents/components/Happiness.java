package com.avrgaming.civcraft.randomevents.components;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.randomevents.RandomEventComponent;

public class Happiness extends RandomEventComponent {

    @Override
    public void process() {
        int happiness = Integer.parseInt(this.getString("value"));
        int duration = Integer.parseInt(this.getString("duration"));

        CivGlobal.getSessionDB().add(getKey(this.getParentTown()), happiness + ":" + duration, this.getParentTown().getCiv().getUUID(), this.getParentTown().getUUID());
        sendMessage(CivSettings.localize.localizedString("var_re_happiness1", happiness, duration));
    }

    public static String getKey(Town town) {
        return "randomevent:happiness:" + town.getId();
    }

}
