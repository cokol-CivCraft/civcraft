package com.avrgaming.civcraft.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public class ConfigRemovedRecipes {
    public Material type_id;
    public int data;


    public static void removeRecipes(FileConfiguration cfg, HashMap<Material, ConfigRemovedRecipes> removedRecipies) {
        List<Map<?, ?>> configMaterials = cfg.getMapList("removed_recipes");
        for (Map<?, ?> b : configMaterials) {
            ConfigRemovedRecipes item = new ConfigRemovedRecipes();
            item.type_id = Material.valueOf((String) b.get("type_id"));
            item.data = (Integer) b.get("data");

            removedRecipies.put(item.type_id, item);

            ItemStack is = new ItemStack(item.type_id, 1, (short) item.data);
            List<Recipe> backup = new ArrayList<>();
            // Idk why you change scope, but why not
            Iterator<Recipe> a = Bukkit.getServer().recipeIterator();
            while (a.hasNext()) {
                Recipe recipe = a.next();
                ItemStack result = recipe.getResult();
                if (!result.isSimilar(is)) {
                    backup.add(recipe);
                }
            }

            Bukkit.getServer().clearRecipes();
            for (Recipe r : backup) {
                Bukkit.getServer().addRecipe(r);
            }
        }
    }
}