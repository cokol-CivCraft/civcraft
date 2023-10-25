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
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.util.BlockCoord;
import org.bukkit.block.Sign;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

public class StructureSign extends SQLObject {

    private String text;
    private Buildable owner;
    private String type;
    private String action;
    private BlockCoord coord;
    private int direction;
    private boolean allowRightClick = false;

    public StructureSign(BlockCoord coord, Buildable owner) {
        this.coord = coord;
        this.owner = owner;
    }

    public StructureSign(ResultSet rs) throws SQLException {
        load(rs);
    }

    public static String TABLE_NAME = "STRUCTURE_SIGNS";

    public static void init() throws SQLException {
        if (!SQLController.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQLController.tb_prefix + TABLE_NAME + " (" +
                    "`uuid` VARCHAR(36) NOT NULL," +
                    "`text` mediumtext, " +
                    "`structure_uuid` VARCHAR(36), " +
                    "`wonder_uuid` VARCHAR(36)," +
                    "`type` mediumtext, " +
                    "`action` mediumtext, " +
                    "`coordHash` mediumtext, " +
                    "`direction` int(11), " +
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
        this.text = rs.getString("text");
        this.action = rs.getString("action");
        this.type = rs.getString("type");
        UUID structure_uuid = UUID.fromString(rs.getString("structure_uuid"));
        UUID wonder_uuid = UUID.fromString(rs.getString("wonder_uuid"));
        this.owner = null;

        if (!structure_uuid.equals(NULL_UUID)) {
            this.owner = CivGlobal.getStructureByUUID(structure_uuid);
        } else if (!wonder_uuid.equals(NULL_UUID)) {
            this.owner = CivGlobal.getWonderByUUID(wonder_uuid);
        }


        this.coord = new BlockCoord(rs.getString("coordHash"));
        this.direction = rs.getInt("direction");

        if (this.owner != null) {
            owner.addStructureSign(this);
        }
    }

    @Override
    public void save() {
        SQLUpdate.add(this);
    }

    @Override
    public void saveNow() throws SQLException {
        HashMap<String, Object> hashmap = new HashMap<>();
        hashmap.put("text", this.getText());

        if (this.owner == null) {
            hashmap.put("structure_uuid", NULL_UUID.toString());
            hashmap.put("wonder_uuid", NULL_UUID.toString());
        } else if (this.owner instanceof Structure) {
            hashmap.put("structure_uuid", owner.getUUID().toString());
            hashmap.put("wonder_uuid", NULL_UUID.toString());
        } else if (this.owner instanceof Wonder) {
            hashmap.put("structure_uuid", NULL_UUID.toString());
            hashmap.put("wonder_uuid", owner.getUUID().toString());
        }

        hashmap.put("type", this.getType());
        hashmap.put("action", this.getAction());
        hashmap.put("coordHash", this.coord.toString());
        hashmap.put("direction", this.direction);

        SQLController.updateNamedObject(this, hashmap, TABLE_NAME);
    }

    @Override
    public void delete() throws SQLException {
        SQLController.deleteNamedObject(this, TABLE_NAME);
        CivGlobal.removeStructureSign(this);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Buildable getOwner() {
        return owner;
    }

    public void setOwner(Buildable owner) {
        this.owner = owner;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public BlockCoord getCoord() {
        return coord;
    }

    public void setCoord(BlockCoord coord) {
        this.coord = coord;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public void setText(String[] message) {
        this.text = String.join("\n", message);
    }

    public void update() {
        if (!(coord.getBlock().getState() instanceof Sign sign)) {
            return;
        }
        String[] lines = this.text.split("\\n");

        for (int i = 0; i < 4; i++) {
            if (i < lines.length) {
                sign.setLine(i, lines[i]);
            } else {
                sign.setLine(i, "");
            }
        }
        sign.update();
    }

    public boolean isAllowRightClick() {
        return allowRightClick;
    }

    public void setAllowRightClick(boolean allowRightClick) {
        this.allowRightClick = allowRightClick;
    }

}