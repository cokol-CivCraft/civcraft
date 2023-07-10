package com.avrgaming.civcraft.loreenhancements;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.util.CivColor;
import gpl.AttributeUtil;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

public class LoreEnhancementUnbreaking extends LoreEnhancement {

    public AttributeUtil add(AttributeUtil attrs) {
        attrs.addEnhancement("LoreEnhancementUnbreaking", null, null);
        attrs.addLore(CivColor.Gold + getDisplayName());
        return attrs;
    }

    @Override
    public void onDurabilityChange(PlayerItemDamageEvent event) {
        event.setDamage(0);
    }

    public boolean canEnchantItem(ItemStack item) {
        return isWeaponOrArmor(item);
    }

    public boolean hasEnchantment(ItemStack item) {
        AttributeUtil attrs = new AttributeUtil(item);
        return attrs.hasEnhancement("LoreEnhancementUnbreaking");
    }

    public String getDisplayName() {
        return CivSettings.localize.localizedString("itemLore_Unbreaking");
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
