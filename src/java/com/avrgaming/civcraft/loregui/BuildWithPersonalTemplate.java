package com.avrgaming.civcraft.loregui;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.MetaStructure;
import com.avrgaming.civcraft.structurevalidation.StructureValidator;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.threading.TaskMaster;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;

public class BuildWithPersonalTemplate extends GuiAction {
    public BuildWithPersonalTemplate(GuiActions key) {
        super(key);
    }

    @Override
    public void performAction(InventoryClickEvent event, ItemStack stack) {
        Player player = (Player) event.getWhoClicked();
        Resident resident = CivGlobal.getResident(player);

        ConfigBuildableInfo info = resident.getPendingBuildable();

        try {
            MetaStructure struct = MetaStructure.newStructOrWonder(player.getLocation(), info, resident.pendingBuildable.town());
            /* get the template name from the perk's CustomTemplate component. */
            String theme = LoreGuiItem.getActionData(stack, "theme");
            Template tpl = new Template();
            try {
                tpl.initTemplate(player.getLocation(), info, theme);
            } catch (IOException e) {
                CivMessage.sendError(player, CivSettings.localize.localizedString("internalIOException"));
                e.printStackTrace();
            } catch (CivException e) {
                e.printStackTrace();
            }
            Location centerLoc = Buildable.repositionCenterStatic(player.getLocation(), info, Template.getDirection(player.getLocation()), tpl.size_x, tpl.size_z);
            TaskMaster.asyncTask(new StructureValidator(player, tpl.getFilepath(), centerLoc, resident.pendingCallback), 0);
            resident.desiredTemplate = tpl;
            struct.buildPlayerPreview(player, tpl);
            player.closeInventory();
        } catch (CivException e) {
            CivMessage.sendError(player, e.getMessage());
            e.printStackTrace();
        }
    }

}
