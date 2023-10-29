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
package com.avrgaming.civcraft.config;

import com.avrgaming.civcraft.main.CivLog;
import com.google.common.collect.ImmutableList;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.ArrayList;
import java.util.Map;

/**
 * @param level   Current level number
 * @param amount  Number of redstone this mine consumes
 * @param count   Number of times that consumes must be met to level up
 * @param hammers hammers generated each time hour
 */
public record ConfigMineLevel(int level, int amount, int count, double hammers) {

    public static ImmutableList<ConfigMineLevel> loadConfig(MemoryConfiguration cfg) {
        ArrayList<ConfigMineLevel> list = new ArrayList<>();
        for (Map<?, ?> level : cfg.getMapList("mine_levels")) {
            ConfigMineLevel mine_level = new ConfigMineLevel(
                    list.size(),
                    (Integer) level.get("amount"),
                    (Integer) level.get("count"),
                    (Double) level.get("hammers")
            );
            list.add(mine_level.level, mine_level);
        }
        CivLog.debug("Loaded " + list.size() + " mine levels.");
        return ImmutableList.copyOf(list);
    }
}