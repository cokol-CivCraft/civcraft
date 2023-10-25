package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.config.ConfigTownLevel;
import com.avrgaming.civcraft.database.SQLController;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.*;
import com.avrgaming.civcraft.permission.PlotPermissions;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.threading.tasks.BuildAsyncTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.BukkitObjects;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.INBTSerializable;
import com.wimbli.WorldBorder.BorderData;
import com.wimbli.WorldBorder.Config;
import net.minecraft.server.v1_12_R1.NBTCompressedStreamTools;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public abstract class MetaStructure extends Buildable implements INBTSerializable {
    public static String TABLE_NAME = "STRUCTURES";
    public int builtBlockCount = 0;
    public int savedBlockCount = 0;
    public static final double DEFAULT_HAMMERRATE = 1.0;

    public MetaStructure(int id, UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        this.setId(id);
        this.setUUID(uuid);
        loadFromNBT(nbt);

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
                    "`type_id` mediumtext NOT NULL," +
                    "`nbt` BLOB," +
                    "PRIMARY KEY (`id`)" + ")";

            SQLController.makeTable(table_create);
            CivLog.info("Created " + TABLE_NAME + " table");
        } else {
            CivLog.info(TABLE_NAME + " table OK!");
        }
    }

    public static MetaStructure newStructOrWonder(ResultSet rs) throws CivException, SQLException {
        int id = rs.getInt("id");
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        String typeId = rs.getString("type_id");
        var data = new ByteArrayInputStream(rs.getBytes("nbt"));
        NBTTagCompound nbt;
        try {
            nbt = NBTCompressedStreamTools.a(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        MetaStructure structure;
        if (CivSettings.structures.get(typeId) != null) {
            structure = CivSettings.structures.get(typeId).type.create(id, uuid, nbt);
        } else {
            structure = StructuresTypes.BASE.create(id, uuid, nbt);
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
        hashmap.put("type_id", this.getConfigId());
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

    public void resumeBuildFromTemplate() throws Exception {

        Location corner = getCorner().getLocation();

        Template tpl = new Template();
        tpl.resumeTemplate(this.getSavedTemplatePath(), this);

        this.setTotalBlockCount(tpl.size_x * tpl.size_y * tpl.size_z);

        if (this instanceof Wonder) {
            this.getTown().setCurrentWonderInProgress(this);
        } else {
            this.getTown().setCurrentStructureInProgress(this);
        }

        this.startBuildTask(tpl, corner);
    }

    protected void startBuildTask(Template tpl, Location center) {
        if (this instanceof Structure) {
            this.getTown().setCurrentStructureInProgress(this);
        } else {
            this.getTown().setCurrentWonderInProgress(this);
        }
        BuildAsyncTask task = new BuildAsyncTask(this, tpl, this.getBuildSpeed(), this.getBlocksPerTick(), center.getBlock());

        this.getTown().build_tasks.add(task);
        BukkitObjects.scheduleAsyncDelayedTask(task, 0);
    }

    public int getBuildSpeed() {
        // buildTime is in hours, we need to return milliseconds.
        boolean hour = CivSettings.civConfig.getBoolean("global.structurespeed", false);
        // We should return the number of milliseconds to wait between each block placement.
        double hoursPerBlock = (this.getHammerCost() / this.getTown().getHammers().total) / this.getTotalBlockCount();
        double millisecondsPerBlock = hour ? hoursPerBlock * 60 * 60 * 1000 : hoursPerBlock * 60 * 30 * 1000;
        millisecondsPerBlock *= this.getTown().getBuildSpeed();
        // Clip millisecondsPerBlock to 150 milliseconds.
        if (millisecondsPerBlock < 150) {
            millisecondsPerBlock = 150;
        }

        return (int) millisecondsPerBlock;
    }

    /* Checks to see if the area is covered by another structure */
    public void canBuildHere(Location center, double distance) {

        // Do not let tile improvements be built on top of each other.
        //String chunkHash = Civ.chunkHash(center.getChunk());

        //TODO Revisit for walls and farms?


    }

    public int getBlocksPerTick() {
        // We do not want the blocks to be placed faster than 500 milliseconds.
        // So in order to deal with speeds that are faster than that, we will
        // increase the number of blocks given per tick.
        double hoursPerBlock = (this.getHammerCost() / this.getTown().getHammers().total) / this.getTotalBlockCount();
        double millisecondsPerBlock = hoursPerBlock * 60 * 60 * 1000;

        // Don't let this get lower than 1 just in case to prevent any crazyiness...

        double blocks = (500 / millisecondsPerBlock);

        if (blocks < 1) {
            blocks = 1;
        }

        return (int) blocks;
    }

    protected void checkBlockPermissionsAndRestrictions(Player player, Block centerBlock, int regionX, int regionY, int regionZ, Location origin) throws CivException {

        boolean foundTradeGood = false;
        TradeOutpost tradeOutpost = null;
        boolean ignoreBorders = false;
        boolean autoClaim = this.autoClaim;

        if (this instanceof TradeOutpost) {
            tradeOutpost = (TradeOutpost) this;
        }

        //Make sure we are building this building inside of culture.
        if (isTownHall()) {
            /* Structure is a town hall, auto-claim the borders. */
            ignoreBorders = true;
        } else {
            CultureChunk cc = CivGlobal.getCultureChunk(centerBlock.getLocation());
            if (cc == null || cc.getTown().getCiv() != this.getTown().getCiv()) {
                throw new CivException(CivSettings.localize.localizedString("buildable_notInCulture"));
            }
        }

        if (isTownHall()) {
            double minDistance = CivSettings.townConfig.getDouble("town.min_town_distance", 150.0);

            for (Town town : Town.getTowns()) {
                TownHall townhall = town.getTownHall();
                if (townhall == null) {
                    continue;
                }

                double dist = townhall.getCenterLocation().distance(new BlockCoord(centerBlock));
                if (dist < minDistance) {
                    DecimalFormat df = new DecimalFormat();
                    CivMessage.sendError(player, CivSettings.localize.localizedString("var_settler_errorTooClose", town.getName(), df.format(dist), minDistance));
                    return;
                }
            }
        }

        if (this.isWaterStructure() && !this.isOnWater(centerBlock.getBiome())) {
            throw new CivException(CivSettings.localize.localizedString("var_buildable_notEnoughWater", this.getDisplayName()));

        }

        Structure struct = CivGlobal.getStructure(new BlockCoord(centerBlock));
        if (struct != null) {
            throw new CivException(CivSettings.localize.localizedString("buildable_structureExistsHere"));
        }

        ignoreBorders = this.isAllowOutsideTown();

        if (!player.isOp()) {
            validateDistanceFromSpawn(centerBlock.getLocation());
        }

        if (this.isTileImprovement()) {
            ignoreBorders = true;
            ConfigTownLevel level = CivSettings.townLevels.get(getTown().getLevel());

            int maxTileImprovements = level.tile_improvements;
            if (getTown().getBuffManager().hasBuff("buff_mother_tree_tile_improvement_bonus")) {
                maxTileImprovements *= 2;
            }
            if (getTown().getTileImprovementCount() >= maxTileImprovements) {
                throw new CivException(CivSettings.localize.localizedString("buildable_errorTILimit"));
            }

            ChunkCoord coord = new ChunkCoord(centerBlock.getLocation());
            for (Structure s : getTown().getStructures()) {
                if (!s.isTileImprovement()) {
                    continue;
                }
                ChunkCoord sCoord = new ChunkCoord(s.getCorner());
                if (sCoord.equals(coord)) {
                    throw new CivException(CivSettings.localize.localizedString("buildable_errorTIHere"));
                }
            }

        }

        TownChunk centertc = CivGlobal.getTownChunk(origin);
        if (centertc == null && !ignoreBorders) {
            throw new CivException(CivSettings.localize.localizedString("buildable_errorNotInTown"));
        }

        if (centerBlock.getLocation().getY() >= 255) {
            throw new CivException(CivSettings.localize.localizedString("buildable_errorTooHigh"));
        }

        if (centerBlock.getLocation().getY() <= 7) {
            throw new CivException(CivSettings.localize.localizedString("buildable_errorTooLow"));
        }

        if (centerBlock.getLocation().getY() < CivGlobal.minBuildHeight) {
            throw new CivException(CivSettings.localize.localizedString("cannotBuild_toofarUnderground"));
        }

        if ((regionY + centerBlock.getLocation().getBlockY()) >= 255) {
            throw new CivException(CivSettings.localize.localizedString("buildable_errorHeightLimit"));
        }

        /* Check that we're not overlapping with another structure's template outline. */
        /* XXX this needs to check actual blocks, not outlines cause thats more annoying than actual problems caused by building into each other. */

        onCheck();

//        LinkedList<RoadBlock> deletedRoadBlocks = new LinkedList<>();
        ArrayList<ChunkCoord> claimCoords = new ArrayList<>();
        for (int x = 0; x < regionX; x++) {
            for (int y = 0; y < regionY; y++) {
                for (int z = 0; z < regionZ; z++) {
                    Block b = centerBlock.getRelative(x, y, z);

                    if (b.getType() == Material.CHEST) {
                        throw new CivException(CivSettings.localize.localizedString("cannotBuild_chestInWay"));
                    }

                    TownChunk tc = CivGlobal.getTownChunk(b.getLocation());
                    if (tc == null && autoClaim) {
                        claimCoords.add(new ChunkCoord(b.getLocation()));
                    }

                    if (tc != null && !tc.perms.hasPermission(PlotPermissions.Type.DESTROY, CivGlobal.getResident(player))) {
                        // Make sure we have permission to destroy any block in this area.
                        throw new CivException(CivSettings.localize.localizedString("cannotBuild_needPermissions") + " " + b.getX() + "," + b.getY() + "," + b.getZ());
                    }

                    BlockCoord coord = new BlockCoord(b);
                    ChunkCoord chunkCoord = new ChunkCoord(coord.getLocation());

                    if (tradeOutpost == null) {
                        //not building a trade outpost, prevent protected blocks from being destroyed.
                        ProtectedBlock pb = CivGlobal.getProtectedBlock(coord);
                        if (pb != null) {
                            CivLog.debug("Type: " + pb.getType());
                            throw new CivException(CivSettings.localize.localizedString("cannotBuild_protectedInWay"));
                        }
                    } else {
                        if (CivGlobal.getTradeGood(coord) != null) {
                            // Make sure we encompass entire trade good.
                            if ((y + 3) < regionY) {
                                foundTradeGood = true;
                                tradeOutpost.setTradeGoodCoord(coord);
                            }
                        }
                    }

                    if (CivGlobal.getStructureBlock(coord) != null) {
                        throw new CivException(CivSettings.localize.localizedString("cannotBuild_structureInWay"));
                    }

                    if (CivGlobal.getFarmChunk(new ChunkCoord(coord.getLocation())) != null) {
                        throw new CivException(CivSettings.localize.localizedString("cannotBuild_farmInWay"));
                    }

                    if (CivGlobal.getCampBlock(coord) != null) {
                        throw new CivException(CivSettings.localize.localizedString("cannotBuild_structureInWay"));
                    }

                    if (CivGlobal.getBuildablesAt(coord) != null) {
                        throw new CivException(CivSettings.localize.localizedString("cannotBuild_structureHere"));
                    }

                    BorderData border = Config.Border(b.getWorld().getName());
                    if (border != null) {
                        if (!border.insideBorder(b.getLocation().getX(), b.getLocation().getZ(), Config.ShapeRound())) {
                            throw new CivException(CivSettings.localize.localizedString("cannotBuild_outsideBorder"));
                        }
                    }
                }
            }
        }

        if (tradeOutpost != null) {
            if (!foundTradeGood) {
                throw new CivException(CivSettings.localize.localizedString("buildable_errorNotOnTradeGood"));
            }
        }

        for (ChunkCoord c : claimCoords) {
            try {
                //XXX These will be added to the array list of objects to save in town.buildStructure();
                this.townChunksToSave.add(TownChunk.townHallClaim(this.getTown(), c));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /* Delete any road blocks we happen to come across. */

    }

    protected abstract void runOnBuildStart(Location centerLoc, Template tpl) throws CivException;

    public abstract void onComplete();

    public abstract void onLoad() throws CivException;

    public abstract void onUnload();

    public abstract void processUndo() throws CivException;

    public double getBuiltHammers() {
        double hoursPerBlock = (this.getHammerCost() / DEFAULT_HAMMERRATE) / this.getTotalBlockCount();
        return this.getBuiltBlockCount() * hoursPerBlock;
    }

    public double getBlocksPerHammer() {
        // no hammer cost should be instant...
        if (this.getHammerCost() == 0)
            return this.getTotalBlockCount();

        return this.getTotalBlockCount() / this.getHammerCost();
    }

    public int getBuiltBlockCount() {
        return builtBlockCount;
    }

    public void setBuiltBlockCount(int builtBlockCount) {
        this.builtBlockCount = builtBlockCount;
        this.savedBlockCount = builtBlockCount;
    }

    public void onUpdate() {
    }

    public void onPreBuild(Location centerLoc) throws CivException {
    }

    public double getHammerCost() {
        double rate = 1;
        rate -= this.getTown().getBuffManager().getEffectiveDouble(Buff.RUSH);
        if (this.isTileImprovement()) {
            rate -= this.getTown().getBuffManager().getEffectiveDouble("buff_mother_tree_tile_improvement_cost");
        }
        return rate * info.hammer_cost;
    }

    public abstract void build(Player player, Location centerLoc, Template tpl) throws Exception;
}
