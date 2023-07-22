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
import java.util.Set;

public class ConfigStableItem {
    public final String name;
    public final double cost;
    public final int store_id;
    public final Material item_id;
    public final int horse_id;

    public ConfigStableItem(String name, double cost, int storeId, Material itemId, int horseId) {
        this.name = name;
        this.cost = cost;
        store_id = storeId;
        item_id = itemId;
        horse_id = horseId;
    }

    public static void loadConfig(FileConfiguration cfg, Set<ConfigStableItem> items) {
        items.clear();
        for (Map<?, ?> level : cfg.getMapList("stable_items")) {
            ConfigStableItem itm = new ConfigStableItem(
                    (String) level.get("name"),
                    (Double) level.get("cost"),
                    (Integer) level.get("store_id"),
                    Material.getMaterial((Integer) level.get("item_id")),
                    (Integer) level.get("horse_id")
            );

            items.add(itm);
        }
        CivLog.info("Loaded " + items.size() + " stable items.");
    }

}
