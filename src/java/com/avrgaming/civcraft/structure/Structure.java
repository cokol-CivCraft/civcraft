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

import com.avrgaming.civcraft.components.Component;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.util.BlockCoord;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Structure extends Buildable {

    public static String TABLE_NAME = "STRUCTURES";

    public Structure(Location center, String id, Town town) throws CivException {
        super(Template.getDirection(center));
        this.info = CivSettings.structures.get(id);
        this.setTown(town);
        this.setCorner(new BlockCoord(center));
        this.hitpoints = info.max_hp;

        // Disallow duplicate structures with the same hash.
        Structure struct = CivGlobal.getStructure(this.getCorner());
        if (struct != null) {
            throw new CivException(CivSettings.localize.localizedString("structure_alreadyExistsHere"));
        }
    }

    public Structure(ResultSet rs) throws SQLException, CivException {
        super(BlockFace.SOUTH);
        this.load(rs);
    }

    /*
     * I'm being a bit lazy here, I don't want to switch on the type id in more than one place
     * so I've overloaded this function to handle both new structures and loaded ones.
     * Either the center,id, and town are set (new structure being created now)
     * or result set is not null (structure being loaded)
     */
    private static Structure _newStructure(Location center, String id, Town town, ResultSet rs) throws CivException, SQLException {
        Structure struct;

        switch (id) {
            case "s_bank":
                struct = rs == null ? new Bank(center, id, town) : new Bank(rs);
                break;
            case "s_trommel":
                struct = rs == null ? new Trommel(center, id, town) : new Trommel(rs);
                break;
            case "ti_fish_hatchery":
                struct = rs == null ? new FishHatchery(center, id, town) : new FishHatchery(rs);
                break;
            case "ti_trade_ship":
                struct = rs == null ? new TradeShip(center, id, town) : new TradeShip(rs);
                break;
            case "ti_quarry":
                struct = rs == null ? new Quarry(center, id, town) : new Quarry(rs);
                break;
            case "s_mob_grinder":
                struct = rs == null ? new MobGrinder(center, id, town) : new MobGrinder(rs);
                break;
            case "s_store":
                struct = rs == null ? new Store(center, id, town) : new Store(rs);
                break;
            case "s_stadium":
                struct = rs == null ? new Stadium(center, id, town) : new Stadium(rs);
                break;
            case "ti_hospital":
                struct = rs == null ? new Hospital(center, id, town) : new Hospital(rs);
                break;
            case "s_grocer":
                struct = rs == null ? new Grocer(center, id, town) : new Grocer(rs);
                break;
            case "s_broadcast_tower":
                struct = rs == null ? new BroadcastTower(center, id, town) : new BroadcastTower(rs);
                break;
            case "s_library":
                struct = rs == null ? new Library(center, id, town) : new Library(rs);
                break;

            case "s_university":
                struct = rs == null ? new University(center, id, town) : new University(rs);
                break;

            case "s_school":
                struct = rs == null ? new School(center, id, town) : new School(rs);
                break;

            case "s_research_lab":
                struct = rs == null ? new ResearchLab(center, id, town) : new ResearchLab(rs);
                break;

            case "s_blacksmith":
                struct = rs == null ? new Blacksmith(center, id, town) : new Blacksmith(rs);
                break;

            case "s_granary":
                struct = rs == null ? new Granary(center, id, town) : new Granary(rs);
                break;

            case "ti_cottage":
                struct = rs == null ? new Cottage(center, id, town) : new Cottage(rs);
                break;
            case "s_monument":
                struct = rs == null ? new Monument(center, id, town) : new Monument(rs);
                break;
            case "s_temple":
                struct = rs == null ? new Temple(center, id, town) : new Temple(rs);
                break;
            case "ti_mine":
                struct = rs == null ? new Mine(center, id, town) : new Mine(rs);
                break;
            case "ti_farm":
                struct = rs == null ? new Farm(center, id, town) : new Farm(rs);
                break;
            case "ti_trade_outpost":
                struct = rs == null ? new TradeOutpost(center, id, town) : new TradeOutpost(rs);
                break;
            case "ti_fishing_boat":
                struct = rs == null ? new FishingBoat(center, id, town) : new FishingBoat(rs);
                break;
            case "s_townhall":
                struct = rs == null ? new TownHall(center, id, town) : new TownHall(rs);
                break;
            // Just for backwards compatibility with old typos on existing servers:
            case "s_capitol":
                struct = rs == null ? new Capitol(center, id, town) : new Capitol(rs);
                break;
            case "s_arrowship":
                struct = rs == null ? new ArrowShip(center, id, town) : new ArrowShip(rs);
                break;
            case "s_arrowtower":
                struct = rs == null ? new ArrowTower(center, id, town) : new ArrowTower(rs);
                break;
            case "s_cannonship":
                struct = rs == null ? new CannonShip(center, id, town) : new CannonShip(rs);
                break;
            case "s_cannontower":
                struct = rs == null ? new CannonTower(center, id, town) : new CannonTower(rs);
                break;
            case "s_scoutship":
                struct = rs == null ? new ScoutShip(center, id, town) : new ScoutShip(rs);
                break;
            case "s_scouttower":
                struct = rs == null ? new ScoutTower(center, id, town) : new ScoutTower(rs);
                break;
            case "s_shipyard":
                struct = rs == null ? new Shipyard(center, id, town) : new Shipyard(rs);
                break;
//            case "ti_wall":
//                struct = rs == null ? new Wall(center, id, town) : new Wall(rs);
//                break;
//            case "ti_fortifiedwall":
//                struct = rs == null ? new FortifiedWall(center, id, town) : new FortifiedWall(rs);
//                break;
//            case "ti_road":
//                struct = rs == null ? new Road(center, id, town) : new Road(rs);
//                break;
            case "s_barracks":
                struct = rs == null ? new Barracks(center, id, town) : new Barracks(rs);
                break;
            case "ti_windmill":
                struct = rs == null ? new Windmill(center, id, town) : new Windmill(rs);
                break;
            case "s_museum":
                struct = rs == null ? new Museum(center, id, town) : new Museum(rs);
                break;
            case "s_market":
                struct = rs == null ? new Market(center, id, town) : new Market(rs);
                break;
            case "s_stable":
                struct = rs == null ? new Stable(center, id, town) : new Stable(rs);
                break;
            case "ti_pasture":
                struct = rs == null ? new Pasture(center, id, town) : new Pasture(rs);
                break;
            case "ti_lighthouse":
                struct = rs == null ? new Lighthouse(center, id, town) : new Lighthouse(rs);
                break;
            case "s_teslatower":
                struct = rs == null ? new TeslaTower(center, id, town) : new TeslaTower(rs);
                break;
            default:
                // This structure is generic, just create a structure type.
                // TODO should ANY structure be generic?
                if (rs == null) {
                    struct = new Structure(center, id, town);
                } else {
                    struct = new Structure(rs);
                }
                break;
        }

        struct.loadSettings();

        if (rs == null) {
            struct.saveComponents();
        } else {
            struct.loadComponents();
        }

        return struct;
    }

    private void loadComponents() {
        for (Component comp : this.attachedComponents) {
            comp.onLoad();
        }
    }

    private void saveComponents() {
        for (Component comp : this.attachedComponents) {
            comp.onSave();
        }
    }

    /*
     * Public interfaces to _newStructure.
     */
    public static Structure newStructure(ResultSet rs) throws CivException, SQLException {
        return _newStructure(null, rs.getString("type_id"), null, rs);
    }

    public static Structure newStructure(Location center, String id, Town town) throws CivException {
        try {
            return _newStructure(center, id, town, null);
        } catch (SQLException e) {
            //This should never happen here..
            e.printStackTrace();
            return null;
        }
    }


    public static void init() throws SQLException {
        if (!SQL.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME + " (" +
                    "`id` int(11) unsigned NOT NULL auto_increment," +
                    "`type_id` mediumtext NOT NULL," +
                    "`town_id` int(11) DEFAULT NULL," +
                    "`complete` bool NOT NULL DEFAULT '0'," +
                    "`builtBlockCount` int(11) DEFAULT NULL, " +
                    "`cornerBlockHash` mediumtext DEFAULT NULL," +
                    "`template_name` mediumtext DEFAULT NULL, " +
                    "`template_x` int(11) DEFAULT NULL, " +
                    "`template_y` int(11) DEFAULT NULL, " +
                    "`template_z` int(11) DEFAULT NULL, " +
                    "`hitpoints` int(11) DEFAULT '100'," +
                    "PRIMARY KEY (`id`)" + ")";

            SQL.makeTable(table_create);
            CivLog.info("Created " + TABLE_NAME + " table");
        } else {
            CivLog.info(TABLE_NAME + " table OK!");
        }
    }

    @Override
    public void load(ResultSet rs) throws SQLException, CivException {
        this.setId(rs.getInt("id"));
        this.info = CivSettings.structures.get(rs.getString("type_id"));
        this.setTown(CivGlobal.getTownFromId(rs.getInt("town_id")));

        if (this.getTown() == null) {
            //if (CivGlobal.testFileFlag("cleanupDatabase")) {
            //CivLog.info("CLEANING");
            this.delete();
            //}
            //		CivLog.warning("Coudln't find town ID:"+rs.getInt("town_id")+ " for structure "+this.getDisplayName()+" ID:"+this.getId());
            throw new CivException("Coudln't find town ID:" + rs.getInt("town_id") + " for structure " + this.getDisplayName() + " ID:" + this.getId());
            //	SQL.deleteNamedObject(this, TABLE_NAME);
            //return;
        }

        this.setCorner(new BlockCoord(rs.getString("cornerBlockHash")));
        this.hitpoints = rs.getInt("hitpoints");
        this.setTemplateName(rs.getString("template_name"));
        this.setTemplateX(rs.getInt("template_x"));
        this.setTemplateY(rs.getInt("template_y"));
        this.setTemplateZ(rs.getInt("template_z"));
        this.setComplete(rs.getBoolean("complete"));
        this.setBuiltBlockCount(rs.getInt("builtBlockCount"));


        this.getTown().addStructure(this);
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
    public void save() {
        SQLUpdate.add(this);
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
        hashmap.put("template_x", this.getTemplateX());
        hashmap.put("template_y", this.getTemplateY());
        hashmap.put("template_z", this.getTemplateZ());
        SQL.updateNamedObject(this, hashmap, TABLE_NAME);
    }

    public void deleteSkipUndo() throws SQLException {
        super.delete();

        if (this.getTown() != null) {
            /* Release trade goods if we are a trade outpost. */
            if (this instanceof TradeOutpost) {
                //TODO move to trade outpost delete..
                TradeOutpost outpost = (TradeOutpost) this;

                if (outpost.getGood() != null) {
                    outpost.getGood().setStruct(null);
                    outpost.getGood().setTown(null);
                    outpost.getGood().setCiv(null);
                    outpost.getGood().save();
                }
            }

//            if (!(this instanceof Wall || this instanceof Road)) {
                CivLog.debug("Delete with Undo! " + this.getDisplayName());
                /* Remove StructureSigns */
                for (StructureSign sign : this.getSigns()) {
                    sign.delete();
                }
                try {
                    this.undoFromTemplate();
                } catch (CivException e1) {
                    e1.printStackTrace();
                    this.fancyDestroyStructureBlocks();
                }
            CivGlobal.removeStructure(this);
            this.getTown().removeStructure(this);
            this.unbindStructureBlocks();
            if (this instanceof Farm) {
                Farm farm = (Farm) this;
                farm.removeFarmChunk();
            }
//            } else {
//                CivLog.debug("Delete skip Undo! " + this.getDisplayName());
//                CivGlobal.removeStructure(this);
//                this.getTown().removeStructure(this);
//                this.unbindStructureBlocks();
//                if (this instanceof Road) {
//                    Road road = (Road) this;
//                    road.deleteOnDisband();
//                } else {
//                    Wall wall = (Wall) this;
//                    wall.deleteOnDisband();
//                }
//            }


        }
        SQL.deleteNamedObject(this, TABLE_NAME);
    }


    @Override
    public void delete() throws SQLException {
        super.delete();

        if (this.getTown() != null) {
            /* Release trade goods if we are a trade outpost. */
            if (this instanceof TradeOutpost) {
                //TODO move to trade outpost delete..
                TradeOutpost outpost = (TradeOutpost) this;

                if (outpost.getGood() != null) {
                    outpost.getGood().setStruct(null);
                    outpost.getGood().setTown(null);
                    outpost.getGood().setCiv(null);
                    outpost.getGood().save();
                }
            }

            try {
                this.undoFromTemplate();
            } catch (CivException e1) {
                e1.printStackTrace();
                this.fancyDestroyStructureBlocks();
            }

            CivGlobal.removeStructure(this);
            this.getTown().removeStructure(this);
            this.unbindStructureBlocks();
        }

        SQL.deleteNamedObject(this, TABLE_NAME);
    }

    @Override
    public void updateBuildProgess() {
        if (this.getId() != 0) {
            HashMap<String, Object> struct_hm = new HashMap<>();
            struct_hm.put("id", this.getId());
            struct_hm.put("type_id", this.getConfigId());
            struct_hm.put("complete", this.isComplete());
            struct_hm.put("builtBlockCount", this.savedBlockCount);

            SQL.updateNamedObjectAsync(this, struct_hm, TABLE_NAME);
        }
    }

    @Override
    public void build(Player player, Location centerLoc, Template tpl) throws Exception {

        this.onPreBuild(centerLoc);

//		// Start building from the structure's template.
//		Template tpl;
//		try {
//			tpl = new Template();
//			tpl.initTemplate(centerLoc, this);
//		} catch (Exception e) {
//			unbind();
//			throw e;
//		}

        doBuild(player, centerLoc, tpl);
    }

    public void doBuild(Player player, Location centerLoc, Template tpl) throws CivException, IOException {
        // We take the player's current position and make it the 'center' by moving the center location
        // to the 'corner' of the structure.
        Location savedLocation = centerLoc.clone();
        centerLoc = repositionCenter(centerLoc, tpl.dir(), tpl.size_x, tpl.size_z);
        Block centerBlock = centerLoc.getBlock();

        this.setTotalBlockCount(tpl.size_x * tpl.size_y * tpl.size_z);
        // Save the template x,y,z for later. This lets us know our own dimensions.
        // this is saved in the db so it remains valid even if the template changes.
        this.setTemplateName(tpl.getFilepath());
        this.setTemplateX(tpl.size_x);
        this.setTemplateY(tpl.size_y);
        this.setTemplateZ(tpl.size_z);
        this.setTemplateAABB(new BlockCoord(centerLoc), tpl);

        checkBlockPermissionsAndRestrictions(player, centerBlock, tpl.size_x, tpl.size_y, tpl.size_z, savedLocation);
        // Before we place the blocks, give our build function a chance to work on it
        this.runOnBuild(centerLoc, tpl);

        // Setup undo information
        getTown().lastBuildableBuilt = this;
        tpl.saveUndoTemplate(this.getCorner().toString(), this.getTown().getName(), centerLoc);
        tpl.buildScaffolding(centerLoc);

        // Player's center was converted to this building's corner, save it as such.
        Resident resident = CivGlobal.getResident(player);
        resident.undoPreview();
        this.startBuildTask(tpl, centerLoc);


        bind();
        this.getTown().addStructure(this);

    }


    protected void runOnBuild(Location centerLoc, Template tpl) throws CivException {
        if (this.getOnBuildEvent() == null || this.getOnBuildEvent().equals("")) {
            return;
        }

        if (this.getOnBuildEvent().equals("build_farm")) {
            if (this instanceof Farm) {
                Farm farm = (Farm) this;
                farm.build_farm(centerLoc);
            }
        }

        if (this.getOnBuildEvent().equals("build_trade_outpost")) {
            if (this instanceof TradeOutpost) {
                TradeOutpost tradeoutpost = (TradeOutpost) this;
                tradeoutpost.build_trade_outpost(centerLoc);
            }
        }

    }

    public void unbind() {
        CivGlobal.removeStructure(this);
    }

    public void bind() {
        CivGlobal.addStructure(this);
    }

    @Override
    public String getDynmapDescription() {
        return null;
    }

    @Override
    public String getMarkerIconName() {
        // options at https://github.com/webbukkit/dynmap/wiki/Using-markers
        return "bighouse";
    }

    @Override
    public void processUndo() throws CivException {

        if (isTownHall()) {
            throw new CivException(CivSettings.localize.localizedString("structure_move_notCaporHall"));
        }

        try {
            delete();
            getTown().removeStructure(this);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CivException(CivSettings.localize.localizedString("internalDatabaseException"));
        }

        CivMessage.sendTown(getTown(), ChatColor.GREEN + CivSettings.localize.localizedString("var_structure_undo_success", getDisplayName()));

        double refund = this.getCost();
        this.getTown().depositDirect(refund);
        CivMessage.sendTown(getTown(), CivSettings.localize.localizedString("var_structure_undo_refund", this.getTown().getName(), refund, CivSettings.CURRENCY_NAME));

        this.unbindStructureBlocks();
    }

    public double getRepairCost() {
        return (double) (int) this.getCost() / 2;
    }

    public void onBonusGoodieUpdate() {

    }

    public void onMarkerPlacement(Player player, Location next, ArrayList<Location> locs) throws CivException {
    }

    @Override
    @Deprecated
    public String getName() {
        return this.getDisplayName();
    }

    @Override
    public void onComplete() {
        // TODO Auto-generated method stub
    }

    @Override
    public void onLoad() throws CivException {
    }

    @Override
    public void onUnload() {

    }

    public void repairStructureForFree() throws CivException {
        setHitpoints(getMaxHitPoints());
        bindStructureBlocks();

        try {
            repairFromTemplate();
        } catch (CivException | IOException e) {
            throw new CivException(CivSettings.localize.localizedString("internalIOException"));
        }
        save();
    }

    public void repairStructure() throws CivException {
        if (this instanceof TownHall) {
            throw new CivException(CivSettings.localize.localizedString("structure_repair_notCaporHall"));
        }

        double cost = getRepairCost();
        if (!getTown().getTreasury().hasEnough(cost)) {
            throw new CivException(CivSettings.localize.localizedString("var_structure_repair_tooPoor", getTown().getName(), cost, CivSettings.CURRENCY_NAME, getDisplayName()));
        }

        repairStructureForFree();

        getTown().getTreasury().withdraw(cost);
        CivMessage.sendTown(getTown(), ChatColor.YELLOW + CivSettings.localize.localizedString("var_structure_repair_success", getTown().getName(), getDisplayName(), getCorner()));
    }

    @Override
    public void loadSettings() {

        /* Build and register all of the components. */
        List<HashMap<String, String>> compInfoList = this.getComponentInfoList();
        if (compInfoList != null) {
            for (HashMap<String, String> compInfo : compInfoList) {
                String className = "com.avrgaming.civcraft.components." + compInfo.get("name");
                Class<?> someClass;
                try {
                    someClass = Class.forName(className);
                    Component compClass = (Component) someClass.newInstance();
                    compClass.setName(compInfo.get("name"));

                    for (String key : compInfo.keySet()) {
                        compClass.setAttribute(key, compInfo.get(key));
                    }

                    compClass.createComponent(this, false);
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        super.loadSettings();
    }

}
