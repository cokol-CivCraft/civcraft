package com.avrgaming.civcraft.siege;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.BlockCoord;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class CannonListener implements Listener {

    BlockCoord bcoord = new BlockCoord();

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {

        bcoord.setFromLocation(event.getBlock().getLocation());
        Cannon cannon = Cannon.cannonBlocks.get(bcoord);
        if (cannon != null) {
            cannon.onHit(event);
            event.setCancelled(true);
        }

    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {

        if (!event.hasBlock()) {
            return;
        }

        try {
            bcoord.setFromLocation(event.getClickedBlock().getLocation());
            Cannon cannon1 = Cannon.fireSignLocations.get(bcoord);
            if (cannon1 != null) {
                cannon1.processFire(event);
                event.setCancelled(true);
                return;
            }

            Cannon cannon2 = Cannon.angleSignLocations.get(bcoord);
            if (cannon2 != null) {
                cannon2.processAngle(event);
                event.setCancelled(true);
                return;
            }

            Cannon cannon3 = Cannon.powerSignLocations.get(bcoord);
            if (cannon3 != null) {
                cannon3.processPower(event);
                event.setCancelled(true);
            }
        } catch (CivException e) {
            CivMessage.sendError(event.getPlayer(), e.getMessage());
            event.setCancelled(true);
        }


    }
}
