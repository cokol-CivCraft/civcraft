package com.avrgaming.civcraft.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMobSpawner;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.util.BlockCoord;

import de.hellfirepvp.api.CustomMobsAPI;
import de.hellfirepvp.api.data.ICustomMob;
import de.hellfirepvp.api.data.ISpawnerEditor;
import de.hellfirepvp.api.data.ISpawnerEditor.SpawnerInfo;

public class MobSpawner extends SQLObject {

    private ConfigMobSpawner spawner;
    private Civilization civ;
    private BlockCoord coord;
    private int buildable = 0;
    private Boolean active;
    
    public MobSpawner(ConfigMobSpawner spawner, BlockCoord coord) {
        this.setSpawner(spawner);
        this.setCoord(coord);
        try {
            this.setName(this.getSpawner().id);
        } catch (InvalidNameException e) {
            e.printStackTrace();
        }
        this.setCiv(null);
        this.setActive(true);
    }

    public MobSpawner(ResultSet rs) throws SQLException, InvalidNameException {
        this.load(rs);
    }

    public static final String TABLE_NAME = "MOB_SPAWNERS";
    public static void init() throws SQLException {
        if (!SQL.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME+" (" + 
                    "`id` int(11) unsigned NOT NULL auto_increment," +
                    "`name` VARCHAR(64) NOT NULL," + 
                    "`buildable_id` int(11), " +
                    "`coord` mediumtext DEFAULT NULL,"+
					"`active` boolean DEFAULT true,"+
                    "PRIMARY KEY (`id`)" + ")";
            
            SQL.makeTable(table_create);
            CivLog.info("Created "+TABLE_NAME+" table");
        } else {
            CivLog.info(TABLE_NAME+" table OK!");
        }
    }

    
    @Override
    public void load(ResultSet rs) throws SQLException, InvalidNameException {
        this.setId(rs.getInt("id"));
        this.setSpawner(CivSettings.spawners.get(this.getName()));
        this.setName(rs.getString("name"));
        this.setCoord(new BlockCoord(rs.getString("coord")));
        this.setBuildable(rs.getInt("buildable_id"));
        
        this.setActive(this.getBuildable() == 0);
    }

    @Override
    public void save() {
        SQLUpdate.add(this);
    }
    
    @Override
    public void saveNow() throws SQLException {
        HashMap<String, Object> hashmap = new HashMap<String, Object>();
        
        hashmap.put("name", this.getName());
        hashmap.put("coord", this.coord.toString());
        hashmap.put("active", this.active);
        if (this.getBuildable() == 0) {
            hashmap.put("buildable_id", null);
        } else {
            hashmap.put("buildable_id", this.getBuildable());
        }
        
        SQL.updateNamedObject(this, hashmap, TABLE_NAME);
    }
    
    @Override
    public void delete() throws SQLException {      
    }

    public Civilization getCiv() {
        return civ;
    }


    public void setCiv(Civilization civ) {
        this.civ = civ;
    }


    public ConfigMobSpawner getSpawner() {
        return spawner;
    }


    public void setSpawner(ConfigMobSpawner spawner) {
        this.spawner = spawner;
    }


    public BlockCoord getCoord() {
        return coord;
    }


    public void setCoord(BlockCoord coord) {
        this.coord = coord;
    }
    
    public int getBuildable() {
        return buildable;
    }

    public void setBuildable(int buildable) {
        this.buildable = buildable;
    }

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
		ISpawnerEditor spawnerEditor = CustomMobsAPI.getSpawnerEditor();
		 if (spawnerEditor != null) {
			if (this.active) {
				SpawnerInfo spawner = spawnerEditor.getSpawner(this.getCoord().getLocation());
				if (spawner.getSpawner() != null) {
//		            CivLog.warning("Unable to create Spawner; " + spawner.toString() + " spawner exists.");
					return;
				}
		        ICustomMob mob = CustomMobsAPI.getCustomMob(this.getName());
		        if (mob == null) {

		            CivLog.warning("Unable to create Spawner; " + this.getName() + " does not exist");
		            return;
		        }
		        spawnerEditor.setSpawner(mob, this.getCoord().getLocation(), 60);
			} else {
				SpawnerInfo spawner = spawnerEditor.getSpawner(this.getCoord().getLocation());
				if (spawner.getSpawner() != null) {
		            CivLog.debug("Spawner Disabled at "+this.getCoord().getLocation());
					spawnerEditor.resetSpawner(this.getCoord().getLocation());
				}
			}
		} else {

            CivLog.warning("Unable to create Spawners; CustomMobsAPI does not exist");
        }
	}
    
    
}
