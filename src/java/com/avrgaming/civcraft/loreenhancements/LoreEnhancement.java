package com.avrgaming.civcraft.loreenhancements;

import gpl.AttributeUtil;

import java.util.HashMap;

import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.object.BuildableDamageBlock;

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
        switch (item.getType()) {
		case WOOD_SWORD:
		case STONE_SWORD:
		case IRON_SWORD:
		case GOLD_SWORD:
		case DIAMOND_SWORD:
		case WOOD_AXE:
		case STONE_AXE:
		case IRON_AXE:
		case GOLD_AXE:
		case DIAMOND_AXE:
		case BOW:
			return true;
		default:
			return false;
		}
	}
	
	public static boolean isArmor(ItemStack item) {
        switch (item.getType()) {
		case LEATHER_BOOTS:
		case LEATHER_CHESTPLATE:
		case LEATHER_HELMET:
		case LEATHER_LEGGINGS:
		case IRON_BOOTS:
		case IRON_CHESTPLATE:
		case IRON_HELMET:
		case IRON_LEGGINGS:
		case DIAMOND_BOOTS:
		case DIAMOND_CHESTPLATE:
		case DIAMOND_HELMET:
		case DIAMOND_LEGGINGS:
		case CHAINMAIL_BOOTS:
		case CHAINMAIL_CHESTPLATE:
		case CHAINMAIL_HELMET:
		case CHAINMAIL_LEGGINGS:
		case GOLD_BOOTS:
		case GOLD_CHESTPLATE:
		case GOLD_HELMET:
		case GOLD_LEGGINGS:
			return true;
		default:
			return false;
		}
	}
	
	public static boolean isTool(ItemStack item) {
        switch (item.getType()) {
		case WOOD_SPADE:
		case WOOD_PICKAXE:
		case WOOD_AXE:
		case STONE_SPADE:
		case STONE_PICKAXE:
		case STONE_AXE:
		case IRON_SPADE:
		case IRON_PICKAXE:
		case IRON_AXE:
		case DIAMOND_SPADE:
		case DIAMOND_PICKAXE:
		case DIAMOND_AXE:
		case GOLD_SPADE:
		case GOLD_PICKAXE:
		case GOLD_AXE:
			return true;
		default:
			return false;
		}
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

	public double getLevel(AttributeUtil attrs) {	return 0; }
	public abstract String serialize(ItemStack stack);
	public abstract ItemStack deserialize(ItemStack stack, String data);
	
	
}
