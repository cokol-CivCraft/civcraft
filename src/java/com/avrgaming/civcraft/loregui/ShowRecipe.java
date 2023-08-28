package com.avrgaming.civcraft.loregui;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMaterialCategory;
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.lorestorage.LoreGuiItemListener;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;
import gpl.AttributeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public class ShowRecipe extends GuiAction {

    public static final int START_OFFSET = LoreGuiItem.INV_ROW_COUNT + 3;

    public ShowRecipe(GuiActions key) {
        super(key);
    }


    public org.bukkit.inventory.ItemStack getIngredItem(LoreCraftableMaterial.ConfigIngredient ingred, Inventory recInv) {
        String name;
        String message;
        org.bukkit.inventory.ItemStack entryStack;
        if (ingred.custom_id() == null) {
            name = new MaterialData(ingred.type_id(), (byte) ingred.data()).toString();
            message = "Vanilla Item";
            entryStack = LoreGuiItem.build(name, ingred.type_id(), ingred.data(), message);
        } else {
            LoreCraftableMaterial cmat = LoreCraftableMaterial.getCraftMaterialFromId(ingred.custom_id());
            name = cmat.getName();

            entryStack = LoreCraftableMaterial.spawn(cmat);
            entryStack = LoreGuiItem.asGuiItem(entryStack);
            if (cmat.isCraftable()) {
                message = CivSettings.localize.localizedString("loreGui_recipes_clickForRecipe");
                entryStack = LoreGuiItem.setAction(entryStack, GuiActions.ShowRecipe);
                entryStack = LoreGuiItem.setActionData(entryStack, "backInventory", recInv.getName());
            } else {
                message = CivSettings.localize.localizedString("loreGui_recipes_notCraftable");
            }
            AttributeUtil attrs = new AttributeUtil(entryStack);
            attrs.addLore(message);
            entryStack = attrs.getStack();
        }
        return entryStack;
    }

    public void buildCraftTableBorder(Inventory recInv) {
        int offset = 2;

        org.bukkit.inventory.ItemStack stack = LoreGuiItem.build("Craft Table Border", Material.CRAFTING_TABLE, 0, "");

        for (int y = 0; y <= 4; y++) {
            for (int x = 0; x <= 4; x++) {
                if (x == 0 || x == 4 || y == 0 || y == 4) {
                    recInv.setItem(offset + (y * LoreGuiItem.INV_ROW_COUNT) + x, stack);
                }
            }
        }
    }

    public void buildInfoBar(LoreCraftableMaterial craftMat, Inventory recInv, Player player) {
        int offset = 0;
        org.bukkit.inventory.ItemStack stack = null;

        if (craftMat.getConfigMaterial().required_tech != null) {
            Resident resident = CivGlobal.getResident(player);
            ConfigTech tech = CivSettings.techs.get(craftMat.getConfigMaterial().required_tech);
            if (tech != null) {

                if (resident.hasTown() && resident.getCiv().hasTechnology(craftMat.getConfigMaterial().required_tech)) {
                    stack = LoreGuiItem.build(CivSettings.localize.localizedString("loreGui_recipes_requiredTech"), Material.EMERALD_BLOCK, 0, tech.name);
                } else {
                    stack = LoreGuiItem.build(CivSettings.localize.localizedString("loreGui_recipes_requiredTech"), Material.REDSTONE_BLOCK, 0, tech.name);
                }
            }

            if (stack != null) {
                recInv.setItem(offset, stack);
            }
        }

        if (craftMat.isShaped()) {
            stack = LoreGuiItem.build(CivSettings.localize.localizedString("loreGui_recipes_shaped"), Material.HOPPER, 0, "");
        } else {
            stack = LoreGuiItem.build(CivSettings.localize.localizedString("loreGui_recipes_unshaped"), Material.COAL, 0, "");
        }
        offset += LoreGuiItem.INV_ROW_COUNT;
        recInv.setItem(offset, stack);


    }

    @Override
    public void performAction(InventoryClickEvent event, ItemStack stack) {
        Player player = (Player) event.getWhoClicked();

        LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
        if (craftMat == null || craftMat.getConfigMaterial().ingredients == null) {
            /* Do nothing for now. */
            return;
        }

        String title = craftMat.getName() + " " + CivSettings.localize.localizedString("loreGui_recipes_guiHeading");
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }

        Inventory recInv = Bukkit.getServer().createInventory(player, LoreGuiItem.MAX_INV_SIZE, title);
        if (craftMat.isShaped()) {
            int offset = START_OFFSET;
            for (String line : craftMat.getConfigMaterial().shape) {
                for (int i = 0; i < line.toCharArray().length; i++) {
                    LoreCraftableMaterial.ConfigIngredient ingred = null;
                    for (LoreCraftableMaterial.ConfigIngredient in : craftMat.getConfigMaterial().ingredients.values()) {
                        if (in.letter().equalsIgnoreCase(String.valueOf(line.toCharArray()[i]))) {
                            ingred = in;
                            break;
                        }
                    }

                    if (ingred != null) {
                        recInv.setItem(i + offset, getIngredItem(ingred, recInv));
                    }
                }
                offset += LoreGuiItem.INV_ROW_COUNT;
            }
        } else {
            int x = 0;
            int offset = START_OFFSET;
            for (LoreCraftableMaterial.ConfigIngredient ingred : craftMat.getConfigMaterial().ingredients.values()) {
                if (ingred != null) {
                    for (int i = 0; i < ingred.count(); i++) {
                        recInv.setItem(x + offset, getIngredItem(ingred, recInv));

                        x++;
                        if (x >= 3) {
                            x = 0;
                            offset += LoreGuiItem.INV_ROW_COUNT;
                        }
                    }
                }
            }
        }

        String backInventory = LoreGuiItem.getActionData(stack, "backInventory");
        if (backInventory != null) {
            Inventory inv = LoreGuiItemListener.guiInventories.get(backInventory);
            org.bukkit.inventory.ItemStack backButton = LoreGuiItem.build(CivSettings.localize.localizedString("loreGui_recipes_back"), Material.MAP, 0, CivSettings.localize.localizedString("loreGui_recipes_back"));
            backButton = LoreGuiItem.setAction(backButton, GuiActions.OpenInventory);
            backButton = LoreGuiItem.setActionData(backButton, "invType", "showGuiInv");
            backButton = LoreGuiItem.setActionData(backButton, "invName", inv.getName());
            recInv.setItem(LoreGuiItem.MAX_INV_SIZE - 1, backButton);
        } else {
            ConfigMaterialCategory cat = ConfigMaterialCategory.getCategory(craftMat.getConfigMaterial().categoryCivColortripped);
            if (cat != null) {
                org.bukkit.inventory.ItemStack backButton = LoreGuiItem.build(CivSettings.localize.localizedString("loreGui_recipes_back"), Material.MAP, 0, CivSettings.localize.localizedString("loreGui_recipes_backMsg") + " " + cat.name);
                backButton = LoreGuiItem.setAction(backButton, GuiActions.OpenInventory);
                backButton = LoreGuiItem.setActionData(backButton, "invType", "showGuiInv");
                backButton = LoreGuiItem.setActionData(backButton, "invName", cat.name + " Recipes");
                recInv.setItem(LoreGuiItem.MAX_INV_SIZE - 1, backButton);
            }
        }

        LoreGuiItemListener.guiInventories.put(title, recInv);
        buildCraftTableBorder(recInv);
        buildInfoBar(craftMat, recInv, player);
        player.openInventory(recInv);
    }

}
