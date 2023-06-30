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
package com.avrgaming.civcraft.items.components;

import org.bukkit.event.player.PlayerItemDamageEvent;

import gpl.AttributeUtil;

public class NoVanillaDurability extends ItemComponent {

	@Override
	public void onPrepareCreate(AttributeUtil attrUtil) {
	}
	
	
	//private ConcurrentHashMap<String, String> playersToUpdateInventory = new ConcurrentHashMap<String, String>();
	
	@Override
	public void onDurabilityChange(PlayerItemDamageEvent event) {
		event.setDamage(0);
		
//		LinkedList<ItemDurabilityEntry> entries = CustomItemManager.itemDuraMap.get(player.getName());
//		
//		if (entries == null) {
//			entries = new LinkedList<ItemDurabilityEntry>();
//		}
//		
//		ItemDurabilityEntry entry = new ItemDurabilityEntry();
//		entry.stack = stack;
//		entry.oldValue = stack.getDurability();
//		
//		entries.add(entry);
//		CustomItemManager.itemDuraMap.put(player.getName(), entries);
//		
//		if (!CustomItemManager.duraTaskScheduled) {
//			TaskMaster.syncTask(new ItemDuraSyncTask());
//		}
	}




//	@SuppressWarnings("deprecation")
//	@Override
//	public void run() {
//		for (String playerName : playersToUpdateInventory.keySet()) {
//			try {
//				Player player = CivGlobal.getPlayer(playerName);
//				player.updateInventory();
//			} catch (CivException e) {
//				e.printStackTrace();
//			}
//		}
//		
//	}
	

}
