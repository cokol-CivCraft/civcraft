package com.avrgaming.civcraft.items.components;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.interactive.InteractiveWarCampFound;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.war.War;
import gpl.AttributeUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class FoundWarCamp extends ItemComponent implements CallbackInterface {

    public static ConfigBuildableInfo info = new ConfigBuildableInfo();

    static {
        info.id = "warcamp";
        info.displayName = "War Camp";
        info.ignore_floating = false;
        info.template_base_name = "warcamp";
        info.tile_improvement = false;
        info.templateYShift = -1;
        info.max_hp = 100;
    }

    @Override
    public void onPrepareCreate(AttributeUtil attrUtil) {
        attrUtil.addLore(ChatColor.RESET + String.valueOf(ChatColor.GOLD) + CivSettings.localize.localizedString("buildWarCamp_lore1"));
        attrUtil.addLore(ChatColor.RESET + String.valueOf(ChatColor.RED) + CivSettings.localize.localizedString("itemLore_RightClickToUse"));
    }

    public void foundCamp(Player player) throws CivException {
        Resident resident = CivGlobal.getResident(player);

        if (!resident.hasTown()) {
            throw new CivException(CivSettings.localize.localizedString("buildWarCamp_errorNotInCiv"));
        }

        if (!resident.getCiv().getLeaderGroup().hasMember(resident) &&
                !resident.getCiv().getAdviserGroup().hasMember(resident)) {
            throw new CivException(CivSettings.localize.localizedString("buildWarCamp_errorNotPerms"));
        }

        if (!War.isWarTime()) {
            throw new CivException(CivSettings.localize.localizedString("buildWarCamp_errorNotWarTime"));
        }

        /*
         * Build a preview for the Capitol structure.
         */
        CivMessage.send(player, String.valueOf(ChatColor.GREEN) + ChatColor.BOLD + CivSettings.localize.localizedString("build_checking_position"));


        Buildable.buildVerifyStatic(player, info, player.getLocation(), this);
    }

    public void onInteract(PlayerInteractEvent event) {
        event.setCancelled(true);
        if (!event.getAction().equals(Action.RIGHT_CLICK_AIR) &&
                !event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        try {
            foundCamp(event.getPlayer());
        } catch (CivException e) {
            CivMessage.sendError(event.getPlayer(), e.getMessage());
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
        int warTimeout = CivSettings.warConfig.getInt("warcamp.rebuild_timeout", 30);

        CivMessage.sendHeading(player, CivSettings.localize.localizedString("buildWarCamp_heading"));
        CivMessage.send(player, ChatColor.GREEN + CivSettings.localize.localizedString("buildWarCamp_prompt1"));
        CivMessage.send(player, ChatColor.GREEN + "   -" + CivSettings.localize.localizedString("buildWarCamp_prompt2"));
        CivMessage.send(player, ChatColor.GREEN + "   -" + CivSettings.localize.localizedString("var_buildWarCamp_prompt3", warTimeout));
        CivMessage.send(player, " ");
        CivMessage.send(player, String.valueOf(ChatColor.GREEN) + ChatColor.BOLD + CivSettings.localize.localizedString("buildWarCamp_prompt5"));
        CivMessage.send(player, ChatColor.GRAY + CivSettings.localize.localizedString("buildWarCamp_prompt6"));

        resident.setInteractiveMode(new InteractiveWarCampFound(info));
    }
}
