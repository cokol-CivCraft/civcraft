package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.components.TradeLevelComponent;
import com.avrgaming.civcraft.components.TradeLevelComponent.Result;
import com.avrgaming.civcraft.components.TradeShipResults;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.CivTaskAbortException;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.MultiInventory;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.TimeTools;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Chest;
import org.bukkit.material.MaterialData;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.UUID;

public class TradeShip extends WaterStructure {

    private int upgradeLevel = 1;

    public HashSet<BlockCoord> goodsDepositPoints = new HashSet<>();
    public HashSet<BlockCoord> goodsWithdrawPoints = new HashSet<>();

    private TradeLevelComponent consumeComp = null;

    protected TradeShip(Location center, String id, Town town) throws CivException {
        super(center, id, town);
        setUpgradeLvl(town.saved_tradeship_upgrade_levels);
    }

    public TradeShip(UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(uuid, nbt);
    }

    @Override
    public void loadSettings() {
        super.loadSettings();
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
            if (!(sb.getMaterialData() instanceof org.bukkit.material.Sign)) {
                continue;
            }
            switch (sb.command) {
                case "/incoming" -> {
                    int ID = Integer.parseInt(sb.keyvalues.get("id"));
                    if (this.getUpgradeLvl() >= ID + 1) {
                        this.goodsWithdrawPoints.add(absCoord);
                        absCoord.getBlock().getState().setData(new Chest(((org.bukkit.material.Sign) sb.getMaterialData()).getFacing()));
                    } else {
                        absCoord.getBlock().getState().setData(new MaterialData(Material.AIR));
                    }
                    this.addStructureBlock(absCoord, false);
                }
                case "/inSign" -> {
                    int ID = Integer.parseInt(sb.keyvalues.get("id"));
                    if (this.getUpgradeLvl() >= ID + 1) {
                        Sign sign = (Sign) absCoord.getBlock().getState();
                        sign.setData(sb.getMaterialData());
                        sign.setLine(0, CivSettings.localize.localizedString("tradeship_sign_input_line0"));
                        sign.setLine(1, String.valueOf(ID + 1));
                        sign.setLine(2, "");
                        sign.setLine(3, "");
                        sign.update();
                    } else {
                        Sign sign = (Sign) absCoord.getBlock().getState();
                        sign.setData(sb.getMaterialData());
                        sign.setLine(0, CivSettings.localize.localizedString("tradeship_sign_input_line0"));
                        sign.setLine(1, CivSettings.localize.localizedString("tradeship_sign_input_notupgraded_line1"));
                        sign.setLine(2, (CivSettings.localize.localizedString("tradeship_sign_input_notupgraded_line2")));
                        sign.setLine(3, CivSettings.localize.localizedString("tradeship_sign_input_notupgraded_line3"));
                        sign.update();
                    }
                    this.addStructureBlock(absCoord, false);
                }
                case "/outgoing" -> {
                    int ID = Integer.parseInt(sb.keyvalues.get("id"));

                    if (this.getLevel() >= (ID * 2) + 1) {
                        this.goodsDepositPoints.add(absCoord);
                        absCoord.getBlock().getState().setData(new Chest(((org.bukkit.material.Sign) sb.getMaterialData()).getFacing()));
                        this.addStructureBlock(absCoord, false);
                    } else {
                        absCoord.getBlock().getState().setData(new MaterialData(Material.AIR));
                    }
                }
                case "/outSign" -> {
                    int ID = Integer.parseInt(sb.keyvalues.get("id"));
                    if (this.getLevel() >= (ID * 2) + 1) {
                        absCoord.getBlock().setType(Material.WALL_SIGN);
                        absCoord.getBlock().setData((byte) sb.getData());

                        Sign sign = (Sign) absCoord.getBlock().getState();
                        sign.setLine(0, CivSettings.localize.localizedString("tradeship_sign_output_line0"));
                        sign.setLine(1, String.valueOf(ID + 1));
                        sign.setLine(2, "");
                        sign.setLine(3, "");
                        sign.update();
                    } else {
                        absCoord.getBlock().setType(Material.WALL_SIGN);
                        absCoord.getBlock().setData((byte) sb.getData());

                        Sign sign = (Sign) absCoord.getBlock().getState();
                        sign.setLine(0, CivSettings.localize.localizedString("tradeship_sign_output_line0"));
                        sign.setLine(1, CivSettings.localize.localizedString("tradeship_sign_output_notupgraded_line1"));
                        sign.setLine(2, (CivSettings.localize.localizedString("var_tradeship_sign_output_notupgraded_line2", ((ID * 2) + 1))));
                        sign.setLine(3, CivSettings.localize.localizedString("tradeship_sign_output_notupgraded_line3"));
                        sign.update();
                    }
                    this.addStructureBlock(absCoord, false);
                }
                case "/in" -> {
                    int ID = Integer.parseInt(sb.keyvalues.get("id"));
                    absCoord.getBlock().setType(Material.WALL_SIGN);
                    absCoord.getBlock().setData((byte) sb.getData());
                    Sign sign = (Sign) absCoord.getBlock().getState();
                    sign.setLine(0, CivSettings.localize.localizedString("tradeship_sign_input_line0"));

                    if (ID == 0) {
                        sign.setLine(1, "1");
                        sign.setLine(2, "2");
                    } else {
                        sign.setLine(1, "3");
                        sign.setLine(2, "4");
                    }
                    sign.setLine(3, "");
                    sign.update();
                    this.addStructureBlock(absCoord, false);
                }
                default -> {
                    /* Unrecognized command... treat as a literal sign. */
                    absCoord.getBlock().setType(Material.WALL_SIGN);
                    absCoord.getBlock().setData((byte) sb.getData());

                    Sign sign = (Sign) absCoord.getBlock().getState();
                    sign.setLine(0, sb.message[0]);
                    sign.setLine(1, sb.message[1]);
                    sign.setLine(2, sb.message[2]);
                    sign.setLine(3, sb.message[3]);
                    sign.update();

                    this.addStructureBlock(absCoord, false);
                }
            }
        }
    }


    public TradeShipResults consume(CivAsyncTask task) throws InterruptedException {
        //Look for the TradeShip chests.
        if (this.goodsDepositPoints.isEmpty() || this.goodsWithdrawPoints.isEmpty()) {
            TradeShipResults tradeResult = new TradeShipResults();
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
                TradeShipResults tradeResult = new TradeShipResults();
                tradeResult.setResult(Result.STAGNATE);
                return tradeResult;
            }
            mInv.addInventory(tmp);
        }

        if (mInv.getInventoryCount() == 0) {
            TradeShipResults tradeResult = new TradeShipResults();
            tradeResult.setResult(Result.STAGNATE);
            return tradeResult;
        }
        getConsumeComponent().setSource(mInv);
        getConsumeComponent().setConsumeRate(1.0);
        TradeShipResults tradeResult;
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

    public void process_trade_ship(CivAsyncTask task) throws InterruptedException {
        TradeShipResults tradeResult = this.consume(task);

        switch (tradeResult.getResult()) {
            case STAGNATE ->
                    CivMessage.sendTown(getTown(), ChatColor.RED + CivSettings.localize.localizedString("var_tradeship_stagnated", getConsumeComponent().getLevel(), ChatColor.GREEN + getConsumeComponent().getCountString()));
            case GROW ->
                    CivMessage.sendTown(getTown(), ChatColor.GREEN + CivSettings.localize.localizedString("var_tradeship_productionGrew", getConsumeComponent().getLevel(), getConsumeComponent().getCountString()));
            case LEVELUP -> {
                CivMessage.sendTown(getTown(), ChatColor.GREEN + CivSettings.localize.localizedString("var_tradeship_lvlUp", getConsumeComponent().getLevel()));
                this.reprocessCommandSigns();
            }
            case MAXED ->
                    CivMessage.sendTown(getTown(), ChatColor.GREEN + CivSettings.localize.localizedString("var_tradeship_maxed", getConsumeComponent().getLevel(), ChatColor.GREEN + getConsumeComponent().getCountString()));
            default -> {
            }
        }
        if (tradeResult.getCulture() >= 1) {
            int total_culture = tradeResult.getCulture();

            this.getTown().addAccumulatedCulture(total_culture);
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
                total_coins *= CivSettings.townConfig.getDouble("town.lighthouse_trade_ship_boost", 1.2);
            }

            double taxesPaid = total_coins * this.getTown().getDepositCiv().getIncomeTaxRate();

            if (total_coins >= 1) {
                CivMessage.sendTown(getTown(), ChatColor.GREEN + CivSettings.localize.localizedString("var_tradeship_success", Math.round(total_coins), CivSettings.CURRENCY_NAME, tradeResult.getCulture(), tradeResult.getConsumed()));
            }
            if (taxesPaid > 0) {
                CivMessage.sendTown(this.getTown(), ChatColor.YELLOW + CivSettings.localize.localizedString("var_tradeship_taxesPaid", Math.round(taxesPaid), CivSettings.CURRENCY_NAME));
            }

            this.getTown().getTreasury().deposit(total_coins - taxesPaid);
            this.getTown().getDepositCiv().taxPayment(this.getTown(), taxesPaid);
        }

        if (!tradeResult.getReturnCargo().isEmpty()) {
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
            CivMessage.sendTown(getTown(), ChatColor.GREEN + CivSettings.localize.localizedString("tradeship_successSpecail"));
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
            return 1;
        }
    }

    public int getCount() {
        return this.getConsumeComponent().getCount();
    }

//    public int getMaxCount() {
//        int level = getLevel();
//
//        ConfigMineLevel lvl = CivSettings.mineLevels.get(level);
//        return lvl.count();
//    }

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
