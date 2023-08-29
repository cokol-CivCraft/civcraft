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
package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.farm.FarmChunk;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.ChunkCoord;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class Farm extends Structure {

    public static final long GROW_RATE = CivSettings.getIntegerStructure("farm.grow_tick_rate");
    public static final int CROP_GROW_LIGHT_LEVEL = 9;
    public static final int MUSHROOM_GROW_LIGHT_LEVEL = 12;
    public static final int MAX_SUGARCANE_HEIGHT = 3;

    private FarmChunk fc = null;
    private double lastEffectiveGrowthRate = 0;

    protected Farm(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    public Farm(ResultSet rs) throws SQLException, CivException {
        super(rs);
        build_farm(this.getCorner().getLocation());
    }

    public void removeFarmChunk() {
        if (this.getCorner() != null) {
            ChunkCoord coord = new ChunkCoord(this.getCorner().getLocation());
            CivGlobal.removeFarmChunk(coord);
            CivGlobal.getSessionDB().delete_all(getSessionKey());
        }
    }

    @Override
    public void delete() throws SQLException {
        if (this.getCorner() != null) {
            ChunkCoord coord = new ChunkCoord(this.getCorner().getLocation());
            CivGlobal.removeFarmChunk(coord);
            CivGlobal.getSessionDB().delete_all(getSessionKey());
        }
        super.delete();
    }

    @Override
    public boolean canRestoreFromTemplate() {
        return false;
    }

    public void build_farm(Location centerLoc) {
        // A new farm, add it to the farm chunk table ...
        Chunk chunk = centerLoc.getChunk();
        FarmChunk fc = new FarmChunk(chunk, this.getTown(), this);
        CivGlobal.addFarmChunk(fc.getCoord(), fc);
        this.fc = fc;
    }

    public static boolean isBlockControlled(Block b) {

        return switch (b.getType()) {
            //case BROWNMUSHROOM:
            //case REDMUSHROOM:
            case COCOA, MELON, MELON_STEM, PUMPKIN, PUMPKIN_STEM, WHEAT, CARROT, POTATO, NETHER_WART ->
                //	case SUGARCANE:
                    true;
            default -> false;
        };

    }

    public void saveMissedGrowths() {

        int missedTicks = this.fc.getMissedGrowthTicks();
        TaskMaster.asyncTask(() -> {
            ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getSessionKey());

            if (entries == null || entries.isEmpty()) {
                if (missedTicks > 0) {
                    Farm.this.sessionAdd(getSessionKey(), String.valueOf(missedTicks));
                    return;
                }
            }

            if (missedTicks == 0) {
                CivGlobal.getSessionDB().delete_all(getSessionKey());
            } else {
                CivGlobal.getSessionDB().update(entries.get(0).request_id, getSessionKey(), String.valueOf(missedTicks));
            }
        }, 0);
    }

    public String getSessionKey() {
        return "FarmMissedGrowth" + ":" + this.getCorner().toString();
    }

    @Override
    public void onLoad() {
        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getSessionKey());
        int missedGrowths = 0;

        if (!entries.isEmpty()) {
            missedGrowths = Integer.parseInt(entries.get(0).value);
        }

        class AsyncTask extends CivAsyncTask {
            final int missedGrowths;

            public AsyncTask(int missedGrowths) {
                this.missedGrowths = missedGrowths;
            }


            @Override
            public void run() {
                fc.setMissedGrowthTicks(missedGrowths);
                fc.processMissedGrowths(true, this);
                saveMissedGrowths();
            }
        }

        TaskMaster.asyncTask(new AsyncTask(missedGrowths), 0);
    }

    public void setLastEffectiveGrowth(double effectiveGrowthRate) {
        this.lastEffectiveGrowthRate = effectiveGrowthRate;
    }

    public double getLastEffectiveGrowthRate() {
        return this.lastEffectiveGrowthRate;
    }
}
