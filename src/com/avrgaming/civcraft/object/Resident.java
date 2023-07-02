/*************************************************************************
 *
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

import gpl.InventorySerializer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
//import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
//import org.bukkit.potion.PotionEffect;
//import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.avrgaming.civcraft.arena.Arena;
import com.avrgaming.civcraft.arena.ArenaTeam;
import com.avrgaming.civcraft.camp.Camp;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.config.ConfigPerk;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.event.EventTimer;
import com.avrgaming.civcraft.exception.AlreadyRegisteredException;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.interactive.InteractiveResponse;
import com.avrgaming.civcraft.items.units.Unit;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.road.RoadBlock;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.TownHall;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.BuildPreviewAsyncTask;
import com.avrgaming.civcraft.tutorial.CivTutorial;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.PlayerBlockChangeUtil;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.global.perks.NotVerifiedException;
import com.avrgaming.global.perks.Perk;
import com.avrgaming.global.perks.components.CustomPersonalTemplate;
import com.avrgaming.global.perks.components.CustomTemplate;

public class Resident extends SQLObject {

    private Town town = null;
    private Camp camp = null;
    private boolean townChat = false;
    private boolean civChat = false;
    private boolean adminChat = false;
    private boolean combatInfo = false;
    private boolean titleAPI = true;
    private int respawntime;

    private boolean usesAntiCheat = false;

    public static HashSet<String> allchatters = new HashSet<String>();

    /* Town or civ to chat in besides your own. */
    private Town townChatOverride = null;
    private Civilization civChatOverride = null;
    private boolean permOverride = false;
    private boolean sbperm = false;
    private boolean controlBlockInstantBreak = false;
    private int townID = 0;
    private int campID = 0;
    private boolean dontSaveTown = false;
    private String timezone;

    private boolean banned = false;

    private long registered;
    private long lastOnline;
    private int daysTilEvict;
    private boolean givenKit;
    private ConcurrentHashMap<String, Integer> friends = new ConcurrentHashMap<>();
    private EconObject treasury;
    private boolean muted;
    private Date muteExpires = null;

    private boolean interactiveMode = false;
    private InteractiveResponse interactiveResponse = null;
    private BuildPreviewAsyncTask previewTask = null;

    private double spyExposure = 0.0;
    public static int MAX_SPY_EXPOSURE = 1000;
    private boolean performingMission = false;

    private Town selectedTown = null;

    private Scoreboard scoreboard = null;
    public String desiredCivName;
    public String desiredCapitolName;
    public String desiredTownName;
    public Location desiredTownLocation = null;
    public Template desiredTemplate = null;

    public boolean allchat = false;

    /* XXX
     * This buildable is used as place to store which buildable we're working on when interacting
     * with GUI items. We want to be able to pass the buildable object to the GUI's action function,
     * but there isn't a good way to do this ATM. If we had a way to send arbitary objects it would
     * be better. Could we store it here on the resident object?
     */
    public Buildable pendingBuildable;
    public ConfigBuildableInfo pendingBuildableInfo;
    public CallbackInterface pendingCallback;

    private boolean showScout = true;
    private boolean showTown = true;
    private boolean showCiv = true;
    private boolean showMap = false;
    private boolean showInfo = false;
    private String itemMode = "all";
    private String savedInventory = null;
    private boolean insideArena = false;
    private boolean isProtected = false;

    public ConcurrentHashMap<BlockCoord, SimpleBlock> previewUndo = null;
    public LinkedHashMap<String, Perk> perks = new LinkedHashMap<String, Perk>();
    private Date lastKilledTime = null;
    private String lastIP = "";
    private UUID uid;
    private double walkingModifier = CivSettings.normal_speed;
    private boolean onRoad = false;
    public String debugTown;

    public Resident(UUID uid, String name) throws InvalidNameException {
        this.setName(name);
        this.uid = uid;
        this.treasury = CivGlobal.createEconObject(this);
        setTimezoneToServerDefault();
        loadSettings();
    }

    public Resident(ResultSet rs) throws SQLException, InvalidNameException {
        this.load(rs);
        loadSettings();
    }

    public void loadSettings() {
        this.spyExposure = 0.0;
    }

    public static final String TABLE_NAME = "RESIDENTS";

    public static void init() throws SQLException {
        if (!SQL.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME + " (" +
                    "`id` int(11) unsigned NOT NULL auto_increment," +
                    "`name` VARCHAR(64) NOT NULL," +
                    "`uuid` VARCHAR(256) NOT NULL DEFAULT 'UNKNOWN'," +
                    "`currentName` VARCHAR(64) DEFAULT NULL," +
                    "`town_id` int(11)," +
                    "`lastOnline` BIGINT NOT NULL," +
                    "`registered` BIGINT NOT NULL," +
                    "`friends` mediumtext," +
                    "`debt` double DEFAULT 0," +
                    "`coins` double DEFAULT 0," +
                    "`daysTilEvict` mediumint DEFAULT NULL," +
                    "`givenKit` bool NOT NULL DEFAULT '0'," +
                    "`camp_id` int(11)," +
                    "`timezone` mediumtext," +
                    "`banned` bool NOT NULL DEFAULT '0'," +
                    "`bannedMessage` mediumtext DEFAULT NULL," +
                    "`savedInventory` mediumtext DEFAULT NULL," +
                    "`insideArena` bool NOT NULL DEFAULT '0'," +
                    "`isProtected` bool NOT NULL DEFAULT '0'," +
                    "`flags` mediumtext DEFAULT NULL," +
                    "`last_ip` mediumtext DEFAULT NULL," +
                    "`debug_town` mediumtext DEFAULT NULL," +
                    "`debug_civ` mediumtext DEFAULT NuLL," +
                    "UNIQUE KEY (`name`), " +
                    "PRIMARY KEY (`id`)" + ")";

            SQL.makeTable(table_create);
            CivLog.info("Created " + TABLE_NAME + " table");
        } else {
            CivLog.info(TABLE_NAME + " table OK!");

            if (!SQL.hasColumn(TABLE_NAME, "uuid")) {
                CivLog.info("\tCouldn't find `uuid` for resident.");
                SQL.addColumn(TABLE_NAME, "`uuid` VARCHAR(256) NOT NULL DEFAULT 'UNKNOWN'");
            }

            if (!SQL.hasColumn(TABLE_NAME, "currentName")) {
                CivLog.info("\tCouldn't find `currentName` for resident.");
                SQL.addColumn(TABLE_NAME, "`currentName` VARCHAR(64) DEFAULT NULL");
            }

            if (!SQL.hasColumn(TABLE_NAME, "banned")) {
                CivLog.info("\tCouldn't find `banned` for resident.");
                SQL.addColumn(TABLE_NAME, "`banned` bool default 0");
            }

            if (!SQL.hasColumn(TABLE_NAME, "bannedMessage")) {
                CivLog.info("\tCouldn't find `bannedMessage` for resident.");
                SQL.addColumn(TABLE_NAME, "`bannedMessage` mediumtext default null");
            }

            if (!SQL.hasColumn(TABLE_NAME, "last_ip")) {
                CivLog.info("\tCouldn't find `last_ip` for resident.");
                SQL.addColumn(TABLE_NAME, "`last_ip` mediumtext default null");
            }

            if (!SQL.hasColumn(TABLE_NAME, "camp_id")) {
                CivLog.info("\tCouldn't find `camp_id` for resident.");
                SQL.addColumn(TABLE_NAME, "`camp_id` int(11) default 0");
            }

            if (!SQL.hasColumn(TABLE_NAME, "timezone")) {
                CivLog.info("\tCouldn't find `timezone` for resident.");
                SQL.addColumn(TABLE_NAME, "`timezone` mediumtext default null");
            }

            if (!SQL.hasColumn(TABLE_NAME, "debug_civ")) {
                CivLog.info("\tCouldn't find `debug_civ` for resident.");
                SQL.addColumn(TABLE_NAME, "`debug_civ` mediumtext default null");
            }

            if (!SQL.hasColumn(TABLE_NAME, "debug_town")) {
                CivLog.info("\tCouldn't find `debug_town` for resident.");
                SQL.addColumn(TABLE_NAME, "`debug_town` mediumtext default null");
            }

            SQL.makeCol("flags", "mediumtext", TABLE_NAME);
            SQL.makeCol("savedInventory", "mediumtext", TABLE_NAME);
            SQL.makeCol("insideArena", "bool NOT NULL DEFAULT '0'", TABLE_NAME);
            SQL.makeCol("isProtected", "bool NOT NULL DEFAULT '0'", TABLE_NAME);
        }
    }

    @Override
    public void load(ResultSet rs) throws SQLException, InvalidNameException {
        this.setId(rs.getInt("id"));
        this.setName(rs.getString("name"));
        this.townID = rs.getInt("town_id");
        this.campID = rs.getInt("camp_id");
        this.lastIP = rs.getString("last_ip");
        this.debugTown = rs.getString("debug_town");

        if (rs.getString("uuid").equalsIgnoreCase("UNKNOWN")) {
            this.uid = null;
        } else {
            this.uid = UUID.fromString(rs.getString("uuid"));
        }

        this.treasury = CivGlobal.createEconObject(this);
        this.getTreasury().setBalance(rs.getDouble("coins"), false);
        this.setGivenKit(rs.getBoolean("givenKit"));
        this.setTimezone(rs.getString("timezone"));
        this.loadFlagSaveString(rs.getString("flags"));
        this.savedInventory = rs.getString("savedInventory");
        this.insideArena = rs.getBoolean("insideArena");
        this.isProtected = rs.getBoolean("isProtected");

        if (this.getTimezone() == null) {
            this.setTimezoneToServerDefault();
        }

        if (this.townID != 0) {
            this.setTown(CivGlobal.getTownFromId(this.townID));
            if (this.town == null) {
                CivLog.error("COULD NOT FIND TOWN(" + this.townID + ") FOR RESIDENT(" + this.getId() + ") Name:" + this.getName());
                /*
                 * When a town fails to load, we wont be able to find it above.
                 * However this can cause a cascade effect where because we couldn't find
                 * the town above, we save this resident's town as NULL which wipes
                 * their town information from the database when the resident gets saved.
                 * Just to make sure this doesn't happen the boolean below guards resident saves.
                 * There ought to be a better way...
                 */
                if (CivGlobal.testFileFlag("cleanupDatabase")) {
                    this.saveNow();
                } else {
                    this.dontSaveTown = true;
                }
                return;
            }
        }

        if (this.campID != 0) {
            this.setCamp(CivGlobal.getCampFromId(this.campID));
            if (this.camp == null) {
                CivLog.error("COULD NOT FIND CAMP(" + this.campID + ") FOR RESIDENT(" + this.getId() + ") Name:" + this.getName());
            } else {
                camp.addMember(this);
            }
        }

        if (this.getTown() != null) {
            try {
                this.getTown().addResident(this);
            } catch (AlreadyRegisteredException e) {
                e.printStackTrace();
            }
        }

        this.setLastOnline(rs.getLong("lastOnline"));
        this.setRegistered(rs.getLong("registered"));
        this.setDaysTilEvict(rs.getInt("daysTilEvict"));
        this.getTreasury().setDebt(rs.getDouble("debt"));
        this.loadFriendsFromSaveString(rs.getString("friends"));

    }

    private void setTimezoneToServerDefault() {
        this.timezone = EventTimer.getCalendarInServerTimeZone().getTimeZone().getID();
    }

    public String getFlagSaveString() {
        String flagString = "";

        if (this.isShowMap()) {
            flagString += "map,";
        }

        if (this.isShowTown()) {
            flagString += "showtown,";
        }

        if (this.isShowCiv()) {
            flagString += "showciv,";
        }

        if (this.isShowScout()) {
            flagString += "showscout,";
        }

        if (this.isShowInfo()) {
            flagString += "info,";
        }

        if (this.combatInfo) {
            flagString += "combatinfo,";
        }
        if (this.isTitleAPI()) {
            flagString += "titleapi,";
        }

        if (this.itemMode.equals("rare")) {
            flagString += "itemModeRare,";
        } else if (this.itemMode.equals("none")) {
            flagString += "itemModeNone,";
        }

        return flagString;
    }

    public void loadFlagSaveString(String str) {
        if (str == null) {
            return;
        }

        String[] split = str.split(",");

        for (String s : split) {
            switch (s.toLowerCase()) {
                case "map":
                    this.setShowMap(true);
                    break;
                case "showtown":
                    this.setShowTown(true);
                    break;
                case "showciv":
                    this.setShowCiv(true);
                    break;
                case "showscout":
                    this.setShowScout(true);
                    break;
                case "info":
                    this.setShowInfo(true);
                    break;
                case "combatinfo":
                    this.setCombatInfo(true);
                    break;
                case "titleapi":
					this.setTitleAPI(CivSettings.hasTitleAPI);
                    break;
                case "itemmoderare":
                    this.itemMode = "rare";
                    break;
                case "itemmodenone":
                    this.itemMode = "none";
                    break;
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

        hashmap.put("name", this.getName());
        hashmap.put("uuid", this.getUUIDString());
        if (this.getTown() != null) {
            hashmap.put("town_id", this.getTown().getId());
        } else {
            if (!dontSaveTown) {
                hashmap.put("town_id", null);
            }
        }

        if (this.getCamp() != null) {
            hashmap.put("camp_id", this.getCamp().getId());
        } else {
            hashmap.put("camp_id", null);
        }

        hashmap.put("lastOnline", this.getLastOnline());
        hashmap.put("registered", this.getRegistered());
        hashmap.put("debt", this.getTreasury().getDebt());
        hashmap.put("daysTilEvict", this.getDaysTilEvict());
        hashmap.put("friends", this.getFriendsSaveString());
        hashmap.put("givenKit", this.isGivenKit());
        hashmap.put("coins", this.getTreasury().getBalance());
        hashmap.put("timezone", this.getTimezone());
        hashmap.put("flags", this.getFlagSaveString());
        hashmap.put("last_ip", this.getLastIP());
        hashmap.put("savedInventory", this.savedInventory);
        hashmap.put("insideArena", this.insideArena);
        hashmap.put("isProtected", this.isProtected);

        if (this.getTown() != null) {
            hashmap.put("debug_town", this.getTown().getName());

            if (this.getTown().getCiv() != null) {
                hashmap.put("debug_civ", this.getCiv().getName());
            }
        }

        SQL.updateNamedObject(this, hashmap, TABLE_NAME);
    }

    public String getTownString() {
        if (town == null) {
            return "none";
        }
        return this.getTown().getName();
    }

    public Town getTown() {
        return town;
    }

    public void setTown(Town town) {
        this.town = town;
    }

    public boolean hasTown() {
        return town != null;
    }

    public long getRegistered() {
        return registered;
    }

    public void setRegistered(long registered) {
        this.registered = registered;
    }

    public long getLastOnline() {
        return lastOnline;
    }

    public void setLastOnline(long lastOnline) {
        this.lastOnline = lastOnline;
    }

    @Override
    public void delete() throws SQLException {
        SQL.deleteByName(this.getName(), TABLE_NAME);
    }

    public EconObject getTreasury() {
        return treasury;
    }

    public void setTreasury(EconObject treasury) {
        this.treasury = treasury;
    }

    public void onEnterDebt() {
        this.daysTilEvict = CivSettings.GRACE_DAYS;
    }

    public void warnDebt() {
        Player player;
        try {
            player = CivGlobal.getPlayer(this);
            CivMessage.send(player, CivColor.Yellow + CivSettings.localize.localizedString("var_resident_debtmsg", this.getTreasury().getDebt(), CivSettings.CURRENCY_NAME));
            CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("var_resident_debtEvictAlert1", this.daysTilEvict));
        } catch (CivException e) {
            //Player is not online.
        }
    }

    public int getDaysTilEvict() {
        return daysTilEvict;
    }

    public void setDaysTilEvict(int daysTilEvict) {
        this.daysTilEvict = daysTilEvict;
    }

    public void decrementGraceCounters() {
        this.daysTilEvict--;
        if (this.daysTilEvict == 0) {
            this.getTown().removeResident(this);

            try {
                CivMessage.send(CivGlobal.getPlayer(this), CivColor.Yellow + CivSettings.localize.localizedString("resident_evictedAlert"));
            } catch (CivException e) {
                // Resident not online.
            }
            return;
        }

        if (this.getTreasury().inDebt()) {
            warnDebt();
        } else {
            warnEvict();
        }

        this.save();
    }

    public double getPropertyTaxOwed() {
        double total = 0;

        if (this.getTown() == null) {
            return total;
        }

        for (TownChunk tc : this.getTown().getTownChunks()) {
            if (tc.perms.getOwner() == this) {
                double tax = tc.getValue() * this.getTown().getTaxRate();
                total += tax;
            }
        }
        return total;
    }

    public boolean isLandOwner() {
        if (this.getTown() == null)
            return false;

        for (TownChunk tc : this.getTown().getTownChunks()) {
            if (tc.perms.getOwner() == this) {
                return true;
            }
        }

        return false;
    }


    public double getFlatTaxOwed() {
        if (this.getTown() == null)
            return 0;

        return this.getTown().getFlatTax();
    }

    public boolean isTaxExempt() {
        return this.getTown().isInGroup("mayors", this) || this.getTown().isInGroup("assistants", this);
    }

    public void payOffDebt() {
        this.getTreasury().payTo(this.getTown().getTreasury(), this.getTreasury().getDebt());
        this.getTreasury().setDebt(0);
        this.daysTilEvict = -1;
        this.save();
    }

    public void addFriend(Resident resident) {
        friends.put(resident.getName(), 1);
    }

    public boolean isFriend(Resident resident) {
        return friends.containsKey(resident.getName());
    }

    public Collection<String> getFriends() {
        return friends.keySet();
    }

    private String getFriendsSaveString() {
        String out = "";
        for (String name : friends.keySet()) {
            out += name + ",";
        }
        return out;
    }

    private void loadFriendsFromSaveString(String string) {
        String[] split = string.split(",");

        for (String str : split) {
            friends.put(str, 1);
        }
    }

    public void removeFriend(Resident friendToAdd) {
        friends.remove(friendToAdd.getName());
    }

    public String getGroupsString() {
        String out = "";

        for (PermissionGroup grp : CivGlobal.getGroups()) {
            if (grp.hasMember(this)) {
                if (grp.getTown() != null) {
                    if (grp.isProtectedGroup()) {
                        out += CivColor.LightPurple;
                    } else {
                        out += CivColor.White;
                    }
                    out += grp.getName() + "(" + grp.getTown().getName() + ")";

                } else if (grp.getCiv() != null) {
                    out += CivColor.Gold + grp.getName() + "(" + grp.getCiv().getName() + ")";
                }

                out += ", ";
            }
        }

        return out;
    }

    public void warnEvict() {
        try {
            CivMessage.send(CivGlobal.getPlayer(this), CivColor.Yellow + CivSettings.localize.localizedString("var_resident_evictionNotice1", this.getDaysTilEvict()));
        } catch (CivException e) {
            //player offline.
        }
    }

    public boolean isTownChat() {
        return townChat;
    }

    public void setTownChat(boolean townChat) {
        this.townChat = townChat;
    }

    public boolean isCivChat() {
        return civChat;
    }

    public void setCivChat(boolean civChat) {
        this.civChat = civChat;
    }
	@SuppressWarnings("unused")
    public boolean isAdminChat() {
        return adminChat;
    }
	@SuppressWarnings("unused")
    public void setAdminChat(boolean adminChat) {
        this.adminChat = adminChat;
    }

    public Town getTownChatOverride() {
        return townChatOverride;
    }

    public void setTownChatOverride(Town townChatOverride) {
        this.townChatOverride = townChatOverride;
    }

    public Civilization getCivChatOverride() {
        return civChatOverride;
    }

    public void setCivChatOverride(Civilization civChatOverride) {
        this.civChatOverride = civChatOverride;
    }

    public boolean isPermOverride() {
        return permOverride;
    }

    public void setPermOverride(boolean permOverride) {
        this.permOverride = permOverride;
    }

    @SuppressWarnings("deprecation")
    public int takeItemsInHand(Material itemId, int itemData) throws CivException {
        Player player = CivGlobal.getPlayer(this);
        Inventory inv = player.getInventory();
        if (!inv.contains(itemId)) {
            return 0;
        }

        if ((player.getInventory().getItemInMainHand().getType() != itemId) &&
                (player.getInventory().getItemInMainHand().getTypeId() != itemData)) {
            return 0;
        }

        ItemStack stack = player.getInventory().getItemInMainHand();
        int count = stack.getAmount();
        inv.removeItem(stack);

        player.updateInventory();
        return count;
    }

	@SuppressWarnings({"unused", "deprecation"})
	public boolean takeItemInHand(int itemId, int itemData, int amount) throws CivException {
        Player player = CivGlobal.getPlayer(this);
        Inventory inv = player.getInventory();

        if (!inv.contains(itemId)) {
            return false;
        }

        if ((player.getInventory().getItemInMainHand().getTypeId() != itemId) &&
                (player.getInventory().getItemInMainHand().getTypeId() != itemData)) {
            return false;
        }

        ItemStack stack = player.getInventory().getItemInMainHand();

        if (stack.getAmount() < amount) {
            return false;
        } else if (stack.getAmount() == amount) {
            inv.removeItem(stack);
        } else {
            stack.setAmount(stack.getAmount() - amount);
        }

        player.updateInventory();
        return true;
    }

    @SuppressWarnings("deprecation")
    public boolean takeItem(int itemId, int itemData, int amount) throws CivException {
        Player player = CivGlobal.getPlayer(this);
        Inventory inv = player.getInventory();

        if (!inv.contains(itemId)) {
            return false;
        }

        HashMap<Integer, ? extends ItemStack> stacks;
        stacks = inv.all(itemId);

        for (ItemStack stack : stacks.values()) {
            if (stack.getData().getData() != (byte) itemData) {
                continue;
            }

            if (stack.getAmount() <= 0)
                continue;

            if (stack.getAmount() < amount) {
                amount -= stack.getAmount();
                stack.setAmount(0);
                inv.removeItem(stack);
                continue;
            }
			stack.setAmount(stack.getAmount() - amount);
			break;
		}

        player.updateInventory();
        return true;
    }

    @SuppressWarnings("deprecation")
    public int giveItem(int itemId, short damage, int amount) throws CivException {
        Player player = CivGlobal.getPlayer(this);
        Inventory inv = player.getInventory();
        ItemStack stack = new ItemStack(itemId, amount, damage);
		HashMap<Integer, ItemStack> leftovers = inv.addItem(stack);

        int leftoverAmount = 0;
        for (ItemStack i : leftovers.values()) {
            leftoverAmount += i.getAmount();
        }
        player.updateInventory();
        return amount - leftoverAmount;
    }

    public boolean buyItem(String itemName, int id, byte data, double price, int amount) throws CivException {

        if (!this.getTreasury().hasEnough(price)) {
            throw new CivException(CivSettings.localize.localizedString("resident_notEnoughMoney") + " " + CivSettings.CURRENCY_NAME);
        }

        boolean completed = true;
        int bought = 0;
        bought = giveItem(id, data, amount);
        if (bought != amount) {
            this.getTreasury().withdraw(price);
            takeItem(id, data, bought);
            completed = false;
        } else {
            this.getTreasury().withdraw(price);
        }

        if (completed) {
            return true;
        } else {
            throw new CivException(CivSettings.localize.localizedString("resident_buyInvenFull"));
        }
    }

    public Civilization getCiv() {
        if (this.getTown() == null) {
            return null;
        }
        return this.getTown().getCiv();
    }

	@SuppressWarnings("unused")
    public void setScoreboardName(String name, String key) {
        if (this.scoreboard == null) {
            this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            Team team = this.scoreboard.registerNewTeam("team");
            team.addPlayer(CivGlobal.getFakeOfflinePlayer(key));
            team.setDisplayName(name);
        } else {
            Team team = this.scoreboard.getTeam("team");
            team.setDisplayName(name);
        }

    }

	@SuppressWarnings("unused")
    public void setScoreboardValue(String name, String key, int value) {
        if (this.scoreboard == null) {
            return;
        }

        Objective obj = scoreboard.getObjective("obj:" + key);
        if (obj == null) {
            obj = scoreboard.registerNewObjective(name, "dummy");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            Score score = obj.getScore(CivGlobal.getFakeOfflinePlayer(key));
            score.setScore(value);
        } else {
            Score score = obj.getScore(CivGlobal.getFakeOfflinePlayer(key));
            score.setScore(value);
        }
    }
	@SuppressWarnings("unused")
    public void showScoreboard() {
        if (this.scoreboard != null) {
            Player player;
            try {
                player = CivGlobal.getPlayer(this);
                player.setScoreboard(this.scoreboard);
            } catch (CivException e) {
                e.printStackTrace();
            }
        }
    }
	@SuppressWarnings("unused")
    public void hideScoreboard() {
        Player player;
        try {
            player = CivGlobal.getPlayer(this);
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        } catch (CivException e) {
            e.printStackTrace();
        }
    }

    public boolean isGivenKit() {
        return givenKit;
    }

    public void setGivenKit(boolean givenKit) {
        this.givenKit = givenKit;
    }

    public boolean isSBPermOverride() {
        return sbperm;
    }

    public void setSBPermOverride(boolean b) {
        sbperm = b;
    }

    public void setInteractiveMode(InteractiveResponse interactive) {
        this.interactiveMode = true;
        this.interactiveResponse = interactive;
    }

    public void clearInteractiveMode() {
        this.interactiveMode = false;
        this.interactiveResponse = null;
    }

    public InteractiveResponse getInteractiveResponse() {
        return this.interactiveResponse;
    }

    public boolean isInteractiveMode() {
        return interactiveMode;
    }

    public Town getSelectedTown() {
        return selectedTown;
    }

    public void setSelectedTown(Town selectedTown) {
        this.selectedTown = selectedTown;
    }

    public Camp getCamp() {
        return camp;
    }

    public void setCamp(Camp camp) {
        this.camp = camp;
    }

    public boolean hasCamp() {
        return (this.camp != null);
    }

    public String getCampString() {
        if (this.camp == null) {
            return "none";
        }
        return this.camp.getName();
    }

    public void showWarnings(Player player) {
        /* Notify Resident of any invalid structures. */
        if (this.getTown() != null) {
            for (Buildable struct : this.getTown().invalidStructures) {
                CivMessage.send(player, CivColor.Yellow + ChatColor.BOLD +
                        CivSettings.localize.localizedString("var_resident_structInvalidAlert1", struct.getDisplayName(), struct.getCenterLocation()) +
                        " " + CivSettings.localize.localizedString("resident_structInvalidAlert2") + " " + struct.getInvalidReason());
            }

            /* Show any event messages. */
            if (this.getTown().getActiveEvent() != null) {
                CivMessage.send(player, CivColor.Yellow + CivSettings.localize.localizedString("var_resident_eventNotice1", this.getTown().getActiveEvent().configRandomEvent.name));
            }
        }


    }

    public boolean isShowScout() {
        return showScout;
    }

    public void setShowScout(boolean showScout) {
        this.showScout = showScout;
    }

    public boolean isShowTown() {
        return showTown;
    }

    public void setShowTown(boolean showTown) {
        this.showTown = showTown;
    }

    public boolean isShowCiv() {
        return showCiv;
    }

    public void setShowCiv(boolean showCiv) {
        this.showCiv = showCiv;
    }

    public boolean isShowMap() {
        return showMap;
    }

    public void setShowMap(boolean showMap) {
        this.showMap = showMap;
    }

    public void startPreviewTask(Template tpl, Block block, UUID uuid) {
        this.previewTask = new BuildPreviewAsyncTask(tpl, block, uuid);
        TaskMaster.asyncTask(previewTask, 0);
    }

    public void undoPreview() {
        if (this.previewUndo == null) {
            this.previewUndo = new ConcurrentHashMap<>();
            return;
        }

        if (this.previewTask != null) {
            previewTask.lock.lock();
            try {
                previewTask.aborted = true;
            } finally {
                previewTask.lock.unlock();
            }
        }

        try {
            Player player = CivGlobal.getPlayer(this);
            PlayerBlockChangeUtil util = new PlayerBlockChangeUtil();
            for (BlockCoord coord : this.previewUndo.keySet()) {
                SimpleBlock sb = this.previewUndo.get(coord);
                util.addUpdateBlock(player.getName(), coord, sb.getType(), sb.getData());
            }

            util.sendUpdate(player.getName());
        } catch (CivException e) {
            //Fall down and return.
        }

        this.previewUndo.clear();
        this.previewUndo = new ConcurrentHashMap<>();
    }

    public boolean isShowInfo() {
        return showInfo;
    }

    public void setShowInfo(boolean showInfo) {
        this.showInfo = showInfo;
    }
	@SuppressWarnings("unused")
    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public double getSpyExposure() {
        return spyExposure;
    }

    public void setSpyExposure(double spyExposure) {
        this.spyExposure = spyExposure;

        try {
            Player player = CivGlobal.getPlayer(this);
            double percentage = spyExposure / MAX_SPY_EXPOSURE;
            player.setExp((float) percentage);
        } catch (CivException ignored) {
        }

    }

    public boolean isPerformingMission() {
        return performingMission;
    }

    public void setPerformingMission(boolean performingMission) {
        this.performingMission = performingMission;
    }

    public void onRoadTest(BlockCoord coord, Player player) {
        /* Test the block beneath us for a road, if so, set the road flag. */
        BlockCoord feet = new BlockCoord(coord);
        feet.setY(feet.getY() - 1);
        RoadBlock rb = CivGlobal.getRoadBlock(feet);

        if (rb == null) {
            onRoad = false;
//			if (player.hasPotionEffect(PotionEffectType.SPEED)) {
//				player.removePotionEffect(PotionEffectType.SPEED);
//			}
        } else {
            onRoad = true;

//			if (!player.hasPotionEffect(PotionEffectType.SPEED)) {
//				CivLog.debug("setting effect.");
//				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 5, 5));
//			}
        }
    }

    public boolean isOnRoad() {
        return onRoad;
    }
	@SuppressWarnings("unused")
    public void setOnRoad(boolean onRoad) {
        this.onRoad = onRoad;
    }

    public void giveTemplate(String name) {
        int perkCount;
        try {
            perkCount = CivSettings.getInteger(CivSettings.perkConfig, "system.free_perk_count");
        } catch (InvalidConfiguration e) {
            e.printStackTrace();
            return;
        }
        for (ConfigPerk p : CivSettings.perks.values()) {
            Perk perk = new Perk(p);

            if (perk.getIdent().startsWith(("tpl_" + name).toLowerCase()) || perk.getIdent().startsWith(("template_" + name).toLowerCase())) {
                perk.count = perkCount;
                this.perks.put(perk.getIdent(), perk);
            }
        }

    }

    public void giveAllFreePerks() {
        int perkCount;
        try {
            perkCount = CivSettings.getInteger(CivSettings.perkConfig, "system.free_perk_count");
        } catch (InvalidConfiguration e) {
            e.printStackTrace();
            return;
        }

        for (ConfigPerk p : CivSettings.perks.values()) {
            Perk perk = new Perk(p);

            if (perk.getIdent().startsWith("perk_")) {
                perk.count = perkCount;
                this.perks.put(perk.getIdent(), perk);
            }
        }

    }

    public void loadPerks(final Player player) {
//		if (!PlatinumManager.isEnabled()) {
//			return;
//		}

        class AsyncTask implements Runnable {
            Resident resident;

            public AsyncTask(Resident resident) {
                this.resident = resident;
            }

            @Override
            public void run() {
                try {
                    resident.perks.clear();
                    Player player = CivGlobal.getPlayer(resident);
                    try {
                        CivGlobal.perkManager.loadPerksForResident(resident);
                    } catch (SQLException e) {
                        CivMessage.sendError(player, CivSettings.localize.localizedString("resident_couldnotloadperks"));
                        e.printStackTrace();
                        return;
                    } catch (NotVerifiedException e) {
                        return;
                    }
                } catch (CivException e1) {
                    return;
                }
                try {

                    StringBuilder perkMessage = new StringBuilder();
                    if (CivSettings.getString(CivSettings.perkConfig, "system.free_perks").equalsIgnoreCase("true")) {
                        resident.giveAllFreePerks();
                        perkMessage = new StringBuilder(CivSettings.localize.localizedString("PlayerLoginAsync_perksMsg1") + " ");
                    } else if (CivSettings.getString(CivSettings.perkConfig, "system.free_admin_perks").equalsIgnoreCase("true")) {
                        if (player.hasPermission(CivSettings.MINI_ADMIN) || player.hasPermission(CivSettings.FREE_PERKS)) {
                            resident.giveAllFreePerks();
                            perkMessage = new StringBuilder(CivSettings.localize.localizedString("PlayerLoginAsync_perksMsg1") + ": ");
                            perkMessage.append("Weather" + ", ");
                        }
                    }

                    for (ConfigPerk p : CivSettings.templates.values()) {
                        if (player.hasPermission("civ.perk." + p.simple_name)) {
                            resident.giveTemplate(p.simple_name);
                            perkMessage.append(p.display_name).append(", ");
                        }
                    }

                    perkMessage.append(CivSettings.localize.localizedString("PlayerLoginAsync_perksMsg2"));

                    CivMessage.send(resident, CivColor.LightGreen + perkMessage);
                } catch (InvalidConfiguration e) {
                    e.printStackTrace();
                }

                /* User was verified, lets see if it was the first time. */
//				PlatinumManager.givePlatinumOnce(resident,
//				CivSettings.platinumRewards.get("loginFirstVerified").name, 
//				CivSettings.platinumRewards.get("loginFirstVerified").amount, 
//				"Achievement! First time you've logged in while verified! %d");
            }
        }

        TaskMaster.asyncTask(new AsyncTask(this), 0);
    }

    public void setRejoinCooldown(Town town) {
        String value = String.valueOf(town.getCiv().getId());
        String key = getCooldownKey();
        CivGlobal.getSessionDB().add(key, value, 0, 0, 0);
    }

    public String getCooldownKey() {
        return "cooldown:" + this.getName();
    }

    public void cleanupCooldown() {
        CivGlobal.getSessionDB().delete_all(getCooldownKey());
    }

    public void validateJoinTown(Town town) throws CivException {
        if (this.hasTown() && this.getCiv() == town.getCiv()) {
            /* allow players to join the same civ, no probs */
            return;
        }

        long cooldownTime;
        int cooldownHours;
        try {
            cooldownHours = CivSettings.getInteger(CivSettings.civConfig, "global.join_civ_cooldown");
        } catch (InvalidConfiguration e) {
            e.printStackTrace();
            return;
        }

        cooldownTime = (long) cooldownHours * 60 * 60 * 1000; /*convert hours to milliseconds. */

        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getCooldownKey());
        if (entries.size() > 0) {
            Civilization oldCiv = CivGlobal.getCivFromId(Integer.parseInt(entries.get(0).value));
            if (oldCiv == null) {
                /* Hmm old civ is gone. */
                cleanupCooldown();
                return;
            }

//			if (oldCiv == town.getCiv()) {
//				/* We're rejoining the old civ, allow it. */
//				return;
//			}

            /* Check if cooldown is expired. */
            Date now = new Date();
            if (now.getTime() > (entries.get(0).time + cooldownTime)) {
                /* Entry is expired, allow cooldown and cleanup. */
                cleanupCooldown();
                return;
            }

            throw new CivException(CivSettings.localize.localizedString("var_resident_cannotJoinCivJustLeft1", cooldownHours));
        }
    }

    public LinkedList<Perk> getPersonalTemplatePerks(ConfigBuildableInfo info) {
        LinkedList<Perk> templates = new LinkedList<>();

        for (Perk perk : this.perks.values()) {
            CustomPersonalTemplate customTemplate = (CustomPersonalTemplate) perk.getComponent("CustomPersonalTemplate");
            if (customTemplate == null) {
                continue;
            }

            if (customTemplate.getString("id").equals(info.id)) {
                templates.add(perk);
            }
        }
        return templates;
    }

    public ArrayList<Perk> getUnboundTemplatePerks(ArrayList<Perk> alreadyBoundPerkList, ConfigBuildableInfo info) {
        ArrayList<Perk> unboundPerks = new ArrayList<>();
        for (Perk ourPerk : perks.values()) {

			if (ourPerk.getIdent().contains("template")) {
				continue;
			}
			CustomTemplate customTemplate = (CustomTemplate) ourPerk.getComponent("CustomTemplate");
			if (customTemplate == null) {
				continue;
			}

			if (!customTemplate.getString("template").equals(info.template_base_name)) {
				/* Not the correct template. */
				continue;
			}

			for (Perk perk : alreadyBoundPerkList) {
				if (perk.getIdent().equals(ourPerk.getIdent())) {
					/* Perk is already bound in this town, do not display for binding. */
					break;
				}
			}

			unboundPerks.add(ourPerk);
		}

        return unboundPerks;
    }

    public boolean isControlBlockInstantBreak() {
        return controlBlockInstantBreak;
    }

    public void setControlBlockInstantBreak(boolean controlBlockInstantBreak) {
        this.controlBlockInstantBreak = controlBlockInstantBreak;
    }

	@SuppressWarnings("unused")
    public boolean isMuted() {
        return muted;
    }
	@SuppressWarnings("unused")
    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isCombatInfo() {
        return combatInfo;
    }

    public void setCombatInfo(boolean combatInfo) {
        this.combatInfo = combatInfo;
    }

    public boolean isInactiveForDays(int days) {
        Calendar now = Calendar.getInstance();
        Calendar expire = Calendar.getInstance();
        expire.setTimeInMillis(this.getLastOnline());

        expire.add(Calendar.DATE, days);

		return now.after(expire);
	}

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Inventory startTradeWith(Resident resident) {
        try {
            Player player = CivGlobal.getPlayer(this);
            if (player.isDead()) {
                throw new CivException(CivSettings.localize.localizedString("resident_tradeErrorPlayerDead"));
            }
            Inventory inv = Bukkit.createInventory(player, 9 * 5, this.getName() + " : " + resident.getName());

            /*
             * Set up top and bottom layer with buttons.
             */

            /* Top part which is for the other resident. */
            ItemStack signStack = LoreGuiItem.build("", Material.WOOL, CivData.DATA_WOOL_WHITE, "");
            int start = 0;
            for (int i = start; i < (9 + start); i++) {
                if ((i - start) == 8) {
                    ItemStack guiStack = LoreGuiItem.build(resident.getName() + " Confirm",
                            Material.WOOL, CivData.DATA_WOOL_RED,
                            CivColor.LightGreen + CivSettings.localize.localizedString("var_resident_tradeWait1", CivColor.LightBlue + resident.getName()),
                            CivColor.LightGray + " " + CivSettings.localize.localizedString("resident_tradeWait2"));
                    inv.setItem(i, guiStack);
                } else if ((i - start) == 7) {
                    ItemStack guiStack = LoreGuiItem.build(CivSettings.CURRENCY_NAME + " " + CivSettings.localize.localizedString("resident_tradeOffered"),
                            Material.NETHER_BRICK_ITEM, 0,
                            CivColor.Yellow + "0 " + CivSettings.CURRENCY_NAME);
                    inv.setItem(i, guiStack);
                } else {
                    inv.setItem(i, signStack);
                }
            }

            start = 4 * 9;
            for (int i = start; i < (9 + start); i++) {
                if ((i - start) == 8) {
                    ItemStack guiStack = LoreGuiItem.build(CivSettings.localize.localizedString("resident_tradeYourConfirm"),
                            Material.WOOL, CivData.DATA_WOOL_RED,
                            CivColor.Gold + CivSettings.localize.localizedString("resident_tradeClicktoConfirm"));
                    inv.setItem(i, guiStack);

                } else if ((i - start) == 0) {
                    ItemStack guiStack = LoreGuiItem.build(CivSettings.localize.localizedString("resident_tradeRemove") + " " + CivSettings.CURRENCY_NAME,
                            Material.NETHER_BRICK_ITEM, 0,
                            CivColor.Gold + CivSettings.localize.localizedString("resident_tradeRemove100") + " " + CivSettings.CURRENCY_NAME,
                            CivColor.Gold + CivSettings.localize.localizedString("resident_tradeRemove1000") + " " + CivSettings.CURRENCY_NAME);
                    inv.setItem(i, guiStack);
                } else if ((i - start) == 1) {
                    ItemStack guiStack = LoreGuiItem.build(CivSettings.localize.localizedString("resident_tradeAdd") + " " + CivSettings.CURRENCY_NAME,
                            Material.GOLD_INGOT, 0,
                            CivColor.Gold + CivSettings.localize.localizedString("resident_tradeAdd100") + " " + CivSettings.CURRENCY_NAME,
                            CivColor.Gold + CivSettings.localize.localizedString("resident_tradeAdd1000") + " " + CivSettings.CURRENCY_NAME);
                    inv.setItem(i, guiStack);
                } else if ((i - start) == 7) {
                    ItemStack guiStack = LoreGuiItem.build(CivSettings.CURRENCY_NAME + " " + CivSettings.localize.localizedString("resident_tradeOffered"),
                            Material.NETHER_BRICK_ITEM, 0,
                            CivColor.Yellow + "0 " + CivSettings.CURRENCY_NAME);
                    inv.setItem(i, guiStack);
                } else {
                    inv.setItem(i, signStack);
                }
            }

            /*
             * Set up middle divider.
             */
            start = 2 * 9;
            for (int i = start; i < (9 + start); i++) {
                inv.setItem(i, signStack);
            }

            player.openInventory(inv);
            return inv;
        } catch (CivException e) {
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("possibleCheaters.txt", true)))) {
                out.println("trade:" + this.getName() + " WITH " + resident.getName() + " and was dead");
            } catch (IOException e1) {
                //exception handling left as an exercise for the reader
            }


            CivMessage.sendError(this, CivSettings.localize.localizedString("resident_tradeCouldNotTrade") + " " + e.getMessage());
            CivMessage.sendError(resident, CivSettings.localize.localizedString("resident_tradeCouldNotTrade") + " " + e.getMessage());
            return null;
        }

    }

    public boolean hasTechForItem(ItemStack stack) {
        if (this.isInsideArena()) {
            return true;
        }

        LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
        if (craftMat == null) {
            return true;
        }

        if (craftMat.getConfigMaterial().required_tech == null) {
            return true;
        }

        if (!this.hasTown()) {
            return false;
        }

        /* Parse technoloies */
        String[] split = craftMat.getConfigMaterial().required_tech.split(",");
        for (String tech : split) {
            tech = tech.replace(" ", "");
            if (!this.getCiv().hasTechnology(tech)) {
                return false;
            }
        }

        return true;
    }

    public Date getLastKilledTime() {
        return lastKilledTime;
    }

    public void setLastKilledTime(Date lastKilledTime) {
        this.lastKilledTime = lastKilledTime;
    }

    public int getRespawnTimeArena() {
        return this.respawntime;
    }

    public void clearRespawnTimeArena() {
        respawntime = 0;
    }

    public void increaseRespawnTime() {
        this.respawntime += 2;
        if (respawntime > 30) {
            respawntime = 30;
        }
    }
	@SuppressWarnings("unused")
    public Date getMuteExpires() {
        return muteExpires;
    }
	@SuppressWarnings("unused")
    public void setMuteExpires(Date muteExpires) {
        this.muteExpires = muteExpires;
    }

    public String getItemMode() {
        return itemMode;
    }
	@SuppressWarnings("unused")
    public void setItemMode(String itemMode) {
        this.itemMode = itemMode;
    }

    public void toggleItemMode() {
        if (this.itemMode.equals("all")) {
            this.itemMode = "rare";
            CivMessage.send(this, CivColor.LightGreen + CivSettings.localize.localizedString("resident_toggleItemRare"));
        } else if (this.itemMode.equals("rare")) {
            this.itemMode = "none";
            CivMessage.send(this, CivColor.LightGreen + CivSettings.localize.localizedString("resident_toggleItemNone"));
        } else {
            this.itemMode = "all";
            CivMessage.send(this, CivColor.LightGreen + CivSettings.localize.localizedString("resident_toggleItemAll"));
        }
        this.save();
    }

    public void setLastIP(String hostAddress) {
        this.lastIP = hostAddress;
    }

    public String getLastIP() {
        return this.lastIP;
    }

    public void teleportHome() {
        Player player;
        try {
            player = CivGlobal.getPlayer(this);
            teleportHome(player);
        } catch (CivException ignored) {
		}
    }

    public void teleportHome(Player player) {
        if (this.hasTown()) {
            TownHall townhall = this.getTown().getTownHall();
            if (townhall != null) {
                BlockCoord coord = townhall.getRandomRevivePoint();
                player.teleport(coord.getLocation());
            }
        } else {
            World world = Bukkit.getWorld("world");
            player.teleport(world.getSpawnLocation());
        }
    }

    public boolean canDamageControlBlock() {
        if (this.hasTown()) {
			return this.getCiv().getCapitolStructure().isValid();
        }

        return true;
    }

    public boolean isUsesAntiCheat() throws CivException {
        CivGlobal.getPlayer(this);
        return usesAntiCheat;
    }
	@SuppressWarnings("unused")
    public void setUsesAntiCheat(boolean usesAntiCheat) {
        this.usesAntiCheat = usesAntiCheat;
    }

    public boolean hasTeam() {
        ArenaTeam team = ArenaTeam.getTeamForResident(this);
		return team != null;
	}

    public ArenaTeam getTeam() {
		return ArenaTeam.getTeamForResident(this);
    }

    public boolean isTeamLeader() {
        ArenaTeam team = ArenaTeam.getTeamForResident(this);
        if (team == null) {
            return false;
        }

		return team.getLeader() == this;
	}

    public void saveInventory() {
        try {
            Player player = CivGlobal.getPlayer(this);
            String serial = InventorySerializer.InventoryToString(player.getInventory());
            this.setSavedInventory(serial);
            this.save();
        } catch (CivException ignored) {
        }
    }

    public void clearInventory() {
        try {
            Player player = CivGlobal.getPlayer(this);
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
        } catch (CivException ignored) {
        }
    }

    public void restoreInventory() {
        if (this.savedInventory == null) {
            return;
        }

        try {
            Player player = CivGlobal.getPlayer(this);
            clearInventory();
            InventorySerializer.StringToInventory(player.getInventory(), this.savedInventory);
            this.setSavedInventory(null);
            this.save();
        } catch (CivException e) {
            // Player offline??
            e.printStackTrace();
            this.setSavedInventory(null);
            this.save();
        }
    }
	@SuppressWarnings("unused")
    public String getSavedInventory() {
        return savedInventory;
    }

    public void setSavedInventory(String savedInventory) {
        this.savedInventory = savedInventory;
    }

    public Arena getCurrentArena() {
        if (this.getTeam() == null) {
            return null;
        }

        return this.getTeam().getCurrentArena();
    }

    public boolean isInsideArena() {

        if (!hasTeam()) {
            this.insideArena = false;
            return false;
        }

        try {
            Player player = CivGlobal.getPlayer(this);

            if (player.getWorld().getName().equals("world")) {
                this.insideArena = false;
                return false;
            }

        } catch (CivException e) {
            return false;
        }

        return this.insideArena;
    }

    public void setInsideArena(boolean inside) {
        this.insideArena = inside;
    }

    public boolean isProtected() {
        return isProtected;
    }

    public void setisProtected(boolean prot) {
        isProtected = prot;
    }

    public void showPerkPage(int pageNumber) {
        Player player;
        try {
            player = CivGlobal.getPlayer(this);
        } catch (CivException e) {
            return;
        }

        Inventory inv = Bukkit.getServer().createInventory(player, CivTutorial.MAX_CHEST_SIZE * 9, CivSettings.localize.localizedString("resident_perksGuiHeading"));

        for (Perk perk : perks.values()) {

            if (perk.getIdent().startsWith("temp")) {
                ItemStack stack = LoreGuiItem.build(perk.configPerk.display_name,
                        perk.configPerk.type_id,
                        perk.configPerk.data, CivColor.LightBlue + CivSettings.localize.localizedString("resident_perksGuiClickToView"),
                        CivColor.LightBlue + CivSettings.localize.localizedString("resident_perksGuiTheseTemplates"));
                stack = LoreGuiItem.setAction(stack, "ShowTemplateType");
                stack = LoreGuiItem.setActionData(stack, "perk", perk.configPerk.id);

                inv.addItem(stack);
            } else if (perk.getIdent().startsWith("perk")) {
                ItemStack stack = LoreGuiItem.build(perk.getDisplayName(),
                        perk.configPerk.type_id,
                        perk.configPerk.data, CivColor.Gold + CivSettings.localize.localizedString("resident_perksGui_clickToActivate"),
                        "Unlimted Uses");
                stack = LoreGuiItem.setAction(stack, "ActivatePerk");
                stack = LoreGuiItem.setActionData(stack, "perk", perk.configPerk.id);

                inv.addItem(stack);

            }

        }

        player.openInventory(inv);
    }

    public void showTemplatePerks(String name) {
        Player player;
        try {
            player = CivGlobal.getPlayer(this);
        } catch (CivException e) {
            return;
        }

        Inventory inv = Bukkit.getServer().createInventory(player, CivTutorial.MAX_CHEST_SIZE * 9, CivSettings.localize.localizedString("resident_perksGui_templatesHeading") + " " + name);

        for (Perk perk : perks.values()) {
			if (perk.getIdent().contains("tpl_" + name)) {
                ItemStack stack = LoreGuiItem.build(perk.configPerk.display_name,
                        perk.configPerk.type_id,
                        perk.configPerk.data, CivColor.Gold + CivSettings.localize.localizedString("resident_perksGui_clickToActivate"),
                        CivColor.LightBlue + "Count: " + perk.count);
                stack = LoreGuiItem.setAction(stack, "ActivatePerk");
                stack = LoreGuiItem.setActionData(stack, "perk", perk.configPerk.id);

                inv.addItem(stack);
            }
        }

        player.openInventory(inv);
    }

    public UUID getUUID() {
        return uid;
    }

    public String getUUIDString() {
        return uid.toString();
    }

    public void setUUID(UUID uid) {
        this.uid = uid;
    }

    public double getWalkingModifier() {
        return walkingModifier;
    }
	@SuppressWarnings("unused")
    public void setWalkingModifier(double walkingModifier) {
        this.walkingModifier = walkingModifier;
    }

    public void calculateWalkingModifier(Player player) {
        double speed = CivSettings.normal_speed;

        /* Set speed from armor. */
        if (Unit.isWearingFullComposite(player)) {
            speed *= CivSettings.T4_leather_speed;
        } else if (Unit.isWearingFullHardened(player)) {
            speed *= CivSettings.T3_leather_speed;
        } else if (Unit.isWearingFullRefined(player)) {
            speed *= CivSettings.T2_leather_speed;
        } else if (Unit.isWearingFullBasicLeather(player)) {
            speed *= CivSettings.T1_leather_speed;
        } else {
            if (Unit.isWearingAnyDiamond(player)) {
                speed *= CivSettings.T4_metal_speed;
            } else if (Unit.isWearingAnyGold(player)) {
                speed *= CivSettings.T3_metal_speed;
            } else if (Unit.isWearingAnyChain(player)) {
                speed *= CivSettings.T2_metal_speed;
            } else if (Unit.isWearingAnyIron(player)) {
                speed *= CivSettings.T1_metal_speed;
            }
        }
        this.walkingModifier = speed;
    }

    public boolean isTitleAPI() {
        return titleAPI;
    }

    public void setTitleAPI(boolean titleAPI) {
        this.titleAPI = titleAPI;
    }
}
