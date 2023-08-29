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

import com.avrgaming.civcraft.components.ProjectileArrowComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.*;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.war.War;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class Capitol extends TownHall {

    private final HashMap<Integer, ProjectileArrowComponent> arrowTowers = new HashMap<>();
    private StructureSign respawnSign;
    private int index = 0;

    public Capitol(ResultSet rs) throws SQLException, CivException {
        super(rs);
    }


    protected Capitol(Location center, String id, Town town)
            throws CivException {
        super(center, id, town);
    }

    private RespawnLocationHolder getSelectedHolder() {
        ArrayList<RespawnLocationHolder> respawnables = this.getTown().getCiv().getAvailableRespawnables();
        return respawnables.get(index);
    }

    private void changeIndex(int newIndex) {
        ArrayList<RespawnLocationHolder> respawnables = this.getTown().getCiv().getAvailableRespawnables();

        if (this.respawnSign == null) {
            CivLog.warning("Could not find civ spawn sign:" + this.getId() + " at " + this.getCorner());
            return;
        }
        try {
            this.respawnSign.setText(CivSettings.localize.localizedString("capitol_sign_respawnAt") + "\n" + ChatColor.DARK_GREEN + ChatColor.BOLD + respawnables.get(newIndex).getRespawnName());
            index = newIndex;
        } catch (IndexOutOfBoundsException e) {
            if (!respawnables.isEmpty()) {
                this.respawnSign.setText(CivSettings.localize.localizedString("capitol_sign_respawnAt") + "\n" + ChatColor.DARK_GREEN + ChatColor.BOLD + respawnables.get(0).getRespawnName());
                index = 0;
            }
            //this.unitNameSign.setText(getUnitSignText(index));
        }
        this.respawnSign.update();

    }

    public StructureSign getRespawnSign() {
        return respawnSign;
    }

    @Override
    public void processSignAction(Player player, StructureSign sign, PlayerInteractEvent event) {
        //int special_id = Integer.valueOf(sign.getAction());
        Resident resident = CivGlobal.getResident(player);
        if (resident == null) {
            return;
        }
        if (!War.isWarTime() && sign.getAction().equalsIgnoreCase("respawn")) {
            player.teleport(this.getRandomRevivePoint().getLocation());
            CivMessage.sendSuccess(player, "capitol_leave_warroom");
        }

        if (!War.isWarTime()) {
            return;
        }
        boolean hasPermission = (resident.getTown().isMayor(resident)) || (resident.getTown().getAssistantGroup().hasMember(resident)) || (resident.getCiv().getLeaderGroup().hasMember(resident)) || (resident.getCiv().getAdviserGroup().hasMember(resident));

        switch (sign.getAction()) {
            case "prev" -> {
                if (hasPermission) {
                    changeIndex((index - 1));
                } else {
                    CivMessage.sendError(resident, CivSettings.localize.localizedString("capitol_Sign_noPermission"));
                }
            }
            case "next" -> {
                if (hasPermission) {
                    changeIndex((index + 1));
                } else {
                    CivMessage.sendError(resident, CivSettings.localize.localizedString("capitol_Sign_noPermission"));
                }
            }
            case "respawn" -> {
                ArrayList<RespawnLocationHolder> respawnables = this.getTown().getCiv().getAvailableRespawnables();
                if (index >= respawnables.size()) {
                    index = 0;
                    changeIndex(index);
                    CivMessage.sendError(resident, CivSettings.localize.localizedString("capitol_cannotRespawn"));
                    return;
                }
                RespawnLocationHolder holder = getSelectedHolder();

                int respawnTimeSeconds = this.getRespawnTime();
                Date now = new Date();
                if (resident.getLastKilledTime() != null) {
                    long secondsLeft = (resident.getLastKilledTime().getTime() + (respawnTimeSeconds * 1000L)) - now.getTime();
                    if (secondsLeft > 0) {
                        secondsLeft /= 1000;
                        CivMessage.sendError(resident, ChatColor.RED + CivSettings.localize.localizedString("var_capitol_secondsLeftTillRespawn", secondsLeft));
                        return;
                    }
                }
                BlockCoord revive = holder.getRandomRevivePoint();
                Location loc;
                if (revive == null) {
                    loc = player.getBedSpawnLocation();
                } else {
                    loc = revive.getLocation();
                }
                CivMessage.send(player, ChatColor.GREEN + CivSettings.localize.localizedString("capitol_respawningAlert"));
                player.teleport(loc);
            }
        }
    }


    @Override
    public void onPostBuild(BlockCoord absCoord, SimpleBlock commandBlock) {
        StructureSign structSign;

        switch (commandBlock.command) {
            case "/towerfire" -> {
                Integer towerID = Integer.valueOf(commandBlock.keyvalues.get("id"));
                if (!arrowTowers.containsKey(towerID)) {

                    ProjectileArrowComponent arrowTower = new ProjectileArrowComponent(this, absCoord.getLocation());
                    arrowTower.createComponent(this);
                    arrowTower.setTurretLocation(absCoord);

                    arrowTowers.put(towerID, arrowTower);
                }
            }
            case "/next" -> {
                commandBlock.setTo(absCoord);
                structSign = new StructureSign(absCoord, this);
                structSign.setText("\n" + ChatColor.BOLD + ChatColor.UNDERLINE + CivSettings.localize.localizedString("capitol_sign_nextLocation"));
                structSign.setDirection(commandBlock.getData());
                structSign.setAction("next");
                structSign.update();
                this.addStructureSign(structSign);
                CivGlobal.addStructureSign(structSign);
            }
            case "/prev" -> {
                commandBlock.setTo(absCoord);
                structSign = new StructureSign(absCoord, this);
                structSign.setText("\n" + ChatColor.BOLD + ChatColor.UNDERLINE + CivSettings.localize.localizedString("capitol_sign_previousLocation"));
                structSign.setDirection(commandBlock.getData());
                structSign.setAction("prev");
                structSign.update();
                this.addStructureSign(structSign);
                CivGlobal.addStructureSign(structSign);
            }
            case "/respawndata" -> {
                commandBlock.setTo(absCoord);
                structSign = new StructureSign(absCoord, this);
                structSign.setText(CivSettings.localize.localizedString("capitol_sign_Capitol"));
                structSign.setDirection(commandBlock.getData());
                structSign.setAction("respawn");
                structSign.update();
                this.addStructureSign(structSign);
                CivGlobal.addStructureSign(structSign);
                this.respawnSign = structSign;
                changeIndex(index);
            }
        }

    }

    @Override
    public void createControlPoint(BlockCoord absCoord) {

        Block centerBlock = absCoord.getBlock();

        /* Build the bedrock tower. */
        //for (int i = 0; i < 1; i++) {
        BlockState state = centerBlock.getState();
        state.setType(Material.BEDROCK);
        state.update(true, false);

        StructureBlock sb = new StructureBlock(new BlockCoord(centerBlock), this);
        this.addStructureBlock(sb.getCoord(), true);
        //}

        /* Build the control block. */
        BlockState state2 = centerBlock.getRelative(0, 1, 0).getState();
        state2.setType(Material.OBSIDIAN);
        state2.update(true, false);

        this.addStructureBlock(new BlockCoord(centerBlock.getRelative(0, 1, 0)), true);

        int capitolControlHitpoints;
        try {
            capitolControlHitpoints = CivSettings.getInteger(CivSettings.warConfig, "war.control_block_hitpoints_capitol");
        } catch (InvalidConfiguration e) {
            e.printStackTrace();
            capitolControlHitpoints = 100;
        }

        BlockCoord coord = new BlockCoord(centerBlock.getRelative(0, 1, 0));
        this.controlPoints.put(coord, new ControlPoint(coord, this, capitolControlHitpoints));
    }

    @Override
    public void onInvalidPunish() {
        int invalid_respawn_penalty;
        try {
            invalid_respawn_penalty = CivSettings.getInteger(CivSettings.warConfig, "war.invalid_respawn_penalty");
        } catch (InvalidConfiguration e) {
            e.printStackTrace();
            return;
        }

        CivMessage.sendTown(this.getTown(), String.valueOf(ChatColor.RED) + ChatColor.BOLD + CivSettings.localize.localizedString("capitol_cannotSupport1") +
                " " + CivSettings.localize.localizedString("var_capitol_cannotSupport2", invalid_respawn_penalty));
    }

    @Override
    public boolean isValid() {
        if (this.getCiv().isAdminCiv()) {
            return true;
        }

        /*
         * Validate that all of the towns in our civ have town halls. If not, then
         * we need to punish by increasing respawn times.
         */
        for (Town town : this.getCiv().getTowns()) {
            if (town.getTownHall() == null) {
                return false;
            }
        }

        return super.isValid();
    }

    @Override
    public String getRespawnName() {
        return "Capitol\n" + this.getTown().getName();
    }
}
