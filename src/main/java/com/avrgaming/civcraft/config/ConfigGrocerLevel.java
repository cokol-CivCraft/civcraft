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
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ConfigGrocerLevel {
    public final int level;
    public final String itemName;
    public final ItemStack item;
    public final double price;

    public ConfigGrocerLevel(int level, String itemName, ItemStack item, double price) {
        this.level = level;
        this.itemName = itemName;
        this.item = item;
        this.price = price;
    }

    public static void loadConfig(FileConfiguration cfg, Map<Integer, ConfigGrocerLevel> levels) {
        levels.clear();
        for (Map<?, ?> level : cfg.getMapList("grocer_levels")) {
            ConfigGrocerLevel grocer_level = new ConfigGrocerLevel(
                    (Integer) level.get("level"),
                    (String) level.get("itemName"),
                    (ItemStack) level.get("item"),
                    (Double) level.get("price")
            );

            levels.put(grocer_level.level, grocer_level);
        }
        CivLog.info("Loaded " + levels.size() + " grocer levels.");
    }

}
