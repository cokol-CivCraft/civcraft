package com.avrgaming.civcraft.config;
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


import com.avrgaming.civcraft.main.CivLog;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigFishing {
    public final String craftMatId;
    public final Material type_id;
    public final double drop_chance;

    public ConfigFishing(String craftMatId, Material typeId, double dropChance) {
        this.craftMatId = craftMatId;
        type_id = typeId;
        drop_chance = dropChance;
    }

    public static void loadConfig(FileConfiguration cfg, ArrayList<ConfigFishing> configList) {
        configList.clear();
        List<Map<?, ?>> drops = cfg.getMapList("fishing_drops");
        for (Map<?, ?> item : drops) {
            ConfigFishing g = new ConfigFishing(
                    (String) item.get("craftMatId"),
                    Material.getMaterial((String) item.get("type_id")),
                    (Double) item.get("drop_chance")
            );

            configList.add(g);
        }
        CivLog.info("Loaded " + configList.size() + " fishing drops.");

    }

}


