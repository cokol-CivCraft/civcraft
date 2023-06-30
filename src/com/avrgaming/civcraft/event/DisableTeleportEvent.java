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
package com.avrgaming.civcraft.event;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;

import org.bukkit.Bukkit;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.war.War;

public class DisableTeleportEvent implements EventInterface {

	@Override
	public void process() {
		CivLog.info("TimerEvent: DisableTeleportEvent -------------------------------------");

		try {
			disableTeleport();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Calendar getNextDate() throws InvalidConfiguration {
		Calendar cal = EventTimer.getCalendarInServerTimeZone();
		
		int dayOfWeek = CivSettings.getInteger(CivSettings.warConfig, "war.disable_tp_time_day");
		int hourBeforeWar = CivSettings.getInteger(CivSettings.warConfig, "war.disable_tp_time_hour");

		cal.set(Calendar.DAY_OF_WEEK, dayOfWeek);
		cal.set(Calendar.HOUR_OF_DAY, hourBeforeWar);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		
		Calendar now = Calendar.getInstance();
		if (now.after(cal)) {
			cal.add(Calendar.WEEK_OF_MONTH, 1);
			cal.set(Calendar.DAY_OF_WEEK, dayOfWeek);
			cal.set(Calendar.HOUR_OF_DAY, hourBeforeWar);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
		}
		
		return cal;
	}
	

	public static void disableTeleport() throws IOException {
		if (War.hasWars())
		{
		File file = new File(CivSettings.plugin.getDataFolder().getPath()+"/data/teleportsOff.txt");
		if (!file.exists()) {
			CivLog.warning("Configuration file: teleportsOff.txt was missing. Streaming to disk from Jar.");
			CivSettings.streamResourceToDisk("/data/teleportsOff.txt");
		}
		
		CivLog.info("Loading Configuration file: teleportsOff.txt");
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			
			String line;
			try {
				CivMessage.globalHeading(CivColor.BOLD+CivSettings.localize.localizedString(CivSettings.localize.localizedString("warteleportDisable")));
				while ((line = br.readLine()) != null) {
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), line);
				}
		
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		}
	}
	

	
	public static void enableTeleport() throws IOException {
		
		File file = new File(CivSettings.plugin.getDataFolder().getPath()+"/data/teleportsOn.txt");
		if (!file.exists()) {
			CivLog.warning("Configuration file: teleportsOn.txt was missing. Streaming to disk from Jar.");
			CivSettings.streamResourceToDisk("/data/teleportsOn.txt");
		}
		
		CivLog.info("Loading Configuration file: teleportsOn.txt");
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			
			String line;
			try {

				CivMessage.globalHeading(CivColor.BOLD+CivSettings.localize.localizedString("warteleportEnable"));
				while ((line = br.readLine()) != null) {
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), line);
				}
		
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
	}


}
