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
package com.avrgaming.civcraft.object;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigTradeGood;
import com.avrgaming.civcraft.database.SQLController;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.TradeOutpost;
import com.avrgaming.civcraft.util.BlockCoord;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

public class TradeGood extends SQLObject {

    private ConfigTradeGood info;
    private Town town;
    private Civilization civ;
    private BlockCoord coord;
    private BlockCoord bonusLocation;
    private Structure struct;

    public TradeGood(ConfigTradeGood good, BlockCoord coord) {
        this.info = good;
        this.coord = coord;
        try {
            this.setName(good.id);
        } catch (InvalidNameException e) {
            e.printStackTrace();
        }

        town = null;
        civ = null;
    }

    public TradeGood(ResultSet rs) throws SQLException, InvalidNameException {
        this.load(rs);
    }

    public static final String TABLE_NAME = "TRADE_GOODS";

    public static void init() throws SQLException {
        if (!SQLController.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQLController.tb_prefix + TABLE_NAME + " (" +
                    "`uuid` VARCHAR(36) NOT NULL," +
                    "`name` VARCHAR(64) NOT NULL," +
                    "`town_uuid` VARCHAR(36)," +
                    "`structure_uuid` VARCHAR(36), " +
                    "`coord` mediumtext DEFAULT NULL," +
                    "`bonusLocation` mediumtext DEFAULT NULL," +
                    //	 "FOREIGN KEY (town_id) REFERENCES "+SQLController.tb_prefix+"TOWNS(id),"+
                    "PRIMARY KEY (`uuid`)" + ")";

            SQLController.makeTable(table_create);
            CivLog.info("Created " + TABLE_NAME + " table");
        } else {
            CivLog.info(TABLE_NAME + " table OK!");
        }
    }

	
	@Override
    public void load(ResultSet rs) throws SQLException, InvalidNameException {
        this.setUUID(UUID.fromString(rs.getString("uuid")));
        this.setName(rs.getString("name"));
        setInfo(CivSettings.goods.get(this.getName()));
        this.setTown(Town.getTownFromUUID(UUID.fromString(rs.getString("town_uuid"))));
        String bonusLocation = rs.getString("bonusLocation");
        if (bonusLocation != null) {
            this.bonusLocation = new BlockCoord(bonusLocation);
        } else {
            this.bonusLocation = null;
        }

        this.coord = new BlockCoord(rs.getString("coord"));
        this.addProtectedBlocks(this.coord);

        this.setStruct(CivGlobal.getStructureByUUID(UUID.fromString(rs.getString("structure_uuid"))));

        if (this.getStruct() != null) {
            if (struct instanceof TradeOutpost) {
                TradeOutpost outpost = (TradeOutpost) this.struct;
                outpost.setGood(this);
            }
        }

        if (this.getTown() != null) {
            this.civ = this.getTown().getCiv();
        }

    }

    public Chunk getChunk() {
        int x = this.getCoord().getX();
        int z = this.getCoord().getZ();
        return Bukkit.getWorld(coord.getWorldname()).getChunkAt(x, z);
    }

    private void addProtectedBlocks(BlockCoord coord2) {
//		CivLog.debug("Protecting TRADE GOOD:"+coord2);
//		for (int i = 0; i < 3; i++) {
//			BlockCoord bcoord = new BlockCoord(coord2);
//			
//            ProtectedBlock pb = new ProtectedBlock(bcoord, ProtectedBlock.Type.TRADE_MARKER);
//            CivGlobal.addProtectedBlock(pb);
//            
//            bcoord.setY(bcoord.getY()+1);
//		}
    }

    @Override
    public void save() {
        SQLUpdate.add(this);
    }

    @Override
    public void saveNow() throws SQLException {
        HashMap<String, Object> hashmap = new HashMap<>();

        hashmap.put("name", this.getName());
        if (this.getTown() != null) {
            hashmap.put("town_uuid", this.getTown().getUUID().toString());
        } else {
            hashmap.put("town_uuid", null);
        }
        if (this.bonusLocation != null) {
            hashmap.put("bonusLocation", this.bonusLocation.toString());
        } else {
            hashmap.put("bonusLocation", null);
        }
        hashmap.put("coord", this.coord.toString());
        if (this.getStruct() == null) {
            hashmap.put("structure_uuid", NULL_UUID.toString());
        } else {
            hashmap.put("structure_uuid", this.getStruct().getUUID().toString());

        }

        SQLController.updateNamedObject(this, hashmap, TABLE_NAME);
    }

    @Override
    public void delete() {
    }


    public Town getTown() {
        return town;
    }


    public void setTown(Town town) {
        this.town = town;
    }


    public Civilization getCiv() {
        return civ;
    }


    public void setCiv(Civilization civ) {
        this.civ = civ;
    }


    public ConfigTradeGood getInfo() {
        return info;
    }


    public void setInfo(ConfigTradeGood info) {
        this.info = info;
    }


    public BlockCoord getCoord() {
        return coord;
    }


    public void setCoord(BlockCoord coord) {
        this.coord = coord;
    }

    public static double getBaseValue(TradeGood good) {
        ConfigTradeGood configTradeGood = good.getInfo();
        return configTradeGood.value;
    }

    public static int getTradeGoodCount(BonusGoodie goodie, Town town) {
        int amount = 0;

        for (BonusGoodie g : town.getBonusGoodies()) {
            if (goodie.getDisplayName().equals(g.getDisplayName())) {
                amount++;
            }
        }
		
		/*for (TradeGood g : town.getTradeGoods()) {
			if ((g.getInfo().id.equals(good.getInfo().id))) {

				if (g.getStruct() != null) {
					CultureChunk cc = CivGlobal.getCultureChunk(g.getCoord().getLocation());
					if (cc != null && cc.getTown() == town) {
						amount++;
					}
				}
			}
		}*/
        return amount;
    }

    private static double getTradeGoodIncomeBonus(TradeGood good, Town town) {
        //TODO revist when we add wonders.
        // Search for any passives that apply.
        //	ArrayList<String> values = town.getPassives("trade_income_bonus");
		
		/*for (String v : values) {
			String[] split = v.split(",");
			for (String good_str : split) {
				if (good_str.equalsIgnoreCase(good.name)) {
					// First value should always be bonus.
					return Double.valueOf(split[0]);
				}
			}
		}*/

        return 0;
    }

    public static double getTradeGoodValue(BonusGoodie goodie, Town town) {

        TradeGood good = goodie.getOutpost().getGood();
        int goodMax = CivSettings.goodsConfig.getInt("trade_good_multiplier_max", 3);
        int effectiveCount = getTradeGoodCount(goodie, town) - 1;

        if (effectiveCount > goodMax) {
            effectiveCount = goodMax;
        }

        double rate = 1.0 + (0.5 * (effectiveCount));

        //Find any passives with trade_income_bonus for this good
        rate += getTradeGoodIncomeBonus(good, town);

        return getBaseValue(good) * rate;
    }

    public static double getTownBaseGoodPaymentViaGoodie(Town town) {
        // Find trade goods from goodies in town hall.
        double total_payment = 0.0;

        for (BonusGoodie goodie : town.getBonusGoodies()) {
            TradeOutpost outpost = goodie.getOutpost();
            if (outpost == null) {
                continue;
            }

            CultureChunk cc = CivGlobal.getCultureChunk(outpost.getCorner().getLocation());
            if (cc == null) {
                continue;
            }

            if (!outpost.isActive()) {
                continue;
            }

            double payment = getTradeGoodValue(goodie, town);
            total_payment += payment;
        }

        return total_payment;
    }

    public static double getTownTradePayment(Town town) {
        double total_payment = getTownBaseGoodPaymentViaGoodie(town);
        total_payment *= town.getTradeRate();


        return total_payment;
    }


    public Structure getStruct() {
        return struct;
    }


    public void setStruct(Structure struct) {
        this.struct = struct;
    }


}
