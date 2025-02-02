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

import com.avrgaming.civcraft.database.SQLController;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.MultiInventory;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ConfigMarketItem {
    public int id;
    public String name;
    public Material type_id;
    public String custom_id;
    public int data;
    public int inital_value;

    private int buy_value;
    private int buy_bulk;
    private int sell_value;
    private int sell_bulk;

    private int bought;
    private int sold;
    private int buysell_count = 0;
    private boolean stackable;
    private int step;

    public static int BASE_ITEM_AMOUNT = 1;
    public static int STEP = 1;
    public static int STEP_COUNT = 256;
    public static double RATE = 0.15;

    public enum LastAction {
        NEUTRAL,
        BUY,
        SELL
    }

    public LastAction lastaction = LastAction.NEUTRAL;

    public static void loadConfig(FileConfiguration cfg, Map<Integer, ConfigMarketItem> items) {
        items.clear();

        STEP = CivSettings.marketConfig.getInt("step", 1);
        STEP_COUNT = CivSettings.marketConfig.getInt("step_count", 256);
        RATE = CivSettings.marketConfig.getDouble("rate", 0.15);


        for (String id : cfg.getConfigurationSection("items").getKeys(false)) {
            ConfigurationSection level = cfg.getConfigurationSection("items").getConfigurationSection(id);
            ConfigMarketItem item = new ConfigMarketItem();

            item.id = Integer.parseInt(id);
            item.name = level.getString("name");
            item.type_id = Material.getMaterial(level.getString("type_id"));
            item.data = level.getInt("data");
            new ItemStack(item.type_id, 1, (short) item.data);
            item.inital_value = level.getInt("value");
            item.custom_id = level.getString("custom_id");
            item.step = level.getInt("step", STEP);
            item.stackable = level.getBoolean("stackable", true);

            items.put(item.id, item);
        }
        CivLog.info("Loaded " + items.size() + " market items.");
    }

    public static final String TABLE_NAME = "MARKET_ITEMS";

    public static void init() throws SQLException {
        if (!SQLController.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQLController.tb_prefix + TABLE_NAME + " (" +
                    "`ident` VARCHAR(64) NOT NULL," +
                    "`buy_value` int(11) NOT NULL DEFAULT 105," +
                    "`buy_bulk` int(11) NOT NULL DEFAULT 1," +
                    "`sell_value` int(11) NOT NULL DEFAULT 95," +
                    "`sell_bulk` int(11) NOT NULL DEFAULT 1," +
                    "`buysell` int(11) NOT NULL DEFAULT 0," +
                    "`bought` int(11) NOT NULL DEFAULT 0," +
                    "`sold` int(11) NOT NULL DEFAULT 0," +
                    "`last_action` mediumtext, " +
                    "PRIMARY KEY (`ident`)" + ")";

            SQLController.makeTable(table_create);
            CivLog.info("Created " + TABLE_NAME + " table");
        } else {
            CivLog.info(TABLE_NAME + " table OK!");
        }

        for (ConfigMarketItem item : CivSettings.marketItems.values()) {
            item.load();
        }
    }

    private String getIdent() {
        return Objects.requireNonNullElseGet(this.custom_id, () -> type_id + ":" + data);
    }

    public void load() throws SQLException {
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            String query = "SELECT * FROM `" + SQLController.tb_prefix + TABLE_NAME + "` WHERE `ident` = ?;";
            ps = SQLController.getGameConnection().prepareStatement(query);
            ps.setString(1, getIdent());
            rs = ps.executeQuery();

            if (rs.next()) {
                this.buy_value = rs.getInt("buy_value");
                this.buy_bulk = rs.getInt("buy_bulk");
                this.sell_value = rs.getInt("sell_value");
                this.sell_bulk = rs.getInt("sell_bulk");
                this.bought = rs.getInt("bought");
                this.sold = rs.getInt("sold");
                this.lastaction = LastAction.valueOf(rs.getString("last_action"));
                this.buysell_count = rs.getInt("buysell");
            } else {
                this.bought = 0;
                this.sold = 0;
                this.buy_bulk = 1;
                this.sell_bulk = 1;
                this.buy_value = this.inital_value + (int) ((double) this.inital_value * RATE);
                this.sell_value = this.inital_value;

                if (buy_value == sell_value) {
                    buy_value++;
                }

                this.saveItemNow();
            }

        } finally {
            SQLController.close(rs, ps);
        }
    }

    public void saveItemNow() throws SQLException {
        PreparedStatement ps = null;

        try {
            String query = "INSERT INTO `" + SQLController.tb_prefix + TABLE_NAME + "` (`ident`, `buy_value`, `buy_bulk`, `sell_value`, `sell_bulk`, `bought`, `sold`, `last_action`, `buysell`) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `buy_value`=?, `buy_bulk`=?, `sell_value`=?, `sell_bulk`=?, `bought`=?, `sold`=?, `last_action`=?, `buysell`=?";
            ps = SQLController.getGameConnection().prepareStatement(query);

            ps.setString(1, getIdent());
            ps.setInt(2, buy_value);
            ps.setInt(3, buy_bulk);
            ps.setInt(4, sell_value);
            ps.setInt(5, sell_bulk);
            ps.setInt(6, bought);
            ps.setInt(7, sold);
            ps.setString(8, lastaction.toString());
            ps.setInt(9, buysell_count);
            ps.setInt(10, buy_value);
            ps.setInt(11, buy_bulk);
            ps.setInt(12, sell_value);
            ps.setInt(13, sell_bulk);
            ps.setInt(14, bought);
            ps.setInt(15, sold);
            ps.setString(16, lastaction.toString());
            ps.setInt(17, buysell_count);

            int rs = ps.executeUpdate();
            if (rs == 0) {
                throw new SQLException("Could not execute SQLController code:" + query);
            }
        } finally {
            SQLController.close(null, ps);
        }
    }

    public void save() {
        class SyncTask implements Runnable {

            @Override
            public void run() {
                try {
                    saveItemNow();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        TaskMaster.syncTask(new SyncTask());
    }

    public int getCoinsCostForAmount(int amount, int value, int dir) {
        int sum = 0;
        int current = value;
        int buysell = 0;

        for (int i = 0; i < amount; i++) {
            sum += current;
            buysell += dir;

            if ((dir * buysell) % (dir * STEP_COUNT) == 0) {
                current += dir * this.step;
                if (current < this.step) {
                    current = this.step;
                }
            }
        }
        return sum;
    }

    public int getBuyCostForAmount(int amount) {
        return getCoinsCostForAmount(amount, buy_value, 1) * 2;
    }

    public int getSellCostForAmount(int amount) {
        return getCoinsCostForAmount(amount, sell_value, -1);
    }

    public void buy(Resident resident, Player player, int amount) throws CivException {
        int total_items = 0;

        double coins = resident.getTreasury().getBalance();
        double cost = getBuyCostForAmount(amount);

        if (coins < cost) {
            throw new CivException(CivSettings.localize.localizedString("var_config_marketItem_notEnoughCurrency", (cost + " " + CivSettings.CURRENCY_NAME)));
        }

        for (int i = 0; i < amount; i++) {
            coins -= buy_value;
            total_items += BASE_ITEM_AMOUNT;
            increment();
        }

        /* We've now got the cost and items we've bought. Give to player. */
        resident.getTreasury().withdraw(cost);

        ItemStack newStack;
        if (this.custom_id == null) {
            newStack = new ItemStack(this.type_id, amount, (short) this.data);
        } else {
            newStack = LoreMaterial.spawn(LoreMaterial.materialMap.get(this.custom_id));
            newStack.setAmount(amount);
        }

        HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(newStack);
        for (ItemStack stack : leftovers.values()) {
            player.getWorld().dropItem(player.getLocation(), stack);
        }

        CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_market_buy", total_items, this.name, cost, CivSettings.CURRENCY_NAME));
        player.updateInventory();
    }

    public void sell(Resident resident, Player player, int amount) throws CivException {
        int total_coins = 0;
        int total_items = 0;

        MultiInventory inv = new MultiInventory();
        inv.addInventory(player.getInventory());

        if (!inv.contains(custom_id, this.type_id, (short) this.data, amount)) {
            throw new CivException(CivSettings.localize.localizedString("var_market_sell_notEnough", amount, this.name));
        }

        for (int i = 0; i < amount; i++) {
            total_coins += sell_value;
            total_items += BASE_ITEM_AMOUNT;

            decrement();
        }

        if (!inv.removeItem(this.custom_id, this.type_id, (short) this.data, amount, true)) {
            throw new CivException(CivSettings.localize.localizedString("var_market_sell_notEnough", amount, this.name));
        }

        resident.getTreasury().deposit(total_coins);
        CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_market_sell", total_items, this.name, total_coins, CivSettings.CURRENCY_NAME));
    }


    public void increment() {
        buysell_count++;
        if (((buysell_count % STEP_COUNT) == 0) || (!stackable)) {
            sell_value += this.step;
            buy_value = sell_value + (int) ((double) sell_value * RATE);

            if (buy_value == sell_value) {
                buy_value++;
            }
            //buy_value += STEP;
            //sell_value = buy_value - (PRICE_DIFF*2);
            buysell_count = 0;
            this.lastaction = LastAction.BUY;
        }
        this.save();
    }

    public void decrement() {
        buysell_count--;

        if ((((-buysell_count) % -STEP_COUNT) == 0) || (!stackable)) {
            sell_value -= this.step;
            buy_value = sell_value + (int) ((double) sell_value * RATE);

            if (buy_value == sell_value) {
                buy_value++;
            }

            //buy_value -= STEP;
            //sell_value = buy_value - (PRICE_DIFF*2);

            if (sell_value < this.step) {
                //buy_value = STEP + (PRICE_DIFF*2);
                sell_value = this.step;
                buy_value = this.step * 2;
            }
            if (sell_value < inital_value) {
                sell_value = inital_value; // планируется добавить минимальную цену не в единицу.
            }
            this.lastaction = LastAction.SELL;
            buysell_count = 0;
        }
        this.save();
    }

    public boolean isStackable() {
        return this.stackable;
    }

}
