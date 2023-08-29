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
            case WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD, DIAMOND_SWORD, WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE, DIAMOND_AXE, BOW ->
                    true;
            default -> false;
        };
    }

    public static boolean isArmor(ItemStack item) {
        return switch (item.getType()) {
            case LEATHER_BOOTS, GOLDEN_LEGGINGS, GOLDEN_HELMET, GOLDEN_CHESTPLATE, GOLDEN_BOOTS, CHAINMAIL_LEGGINGS, CHAINMAIL_HELMET, CHAINMAIL_CHESTPLATE, CHAINMAIL_BOOTS, DIAMOND_LEGGINGS, DIAMOND_HELMET, DIAMOND_CHESTPLATE, DIAMOND_BOOTS, IRON_LEGGINGS, IRON_HELMET, IRON_CHESTPLATE, IRON_BOOTS, LEATHER_LEGGINGS, LEATHER_HELMET, LEATHER_CHESTPLATE ->
                    true;
            default -> false;
        };
    }

    public static boolean isTool(ItemStack item) {
        return switch (item.getType()) {
            case WOODEN_SHOVEL, WOODEN_PICKAXE, WOODEN_AXE, STONE_SHOVEL, STONE_PICKAXE, STONE_AXE, IRON_SHOVEL, IRON_PICKAXE, IRON_AXE, DIAMOND_SHOVEL, DIAMOND_PICKAXE, DIAMOND_AXE, GOLDEN_SHOVEL, GOLDEN_PICKAXE, GOLDEN_AXE ->
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
