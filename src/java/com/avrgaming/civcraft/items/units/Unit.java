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
package com.avrgaming.civcraft.items.units;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMission;
import com.avrgaming.civcraft.config.ConfigUnit;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public abstract class Unit {

    public static Spy SPY_UNIT;
    public static Settler SETTLER_UNIT;
    public static ArrayList<MissionBook> SPY_MISSIONS = new ArrayList<>();

    public static void init() {

        SPY_UNIT = new Spy("u_spy", CivSettings.units.get("u_spy"));

        for (ConfigMission mission : CivSettings.missions.values()) {
            if (mission.slot > 0) {
                MissionBook book = new MissionBook(mission.id, Material.ENCHANTED_BOOK, (short) 0);
                book.setName(mission.name);
                book.setupLore(book.getId());
                book.setParent(SPY_UNIT);
                book.setSocketSlot(mission.slot);
                SPY_UNIT.addMissionBook(book);
                SPY_MISSIONS.add(book);
            }
        }

        SETTLER_UNIT = new Settler("u_settler", CivSettings.units.get("u_settler"));
    }


    protected static boolean addItemNoStack(Inventory inv, ItemStack stack) {

        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] == null) {
                contents[i] = stack;
                inv.setContents(contents);
                return true;
            }
        }

        return false;
    }

    public static ConfigUnit getPlayerUnit(Player player) {

        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null) {
                continue;
            }

            LoreMaterial material = LoreMaterial.getMaterial(stack);
            if ((material instanceof UnitMaterial)) {

                if (!UnitMaterial.validateUnitUse(player, stack)) {
                    return null;
                }


                return ((UnitMaterial) material).getUnit();
            }
        }

        return null;
    }

    public static void removeUnit(Player player) {

        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null) {
                LoreMaterial material = LoreMaterial.getMaterial(stack);
                if (material != null) {
                    if (material instanceof UnitMaterial) {
                        player.getInventory().remove(stack);
                        continue;
                    }

                    if (material instanceof UnitItemMaterial) {
                        player.getInventory().remove(stack);
                    }

                }
            }
        }
    }

    public static boolean isWearingFullLeather(Player player) {

        try {
            ItemStack stack3 = player.getEquipment().getBoots();
            if (stack3.getType() != Material.LEATHER_BOOTS) {
                return false;
            }

            ItemStack stack2 = player.getEquipment().getChestplate();
            if (stack2.getType() != Material.LEATHER_CHESTPLATE) {
                return false;
            }

            ItemStack stack1 = player.getEquipment().getHelmet();
            if (stack1.getType() != Material.LEATHER_HELMET) {
                return false;
            }

            ItemStack stack = player.getEquipment().getLeggings();
            if (stack.getType() != Material.LEATHER_LEGGINGS) {
                return false;
            }

        } catch (NullPointerException e) {
            return false;
        }
        return true;
    }

    public static boolean isWearingFullComposite(Player player) {

        for (ItemStack stack : player.getInventory().getArmorContents()) {

            LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
            if (craftMat == null) {
                return false;
            }

            if ((!craftMat.getConfigId().contains("mat_composite_leather"))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isWearingFullHardened(Player player) {

        for (ItemStack stack : player.getInventory().getArmorContents()) {

            LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
            if (craftMat == null) {
                return false;
            }

            if ((!craftMat.getConfigId().contains("mat_hardened_leather"))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isWearingFullRefined(Player player) {

        for (ItemStack stack : player.getInventory().getArmorContents()) {

            LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
            if (craftMat == null) {
                return false;
            }

            if ((!craftMat.getConfigId().contains("mat_refined_leather"))) {
                return false;
            }

        }
        return true;
    }

    public static boolean isWearingFullBasicLeather(Player player) {

        for (ItemStack stack : player.getInventory().getArmorContents()) {

            LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
            if (craftMat == null) {
                return false;
            }

            if ((!craftMat.getConfigId().contains("mat_leather_"))) {
                return false;
            }


        }
        return true;
    }

    public static boolean isWearingAnyMetal(Player player) {
        return isWearingAnyChain(player) || isWearingAnyGold(player) || isWearingAnyIron(player) || isWearingAnyDiamond(player);
    }

    public static boolean isWearingAnyChain(Player player) {

        if (player.getEquipment().getBoots() != null) {
            if (player.getEquipment().getBoots().getType().equals(Material.CHAINMAIL_BOOTS)) {
                return true;
            }
        }

        if (player.getEquipment().getChestplate() != null) {
            if (player.getEquipment().getChestplate().getType().equals(Material.CHAINMAIL_CHESTPLATE)) {
                return true;
            }
        }

        if (player.getEquipment().getHelmet() != null) {
            if (player.getEquipment().getHelmet().getType().equals(Material.CHAINMAIL_HELMET)) {
                return true;
            }
        }

        if (player.getEquipment().getLeggings() != null) {
            return player.getEquipment().getLeggings().getType().equals(Material.CHAINMAIL_LEGGINGS);
        }

        return false;
    }


    public static boolean isWearingAnyGold(Player player) {

        if (player.getEquipment().getBoots() != null) {
            if (player.getEquipment().getBoots().getType().equals(Material.GOLD_BOOTS)) {
                return true;
            }
        }

        if (player.getEquipment().getChestplate() != null) {
            if (player.getEquipment().getChestplate().getType().equals(Material.GOLD_CHESTPLATE)) {
                return true;
            }
        }

        if (player.getEquipment().getHelmet() != null) {
            if (player.getEquipment().getHelmet().getType().equals(Material.GOLD_HELMET)) {
                return true;
            }
        }

        if (player.getEquipment().getLeggings() != null) {
            return player.getEquipment().getLeggings().getType().equals(Material.GOLD_LEGGINGS);
        }

        return false;
    }


    public static boolean isWearingAnyIron(Player player) {

        if (player.getEquipment().getBoots() != null) {
            if (player.getEquipment().getBoots().getType() == Material.IRON_BOOTS) {
                return true;
            }
        }

        if (player.getEquipment().getChestplate() != null) {
            if (player.getEquipment().getChestplate().getType() == Material.IRON_CHESTPLATE) {
                return true;
            }
        }

        if (player.getEquipment().getHelmet() != null) {
            if (player.getEquipment().getHelmet().getType() == Material.IRON_HELMET) {
                return true;
            }
        }

        if (player.getEquipment().getLeggings() != null) {
            return player.getEquipment().getLeggings().getType() == Material.IRON_LEGGINGS;
        }

        return false;
    }

    public static boolean isWearingAnyDiamond(Player player) {

        if (player.getEquipment().getBoots() != null) {
            if (player.getEquipment().getBoots().getType() == Material.DIAMOND_BOOTS) {
                return true;
            }
        }

        if (player.getEquipment().getChestplate() != null) {
            if (player.getEquipment().getChestplate().getType() == Material.DIAMOND_CHESTPLATE) {
                return true;
            }
        }

        if (player.getEquipment().getHelmet() != null) {
            if (player.getEquipment().getHelmet().getType() == Material.DIAMOND_HELMET) {
                return true;
            }
        }

        if (player.getEquipment().getLeggings() != null) {
            return player.getEquipment().getLeggings().getType() == Material.DIAMOND_LEGGINGS;
        }

        return false;
    }
}
