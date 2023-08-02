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
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.database.SQLController;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.wonders.Wonder;
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
        this.dir = Template.getDirection(center);
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
        this.load(rs);
    }

    /*
     * I'm being a bit lazy here, I don't want to switch on the type id in more than one place
     * so I've overloaded this function to handle both new structures and loaded ones.
     * Either the center,id, and town are set (new structure being created now)
     * or result set is not null (structure being loaded)
     */

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

    public static Structure newStructure(ResultSet rs) throws CivException, SQLException {
        String id = rs.getString("type_id");
        Structure struct = switch (id) {
            case "s_bank" -> new Bank(rs);
            case "s_trommel" -> new Trommel(rs);
            case "ti_fish_hatchery" -> new FishHatchery(rs);
            case "ti_trade_ship" -> new TradeShip(rs);
            case "ti_quarry" -> new Quarry(rs);
            case "s_mob_grinder" -> new MobGrinder(rs);
            case "s_store" -> new Store(rs);
            case "s_stadium" -> new Stadium(rs);
            case "ti_hospital" -> new Hospital(rs);
            case "s_grocer" -> new Grocer(rs);
            case "s_broadcast_tower" -> new BroadcastTower(rs);
            case "s_library" -> new Library(rs);
            case "s_university" -> new University(rs);
            case "s_school" -> new School(rs);
            case "s_research_lab" -> new ResearchLab(rs);
            case "s_blacksmith" -> new Blacksmith(rs);
            case "s_granary" -> new Granary(rs);
            case "ti_cottage" -> new Cottage(rs);
            case "s_monument" -> new Monument(rs);
            case "s_temple" -> new Temple(rs);
            case "ti_mine" -> new Mine(rs);
            case "ti_farm" -> new Farm(rs);
            case "ti_trade_outpost" -> new TradeOutpost(rs);
            case "ti_fishing_boat" -> new FishingBoat(rs);
            case "s_townhall" -> new TownHall(rs);
            case "s_capitol" -> new Capitol(rs);
            case "s_arrowship" -> new ArrowShip(rs);
            case "s_arrowtower" -> new ArrowTower(rs);
            case "s_cannonship" -> new CannonShip(rs);
            case "s_cannontower" -> new CannonTower(rs);
            case "s_scoutship" -> new ScoutShip(rs);
            case "s_scouttower" -> new ScoutTower(rs);
            case "s_shipyard" -> new Shipyard(rs);
            case "s_barracks" -> new Barracks(rs);
            case "ti_windmill" -> new Windmill(rs);
            case "s_museum" -> new Museum(rs);
            case "s_market" -> new Market(rs);
            case "s_stable" -> new Stable(rs);
            case "ti_pasture" -> new Pasture(rs);
            case "ti_lighthouse" -> new Lighthouse(rs);
            case "s_teslatower" -> new TeslaTower(rs);
            default -> new Structure(rs);
        };

        struct.loadSettings();
        struct.loadComponents();
        return struct;
    }

    public static Structure newStructure(Location center, String id, Town town) throws CivException {
        Structure struct = switch (id) {
            case "s_bank" -> new Bank(center, id, town);
            case "s_trommel" -> new Trommel(center, id, town);
            case "ti_fish_hatchery" -> new FishHatchery(center, id, town);
            case "ti_trade_ship" -> new TradeShip(center, id, town);
            case "ti_quarry" -> new Quarry(center, id, town);
            case "s_mob_grinder" -> new MobGrinder(center, id, town);
            case "s_store" -> new Store(center, id, town);
            case "s_stadium" -> new Stadium(center, id, town);
            case "ti_hospital" -> new Hospital(center, id, town);
            case "s_grocer" -> new Grocer(center, id, town);
            case "s_broadcast_tower" -> new BroadcastTower(center, id, town);
            case "s_library" -> new Library(center, id, town);
            case "s_university" -> new University(center, id, town);
            case "s_school" -> new School(center, id, town);
            case "s_research_lab" -> new ResearchLab(center, id, town);
            case "s_blacksmith" -> new Blacksmith(center, id, town);
            case "s_granary" -> new Granary(center, id, town);
            case "ti_cottage" -> new Cottage(center, id, town);
            case "s_monument" -> new Monument(center, id, town);
            case "s_temple" -> new Temple(center, id, town);
            case "ti_mine" -> new Mine(center, id, town);
            case "ti_farm" -> new Farm(center, id, town);
            case "ti_trade_outpost" -> new TradeOutpost(center, id, town);
            case "ti_fishing_boat" -> new FishingBoat(center, id, town);
            case "s_townhall" -> new TownHall(center, id, town);
            case "s_capitol" -> new Capitol(center, id, town);
            case "s_arrowship" -> new ArrowShip(center, id, town);
            case "s_arrowtower" -> new ArrowTower(center, id, town);
            case "s_cannonship" -> new CannonShip(center, id, town);
            case "s_cannontower" -> new CannonTower(center, id, town);
            case "s_scoutship" -> new ScoutShip(center, id, town);
            case "s_scouttower" -> new ScoutTower(center, id, town);
            case "s_shipyard" -> new Shipyard(center, id, town);
            case "s_barracks" -> new Barracks(center, id, town);
            case "ti_windmill" -> new Windmill(center, id, town);
            case "s_museum" -> new Museum(center, id, town);
            case "s_market" -> new Market(center, id, town);
            case "s_stable" -> new Stable(center, id, town);
            case "ti_pasture" -> new Pasture(center, id, town);
            case "ti_lighthouse" -> new Lighthouse(center, id, town);
            case "s_teslatower" -> new TeslaTower(center, id, town);
            default -> new Structure(center, id, town);
        };

        struct.loadSettings();
        struct.saveComponents();

        return struct;
    }

    public static Buildable newStructOrWonder(Location center, ConfigBuildableInfo info, Town town) throws CivException {
        if (info.isWonder) {
            return Wonder.newWonder(center, info.id, town);
        } else {
            return Structure.newStructure(center, info.id, town);
        }
    }

    public static Buildable newStructOrWonder(ResultSet rs) throws CivException, SQLException {
        ConfigBuildableInfo info = CivSettings.wonders.get(rs.getString("type_id"));
        if (info == null) {
            info = CivSettings.structures.get(rs.getString("type_id"));
        }
        if (info.isWonder) {
            return Wonder.newWonder(rs);
        } else {
            return Structure.newStructure(rs);
        }
    }


    public static void init() throws SQLException {
        if (!SQLController.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQLController.tb_prefix + TABLE_NAME + " (" +
                    "`id` int(11) unsigned NOT NULL auto_increment," +
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
            //	SQLController.deleteNamedObject(this, TABLE_NAME);
            //return;
        }

        this.setCorner(new BlockCoord(rs.getString("cornerBlockHash")));
        this.hitpoints = rs.getInt("hitpoints");
        this.setTemplateName(rs.getString("template_name"));
        this.dir = BlockFace.valueOf(rs.getString("direction"));
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
        hashmap.put("direction", this.dir.toString());
        SQLController.updateNamedObject(this, hashmap, TABLE_NAME);
    }

    public void deleteSkipUndo() throws SQLException {
        super.delete();

        if (this.getTown() != null) {
            /* Release trade goods if we are a trade outpost. */
            if (this instanceof TradeOutpost outpost) {
                //TODO move to trade outpost delete..

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
            if (this instanceof Farm farm) {
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
        SQLController.deleteNamedObject(this, TABLE_NAME);
    }


    @Override
    public void delete() throws SQLException {
        super.delete();

        if (this.getTown() != null) {
            /* Release trade goods if we are a trade outpost. */
            if (this instanceof TradeOutpost outpost) {
                //TODO move to trade outpost delete..

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

        SQLController.deleteNamedObject(this, TABLE_NAME);
    }

    @Override
    public void updateBuildProgess() {
        if (this.getId() != 0) {
            HashMap<String, Object> struct_hm = new HashMap<>();
            struct_hm.put("id", this.getId());
            struct_hm.put("type_id", this.getConfigId());
            struct_hm.put("complete", this.isComplete());
            struct_hm.put("builtBlockCount", this.savedBlockCount);

            SQLController.updateNamedObjectAsync(this, struct_hm, TABLE_NAME);
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
        if (this.getOnBuildEvent() == null || this.getOnBuildEvent().isEmpty()) {
            return;
        }

        if (this.getOnBuildEvent().equals("build_farm")) {
            if (this instanceof Farm farm) {
                farm.build_farm(centerLoc);
            }
        }

        if (this.getOnBuildEvent().equals("build_trade_outpost")) {
            if (this instanceof TradeOutpost tradeoutpost) {
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
