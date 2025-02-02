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

package com.avrgaming.civcraft.items.components;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.interactive.InteractiveCivName;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.CallbackInterface;
import gpl.AttributeUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class FoundCivilization extends ItemComponent implements CallbackInterface {

    @Override
    public void onPrepareCreate(AttributeUtil attrUtil) {
        attrUtil.addLore(ChatColor.RESET + String.valueOf(ChatColor.GOLD) + CivSettings.localize.localizedString("foundCiv_lore1"));
        attrUtil.addLore(ChatColor.RESET + String.valueOf(ChatColor.RED) + CivSettings.localize.localizedString("itemLore_RightClickToUse"));
        attrUtil.addEnhancement("LoreEnhancementSoulBound", null, null);
        attrUtil.addLore(ChatColor.GOLD + CivSettings.localize.localizedString("Soulbound"));
    }

    public void foundCiv(Player player) throws CivException {

        Resident resident = CivGlobal.getResident(player);
        if (resident == null) {
            throw new CivException(CivSettings.localize.localizedString("foundCiv_notResident"));
        }

        /*
         * Build a preview for the Capitol structure.
         */
        CivMessage.send(player, String.valueOf(ChatColor.GREEN) + ChatColor.BOLD + CivSettings.localize.localizedString("build_checking_position"));
        ConfigBuildableInfo info = CivSettings.structures.get("s_capitol");
        Buildable.buildVerifyStatic(player, info, player.getLocation(), this);
    }

    public void onInteract(PlayerInteractEvent event) {
        event.setCancelled(true);
        if (!event.getAction().equals(Action.RIGHT_CLICK_AIR) && !event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        TaskMaster.syncTask(() -> {
            try {
                Player player = CivGlobal.getPlayer(event.getPlayer().getName());
                try {
                    foundCiv(player);
                } catch (CivException e) {
                    CivMessage.sendError(player, e.getMessage());
                }
            } catch (CivException e) {
                e.printStackTrace();
            }
        });

    }

    @Override
    public void execute(String playerName) {

        Player player;
        try {
            player = CivGlobal.getPlayer(playerName);
        } catch (CivException e) {
            return;
        }

        Resident resident = CivGlobal.getResident(player);

        /* Save the location so we dont have to re-validate the structure position. */
        resident.desiredTownLocation = player.getLocation();
        CivMessage.sendHeading(player, CivSettings.localize.localizedString("foundCiv_Heading"));
        CivMessage.send(player, ChatColor.GREEN + CivSettings.localize.localizedString("foundCiv_Prompt1"));
        CivMessage.send(player, ChatColor.GREEN + CivSettings.localize.localizedString("foundCiv_Prompt2"));
        CivMessage.send(player, ChatColor.GREEN + CivSettings.localize.localizedString("foundCiv_Prompt3"));
        CivMessage.send(player, ChatColor.GREEN + CivSettings.localize.localizedString("foundCiv_Prompt4"));
        CivMessage.send(player, " ");
        CivMessage.send(player, String.valueOf(ChatColor.GREEN) + ChatColor.BOLD + CivSettings.localize.localizedString("foundCiv_Prompt5"));
        CivMessage.send(player, ChatColor.GRAY + CivSettings.localize.localizedString("build_cancel_prompt"));

        resident.setInteractiveMode(new InteractiveCivName());
    }


}
