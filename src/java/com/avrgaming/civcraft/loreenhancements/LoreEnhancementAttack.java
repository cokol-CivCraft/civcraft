package com.avrgaming.civcraft.loreenhancements;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.main.CivLog;
import gpl.AttributeUtil;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

public class LoreEnhancementAttack extends LoreEnhancement {

    public LoreEnhancementAttack() {
        this.variables.put("amount", "1.0");
    }

    public String getLoreString(double baseLevel) {
        double m = CivSettings.civConfig.getDouble("global.attack_catalyst_multiplier", 1.0);
        return ChatColor.DARK_AQUA + "+" + (baseLevel * m) + " " + CivSettings.localize.localizedString("itemLore_Attack");
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
        if (attrs.hasEnhancement("LoreEnhancementAttack")) {

            /* Get base Level. */
            baseLevel = Double.parseDouble(attrs.getEnhancementData("LoreEnhancementAttack", "level"));

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
            attrs.setEnhancementData("LoreEnhancementAttack", "level", String.valueOf(baseLevel + amount));
        } else {
            attrs.addEnhancement("LoreEnhancementAttack", "level", String.valueOf(baseLevel));
            attrs.addLore(getLoreString(baseLevel));
            attrs.setName(attrs.getName() + ChatColor.AQUA + "(+" + amount + ")");
        }

        LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterialFromId(attrs.getCivCraftProperty("mid"));
        if (craftMat == null) {
            CivLog.warning("Couldn't find craft mat with MID of:" + attrs.getCivCraftProperty("mid"));
            return attrs;
        }

        return attrs;
    }

    @Override
    public double getLevel(AttributeUtil attrs) {
        if (attrs.hasEnhancement("LoreEnhancementAttack")) {
            /* Get base Level. */
            return Double.parseDouble(attrs.getEnhancementData("LoreEnhancementAttack", "level"));
        }
        return 0;
    }


    @Override
    public boolean canEnchantItem(ItemStack item) {
        return isWeapon(item);
    }

    public double getExtraAttack(AttributeUtil attrs) {
        return getLevel(attrs) * CivSettings.civConfig.getDouble("global.attack_catalyst_multiplier", 1.0);

    }

    @Override
    public String serialize(ItemStack stack) {
        AttributeUtil attrs = new AttributeUtil(stack);
        return attrs.getEnhancementData("LoreEnhancementAttack", "level");
    }

    @Override
    public ItemStack deserialize(ItemStack stack, String data) {
        AttributeUtil attrs = new AttributeUtil(stack);
        attrs.setEnhancementData("LoreEnhancementAttack", "level", data);
        attrs.setName(attrs.getName() + ChatColor.AQUA + "(+" + Double.valueOf(data) + ")");
        return attrs.getStack();
    }
}
