package com.avrgaming.civcraft.listener.armor;

import org.bukkit.inventory.ItemStack;

/**
 * @author Borlea
 * @Github https://github.com/borlea/
 * @Website http://codingforcookies.com/
 * @since Jul 30, 2015 6:46:16 PM
 */
public enum ArmorType {
    HELMET(5), CHESTPLATE(6), LEGGINGS(7), BOOTS(8);

    private final int slot;

    ArmorType(int slot) {
        this.slot = slot;
    }

    /**
     * Attempts to match the ArmorType for the specified ItemStack.
     *
     * @param itemStack The ItemStack to parse the type of.
     * @return The parsed ArmorType. (null if none were found.)
     */
    public static ArmorType matchType(final ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        return switch (itemStack.getType()) {
            case DIAMOND_HELMET, GOLD_HELMET, IRON_HELMET, CHAINMAIL_HELMET, LEATHER_HELMET -> HELMET;
            case DIAMOND_CHESTPLATE, GOLD_CHESTPLATE, IRON_CHESTPLATE, CHAINMAIL_CHESTPLATE, LEATHER_CHESTPLATE ->
                    CHESTPLATE;
            case DIAMOND_LEGGINGS, GOLD_LEGGINGS, IRON_LEGGINGS, CHAINMAIL_LEGGINGS, LEATHER_LEGGINGS -> LEGGINGS;
            case DIAMOND_BOOTS, GOLD_BOOTS, IRON_BOOTS, CHAINMAIL_BOOTS, LEATHER_BOOTS -> BOOTS;
            default -> null;
        };
    }

    public int getSlot() {
        return slot;
    }
}