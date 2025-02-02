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
package com.avrgaming.civcraft.command;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GlobalChatCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

        //TODO let non players use this command
        if (!(sender instanceof Player player)) {
            return false;
        }

        Resident resident = CivGlobal.getResident(player);
        if (resident == null) {
            CivMessage.sendError(sender, CivSettings.localize.localizedString("cmd_gc_notResident"));
            return false;
        }

        if (args.length == 0) {
            resident.setCivChat(false);
            resident.setTownChat(false);
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_gc_enabled"));
            return true;
        }

        CivMessage.sendError(sender, CivSettings.localize.localizedString("cmd_gc_disabled"));
        return true;

//		String fullArgs = "";
//		for (String arg : args) {
//			fullArgs += arg + " ";
//		}
//		
//		CivMessage.sendChat(resident, "<%s> %s", fullArgs);
//		return true;
    }
}
