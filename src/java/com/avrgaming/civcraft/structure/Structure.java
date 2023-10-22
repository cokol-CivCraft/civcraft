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
import com.avrgaming.civcraft.database.SQLController;
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
import org.bukkit.entity.Player;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Structure extends MetaStructure {
    public Structure(ResultSet rs) throws SQLException, CivException {
        super(rs);
    }

    public Structure(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    /*
     * I'm being a bit lazy here, I don't want to switch on the type id in more than one place
     * so I've overloaded this function to handle both new structures and loaded ones.
     * Either the center,id, and town are set (new structure being created now)
     * or result set is not null (structure being loaded)
     */

    public void loadComponents() {
        for (Component comp : this.attachedComponents) {
            comp.onLoad();
        }
    }

    private void saveComponents() {
        for (Component comp : this.attachedComponents) {
            comp.onSave();
        }
    }

    @Override
    public void save() {
        SQLUpdate.add(this);
    }

    public void deleteSkipUndo() throws SQLException {
        super.delete();

        if (this.getTown() == null) {
            SQLController.deleteNamedObject(this, TABLE_NAME);
        }
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


        SQLController.deleteNamedObject(this, TABLE_NAME);
    }


    @Override
    public void delete() throws SQLException {
        super.delete();

        if (this.getTown() == null) {
            SQLController.deleteNamedObject(this, TABLE_NAME);
        }
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

        SQLController.deleteNamedObject(this, TABLE_NAME);
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
        this.setCorner(new BlockCoord(centerLoc));

        checkBlockPermissionsAndRestrictions(player, centerBlock, tpl.size_x, tpl.size_y, tpl.size_z, savedLocation);
        // Before we place the blocks, give our build function a chance to work on it
        this.runOnBuildStart(centerLoc, tpl);

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


    protected void runOnBuildStart(Location centerLoc, Template tpl) throws CivException {
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
        this.loadComponents();
    }

}
