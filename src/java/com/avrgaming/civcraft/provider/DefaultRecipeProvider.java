package com.avrgaming.civcraft.provider;

import com.avrgaming.civcraft.config.ConfigMaterial;
import com.avrgaming.civcraft.config.ConfigRecipe;
import com.avrgaming.civcraft.config.ConfigRecipeShaped;
import com.avrgaming.civcraft.config.ConfigRecipeShapless;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivData;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.avrgaming.civcraft.provider.DefaultMaterialProvider.*;

@ParametersAreNonnullByDefault
public class DefaultRecipeProvider {
    public final Map<NamespacedKey, ConfigRecipe> entries = new HashMap<>();

    public DefaultRecipeProvider() {
        shapeless(REFINED_STONE, "granite")
                .ingridient(new MaterialData(Material.STONE, (byte) CivData.GRANITE).toItemStack(9))
                .end();
        shapeless(REFINED_STONE, "diorite")
                .ingridient(new MaterialData(Material.STONE, (byte) CivData.DIORITE).toItemStack(9))
                .end();
        shapeless(REFINED_STONE, "andesite")
                .ingridient(new MaterialData(Material.STONE, (byte) CivData.ANDESITE).toItemStack(9))
                .end();
        shapeless(REFINED_STONE, "cobblestone")
                .ingridient(new MaterialData(Material.COBBLESTONE).toItemStack(9))
                .end();
        shapeless(CARVED_LEATHER).ingridient(new MaterialData(Material.LEATHER).toItemStack(9)).end();
        shapeless(REFINED_FEATHERS).ingridient(new MaterialData(Material.FEATHER).toItemStack(9)).end();
        shapeless(FORGED_CLAY).ingridient(new MaterialData(Material.CLAY_BALL).toItemStack(9)).end();
        shapeless(CRAFTED_STRING).ingridient(new MaterialData(Material.STRING).toItemStack(9)).end();
        shapeless(CRAFTED_STICK).ingridient(new MaterialData(Material.STICK).toItemStack(9)).end();
        shapeless(REFINED_SULPHUR).ingridient(new MaterialData(Material.SULPHUR).toItemStack(9)).end();
        shapeless(COMPACTED_SAND).ingridient(new MaterialData(Material.SAND, (byte) -1).toItemStack(9)).end();
        shapeless(CRAFTED_REEDS).ingridient(new MaterialData(Material.SUGAR_CANE).toItemStack(9)).end();


    }

    public static Map<NamespacedKey, ConfigRecipe> provide() {
        return new HashMap<>(new DefaultRecipeProvider().entries);
    }

    private BuilderShapeless shapeless(ConfigMaterial material) {
        return shapeless(new NamespacedKey(CivCraft.getPlugin(), material.id), material);
    }

    private BuilderShapeless shapeless(ConfigMaterial material, String alt) {
        return shapeless(new NamespacedKey(CivCraft.getPlugin(), material.id + "_" + alt), material);
    }

    private BuilderShapeless shapeless(NamespacedKey key, ConfigMaterial material) {
        return new BuilderShapeless(key, material);
    }

    private class BuilderShapeless {
        private final NamespacedKey key;
        private final ConfigMaterial material;
        private final List<ItemStack> ingridents = new ArrayList<>();

        public BuilderShapeless(NamespacedKey key, ConfigMaterial material) {
            this.key = key;
            this.material = material;
        }

        public BuilderShapeless ingridient(ItemStack stack) {
            ingridents.add(stack);
            return this;
        }

        public ConfigRecipeShapless end() {
            var conf = new ConfigRecipeShapless(this.material, ingridents.toArray(new ItemStack[0]));
            entries.put(key, conf);
            return conf;
        }
    }

    private BuilderShaped shaped(ConfigMaterial material, String[] rows) {
        return shaped(new NamespacedKey(CivCraft.getPlugin(), material.id), material, rows);
    }

    private BuilderShaped shaped(ConfigMaterial material, String alt, String[] rows) {
        return shaped(new NamespacedKey(CivCraft.getPlugin(), material.id + "_" + alt), material, rows);
    }

    private BuilderShaped shaped(NamespacedKey key, ConfigMaterial material, String[] rows) {
        return new BuilderShaped(key, material, rows);
    }

    private class BuilderShaped {
        private final NamespacedKey key;
        private final ConfigMaterial material;
        private final String[] rows;
        private final Map<Character, ItemStack> ingredients = new HashMap<>();

        public BuilderShaped(NamespacedKey key, ConfigMaterial material, String[] rows) {
            this.key = key;
            this.material = material;
            this.rows = rows;
        }

        public BuilderShaped ingridient(Character key, ItemStack stack) {
            this.ingredients.put(key, stack);
            return this;
        }

        public ConfigRecipeShaped end() {
            var conf = new ConfigRecipeShaped(this.material, rows, ingredients);
            entries.put(key, conf);
            return conf;
        }
    }
}
