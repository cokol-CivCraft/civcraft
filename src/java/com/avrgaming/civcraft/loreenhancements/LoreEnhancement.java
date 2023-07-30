package com.avrgaming.civcraft.loreenhancements;

import com.avrgaming.civcraft.object.BuildableDamageBlock;
import gpl.AttributeUtil;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public abstract class LoreEnhancement {
    public AttributeUtil add(AttributeUtil attrs) {
        return attrs;
    }

    public static HashMap<String, LoreEnhancement> enhancements = new HashMap<>();
    public HashMap<String, String> variables = new HashMap<>();

    public static void init() {
        enhancements.put("LoreEnhancementSoulBound", new LoreEnhancementSoulBound());
        enhancements.put("LoreEnhancementAttack", new LoreEnhancementAttack());
        enhancements.put("LoreEnhancementDefense", new LoreEnhancementDefense());
        enhancements.put("LoreEnhancementPunchout", new LoreEnhancementPunchout());
        enhancements.put("LoreEnhancementArenaItem", new LoreEnhancementArenaItem());
    }

    public boolean onDeath(PlayerDeathEvent event, ItemStack stack) {
        return false;
    }

    public void onDurabilityChange(PlayerItemDamageEvent event) {
    }

    public boolean canEnchantItem(ItemStack item) {
        return true;
    }

    public static boolean isWeapon(ItemStack item) {
        return switch (item.getType()) {
            case WOOD_SWORD, STONE_SWORD, IRON_SWORD, GOLD_SWORD, DIAMOND_SWORD, WOOD_AXE, STONE_AXE, IRON_AXE, GOLD_AXE, DIAMOND_AXE, BOW ->
                    true;
            default -> false;
        };
    }

    public static boolean isArmor(ItemStack item) {
        return switch (item.getType()) {
            case LEATHER_BOOTS, LEATHER_CHESTPLATE, LEATHER_HELMET, LEATHER_LEGGINGS, IRON_BOOTS, IRON_CHESTPLATE, IRON_HELMET, IRON_LEGGINGS, DIAMOND_BOOTS, DIAMOND_CHESTPLATE, DIAMOND_HELMET, DIAMOND_LEGGINGS, CHAINMAIL_BOOTS, CHAINMAIL_CHESTPLATE, CHAINMAIL_HELMET, CHAINMAIL_LEGGINGS, GOLD_BOOTS, GOLD_CHESTPLATE, GOLD_HELMET, GOLD_LEGGINGS ->
                    true;
            default -> false;
        };
    }

    public static boolean isTool(ItemStack item) {
        return switch (item.getType()) {
            case WOOD_SPADE, WOOD_PICKAXE, WOOD_AXE, STONE_SPADE, STONE_PICKAXE, STONE_AXE, IRON_SPADE, IRON_PICKAXE, IRON_AXE, DIAMOND_SPADE, DIAMOND_PICKAXE, DIAMOND_AXE, GOLD_SPADE, GOLD_PICKAXE, GOLD_AXE ->
                    true;
            default -> false;
        };
    }

    public static boolean isWeaponOrArmor(ItemStack item) {
        return isWeapon(item) || isArmor(item);
    }

    public boolean hasEnchantment(ItemStack item) {
        return false;
    }

    public String getDisplayName() {
        return "LoreEnchant";
    }

    public int onStructureBlockBreak(BuildableDamageBlock dmgBlock, int damage) {
        return damage;
    }

    public double getLevel(AttributeUtil attrs) {
        return 0;
    }

    public abstract String serialize(ItemStack stack);

    public abstract ItemStack deserialize(ItemStack stack, String data);


}
