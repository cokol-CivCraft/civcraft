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

import com.avrgaming.civcraft.components.ConsumeLevelComponent.Result;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMineLevel;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.CivTaskAbortException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.StructureChest;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.util.MultiInventory;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class Mine extends Structure {

    private int level = 1;
    private int xp = 0;

    protected Mine(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    public Mine(UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(uuid, nbt);
    }

    public Result consume(CivAsyncTask task) throws InterruptedException {
        ConfigMineLevel current = CivSettings.mineLevels.get(this.getLevel());
        if (CivSettings.mineLevels.get(this.getLevel() + 1) == null) {
            return Result.MAXED;
        }

        //Look for the mine's chest.
        if (this.getChests().isEmpty())
            return Result.STAGNATE;

        MultiInventory multiInv = new MultiInventory();

        // Make sure the chest is loaded and add it to the multi inv.
        for (StructureChest chest : this.getAllChestsById(0)) {
            task.syncLoadChunk(chest.getCoord().getWorldname(), chest.getCoord().getX(), chest.getCoord().getZ());
            Inventory tmp;
            try {
                tmp = task.getChestInventory(chest.getCoord().getWorldname(), chest.getCoord().getX(), chest.getCoord().getY(), chest.getCoord().getZ(), true);
            } catch (CivTaskAbortException e) {
                return Result.STAGNATE;
            }
            multiInv.addInventory(tmp);
        }
        try {
            if (!multiInv.removeItem(Material.REDSTONE, current.amount())) {
                return Result.STAGNATE;
            }
        } catch (CivException e) {
            e.printStackTrace();
            return Result.STAGNATE;
        }
        xp += 1;
        if (xp >= getMaxXp()) {
            xp = 0;
            level += 1;
            return Result.LEVELUP;
        }
        return Result.GROW;
    }

    public void process_mine(CivAsyncTask task) {
        Result result;
        try {
            result = this.consume(task);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }
        switch (result) {
            case STARVE ->
                    CivMessage.sendTown(getTown(), ChatColor.RED + CivSettings.localize.localizedString("var_mine_productionFell", getLevel(), ChatColor.GREEN + getXpString()));
            case LEVELDOWN ->
                    CivMessage.sendTown(getTown(), ChatColor.RED + CivSettings.localize.localizedString("var_mine_lostalvl", getLevel()));
            case STAGNATE ->
                    CivMessage.sendTown(getTown(), ChatColor.RED + CivSettings.localize.localizedString("var_mine_stagnated", getLevel(), ChatColor.GREEN + getXpString()));
            case GROW ->
                    CivMessage.sendTown(getTown(), ChatColor.GREEN + CivSettings.localize.localizedString("var_mine_productionGrew", getLevel(), getXpString()));
            case LEVELUP ->
                    CivMessage.sendTown(getTown(), ChatColor.GREEN + CivSettings.localize.localizedString("var_mine_lvlUp", getLevel()));
            case MAXED ->
                    CivMessage.sendTown(getTown(), ChatColor.GREEN + CivSettings.localize.localizedString("var_mine_maxed", getLevel(), ChatColor.GREEN + getXpString()));
            default -> {
            }
        }
    }

    public String getXpString() {
        int currentCountMax = CivSettings.mineLevels.get(getLevel()).count();
        return "(" + this.getXp() + "/" + (Optional.of(currentCountMax).map(countMax -> countMax + ")").orElse("?)"));
    }

    public double getBonusHammers() {
        if (!this.isComplete()) {
            return 0.0;
        }

        return CivSettings.mineLevels.get(getLevel()).hammers();
    }

    public int getLevel() {
        return level;
    }

    public int getXp() {
        return xp;
    }

    public int getMaxXp() {
        return CivSettings.mineLevels.get(getLevel()).count();
    }
}
