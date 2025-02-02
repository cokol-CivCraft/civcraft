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

import com.avrgaming.civcraft.camp.Camp;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigTownLevel;
import com.avrgaming.civcraft.database.SQLController;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.exception.AlreadyRegisteredException;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.permission.PlotPermissions;
import com.avrgaming.civcraft.util.ChunkCoord;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.UUID;

public class TownChunk extends SQLObject {

    private ChunkCoord chunkLocation;
    private Town town;
    private boolean canUnclaim = true;

    public PlotPermissions perms = new PlotPermissions();

    public static final String TABLE_NAME = "TOWNCHUNKS";

    public TownChunk(ResultSet rs) throws SQLException, CivException {
        this.load(rs);
    }

    public TownChunk(Town newTown, Location location) {
        ChunkCoord coord = new ChunkCoord(location);
        setTown(newTown);
        setChunkCord(coord);
        perms.addGroup(newTown.getDefaultGroup());
    }

    public TownChunk(Town newTown, ChunkCoord chunkLocation) {
        setTown(newTown);
        setChunkCord(chunkLocation);
        perms.addGroup(newTown.getDefaultGroup());
    }

    public static void init() throws SQLException {
        if (!SQLController.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQLController.tb_prefix + TABLE_NAME + " (" +
                    "`uuid` VARCHAR(36) NOT NULL," +
                    "`town_uuid` VARCHAR(36) NOT NULL," +
                    "`world` VARCHAR(32) NOT NULL," +
                    "`x` bigint(20) NOT NULL," +
                    "`z` bigint(20) NOT NULL," +
                    "`owner_uuid` VARCHAR(36)," +
                    "`cc_groups` mediumtext DEFAULT NULL," +
                    "`permissions` mediumtext NOT NULL," +
                    "`canunclaim` bool DEFAULT '1'," +
                    //	 "FOREIGN KEY (owner_id) REFERENCES "+SQLController.tb_prefix+Resident.TABLE_NAME+"(id),"+
                    //	 "FOREIGN KEY (town_id) REFERENCES "+SQLController.tb_prefix+Town.TABLE_NAME+"(id),"+
                    "PRIMARY KEY (`uuid`)" + ")";

            SQLController.makeTable(table_create);
            CivLog.info("Created " + TABLE_NAME + " table");
        } else {
            CivLog.info(TABLE_NAME + " table OK!");
        }
    }

    public static TownChunk claim(Town town, ChunkCoord coord) throws CivException {
        if (CivGlobal.getTownChunk(coord) != null) {
            throw new CivException(CivSettings.localize.localizedString("town_chunk_errorClaimed"));
        }

        double cost;
        cost = getNextPlotCost(town);

        if (!town.hasEnough(cost)) {
            throw new CivException(CivSettings.localize.localizedString("var_town_chunk_claimTooPoor", cost, CivSettings.CURRENCY_NAME));
        }

        CultureChunk cultureChunk = CivGlobal.getCultureChunk(coord);
        if (cultureChunk == null || cultureChunk.getCiv() != town.getCiv()) {
            throw new CivException(CivSettings.localize.localizedString("town_chunk_claimOutsideCulture"));
        }

        TownChunk tc = new TownChunk(town, coord);
        tc.setCanUnclaim(true);

        if (!tc.isOnEdgeOfOwnership()) {
            throw new CivException(CivSettings.localize.localizedString("town_chunk_claimTooFar"));
        }

        if (!town.canClaim()) {
            throw new CivException(CivSettings.localize.localizedString("town_chunk_claimTooMany"));
        }

        //Test that we are not too close to another civ
        int min_distance = CivSettings.civConfig.getInt("civ.min_distance", 15);

        for (TownChunk cc : CivGlobal.getTownChunks()) {
            if (cc.getCiv() != town.getCiv()) {
                double dist = coord.distance(cc.getChunkCoord());
                if (dist <= min_distance) {
                    DecimalFormat df = new DecimalFormat();
                    throw new CivException(CivSettings.localize.localizedString("var_town_chunk_claimTooClose", cc.getCiv().getName(), df.format(dist), min_distance));
                }
            }
        }

        //Test that we are not too far protruding from our own town chunks
//		try {
//			int max_protrude = CivSettings.getInteger(CivSettings.townConfig, "town.max_town_chunk_protrude");
//			if (max_protrude != 0) {
//				if (isTownChunkProtruding(tc, 0, max_protrude, new HashSet<ChunkCoord>())) {
//					throw new CivException("You cannot claim here, too far away from the rest of your town chunks.");
//				}
//			}
//		} catch (InvalidConfiguration e1) {
//			e1.printStackTrace();
//			throw new CivException("Internal configuration exception.");
//		}

        try {
            town.addTownChunk(tc);
        } catch (AlreadyRegisteredException e1) {
            e1.printStackTrace();
            throw new CivException(CivSettings.localize.localizedString("internalCommandException"));

        }

        Camp camp = CivGlobal.getCampFromChunk(coord);
        if (camp != null) {
            CivMessage.sendCamp(camp, String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.localize.localizedString("var_town_chunk_dibandCamp", town.getName()));
            camp.disband();
        }

        tc.save();
        town.withdraw(cost);
        CivGlobal.addTownChunk(tc);
        CivGlobal.processCulture();
        return tc;
    }

    @Override
    public void save() {
        SQLUpdate.add(this);
    }

    @Override
    public void saveNow() throws SQLException {
        HashMap<String, Object> hashmap = new HashMap<>();

        hashmap.put("town_uuid", this.getTown().getUUID().toString());
        hashmap.put("world", this.getChunkCoord().getWorldname());
        hashmap.put("x", this.getChunkCoord().getX());
        hashmap.put("z", this.getChunkCoord().getZ());
        hashmap.put("permissions", perms.getSaveString());

        if (this.perms.getOwner() != null) {
            hashmap.put("owner_uuid", this.perms.getOwner().getUUID().toString());
        } else {
            hashmap.put("owner_uuid", NULL_UUID.toString());
        }

        if (!this.perms.getGroups().isEmpty()) {
            StringBuilder out = new StringBuilder();
            for (PermissionGroup grp : this.perms.getGroups()) {
                out.append(grp.getUUID()).append(":");
            }
            hashmap.put("cc_groups", out.toString());
        } else {
            hashmap.put("cc_groups", null);
        }

        SQLController.updateNamedObject(this, hashmap, TABLE_NAME);
    }

    public Town getTown() {
        return town;
    }

    public void setTown(Town town) {
        this.town = town;
    }

    public ChunkCoord getChunkCoord() {
        return chunkLocation;
    }

    public void setChunkCord(ChunkCoord chunkLocation) {
        this.chunkLocation = chunkLocation;
    }

    public static double getNextPlotCost(Town town) {

        ConfigTownLevel effectiveTownLevel = CivSettings.townLevels.get(CivSettings.townLevels.size());
        int currentPlotCount = town.getTownChunks().size();

        for (ConfigTownLevel lvl : CivSettings.townLevels.values()) {
            if (currentPlotCount < lvl.plots) {
                if (effectiveTownLevel.plots > lvl.plots) {
                    effectiveTownLevel = lvl;
                }
            }
        }


        return effectiveTownLevel.plot_cost;
    }

    public static TownChunk claim(Town town, Player player) throws CivException {
        double cost = getNextPlotCost(town);
        TownChunk tc = claim(town, new ChunkCoord(player.getLocation()));
        CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_town_chunk_success", tc.getChunkCoord(), String.valueOf(ChatColor.YELLOW) + cost + ChatColor.GREEN, CivSettings.CURRENCY_NAME));
        return tc;
    }

    @Override
    public void load(ResultSet rs) throws SQLException, CivException {
        this.setUUID(UUID.fromString(rs.getString("uuid")));
        this.setTown(Town.getTownFromUUID(UUID.fromString(rs.getString("town_uuid"))));
        if (this.getTown() == null) {
            CivLog.warning("TownChunk tried to load without a town...");
            if (CivGlobal.testFileFlag("cleanupDatabase")) {
                CivLog.info("CLEANING");
                this.delete();
            }
            throw new CivException("No town(" + rs.getInt("town_uuid") + ") to load this town chunk(" + rs.getInt("id"));
        }

        ChunkCoord cord = new ChunkCoord(rs.getString("world"), rs.getInt("x"), rs.getInt("z"));
        this.setChunkCord(cord);

        this.perms.loadFromSaveString(town, rs.getString("permissions"));

        this.perms.setOwner(CivGlobal.getResidentFromUUID(UUID.fromString(rs.getString("owner_uuid"))));
        //this.perms.setGroup(CivGlobal.getPermissionGroup(this.getTown(), rs.getInt("groups")));
        String grpString = rs.getString("cc_groups");
        if (grpString != null) {
            String[] groups = grpString.split(":");
            for (String grp : groups) {
                this.perms.addGroup(this.getTown().getGroupFromUUID(UUID.fromString(grp)));
            }
        }

        this.setCanUnclaim(rs.getBoolean("canunclaim"));

        try {
            this.getTown().addTownChunk(this);
        } catch (AlreadyRegisteredException e1) {
            e1.printStackTrace();
        }

    }


    /* Returns true if this townchunk is outside our protrude limits. */
//	private static boolean isTownChunkProtruding(TownChunk start, int protrude_count, int max_protrude, 
//			HashSet<ChunkCoord> closedList) {
//		
//		if (protrude_count > max_protrude) {
//			return true;
//		}
//		
//		int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
//		ChunkCoord coord = new ChunkCoord(start.getChunkCoord().getWorldname(), 
//				start.getChunkCoord().getX(), start.getChunkCoord().getZ());
//		closedList.add(coord);
//		
//		TownChunk nextChunk = null;
//		for (int i = 0; i < 4; i++) {
//			coord.setX(coord.getX() + offset[i][0]);
//			coord.setZ(coord.getZ() + offset[i][1]);
//			
//			if (closedList.contains(coord)) {
//				continue;
//			}
//			
//			TownChunk tc = CivGlobal.getTownChunk(coord);
//			if (tc == null) {
//				continue;
//			}
//			
//			if (nextChunk == null) {
//				nextChunk = tc;
//			} else {
//				/* We found another chunk next to us, this chunk doesnt protrude. */
//				return false;
//			}
//		}
//		
//		if (nextChunk == null) {
//			/* We found no chunk next to us at all.. shouldn't happen but this chunk doesnt protrude. */
//			return false;
//		}
//		
//		return isTownChunkProtruding(nextChunk, protrude_count + 1, max_protrude, closedList);
//	}

    private Civilization getCiv() {
        return this.getTown().getCiv();
    }

    /*
     * XXX This claim is only called when a town hall is building and needs to be claimed.
     * We do not save here since its going to be saved in-order using the SQLController save in order
     * task. Also certain types of validation and cost cacluation are skipped.
     */
    public static TownChunk townHallClaim(Town town, ChunkCoord coord) throws CivException {
        //This is only called when the town hall is built and needs to be claimed.

        if (CivGlobal.getTownChunk(coord) != null) {
            throw new CivException(CivSettings.localize.localizedString("town_chunk_errorClaimed"));
        }

        TownChunk tc = new TownChunk(town, coord);

        tc.setCanUnclaim(false);

        try {
            town.addTownChunk(tc);
        } catch (AlreadyRegisteredException e1) {
            e1.printStackTrace();
            throw new CivException(CivSettings.localize.localizedString("internalCommandException"));

        }

        Camp camp = CivGlobal.getCampFromChunk(coord);
        if (camp != null) {
            CivMessage.sendCamp(camp, String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.localize.localizedString("var_town_chunk_dibandCamp", town.getName()));
            camp.disband();
        }

        CivGlobal.addTownChunk(tc);
        tc.save();
        return tc;
    }

    private boolean isOnEdgeOfOwnership() {

        int[][] offset = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int i = 0; i < 4; i++) {
            TownChunk tc = CivGlobal.getTownChunk(new ChunkCoord(this.getChunkCoord().getWorldname(),
                    this.getChunkCoord().getX() + offset[i][0],
                    this.getChunkCoord().getZ() + offset[i][1]));
            if (tc != null && tc.getTown() == this.getTown()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void delete() throws SQLException {
        SQLController.deleteNamedObject(this, TABLE_NAME);
        CivGlobal.removeTownChunk(this);
    }

    /* Called when a player enters this plot. */
    public String getOnEnterString(Player player, TownChunk fromTc) {
        String out = "";

        if (this.perms.getOwner() != null) {
            out += ChatColor.GRAY + "[" + CivSettings.localize.localizedString("town_chunk_status_owned") + " " + ChatColor.GREEN + this.perms.getOwner().getName() + ChatColor.GRAY + "]";
        }

        if (this.perms.getOwner() == null && fromTc != null && fromTc.perms.getOwner() != null) {
            out += ChatColor.GRAY + "[" + CivSettings.localize.localizedString("town_chunk_status_unowned") + "]";
        }

        return out;
    }

    public String getCenterString() {
        /*
         * Since the chunk is the floor of the block coords divided by 16.
         * The middle of the chunk is 8 more from there....
         */
        //int blockx = (this.chunkLocation.getX()*16)+8;
        //int blockz = (this.chunkLocation.getZ()*16)+8;
        //TODO work out the bugs with this.

        return this.chunkLocation.toString();
    }

    public boolean isEdgeBlock() {
        int[][] offset = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int i = 0; i < 4; i++) {
            TownChunk next = CivGlobal.getTownChunk(new ChunkCoord(this.chunkLocation.getWorldname(),
                    this.chunkLocation.getX() + offset[i][0],
                    this.chunkLocation.getZ() + offset[i][1]));
            if (next == null) {
                return true;
            }
        }

        return false;
    }

    public static void unclaim(TownChunk tc) throws CivException {

        //TODO check that its not the last chunk
        //TODO make sure that its not owned by someone else.


        tc.getTown().removeTownChunk(tc);
        try {
            tc.delete();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CivException(CivSettings.localize.localizedString("internalDatabaseException"));
        }

    }

    public void setOutpost(boolean outpost) {

    }

    public boolean getCanUnclaim() {
        return canUnclaim;
    }

    public void setCanUnclaim(boolean canUnclaim) {
        this.canUnclaim = canUnclaim;
    }


}
