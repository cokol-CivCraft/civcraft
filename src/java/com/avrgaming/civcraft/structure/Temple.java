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

import com.avrgaming.civcraft.components.ConsumeLevelComponent;
import com.avrgaming.civcraft.components.ConsumeLevelComponent.Result;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigTempleLevel;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.CivTaskAbortException;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.StructureChest;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.util.MultiInventory;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;

import java.sql.SQLException;
import java.util.UUID;

public class Temple extends Structure {

    private ConsumeLevelComponent consumeComp = null;

    protected Temple(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    public Temple(UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(uuid, nbt);
    }

    public ConsumeLevelComponent getConsumeComponent() {
        if (consumeComp == null) {
            consumeComp = (ConsumeLevelComponent) this.getComponent(ConsumeLevelComponent.class.getSimpleName());
        }
        return consumeComp;
    }

    @Override
    public void loadSettings() {
        super.loadSettings();

    }

    public Result consume(CivAsyncTask task) throws InterruptedException {

        //Look for the temple's chest.
        if (this.getChests().isEmpty())
            return Result.STAGNATE;

        MultiInventory multiInv = new MultiInventory();

        // Make sure the chest is loaded and add it to the multi inv.
        for (StructureChest chest : this.getAllChestsById(1)) {
            task.syncLoadChunk(chest.getCoord().getWorldname(), chest.getCoord().getX(), chest.getCoord().getZ());
            Inventory tmp;
            try {
                tmp = task.getChestInventory(chest.getCoord().getWorldname(), chest.getCoord().getX(), chest.getCoord().getY(), chest.getCoord().getZ(), true);
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

    public void templeCulture(CivAsyncTask task) throws InterruptedException {
        Result result = this.consume(task);
        switch (result) {
            case STARVE -> {
                CivMessage.sendTown(getTown(), ChatColor.RED + CivSettings.localize.localizedString("var_temple_productionFell", getConsumeComponent().getLevel(), getConsumeComponent().getCountString()));
                return;
            }
            case LEVELDOWN -> {
                CivMessage.sendTown(getTown(), ChatColor.RED + CivSettings.localize.localizedString("var_temple_lostalvl", getConsumeComponent().getLevel()));
                return;
            }
            case STAGNATE -> {
                CivMessage.sendTown(getTown(), ChatColor.RED + CivSettings.localize.localizedString("var_temple_stagnated", getConsumeComponent().getLevel(), getConsumeComponent().getCountString()));
                return;
            }
            case GROW ->
                    CivMessage.sendTown(getTown(), ChatColor.GREEN + CivSettings.localize.localizedString("var_temple_productionGrew", getConsumeComponent().getLevel(), getConsumeComponent().getCountString()));
            case LEVELUP ->
                    CivMessage.sendTown(getTown(), ChatColor.GREEN + CivSettings.localize.localizedString("var_temple_lvlUp", getConsumeComponent().getLevel()));
            case MAXED ->
                    CivMessage.sendTown(getTown(), ChatColor.GREEN + CivSettings.localize.localizedString("var_temple_maxed", getConsumeComponent().getLevel(), getConsumeComponent().getCountString()));
            case UNKNOWN -> {
                CivMessage.sendTown(getTown(), ChatColor.BLUE + CivSettings.localize.localizedString("temple_unknown"));
                return;
            }
            default -> {
            }
        }

        ConfigTempleLevel lvl;
        if (result == Result.LEVELUP) {
            lvl = CivSettings.templeLevels.get(getConsumeComponent().getLevel() - 1);
        } else {
            lvl = CivSettings.templeLevels.get(getConsumeComponent().getLevel());
        }

        int total_culture = (int) Math.round(lvl.culture * this.getTown().getCottageRate());
//		if (this.getTown().getBuffManager().hasBuff("buff_pyramid_cottage_bonus")) {
//			total_coins *= this.getTown().getBuffManager().getEffectiveDouble("buff_pyramid_cottage_bonus");
//		}
        this.getTown().addAccumulatedCulture(total_culture);

        CivMessage.sendTown(getTown(), ChatColor.GREEN + CivSettings.localize.localizedString("var_temple_cultureGenerated", (String.valueOf(ChatColor.LIGHT_PURPLE) + total_culture + ChatColor.GREEN)));

    }

    public int getLevel() {
        return this.getConsumeComponent().getLevel();
    }

    public int getCount() {
        return this.getConsumeComponent().getCount();
    }

    public int getMaxCount() {
        int level = getLevel();

        ConfigTempleLevel lvl = CivSettings.templeLevels.get(level);
        return lvl.count;
    }

    public Result getLastResult() {
        return this.getConsumeComponent().getLastResult();
    }

    public double getCultureGenerated() {
        int level = getLevel();

        ConfigTempleLevel lvl = CivSettings.templeLevels.get(level);
        if (lvl == null) {
            return 0;
        }
        return lvl.culture;
    }

    public void delevel() {
        if (getLevel() > 1) {
            getConsumeComponent().setLevel(getLevel() - 1);
            getConsumeComponent().setCount(0);
            getConsumeComponent().onSave();
        }
    }

    @Override
    public void delete() throws SQLException {
        super.delete();
        if (getConsumeComponent() != null) {
            getConsumeComponent().onDelete();
        }
    }

    public void onDestroy() {
        super.onDestroy();

        getConsumeComponent().setLevel(1);
        getConsumeComponent().setCount(0);
        getConsumeComponent().onSave();
    }


}
