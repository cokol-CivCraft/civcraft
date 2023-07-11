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
import com.avrgaming.global.perks.components.CustomPersonalTemplate;
import com.avrgaming.global.perks.components.CustomTemplate;
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
            Template tpl;
            if (perk_id != null) {
                /* Use a template defined by a perk. */
                Perk perk = Perk.staticPerks.get(perk_id);
                if (perk != null) {

                    /* get the template name from the perk's CustomTemplate component. */
                    CustomTemplate customTemplate = (CustomTemplate) perk.getComponent(Perk.ComponentsNames.CustomTemplate);
                    if (customTemplate != null) {
                        tpl = customTemplate.getTemplate(player, resident.pendingBuildable);
                    } else {
                        CustomPersonalTemplate customPersonalTemplate = (CustomPersonalTemplate) perk.getComponent(Perk.ComponentsNames.CustomPersonalTemplate);
                        tpl = customPersonalTemplate.getTemplate(player, resident.pendingBuildable.info);
                    }

                    resident.pendingBuildable.buildPlayerPreview(player, player.getLocation(), tpl);

                } else {
                    CivLog.error(perk_id + " " + CivSettings.localize.localizedString("loreGui_perkActivationFailed"));
                }
            } else {
                /* Use the default template. */
                tpl = new Template();
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
