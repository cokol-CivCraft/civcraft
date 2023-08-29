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
package com.avrgaming.civcraft.threading.tasks;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class DelayMoveInventoryItem implements Runnable {

    /*
     * Sometimes we want to preform an action on an inventory after a very short delay.
     * For example, if we want to lock an item to a slot, we cannot cancel the move event since
     * that results in the item being 'dropped' on the ground. Instead we have to let the move
     * event complete, and then issue another action to move the item back.
     */

    public final int fromSlot;
    public final int toSlot;
    public final Inventory inv;

    public DelayMoveInventoryItem(int fromSlot, int toSlot, Inventory inv) {
        this.fromSlot = fromSlot;
        this.toSlot = toSlot;
        this.inv = inv;
    }

    @Override
    public void run() {

        ItemStack fromStack = inv.getItem(fromSlot);
        ItemStack toStack = inv.getItem(toSlot);

        if (fromStack != null) {
            inv.setItem(toSlot, fromStack);
            inv.setItem(fromSlot, toStack);
        }
    }
}
