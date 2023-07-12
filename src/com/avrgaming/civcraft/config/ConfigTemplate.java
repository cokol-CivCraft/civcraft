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

import java.util.ArrayList;
import java.util.Map;

public class ConfigTemplate {
    public final String display_name;
    public final Material type_id;
    public final Integer data;
    public final String theme;
    public final String template;

    public ConfigTemplate(String displayName, Material typeId, Integer data, String theme, String template) {
        this.display_name = displayName;
        this.type_id = typeId;
        this.data = data;
        this.theme = theme;
        this.template = template;
    }

    public static void loadConfig(FileConfiguration cfg, ArrayList<ConfigTemplate> template_map) {
        template_map.clear();
        for (Map<?, ?> obj : cfg.getMapList("perks")) {
            ConfigTemplate p = new ConfigTemplate(
                    (String) obj.get("display_name"),
                    Material.getMaterial((String) obj.get("item_id")),
                    (Integer) obj.get("data"),
                    (String) obj.get("theme"),
                    (String) obj.get("template"));


            template_map.add(p);
        }
        CivLog.info("Loaded " + template_map.size() + " Perks.");
    }


}
