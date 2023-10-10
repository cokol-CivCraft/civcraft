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

import com.avrgaming.civcraft.cache.PlayerLocationCache;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMission;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.units.Unit;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.ScoutShip;
import com.avrgaming.civcraft.structure.ScoutTower;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class EspionageMissionTask implements Runnable {

    private final ConfigMission mission;
    private final String playerName;
    private final Town target;
    private int secondsLeft;


    public EspionageMissionTask(ConfigMission mission, String playerName, Town target, int seconds) {
        this.mission = mission;
        this.playerName = playerName;
        this.target = target;
        this.secondsLeft = seconds;
    }

    @Override
    public void run() {
        int exposePerSecond = CivSettings.espionageConfig.getInt("espionage.exposure_per_second", 1);
        int exposePerPlayer = CivSettings.espionageConfig.getInt("espionage.exposure_per_player", 1);
        int exposePerScout = CivSettings.espionageConfig.getInt("espionage.exposure_per_scout", 3);

        Player player;
        try {
            player = CivGlobal.getPlayer(playerName);
        } catch (CivException e) {
            return;
        }
        Resident resident = CivGlobal.getResident(player);
        CivMessage.send(player, String.valueOf(ChatColor.GREEN) + ChatColor.BOLD + CivSettings.localize.localizedString("espionage_missionStarted"));

        while (secondsLeft > 0) {

            secondsLeft--;

            /* Add base exposure. */
            resident.setPerformingMission(true);
            resident.setSpyExposure(resident.getSpyExposure() + exposePerSecond);

            /* Add players nearby exposure */
            //PlayerLocationCache.lock.lock();
            int playerCount = PlayerLocationCache.getNearbyPlayers(new BlockCoord(player.getLocation()), 600).size();
            playerCount--;
            resident.setSpyExposure(resident.getSpyExposure() + (playerCount * exposePerPlayer));

            /* Add scout tower exposure */
            double range = CivSettings.warConfig.getDouble("scout_tower.range", 400.0);

            BlockCoord bcoord = new BlockCoord(player.getLocation());

            int amount = 0;
            for (Structure struct : target.getStructures()) {
                if (!struct.isActive()) {
                    continue;
                }

                if (struct instanceof ScoutTower || struct instanceof ScoutShip) {
                    if (bcoord.distance(struct.getCenterLocation()) < range) {
                        amount += exposePerScout;
                    }
                }
            }
            resident.setSpyExposure(resident.getSpyExposure() + amount);

            /* Process exposure penalities */
            if (target.processSpyExposure(resident)) {
                CivMessage.global(ChatColor.YELLOW + CivSettings.localize.localizedString("var_espionage_missionFailedAlert", (ChatColor.WHITE + player.getName()), mission.name, target.getName()));
                CivMessage.send(player, ChatColor.RED + CivSettings.localize.localizedString("espionage_missionFailed"));
                Unit.removeUnit(player);
                resident.setPerformingMission(false);
                return;
            }

            if ((secondsLeft % 15) == 0) {
                CivMessage.send(player, String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.localize.localizedString("var_espionage_secondsRemain", secondsLeft));
            } else if (secondsLeft < 15) {
                CivMessage.send(player, String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.localize.localizedString("var_espionage_secondsRemain", secondsLeft));
            }

            ChunkCoord coord = new ChunkCoord(player.getLocation());
            CultureChunk cc = CivGlobal.getCultureChunk(coord);

            if (cc == null || cc.getCiv() != target.getCiv()) {
                CivMessage.sendError(player, CivSettings.localize.localizedString("espionage_missionAborted"));
                return;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            }
        }

        resident.setPerformingMission(false);
        TaskMaster.syncTask(new PerformMissionTask(mission, playerName));
    }

}
