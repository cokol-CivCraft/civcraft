package com.avrgaming.civcraft.command.admin;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.loreenhancements.*;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class AdminItemCommand extends CommandBase {

    @Override
    public void init() {
        command = "/ad item";
        displayName = CivSettings.localize.localizedString("adcmd_item_cmdDesc");

        register_sub("enhance", this::enhance_cmd, CivSettings.localize.localizedString("adcmd_item_enhanceDesc"));
        register_sub("give", this::give_cmd, CivSettings.localize.localizedString("adcmd_item_giveDesc"));
    }


    public void give_cmd() throws CivException {
        Resident resident = getNamedResident(1);
        String id = getNamedString(2, CivSettings.localize.localizedString("adcmd_item_givePrompt") + " materials.yml");
        int amount = getNamedInteger(3);

        Player player = CivGlobal.getPlayer(resident);

        LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterialFromId(id);
        if (craftMat == null) {
            throw new CivException(CivSettings.localize.localizedString("adcmd_item_giveInvalid") + id);
        }

        ItemStack stack = LoreCraftableMaterial.spawn(craftMat);

        stack.setAmount(amount);
        HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
        for (ItemStack is : leftovers.values()) {
            player.getWorld().dropItem(player.getLocation(), is);
        }

        CivMessage.sendSuccess(player, CivSettings.localize.localizedString("adcmd_item_giveSuccess"));
    }


    public void enhance_cmd() throws CivException {
        Player player = getPlayer();
        HashMap<String, LoreEnhancement> enhancements = new HashMap<>();
        ItemStack inHand = getPlayer().getInventory().getItemInMainHand();

        enhancements.put("soulbound", new LoreEnhancementSoulBound());
        enhancements.put("attack", new LoreEnhancementAttack());
        enhancements.put("defence", new LoreEnhancementDefense());
        enhancements.put("arena", new LoreEnhancementArenaItem());

        if (inHand == null || inHand.getType() == Material.AIR) {
            throw new CivException(CivSettings.localize.localizedString("adcmd_item_enhanceNoItem"));
        }

        if (args.length < 2) {
            CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_item_enhancementList"));
            StringBuilder out = new StringBuilder();
            for (String str : enhancements.keySet()) {
                out.append(str).append(", ");
            }
            CivMessage.send(sender, out.toString());
            return;
        }

        String name = getNamedString(1, "enchantname");
        for (String str : enhancements.keySet()) {
            if (name.equals(str)) {
                LoreEnhancement enh = enhancements.get(str);
                ItemStack stack = LoreMaterial.addEnhancement(inHand, enh);
                player.getInventory().setItemInMainHand(stack);
                CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_item_enhanceSuccess", name));
                return;
            }
        }
    }

    @Override
    public void doDefaultAction() {
        showHelp();
    }

    @Override
    public void showHelp() {
        showBasicHelp();
    }

    @Override
    public void permissionCheck() {

    }

}
