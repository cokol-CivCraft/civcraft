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

import java.util.Map;

public class ConfigPerk {
    public final String id;
    public final String display_name;
    public final Material type_id;
    public final Integer data;
    public final String theme;
    public final String template;

    public ConfigPerk(String id, String displayName, Material typeId, Integer data, String theme, String template) {
        this.id = id;
        this.display_name = displayName;
        this.type_id = typeId;
        this.data = data;
        this.theme = theme;
        this.template = template;
    }

    public static void loadConfig(FileConfiguration cfg, Map<String, ConfigPerk> perk_map) {
        perk_map.clear();
        for (Map<?, ?> obj : cfg.getMapList("perks")) {
            ConfigPerk p = new ConfigPerk(
                    (String) obj.get("id"),
                    (String) obj.get("display_name"),
                    Material.getMaterial((String) obj.get("item_id")),
                    (Integer) obj.get("data"),
                    (String) obj.get("theme"),
                    (String) obj.get("template"));


            perk_map.put(p.id, p);
        }
        CivLog.info("Loaded " + perk_map.size() + " Perks.");
    }


}
