/*
 * AVRGAMING LLC
 * __________________
 *
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,TICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.lorestorage;

import com.avrgaming.civcraft.loregui.*;
import gpl.AttributeUtil;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class LoreGuiItem {

    public static final int MAX_INV_SIZE = 54;
    public static final int INV_ROW_COUNT = 9;

    public static ItemStack getGUIItem(String title, String[] messages, Material type, int data) {
        ItemStack stack = new ItemStack(type, 1, (short) data);
        AttributeUtil attrs = new AttributeUtil(stack);
        attrs.setCivCraftProperty("GUI", title);
        attrs.setName(title);
        attrs.setLore(messages);
        return attrs.getStack();
    }

    public static boolean isGUIItem(ItemStack stack) {
        AttributeUtil attrs = new AttributeUtil(stack);
        String title = attrs.getCivCraftProperty("GUI");
        return title != null;
    }

    public static ItemStack setAction(ItemStack stack, String action) {
        AttributeUtil attrs = new AttributeUtil(stack);
        attrs.setCivCraftProperty("GUI_ACTION", action);
        return attrs.getStack();
    }

    public static String getAction(ItemStack stack) {
        return new AttributeUtil(stack).getCivCraftProperty("GUI_ACTION");
    }

    public static ItemStack setActionData(ItemStack stack, String key, String value) {
        AttributeUtil attrs = new AttributeUtil(stack);
        attrs.setCivCraftProperty("GUI_ACTION_DATA:" + key, value);
        return attrs.getStack();
    }

    public static String getActionData(ItemStack stack, String key) {
        return new AttributeUtil(stack).getCivCraftProperty("GUI_ACTION_DATA:" + key);
    }

    public static ItemStack build(String title, Material type, int data, String... messages) {
        return getGUIItem(title, messages, type, data);
    }

    public static ItemStack asGuiItem(ItemStack stack) {
        AttributeUtil attrs = new AttributeUtil(stack);
        attrs.setCivCraftProperty("GUI", String.valueOf(stack.getType()));
        return attrs.getStack();
    }

    public static final HashMap<String, Class<? extends GuiAction>> gui_classes = new HashMap<>();

    static {
        gui_classes.put("BuildChooseTemplate", BuildChooseTemplate.class);
        gui_classes.put("BuildStructureList", BuildStructureList.class);
        gui_classes.put("BuildWithDefaultPersonalTemplate", BuildWithDefaultPersonalTemplate.class);
        gui_classes.put("BuildWithPersonalTemplate", BuildWithPersonalTemplate.class);
        gui_classes.put("BuildWithTemplate", BuildWithTemplate.class);
        gui_classes.put("OpenInventory", OpenInventory.class);
        gui_classes.put("ShowPerkPage", ShowPerkPage.class);
        gui_classes.put("ShowRecipe", ShowRecipe.class);
        gui_classes.put("ShowTemplateType", ShowTemplateType.class);
        gui_classes.put("SpawnItem", SpawnItem.class);
    }

    public static void processAction(String action, ItemStack stack, InventoryClickEvent event) {

        /* Get class name from reflection and perform assigned action */
        try {
            gui_classes.get(action).newInstance().performAction(event, stack);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
