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
package com.avrgaming.civcraft.event;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.AttrSource;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.*;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.util.BlockCoord;
import org.bukkit.ChatColor;

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

public class EffectEventTimer extends CivAsyncTask {

	//public static Boolean running = false;

	public static ReentrantLock runningLock = new ReentrantLock();

	public EffectEventTimer() {
	}

	private void processTick() {
		/* Clear the last taxes so they don't accumulate. */
		for (Civilization civ : CivGlobal.getCivs()) {
			civ.lastTaxesPaidMap.clear();
		}

		//HashMap<Town, Integer> cultureGenerated = new HashMap<Town, Integer>();

		// Loop through each structure, if it has an update function call it in another async process
		Iterator<Entry<BlockCoord, Structure>> iter = CivGlobal.getStructureIterator();

		while (iter.hasNext()) {
			Structure struct = iter.next().getValue();
			TownHall townhall = struct.getTown().getTownHall();

			if (townhall == null) {
				continue;
			}

			if (!struct.isActive())
				continue;

			struct.onEffectEvent();

            if (struct.getEffectEvent() == null || struct.getEffectEvent().isEmpty())
				continue;

			String[] split = struct.getEffectEvent().toLowerCase().split(":");
			switch (split[0]) {
				case "generate_coins" -> {
					if (struct instanceof Cottage cottage) {
                        //cottage.generate_coins(this);
                        cottage.generateCoins(this);
                    }
				}
				case "process_mine" -> {
                    if (struct instanceof Mine mine) {
                        mine.process_mine(this);
                    }
				}
				case "temple_culture" -> {
                    if (struct instanceof Temple temple) {
                        try {
                            temple.templeCulture(this);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
				}
				case "process_trade_ship" -> {
                    if (struct instanceof TradeShip tradeShip) {
                        try {
                            tradeShip.process_trade_ship(this);
						} catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
				}
			}

		}

		/*
		 * Process any hourly attributes for this town.
		 *  - Culture
		 *
		 */
		for (Town town : CivGlobal.getTowns()) {
			double cultureGenerated;

			// highjack this loop to display town hall warning.
			TownHall townhall = town.getTownHall();
			if (townhall == null) {
				CivMessage.sendTown(town, ChatColor.YELLOW + CivSettings.localize.localizedString("effectEvent_noTownHall"));
				continue;
			}

            AttrSource cultureSources = town.getCulture();

            // Get amount generated after culture rate/bonus.
            cultureGenerated = cultureSources.total;
            cultureGenerated = Math.round(cultureGenerated);
            town.addAccumulatedCulture(cultureGenerated);

            // Get from unused beakers.
            DecimalFormat df = new DecimalFormat();
            double unusedBeakers = town.getUnusedBeakers();
            if (town.getBuffManager().hasBuff("wonder_trade_great_library")) {
                town.giveExtraHammers(unusedBeakers * town.getBuffManager().getEffectiveDouble("wonder_trade_great_library"));
            }

			if (unusedBeakers > 0) {
				double cultureFromBeakers = unusedBeakers * CivSettings.cultureConfig.getDouble("beakers_per_culture", 0.1);
				cultureFromBeakers = Math.round(cultureFromBeakers);
				unusedBeakers = Math.round(unusedBeakers);

				if (cultureFromBeakers > 0) {
					CivMessage.sendTown(town, ChatColor.GREEN + CivSettings.localize.localizedString("var_effectEvent_convertBeakers", (ChatColor.LIGHT_PURPLE +
							df.format(unusedBeakers) + ChatColor.GREEN), (ChatColor.LIGHT_PURPLE +
							df.format(cultureFromBeakers) + ChatColor.GREEN)));
					cultureGenerated += cultureFromBeakers;
					town.addAccumulatedCulture(cultureFromBeakers);
					town.setUnusedBeakers(0);
				}
			}
			Granary granary = town.getFreeGranary();
			if (granary != null) {
				granary.spawnResources();
			}

			cultureGenerated = Math.round(cultureGenerated);
			CivMessage.sendTown(town, ChatColor.GREEN + CivSettings.localize.localizedString("var_effectEvent_generatedCulture", (ChatColor.LIGHT_PURPLE.toString() + cultureGenerated + ChatColor.GREEN)));
		}

		/* Checking for expired vassal states. */
		CivGlobal.checkForExpiredRelations();
	}

	@Override
	public void run() {

		if (runningLock.tryLock()) {
			try {
				processTick();
			} finally {
				runningLock.unlock();
			}
		} else {
			CivLog.error("COULDN'T GET LOCK FOR HOURLY TICK. LAST TICK STILL IN PROGRESS?");
		}


	}


}
