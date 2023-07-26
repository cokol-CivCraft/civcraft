package com.avrgaming.civcraft.loreenhancements;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import gpl.AttributeUtil;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

public class LoreEnhancementDefense extends LoreEnhancement {

    public LoreEnhancementDefense() {
        this.variables.put("amount", "1.0");
    }

    public String getLoreString(double baseLevel) {
        return ChatColor.DARK_AQUA + "+" + baseLevel + " " + CivSettings.localize.localizedString("newItemLore_Defense");
    }

    @Override
    public AttributeUtil add(AttributeUtil attrs) {

        /*
         * Look for any existing attack enhancements.
         * If we have one, add to it instead of making a
         * new one.
         */
        double amount = Double.parseDouble(this.variables.get("amount"));
        double baseLevel = amount;

        if (attrs.hasEnhancement("LoreEnhancementDefense")) {

            /* Get base Level. */
            baseLevel = Double.parseDouble(attrs.getEnhancementData("LoreEnhancementDefense", "level"));

            /* Reset the lore. */
            String[] lore = attrs.getLore();
            for (int i = 0; i < lore.length; i++) {
                if (lore[i].equals(getLoreString(baseLevel))) {
                    lore[i] = getLoreString(baseLevel + amount);
                }
            }
            attrs.setLore(lore);

            /* Reset the item name. */
            String[] split = attrs.getName().split("\\(");
            attrs.setName(split[0] + "(+" + (baseLevel + amount) + ")");

            /* Store the data back in the enhancement. */
            attrs.setEnhancementData("LoreEnhancementDefense", "level", String.valueOf(baseLevel + amount));
        } else {
            attrs.addEnhancement("LoreEnhancementDefense", "level", String.valueOf(baseLevel));
            attrs.addLore(getLoreString(baseLevel));
            attrs.setName(attrs.getName() + ChatColor.AQUA + "(+" + amount + ")");
        }

        return attrs;
    }

    @Override
    public boolean canEnchantItem(ItemStack item) {
        return isArmor(item);
    }

    @Override
    public double getLevel(AttributeUtil attrs) {
        if (attrs.hasEnhancement("LoreEnhancementDefense")) {
            /* Get base Level. */
            return Double.parseDouble(attrs.getEnhancementData("LoreEnhancementDefense", "level"));
        }
        return 1;
    }

    public double getExtraDefense(AttributeUtil attrs) {
        double m;
        try {
            m = CivSettings.getDouble(CivSettings.civConfig, "global.defense_catalyst_multiplier");
            return getLevel(attrs) * m;
        } catch (InvalidConfiguration e) {
            e.printStackTrace();
        }

        return getLevel(attrs);
    }

    @Override
    public String serialize(ItemStack stack) {
        AttributeUtil attrs = new AttributeUtil(stack);
        return attrs.getEnhancementData("LoreEnhancementDefense", "level");
    }

    @Override
    public ItemStack deserialize(ItemStack stack, String data) {
        AttributeUtil attrs = new AttributeUtil(stack);
        attrs.setEnhancementData("LoreEnhancementDefense", "level", data);
        attrs.setName(attrs.getName() + ChatColor.AQUA + "(+" + Double.valueOf(data) + ")");
        return attrs.getStack();
    }
}
