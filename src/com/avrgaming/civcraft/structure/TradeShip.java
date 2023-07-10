package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.components.AttributeBiomeRadiusPerLevel;
import com.avrgaming.civcraft.components.TradeLevelComponent;
import com.avrgaming.civcraft.components.TradeLevelComponent.Result;
import com.avrgaming.civcraft.components.TradeShipResults;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMineLevel;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.CivTaskAbortException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;

public class TradeShip extends WaterStructure {

    private int upgradeLevel = 1;
    private final int tickLevel = 1;

    public HashSet<BlockCoord> goodsDepositPoints = new HashSet<>();
    public HashSet<BlockCoord> goodsWithdrawPoints = new HashSet<>();

    private TradeLevelComponent consumeComp = null;

    protected TradeShip(Location center, String id, Town town) throws CivException {
        super(center, id, town);
        setUpgradeLvl(town.saved_tradeship_upgrade_levels);
    }

    public TradeShip(ResultSet rs) throws SQLException, CivException {
        super(rs);
    }

    @Override
    public void loadSettings() {
        super.loadSettings();
    }

    public String getkey() {
        return getTown().getName() + "_" + this.getConfigId() + "_" + this.getCorner().toString();
    }

    public TradeLevelComponent getConsumeComponent() {
        if (consumeComp == null) {
            consumeComp = (TradeLevelComponent) this.getComponent(TradeLevelComponent.class.getSimpleName());
        }
        return consumeComp;
    }

    @Override
    public void updateSignText() {
        reprocessCommandSigns();
    }

    public void reprocessCommandSigns() {
        /* Load in the template. */
        //Template tpl = new Template();
        Template tpl;
        try {
            //tpl.load_template(this.getSavedTemplatePath());
            tpl = Template.getTemplate(this.getSavedTemplatePath(), null);
        } catch (IOException | CivException e) {
            e.printStackTrace();
            return;
        }

        TaskMaster.syncTask(() -> processCommandSigns(tpl, corner), TimeTools.toTicks(1));
    }

    private void processCommandSigns(Template tpl, BlockCoord corner) {
        for (BlockCoord relativeCoord : tpl.commandBlockRelativeLocations) {
            SimpleBlock sb = tpl.blocks[relativeCoord.getX()][relativeCoord.getY()][relativeCoord.getZ()];
            BlockCoord absCoord = new BlockCoord(corner.getBlock().getRelative(relativeCoord.getX(), relativeCoord.getY(), relativeCoord.getZ()));

            switch (sb.command) {
                case "/incoming": {
                    int ID = Integer.parseInt(sb.keyvalues.get("id"));
                    if (this.getUpgradeLvl() >= ID + 1) {
                        this.goodsWithdrawPoints.add(absCoord);
                        Block block = absCoord.getBlock();
                        block.setType(Material.CHEST);
                        byte data3 = CivData.convertSignDataToChestData((byte) sb.getData());
                        Block block1 = absCoord.getBlock();
                        block1.setData((byte) (int) data3);
                    } else {
                        Block block = absCoord.getBlock();
                        block.setType(Material.AIR);
                        Block block1 = absCoord.getBlock();
                        block1.setData((byte) sb.getData());
                    }
                    this.addStructureBlock(absCoord, false);
                    break;
                }
                case "/inSign": {
                    int ID = Integer.parseInt(sb.keyvalues.get("id"));
                    if (this.getUpgradeLvl() >= ID + 1) {
                        Block block = absCoord.getBlock();
                        block.setType(Material.WALL_SIGN);
                        Block block1 = absCoord.getBlock();
                        block1.setData((byte) sb.getData());

                        Sign sign = (Sign) absCoord.getBlock().getState();
                        sign.setLine(0, CivSettings.localize.localizedString("tradeship_sign_input_line0"));
                        sign.setLine(1, String.valueOf(ID + 1));
                        sign.setLine(2, "");
                        sign.setLine(3, "");
                        sign.update();
                    } else {
                        Block block = absCoord.getBlock();
                        block.setType(Material.WALL_SIGN);
                        Block block1 = absCoord.getBlock();
                        block1.setData((byte) sb.getData());

                        Sign sign = (Sign) absCoord.getBlock().getState();
                        sign.setLine(0, CivSettings.localize.localizedString("tradeship_sign_input_line0"));
                        sign.setLine(1, CivSettings.localize.localizedString("tradeship_sign_input_notupgraded_line1"));
                        sign.setLine(2, (CivSettings.localize.localizedString("tradeship_sign_input_notupgraded_line2")));
                        sign.setLine(3, CivSettings.localize.localizedString("tradeship_sign_input_notupgraded_line3"));
                        sign.update();
                    }
                    this.addStructureBlock(absCoord, false);
                    break;
                }
                case "/outgoing": {
                    int ID = Integer.parseInt(sb.keyvalues.get("id"));

                    if (this.getLevel() >= (ID * 2) + 1) {
                        this.goodsDepositPoints.add(absCoord);
                        Block block = absCoord.getBlock();
                        block.setType(Material.CHEST);
                        byte data3 = CivData.convertSignDataToChestData((byte) sb.getData());
                        Block block1 = absCoord.getBlock();
                        block1.setData((byte) (int) data3);
                        this.addStructureBlock(absCoord, false);
                    } else {
                        Block block = absCoord.getBlock();
                        block.setType(Material.AIR);
                        Block block1 = absCoord.getBlock();
                        block1.setData((byte) sb.getData());
                    }
                    break;
                }
                case "/outSign": {
                    int ID = Integer.parseInt(sb.keyvalues.get("id"));
                    if (this.getLevel() >= (ID * 2) + 1) {
                        Block block = absCoord.getBlock();
                        block.setType(Material.WALL_SIGN);
                        Block block1 = absCoord.getBlock();
                        block1.setData((byte) sb.getData());

                        Sign sign = (Sign) absCoord.getBlock().getState();
                        sign.setLine(0, CivSettings.localize.localizedString("tradeship_sign_output_line0"));
                        sign.setLine(1, String.valueOf(ID + 1));
                        sign.setLine(2, "");
                        sign.setLine(3, "");
                        sign.update();
                    } else {
                        Block block = absCoord.getBlock();
                        block.setType(Material.WALL_SIGN);
                        Block block1 = absCoord.getBlock();
                        block1.setData((byte) sb.getData());

                        Sign sign = (Sign) absCoord.getBlock().getState();
                        sign.setLine(0, CivSettings.localize.localizedString("tradeship_sign_output_line0"));
                        sign.setLine(1, CivSettings.localize.localizedString("tradeship_sign_output_notupgraded_line1"));
                        sign.setLine(2, (CivSettings.localize.localizedString("var_tradeship_sign_output_notupgraded_line2", ((ID * 2) + 1))));
                        sign.setLine(3, CivSettings.localize.localizedString("tradeship_sign_output_notupgraded_line3"));
                        sign.update();
                    }
                    this.addStructureBlock(absCoord, false);
                    break;
                }
                case "/in": {
                    int ID = Integer.parseInt(sb.keyvalues.get("id"));
                    if (ID == 0) {
                        Block block = absCoord.getBlock();
                        block.setType(Material.WALL_SIGN);
                        Block block1 = absCoord.getBlock();
                        block1.setData((byte) sb.getData());

                        Sign sign = (Sign) absCoord.getBlock().getState();
                        sign.setLine(0, CivSettings.localize.localizedString("tradeship_sign_input_line0"));
                        sign.setLine(1, "1");
                        sign.setLine(2, "2");
                        sign.setLine(3, "");
                        sign.update();
                    } else {
                        Block block = absCoord.getBlock();
                        block.setType(Material.WALL_SIGN);
                        Block block1 = absCoord.getBlock();
                        block1.setData((byte) sb.getData());

                        Sign sign = (Sign) absCoord.getBlock().getState();
                        sign.setLine(0, CivSettings.localize.localizedString("tradeship_sign_input_line0"));
                        sign.setLine(1, "3");
                        sign.setLine(2, "4");
                        sign.setLine(3, "");
                        sign.update();
                    }
                    this.addStructureBlock(absCoord, false);
                    break;
                }
                default: {
                    /* Unrecognized command... treat as a literal sign. */
                    Block block = absCoord.getBlock();
                    block.setType(Material.WALL_SIGN);
                    Block block1 = absCoord.getBlock();
                    block1.setData((byte) sb.getData());

                    Sign sign = (Sign) absCoord.getBlock().getState();
                    sign.setLine(0, sb.message[0]);
                    sign.setLine(1, sb.message[1]);
                    sign.setLine(2, sb.message[2]);
                    sign.setLine(3, sb.message[3]);
                    sign.update();

                    this.addStructureBlock(absCoord, false);
                    break;
                }
            }
        }
    }


    public TradeShipResults consume(CivAsyncTask task) throws InterruptedException {
        TradeShipResults tradeResult;
        //Look for the TradeShip chests.
        if (this.goodsDepositPoints.size() == 0 || this.goodsWithdrawPoints.size() == 0) {
            tradeResult = new TradeShipResults();
            tradeResult.setResult(Result.STAGNATE);
            return tradeResult;
        }
        MultiInventory mInv = new MultiInventory();

        for (BlockCoord bcoord : this.goodsDepositPoints) {
            task.syncLoadChunk(bcoord.getWorldname(), bcoord.getX(), bcoord.getZ());
            Inventory tmp;
            try {
                tmp = task.getChestInventory(bcoord.getWorldname(), bcoord.getX(), bcoord.getY(), bcoord.getZ(), true);
            } catch (CivTaskAbortException e) {
                tradeResult = new TradeShipResults();
                tradeResult.setResult(Result.STAGNATE);
                return tradeResult;
            }
            mInv.addInventory(tmp);
        }

        if (mInv.getInventoryCount() == 0) {
            tradeResult = new TradeShipResults();
            tradeResult.setResult(Result.STAGNATE);
            return tradeResult;
        }
        getConsumeComponent().setSource(mInv);
        getConsumeComponent().setConsumeRate(1.0);

        try {
            tradeResult = getConsumeComponent().processConsumption(this.getUpgradeLvl() - 1);
            getConsumeComponent().onSave();
        } catch (IllegalStateException e) {
            tradeResult = new TradeShipResults();
            tradeResult.setResult(Result.STAGNATE);
            CivLog.exception(this.getDisplayName() + " Process Error in town: " + this.getTown().getName() + " and Location: " + this.getCorner(), e);
            return tradeResult;
        }
        return tradeResult;
    }

    public void process_trade_ship(CivAsyncTask task) throws InterruptedException, InvalidConfiguration {
        TradeShipResults tradeResult = this.consume(task);

        Result result = tradeResult.getResult();
        switch (result) {
            case STAGNATE:
                CivMessage.sendTown(getTown(), CivColor.Rose + CivSettings.localize.localizedString("var_tradeship_stagnated", getConsumeComponent().getLevel(), CivColor.LightGreen + getConsumeComponent().getCountString()));
                break;
            case GROW:
                CivMessage.sendTown(getTown(), CivColor.LightGreen + CivSettings.localize.localizedString("var_tradeship_productionGrew", getConsumeComponent().getLevel(), getConsumeComponent().getCountString()));
                break;
            case LEVELUP:
                CivMessage.sendTown(getTown(), CivColor.LightGreen + CivSettings.localize.localizedString("var_tradeship_lvlUp", getConsumeComponent().getLevel()));
                this.reprocessCommandSigns();
                break;
            case MAXED:
                CivMessage.sendTown(getTown(), CivColor.LightGreen + CivSettings.localize.localizedString("var_tradeship_maxed", getConsumeComponent().getLevel(), CivColor.LightGreen + getConsumeComponent().getCountString()));
                break;
            default:
                break;
        }
        if (tradeResult.getCulture() >= 1) {
            int total_culture = tradeResult.getCulture();

            this.getTown().addAccumulatedCulture(total_culture);
            this.getTown().save();
        }
        if (tradeResult.getMoney() >= 1) {
            double total_coins = tradeResult.getMoney() * (getTown().getTradeRate() / 2);
            if (this.getTown().getBuffManager().hasBuff("buff_ingermanland_trade_ship_income")) {
                total_coins *= this.getTown().getBuffManager().getEffectiveDouble("buff_ingermanland_trade_ship_income");
            }

            if (this.getTown().getBuffManager().hasBuff("buff_great_lighthouse_trade_ship_income")) {
                total_coins *= this.getTown().getBuffManager().getEffectiveDouble("buff_great_lighthouse_trade_ship_income");
            }
            if (this.getTown().hasStructure("s_lighthouse")) {
                total_coins *= CivSettings.getDouble(CivSettings.townConfig, "town.lighthouse_trade_ship_boost");
            }

            double taxesPaid = total_coins * this.getTown().getDepositCiv().getIncomeTaxRate();

            if (total_coins >= 1) {
                CivMessage.sendTown(getTown(), CivColor.LightGreen + CivSettings.localize.localizedString("var_tradeship_success", Math.round(total_coins), CivSettings.CURRENCY_NAME, tradeResult.getCulture(), tradeResult.getConsumed()));
            }
            if (taxesPaid > 0) {
                CivMessage.sendTown(this.getTown(), CivColor.Yellow + CivSettings.localize.localizedString("var_tradeship_taxesPaid", Math.round(taxesPaid), CivSettings.CURRENCY_NAME));
            }

            this.getTown().getTreasury().deposit(total_coins - taxesPaid);
            this.getTown().getDepositCiv().taxPayment(this.getTown(), taxesPaid);
        }

        if (tradeResult.getReturnCargo().size() >= 1) {
            MultiInventory multiInv = new MultiInventory();

            for (BlockCoord bcoord : this.goodsWithdrawPoints) {
                task.syncLoadChunk(bcoord.getWorldname(), bcoord.getX(), bcoord.getZ());
                Inventory tmp;
                try {
                    tmp = task.getChestInventory(bcoord.getWorldname(), bcoord.getX(), bcoord.getY(), bcoord.getZ(), true);
                    multiInv.addInventory(tmp);
                } catch (CivTaskAbortException e) {

                    e.printStackTrace();
                }
            }

            for (ItemStack item : tradeResult.getReturnCargo()) {
                multiInv.addItemStack(item);
            }
            CivMessage.sendTown(getTown(), CivColor.LightGreen + CivSettings.localize.localizedString("tradeship_successSpecail"));
        }
    }

    public void onPostBuild(BlockCoord absCoord, SimpleBlock commandBlock) {
        this.upgradeLevel = getTown().saved_tradeship_upgrade_levels;
        this.reprocessCommandSigns();
    }

    public int getUpgradeLvl() {
        return upgradeLevel;
    }

    public void setUpgradeLvl(int level) {
        this.upgradeLevel = level;

        if (this.isComplete()) {
            this.reprocessCommandSigns();
        }
    }

    public int getLevel() {
        try {
            return this.getConsumeComponent().getLevel();
        } catch (Exception e) {
            return tickLevel;
        }
    }

    public double getHammersPerTile() {
        AttributeBiomeRadiusPerLevel attrBiome = (AttributeBiomeRadiusPerLevel) this.getComponent("AttributeBiomeBase");
        double base = attrBiome.getBaseValue();

        double rate = 1;
        rate += this.getTown().getBuffManager().getEffectiveDouble(Buff.ADVANCED_TOOLING);
        return (rate * base);
    }

    public int getCount() {
        return this.getConsumeComponent().getCount();
    }

    public int getMaxCount() {
        int level = getLevel();

        ConfigMineLevel lvl = CivSettings.mineLevels.get(level);
        return lvl.count;
    }

    public Result getLastResult() {
        return this.getConsumeComponent().getLastResult();
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
