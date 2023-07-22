package com.avrgaming.civcraft.items.components;

import com.avrgaming.civcraft.util.BlockCoord;
import gpl.AttributeUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

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

            Block block = bcoord.getBlock();
            if (block.getTypeId() == Material.CAULDRON.getId()) {
                event.setCancelled(true);
            }
        }
    }
}