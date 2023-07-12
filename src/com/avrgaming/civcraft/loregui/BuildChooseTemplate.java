package com.avrgaming.civcraft.loregui;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.config.ConfigPerk;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.tutorial.CivTutorial;
import com.avrgaming.civcraft.util.CivColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class BuildChooseTemplate implements GuiAction {

    @Override
    public void performAction(InventoryClickEvent event, ItemStack stack) {
        Player player = (Player) event.getWhoClicked();
        Resident resident = CivGlobal.getResident(player);
        ConfigBuildableInfo sinfo = CivSettings.structures.get(LoreGuiItem.getActionData(stack, "info"));
        Structure struct;
        try {
            struct = Structure.newStructure(player.getLocation(), sinfo.id, resident.getTown());
        } catch (CivException e) {
            e.printStackTrace();
            return;
        }

        /* Look for any custom template perks and ask the player if they want to use them. */
        ArrayList<ConfigPerk> perkList = struct.getTown().getTemplatePerks(struct, resident, struct.info);
        //if (perkList.size() != 0 || personalUnboundPerks.size() != 0) {
        /* Store the pending buildable. */
        resident.pendingBuildable = struct;

        /* Build an inventory full of templates to select. */
        Inventory inv = Bukkit.getServer().createInventory(player, CivTutorial.MAX_CHEST_SIZE * 9);
        ItemStack infoRec = LoreGuiItem.build("Default " + struct.getDisplayName(),
                Material.WRITTEN_BOOK,
                0, CivColor.Gold + CivSettings.localize.localizedString("loreGui_template_clickToBuild"));
        infoRec = LoreGuiItem.setAction(infoRec, "BuildWithTemplate");
        inv.addItem(infoRec);

        for (ConfigPerk perk : perkList) {
            infoRec = LoreGuiItem.build(
                    perk.display_name,
                    perk.type_id,
                    perk.data,
                    CivColor.Gold + CivSettings.localize.localizedString("loreGui_template_clickToBuild")
            );
            infoRec = LoreGuiItem.setAction(infoRec, "BuildWithTemplate");
            infoRec = LoreGuiItem.setActionData(infoRec, "theme", perk.theme);
            inv.addItem(infoRec);

        }

        TaskMaster.syncTask(new OpenInventoryTask(player, inv));
    }
}
