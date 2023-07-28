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

public class ConfigHemisphere {
    public final String id;
    public final int x_min;
    public final int x_max;
    public final int z_min;
    public final int z_max;

    public ConfigHemisphere(String id, int xMin, int xMax, int zMin, int zMax) {
        this.id = id;
        x_min = xMin;
        x_max = xMax;
        z_min = zMin;
        z_max = zMax;
    }


    public static void loadConfig(FileConfiguration cfg, Map<String, ConfigHemisphere> hemis) {
        hemis.clear();
        List<Map<?, ?>> configHemis = cfg.getMapList("hemispheres");
        for (Map<?, ?> b : configHemis) {
            ConfigHemisphere buff = new ConfigHemisphere(
                    (String) b.get("id"),
                    (Integer) b.get("x_min"),
                    (Integer) b.get("x_max"),
                    (Integer) b.get("z_min"),
                    (Integer) b.get("z_max")
            );
            hemis.put(buff.id, buff);
        }

        CivLog.info("Loaded " + hemis.size() + " Hemispheres.");
    }

}
