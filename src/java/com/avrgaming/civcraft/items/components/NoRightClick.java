package com.avrgaming.civcraft.items.components;

import gpl.AttributeUtil;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class NoRightClick extends ItemComponent {

    @Override
    public void onPrepareCreate(AttributeUtil attrUtil) {
    }


    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction().equals(Action.RIGHT_CLICK_AIR)) {
            event.setCancelled(true);
        } else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            if (event.getClickedBlock().getType() != Material.CHEST) {
                event.setCancelled(true);
            }
        }
    }

    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        event.setCancelled(true);
    }

}
