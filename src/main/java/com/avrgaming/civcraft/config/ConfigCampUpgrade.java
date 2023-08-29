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

import com.avrgaming.civcraft.camp.Camp;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

public record ConfigCampUpgrade(String id, String name, double cost, String action, String require_upgrade) {

    public static void loadConfig(FileConfiguration cfg, Map<String, ConfigCampUpgrade> upgrades) {
        upgrades.clear();
        for (Map<?, ?> level : cfg.getMapList("upgrades")) {
            ConfigCampUpgrade upgrade = new ConfigCampUpgrade(
                    (String) level.get("id"),
                    (String) level.get("name"),
                    (Double) level.get("cost"),
                    (String) level.get("action"),
                    (String) level.get("require_upgrade")
            );
            upgrades.put(upgrade.id, upgrade);
        }
        CivLog.info("Loaded " + upgrades.size() + " camp upgrades.");
    }

    public boolean isAvailable(Camp camp) {
        if (camp.hasUpgrade(this.id)) {
            return false;
        }

        if (this.require_upgrade == null || this.require_upgrade.isEmpty()) {
            return true;
        }

        return camp.hasUpgrade(this.require_upgrade);
    }

    public void processAction(Camp camp) {

        if (this.action == null) {
            CivLog.warning("No action found for upgrade:" + this.id);
            return;
        }

        switch (this.action.toLowerCase()) {
            case "enable_sifter" -> {
                camp.setSifterEnabled(true);
                CivMessage.sendCamp(camp, CivSettings.localize.localizedString("camp_upgrade_Sifter"));
            }
            case "enable_longhouse" -> {
                camp.setLonghouseEnabled(true);
                CivMessage.sendCamp(camp, CivSettings.localize.localizedString("camp_upgrade_longhouse"));
            }
            case "enable_garden" -> {
                camp.setGardenEnabled(true);
                CivMessage.sendCamp(camp, CivSettings.localize.localizedString("camp_upgrade_garden"));
            }
            default ->
                    CivLog.warning(CivSettings.localize.localizedString("var_camp_upgrade_unknown", this.action, this.id));
        }
    }

}
