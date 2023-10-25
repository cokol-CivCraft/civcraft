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

import com.avrgaming.civcraft.database.SQLController;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.util.BlockCoord;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

public class ProtectedBlock extends SQLObject {

    private BlockCoord coord;
    private Type type;
    //private Structure owner;

    public ProtectedBlock(BlockCoord coord, Type type) {
        this.coord = coord;
        this.type = type;
    }

    public ProtectedBlock(ResultSet rs) throws SQLException {
        //	this.coord = new BlockCoord(rs.getString("coord"));
        //	this.type = Type.TRADE_MARKER;
        //this.owner = rs.getInt(getId());
        this.load(rs);
    }

    public enum Type {
        NONE,
        TRADE_MARKER,
    }

    public static final String TABLE_NAME = "PROTECTED_BLOCKS";

    public static void init() throws SQLException {
        if (!SQLController.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQLController.tb_prefix + TABLE_NAME + " (" +
                    "`uuid` VARCHAR(36) NOT NULL," +
                    "`coord` mediumtext NOT NULL," +
                    "`type` mediumtext NOT NULL," +
                    "`structure_id` int(11) DEFAULT 0," +
                    "PRIMARY KEY (`uuid`)" + ")";

            SQLController.makeTable(table_create);
            CivLog.info("Created " + TABLE_NAME + " table");
        } else {
            CivLog.info(TABLE_NAME + " table OK!");
        }
    }

    @Override
    public void load(ResultSet rs) throws SQLException {
        this.setUUID(UUID.fromString(rs.getString("uuid")));
        this.coord = new BlockCoord(rs.getString("coord"));
        this.type = Type.valueOf(rs.getString("type"));
        //	int structure_id = rs.getInt("structure_id");
//		if (structure_id == 0) {
//			this.owner = null;
//		} else {
//			this.owner = CivGlobal.getStructureById(structure_id);
//		}
    }

    @Override
    public void save() {
        SQLUpdate.add(this);
    }

    @Override
    public void saveNow() throws SQLException {
        HashMap<String, Object> hashmap = new HashMap<>();

        hashmap.put("coord", this.coord.toString());
        hashmap.put("type", this.type.name());

//		if (this.owner == null) {
//			hashmap.put("structure_id", 0);		
//		} else {
//			hashmap.put("structure_id", this.owner.getId());		
//		}
//		
        SQLController.updateNamedObject(this, hashmap, TABLE_NAME);
    }

    @Override
    public void delete() {
    }

//	public Structure getOwner() {
//		return owner;
//	}
//
//	public void setOwner(Structure owner) {
//		this.owner = owner;
//	}

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public BlockCoord getCoord() {
        return coord;
    }

    public void setCoord(BlockCoord coord) {
        this.coord = coord;
    }

}
