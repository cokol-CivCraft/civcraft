package com.avrgaming.civcraft.config;

import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ConfigMaterial {

    /* Required */
    public String id;
    public Material item_id;
    public int item_data;
    public String name;
    public String category = CivSettings.localize.localizedString("config_material_misc");
    public String categoryCivColortripped = category;
    public int tier;

    /* Optional */
    public String[] lore = null;
    public boolean craftable = false;
    public String required_tech = null;
    public boolean shaped = false;
    public boolean shiny = false;
    public boolean tradeable = false;
    public HashMap<String, LoreCraftableMaterial.ConfigIngredient> ingredients;
    public String[] shape;
    public List<HashMap<String, String>> components = new LinkedList<>();
    public boolean vanilla = false;
    public int amount = 1;
    public double tradeValue = 0;

    public static void loadConfig(FileConfiguration cfg, Map<String, ConfigMaterial> materials) {
        materials.clear();

        ConfigurationSection materials_items = cfg.getConfigurationSection("materials");
        for (String id : materials_items.getKeys(false)) {
            ConfigMaterial mat = new ConfigMaterial();
            ConfigurationSection data = materials_items.getConfigurationSection(id);

            /* Mandatory Settings */
            mat.id = id;
            mat.item_id = Material.getMaterial(data.getInt("item_id"));
            mat.item_data = data.getInt("item_data");
            if (!(data.get("item") instanceof ItemStack)) {
                CivLog.info("not found item in " + mat.id);
            }
            mat.name = CivColor.colorize(data.getString("name"));


            String category = data.getString("category");
            if (category != null) {
                mat.category = CivColor.colorize(category);
                mat.categoryCivColortripped = CivColor.stripTags(category);

                if (mat.category.toLowerCase().contains("tier 1")) {
                    mat.tier = 1;
                } else if (mat.category.toLowerCase().contains("tier 2")) {
                    mat.tier = 2;
                } else if (mat.category.toLowerCase().contains("tier 3")) {
                    mat.tier = 3;
                } else if (mat.category.toLowerCase().contains("tier 4")) {
                    mat.tier = 4;
                } else {
                    mat.tier = 0;
                }

            }

            mat.craftable = data.getBoolean("craftable");
            mat.shaped = data.getBoolean("shaped");
            mat.shiny = data.getBoolean("shiny");
            mat.tradeable = data.getBoolean("tradeable");
            mat.tradeValue = data.getDouble("tradeValue", 0);
            mat.vanilla = data.getBoolean("vanilla");
            mat.amount = data.getInt("amount", 1);
            mat.required_tech = data.getString("required_techs");


            for (Map<?, ?> compObj : data.getMapList("components")) {

                HashMap<String, String> compMap = new HashMap<>();
                for (Object key : compObj.keySet()) {
                    compMap.put((String) key, (String) compObj.get(key));
                }
                mat.components.add(compMap);
            }


            List<Map<?, ?>> configIngredients = data.getMapList("ingredients");
            if (!configIngredients.isEmpty()) {
                mat.ingredients = new HashMap<>();

                for (Map<?, ?> ingred : configIngredients) {
                    LoreCraftableMaterial.ConfigIngredient ingredient = new LoreCraftableMaterial.ConfigIngredient(
                            Material.getMaterial((Integer) ingred.get("type_id")),
                            Optional.ofNullable((Integer) ingred.get("data")).orElse(0),
                            (String) ingred.get("custom_id"),
                            Optional.ofNullable((Integer) ingred.get("count")).orElse(1),
                            (String) ingred.get("letter"),
                            Optional.ofNullable((Boolean) ingred.get("ignore_data")).orElse(false)
                    );
                    String key = Objects.requireNonNullElseGet(ingredient.custom_id(), () -> "mc_" + ingredient.type_id());

                    mat.ingredients.put(key, ingredient);
                    //ConfigIngredient.ingredientMap.put(ingredient.custom_id, ingredient);
                }
            }

            if (mat.shaped) {
                /* Optional shape argument. */

                String[] shape = new String[data.getList("shape", new ArrayList<>()).size()];

                int i = 0;
                for (Object obj : data.getList("shape", new ArrayList<>())) {
                    if (obj instanceof String) {
                        shape[i] = (String) obj;
                        i++;
                    }
                }
                mat.shape = shape;
            }


            /* Add to category map. */
            ConfigMaterialCategory.addMaterial(mat);
            materials.put(mat.id, mat);
        }

        CivLog.info("Loaded " + materials.size() + " Materials.");
    }

    public boolean playerHasTechnology(Player player) {
        if (this.required_tech == null) {
            return true;
        }

        Resident resident = CivGlobal.getResident(player);
        if (resident == null || !resident.hasTown()) {
            return false;
        }

        /* Parse technoloies */
        String[] split = this.required_tech.split(",");
        for (String tech : split) {
            tech = tech.replace(" ", "");
            if (!resident.getCiv().hasTechnology(tech)) {
                return false;
            }
        }

        return true;
    }

    public String getRequireString() {
        StringBuilder out = new StringBuilder();
        if (this.required_tech == null) {
            return out.toString();
        }

        /* Parse technoloies */
        String[] split = this.required_tech.split(",");
        for (String tech : split) {
            tech = tech.replace(" ", "");
            ConfigTech technology = CivSettings.techs.get(tech);
            if (technology != null) {
                out.append(technology.name).append(", ");
            }
        }

        return out.toString();
    }

}
