package com.avrgaming.civcraft.loregui;

import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.MetaStructure;
import com.avrgaming.civcraft.structurevalidation.StructureValidator;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.template.Template.TemplateType;
import com.avrgaming.civcraft.threading.TaskMaster;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;

public class BuildWithDefaultPersonalTemplate extends GuiAction {
    public BuildWithDefaultPersonalTemplate(GuiActions key) {
        super(key);
    }

    @Override
    public void performAction(InventoryClickEvent event, ItemStack stack) {
        Player player = (Player) event.getWhoClicked();
        Resident resident = CivGlobal.getResident(player);
        ConfigBuildableInfo info = resident.pendingBuildableInfo;

        try {
            String path = Template.getTemplateFilePath(info.template_base_name, TemplateType.STRUCTURE, "default");
            Template tpl;
            MetaStructure struct = MetaStructure.newStructOrWonder(player.getLocation(), info, resident.pendingBuildable.town());
            try {
                //tpl.load_template(path);
                tpl = Template.getTemplate(path, player.getLocation());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            Location centerLoc = Buildable.repositionCenterStatic(player.getLocation(), info, Template.getDirection(player.getLocation()), tpl.size_x, tpl.size_z);
            //Buildable.validate(player, null, tpl, centerLoc, resident.pendingCallback);
            struct.buildPlayerPreview(player, tpl);
            TaskMaster.asyncTask(new StructureValidator(player, tpl.getFilepath(), centerLoc, resident.pendingCallback), 0);
            player.closeInventory();

        } catch (CivException e) {
            CivMessage.sendError(player, e.getMessage());
        }
    }

}
