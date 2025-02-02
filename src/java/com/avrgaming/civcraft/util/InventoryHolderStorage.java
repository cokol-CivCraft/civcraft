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
package com.avrgaming.civcraft.util;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

public class InventoryHolderStorage {

    /*
     * An inventory holder can be a 'block'or a player.
     */
    private Location blockLocation;

    private String playerName;

    public InventoryHolderStorage(InventoryHolder holder, Location holderLocation) {
        if (holder instanceof Player player) {
            playerName = player.getName();
            blockLocation = null;
        } else {
            blockLocation = holderLocation;
        }
    }

    public InventoryHolderStorage(Location blockLoc) {
        blockLocation = blockLoc;
        playerName = null;
    }

    public InventoryHolderStorage(Player player) {
        blockLocation = null;
        playerName = player.getName();
    }

    public InventoryHolder getHolder() throws CivException {
        if (playerName != null) {
            return CivGlobal.getPlayer(playerName);
        }

        if (blockLocation != null) {
            /* Make sure the chunk is loaded. */

            if (!blockLocation.getChunk().isLoaded()) {
                if (!blockLocation.getChunk().load()) {
                    throw new CivException("Couldn't load chunk at " + blockLocation + " where holder should reside.");
                }
            }
            if (!(blockLocation.getBlock().getState() instanceof Chest chest)) {
                throw new CivException("Holder location is not a chest, invalid.");
            }

            return chest.getInventory().getHolder();
        }

        throw new CivException("Invalid holder.");
    }

    public void setHolder(InventoryHolder holder) throws CivException {
        if (holder instanceof Player player) {
            playerName = player.getName();
            blockLocation = null;
            return;
        }

        if (holder instanceof Chest chest) {
            playerName = null;
            blockLocation = chest.getLocation();
            return;
        }

        if (holder instanceof DoubleChest dchest) {
            playerName = null;
            blockLocation = dchest.getLocation();
            return;
        }

        throw new CivException("Invalid holder passed to set holder:" + holder.toString());
    }

}
