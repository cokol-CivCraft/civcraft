package com.avrgaming.civcraft.interactive;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.structure.Barracks;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class InteractiveRepairItem implements InteractiveResponse {

    double cost;
    String playerName;
    LoreCraftableMaterial craftMat;

    public InteractiveRepairItem(double cost, String playerName, LoreCraftableMaterial craftMat) {
        this.cost = cost;
        this.playerName = playerName;
        this.craftMat = craftMat;
    }

    public void displayMessage() {
        Player player;
        try {
            player = CivGlobal.getPlayer(playerName);
        } catch (CivException e) {
            return;
        }

        CivMessage.sendHeading(player, CivSettings.localize.localizedString("interactive_repair_heading"));
        CivMessage.send(player, String.valueOf(ChatColor.GREEN) + ChatColor.BOLD + CivSettings.localize.localizedString("var_interactive_repair_prompt1", craftMat.getName()));
        CivMessage.send(player, String.valueOf(ChatColor.GREEN) + ChatColor.BOLD + CivSettings.localize.localizedString("var_interactive_repair_prompt2", String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + cost + ChatColor.GREEN, String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.CURRENCY_NAME + ChatColor.GREEN));
        CivMessage.send(player, String.valueOf(ChatColor.GREEN) + ChatColor.BOLD + CivSettings.localize.localizedString("interactive_repair_prompt3"));

    }


    @Override
    public void respond(String message, Resident resident) {
        resident.clearInteractiveMode();

        if (!message.equalsIgnoreCase("yes")) {
            CivMessage.send(resident, ChatColor.GRAY + CivSettings.localize.localizedString("interactive_repair_canceled"));
            return;
        }

        Barracks.repairItemInHand(cost, resident.getName(), craftMat);
    }

}
