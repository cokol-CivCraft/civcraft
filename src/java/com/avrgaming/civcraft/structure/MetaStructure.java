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
import com.avrgaming.civcraft.util.INBTSerializable;
import net.minecraft.server.v1_12_R1.NBTCompressedStreamTools;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

public abstract class MetaStructure extends Buildable implements INBTSerializable {
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
                    "`nbt` BLOB," +
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
        NBTTagCompound nbt = new NBTTagCompound();
        this.saveToNBT(nbt);
        var data = new ByteArrayOutputStream();
        try {
            NBTCompressedStreamTools.a(nbt, data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        hashmap.put("nbt", data.toByteArray());
        SQLController.updateNamedObject(this, hashmap, TABLE_NAME);
    }

    @Override
    public void saveToNBT(NBTTagCompound nbt) {
        nbt.setString("type_id", this.getConfigId());
        nbt.setString("town_uuid", this.getTown().getUUID().toString());
        nbt.setBoolean("complete", this.isComplete());
        nbt.setLong("builtBlockCount", this.getBuiltBlockCount());
        nbt.setString("cornerBlockHash", this.getCorner().toString());
        nbt.setLong("hitpoints", this.getHitpoints());
        nbt.setString("template_name", this.getSavedTemplatePath());
        nbt.setString("direction", this.dir.toString());
    }

    @Override
    public void load(ResultSet rs) throws SQLException, CivException {
        this.setId(rs.getInt("id"));
        this.setUUID(UUID.fromString(rs.getString("uuid")));
        var data = new ByteArrayInputStream(rs.getBytes("nbt"));
        NBTTagCompound nbt;
        try {
            nbt = NBTCompressedStreamTools.a(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.loadFromNBT(nbt);

        if (this.getTown() == null) {
            this.delete();
            throw new CivException("Coudln't find town ID:" + nbt.getString("town_uuid") + " for structure " + this.getDisplayName() + " ID:" + this.getUUID());
        }
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

    @Override
    public void loadFromNBT(NBTTagCompound nbt) {
        this.info = CivSettings.structures.get(nbt.getString("type_id"));
        this.setTown(Town.getTownFromUUID(UUID.fromString(nbt.getString("town_uuid"))));
        this.setCorner(new BlockCoord(nbt.getString("cornerBlockHash")));
        this.hitpoints = nbt.getInt("hitpoints");
        this.setTemplateName(nbt.getString("template_name"));
        this.dir = BlockFace.valueOf(nbt.getString("direction"));
        this.setComplete(nbt.getBoolean("complete"));
        this.setBuiltBlockCount(nbt.getInt("builtBlockCount"));
    }
}
