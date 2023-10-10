package com.avrgaming.civcraft.items.components;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivLog;
import gpl.AttributeUtil;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class Catalyst extends ItemComponent {

    @Override
    public void onPrepareCreate(AttributeUtil attrUtil) {
        attrUtil.addLore(ChatColor.RESET + String.valueOf(ChatColor.GOLD) + CivSettings.localize.localizedString("itemLore_Catalyst"));
    }

    public ItemStack getEnchantedItem(ItemStack stack) {

        LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
        if (craftMat == null) {
            return null;
        }

        String[] materials = this.getString("allowed_materials").split(",");
        boolean found = false;
        for (String mat : materials) {
            mat = mat.replaceAll(" ", "");
            if (mat.equals(LoreMaterial.getMID(stack))) {
                found = true;
                break;
            }
        }

        if (!found) {
            return null;
        }

        String enhStr = this.getString("enhancement");

        LoreEnhancement enhance = LoreEnhancement.enhancements.get(enhStr);
        if (enhance == null) {
            CivLog.error("Couldn't find enhancement titled:" + enhStr);
            return null;
        }

        if (enhance.canEnchantItem(stack)) {
            AttributeUtil attrs = new AttributeUtil(stack);
            enhance.variables.put("amount", getString("amount"));
            attrs = enhance.add(attrs);
            return attrs.getStack();
        }

        return null;
    }

    public int getEnhancedLevel(ItemStack stack) {
        String enhStr = this.getString("enhancement");

        LoreEnhancement enhance = LoreEnhancement.enhancements.get(enhStr);
        if (enhance == null) {
            CivLog.error("Couldn't find enhancement titled:" + enhStr);
            return 0;
        }

        return (int) enhance.getLevel(new AttributeUtil(stack));
    }

    public boolean enchantSuccess(ItemStack stack) {
        int free_catalyst_amount = CivSettings.civConfig.getInt("global.free_catalyst_amount", 3);
        int extra_catalyst_amount = CivSettings.civConfig.getInt("global.extra_catalyst_amount", 3);
        double extra_catalyst_percent = CivSettings.civConfig.getDouble("global.extra_catalyst_percent", 0.0);

        int level = getEnhancedLevel(stack);

        if (level <= free_catalyst_amount) {
            return true;
        }

        int chance = Integer.parseInt(getString("chance"));
        Random rand = new Random();
        int extra = 0;
        int n = rand.nextInt(100);

        if (level <= extra_catalyst_amount) {
            n -= (int) (extra_catalyst_percent * 100);
        }

        n = n + extra;

        return n <= chance;
    }


}
