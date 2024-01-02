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
        return data;
    }
}
