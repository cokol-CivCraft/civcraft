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
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

public class ConfigTechItem {
    public final Material id;
    public final String name;
    public final String require_tech;

    public ConfigTechItem(Material id, String name, String requireTech) {
        this.id = id;
        this.name = name;
        require_tech = requireTech;
    }

    public static void loadConfig(FileConfiguration cfg, Map<Material, ConfigTechItem> tech_maps) {
        tech_maps.clear();
        for (Map<?, ?> confTech : cfg.getMapList("items")) {
            ConfigTechItem tech = new ConfigTechItem(
                    Material.getMaterial((Integer) confTech.get("id")),
                    (String) confTech.get("name"),
                    (String) confTech.get("require_tech")
            );
            tech_maps.put(tech.id, tech);
        }
        CivLog.info("Loaded " + tech_maps.size() + " technologies.");
    }

}
