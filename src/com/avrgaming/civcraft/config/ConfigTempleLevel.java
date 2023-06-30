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
package com.avrgaming.civcraft.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;

public class ConfigTempleLevel {
	public int level;			/* Current level number */
	public Map<Integer, Integer> consumes; /* A map of block ID's and amounts required for this level to progress */
	public int count; /* Number of times that consumes must be met to level up */
	public double culture; /* Culture generated each time for the temple */
	
	public ConfigTempleLevel() {
		
	}
	
	public ConfigTempleLevel(ConfigTempleLevel currentlvl) {
		this.level = currentlvl.level;
		this.count = currentlvl.count;
		this.culture = currentlvl.culture;
		
		this.consumes = new HashMap<Integer, Integer>();
		for (Entry<Integer, Integer> entry : currentlvl.consumes.entrySet()) {
			this.consumes.put(entry.getKey(), entry.getValue());
		}
		
	}


	public static void loadConfig(FileConfiguration cfg, Map<Integer, ConfigTempleLevel> temple_levels) {
		temple_levels.clear();
		List<Map<?, ?>> temple_list = cfg.getMapList("temple_levels");
		Map<Integer, Integer> consumes_list = null;
		for (Map<?,?> cl : temple_list ) {
			List<?> consumes = (List<?>)cl.get("consumes");
			if (consumes != null) {
				consumes_list = new HashMap<Integer, Integer>();
				for (int i = 0; i < consumes.size(); i++) {
					String line = (String) consumes.get(i);
					String split[];
					split = line.split(",");
					consumes_list.put(Integer.valueOf(split[0]), Integer.valueOf(split[1]));
				}
			}
			
			
			ConfigTempleLevel templelevel = new ConfigTempleLevel();
			templelevel.level = (Integer)cl.get("level");
			templelevel.consumes = consumes_list;
			templelevel.count = (Integer)cl.get("count");
			templelevel.culture = (Double)cl.get("culture");
			
			temple_levels.put(templelevel.level, templelevel);
			
		}
		CivLog.info("Loaded "+temple_levels.size()+" temple levels.");		
	}
	
}
