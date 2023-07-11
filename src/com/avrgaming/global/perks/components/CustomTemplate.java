package com.avrgaming.global.perks.components;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.template.Template;
import org.bukkit.entity.Player;

import java.io.IOException;

public class CustomTemplate extends PerkComponent {

    public Template getTemplate(Player player, Buildable buildable) {
        Template tpl = new Template();
        try {
            tpl.initTemplate(player.getLocation(), buildable, this.getString("theme"));
        } catch (CivException | IOException e) {
            e.printStackTrace();
        }
        return tpl;
    }

}
