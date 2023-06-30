/*************************************************************************
 * 
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
package com.avrgaming.civcraft.items;

import java.util.LinkedList;

import com.avrgaming.civcraft.listener.CustomItemManager;

public class ItemDuraSyncTask implements Runnable {

	@Override
	public void run() {
		
		for (String playerName : CustomItemManager.itemDuraMap.keySet()) {
			LinkedList<ItemDurabilityEntry> entries = CustomItemManager.itemDuraMap.get(playerName);
			
			for (ItemDurabilityEntry entry : entries) {
				entry.stack.setDurability(entry.oldValue);
			}
		}
		CustomItemManager.duraTaskScheduled = false;
	}
}