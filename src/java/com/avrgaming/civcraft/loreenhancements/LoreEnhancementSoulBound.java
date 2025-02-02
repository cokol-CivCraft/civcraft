package com.avrgaming.civcraft.loreenhancements;

import com.avrgaming.civcraft.config.CivSettings;
import gpl.AttributeUtil;
import org.bukkit.ChatColor;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

public class LoreEnhancementSoulBound extends LoreEnhancement {

    public AttributeUtil add(AttributeUtil attrs) {
        attrs.addEnhancement("LoreEnhancementSoulBound", null, null);
        attrs.addLore(ChatColor.GOLD + getDisplayName());
        return attrs;
    }

    public boolean onDeath(final PlayerDeathEvent event, final ItemStack stack) {
        event.getDrops().remove(stack);
        return true;
    }

    public boolean canEnchantItem(ItemStack item) {
        return isWeaponOrArmor(item);
    }

    public boolean hasEnchantment(ItemStack item) {
        AttributeUtil attrs = new AttributeUtil(item);
        return attrs.hasEnhancement("LoreEnhancementSoulBound");
    }

    public String getDisplayName() {
        return CivSettings.localize.localizedString("itemLore_Soulbound");
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
