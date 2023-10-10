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
import com.avrgaming.civcraft.config.ConfigCottageLevel;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.CivTaskAbortException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.StructureChest;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.wonders.TheHangingGardens;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.util.MultiInventory;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class Cottage extends Structure {

    private ConsumeLevelComponent consumeComp = null;


    protected Cottage(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    public Cottage(ResultSet rs) throws SQLException, CivException {
        super(rs);
    }

	public ConsumeLevelComponent getConsumeComponent() {
		if (consumeComp == null) {
			consumeComp = (ConsumeLevelComponent) this.getComponent(ConsumeLevelComponent.class.getSimpleName());
		}
		return consumeComp;
	}

	/* @Override
	public void loadSettings() {
		super.loadSettings();

    } */

    public String getkey() {
		return this.getTown().getName()+"_"+this.getConfigId()+"_"+this.getCorner().toString();
	}

	/*
	 * Returns true if the granary has been poisoned, false otherwise.
	 */
	public boolean processPoison(MultiInventory inv) {
		//Check to make sure the granary has not been poisoned!
		String key = "posiongranary:"+getTown().getName();
		ArrayList<SessionEntry> entries;
		entries = CivGlobal.getSessionDB().lookup(key);
		int max_poison_ticks = -1;
		for (SessionEntry entry : entries) {
			int next = Integer.parseInt(entry.value);

			if (next > max_poison_ticks) {
				max_poison_ticks = next;
			}
		}

		if (max_poison_ticks > 0) {
			CivGlobal.getSessionDB().delete_all(key);
			max_poison_ticks--;

			if (max_poison_ticks > 0)
                CivGlobal.getSessionDB().add(key, String.valueOf(max_poison_ticks), this.getTown().getCiv().getId(), this.getTown().getId(), this.getId());

			// Add some rotten flesh to the chest lol
            CivMessage.sendTown(this.getTown(), ChatColor.RED + CivSettings.localize.localizedString("cottage_poisoned"));
            inv.addItemStack(new ItemStack(Material.ROTTEN_FLESH, 4, (short) 0));
			return true;
		}
		return false;
	}

    public void generateCoins(CivAsyncTask task) {

        if (!this.isActive()) {
            return;
        }

        /* Build a multi-inv from granaries. */
        MultiInventory multiInv = new MultiInventory();
        if (this.getCiv().hasWonder("w_hanginggardens")) {
            TheHangingGardens thg = (TheHangingGardens) this.getCiv().getWonder("w_hanginggardens");
            try {
                for (StructureChest c : thg.getAllChestsById(1)) {
                    task.syncLoadChunk(c.getCoord().getWorldname(), c.getCoord().getX(), c.getCoord().getZ());
                    Inventory tmp;
                    try {
                        tmp = task.getChestInventory(c.getCoord().getWorldname(), c.getCoord().getX(), c.getCoord().getY(), c.getCoord().getZ(), true);
                        multiInv.addInventory(tmp);
                    } catch (CivTaskAbortException e) {
                        e.printStackTrace();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (Structure struct : this.getTown().getStructures()) {
            if (struct instanceof Granary) {

                // Make sure the chunk is loaded and add it to the inventory.
                try {
                    for (StructureChest c : struct.getAllChestsById(1)) {
                        task.syncLoadChunk(c.getCoord().getWorldname(), c.getCoord().getX(), c.getCoord().getZ());
                        Inventory tmp;
                        try {
                            tmp = task.getChestInventory(c.getCoord().getWorldname(), c.getCoord().getX(), c.getCoord().getY(), c.getCoord().getZ(), true);
                            multiInv.addInventory(tmp);
                        } catch (CivTaskAbortException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        getConsumeComponent().setSource(multiInv);

        double cottage_consume_mod = 1.0; //allows buildings and govs to change the totals for cottage consumption.

        if (this.getTown().getBuffManager().hasBuff(Buff.REDUCE_CONSUME)) {
            cottage_consume_mod *= this.getTown().getBuffManager().getEffectiveDouble(Buff.REDUCE_CONSUME);
        }
        if (this.getTown().getBuffManager().hasBuff("buff_pyramid_cottage_consume")) {
            cottage_consume_mod *= this.getTown().getBuffManager().getEffectiveDouble("buff_pyramid_cottage_consume");
        }

        if (this.getTown().getBuffManager().hasBuff(Buff.FISHING)) {
            // XXX change this to config var after testing...
            int breadPerFish = this.getTown().getBuffManager().getEffectiveInt(Buff.FISHING);
            getConsumeComponent().addEquivExchange(Material.BREAD, Material.RAW_FISH, breadPerFish);
        }
        if (this.getTown().getCiv().getReligion().id.contains("islam")) {
            // getConsumeComponent().addEquivExchange(Material.PORK, null, 1);
            getConsumeComponent().removeEquivExchange(Material.PORK);
        }

        getConsumeComponent().setConsumeRate(cottage_consume_mod);

        Result result = Result.STAGNATE;
        try {
            result = getConsumeComponent().processConsumption();
            getConsumeComponent().onSave();
            getConsumeComponent().clearEquivExchanges();
        } catch (IllegalStateException e) {
            CivLog.exception(this.getDisplayName() + " Process Error in town: " + this.getTown().getName() + " and Location: " + this.getCorner(), e);
        }

        /* Bail early for results that do not generate coins. */
        switch (result) {
            case STARVE -> {
                CivMessage.sendTown(getTown(), ChatColor.RED + CivSettings.localize.localizedString("var_cottage_starved_base", getConsumeComponent().getLevel(), CivSettings.localize.localizedString("var_cottage_status_starved", getConsumeComponent().getCountString()), CivSettings.CURRENCY_NAME));
                return;
            }
            case LEVELDOWN -> {
                CivMessage.sendTown(getTown(), ChatColor.RED + CivSettings.localize.localizedString("var_cottage_starved_base", (getConsumeComponent().getLevel() + 1), CivSettings.localize.localizedString("var_cottage_status_lvlDown"), CivSettings.CURRENCY_NAME));
                return;
            }
            case STAGNATE -> {
                CivMessage.sendTown(getTown(), ChatColor.RED + CivSettings.localize.localizedString("var_cottage_starved_base", getConsumeComponent().getLevel(), CivSettings.localize.localizedString("var_cottage_status_stagnated", getConsumeComponent().getCountString()), CivSettings.CURRENCY_NAME));
                return;
            }
            case UNKNOWN -> {
                CivMessage.sendTown(getTown(), ChatColor.DARK_PURPLE + CivSettings.localize.localizedString("var_cottage_starved_unknwon", CivSettings.CURRENCY_NAME));
                return;
            }
            default -> {
            }
        }

        if (processPoison(multiInv)) {
            return;
        }

        /* Calculate how much money we made. */
        /* XXX leveling down doesnt generate coins, so we don't have to check it here. */
        ConfigCottageLevel lvl = null;
        if (result == Result.LEVELUP) {
            lvl = CivSettings.cottageLevels.get(getConsumeComponent().getLevel()-1);
        } else {
            lvl = CivSettings.cottageLevels.get(getConsumeComponent().getLevel());
        }

        int total_coins = (int)Math.round(lvl.coins*this.getTown().getCottageRate());
        if (this.getTown().getBuffManager().hasBuff("buff_pyramid_cottage_bonus")) {
            total_coins *= this.getTown().getBuffManager().getEffectiveDouble("buff_pyramid_cottage_bonus");
        }

        if (this.getCiv().hasTechnology("tech_taxation")) {
            total_coins *= CivSettings.techsConfig.getDouble("taxation_cottage_buff", 2.0);
        }

        //	this.getTown().depositTaxed(total_coins);
        //	attrComp.setValue(total_coins);
        double taxesPaid = total_coins * this.getTown().getDepositCiv().getIncomeTaxRate();

        String stateMessage = "";
        switch (result) {
            case GROW ->
                    stateMessage = ChatColor.DARK_GREEN + CivSettings.localize.localizedString("var_cottage_grew", getConsumeComponent().getCountString()) + ChatColor.YELLOW;
            case LEVELUP ->
                    stateMessage = ChatColor.BLUE + CivSettings.localize.localizedString("var_cottage_grew_lvlUp") + ChatColor.GREEN;
            case MAXED ->
                    stateMessage = ChatColor.DARK_AQUA + CivSettings.localize.localizedString("var_cottage_grew_isMaxed", getConsumeComponent().getCountString()) + ChatColor.AQUA;
            default -> {
            }
        }

        if (taxesPaid > 0) {
            CivMessage.sendTown(this.getTown(), ChatColor.GREEN + CivSettings.localize.localizedString("var_cottage_grew_base", getConsumeComponent().getLevel(), stateMessage, total_coins, CivSettings.CURRENCY_NAME,
                    ChatColor.YELLOW + CivSettings.localize.localizedString("var_cottage_grew_taxes", Math.floor(taxesPaid), this.getTown().getDepositCiv().getName())));
        } else {
            CivMessage.sendTown(this.getTown(), ChatColor.GREEN + CivSettings.localize.localizedString("var_cottage_grew_base", getConsumeComponent().getLevel(), stateMessage, total_coins, CivSettings.CURRENCY_NAME, ""));
        }

        this.getTown().getTreasury().deposit(total_coins - taxesPaid);
        this.getTown().getDepositCiv().taxPayment(this.getTown(), taxesPaid);
        this.generateResources(getLevel(), getMultiplier());
    }

    public double getMultiplier() {
        double rate = 1.0;
        rate += 0.05 * this.getTown().getStructureTypeCount("ti_tradeoutpost");
        rate += 0.05 * this.getTown().getStructureTypeCount("ti_fishing_boat");
        rate *= this.getTown().getGovernment().cottage_rate * this.getTown().getGovernment().maximum_tax_rate + this.getTown().getHappinessPercentage();
        if (this.getCiv().hasWonder("w_hanginggardens")) {
            rate *= 1.5;
        }
        return rate * this.getTown().getCottageRate();
    }

    public int getLevel() {
        return getConsumeComponent().getLevel();
    }

    public Result getLastResult() {
        return getConsumeComponent().getLastResult();
    }

    public int getCount() {
        return getConsumeComponent().getCount();
    }

    public void generateResources(int cottageLevel, double multiplier) {
        double iron = 0.5;
        double gold = 0.25;
        double diamond = 0.18;
        double emerald = 0.075;
        double tungsten = 0.15;
        double chromium = 0.12;
        this.getTown().getFreeGranary().putResources(
                iron * cottageLevel * multiplier,
                gold * cottageLevel * multiplier,
                diamond * cottageLevel * multiplier,
                emerald * cottageLevel * multiplier,
                tungsten * cottageLevel * multiplier,
                chromium * cottageLevel * multiplier);
    }

    public int getMaxCount() {
        int level = getLevel();

        ConfigCottageLevel lvl = CivSettings.cottageLevels.get(level);
        return lvl.count;
    }

    public double getCoinsGenerated() {
        int level = getLevel();

		ConfigCottageLevel lvl = CivSettings.cottageLevels.get(level);
		if (lvl == null) {
			return 0;
		}
		return lvl.coins;
	}

    public void delevel() {
        int currentLevel = getLevel();

        if (currentLevel > 1) {
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
