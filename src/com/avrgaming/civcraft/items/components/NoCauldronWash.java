package com.avrgaming.civcraft.items.components;

import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ItemManager;

import gpl.AttributeUtil;

public class NoCauldronWash extends ItemComponent {

	@Override
	public void onPrepareCreate(AttributeUtil attrUtil) {
	}

	
	public void onInteract(PlayerInteractEvent event) {
		if (event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			if (!event.hasBlock()) {
				return;
			}
						
			BlockCoord bcoord = new BlockCoord(event.getClickedBlock());
						
			if (ItemManager.getId(bcoord.getBlock()) == ItemManager.getId(Material.CAULDRON)) {
				event.setCancelled(true);
				return;
			}
		}
	}
}