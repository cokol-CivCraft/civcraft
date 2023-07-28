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
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;

public class ConfigEnchant {

    public final String id;
    public final String name;
    public final String description;
    public final double cost;
    public final String enchant_id;

    public ConfigEnchant(String id, String name, String description, double cost, String enchantId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.cost = cost;
        this.enchant_id = enchantId;
    }

    public static void loadConfig(FileConfiguration cfg, Map<String, ConfigEnchant> enchant_map) {
        enchant_map.clear();
        List<Map<?, ?>> techs = cfg.getMapList("enchants");
        for (Map<?, ?> level : techs) {
            ConfigEnchant enchant = new ConfigEnchant(
                    (String) level.get("id"),
                    (String) level.get("name"),
                    (String) level.get("description"),
                    (Double) level.get("cost"),
                    (String) level.get("enchant_id")
            );
            enchant_map.put(enchant.id, enchant);
        }
        CivLog.info("Loaded " + enchant_map.size() + " enchantments.");
    }
}
