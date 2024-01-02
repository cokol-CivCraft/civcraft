package com.avrgaming.civcraft.provider;

import com.avrgaming.civcraft.config.ConfigRecipe;
import com.avrgaming.civcraft.config.ConfigRecipeShapless;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivData;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import java.util.HashMap;
import java.util.Map;

public class DefaultRecipeProvider {
    public static Map<NamespacedKey, ConfigRecipe> provide() {
        Map<NamespacedKey, ConfigRecipe> data = new HashMap<>();
        data.put(
                new NamespacedKey(CivCraft.getPlugin(), "mat_refined_stone_granite"),
                new ConfigRecipeShapless(
                        LoreCraftableMaterial.materials.get("mat_refined_stone"),
                        new ItemStack[]{
                                new MaterialData(Material.STONE, (byte) CivData.GRANITE).toItemStack(9)
                        }
                ));
        data.put(
                new NamespacedKey(CivCraft.getPlugin(), "mat_refined_stone_diorite"),
                new ConfigRecipeShapless(
                        LoreCraftableMaterial.materials.get("mat_refined_stone"),
                        new ItemStack[]{
                                new MaterialData(Material.STONE, (byte) CivData.DIORITE).toItemStack(9)
                        }
                ));
        data.put(
                new NamespacedKey(CivCraft.getPlugin(), "mat_refined_stone_andesite"),
                new ConfigRecipeShapless(
                        LoreCraftableMaterial.materials.get("mat_refined_stone"),
                        new ItemStack[]{
                                new MaterialData(Material.STONE, (byte) CivData.ANDESITE).toItemStack(9)
                        }
                ));
        data.put(
                new NamespacedKey(CivCraft.getPlugin(), "mat_refined_stone_cobblestone"),
                new ConfigRecipeShapless(
                        LoreCraftableMaterial.materials.get("mat_refined_stone"),
                        new ItemStack[]{
                                new MaterialData(Material.COBBLESTONE).toItemStack(9)
                        }
                ));
        data.put(
                new NamespacedKey(CivCraft.getPlugin(), "mat_carved_leather"),
                new ConfigRecipeShapless(
                        LoreCraftableMaterial.materials.get("mat_carved_leather"),
                        new ItemStack[]{
                                new MaterialData(Material.LEATHER).toItemStack(9)
                        }
                ));
        data.put(
                new NamespacedKey(CivCraft.getPlugin(), "mat_refined_feathers"),
                new ConfigRecipeShapless(
                        LoreCraftableMaterial.materials.get("mat_refined_feathers"),
                        new ItemStack[]{
                                new MaterialData(Material.FEATHER).toItemStack(9)
                        }
                ));
        data.put(
                new NamespacedKey(CivCraft.getPlugin(), "mat_crafted_string"),
                new ConfigRecipeShapless(
                        LoreCraftableMaterial.materials.get("mat_crafted_string"),
                        new ItemStack[]{
                                new MaterialData(Material.STRING).toItemStack(9)
                        }
                ));
        data.put(
                new NamespacedKey(CivCraft.getPlugin(), "mat_refined_feathers"),
                new ConfigRecipeShapless(
                        LoreCraftableMaterial.materials.get("mat_refined_feathers"),
                        new ItemStack[]{
                                new MaterialData(Material.FEATHER).toItemStack(9)
                        }
                ));
        data.put(
                new NamespacedKey(CivCraft.getPlugin(), "mat_crafted_reeds"),
                new ConfigRecipeShapless(
                        LoreCraftableMaterial.materials.get("mat_crafted_reeds"),
                        new ItemStack[]{
                                new MaterialData(Material.SUGAR_CANE).toItemStack(9)
                        }
                ));
        data.put(
                new NamespacedKey(CivCraft.getPlugin(), "mat_crafted_sticks"),
                new ConfigRecipeShapless(
                        LoreCraftableMaterial.materials.get("mat_crafted_sticks"),
                        new ItemStack[]{
                                new MaterialData(Material.STICK).toItemStack(9)
                        }
                ));
        data.put(
                new NamespacedKey(CivCraft.getPlugin(), "mat_refined_sulphur"),
                new ConfigRecipeShapless(
                        LoreCraftableMaterial.materials.get("mat_refined_sulphur"),
                        new ItemStack[]{
                                new MaterialData(Material.SULPHUR).toItemStack(9)
                        }
                ));
        data.put(
                new NamespacedKey(CivCraft.getPlugin(), "mat_compacted_sand"),
                new ConfigRecipeShapless(
                        LoreCraftableMaterial.materials.get("mat_compacted_sand"),
                        new ItemStack[]{
                                new MaterialData(Material.SAND, (byte) -1).toItemStack(9)
                        }
                ));
        return data;
    }
}
