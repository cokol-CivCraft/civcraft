/*
 * AVRGAMING LLC
 * __________________
 *
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.command.civ;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CivChatCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

        //TODO let non players use this command
        if (!(sender instanceof Player player)) {
            return false;
        }

        Resident resident = CivGlobal.getResident(player);
        if (resident == null) {
            CivMessage.sendError(sender, CivSettings.localize.localizedString("cmd_civchat_notResident"));
            return false;
        }

        if (args.length == 0) {
            resident.setCivChat(!resident.isCivChat());
            resident.setTownChat(false);
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_civchat_modeSet") + " " + resident.isCivChat());
            return true;
        }


        StringBuilder fullArgs = new StringBuilder();
        for (String arg : args) {
            fullArgs.append(arg).append(" ");
        }

        if (resident.getTown() == null) {
            player.sendMessage(ChatColor.RED + CivSettings.localize.localizedString("cmd_civchat_error"));
            return false;
        }

        CivMessage.sendCivChat(resident.getTown().getCiv(), resident, "<%s> %s", fullArgs.toString());
        return true;
    }
}
