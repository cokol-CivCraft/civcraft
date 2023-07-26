package com.avrgaming.global.perks.components;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.util.CivColor;
import org.bukkit.entity.Player;

import java.io.IOException;

public class CustomPersonalTemplate extends PerkComponent {

    @Override
    public void onActivate(Resident resident) {
        CivMessage.send(resident, CivColor.LightGreen + CivSettings.localize.localizedString("customTemplate_personal"));
    }


    public Template getTemplate(Player player, ConfigBuildableInfo info) {
        Template tpl = new Template();
        try {
            tpl.initTemplate(player.getLocation(), info, this.getString("theme"));
        } catch (CivException | IOException e) {
            e.printStackTrace();
        }

        return tpl;
    }
}
