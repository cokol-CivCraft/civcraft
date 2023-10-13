package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.database.SQLController;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.util.BlockCoord;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

public abstract class MetaStructure extends Buildable {
    public static String TABLE_NAME = "STRUCTURES";

    public MetaStructure(ResultSet rs) throws SQLException, CivException {
        this.load(rs);

        if (this.hitpoints == 0) {
            this.delete();
        }
    }

    public MetaStructure(Location center, String id, Town town) throws CivException {
        this.dir = Template.getDirection(center);
        this.info = CivSettings.structures.get(id);
        this.setTown(town);
        this.setCorner(new BlockCoord(center));
        this.hitpoints = info.max_hp;

        if (this instanceof Wonder) {
            Wonder wonder = CivGlobal.getWonder(this.getCorner());
            if (wonder != null) {
                throw new CivException(CivSettings.localize.localizedString("wonder_alreadyExistsHere"));
            }
        } else {
            // Disallow duplicate structures with the same hash.
            Structure struct = CivGlobal.getStructure(this.getCorner());
            if (struct != null) {
                throw new CivException(CivSettings.localize.localizedString("structure_alreadyExistsHere"));
            }
        }
    }

    public static void init() throws SQLException {
        if (!SQLController.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQLController.tb_prefix + TABLE_NAME + " (" +
                    "`id` int(11) unsigned NOT NULL auto_increment," +
                    "`uuid` VARCHAR(36) NOT NULL," +
                    "`type_id` mediumtext NOT NULL," +
                    "`town_id` int(11) DEFAULT NULL," +
                    "`complete` bool NOT NULL DEFAULT '0'," +
                    "`builtBlockCount` int(11) DEFAULT NULL, " +
                    "`cornerBlockHash` mediumtext DEFAULT NULL," +
                    "`template_name` mediumtext DEFAULT NULL," +
                    "`direction` mediumtext DEFAULT NULL," +
                    "`hitpoints` int(11) DEFAULT '100'," +
                    "PRIMARY KEY (`id`)" + ")";

            SQLController.makeTable(table_create);
            CivLog.info("Created " + TABLE_NAME + " table");
        } else {
            CivLog.info(TABLE_NAME + " table OK!");
        }
    }

    public static MetaStructure newStructOrWonder(ResultSet rs) throws CivException, SQLException {
        String id = rs.getString("type_id");
        MetaStructure structure;
        if (CivSettings.structures.get(id) != null) {
            structure = CivSettings.structures.get(id).type.create(rs);
        } else {
            structure = StructuresTypes.BASE.create(rs);
        }
        structure.loadSettings();
        return structure;
    }

    public static MetaStructure newStructOrWonder(Location center, ConfigBuildableInfo info, Town town) throws CivException {
        MetaStructure structure = info.type.create(center, info.id, town);
        structure.loadSettings();
        return structure;
    }

    @Override
    public void saveNow() throws SQLException {
        HashMap<String, Object> hashmap = new HashMap<>();
        hashmap.put("type_id", this.getConfigId());
        hashmap.put("town_id", this.getTown().getId());
        hashmap.put("complete", this.isComplete());
        hashmap.put("builtBlockCount", this.getBuiltBlockCount());
        hashmap.put("cornerBlockHash", this.getCorner().toString());
        hashmap.put("hitpoints", this.getHitpoints());
        hashmap.put("template_name", this.getSavedTemplatePath());
        hashmap.put("direction", this.dir.toString());
        SQLController.updateNamedObject(this, hashmap, TABLE_NAME);
    }

    @Override
    public void updateBuildProgess() {
        if (this.getId() == 0) {
            return;
        }
        HashMap<String, Object> struct_hm = new HashMap<>();
        struct_hm.put("id", this.getId());
        struct_hm.put("type_id", this.getConfigId());
        struct_hm.put("complete", this.isComplete());
        struct_hm.put("builtBlockCount", this.savedBlockCount);

        SQLController.updateNamedObjectAsync(this, struct_hm, TABLE_NAME);
    }

    @Override
    public void load(ResultSet rs) throws SQLException, CivException {
        this.setId(rs.getInt("id"));
        this.setUUID(UUID.fromString(rs.getString("uuid")));
        this.info = CivSettings.structures.get(rs.getString("type_id"));

        this.setTown(CivGlobal.getTownFromId(rs.getInt("town_id")));

        if (this.getTown() == null) {
            this.delete();
            throw new CivException("Coudln't find town ID:" + rs.getInt("town_id") + " for structure " + this.getDisplayName() + " ID:" + this.getId());
        }

        this.setCorner(new BlockCoord(rs.getString("cornerBlockHash")));
        this.hitpoints = rs.getInt("hitpoints");
        this.setTemplateName(rs.getString("template_name"));
        this.dir = BlockFace.valueOf(rs.getString("direction"));
        this.setComplete(rs.getBoolean("complete"));
        this.setBuiltBlockCount(rs.getInt("builtBlockCount"));
        if (this instanceof Wonder) {
            this.getTown().addWonder(this);
        } else {
            this.getTown().addStructure((Structure) this);
        }
        bindStructureBlocks();

        if (!this.isComplete()) {
            try {
                this.resumeBuildFromTemplate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
