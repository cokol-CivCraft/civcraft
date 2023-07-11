package com.avrgaming.global.perks.components;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.global.perks.Perk;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;

public class CustomTemplate extends PerkComponent {

    private String getTemplateSessionKey(Town town) {
        return "customtemplate:" + town.getName() + ":" + this.getString("template");
    }

    private static String getTemplateSessionKey(Town town, String buildableBaseName) {
        return "customtemplate:" + town.getName() + ":" + buildableBaseName;
    }

    private static String getTemplateSessionValue(Perk perk, Resident resident) {
        return perk.getIdent() + ":" + resident.getName();
    }

    public void bindTemplateToTown(Town town, Resident resident) {
        CivGlobal.getSessionDB().add(getTemplateSessionKey(town), getTemplateSessionValue(this.getParent(), resident),
                town.getCiv().getId(), town.getId(), 0);
    }

    public boolean hasTownTemplate(Town town) {
        for (SessionEntry entry : CivGlobal.getSessionDB().lookup(getTemplateSessionKey(town))) {
            if (this.getParent().getIdent().equals(entry.value.split(":")[0])) {
                return true;
            }
        }

        return false;
    }

    public static ArrayList<Perk> getTemplatePerksForBuildable(Town town, String buildableBaseName) {
        ArrayList<Perk> perks = new ArrayList<>();

        for (SessionEntry entry : CivGlobal.getSessionDB().lookup(getTemplateSessionKey(town, buildableBaseName))) {
            String[] split = entry.value.split(":");

            Perk perk = Perk.staticPerks.get(split[0]);
            if (perk == null) {
                CivLog.warning("Unknown perk in session db:" + split[0]);
                continue;
            }
            Perk tmpPerk = new Perk(perk.configPerk);
            tmpPerk.provider = split[1];
            perks.add(tmpPerk);

        }

        return perks;
    }


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
