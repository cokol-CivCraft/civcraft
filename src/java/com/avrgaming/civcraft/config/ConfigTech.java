/*
 * AVRGAMING LLC
 * __________________
 *
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.config;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Map;

public record ConfigTech(
        String id,
        String name,
        double beakerCost,
        double cost,
        String requireTechs,
        int era,
        Integer points
) {

    public static void loadConfig(FileConfiguration cfg, Map<String, ConfigTech> tech_maps) {
        tech_maps.clear();
        for (Map<?, ?> confTech : cfg.getMapList("techs")) {
            ConfigTech tech = new ConfigTech(
                    (String) confTech.get("id"),
                    (String) confTech.get("name"),
                    (Double) confTech.get("beaker_cost"),
                    (Double) confTech.get("cost"),
                    (String) confTech.get("require_techs"),
                    (Integer) confTech.get("era"),
                    (Integer) confTech.get("points")
            );

            tech_maps.put(tech.id, tech);
        }
        CivLog.info("Loaded " + tech_maps.size() + " technologies.");
    }

    public static double eraRate(Civilization civ) {
        double era = (CivGlobal.highestCivEra - 1) - civ.getCurrentEra();
        return era > 0 ? (era / 10) : 0.0;
    }

    public double getAdjustedBeakerCost(Civilization civ) {
        return Math.floor(this.beakerCost * Math.max(1.0 - eraRate(civ), .01));
    }

    public double getAdjustedTechCost(Civilization civ) {
        double rate = 1.0;

        for (Town town : civ.getTowns()) {
            if (town.getBuffManager().hasBuff("buff_profit_sharing")) {
                rate -= town.getBuffManager().getEffectiveDouble("buff_profit_sharing");
            }
        }
        rate = Math.max(rate, 0.75);
        rate -= eraRate(civ);

        return Math.floor(this.cost * Math.max(rate, .01));
    }


    public static ArrayList<ConfigTech> getAvailableTechs(Civilization civ) {
        ArrayList<ConfigTech> returnTechs = new ArrayList<>();
        for (ConfigTech tech : CivSettings.techs.values()) {
            if (!civ.hasTechnology(tech.id) && tech.isAvailable(civ)) {
                returnTechs.add(tech);
            }
        }
        return returnTechs;
    }

    public boolean isAvailable(Civilization civ) {
        if (CivGlobal.testFileFlag("debug-norequire")) {
            CivMessage.global("Ignoring requirements! debug-norequire found.");
            return true;
        }

        if (requireTechs == null || requireTechs.isEmpty()) {
            return true;
        }

        for (String reqTech : requireTechs.split(":")) {
            if (!civ.hasTechnology(reqTech)) {
                return false;
            }
        }
        return true;
    }

}
