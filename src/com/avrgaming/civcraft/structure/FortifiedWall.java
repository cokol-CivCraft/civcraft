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
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.listener.MarkerPlacementManager;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.*;
import com.avrgaming.civcraft.permission.PlotPermissions;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.tutorial.CivTutorial;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.global.perks.Perk;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class FortifiedWall extends Wall {

    //TODO make these configurable.
    private static int RECURSION_LIMIT;

    private static int HEIGHT;
    private static int MAX_HEIGHT;
    private static double COST_PER_SEGMENT;
    private static double MAX_SEGMENT;
    private static String TEMPLATE;

    public static void init_settings() throws InvalidConfiguration {
        HEIGHT = CivSettings.getInteger(CivSettings.warConfig, "fortified_wall.height");
        MAX_HEIGHT = CivSettings.getInteger(CivSettings.warConfig, "fortified_wall.maximum_height");
        COST_PER_SEGMENT = CivSettings.getDouble(CivSettings.warConfig, "fortified_wall.cost_per_segment");
        MAX_SEGMENT = CivSettings.getDouble(CivSettings.warConfig, "fortified_wall.max_segment");
        RECURSION_LIMIT = CivSettings.getInteger(CivSettings.warConfig, "fortified_wall.recursion_limit");
    }

    public Map<BlockCoord, WallBlock> wallBlocks = new HashMap<>();
    public HashSet<ChunkCoord> wallChunks = new HashSet<>();

    /*
     *  This is used to chain together the wall chunks built by the last operation.
     * this allows us to undo all of the walls built in a single pass.
     */
    private FortifiedWall nextWallBuilt = null;

//	private int verticalsegments = 0;

//	private HashMap<String, SimpleBlock> simpleBlocks = new HashMap<String, SimpleBlock>();

    protected FortifiedWall(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    public FortifiedWall(ResultSet rs) throws SQLException, CivException {
        super(rs);
        this.hitpoints = this.getMaxHitPoints();
    }

    @Override
    public void processUndo() {

        double refund = 0.0;
        for (WallBlock wb : wallBlocks.values()) {

            Material material = wb.getOldId();
            if (CivSettings.restrictedUndoBlocks.contains(material)) {
                continue;
            }

            Block block = wb.getCoord().getBlock();
            block.setType(wb.getOldId());
            Block block1 = wb.getCoord().getBlock();
            block1.setData((byte) (int) wb.getOldData());
            refund += COST_PER_SEGMENT;
            try {
                wb.delete();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        refund /= HEIGHT;
        refund = Math.round(refund);
        this.getTown().getTreasury().deposit(refund);
        CivMessage.sendTown(this.getTown(), CivColor.Yellow + CivSettings.localize.localizedString("wall_undoRefund") + " " + refund + " " + CivSettings.CURRENCY_NAME);
        try {
            this.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unbindStructureBlocks() {
        super.unbindStructureBlocks();
    }

    public void deleteOnDisband() throws SQLException {
        if (this.wallBlocks != null) {
            for (WallBlock wb : this.wallBlocks.values()) {
                wb.delete();
            }
        }

        if (wallChunks != null) {
            for (ChunkCoord coord : wallChunks) {
                CivGlobal.removeWallChunk(this, coord);
            }
        }
    }

    @Override
    public void delete() throws SQLException {
        if (this.wallBlocks != null) {
            for (WallBlock wb : this.wallBlocks.values()) {
                wb.delete();
            }
        }

        if (wallChunks != null) {
            for (ChunkCoord coord : wallChunks) {
                CivGlobal.removeWallChunk(this, coord);
            }
        }

        super.delete();
    }

    @Override
    public void undoFromTemplate() {

        if (this.nextWallBuilt == null) {
            for (BlockCoord coord : wallBlocks.keySet()) {
                WallBlock wb = wallBlocks.get(coord);
                Block block = coord.getBlock();
                block.setType(wb.getOldId());
                coord.getBlock().setData(wb.getOldData());
                try {
                    wb.delete();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            // Remove this wall chunk.
            ChunkCoord coord = new ChunkCoord(this.getCorner());
            CivGlobal.removeWallChunk(this, coord);
        } else {
            this.nextWallBuilt.processUndo();
        }
    }

    @Override
    public void buildPlayerPreview(Player player, Location centerLoc) throws CivException, IOException {

        /* Look for any custom template perks and ask the player if they want to use them. */
        Resident resident = CivGlobal.getResident(player);
        ArrayList<Perk> perkList = this.getTown().getTemplatePerks(this, resident, this.info);
        if (perkList.size() != 0) {
            /* Store the pending buildable. */
            resident.pendingBuildable = this;

            /* Build an inventory full of templates to select. */
            Inventory inv = Bukkit.getServer().createInventory(player, CivTutorial.MAX_CHEST_SIZE * 9);
            ItemStack infoRec = LoreGuiItem.build("Default " + this.getDisplayName(),
                    Material.WRITTEN_BOOK,
                    0, CivColor.Gold + "<Click To Build>");
            infoRec = LoreGuiItem.setAction(infoRec, "BuildWithTemplate");
            inv.addItem(infoRec);

            for (Perk perk : perkList) {
                infoRec = LoreGuiItem.build(perk.getDisplayName(),
                        perk.configPerk.type_id,
                        perk.configPerk.data, CivColor.Gold + CivSettings.localize.localizedString("loreGui_template_clickToBuild"),
                        CivColor.Gray + CivSettings.localize.localizedString("loreGui_template_providedBy") + " " + CivColor.LightBlue + perk.provider);
                infoRec = LoreGuiItem.setAction(infoRec, "BuildWithTemplate");
                infoRec = LoreGuiItem.setActionData(infoRec, "perk", perk.getIdent());
                inv.addItem(infoRec);
            }

            /* We will resume by calling buildPlayerPreview with the template when a gui item is clicked. */
            player.openInventory(inv);
            return;
        }


        Template tpl;

        tpl = new Template();
        try {
            tpl.initTemplate(centerLoc, this);
        } catch (CivException | IOException e) {
            e.printStackTrace();
            throw e;
        }

        buildPlayerPreview(player, centerLoc, tpl);
    }

    @Override
    public void buildPlayerPreview(Player player, Location centerLoc, Template tpl) throws CivException {
        this.setTEMPLATE(tpl.getTheme());

        if (!this.getTown().hasTechnology(this.getRequiredTechnology())) {
            throw new CivException(CivSettings.localize.localizedString("wall_missingTech"));
        }

        if (War.isWarTime()) {
            throw new CivException(CivSettings.localize.localizedString("wall_noBuildInWar"));
        }

        MarkerPlacementManager.addToPlacementMode(player, this, CivSettings.localize.localizedString("wall_marketHeading"));
    }


    private boolean isValidWall() {
        for (WallBlock block : this.wallBlocks.values()) {
            BlockCoord bcoord = new BlockCoord(block.getCoord());

            for (int y = 0; y < 256; y++) {
                bcoord.setY(y);

                StructureBlock sb = CivGlobal.getStructureBlock(bcoord);
                if (sb != null) {
                    if (sb.getOwner() != this) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public void onMarkerPlacement(Player player, Location next, ArrayList<Location> locs) throws CivException {
        BlockCoord first = new BlockCoord(next);
        BlockCoord second = null;

        CultureChunk cc = CivGlobal.getCultureChunk(next);
        if (cc == null || cc.getTown().getCiv() != this.getTown().getCiv()) {
            throw new CivException(CivSettings.localize.localizedString("buildable_notInCulture"));
        }

        if (locs.size() <= 1) {
            CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("wall_firstLocation"));
            return;
        }

        // Validate our locations
        if (locs.get(0).distance(locs.get(1)) > FortifiedWall.MAX_SEGMENT) {
            throw new CivException(CivSettings.localize.localizedString("var_wall_maxLength", FortifiedWall.MAX_SEGMENT));
        }


        second = new BlockCoord(locs.get(0));
        locs.clear();
        MarkerPlacementManager.removeFromPlacementMode(player, false);


        Location secondLoc = second.getLocation();
        // Setting to a new block coord so we can increment in buildWallSegment without changing the corner.
        this.setCorner(new BlockCoord(secondLoc));
        this.setComplete(true);
        this.save();

        // We should now be able to draw a line between these two block points.
        HashMap<String, SimpleBlock> simpleBlocks = new HashMap<>();
        int verticalSegments = this.buildWallSegment(player, first, second, 0, simpleBlocks, 0);

        // Pay the piper
        double cost = 0;

        cost = COST_PER_SEGMENT * simpleBlocks.size();

        cost /= HEIGHT;
        cost = Math.round(cost);
        if (!this.getTown().getTreasury().hasEnough(cost)) {

            for (WallBlock wb : this.wallBlocks.values()) {
                try {
                    wb.delete();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            this.wallBlocks.clear();

            throw new CivException(CivSettings.localize.localizedString("var_wall_cannotAfford", cost, CivSettings.CURRENCY_NAME, verticalSegments));
        }

        this.getTown().getTreasury().withdraw(cost);

        CivMessage.sendTown(this.getTown(), CivColor.Yellow + CivSettings.localize.localizedString("var_wall_buildSuccess", cost, CivSettings.CURRENCY_NAME, verticalSegments));

        // build the blocks
        for (SimpleBlock sb : simpleBlocks.values()) {
            BlockCoord bcoord = new BlockCoord(sb);
            bcoord.getBlock().setType(sb.getType());
            Block block = bcoord.getBlock();
            block.setData((byte) sb.getData());

        }

        // Add wall to town and global tables
        this.getTown().addStructure(this);
        CivGlobal.addStructure(this);
        this.getTown().lastBuildableBuilt = this;
    }

    private void validateBlockLocation(Player player, Location loc) throws CivException {
        Block b = loc.getBlock();

        if (b.getTypeId() == Material.CHEST.getId()) {
            throw new CivException(CivSettings.localize.localizedString("cannotBuild_chestInWay"));
        }

        TownChunk tc = CivGlobal.getTownChunk(b.getLocation());

        if (tc != null && !tc.perms.hasPermission(PlotPermissions.Type.DESTROY, CivGlobal.getResident(player))) {
            // Make sure we have permission to destroy any block in this area.
            throw new CivException(CivSettings.localize.localizedString("cannotBuild_needPermissions") + " " + b.getX() + "," + b.getY() + "," + b.getZ());
        }

        BlockCoord coord = new BlockCoord(b);
        //not building a trade outpost, prevent protected blocks from being destroyed.
        if (CivGlobal.getProtectedBlock(coord) != null) {
            throw new CivException(CivSettings.localize.localizedString("cannotBuild_protectedInWay"));
        }


        if (CivGlobal.getStructureBlock(coord) != null) {
            throw new CivException(CivSettings.localize.localizedString("cannotBuild_structureInWay") + " " + coord);
        }


        if (CivGlobal.getFarmChunk(new ChunkCoord(coord.getLocation())) != null) {
            throw new CivException(CivSettings.localize.localizedString("cannotBuild_farmInWay"));
        }

        if (loc.getBlockY() >= FortifiedWall.MAX_HEIGHT) {
            throw new CivException(CivSettings.localize.localizedString("wall_build_tooHigh"));
        }

        if (loc.getBlockY() < CivGlobal.minBuildHeight) {
            throw new CivException(CivSettings.localize.localizedString("cannotBuild_toofarUnderground"));
        }

        BlockCoord bcoord = new BlockCoord(loc);
        for (int y = 0; y < 256; y++) {
            bcoord.setY(y);
            StructureBlock sb = CivGlobal.getStructureBlock(bcoord);
            if (sb != null) {
                throw new CivException(CivSettings.localize.localizedString("cannotBuild_structureInWay"));
            }
        }

    }

    private SimpleBlock getBlock(int block) {
        String template = this.getTEMPLATE();
        switch (template) {
            case "atlantean":
                if (block == 0 || block == FortifiedWall.HEIGHT / 2) {
                    return new SimpleBlock(Material.PRISMARINE, CivData.DARK_PRISMARINE);
                } else {
                    return new SimpleBlock(Material.PRISMARINE, CivData.PRISMARINE_BRICKS);
                }
            case "arctic":
                if (block == 0 || block == FortifiedWall.HEIGHT / 2) {
                    return new SimpleBlock(Material.PACKED_ICE, 0);
                } else {
                    return new SimpleBlock(Material.SNOW_BLOCK, 0);
                }
            case "aztec":
                if (block == 0 || block == FortifiedWall.HEIGHT / 2) {
                    return new SimpleBlock(Material.MOSSY_COBBLESTONE, 0);
                } else {
                    return new SimpleBlock(Material.COBBLESTONE, 0);
                }
            case "cultist":
                if (block == 0 || block == FortifiedWall.HEIGHT / 2) {
                    return new SimpleBlock(Material.STAINED_CLAY, 11);
                } else {
                    return new SimpleBlock(Material.STAINED_CLAY, 3);
                }
            case "egyptian":
                if (block == 0 || block == FortifiedWall.HEIGHT / 2) {
                    return new SimpleBlock(Material.SANDSTONE, CivData.CHISELED_SANDSTONE);
                } else {
                    return new SimpleBlock(Material.SANDSTONE, CivData.SMOOTH_SANDSTONE);
                }
            case "elven":
                if (block == 0 || block == FortifiedWall.HEIGHT / 2) {
                    return new SimpleBlock(Material.LOG, 3);
                } else {
                    return new SimpleBlock(Material.LEAVES, 3);
                }
            case "hell":
                if (block == 0 || block == FortifiedWall.HEIGHT / 2) {
                    return new SimpleBlock(Material.NETHER_BRICK, 0);
                } else {
                    return new SimpleBlock(Material.NETHERRACK, 0);
                }
            case "roman":
                if (block == 0 || block == FortifiedWall.HEIGHT / 2) {
                    return new SimpleBlock(Material.QUARTZ_BLOCK, 1);
                } else {
                    return new SimpleBlock(Material.QUARTZ_BLOCK, 0);
                }
            default:
                if (block == 0 || block == FortifiedWall.HEIGHT / 2) {
                    return new SimpleBlock(Material.SMOOTH_BRICK, 0x1);
                } else {
                    return new SimpleBlock(Material.SMOOTH_BRICK, 0);
                }
        }
    }

    private void getVerticalWallSegment(Player player, Location loc, Map<String, SimpleBlock> simpleBlocks) throws CivException {
        Location tmp = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ());
        for (int i = 0; i < FortifiedWall.HEIGHT; i++) {
            SimpleBlock sb = getBlock(i);
            sb.worldname = tmp.getWorld().getName();
            sb.x = tmp.getBlockX();
            sb.y = tmp.getBlockY();
            sb.z = tmp.getBlockZ();

            validateBlockLocation(player, tmp);
            simpleBlocks.put(sb.worldname + "," + sb.x + "," + sb.y + "," + sb.z, sb);

            tmp.add(0, 1.0, 0);
        }
        tmp = new Location(loc.getWorld(), loc.getX() + 1, loc.getY(), loc.getZ());
        for (int i = 0; i < FortifiedWall.HEIGHT; i++) {
            SimpleBlock sb = getBlock(i);

            sb.worldname = tmp.getWorld().getName();
            sb.x = tmp.getBlockX();
            sb.y = tmp.getBlockY();
            sb.z = tmp.getBlockZ();

            validateBlockLocation(player, tmp);
            simpleBlocks.put(sb.worldname + "," + sb.x + "," + sb.y + "," + sb.z, sb);

            tmp.add(0, 1.0, 0);
        }
        tmp = new Location(loc.getWorld(), loc.getX() - 1, loc.getY(), loc.getZ());
        for (int i = 0; i < FortifiedWall.HEIGHT; i++) {
            SimpleBlock sb = getBlock(i);

            sb.worldname = tmp.getWorld().getName();
            sb.x = tmp.getBlockX();
            sb.y = tmp.getBlockY();
            sb.z = tmp.getBlockZ();

            validateBlockLocation(player, tmp);
            simpleBlocks.put(sb.worldname + "," + sb.x + "," + sb.y + "," + sb.z, sb);

            tmp.add(0, 1.0, 0);
        }
        tmp = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ() + 1);
        for (int i = 0; i < FortifiedWall.HEIGHT; i++) {
            SimpleBlock sb = getBlock(i);

            sb.worldname = tmp.getWorld().getName();
            sb.x = tmp.getBlockX();
            sb.y = tmp.getBlockY();
            sb.z = tmp.getBlockZ();

            validateBlockLocation(player, tmp);
            simpleBlocks.put(sb.worldname + "," + sb.x + "," + sb.y + "," + sb.z, sb);

            tmp.add(0, 1.0, 0);
        }
        tmp = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ() - 1);
        for (int i = 0; i < FortifiedWall.HEIGHT; i++) {
            SimpleBlock sb = getBlock(i);

            sb.worldname = tmp.getWorld().getName();
            sb.x = tmp.getBlockX();
            sb.y = tmp.getBlockY();
            sb.z = tmp.getBlockZ();

            validateBlockLocation(player, tmp);
            simpleBlocks.put(sb.worldname + "," + sb.x + "," + sb.y + "," + sb.z, sb);

            tmp.add(0, 1.0, 0);
        }
    }

//	private boolean inSameChunk(Location loc1, Location loc2) {
//		
//		if (loc1.getChunk().getX() == loc2.getChunk().getX()) {
//			if (loc1.getChunk().getZ() == loc2.getChunk().getZ()) {
//				return true;
//			}
//		}
//		
//		return false;
//	}

    private int buildWallSegment(Player player, BlockCoord first, BlockCoord second, int blockCount,
                                 HashMap<String, SimpleBlock> simpleBlocks, int verticalSegments) throws CivException {
        Location locFirst = first.getLocation();
        Location locSecond = second.getLocation();

        Vector dir = new Vector(locFirst.getX() - locSecond.getX(),
                locFirst.getY() - locSecond.getY(),
                locFirst.getZ() - locSecond.getZ());
        dir.normalize();
        dir.multiply(0.5);
        HashMap<String, SimpleBlock> thisWallBlocks = new HashMap<>();

        this.getTown().lastBuildableBuilt = null;

        getVerticalWallSegment(player, locSecond, thisWallBlocks);
        simpleBlocks.putAll(thisWallBlocks);
        verticalSegments++;

        double distance = locSecond.distance(locFirst);
        BlockCoord lastBlockCoord = new BlockCoord(locSecond);
        BlockCoord currentBlockCoord = new BlockCoord(locSecond);
        while (locSecond.distance(locFirst) > 1.0) {
            locSecond.add(dir);
            ChunkCoord coord = new ChunkCoord(locSecond);
            CivGlobal.addWallChunk(this, coord);

            currentBlockCoord.setFromLocation(locSecond);
            if (lastBlockCoord.equals(currentBlockCoord)) {
                continue;
            } else {
                lastBlockCoord.setFromLocation(locSecond);
            }

            blockCount++;
            if (blockCount > FortifiedWall.RECURSION_LIMIT) {
                throw new CivException(CivSettings.localize.localizedString("wall_build_recursionHalt"));
            }

            getVerticalWallSegment(player, locSecond, thisWallBlocks);
            simpleBlocks.putAll(thisWallBlocks);
            verticalSegments++;

            //Distance should always be going down, as a failsave
            //check that it is. Abort if our distance goes up.
            double tmpDist = locSecond.distance(locFirst);
            if (tmpDist > distance) {
                break;
            }
        }

        /* build the last wall segment. */
        if (!wallBlocks.containsKey(new BlockCoord(locFirst))) {
            try {
                getVerticalWallSegment(player, locFirst, thisWallBlocks);
                simpleBlocks.putAll(thisWallBlocks);
                verticalSegments++;
            } catch (CivException e) {
                CivLog.warning("Couldn't build the last wall segment, oh well.");
            }
        }

        for (SimpleBlock sb : simpleBlocks.values()) {
            BlockCoord bcoord = new BlockCoord(sb);
            Block block = bcoord.getBlock();
            Block block1 = bcoord.getBlock();
            int old_data = block1.getData();
            if (!wallBlocks.containsKey(bcoord)) {
                WallBlock wb = new WallBlock(bcoord, this, block.getType(), old_data, sb.getType(), sb.getData());

                wallBlocks.put(bcoord, wb);
                this.addStructureBlock(bcoord, true);
                wb.save();
            }
        }
        return verticalSegments;
    }

    public boolean isProtectedLocation(Location location) {
        // Destroyed walls do not protect anything.
        if (!this.isActive()) {
            return false;
        }

        // We already know this location is inside a protected chunk
        // A protected location then, is any location which has a x, z match
        // and its y is less than our structure blocks'

        for (BlockCoord coord : this.wallBlocks.keySet()) {
            Location blockLocation = coord.getLocation();

            if (location.getBlockX() == blockLocation.getBlockX() &&
                    location.getBlockZ() == blockLocation.getBlockZ()) {

                //x and z match, now check that block is 'below' us.
                if (location.getBlockY() < FortifiedWall.MAX_HEIGHT) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void repairFromTemplate() {
        this.repairStructureForFree();
    }

    @Override
    public void repairStructureForFree() {
        setHitpoints(getMaxHitPoints());
        bindStructureBlocks();

        for (WallBlock wb : this.wallBlocks.values()) {
            BlockCoord bcoord = wb.getCoord();
            Block block = bcoord.getBlock();
            block.setType(wb.getTypeId());
            Block block1 = bcoord.getBlock();
            block1.setData((byte) wb.getData());
        }

        save();
    }

    @Override
    public void repairStructure() throws CivException {
        double cost = getRepairCost();

        if (!this.isValidWall()) {
            throw new CivException(CivSettings.localize.localizedString("wall_repair_invalid"));
        }

        if (!getTown().getTreasury().hasEnough(cost)) {
            throw new CivException(CivSettings.localize.localizedString("var_wall_repair_tooPoor", cost, CivSettings.CURRENCY_NAME, getDisplayName()));
        }

        setHitpoints(this.getMaxHitPoints());
        bindStructureBlocks();

        for (WallBlock wb : this.wallBlocks.values()) {
            BlockCoord bcoord = wb.getCoord();
            Block block = bcoord.getBlock();
            block.setType(wb.getTypeId());
            Block block1 = bcoord.getBlock();
            block1.setData((byte) wb.getData());
        }

        save();
        getTown().getTreasury().withdraw(cost);
        CivMessage.sendTown(getTown(), CivColor.Yellow + CivSettings.localize.localizedString("var_wall_repair_success", getDisplayName(), getCorner().toString()));
    }

    public String getTEMPLATE() {
        return TEMPLATE;
    }

    public void setTEMPLATE(String template) {
        TEMPLATE = template;
    }

}
