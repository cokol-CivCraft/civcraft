/*
 * AVRGAMING LLC
 * __________________
 *
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
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

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoreStoreage {

    public static void setMatID(int id, ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();

        List<String> lore;
        if (meta.hasLore()) {
            lore = meta.getLore();
        } else {
            lore = new ArrayList<>();
        }

        lore.set(0, ChatColor.BLACK + "MID:" + id);
        meta.setLore(lore);
        stack.setItemMeta(meta);
    }

//	public static void addLore(String)

    public static void setItemName(String name, ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        stack.setItemMeta(meta);
    }

    public static void saveLoreMap(String type, Map<String, String> map, ItemStack stack) {

        ItemMeta meta = stack.getItemMeta();
        ArrayList<String> lore = new ArrayList<>();

        lore.add(ChatColor.DARK_AQUA + type);

        for (String key : map.keySet()) {
            String value = map.get(key);
            lore.add(ChatColor.DARK_GRAY + key + ":" + value);
        }

        meta.setLore(lore);
        stack.setItemMeta(meta);
    }

    public static String getType(ItemStack stack) {

        ItemMeta meta = stack.getItemMeta();

        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            return lore.get(0);
        } else {
            return "none";
        }
    }

    public static Map<String, String> getLoreMap(ItemStack stack) {
        HashMap<String, String> map = new HashMap<>();

        ItemMeta meta = stack.getItemMeta();
        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            for (String str : lore) {
                String[] split = str.split(":");
                if (split.length > 2) {
                    map.put(split[0], split[1]);
                }
            }
        }
        return map;
    }


}
