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

import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.StructuresTypes;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class ConfigBuildableInfo {
    public String id = "";
    public String template_base_name = "";
    public int templateYShift = 0;
    public String displayName = "";
    public String require_tech = "";
    public String require_upgrade = "";
    public String require_structure = "";
    public String check_event = "";
    public String effect_event = "";
    public String update_event = "";
    public String onBuild_event = "";
    public int limit = 0;
    public ArrayList<String> signs = new ArrayList<>();
    public double cost = 0;
    public double upkeep = 0;
    public double hammer_cost = 0;
    public int max_hp = 0;
    public Boolean destroyable = false;
    public Boolean allow_outside_town = false;
    public Boolean nationalWonder = false;
    public Integer regenRate = 0;
    public Boolean tile_improvement = false;
    public Integer points = 0;
    public boolean allow_demolish = false;
    public boolean strategic = false;
    public boolean water_structure = false;
    public boolean ignore_floating = false;
    public List<HashMap<String, String>> components = new LinkedList<>();
    public StructuresTypes type;

    public boolean isAvailable(Town town) {
        if (
                town.hasTechnology(require_tech) &&
                        town.hasUpgrade(require_upgrade) &&
                        town.hasStructure(require_structure)
        ) {
            if (limit == 0 || town.getStructureTypeCount(id) < limit) {
                boolean capitol = town.isCapitol();

                if (id.equals("s_townhall") && capitol) {
                    return false;
                }

                if (id.equals("s_capitol") && !capitol) {
                    return false;
                }
                if (id.equals("w_colosseum")) {
                    return capitol && town.getStructureTypeCount(id) <= 0;
                }

                return true;
            }
        }
        return false;
    }

    public ArrayList<ConfigTemplate> getTemplates() {
        return CivSettings.templates.getOrDefault(this.template_base_name, new ArrayList<>());
    }

    public static void loadConfig(FileConfiguration structures, FileConfiguration wonders, Map<String, ConfigBuildableInfo> structureMap) {
        structureMap.clear();
        for (File config_file : Objects.requireNonNull(new File(CivCraft.getPlugin().getDataFolder().getPath() + "/data/structures").listFiles(File::isFile))) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(config_file);

            ConfigBuildableInfo sinfo = new ConfigBuildableInfo();

            sinfo.id = FilenameUtils.removeExtension(config_file.getName());

            sinfo.type = StructuresTypes.valueOf(config.getString("type", "base").toUpperCase());
            sinfo.template_base_name = config.getString("template");
            sinfo.templateYShift = config.getInt("template_y_shift");
            sinfo.displayName = config.getString("displayName");
            sinfo.require_tech = config.getString("require_tech");
            sinfo.require_upgrade = config.getString("require_upgrade");
            sinfo.require_structure = config.getString("require_structure");
            sinfo.check_event = config.getString("check_event");
            sinfo.effect_event = config.getString("effect_event");
            sinfo.update_event = config.getString("update_event");
            sinfo.onBuild_event = config.getString("onBuild_event");
            sinfo.limit = config.getInt("limit");
            //TODO handle signs
            sinfo.cost = config.getDouble("cost");
            sinfo.upkeep = config.getDouble("upkeep");
            sinfo.hammer_cost = config.getDouble("hammer_cost");
            sinfo.max_hp = config.getInt("max_hitpoints");
            sinfo.destroyable = config.getBoolean("destroyable");
            sinfo.allow_outside_town = config.getBoolean("allow_outside_town");
            sinfo.regenRate = config.getInt("regen_rate");
            sinfo.points = config.getInt("points");
            sinfo.water_structure = config.getBoolean("onwater");
            sinfo.nationalWonder = config.getBoolean("national_wonder");
            for (Map<?, ?> compObj : config.getMapList("components")) {

                HashMap<String, String> compMap = new HashMap<>();
                for (Object key : compObj.keySet()) {
                    compMap.put((String) key, (String) compObj.get(key));
                }

                sinfo.components.add(compMap);
            }


            sinfo.tile_improvement = config.getBoolean("tile_improvement");

            sinfo.allow_demolish = config.getBoolean("allow_demolish", true);

            sinfo.strategic = config.getBoolean("strategic");

            sinfo.ignore_floating = config.getBoolean("ignore_floating");

            structureMap.put(sinfo.id, sinfo);
        }

        CivLog.info("Loaded " + structureMap.size() + " structures.");
    }
}
