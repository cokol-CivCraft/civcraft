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
package com.avrgaming.civcraft.structure.wonders;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuff;
import com.avrgaming.civcraft.config.ConfigWonderBuff;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public abstract class Wonder extends Buildable {

    public static String TABLE_NAME = "WONDERS";
    private ConfigWonderBuff wonderBuffs = null;

    public Wonder(ResultSet rs) throws SQLException, CivException {
        this.load(rs);

        if (this.hitpoints == 0) {
            this.delete();
        }
    }

    public Wonder(Location center, String id, Town town) throws CivException {

        this.info = CivSettings.wonders.get(id);
        this.setTown(town);
        this.setCorner(new BlockCoord(center));
        this.hitpoints = info.max_hitpoints;

        // Disallow duplicate structures with the same hash.
        Wonder wonder = CivGlobal.getWonder(this.getCorner());
        if (wonder != null) {
            throw new CivException(CivSettings.localize.localizedString("wonder_alreadyExistsHere"));
        }
    }

    public void loadSettings() {
        wonderBuffs = CivSettings.wonderBuffs.get(this.getConfigId());

        if (this.isComplete() && this.isActive()) {
            this.addWonderBuffsToTown();
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
        this.info = CivSettings.wonders.get(rs.getString("type_id"));
        this.setTown(CivGlobal.getTownFromId(rs.getInt("town_id")));
        if (this.getTown() == null) {
            //CivLog.warning("Coudln't find town ID:"+rs.getInt("town_id")+ " for wonder "+this.getDisplayName()+" ID:"+this.getId());
            throw new CivException("Coudln't find town ID:" + rs.getInt("town_id") + " for wonder " + this.getDisplayName() + " ID:" + this.getId());
        }

        this.setCorner(new BlockCoord(rs.getString("cornerBlockHash")));
        this.hitpoints = rs.getInt("hitpoints");
        this.setTemplateName(rs.getString("template_name"));
        this.setTemplateX(rs.getInt("template_x"));
        this.setTemplateY(rs.getInt("template_y"));
        this.setTemplateZ(rs.getInt("template_z"));
        this.setComplete(rs.getBoolean("complete"));
        this.setBuiltBlockCount(rs.getInt("builtBlockCount"));


        this.getTown().addWonder(this);
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


    @Override
    public void delete() throws SQLException {
        super.delete();

        if (this.wonderBuffs != null) {
            for (ConfigBuff buff : this.wonderBuffs.buffs) {
                this.getTown().getBuffManager().removeBuff(buff.id);
            }
        }

        SQL.deleteNamedObject(this, TABLE_NAME);
        CivGlobal.removeWonder(this);
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

    public static boolean isWonderAvailable(String configId) {
        if (CivGlobal.isCasualMode()) {
            return true;
        }

        for (Wonder wonder : CivGlobal.getWonders()) {
            if (wonder.getConfigId().equals(configId)) {
                if (wonder.isNationalWonder()) {
                    return true;
                }
                if (wonder.isComplete()) {
                    return false;
                }
            }
        }

        return true;
    }


    @Override
    public void processUndo() throws CivException {
        this.undoFromTemplate();

        CivMessage.global(CivSettings.localize.localizedString("var_wonder_undo_broadcast", (CivColor.LightGreen + this.getDisplayName() + CivColor.White), this.getTown().getName(), this.getTown().getCiv().getName()));

        double refund = this.getCost();
        this.getTown().depositDirect(refund);
        CivMessage.sendTown(getTown(), CivSettings.localize.localizedString("var_structure_undo_refund", this.getTown().getName(), refund, CivSettings.CURRENCY_NAME));

        this.unbindStructureBlocks();

        try {
            delete();
            getTown().removeWonder(this);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CivException(CivSettings.localize.localizedString("internalDatabaseException"));
        }
    }

    @Override
    public void build(Player player, Location centerLoc, Template tpl) throws Exception {

        // We take the player's current position and make it the 'center' by moving the center location
        // to the 'corner' of the structure.
        Location savedLocation = centerLoc.clone();

        centerLoc = this.repositionCenter(centerLoc, tpl.dir(), tpl.size_x, tpl.size_z);
        Block centerBlock = centerLoc.getBlock();
        // Before we place the blocks, give our build function a chance to work on it

        this.setTotalBlockCount(tpl.size_x * tpl.size_y * tpl.size_z);
        // Save the template x,y,z for later. This lets us know our own dimensions.
        // this is saved in the db so it remains valid even if the template changes.
        this.setTemplateName(tpl.getFilepath());
        this.setTemplateX(tpl.size_x);
        this.setTemplateY(tpl.size_y);
        this.setTemplateZ(tpl.size_z);
        this.setTemplateAABB(new BlockCoord(centerLoc), tpl);

        checkBlockPermissionsAndRestrictions(player, centerBlock, tpl.size_x, tpl.size_y, tpl.size_z, savedLocation);
        this.runOnBuild(centerLoc, tpl);

        // Setup undo information
        getTown().lastBuildableBuilt = this;
        tpl.saveUndoTemplate(this.getCorner().toString(), this.getTown().getName(), centerLoc);
        tpl.buildScaffolding(centerLoc);

        // Player's center was converted to this building's corner, save it as such.
        this.startBuildTask(tpl, centerLoc);

        this.save();
        CivGlobal.addWonder(this);
        CivMessage.global(CivSettings.localize.localizedString("var_wonder_startedByCiv", this.getCiv().getName(), this.getDisplayName(), this.getTown().getName()));
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 1.25f, 0.75f);
        }
    }


    @Override
    public String getDynmapDescription() {
        return null;
    }


    @Override
    public String getMarkerIconName() {
        return "beer";
    }

    @Override
    protected void runOnBuild(Location centerLoc, Template tpl) {
    }

    public void onDestroy() {
        if (!CivGlobal.isCasualMode()) {
            //can be overriden in subclasses.
            CivMessage.global(CivSettings.localize.localizedString("var_wonder_destroyed", this.getDisplayName(), this.getTown().getName()));
            try {
                this.getTown().removeWonder(this);
                this.fancyDestroyStructureBlocks();
                this.unbindStructureBlocks();
                this.delete();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static Wonder newWonder(Location center, String id, Town town) throws CivException {
        try {
            return _newWonder(center, id, town, null);
        } catch (SQLException e) {
            // should never happen
            e.printStackTrace();
            return null;
        }
    }

    public static Wonder _newWonder(Location center, String id, Town town, ResultSet rs) throws CivException, SQLException {
        Wonder wonder;
        switch (id) {
            case "w_pyramid":
                if (rs == null) {
                    wonder = new TheGreatPyramid(center, id, town);
                } else {
                    wonder = new TheGreatPyramid(rs);
                }
                break;
            case "w_greatlibrary":
                if (rs == null) {
                    wonder = new GreatLibrary(center, id, town);
                } else {
                    wonder = new GreatLibrary(rs);
                }
                break;
            case "w_hanginggardens":
                if (rs == null) {
                    wonder = new TheHangingGardens(center, id, town);
                } else {
                    wonder = new TheHangingGardens(rs);
                }
                break;
            case "w_colossus":
                if (rs == null) {
                    wonder = new TheColossus(center, id, town);
                } else {
                    wonder = new TheColossus(rs);
                }
                break;
            case "w_notre_dame":
                if (rs == null) {
                    wonder = new NotreDame(center, id, town);
                } else {
                    wonder = new NotreDame(rs);
                }
                break;
            case "w_chichen_itza":
                if (rs == null) {
                    wonder = new ChichenItza(center, id, town);
                } else {
                    wonder = new ChichenItza(rs);
                }
                break;
            case "w_council_of_eight":
                if (rs == null) {
                    wonder = new CouncilOfEight(center, id, town);
                } else {
                    wonder = new CouncilOfEight(rs);
                }
                break;
            case "w_colosseum":
                if (rs == null) {
                    wonder = new Colosseum(center, id, town);
                } else {
                    wonder = new Colosseum(rs);
                }
                break;
            case "w_globe_theatre":
                if (rs == null) {
                    wonder = new GlobeTheatre(center, id, town);
                } else {
                    wonder = new GlobeTheatre(rs);
                }
                break;
            case "w_great_lighthouse":
                if (rs == null) {
                    wonder = new GreatLighthouse(center, id, town);
                } else {
                    wonder = new GreatLighthouse(rs);
                }
                break;
            case "w_mother_tree":
                if (rs == null) {
                    wonder = new MotherTree(center, id, town);
                } else {
                    wonder = new MotherTree(rs);
                }
                break;
            case "w_grand_ship_ingermanland":
                if (rs == null) {
                    wonder = new GrandShipIngermanland(center, id, town);
                } else {
                    wonder = new GrandShipIngermanland(rs);
                }
                break;
            case "w_battledome":
                if (rs == null) {
                    wonder = new Battledome(center, id, town);
                } else {
                    wonder = new Battledome(rs);
                }
                break;
            default:
                throw new CivException(CivSettings.localize.localizedString("wonder_unknwon_type") + " " + id);
        }

        wonder.loadSettings();
        return wonder;
    }

    public void addWonderBuffsToTown() {

        if (this.wonderBuffs == null) {
            return;
        }

        for (ConfigBuff buff : this.wonderBuffs.buffs) {
            try {
                this.getTown().getBuffManager().addBuff("wonder:" + this.getDisplayName() + ":" + this.getCorner() + ":" + buff.id,
                        buff.id, this.getDisplayName());
            } catch (CivException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onComplete() {
        addWonderBuffsToTown();
    }

    public ConfigWonderBuff getWonderBuffs() {
        return wonderBuffs;
    }


    public void setWonderBuffs(ConfigWonderBuff wonderBuffs) {
        this.wonderBuffs = wonderBuffs;
    }

    public static Wonder newWonder(ResultSet rs) throws CivException, SQLException {
        return _newWonder(null, rs.getString("type_id"), null, rs);
    }

    @Override
    public void onLoad() {
    }

    @Override
    public void onUnload() {
    }

    protected void addBuffToTown(Town town, String id) {
        try {
            town.getBuffManager().addBuff(id, id, this.getDisplayName() + " in " + this.getTown().getName());
        } catch (CivException e) {
            e.printStackTrace();
        }
    }

    protected void addBuffToCiv(Civilization civ, String id) {
        for (Town t : civ.getTowns()) {
            addBuffToTown(t, id);
        }
    }

    protected void removeBuffFromTown(Town town, String id) {
        town.getBuffManager().removeBuff(id);
    }

    protected void removeBuffFromCiv(Civilization civ, String id) {
        for (Town t : civ.getTowns()) {
            removeBuffFromTown(t, id);
        }
    }

    protected abstract void removeBuffs();

    protected abstract void addBuffs();

    public void processCoinsFromCulture() {
        int cultureCount = 0;
        for (Town t : this.getCiv().getTowns()) {
            cultureCount += t.getCultureChunks().size();
        }

        double coinsPerCulture = Double.parseDouble(CivSettings.buffs.get("buff_colossus_coins_from_culture").value);

        double total = coinsPerCulture * cultureCount;
        this.getCiv().getTreasury().deposit(total);

        CivMessage.sendCiv(this.getCiv(), CivColor.LightGreen + CivSettings.localize.localizedString("var_colossus_generatedCoins", (CivColor.Yellow + total + CivColor.LightGreen), CivSettings.CURRENCY_NAME, cultureCount));
    }

    public void processCoinsFromColosseum() {
        int townCount = 0;
        for (Civilization civ : CivGlobal.getCivs()) {
            townCount += civ.getTownCount();
        }
        double coinsPerTown = Double.parseDouble(CivSettings.buffs.get("buff_colosseum_coins_from_towns").value);

        double total = coinsPerTown * townCount;
        this.getCiv().getTreasury().deposit(total);

        CivMessage.sendCiv(this.getCiv(), CivColor.LightGreen + CivSettings.localize.localizedString("var_colosseum_generatedCoins", (CivColor.Yellow + total + CivColor.LightGreen), CivSettings.CURRENCY_NAME, townCount));
    }
}
