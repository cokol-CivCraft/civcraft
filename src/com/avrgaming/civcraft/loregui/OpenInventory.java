package com.avrgaming.civcraft.loregui;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.lorestorage.LoreGuiItemListener;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.tutorial.CivTutorial;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class OpenInventory implements GuiAction {

    @Override
    public void performAction(InventoryClickEvent event, ItemStack stack) {
        Player player = (Player) event.getWhoClicked();
        player.closeInventory();

        TaskMaster.syncTask(() -> {
            Player player1;
            try {
                player1 = CivGlobal.getPlayer(player.getName());
            } catch (CivException e) {
                e.printStackTrace();
                return;
            }

            switch (LoreGuiItem.getActionData(stack, "invType")) {
                case "showTutorialInventory":
                    CivTutorial.showTutorialInventory(player1);
                    break;
                case "showCraftingHelp":
                    CivTutorial.showCraftingHelp(player1);
                    break;
                case "showGuiInv":
                    String invName = LoreGuiItem.getActionData(stack, "invName");
                    Inventory inv = LoreGuiItemListener.guiInventories.get(invName);
                    if (inv == null) {
                        CivLog.error("Couldn't find GUI inventory:" + invName);
                        break;
                    }
                    player1.openInventory(inv);
                    break;
                default:
                    break;
            }
        });
    }

}
