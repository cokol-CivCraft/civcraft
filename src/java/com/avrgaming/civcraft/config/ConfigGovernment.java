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

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Civilization;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigGovernment {
    public final String id;
    public final String displayName;
    public final String require_tech;

    public final double trade_rate;
    public final double upkeep_rate;
    public final double cottage_rate;
    public final double growth_rate;
    public final double culture_rate;
    public final double hammer_rate;
    public final double beaker_rate;
    public final double maximum_tax_rate;
    public final double trommel_rate;

    public ConfigGovernment(String id, String displayName, String requireTech, double tradeRate, double upkeepRate, double cottageRate, double growthRate, double cultureRate, double hammerRate, double beakerRate, double maximumTaxRate, double trommelRate) {
        this.id = id;
        this.displayName = displayName;
        require_tech = requireTech;
        trade_rate = tradeRate;
        upkeep_rate = upkeepRate;
        cottage_rate = cottageRate;
        growth_rate = growthRate;
        culture_rate = cultureRate;
        hammer_rate = hammerRate;
        beaker_rate = beakerRate;
        maximum_tax_rate = maximumTaxRate;
        trommel_rate = trommelRate;
    }

    public static void loadConfig(FileConfiguration cfg, Map<String, ConfigGovernment> government_map) {
        government_map.clear();
        List<Map<?, ?>> techs = cfg.getMapList("governments");
        for (Map<?, ?> level : techs) {
            ConfigGovernment gov = new ConfigGovernment(
                    (String) level.get("id"),
                    (String) level.get("displayName"),
                    (String) level.get("require_tech"),
                    (Double) level.get("trade_rate"),
                    (Double) level.get("upkeep_rate"),
                    (Double) level.get("cottage_rate"),
                    (Double) level.get("growth_rate"),
                    (Double) level.get("culture_rate"),
                    (Double) level.get("hammer_rate"),
                    (Double) level.get("beaker_rate"),
                    (Double) level.get("maximum_tax_rate"),
                    (Double) level.get("trommel_rate")
            );

            government_map.put(gov.id, gov);
        }
        CivLog.info("Loaded " + government_map.size() + " governments.");
    }

    public static ArrayList<ConfigGovernment> getAvailableGovernments(Civilization civ) {
        ArrayList<ConfigGovernment> govs = new ArrayList<>();

        for (ConfigGovernment gov : CivSettings.governments.values()) {
            if (gov.id.equalsIgnoreCase("gov_anarchy")) {
                continue;
            }
            if (gov.isAvailable(civ)) {
                govs.add(gov);
            }
        }

        return govs;
    }

    public static ConfigGovernment getGovernmentFromName(String string) {

        for (ConfigGovernment gov : CivSettings.governments.values()) {
            if (gov.id.equalsIgnoreCase("gov_anarchy")) {
                continue;
            }
            if (gov.displayName.equalsIgnoreCase(string)) {
                return gov;
            }
        }

        return null;
    }

    public boolean isAvailable(Civilization civ) {
        return civ.hasTechnology(this.require_tech);
    }

}
