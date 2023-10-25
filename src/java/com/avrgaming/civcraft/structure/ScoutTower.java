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
package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.cache.PlayerLocationCache;
import com.avrgaming.civcraft.components.PlayerProximityComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Relation;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.UUID;

public class ScoutTower extends Structure {

    double range;
    private PlayerProximityComponent proximityComponent;

    private int reportSeconds = 60;
    private int count = 0;

    public ScoutTower(int id, UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(id, uuid, nbt);
    }

    protected ScoutTower(Location center, String id, Town town) throws CivException {
        super(center, id, town);
        this.hitpoints = this.getMaxHitPoints();
    }

    @Override
    public void loadSettings() {
        super.loadSettings();

        range = CivSettings.warConfig.getDouble("scout_tower.range", 400.0);
        proximityComponent = new PlayerProximityComponent();
        proximityComponent.createComponent(this);

        proximityComponent.setBuildable(this);
        proximityComponent.setCenter(this.getCenterLocation());
        proximityComponent.setRadius(range);

        reportSeconds = (int) CivSettings.warConfig.getDouble("scout_tower.update", 120);
    }

    private void scoutDebug(String str) {
        if (this.getCiv().scoutDebug && this.getCiv().scoutDebugPlayer != null) {
            Player player;
            try {
                player = CivGlobal.getPlayer(this.getCiv().scoutDebugPlayer);
            } catch (CivException e) {
                return;
            }
            CivMessage.send(player, ChatColor.YELLOW + "[ScoutDebug] " + str);
        }
    }

    @Override
    public int getMaxHitPoints() {
        double rate = 1;
            rate += this.getTown().getBuffManager().getEffectiveDouble("buff_chichen_itza_tower_hp");
            rate += this.getTown().getBuffManager().getEffectiveDouble(Buff.BARRICADE);
        return (int) (info.max_hp * rate);
    }

    /*
     * Asynchronously sweeps for players within the scout tower's radius. If
     * it finds a player that is not in the civ, then it informs the town.
     * If the town is the capitol, it informs the civ.
     */
    public void process(HashSet<String> alreadyAnnounced) {
        count++;
        if (count < reportSeconds) {
            return;
        }

        count = 0;
        boolean empty = true;

        for (PlayerLocationCache pc : proximityComponent.tryGetNearbyPlayers(true)) {
            empty = false;
            scoutDebug(CivSettings.localize.localizedString("scoutTower_debug_inspectingPlayer") + pc.getName());
            Player player;
            try {
                player = CivGlobal.getPlayer(pc.getName());
            } catch (CivException e) {
                scoutDebug(CivSettings.localize.localizedString("scoutTower_debug_notOnline"));
                return;
            }

            if (player.isOp() || player.getGameMode() != GameMode.SURVIVAL) {
                scoutDebug(CivSettings.localize.localizedString("scoutTower_debug_isOP"));
                continue;
            }

            Location center = this.getCenterLocation().getLocation();

            /* Do not re-announce players announced by other scout towers */
            if (alreadyAnnounced.contains(this.getCiv().getName() + ":" + player.getName())) {
                scoutDebug(CivSettings.localize.localizedString("scoutTower_debug_alreadyAnnounced") + pc.getName());
                continue;
            }

            /* Always announce outlaws, so skip down to bottom. */
            String relationName = "";
            String relationColor = "";
            if (!this.getTown().isOutlaw(player.getName())) {
                /* do not announce residents in this civ */
                Resident resident = CivGlobal.getResident(player);
                if (resident != null && resident.hasTown() && resident.getCiv() == this.getCiv()) {
                    scoutDebug(CivSettings.localize.localizedString("scoutTower_debug_sameCiv"));
                    continue;
                }

                /* Only announce hostile, war, and neutral players */
                Relation.Status relation = this.getCiv().getDiplomacyManager().getRelationStatus(player);
                switch (relation) {
                    case PEACE, ALLY -> {
//				case VASSAL:
//				case MASTER:
                        scoutDebug(CivSettings.localize.localizedString("acoutTower_debug_ally"));
                        continue;
                    }
                    default -> {
                    }
                }

                relationName = relation.name();
                relationColor = Relation.getRelationColor(relation);
            } else {
                relationName = CivSettings.localize.localizedString("scoutTower_isOutlaw");
                relationColor = String.valueOf(ChatColor.YELLOW);
            }


            if (center.getWorld() != this.getCorner().getLocation().getWorld()) {
                scoutDebug(CivSettings.localize.localizedString("scoutTower_debug_wrongWorld"));
                continue;
            }

            if (center.distance(player.getLocation()) < range) {
                /* Notify the town or civ. */
                CivMessage.sendScout(this.getCiv(), CivSettings.localize.localizedString("var_scoutTower_detection",
                        (relationColor + player.getName() + "(" + relationName + ")" + ChatColor.WHITE), (player.getLocation().getBlockX() + "," + player.getLocation().getBlockY() + "," + player.getLocation().getBlockZ()),
                        this.getTown().getName()));
                alreadyAnnounced.add(this.getCiv().getName() + ":" + player.getName());
                player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.11f, 1.11f);
                CivMessage.sendActionBar(player, CivSettings.localize.localizedString("var_scoutTower_you_detected", String.valueOf(ChatColor.GOLD) + ChatColor.RED + ChatColor.GREEN + this.getTown().getName() + ChatColor.RED));


            }
        }

        if (empty) {
            scoutDebug(CivSettings.localize.localizedString("scoutTower_debug_emptyCache"));
        }
    }

    public int getReportSeconds() {
        return reportSeconds;
    }

    public void setReportSeconds(int reportSeconds) {
        this.reportSeconds = reportSeconds;
    }
}
