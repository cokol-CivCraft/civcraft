package com.avrgaming.civcraft.config;

import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivLog;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigRecipeShapless extends ConfigRecipe implements ConfigurationSerializable {
    public final LoreMaterial material;
    public final ItemStack[] ingridients;

    public ConfigRecipeShapless(LoreMaterial material, ItemStack[] ingridients) {
        this.material = material;
        this.ingridients = ingridients;
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

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("output", this.material.getId());
        data.put("ingruduents", this.ingridients);
        return data;
    }
//    public static ConfigRecipeShapless deserialize(Map<String, Object> data) {
//
//    }

}
