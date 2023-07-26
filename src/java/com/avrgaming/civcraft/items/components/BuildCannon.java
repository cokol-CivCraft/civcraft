package com.avrgaming.civcraft.items.components;

import gpl.AttributeUtil;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.siege.Cannon;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.war.War;

public class BuildCannon extends ItemComponent {

	public void onInteract(PlayerInteractEvent event) {
		try {
			
			if (!War.isWarTime()) {
				throw new CivException(CivSettings.localize.localizedString("buildCannon_NotWar"));
			}
			
			Resident resident = CivGlobal.getResident(event.getPlayer());
			Cannon.newCannon(resident);
			
			CivMessage.sendCiv(resident.getCiv(), CivSettings.localize.localizedString("var_buildCannon_Success",
					(event.getPlayer().getLocation().getBlockX()+","+
					event.getPlayer().getLocation().getBlockY()+","+
					event.getPlayer().getLocation().getBlockZ())));
			
			ItemStack newStack = new ItemStack(Material.AIR);
			event.getPlayer().getInventory().setItemInMainHand(newStack);
		} catch (CivException e) {
			CivMessage.sendError(event.getPlayer(), e.getMessage());
		}
		
	}

	@Override
	public void onPrepareCreate(AttributeUtil attrUtil) {
		attrUtil.addLore(ChatColor.RESET+CivColor.Gold+CivSettings.localize.localizedString("buildCannon_Lore1"));
		attrUtil.addLore(ChatColor.RESET+CivColor.Rose+CivSettings.localize.localizedString("itemLore_RightClickToUse"));	
	}
	
}
