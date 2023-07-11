package com.avrgaming.global.perks.components;

import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.template.Template;
import org.bukkit.entity.Player;

import java.io.IOException;

public class CustomPersonalTemplate extends PerkComponent {


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
