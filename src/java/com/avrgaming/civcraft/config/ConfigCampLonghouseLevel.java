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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConfigCampLonghouseLevel {
    public int level;            /* Current level number */
    public Map<Material, Integer> consumes; /* A map of block ID's and amounts required for this level to progress */
    public int count; /* Number of times that consumes must be met to level up */
    public double coins; /* Coins generated each time for the cottage */

    public ConfigCampLonghouseLevel() {

    }

    @SuppressWarnings("unused")
    public ConfigCampLonghouseLevel(ConfigCampLonghouseLevel currentLVL) {
        this.level = currentLVL.level;
        this.count = currentLVL.count;
        this.coins = currentLVL.coins;

        this.consumes = new HashMap<>(currentLVL.consumes);

    }


    public static void loadConfig(FileConfiguration cfg, Map<Integer, ConfigCampLonghouseLevel> longhouse_levels) {
        longhouse_levels.clear();
        List<Map<?, ?>> list = cfg.getMapList("longhouse_levels");
        for (Map<?, ?> cl : list) {
            List<?> consumes = (List<?>) cl.get("consumes");
            if (consumes == null) {
                continue;
            }
            Map<Material, Integer> consumes_list = new HashMap<>();
            for (Object consume : consumes) {
                String[] split = ((String) consume).split(",");
                consumes_list.put(Objects.requireNonNull(Material.getMaterial(split[0])), Integer.valueOf(split[1]));
            }


            ConfigCampLonghouseLevel level = new ConfigCampLonghouseLevel();
            level.level = (Integer) cl.get("level");
            level.consumes = consumes_list;
            level.count = (Integer) cl.get("count");
            level.coins = (Double) cl.get("coins");

            longhouse_levels.put(level.level, level);

        }
        CivLog.info("Loaded " + longhouse_levels.size() + " longhouse levels.");
    }
}
