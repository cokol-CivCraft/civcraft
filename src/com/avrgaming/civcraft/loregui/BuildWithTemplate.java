package com.avrgaming.civcraft.loregui;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.global.perks.Perk;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;

public class BuildWithTemplate implements GuiAction {

    @Override
    public void performAction(InventoryClickEvent event, ItemStack stack) {
        Player player = (Player) event.getWhoClicked();
        Resident resident = CivGlobal.getResident(player);

        String perk_id = LoreGuiItem.getActionData(stack, "perk");

        try {
            if (perk_id != null) {
                /* Use a template defined by a perk. */
                Perk perk = Perk.staticPerks.get(perk_id);
                if (perk == null) {
                    player.closeInventory();
                    CivLog.error(perk_id + " " + CivSettings.localize.localizedString("loreGui_perkActivationFailed"));
                    return;
                }
                Template tpl = new Template();
                try {
                    tpl.initTemplate(player.getLocation(), resident.pendingBuildable, perk.theme);
                } catch (CivException | IOException e) {
                    e.printStackTrace();
                }

                resident.pendingBuildable.buildPlayerPreview(player, player.getLocation(), tpl);


            } else {
                /* Use the default template. */
                Template tpl = new Template();
                try {
                    tpl.initTemplate(player.getLocation(), resident.pendingBuildable);
                } catch (CivException | IOException e) {
                    e.printStackTrace();
                    throw e;
                }

                resident.pendingBuildable.buildPlayerPreview(player, player.getLocation(), tpl);
            }
        } catch (CivException e) {
            CivMessage.sendError(player, e.getMessage());
        } catch (IOException e) {
            CivMessage.sendError(player, CivSettings.localize.localizedString("internalIOException"));
            e.printStackTrace();
        }
        player.closeInventory();
    }

}
