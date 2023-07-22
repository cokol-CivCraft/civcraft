package com.avrgaming.civcraft.loregui;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.template.Template;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;

public class BuildWithTemplate extends GuiAction {
    public BuildWithTemplate(GuiActions key) {
        super(key);
    }

    @Override
    public void performAction(InventoryClickEvent event, ItemStack stack) {
        Player player = (Player) event.getWhoClicked();
        Resident resident = CivGlobal.getResident(player);

        String theme = LoreGuiItem.getActionData(stack, "theme");

        try {
            if (theme == null) {
                /* Use the default template. */
                Template tpl = new Template();
                try {
                    tpl.initTemplate(player.getLocation(), resident.pendingBuildable);
                } catch (IOException e) {
                    CivMessage.sendError(player, CivSettings.localize.localizedString("internalIOException"));
                    e.printStackTrace();
                } catch (CivException e) {
                    e.printStackTrace();
                }

                resident.pendingBuildable.buildPlayerPreview(player, player.getLocation(), tpl);
                player.closeInventory();
                return;
            }

            /* Use a template defined by a perk. */
            Template tpl = new Template();
            try {
                tpl.initTemplate(player.getLocation(), resident.pendingBuildable, theme);
            } catch (IOException e) {
                CivMessage.sendError(player, CivSettings.localize.localizedString("internalIOException"));
                e.printStackTrace();
            } catch (CivException e) {
                e.printStackTrace();
            }

            resident.pendingBuildable.buildPlayerPreview(player, player.getLocation(), tpl);
            player.closeInventory();


        } catch (CivException e) {
            CivMessage.sendError(player, e.getMessage());
        }
    }

}
