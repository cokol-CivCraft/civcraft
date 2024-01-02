package com.avrgaming.civcraft.provider;

import com.avrgaming.civcraft.config.ConfigMaterial;
import com.avrgaming.civcraft.config.ConfigRecipe;
import com.avrgaming.civcraft.config.ConfigRecipeShapless;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
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

import static com.avrgaming.civcraft.provider.DefaultMaterialProvider.CARVED_LEATHER;
import static com.avrgaming.civcraft.provider.DefaultMaterialProvider.REFINED_STONE;

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
        shapeless(LoreCraftableMaterial.materials.get("mat_refined_feathers").getConfigMaterial()).ingridient(new MaterialData(Material.FEATHER).toItemStack(9)).end();
    }

    public static Map<NamespacedKey, ConfigRecipe> provide() {
        Map<NamespacedKey, ConfigRecipe> data = new HashMap<>(new DefaultRecipeProvider().entries);
        data.put(
                new NamespacedKey(CivCraft.getPlugin(), "mat_crafted_string"),
                new ConfigRecipeShapless(
                        LoreCraftableMaterial.materials.get("mat_crafted_string").getConfigMaterial(),
                        new ItemStack[]{
                                new MaterialData(Material.STRING).toItemStack(9)
                        }
                ));
        data.put(
                new NamespacedKey(CivCraft.getPlugin(), "mat_crafted_reeds"),
                new ConfigRecipeShapless(
                        LoreCraftableMaterial.materials.get("mat_crafted_reeds").getConfigMaterial(),
                        new ItemStack[]{
                                new MaterialData(Material.SUGAR_CANE).toItemStack(9)
                        }
                ));
        data.put(
                new NamespacedKey(CivCraft.getPlugin(), "mat_crafted_sticks"),
                new ConfigRecipeShapless(
                        LoreCraftableMaterial.materials.get("mat_crafted_sticks").getConfigMaterial(),
                        new ItemStack[]{
                                new MaterialData(Material.STICK).toItemStack(9)
                        }
                ));
        data.put(
                new NamespacedKey(CivCraft.getPlugin(), "mat_refined_sulphur"),
                new ConfigRecipeShapless(
                        LoreCraftableMaterial.materials.get("mat_refined_sulphur").getConfigMaterial(),
                        new ItemStack[]{
                                new MaterialData(Material.SULPHUR).toItemStack(9)
                        }
                ));
        data.put(
                new NamespacedKey(CivCraft.getPlugin(), "mat_compacted_sand"),
                new ConfigRecipeShapless(
                        LoreCraftableMaterial.materials.get("mat_compacted_sand").getConfigMaterial(),
                        new ItemStack[]{
                                new MaterialData(Material.SAND, (byte) -1).toItemStack(9)
                        }
                ));
        return data;
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
}
