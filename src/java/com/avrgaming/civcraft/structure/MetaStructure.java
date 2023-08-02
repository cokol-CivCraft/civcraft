package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.database.SQLController;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.wonders.*;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.util.BlockCoord;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

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
        if (this instanceof Wonder) {
            this.info = CivSettings.wonders.get(id);
        } else {
            this.info = CivSettings.structures.get(id);
        }
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
        MetaStructure structure = switch (id) {
            case "w_pyramid" -> new TheGreatPyramid(rs);
            case "w_greatlibrary" -> new GreatLibrary(rs);
            case "w_hanginggardens" -> new TheHangingGardens(rs);
            case "w_colossus" -> new TheColossus(rs);
            case "w_notre_dame" -> new NotreDame(rs);
            case "w_chichen_itza" -> new ChichenItza(rs);
            case "w_council_of_eight" -> new CouncilOfEight(rs);
            case "w_colosseum" -> new Colosseum(rs);
            case "w_globe_theatre" -> new GlobeTheatre(rs);
            case "w_great_lighthouse" -> new GreatLighthouse(rs);
            case "w_mother_tree" -> new MotherTree(rs);
            case "w_grand_ship_ingermanland" -> new GrandShipIngermanland(rs);
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

        structure.loadSettings();
        return structure;
    }

    public static MetaStructure newStructOrWonder(Location center, ConfigBuildableInfo info, Town town) throws CivException {
        if (info.isWonder) {
            return Wonder.newWonder(center, info.id, town);
        } else {
            return Structure.newStructure(center, info.id, town);
        }
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
    public void load(ResultSet rs) throws SQLException, CivException {
        this.setId(rs.getInt("id"));
        if (this instanceof Wonder) {
            this.info = CivSettings.wonders.get(rs.getString("type_id"));
        } else {
            this.info = CivSettings.structures.get(rs.getString("type_id"));
        }
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
