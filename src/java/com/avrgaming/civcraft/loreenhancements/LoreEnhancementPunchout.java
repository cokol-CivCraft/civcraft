package com.avrgaming.civcraft.loreenhancements;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.object.BuildableDamageBlock;
import gpl.AttributeUtil;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class LoreEnhancementPunchout extends LoreEnhancement {
    private int a = 50;
    private int b = 5;

    public String getDisplayName() {
        return CivSettings.localize.localizedString("itemLore_Punchout");
    }

    public AttributeUtil add(AttributeUtil attrs) {
        attrs.addEnhancement("LoreEnhancementPunchout", null, null);
        attrs.addLore(ChatColor.GOLD + getDisplayName());
        return attrs;
    }

    private int getPunchoutPercent() {
        try {
            a = CivSettings.getInteger(CivSettings.enchantConfig, "punchout_chance");
        } catch (InvalidConfiguration e) {
            a = 50;
            e.printStackTrace();
        }
        return a;
    }

    private int getPunchoutDamage() {
        try {
            b = CivSettings.getInteger(CivSettings.enchantConfig, "punchout_maxdamage");
        } catch (InvalidConfiguration e) {
            b = 5;
            e.printStackTrace();
        }
        return b;
    }

    @Override
    public int onStructureBlockBreak(BuildableDamageBlock sb, int damage) {
        Random rand = new Random();

        if (damage <= 1) {
            if (rand.nextInt(100) <= getPunchoutPercent()) {
                damage += rand.nextInt(getPunchoutDamage()) + 1;
            }
        }

        return damage;
    }

    @Override
    public String serialize(ItemStack stack) {
        return "";
    }

    @Override
    public ItemStack deserialize(ItemStack stack, String data) {
        return stack;
    }

}