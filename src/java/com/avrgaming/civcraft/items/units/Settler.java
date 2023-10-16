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
package com.avrgaming.civcraft.items.units;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigUnit;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.interactive.InteractiveTownName;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.TownHall;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CallbackInterface;
import gpl.AttributeUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;

public class Settler extends UnitMaterial implements CallbackInterface {

    public Settler(String id, ConfigUnit configUnit) {
        super(id, configUnit);
    }

    public static void spawn(Inventory inv, Town town) throws CivException {

        ItemStack is = LoreMaterial.spawn(Unit.SETTLER_UNIT);

        UnitMaterial.setOwningTown(town, is);

        AttributeUtil attrs = new AttributeUtil(is);
        attrs.addLore(ChatColor.RED + CivSettings.localize.localizedString("settler_Lore1") + " " + ChatColor.AQUA + town.getCiv().getName());
        attrs.addLore(ChatColor.GOLD + CivSettings.localize.localizedString("settler_Lore2"));
        attrs.addEnhancement("LoreEnhancementSoulBound", null, null);
        attrs.addLore(ChatColor.GOLD + CivSettings.localize.localizedString("Soulbound"));

        attrs.setCivCraftProperty("owner_civ_id", String.valueOf(town.getCiv().getId()));


        if (!Unit.addItemNoStack(inv, attrs.getStack())) {
            throw new CivException(CivSettings.localize.localizedString("var_settler_errorBarracksFull", Unit.SETTLER_UNIT.getUnit().name));
        }

    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        Resident resident = CivGlobal.getResident(player);

        if (resident == null || !resident.hasTown()) {
            CivMessage.sendError(player, CivSettings.localize.localizedString("settler_errorNotRes"));
            return;
        }

        if (new AttributeUtil(event.getItem()).getCivCraftProperty("owner_civ_id") == null) {
            CivMessage.sendError(player, CivSettings.localize.localizedString("settler_errorInvalidOwner"));
            return;
        }

        if (Integer.parseInt(new AttributeUtil(event.getItem()).getCivCraftProperty("owner_civ_id")) != resident.getCiv().getId()) {
            CivMessage.sendError(player, CivSettings.localize.localizedString("settler_errorNotOwner"));
            return;
        }

        double minDistance = CivSettings.townConfig.getDouble("town.min_town_distance", 150.0);

        for (Town town : Town.getTowns()) {
            TownHall townhall = town.getTownHall();
            if (townhall == null) {
                continue;
            }

            double dist = townhall.getCenterLocation().distance(new BlockCoord(event.getPlayer().getLocation()));
            if (dist < minDistance) {
                DecimalFormat df = new DecimalFormat();
                CivMessage.sendError(player, CivSettings.localize.localizedString("var_settler_errorTooClose", town.getName(), df.format(dist), minDistance));
                return;
            }
        }


        /*
         * Build a preview for the Capitol structure.
         */
        CivMessage.send(player, String.valueOf(ChatColor.GREEN) + ChatColor.BOLD + CivSettings.localize.localizedString("build_checking_position"));
        try {
            Buildable.buildVerifyStatic(player, CivSettings.structures.get("s_townhall"), player.getLocation(), this);
        } catch (CivException e) {
            CivMessage.sendError(player, e.getMessage());
        }
    }

    @Override
    public void execute(String playerName) {
        Player player;
        try {
            player = CivGlobal.getPlayer(playerName);
        } catch (CivException e) {
            return;
        }
        Resident resident = CivGlobal.getResident(playerName);
        resident.desiredTownLocation = player.getLocation();

        CivMessage.sendHeading(player, CivSettings.localize.localizedString("settler_heading"));
        CivMessage.send(player, ChatColor.GREEN + CivSettings.localize.localizedString("settler_prompt1"));
        CivMessage.send(player, " ");
        CivMessage.send(player, String.valueOf(ChatColor.GREEN) + ChatColor.BOLD + CivSettings.localize.localizedString("settler_prompt2"));
        CivMessage.send(player, ChatColor.GRAY + CivSettings.localize.localizedString("build_cancel_prompt"));

        resident.setInteractiveMode(new InteractiveTownName());
    }

}
