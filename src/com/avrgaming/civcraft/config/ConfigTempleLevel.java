/*
 * AVRGAMING LLC
 * __________________
 *
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,TICE:  All information contained herein is, and remains
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

public class ConfigTempleLevel {
    public int level;            /* Current level number */
    public Map<Material, Integer> consumes; /* A map of block ID's and amounts required for this level to progress */
    public int count; /* Number of times that consumes must be met to level up */
    public double culture; /* Culture generated each time for the temple */

    public ConfigTempleLevel() {

    }

    @SuppressWarnings("unused")
    public ConfigTempleLevel(ConfigTempleLevel currentLVL) {
        this.level = currentLVL.level;
        this.count = currentLVL.count;
        this.culture = currentLVL.culture;

        this.consumes = new HashMap<>(currentLVL.consumes);

    }


    public static void loadConfig(FileConfiguration cfg, Map<Integer, ConfigTempleLevel> temple_levels) {
        temple_levels.clear();
        List<Map<?, ?>> temple_list = cfg.getMapList("temple_levels");
        Map<Material, Integer> consumes_list;
        for (Map<?, ?> cl : temple_list) {
            List<?> consumes = (List<?>) cl.get("consumes");
            if (consumes == null) {
                continue;
            }
            consumes_list = new HashMap<>();
            for (Object consume : consumes) {
                String[] split = ((String) consume).split(",");
                consumes_list.put(Material.getMaterial(Integer.parseInt(split[0])), Integer.valueOf(split[1]));
            }


            ConfigTempleLevel templeLevel = new ConfigTempleLevel();
            templeLevel.level = (Integer) cl.get("level");
            templeLevel.consumes = consumes_list;
            templeLevel.count = (Integer) cl.get("count");
            templeLevel.culture = (Double) cl.get("culture");

            temple_levels.put(templeLevel.level, templeLevel);

        }
        CivLog.info("Loaded " + temple_levels.size() + " temple levels.");
    }

}
