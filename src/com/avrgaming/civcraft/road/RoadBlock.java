package com.avrgaming.civcraft.road;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.BuildableDamageBlock;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.SQLObject;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.StructureBlockHitEvent;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class RoadBlock extends SQLObject implements BuildableDamageBlock {
    private BlockCoord coord;
    private Road road;
    private boolean aboveRoadBlock = false;
    private Material oldType;
    private int oldData;

    public static final String TABLE_NAME = "ROADBLOCKS";


    public RoadBlock(ResultSet rs) throws SQLException, CivException {
        this.load(rs);
    }

    public RoadBlock(Material oldType, int oldData) {
        this.oldType = oldType;
        this.oldData = oldData;
    }

    public static void init() throws SQLException {
        if (!SQL.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME + " (" +
                    "`id` int(11) unsigned NOT NULL auto_increment," +
                    "`road_id` int(11) NOT NULL DEFAULT 0," +
                    "`old_type` mediumtext NOT NULL," +
                    "`old_data` int(11) NOT NULL DEFAULT 0," +
                    "`above_road` bool DEFAULT 0," +
                    "`coord` mediumtext DEFAULT NULL," +
                    "PRIMARY KEY (`id`)" + ")";

            SQL.makeTable(table_create);
            CivLog.info("Created " + TABLE_NAME + " table");
        }
    }

    @Override
    public void load(ResultSet rs) throws SQLException,
            CivException {
        this.setId(rs.getInt("id"));
        this.setRoad((Road) CivGlobal.getStructureById(rs.getInt("road_id")));
        this.oldData = rs.getInt("old_data");
        this.oldType = Material.getMaterial("old_type");
        this.aboveRoadBlock = rs.getBoolean("above_road");
        if (this.road == null) {
            int id = rs.getInt("road_id");
            this.delete();
            throw new CivException("Couldn't load road block, could not find structure:" + id);
        }

        this.setCoord(new BlockCoord(rs.getString("coord")));
        road.addRoadBlock(this);
    }

    @Override
    public void save() {
        SQLUpdate.add(this);
    }

    @Override
    public void saveNow() throws SQLException {
        HashMap<String, Object> hashmap = new HashMap<>();

        hashmap.put("road_id", this.getRoad().getId());
        hashmap.put("coord", this.getCoord().toString());
        hashmap.put("old_type", this.getOldType().toString());
        hashmap.put("old_data", this.getOldData());
        hashmap.put("above_road", this.aboveRoadBlock);

        SQL.updateNamedObject(this, hashmap, TABLE_NAME);
    }

    @Override
    public void delete() throws SQLException {
        if (this.coord != null && this.road != null) {
            road.removeRoadBlock(this);
        }

        SQL.deleteNamedObject(this, TABLE_NAME);
    }

    public Road getRoad() {
        return road;
    }

    public void setRoad(Road road) {
        this.road = road;
    }

    public BlockCoord getCoord() {
        return coord;
    }

    public void setCoord(BlockCoord coord) {
        this.coord = coord;
    }

    public boolean isAboveRoadBlock() {
        return aboveRoadBlock;
    }

    public void setAboveRoadBlock(boolean aboveRoadBlock) {
        this.aboveRoadBlock = aboveRoadBlock;
    }

    public boolean canHit() {
        Date now = new Date();

        return now.after(this.road.getNextRaidDate());
    }

    public void onHit(Player player) {
        if (canHit()) {
            TaskMaster.syncTask(new StructureBlockHitEvent(player.getName(), this.getCoord(), this, player.getWorld()), 0);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("M/dd h:mm:ss a z");
            CivMessage.send(player, CivColor.Rose + CivSettings.localize.localizedString("var_roadBlock_cannotDestroy1", this.getOwner().getCiv().getName(), sdf.format(this.road.getNextRaidDate())));
        }
    }

    @Override
    public Buildable getOwner() {
        return this.road;
    }

    @Override
    public void setOwner(Buildable owner) {
        this.road = (Road) owner;
    }

    @Override
    public Town getTown() {
        return this.road.getTown();
    }

    @Override
    public Civilization getCiv() {
        return this.road.getCiv();
    }

    @Override
    public int getX() {
        return this.coord.getX();
    }

    @Override
    public int getY() {
        return this.coord.getY();
    }

    @Override
    public int getZ() {
        return this.coord.getZ();
    }

    @Override
    public String getWorldname() {
        return this.coord.getWorldname();
    }

    @Override
    public boolean isDamageable() {
        return true;
    }

    @Override
    public void setDamageable(boolean damageable) {
        // Do nothing.
    }

    @Override
    public boolean canDestroyOnlyDuringWar() {
        return false;
    }

    @Override
    public boolean allowDamageNow(Player player) {
        return true;
    }

    public Material getOldType() {
        return oldType;
    }

    public int getOldData() {
        return oldData;
    }

}
