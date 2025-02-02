package com.avrgaming.civcraft.lorestorage;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.config.ConfigTechItem;
import com.avrgaming.civcraft.items.components.Tagged;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.TaskMaster;
import gpl.AttributeUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;

public class LoreCraftableMaterialListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void OnCraftItemEvent(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {

            LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(event.getInventory().getResult());
            if (craftMat == null) {

                /* Disable notch apples */
                ItemStack resultStack = event.getInventory().getResult();
                if (resultStack == null) {
                    return;
                }
                if (resultStack.getType().equals(Material.GOLDEN_APPLE)) {
                    CivMessage.sendError(event.getWhoClicked(), CivSettings.localize.localizedString("loreCraft_goldenApples"));
                    event.setCancelled(true);
                    return;
                }

                ConfigTechItem restrictedTechItem = CivSettings.techItems.get(resultStack.getType());
                if (restrictedTechItem != null) {
                    ConfigTech tech = CivSettings.techs.get(restrictedTechItem.require_tech);
                    CivMessage.sendError(player, CivSettings.localize.localizedString("var_loreCraft_missingTech", tech.name()));
                    event.setCancelled(true);
                    return;
                }

                return;
            }

            if (!craftMat.getConfigMaterial().playerHasTechnology(player)) {
                CivMessage.sendError(player, CivSettings.localize.localizedString("var_loreCraft_missingTech", craftMat.getConfigMaterial().getRequireString()));
                event.setCancelled(true);
                return;
            }

            Resident resident = CivGlobal.getResident(player);
            if (craftMat.getId().equals("mat_found_camp")) {
            } else if (craftMat.getId().equals("mat_found_civ")) {
            } else {

                /* if shift clicked, the amount crafted is always min. */
                int amount;
                if (event.isShiftClick()) {
                    amount = 64; //cant craft more than 64.
                    for (ItemStack stack : event.getInventory().getMatrix()) {
                        if (stack == null) {
                            continue;
                        }

                        if (stack.getAmount() < amount) {
                            amount = stack.getAmount();
                        }
                    }
                } else {
                    amount = 1;
                }

                int finalAmount = amount;
                TaskMaster.asyncTask(() -> {
                    String key = resident.getName() + ":platinumCrafted";
                    ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(key);

                    if (entries.isEmpty()) {
                        CivGlobal.getSessionDB().add(key, String.valueOf(finalAmount));

                    } else {
                        int amount2 = Integer.parseInt(entries.get(0).value) + finalAmount;
                        if (amount2 >= 100) {

                            amount2 -= 100;
                        }

                        CivGlobal.getSessionDB().update(entries.get(0).request_id, key, String.valueOf(amount2));
                    }
                }, 0);
            }
        }
    }


    private boolean matrixContainsCustom(ItemStack[] matrix) {
        for (ItemStack stack : matrix) {
            if (LoreMaterial.isCustom(stack)) {
                return true;
            }
        }
        return false;
    }


    @EventHandler(priority = EventPriority.LOW)
    public void OnPrepareItemCraftEvent(PrepareItemCraftEvent event) {

        if (event.getRecipe() instanceof ShapedRecipe) {
            String key = LoreCraftableMaterial.getShapedRecipeKey(event.getInventory().getMatrix());
            LoreCraftableMaterial loreMat = LoreCraftableMaterial.shapedKeys.get(key);

            if (loreMat == null) {
                if (LoreCraftableMaterial.isCustom(event.getRecipe().getResult())) {
                    /* Result is custom, but we have found no custom recipie. Set to blank. */
                    event.getInventory().setResult(new ItemStack(Material.AIR));
                }

                if (matrixContainsCustom(event.getInventory().getMatrix())) {
                    event.getInventory().setResult(new ItemStack(Material.AIR));
                }

                return;
            } else {
                if (!LoreCraftableMaterial.isCustom(event.getRecipe().getResult())) {
                    /* Result is not custom, but recipie is. Set to blank. */
                    if (!loreMat.isVanilla()) {
                        /* A Minecraft 1.12 Fix */
                        if (!LoreEnhancement.isTool(event.getRecipe().getResult())) {
                            event.getInventory().setResult(new ItemStack(Material.AIR));
                            return;
                        }
                        return;
                    }
                }
            }

            String matName = loreMat.getId();
            if (matName.contains("_alt")) {
                String id = matName.replaceAll("_alt(.*)", "");
                loreMat = LoreCraftableMaterial.getCraftMaterialFromId(id);
            }

            ItemStack newStack;
            if (!loreMat.isVanilla()) {
                newStack = LoreMaterial.spawn(loreMat);
                AttributeUtil attrs = new AttributeUtil(newStack);
                loreMat.applyAttributes(attrs);
                newStack.setAmount(loreMat.getCraftAmount());
            } else {
                newStack = new ItemStack(loreMat.getType(), loreMat.getCraftAmount(), (short) 0);
            }

            event.getInventory().setResult(newStack);

        } else if (event.getRecipe() instanceof ShapelessRecipe) {
            String key = LoreCraftableMaterial.getShapelessRecipeKey(event.getInventory().getMatrix());
            LoreCraftableMaterial loreMat = LoreCraftableMaterial.shapelessKeys.get(key);

            if (loreMat == null) {
                if (LoreCraftableMaterial.isCustom(event.getRecipe().getResult())) {
                    /* Result is custom, but we have found no custom recipie. Set to blank. */
                    event.getInventory().setResult(new ItemStack(Material.AIR));
                }

                if (matrixContainsCustom(event.getInventory().getMatrix())) {
                    event.getInventory().setResult(new ItemStack(Material.AIR));
                }

                return;
            } else {
                if (!LoreCraftableMaterial.isCustom(event.getRecipe().getResult())) {
                    /* Result is not custom, but recipie is. Set to blank. */
                    if (!loreMat.isVanilla()) {
                        event.getInventory().setResult(new ItemStack(Material.AIR));
                        return;
                    }
                }
            }

            String matName = loreMat.getId();
            if (matName.contains("_alt")) {
                String id = matName.replaceAll("_alt(.*)", "");
                loreMat = LoreCraftableMaterial.getCraftMaterialFromId(id);
            }

            ItemStack newStack;
            if (!loreMat.isVanilla()) {
                newStack = LoreMaterial.spawn(loreMat);
                AttributeUtil attrs = new AttributeUtil(newStack);
                loreMat.applyAttributes(attrs);
                newStack.setAmount(loreMat.getCraftAmount());
            } else {
                newStack = new ItemStack(loreMat.getType(), loreMat.getCraftAmount(), (short) 0);
            }

            event.getInventory().setResult(newStack);
        }

        ItemStack result = event.getInventory().getResult();
        LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(result);
        if (craftMat != null) {
            if (craftMat.hasComponent("Tagged")) {
                String tag = Tagged.matrixHasSameTag(event.getInventory().getMatrix());
                if (tag == null) {
                    event.getInventory().setResult(new ItemStack(Material.AIR, 1, (short) 0));
                    return;
                }

                Tagged tagged = (Tagged) craftMat.getComponent("Tagged");
                ItemStack stack = tagged.addTag(event.getInventory().getResult(), tag);
                AttributeUtil attrs = new AttributeUtil(stack);
                attrs.addLore(ChatColor.GRAY + tag);
                stack = attrs.getStack();
                event.getInventory().setResult(stack);
            }
        }

    }
}
