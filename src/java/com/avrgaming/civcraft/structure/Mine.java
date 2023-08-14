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

import com.avrgaming.civcraft.components.AttributeBiomeRadiusPerLevel;
import com.avrgaming.civcraft.components.ConsumeLevelComponent;
import com.avrgaming.civcraft.components.ConsumeLevelComponent.Result;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMineLevel;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.CivTaskAbortException;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.StructureChest;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.util.MultiInventory;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;

public class Mine extends Structure {

    private ConsumeLevelComponent consumeComp = null;

    protected Mine(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    public Mine(ResultSet rs) throws SQLException, CivException {
        super(rs);
    }


    public String getkey() {
        return getTown().getName() + "_" + this.getConfigId() + "_" + this.getCorner().toString();
    }

    public ConsumeLevelComponent getConsumeComponent() {
        if (consumeComp == null) {
            consumeComp = (ConsumeLevelComponent) this.getComponent(ConsumeLevelComponent.class.getSimpleName());
        }
        return consumeComp;
    }

    public Result consume(CivAsyncTask task) throws InterruptedException {

        //Look for the mine's chest.
        if (this.getChests().isEmpty())
            return Result.STAGNATE;

        MultiInventory multiInv = new MultiInventory();

        ArrayList<StructureChest> chests = this.getAllChestsById(0);

        // Make sure the chest is loaded and add it to the multi inv.
        for (StructureChest c : chests) {
            task.syncLoadChunk(c.getCoord().getWorldname(), c.getCoord().getX(), c.getCoord().getZ());
            Inventory tmp;
            try {
                tmp = task.getChestInventory(c.getCoord().getWorldname(), c.getCoord().getX(), c.getCoord().getY(), c.getCoord().getZ(), true);
            } catch (CivTaskAbortException e) {
                return Result.STAGNATE;
            }
            multiInv.addInventory(tmp);
        }
        getConsumeComponent().setSource(multiInv);
        getConsumeComponent().setConsumeRate(1.0);
        try {
            Result result = getConsumeComponent().processConsumption();
            getConsumeComponent().onSave();
            return result;
        } catch (IllegalStateException e) {
            CivLog.exception(this.getDisplayName() + " Process Error in town: " + this.getTown().getName() + " and Location: " + this.getCorner(), e);
            return Result.STAGNATE;
        }
    }

    public void process_mine(CivAsyncTask task) {
        Result result = null;
        try {
            result = this.consume(task);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        switch (result) {
            case STARVE ->
                    CivMessage.sendTown(getTown(), ChatColor.RED + CivSettings.localize.localizedString("var_mine_productionFell", getConsumeComponent().getLevel(), ChatColor.GREEN + getConsumeComponent().getCountString()));
            case LEVELDOWN ->
                    CivMessage.sendTown(getTown(), ChatColor.RED + CivSettings.localize.localizedString("var_mine_lostalvl", getConsumeComponent().getLevel()));
            case STAGNATE ->
                    CivMessage.sendTown(getTown(), ChatColor.RED + CivSettings.localize.localizedString("var_mine_stagnated", getConsumeComponent().getLevel(), ChatColor.GREEN + getConsumeComponent().getCountString()));
            case GROW ->
                    CivMessage.sendTown(getTown(), ChatColor.GREEN + CivSettings.localize.localizedString("var_mine_productionGrew", getConsumeComponent().getLevel(), getConsumeComponent().getCountString()));
            case LEVELUP ->
                    CivMessage.sendTown(getTown(), ChatColor.GREEN + CivSettings.localize.localizedString("var_mine_lvlUp", getConsumeComponent().getLevel()));
            case MAXED ->
                    CivMessage.sendTown(getTown(), ChatColor.GREEN + CivSettings.localize.localizedString("var_mine_maxed", getConsumeComponent().getLevel(), ChatColor.GREEN + getConsumeComponent().getCountString()));
            default -> {
            }
        }
    }

    public double getBonusHammers() {
        if (!this.isComplete()) {
            return 0.0;
        }
        int level = getLevel();

        ConfigMineLevel lvl = CivSettings.mineLevels.get(level);
        return lvl.hammers();
    }

    public int getLevel() {
        if (!this.isComplete()) {
            return 1;
        }
        return this.getConsumeComponent().getLevel();
    }

    public double getHammersPerTile() {
        AttributeBiomeRadiusPerLevel attrBiome = (AttributeBiomeRadiusPerLevel) this.getComponent("AttributeBiomeRadiusPerLevel");
        double base = Optional.ofNullable(attrBiome).map(AttributeBiomeRadiusPerLevel::getBaseValue).orElse(1.0);
        double rate = 1 + this.getTown().getBuffManager().getEffectiveDouble(Buff.ADVANCED_TOOLING);
        return rate * base;
    }

    public int getCount() {
        return this.getConsumeComponent().getCount();
    }

    public int getMaxCount() {
        int level = getLevel();

        ConfigMineLevel lvl = CivSettings.mineLevels.get(level);
        return lvl.count();
    }

    public Result getLastResult() {
        return this.getConsumeComponent().getLastResult();
    }

}
