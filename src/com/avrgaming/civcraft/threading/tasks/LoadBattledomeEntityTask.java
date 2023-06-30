package com.avrgaming.civcraft.threading.tasks;

import java.util.Queue;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.wonders.Battledome;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.EntityUtil;
import com.avrgaming.civcraft.util.TimeTools;

public class LoadBattledomeEntityTask implements Runnable {

	public Queue<SessionEntry> entriesToLoad;
	public Battledome battledome;
		
	public LoadBattledomeEntityTask(Queue<SessionEntry> entriesToLoad, Battledome battledome) {
		this.entriesToLoad = entriesToLoad;
		this.battledome = battledome;
	}
	
	@Override
	public void run() {
		int max = battledome.getMobMax();
		
		if (battledome.lock.tryLock()) {
			CivLog.info("Started Battledome Entity Load Task...");
			try {
				for (int i = 0; i < max; i++) {
					SessionEntry entry = entriesToLoad.poll();
					if (entry == null) {
						break;
					}
					
					String[] split = entry.value.split(":");
					Entity entity = EntityUtil.getEntity(Bukkit.getWorld(split[0]), UUID.fromString(split[1]));
					
					if (entity != null) {			
						battledome.entities.add(entity.getUniqueId());
						Battledome.battledomeEntities.put(entity.getUniqueId(), battledome);
					} else {
						CivGlobal.getSessionDB().delete(entry.request_id, entry.key);
					}
				}
			} finally {
				battledome.lock.unlock();
			}
		} else {
			/* try again in 5 seconds. */
			CivLog.warning("Couldn't obtain battledome lock, trying again in 5 seconds.");
			TaskMaster.syncTask(this, TimeTools.toTicks(5));
		}
		
		/* Everything else is beyond our max, lets just forget about them. */
		SessionEntry entry = entriesToLoad.poll();
		while (entry != null) {
			CivGlobal.getSessionDB().delete(entry.request_id, entry.key);
			entry = entriesToLoad.poll();
		}
		CivLog.info("...Finished Battledome Entity Load Task");
	}
}
