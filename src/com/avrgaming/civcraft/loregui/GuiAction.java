package com.avrgaming.civcraft.loregui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;


public abstract class GuiAction {
    public final GuiActions KEY;

    public GuiAction(GuiActions key) {
        KEY = key;
    }

    abstract public void performAction(InventoryClickEvent event, ItemStack stack);
}
