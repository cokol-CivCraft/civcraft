/**
 * AVRGAMING LLC
 * <p></p>
 * __________________
 * <p></p>
 * [2013] AVRGAMING LLC
 * All Rights Reserved.
 * <p></p>
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
package com.avrgaming.civcraft.camp;

import com.avrgaming.civcraft.components.ConsumeLevelComponent;
import com.avrgaming.civcraft.components.ConsumeLevelComponent.Result;
import com.avrgaming.civcraft.components.SifterComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigCampLonghouseLevel;
import com.avrgaming.civcraft.config.ConfigCampUpgrade;
import com.avrgaming.civcraft.database.SQLController;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.items.components.Tagged;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.*;
import com.avrgaming.civcraft.permission.PlotPermissions;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.template.Template.TemplateType;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.PostBuildSyncTask;
import com.avrgaming.civcraft.util.*;
import com.avrgaming.civcraft.util.SimpleBlock.Type;
import gpl.AttributeUtil;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Door;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class Camp extends Buildable {

    private String ownerName;
    private int hitpoints;
    private int firepoints;
    private BlockCoord corner;

    private final HashMap<String, Resident> members = new HashMap<>();
    public static final double SHIFT_OUT = 2;
    public static final String SUBDIR = "camp";
    private boolean undoable = false;

    /* Locations that exhibit vanilla growth */
    public HashSet<BlockCoord> growthLocations = new HashSet<>();
    private boolean gardenEnabled = false;

    /* Camp blocks on this structure. */
    public HashMap<BlockCoord, CampBlock> campBlocks = new HashMap<>();

    /* Fire locations for the firepit. */
    public HashMap<Integer, BlockCoord> firepitBlocks = new HashMap<>();
    public HashSet<BlockCoord> fireFurnaceBlocks = new HashSet<>();
    private Integer coal_per_firepoint;
    private Integer maxFirePoints;

    /* Sifter Component */
    public SifterComponent sifter = new SifterComponent();
    public ReentrantLock sifterLock = new ReentrantLock();
    private boolean sifterEnabled = false;

    /* Longhouse Stuff. */
    public HashSet<BlockCoord> foodDepositPoints = new HashSet<>();
    public ConsumeLevelComponent consumeComponent;
    private boolean longhouseEnabled = false;

    /* Doors we protect. */
    public HashSet<BlockCoord> doors = new HashSet<>();


    /* Control blocks */
    public HashMap<BlockCoord, ControlPoint> controlBlocks = new HashMap<>();

    private Date nextRaidDate;
    private int raidLength;

    private final HashMap<String, ConfigCampUpgrade> upgrades = new HashMap<>();

    public static void newCamp(Resident resident, Player player, String name) {

        TaskMaster.syncTask(() -> {
            try {
                Camp existCamp = CivGlobal.getCamp(name);
                if (existCamp != null) {
                    throw new CivException("(" + name + ") " + CivSettings.localize.localizedString("camp_nameTaken"));
                }

                LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(player.getInventory().getItemInMainHand());
                if (craftMat == null || !craftMat.hasComponent("FoundCamp")) {
                    throw new CivException(CivSettings.localize.localizedString("camp_missingItem"));
                }

                Camp camp = new Camp(resident, name, player.getLocation());
                camp.buildCamp(player, player.getLocation());
                camp.setUndoable(true);
                CivGlobal.addCamp(camp);
                camp.save();

                CivMessage.sendSuccess(player, CivSettings.localize.localizedString("camp_createSuccess"));
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                resident.clearInteractiveMode();
            } catch (CivException e) {
                CivMessage.sendError(player, e.getMessage());
            }
        });
    }

    public Camp(Resident owner, String name, Location corner) throws CivException {
        this.ownerName = owner.getUUID().toString();
        this.corner = new BlockCoord(corner);
        try {
            this.setName(name);
        } catch (InvalidNameException e1) {
            //e1.printStackTrace();
            throw new CivException("Invalid name, please choose another.");
        }
        nextRaidDate = new Date();
        nextRaidDate.setTime(nextRaidDate.getTime() + 24 * 60 * 60 * 1000);

        this.firepoints = CivSettings.campConfig.getInt("camp.firepoints", 24);
        this.hitpoints = CivSettings.campConfig.getInt("camp.hitpoints", 5000);
        loadSettings();
    }

    public Camp(ResultSet rs) throws SQLException, InvalidNameException {
        this.load(rs);
        loadSettings();
    }

    @Override
    public void loadSettings() {
        coal_per_firepoint = CivSettings.campConfig.getInt("camp.coal_per_firepoint", 4);
        maxFirePoints = CivSettings.campConfig.getInt("camp.firepoints", 24);

        // Setup sifter

        raidLength = CivSettings.campConfig.getInt("camp.raid_length", 2);

        sifter.addSiftItem(Material.COBBLESTONE, (short) 0, CivSettings.campConfig.getDouble("camp.sifter_gold_nugget_chance", 0.10), Material.GOLD_NUGGET, (short) 0, 1);
        sifter.addSiftItem(Material.COBBLESTONE, (short) 0, CivSettings.campConfig.getDouble("camp.sifter_iron_ingot_chance", 0.025), Material.IRON_INGOT, (short) 0, 1);
        sifter.addSiftItem(Material.COBBLESTONE, (short) 0, 1.0, Material.GRAVEL, (short) 0, 1);

        consumeComponent = new ConsumeLevelComponent();
        consumeComponent.setBuildable(this);
        for (ConfigCampLonghouseLevel lvl : CivSettings.longhouseLevels.values()) {
            consumeComponent.addLevel(lvl.level, lvl.count);
            consumeComponent.setConsumes(lvl.level, lvl.consumes);
        }
        this.consumeComponent.onLoad();

    }

    public static final String TABLE_NAME = "CAMPS";

    public static void init() throws SQLException {
        if (!SQLController.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQLController.tb_prefix + TABLE_NAME + " (" +
                    "`id` int(11) unsigned NOT NULL auto_increment," +
                    "`name` VARCHAR(64) NOT NULL," +
                    "`owner_name` mediumtext NOT NULL," +
                    "`firepoints` int(11) DEFAULT 0," +
                    "`next_raid_date` long," +
                    "`corner` mediumtext," +
                    "`upgrades` mediumtext," +
                    "`template_name` mediumtext," +
                    "PRIMARY KEY (`id`)" + ")";

            SQLController.makeTable(table_create);
            CivLog.info("Created " + TABLE_NAME + " table");
        }
    }


    @Override
    public void load(ResultSet rs) throws SQLException, InvalidNameException {
        this.setId(rs.getInt("id"));
        this.setName(rs.getString("name"));
        this.ownerName = rs.getString("owner_name");

        this.corner = new BlockCoord(rs.getString("corner"));
        this.nextRaidDate = new Date(rs.getLong("next_raid_date"));
        this.setTemplateName(rs.getString("template_name"));

        this.hitpoints = CivSettings.campConfig.getInt("camp.hitpoints", 500);

        this.firepoints = rs.getInt("firepoints");

        if (this.ownerName == null) {
            CivLog.error("COULD NOT FIND OWNER FOR CAMP ID:" + this.getId());
            return;
        }

        this.loadUpgradeString(rs.getString("upgrades"));
        this.bindCampBlocks();
    }

    @Override
    public void save() {
        SQLUpdate.add(this);

    }

    @Override
    public void saveNow() throws SQLException {
        HashMap<String, Object> hashmap = new HashMap<>();
        hashmap.put("name", this.getName());
        hashmap.put("owner_name", this.getOwner().getUUIDString());

        hashmap.put("firepoints", this.firepoints);
        hashmap.put("corner", this.corner.toString());
        hashmap.put("next_raid_date", this.nextRaidDate.getTime());
        hashmap.put("upgrades", this.getUpgradeSaveString());
        hashmap.put("template_name", this.getSavedTemplatePath());

        SQLController.updateNamedObject(this, hashmap, TABLE_NAME);
    }

    @Override
    public void delete() throws SQLException {

        for (Resident resident : this.members.values()) {
            resident.setCamp(null);
            resident.save();
        }

        this.unbindCampBlocks();
        SQLController.deleteNamedObject(this, TABLE_NAME);
        CivGlobal.removeCamp(this.getName());
    }

    public void loadUpgradeString(String upgrades) {
        String[] split = upgrades.split(",");
        for (String id : split) {

            if (id == null || id.equalsIgnoreCase("")) {
                continue;
            }
            id = id.trim();
            ConfigCampUpgrade upgrade = CivSettings.campUpgrades.get(id);
            if (upgrade == null) {
                CivLog.warning("Unknown upgrade id " + id + " during load.");
                continue;
            }

            this.upgrades.put(id, upgrade);
            upgrade.processAction(this);
        }
    }

    public String getUpgradeSaveString() {
        StringBuilder out = new StringBuilder();
        for (ConfigCampUpgrade upgrade : this.upgrades.values()) {
            out.append(upgrade.id()).append(",");
        }

        return out.toString();
    }

    public void destroy() {
        this.fancyCampBlockDestory();
        try {
            this.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void disband() {
        this.undoFromTemplate();

        try {
            this.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void undo() {
        this.undoFromTemplate();

        try {
            this.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void undoFromTemplate() {
        Template undo_tpl = new Template();
        try {
            undo_tpl.initUndoTemplate(this.getCorner().toString(), SUBDIR);
            undo_tpl.buildUndoTemplate(undo_tpl, this.getCorner().getBlock());
            undo_tpl.deleteUndoTemplate(this.getCorner().toString(), SUBDIR);

        } catch (IOException | CivException e1) {
            e1.printStackTrace();
        }
    }

    public void buildCamp(Player player, Location center) throws CivException {

        String templateFile = CivSettings.campConfig.getString("camp.template", "camp");
        Resident resident = CivGlobal.getResident(player);

        /* Load in the template. */
        Template tpl;
        if (resident.desiredTemplate == null) {
            try {
                //tpl.setDirection(center);
                String templatePath = Template.getTemplateFilePath(templateFile, TemplateType.STRUCTURE, "default");
                this.setTemplateName(templatePath);
                //tpl.load_template(templatePath);
                tpl = Template.getTemplate(templatePath, center);
            } catch (IOException | CivException e) {
                e.printStackTrace();
                return;
            }
        } else {
            tpl = resident.desiredTemplate;
            resident.desiredTemplate = null;
            this.setTemplateName(tpl.getFilepath());
        }

        corner.setFromLocation(this.repositionCenter(center, tpl.dir(), tpl.size_x, tpl.size_z));
        checkBlockPermissionsAndRestrictions(player, corner.getBlock(), tpl.size_x, tpl.size_y, tpl.size_z);
        try {
            tpl.saveUndoTemplate(this.getCorner().toString(), SUBDIR, getCorner().getLocation());
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        buildCampFromTemplate(tpl, corner);

        TaskMaster.syncTask(new PostBuildSyncTask(tpl, this));
        processCommandSigns(tpl, corner);
        try {
            this.saveNow();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CivException("Internal SQLController Error.");
        }

        this.addMember(resident);
        resident.save();

    }

    public void reprocessCommandSigns() {
        /* Load in the template. */
        //Template tpl = new Template();
        Template tpl;
        try {
            //tpl.load_template(this.getSavedTemplatePath());
            tpl = Template.getTemplate(this.getSavedTemplatePath(), null);
        } catch (IOException | CivException e) {
            e.printStackTrace();
            return;
        }

        processCommandSigns(tpl, corner);
    }

    private void processCommandSigns(Template tpl, BlockCoord corner) {
        for (BlockCoord relativeCoord : tpl.commandBlockRelativeLocations) {
            SimpleBlock sb = tpl.blocks[relativeCoord.getX()][relativeCoord.getY()][relativeCoord.getZ()];
            BlockCoord absCoord = new BlockCoord(corner.getBlock().getRelative(relativeCoord.getX(), relativeCoord.getY(), relativeCoord.getZ()));
            if (!(sb.getMaterialData() instanceof org.bukkit.material.Sign)) {
                continue;
            }
            switch (sb.command) {
                case "/gardensign" -> {
                    if (!this.gardenEnabled) {
                        absCoord.getBlock().setType(Material.SIGN);
                        Sign sign = (Sign) absCoord.getBlock().getState();
                        sign.setData(sb.getMaterialData());
                        sign.setLine(0, "Garden Disabled");
                        sign.setLine(1, "Upgrade using");
                        sign.setLine(2, "/camp upgrade");
                        sign.setLine(3, "command");
                        sign.update();
                        this.addCampBlock(absCoord);
                    } else {
                        absCoord.getBlock().setType(Material.AIR);
                        this.removeCampBlock(absCoord);
                    }
                }
                case "/growth" -> {
                    if (this.gardenEnabled) {
                        this.growthLocations.add(absCoord);
                        CivGlobal.vanillaGrowthLocations.add(absCoord);

                        absCoord.getBlock().setType(Material.SOIL);

                        this.addCampBlock(absCoord, true);
                        this.addCampBlock(new BlockCoord(absCoord.getBlock().getRelative(0, 1, 0)), true);
                    } else {
                        this.addCampBlock(absCoord);
                        this.addCampBlock(new BlockCoord(absCoord.getBlock().getRelative(0, 1, 0)));
                    }
                }
                case "/firepit" -> {
                    this.firepitBlocks.put(Integer.valueOf(sb.keyvalues.get("id")), absCoord);
                    this.addCampBlock(absCoord);
                }
                case "/fire" -> absCoord.getBlock().setType(Material.FIRE);
                case "/firefurnace" -> {
                    this.fireFurnaceBlocks.add(absCoord);
                    absCoord.getBlock().setType(Material.FURNACE);
                    BlockState state = absCoord.getBlock().getState();
                    state.setData(new org.bukkit.material.Furnace(((org.bukkit.material.Sign) sb.getMaterialData()).getFacing()));
                    state.update(true, false);
                    this.addCampBlock(absCoord);
                }
                case "/sifter" -> {
                    int id = Integer.parseInt(sb.keyvalues.get("id"));
                    switch (id) {
                        case 0 -> sifter.setSourceCoord(absCoord);
                        case 1 -> sifter.setDestCoord(absCoord);
                        default -> CivLog.warning("Unknown ID for sifter in camp:" + id);
                    }
                    if (this.sifterEnabled) {
                        BlockState state = absCoord.getBlock().getState();
                        state.setData(new org.bukkit.material.Chest(((org.bukkit.material.Sign) sb.getMaterialData()).getFacing()));
                        state.update(true, false);
                    } else {
                        try {
                            absCoord.getBlock().setType(Material.SIGN);
                            Sign sign = (Sign) absCoord.getBlock().getState();
                            sign.setData(sb.getMaterialData());
                            sign.setLine(0, CivSettings.localize.localizedString("camp_sifterUpgradeSign1"));
                            sign.setLine(1, CivSettings.localize.localizedString("upgradeUsing_SignText"));
                            sign.setLine(2, "/camp upgrade");
                            sign.setLine(3, "");
                            sign.update();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    this.addCampBlock(absCoord);
                }
                case "/foodinput" -> {
                    if (this.longhouseEnabled) {
                        this.foodDepositPoints.add(absCoord);
                        absCoord.getBlock().getState().setData(new org.bukkit.material.Chest(((org.bukkit.material.Sign) sb.getMaterialData()).getFacing()));
                    } else {
                        Sign sign = (Sign) absCoord.getBlock().getState();
                        sign.setData(sb.getMaterialData());
                        sign.setLine(0, CivSettings.localize.localizedString("camp_longhouseSign1"));
                        sign.setLine(1, CivSettings.localize.localizedString("camp_longhouseSign2"));
                        sign.setLine(2, CivSettings.localize.localizedString("upgradeUsing_SignText"));
                        sign.setLine(3, "/camp upgrade");
                        sign.update();
                    }
                    this.addCampBlock(absCoord);
                }
                case "/door" -> {
                    this.doors.add(absCoord);
                    Block doorBlock = absCoord.getBlock();
                    doorBlock.setType(Material.WOODEN_DOOR);
                    BlockState state = doorBlock.getState();
                    state.setData(new Door(Material.WOODEN_DOOR, ((org.bukkit.material.Sign) sb.getMaterialData()).getFacing()));
                    state.update(true, false);
                    this.addCampBlock(new BlockCoord(doorBlock));

                    Block doorBlock2 = absCoord.getBlock().getRelative(0, 1, 0);
                    doorBlock2.setType(Material.WOODEN_DOOR);
                    BlockState state2 = doorBlock2.getState();
                    state2.setData(new Door(Material.WOODEN_DOOR, false));
                    state2.update(true, false);
                    this.addCampBlock(new BlockCoord(doorBlock2));
                }
                case "/control" -> this.createControlPoint(absCoord);
                case "/literal" -> {
                    /* Unrecognized command... treat as a literal sign. */
                    Sign sign = (Sign) absCoord.getBlock().getState();
                    sign.setData(sb.getMaterialData());
                    sign.setLine(0, sb.message[0]);
                    sign.setLine(1, sb.message[1]);
                    sign.setLine(2, sb.message[2]);
                    sign.setLine(3, sb.message[3]);
                    sign.update();
                }
            }
        }

        updateFirepit();
    }

    private void removeCampBlock(BlockCoord absCoord) {
        this.campBlocks.remove(absCoord);
        CivGlobal.removeCampBlock(absCoord);
    }

    private void updateFirepit() {
        int maxFirePoints = CivSettings.campConfig.getInt("camp.firepoints", 24);
        int totalFireBlocks = this.firepitBlocks.size();

        double percentLeft = (double) this.firepoints / (double) maxFirePoints;

        //  x/totalFireBlocks = percentLeft / 100
        int litFires = (int) (percentLeft * totalFireBlocks);

        for (int i = 0; i < totalFireBlocks; i++) {
            BlockCoord next = this.firepitBlocks.get(i);
            if (next == null) {
                CivLog.warning("Couldn't find firepit id:" + i);
                continue;
            }

            next.getBlock().setType(i < litFires ? Material.FIRE : Material.AIR);
        }
    }

    public void processFirepoints() {

        MultiInventory mInv = new MultiInventory();
        for (BlockCoord bcoord : this.fireFurnaceBlocks) {
            Furnace furnace = (Furnace) bcoord.getBlock().getState();
            mInv.addInventory(furnace.getInventory());
        }

        if (mInv.contains(null, Material.COAL, (short) 0, coal_per_firepoint)) {
            try {
                mInv.removeItem(Material.COAL, coal_per_firepoint, true);
            } catch (CivException e) {
                e.printStackTrace();
            }

            this.firepoints++;
            if (firepoints > maxFirePoints) {
                firepoints = maxFirePoints;
            }
        } else {
            this.firepoints--;
            CivMessage.sendCamp(this, ChatColor.YELLOW + CivSettings.localize.localizedString("var_camp_campfireDown", this.firepoints));

            double percentLeft = (double) this.firepoints / (double) this.maxFirePoints;
            if (percentLeft < 0.3) {
                CivMessage.sendCamp(this, String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.localize.localizedString("camp_campfire30percent"));
            }

            if (this.firepoints < 0) {
                this.destroy();
            }
        }

        this.save();
        this.updateFirepit();
    }

    public void processLonghouse() {
        MultiInventory mInv = new MultiInventory();

        for (BlockCoord bcoord : this.foodDepositPoints) {
            Block b = bcoord.getBlock();
            if (b.getState() instanceof Chest) {
                try {
                    mInv.addInventory(((Chest) b.getState()).getInventory());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (mInv.getInventoryCount() == 0) {
            CivMessage.sendCamp(this, ChatColor.RED + CivSettings.localize.localizedString("camp_longhouseNoChest"));
            return;
        }

        this.consumeComponent.setSource(mInv);
        Result result = this.consumeComponent.processConsumption(true);
        this.consumeComponent.onSave();

        switch (result) {
            case STARVE -> {
                CivMessage.sendCamp(this, ChatColor.GREEN + CivSettings.localize.localizedString("var_camp_yourLonghouseDown", (ChatColor.RED + CivSettings.localize.localizedString("var_camp_longhouseStarved", consumeComponent.getCountString()) + ChatColor.GREEN), CivSettings.CURRENCY_NAME));
                return;
            }
            case LEVELDOWN -> {
                CivMessage.sendCamp(this, ChatColor.GREEN + CivSettings.localize.localizedString("var_camp_yourLonghouseDown", (ChatColor.RED + CivSettings.localize.localizedString("camp_longhouseStavedAndLeveledDown") + ChatColor.GREEN), CivSettings.CURRENCY_NAME));
                return;
            }
            case STAGNATE -> {
                CivMessage.sendCamp(this, ChatColor.GREEN + CivSettings.localize.localizedString("var_camp_yourLonghouseDown", (ChatColor.YELLOW + CivSettings.localize.localizedString("camp_longhouseStagnated") + ChatColor.GREEN), CivSettings.CURRENCY_NAME));
                return;
            }
            case UNKNOWN -> {
                CivMessage.sendCamp(this, ChatColor.GREEN + CivSettings.localize.localizedString("var_camp_yourLonghouseDown", (ChatColor.DARK_PURPLE + CivSettings.localize.localizedString("camp_longhouseSomethingUnknown") + ChatColor.GREEN), CivSettings.CURRENCY_NAME));
                return;
            }
            default -> {
            }
        }

        ConfigCampLonghouseLevel lvl = null;
        if (result == Result.LEVELUP) {
            lvl = CivSettings.longhouseLevels.get(consumeComponent.getLevel() - 1);
        } else {
            lvl = CivSettings.longhouseLevels.get(consumeComponent.getLevel());
        }

        double total_coins = lvl.coins;
        this.getOwner().getTreasury().deposit(total_coins);

        LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterialFromId("mat_token_of_leadership");
        if (craftMat != null) {
            ItemStack token = LoreCraftableMaterial.spawn(craftMat);

            Tagged tag = (Tagged) craftMat.getComponent("Tagged");
            Resident res = CivGlobal.getResident(this.getOwnerName());
            Player p = null;
            try {
                p = CivGlobal.getPlayer(res);
            } catch (CivException ee) {
                ee.printStackTrace();
            }

            token = tag.addTag(token, p.getName());

            AttributeUtil attrs = new AttributeUtil(token);
            attrs.addLore(ChatColor.GRAY + res.getName());
            token = attrs.getStack();

            mInv.addItems(token, true);
        }

        String stateMessage = switch (result) {
            case GROW ->
                    ChatColor.DARK_GREEN + CivSettings.localize.localizedString("var_camp_longhouseGrew", consumeComponent.getCountString() + ChatColor.GREEN);
            case LEVELUP ->
                    ChatColor.DARK_GREEN + CivSettings.localize.localizedString("camp_longhouselvlUp") + ChatColor.GREEN;
            case MAXED ->
                    ChatColor.DARK_GREEN + CivSettings.localize.localizedString("var_camp_longhouseIsMaxed", consumeComponent.getCountString() + ChatColor.GREEN);
            default -> "";
        };

        CivMessage.sendCamp(this, ChatColor.GREEN + CivSettings.localize.localizedString("var_camp_yourLonghouse", stateMessage, total_coins, CivSettings.CURRENCY_NAME));
    }

    private void buildCampFromTemplate(Template tpl, BlockCoord corner) {

        Block cornerBlock = corner.getBlock();
        for (int x = 0; x < tpl.size_x; x++) {
            for (int y = 0; y < tpl.size_y; y++) {
                for (int z = 0; z < tpl.size_z; z++) {
                    Block nextBlock = cornerBlock.getRelative(x, y, z);

                    if (tpl.blocks[x][y][z].specialType == Type.COMMAND) {
                        continue;
                    }

                    if (tpl.blocks[x][y][z].specialType == Type.LITERAL) {
                        // Adding a command block for literal sign placement
                        tpl.blocks[x][y][z].command = "/literal";
                        tpl.commandBlockRelativeLocations.add(new BlockCoord(cornerBlock.getWorld().getName(), x, y, z));
                        continue;
                    }

                    try {
                        nextBlock.setType(tpl.blocks[x][y][z].getType());
                        nextBlock.getState().setData(tpl.blocks[x][y][z].getMaterialData());

                        if (nextBlock.getType() != Material.AIR) {
                            this.addCampBlock(new BlockCoord(nextBlock.getLocation()));
                        }
                    } catch (Exception e) {
                        CivLog.error(e.getMessage());
                    }
                }
            }
        }
    }

    private void bindCampBlocks() {
        // Called mostly on a reload, determines which blocks should be protected based on the corner
        // location and the template's size. We need to verify that each block is a part of the template.


        /* Load in the template. */
        Template tpl;
        try {
            //tpl.load_template(this.getSavedTemplatePath());
            tpl = Template.getTemplate(this.getSavedTemplatePath(), null);
        } catch (IOException | CivException e) {
            e.printStackTrace();
            return;
        }

        for (int y = 0; y < tpl.size_y; y++) {
            for (int z = 0; z < tpl.size_z; z++) {
                for (int x = 0; x < tpl.size_x; x++) {
                    int relx = getCorner().getX() + x;
                    int rely = getCorner().getY() + y;
                    int relz = getCorner().getZ() + z;

                    BlockCoord coord = new BlockCoord(this.getCorner().getWorldname(), (relx), (rely), (relz));

                    if (tpl.blocks[x][y][z].getType() == Material.AIR) {
                        continue;
                    }

                    if (tpl.blocks[x][y][z].specialType == SimpleBlock.Type.COMMAND) {
                        continue;
                    }

                    this.addCampBlock(coord);
                }
            }
        }

        this.processCommandSigns(tpl, corner);

    }

    protected Location repositionCenter(Location center, BlockFace dir, double x_size, double z_size) {
        Location loc = new Location(center.getWorld(),
                center.getX(), center.getY(), center.getZ(),
                center.getYaw(), center.getPitch());

        // Reposition tile improvements
        switch (dir) {
            case EAST -> {
                loc.setZ(loc.getZ() - (z_size / 2));
                loc.setX(loc.getX() + SHIFT_OUT);
            }
            case WEST -> {
                loc.setZ(loc.getZ() - (z_size / 2));
                loc.setX(loc.getX() - (SHIFT_OUT + x_size));
            }
            case NORTH -> {
                loc.setX(loc.getX() - (x_size / 2));
                loc.setZ(loc.getZ() - (SHIFT_OUT + z_size));
            }
            case SOUTH -> {
                loc.setX(loc.getX() - (x_size / 2));
                loc.setZ(loc.getZ() + SHIFT_OUT);
            }
        }

        return loc;
    }

    protected void checkBlockPermissionsAndRestrictions(Player player, Block centerBlock, int regionX, int regionY, int regionZ) throws CivException {

        ChunkCoord ccoord = new ChunkCoord(centerBlock.getLocation());
        CultureChunk cc = CivGlobal.getCultureChunk(ccoord);
        if (cc != null) {
            throw new CivException(CivSettings.localize.localizedString("camp_checkInCivError"));
        }

        if (player.getLocation().getY() >= 200) {
            throw new CivException(CivSettings.localize.localizedString("camp_checkTooHigh"));
        }

        if ((regionY + centerBlock.getLocation().getBlockY()) >= 255) {
            throw new CivException(CivSettings.localize.localizedString("camp_checkWayTooHigh"));
        }

        if (player.getLocation().getY() < CivGlobal.minBuildHeight) {
            throw new CivException(CivSettings.localize.localizedString("cannotBuild_toofarUnderground"));
        }

        if (!player.isOp()) {
            Buildable.validateDistanceFromSpawn(centerBlock.getLocation());
        }

        int yTotal = 0;
        int yCount = 0;

        for (int x = 0; x < regionX; x++) {
            for (int y = 0; y < regionY; y++) {
                for (int z = 0; z < regionZ; z++) {
                    Block b = centerBlock.getRelative(x, y, z);

                    if (b.getType() == Material.CHEST) {
                        throw new CivException(CivSettings.localize.localizedString("cannotBuild_chestInWay"));
                    }

                    BlockCoord coord = new BlockCoord(b);
                    ChunkCoord chunkCoord = new ChunkCoord(coord.getLocation());

                    TownChunk tc = CivGlobal.getTownChunk(chunkCoord);
                    if (tc != null && !tc.perms.hasPermission(PlotPermissions.Type.DESTROY, CivGlobal.getResident(player))) {
                        // Make sure we have permission to destroy any block in this area.
                        throw new CivException(CivSettings.localize.localizedString("cannotBuild_needPermissions") + " " + b.getX() + "," + b.getY() + "," + b.getZ());
                    }

                    if (CivGlobal.getProtectedBlock(coord) != null) {
                        throw new CivException(CivSettings.localize.localizedString("cannotBuild_protectedInWay"));
                    }

                    if (CivGlobal.getStructureBlock(coord) != null) {
                        throw new CivException(CivSettings.localize.localizedString("cannotBuild_structureInWay"));
                    }

                    if (CivGlobal.getFarmChunk(chunkCoord) != null) {
                        throw new CivException(CivSettings.localize.localizedString("cannotBuild_farmInWay"));
                    }

//                    if (CivGlobal.getWallChunk(chunkCoord) != null) {
//                        throw new CivException(CivSettings.localize.localizedString("cannotBuild_wallInWay"));
//                    }

                    if (CivGlobal.getCampBlock(coord) != null) {
                        throw new CivException(CivSettings.localize.localizedString("cannotBuild_campinWay"));
                    }

                    yTotal += b.getWorld().getHighestBlockYAt(centerBlock.getX() + x, centerBlock.getZ() + z);
                    yCount++;

//                    rb = CivGlobal.getRoadBlock(coord);
//                    if (CivGlobal.getRoadBlock(coord) != null) {
//                        /*
//                         * XXX Special case. Since road blocks can be built in wilderness
//                         * we don't want people griefing with them. Building a structure over
//                         * a road block should always succeed.
//                         */
//                        deletedRoadBlocks.add(rb);
//                    }
                }
            }
        }

        /* Delete any roads that we're building over. */
//        for (RoadBlock roadBlock : deletedRoadBlocks) {
//            roadBlock.getRoad().deleteRoadBlock(roadBlock);
//        }

        double highestAverageBlock = (double) yTotal / (double) yCount;

        if (((centerBlock.getY() > (highestAverageBlock + 10)) ||
                (centerBlock.getY() < (highestAverageBlock - 10)))) {
            throw new CivException(CivSettings.localize.localizedString("cannotBuild_toofarUnderground"));
        }

    }

    public void unbindCampBlocks() {
        for (BlockCoord bcoord : this.campBlocks.keySet()) {
            CivGlobal.removeCampBlock(bcoord);
            ChunkCoord coord = new ChunkCoord(bcoord);
            CivGlobal.removeCampChunk(coord);
        }
    }

    private void addCampBlock(BlockCoord coord) {
        addCampBlock(coord, false);
    }

    private void addCampBlock(BlockCoord coord, boolean friendlyBreakable) {
        CampBlock cb = new CampBlock(coord, this, friendlyBreakable);

        this.campBlocks.put(coord, cb);
        CivGlobal.addCampBlock(cb);
    }

    public void addMember(Resident resident) {
        this.members.put(resident.getName(), resident);
        resident.setCamp(this);
        resident.save();
    }

    public void removeMember(Resident resident) {
        this.members.remove(resident.getName());
        resident.setCamp(null);
        resident.save();
    }

    public Resident getMember(String name) {
        return this.members.get(name);
    }

    public boolean hasMember(String name) {
        return this.members.containsKey(name);
    }

    public Resident getOwner() {
        return CivGlobal.getResidentViaUUID(UUID.fromString(ownerName));
    }


    public void setOwner(Resident owner) {
        this.ownerName = owner.getUUID().toString();
    }


    public int getHitpoints() {
        return hitpoints;
    }


    public void setHitpoints(int hitpoints) {
        this.hitpoints = hitpoints;
    }


    public int getFirepoints() {
        return firepoints;
    }


    public void setFirepoints(int firepoints) {
        this.firepoints = firepoints;
    }


    public BlockCoord getCorner() {
        return corner;
    }


    public void setCorner(BlockCoord corner) {
        this.corner = corner;
    }

    public void fancyCampBlockDestory() {
        for (BlockCoord coord : this.campBlocks.keySet()) {

            if (CivGlobal.getStructureChest(coord) != null) {
                continue;
            }

            if (CivGlobal.getStructureSign(coord) != null) {
                continue;
            }

            Block block4 = coord.getBlock();
            if (block4.getType() == Material.CHEST ||
                    block4.getType() == Material.SIGN_POST ||
                    block4.getType() == Material.WALL_SIGN) {
                continue;
            }

            if (CivSettings.alwaysCrumble.contains(block4.getType())) {
                Block block = coord.getBlock();
                block.setType(Material.GRAVEL);
                continue;
            }

            Random rand = new Random();

            // Each block has a 10% chance to turn into gravel
            if (rand.nextInt(100) <= 10) {
                Block block = coord.getBlock();
                block.setType(Material.GRAVEL);
                continue;
            }

            // Each block has a 50% chance of starting a fire
            if (rand.nextInt(100) <= 50) {
                Block block = coord.getBlock();
                block.setType(Material.FIRE);
                continue;
            }

            // Each block has a 1% chance of launching an explosion effect
            if (rand.nextInt(100) <= 1) {
                FireworkEffect effect = FireworkEffect.builder().with(org.bukkit.FireworkEffect.Type.BURST).withColor(Color.ORANGE).withColor(Color.RED).withTrail().withFlicker().build();
                FireworkEffectPlayer fePlayer = new FireworkEffectPlayer();
                for (int i = 0; i < 3; i++) {
                    try {
                        fePlayer.playFirework(coord.getBlock().getWorld(), coord.getLocation(), effect);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void createControlPoint(BlockCoord absCoord) {

        Location centerLoc = absCoord.getLocation();

        /* Build the bedrock tower. */
        centerLoc.getBlock().setType(Material.FENCE);

        StructureBlock sb = new StructureBlock(new BlockCoord(centerLoc.getBlock()), this);
        this.addCampBlock(sb.getCoord());

        /* Build the control block. */
        Block b = centerLoc.getBlock().getRelative(0, 1, 0);
        b.setType(Material.OBSIDIAN);
        this.addCampBlock(new StructureBlock(new BlockCoord(b), this).getCoord());

        int campControlHitpoints = CivSettings.warConfig.getInt("war.control_block_hitpoints_camp", 80);

        BlockCoord coord = new BlockCoord(b);
        this.controlBlocks.put(coord, new ControlPoint(coord, this, campControlHitpoints));
    }


    public boolean isUndoable() {
        return undoable;
    }

    public void setUndoable(boolean undoable) {
        this.undoable = undoable;
    }

    @Override
    public String getDisplayName() {
        return CivSettings.localize.localizedString("Camp");
    }

    @Override
    public void sessionAdd(String key, String value) {
        CivGlobal.getSessionDB().add(key, value, 0, 0, 0);
    }

    //XXX TODO make sure these all work...
    @Override
    public void processUndo() {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBuildProgess() {
        // TODO Auto-generated method stub

    }

    @Override
    public void build(Player player, Location centerLoc, Template tpl) {
    }

    @Override
    protected void runOnBuild(Location centerLoc, Template tpl) {
    }

    @Override
    public void onComplete() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onLoad() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onUnload() {
        // TODO Auto-generated method stub

    }

    public Collection<Resident> getMembers() {
        return this.members.values();
    }

    public String getOwnerName() {
        Resident res = CivGlobal.getResidentViaUUID(UUID.fromString(ownerName));
        return res.getName();
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public int getLonghouseLevel() {
        return this.consumeComponent.getLevel();
    }

    public String getLonghouseCountString() {
        return this.consumeComponent.getCountString();
    }

    public String getMembersString() {
        StringBuilder out = new StringBuilder();
        for (Resident resident : members.values()) {
            out.append(resident.getName()).append(" ");
        }
        return out.toString();
    }

    public void onControlBlockHit(ControlPoint cp, World world, Player player) {
        world.playSound(cp.getCoord().getLocation(), Sound.BLOCK_ANVIL_USE, 0.2f, 1);
        world.playEffect(cp.getCoord().getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
        CivMessage.sendActionBar(player, CivData.getStringForBar(CivData.TaskType.CONTROL, cp.getHitpoints(), cp.getMaxHitpoints()));

        CivMessage.send(player, ChatColor.GRAY + CivSettings.localize.localizedString("camp_hitControlBlock") + "(" + cp.getHitpoints() + " / " + cp.getMaxHitpoints() + ")");
        CivMessage.sendCamp(this, ChatColor.YELLOW + CivSettings.localize.localizedString("camp_controlBlockUnderAttack"));
    }


    public void onControlBlockDestroy(ControlPoint cp, World world, Player player) {
        Block block = cp.getCoord().getLocation().getBlock();
        block.setType(Material.AIR);
        world.playSound(cp.getCoord().getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0f, -1.0f);
        world.playSound(cp.getCoord().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        FireworkEffect effect = FireworkEffect.builder().with(org.bukkit.FireworkEffect.Type.BURST).withColor(Color.YELLOW).withColor(Color.RED).withTrail().withFlicker().build();
        FireworkEffectPlayer fePlayer = new FireworkEffectPlayer();
        for (int i = 0; i < 3; i++) {
            try {
                fePlayer.playFirework(world, cp.getCoord().getLocation(), effect);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        boolean allDestroyed = true;
        for (ControlPoint c : this.controlBlocks.values()) {
            if (!c.isDestroyed()) {
                allDestroyed = false;
                break;
            }
        }

        if (allDestroyed) {
            CivMessage.sendCamp(this, ChatColor.RED + CivSettings.localize.localizedString("camp_destroyed"));
            this.destroy();
        } else {
            CivMessage.sendCamp(this, ChatColor.RED + CivSettings.localize.localizedString("camp_controlBlockDestroyed"));
        }

    }

    @Override
    public void onDamage(int amount, World world, Player player, BlockCoord hit, BuildableDamageBlock hit2) {

        ControlPoint cp = this.controlBlocks.get(hit);
        if (cp != null) {
            Date now = new Date();
            Resident resident = CivGlobal.getResident(player);

            if (resident.isProtected()) {
                CivMessage.sendError(player, CivSettings.localize.localizedString("camp_protected"));
                return;
            }

            if (now.after(getNextRaidDate())) {
                if (!cp.isDestroyed()) {
                    cp.damage(amount);
                    if (cp.isDestroyed()) {
                        onControlBlockDestroy(cp, world, player);
                    } else {
                        onControlBlockHit(cp, world, player);
                    }
                } else {
                    CivMessage.send(player, ChatColor.RED + CivSettings.localize.localizedString("camp_controlBlockAlreadyDestroyed"));
                }
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("M/dd h:mm:ss a z");
                CivMessage.send(player, ChatColor.RED + CivSettings.localize.localizedString("camp_protectedUntil") + " " + sdf.format(getNextRaidDate()));
            }

        }
    }

    public void setNextRaidDate(Date next) {
        this.nextRaidDate = next;
        this.save();
    }

    public Date getNextRaidDate() {
        Date raidEnd = new Date(this.nextRaidDate.getTime());
        raidEnd.setTime(this.nextRaidDate.getTime() + 60L * 60 * 1000 * this.raidLength);

        Date now = new Date();
        if (now.getTime() > raidEnd.getTime()) {
            this.nextRaidDate.setTime(nextRaidDate.getTime() + 60 * 60 * 1000 * 24);
        }

        return this.nextRaidDate;
    }

    public boolean isSifterEnabled() {
        return sifterEnabled;
    }

    public void setSifterEnabled(boolean sifterEnabled) {
        this.sifterEnabled = sifterEnabled;
    }

    public Collection<ConfigCampUpgrade> getUpgrades() {
        return this.upgrades.values();
    }

    public boolean hasUpgrade(String require_upgrade) {
        return this.upgrades.containsKey(require_upgrade);
    }

    public void purchaseUpgrade(ConfigCampUpgrade upgrade) throws CivException {
        Resident owner = this.getOwner();

        if (!owner.getTreasury().hasEnough(upgrade.cost())) {
            throw new CivException(CivSettings.localize.localizedString("var_camp_ownerMissingCost", upgrade.cost(), CivSettings.CURRENCY_NAME));
        }

        this.upgrades.put(upgrade.id(), upgrade);
        upgrade.processAction(this);


        this.reprocessCommandSigns();
        owner.getTreasury().withdraw(upgrade.cost());
        this.save();
    }

    public boolean isLonghouseEnabled() {
        return longhouseEnabled;
    }

    public void setLonghouseEnabled(boolean longhouseEnabled) {
        this.longhouseEnabled = longhouseEnabled;
    }

    public boolean isGardenEnabled() {
        return gardenEnabled;
    }

    public void setGardenEnabled(boolean gardenEnabled) {
        this.gardenEnabled = gardenEnabled;
    }
}
