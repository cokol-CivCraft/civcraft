/*
 * AVRGAMING LLC
 * __________________
 *
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,TICE:  All information contained herein is, and remains
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

import com.avrgaming.civcraft.arena.Arena;
import com.avrgaming.civcraft.arena.ArenaTeam;
import com.avrgaming.civcraft.camp.Camp;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.database.SQLController;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.event.EventTimer;
import com.avrgaming.civcraft.exception.AlreadyRegisteredException;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.interactive.InteractiveResponse;
import com.avrgaming.civcraft.items.units.Unit;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.*;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.TownHall;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.threading.tasks.BuildPreviewAsyncTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.PlayerBlockChangeUtil;
import com.avrgaming.civcraft.util.SimpleBlock;
import gpl.InventorySerializer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.MaterialData;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Resident extends SQLObject {

    private Town town = null;
    private Camp camp = null;
    private boolean townChat = false;
    private boolean civChat = false;
    private boolean combatInfo = true;
    private boolean titleAPI = true;
    private int respawntime;
    private Player player;

    /* Town or civ to chat in besides your own. */
    private Town townChatOverride = null;
    private Civilization civChatOverride = null;
    private boolean permOverride = false;
    private boolean sbperm = false;
    private boolean controlBlockInstantBreak = false;
    private boolean dontSaveTown = false;
    private String timezone;
    private long registered;
    private long lastOnline;
    private int daysTilEvict;
    private boolean givenKit;
    private final ConcurrentHashMap<String, Integer> friends = new ConcurrentHashMap<>();
    private EconObject treasury;

    private boolean interactiveMode = false;
    private InteractiveResponse interactiveResponse = null;
    private BuildPreviewAsyncTask previewTask = null;

    private double spyExposure = 0.0;
    public static int MAX_SPY_EXPOSURE = 1000;
    private boolean performingMission = false;

    private Town selectedTown = null;

    public String desiredCivName;
    public String desiredCapitolName;
    public String desiredTownName;
    public Location desiredTownLocation = null;
    public Template desiredTemplate = null;

    /* XXX
     * This buildable is used as place to store which buildable we're working on when interacting
     * with GUI items. We want to be able to pass the buildable object to the GUI's action function,
     * but there isn't a good way to do this ATM. If we had a way to send arbitary objects it would
     * be better. Could we store it here on the resident object?
     */
    public Buildable.BuildTaskInstance pendingBuildable;
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
    private Date lastKilledTime = null;
    private String lastIP = "";
    private double walkingModifier = CivSettings.normal_speed;
    public String debugTown;

    public Resident(UUID uid, String name) throws InvalidNameException {
        this.setName(name);
        setUUID(uid);
        this.player = Bukkit.getPlayer(uid);
        this.treasury = CivGlobal.createEconObject(this);
        setTimezoneToServerDefault();
        loadSettings();
    }

    @Override
    public void setUUID(UUID uuid) {
        super.setUUID(uuid);
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
        if (!SQLController.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQLController.tb_prefix + TABLE_NAME + " (" +
                    "`name` VARCHAR(64) NOT NULL," +
                    "`uuid` VARCHAR(256) NOT NULL DEFAULT 'UNKNOWN'," +
                    "`currentName` VARCHAR(64) DEFAULT NULL," +
                    "`town_uuid` VARCHAR(36)," +
                    "`lastOnline` BIGINT NOT NULL," +
                    "`registered` BIGINT NOT NULL," +
                    "`friends` mediumtext," +
                    "`debt` double DEFAULT 0," +
                    "`coins` double DEFAULT 0," +
                    "`daysTilEvict` mediumint DEFAULT NULL," +
                    "`givenKit` bool NOT NULL DEFAULT '0'," +
                    "`camp_uuid` VARCHAR(36)," +
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
                    "PRIMARY KEY (`uuid`)" + ")";

            SQLController.makeTable(table_create);
            CivLog.info("Created " + TABLE_NAME + " table");
        }
    }

    @Override
    public void load(ResultSet rs) throws SQLException, InvalidNameException {
        this.setUUID(UUID.fromString(rs.getString("uuid")));
        this.setName(rs.getString("name"));
        UUID townUUID = UUID.fromString(rs.getString("town_uuid"));
        UUID campUUID = UUID.fromString(rs.getString("camp_uuid"));
        this.lastIP = rs.getString("last_ip");
        this.debugTown = rs.getString("debug_town");

        this.treasury = CivGlobal.createEconObject(this);
        this.getTreasury().setBalance(rs.getDouble("coins"), false);
        this.setGivenKit(rs.getBoolean("givenKit"));
        this.setTimezone(rs.getString("timezone"));
        this.loadFlagSaveString(rs.getString("flags"));
        this.savedInventory = rs.getString("savedInventory");
        this.insideArena = rs.getBoolean("insideArena");
        this.isProtected = rs.getBoolean("isProtected");
        this.player = Bukkit.getPlayer(getUUID());

        if (this.getTimezone() == null) {
            this.setTimezoneToServerDefault();
        }

        this.setTown(Town.getTownFromUUID(townUUID));
        if (this.town == null && !townUUID.equals(NULL_UUID)) {
            CivLog.error("COULD NOT FIND TOWN(" + townUUID + ") FOR RESIDENT(" + this.getUUID() + ") Name:" + this.getName());
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

        this.setCamp(CivGlobal.getCampFromUUID(campUUID));
        if (this.camp != null) {
            camp.addMember(this);
        } else if (!campUUID.equals(NULL_UUID)) {
            CivLog.error("COULD NOT FIND CAMP(" + campUUID + ") FOR RESIDENT(" + this.getUUID() + ") Name:" + this.getName());
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

        for (String s : str.split(",")) {
            switch (s.toLowerCase()) {
                case "map" -> this.setShowMap(true);
                case "showtown" -> this.setShowTown(true);
                case "showciv" -> this.setShowCiv(true);
                case "showscout" -> this.setShowScout(true);
                case "info" -> this.setShowInfo(true);
                case "combatinfo" -> this.setCombatInfo(true);
                case "titleapi" -> this.setTitleAPI(CivSettings.hasTitleAPI);
                case "itemmoderare" -> this.itemMode = "rare";
                case "itemmodenone" -> this.itemMode = "none";
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
            hashmap.put("town_uuid", this.getTown().getUUID().toString());
        } else {
            if (!dontSaveTown) {
                hashmap.put("town_uuid", NULL_UUID);
            }
        }

        if (this.getCamp() != null) {
            hashmap.put("camp_uuid", this.getCamp().getUUID().toString());
        } else {
            hashmap.put("camp_uuid", NULL_UUID.toString());
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

        SQLController.updateNamedObject(this, hashmap, TABLE_NAME);
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
        SQLController.deleteByName(this.getName(), TABLE_NAME);
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
        try {
            Player player = CivGlobal.getPlayer(this);
            CivMessage.send(player, ChatColor.YELLOW + CivSettings.localize.localizedString("var_resident_debtmsg", this.getTreasury().getDebt(), CivSettings.CURRENCY_NAME));
            CivMessage.send(player, ChatColor.GRAY + CivSettings.localize.localizedString("var_resident_debtEvictAlert1", this.daysTilEvict));
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
                CivMessage.send(CivGlobal.getPlayer(this), ChatColor.YELLOW + CivSettings.localize.localizedString("resident_evictedAlert"));
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
        return String.join(",", friends.keySet());
    }

    private void loadFriendsFromSaveString(String string) {
        for (String str : string.split(",")) {
            friends.put(str, 1);
        }
    }

    public void removeFriend(Resident friendToAdd) {
        friends.remove(friendToAdd.getName());
    }

    public String getGroupsString() {
        StringBuilder out = new StringBuilder();

        for (PermissionGroup grp : CivGlobal.getGroups()) {
            if (!grp.hasMember(this)) {
                continue;
            }
            if (grp.getTown() != null) {
                out.append(grp.isProtectedGroup() ? String.valueOf(ChatColor.LIGHT_PURPLE) : String.valueOf(ChatColor.WHITE));
                out.append(grp.getName()).append("(").append(grp.getTown().getName()).append(")");
            } else if (grp.getCiv() != null) {
                out.append(ChatColor.GOLD).append(grp.getName()).append("(").append(grp.getCiv().getName()).append(")");
            }

            out.append(", ");
        }

        return out.toString();
    }

    public void warnEvict() {
        try {
            CivMessage.send(CivGlobal.getPlayer(this), ChatColor.YELLOW + CivSettings.localize.localizedString("var_resident_evictionNotice1", this.getDaysTilEvict()));
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

    public int takeItemsInHand(MaterialData materialData) throws CivException {
        Player player = CivGlobal.getPlayer(this);
        PlayerInventory inv = player.getInventory();

        if (!inv.getItemInMainHand().getData().equals(materialData)) {
            return 0;
        }

        ItemStack stack = player.getInventory().getItemInMainHand();
        int count = stack.getAmount();
        inv.removeItem(stack);

        player.updateInventory();
        return count;
    }

    public boolean takeItem(Material itemId, int itemData, int amount) throws CivException {
        Player player = CivGlobal.getPlayer(this);
        Inventory inv = player.getInventory();

        if (!inv.contains(itemId)) {
            return false;
        }

        for (ItemStack stack : inv.all(itemId).values()) {
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

    public int giveItem(Material itemId, short damage, int amount) throws CivException {
        Player player = CivGlobal.getPlayer(this);
        Inventory inv = player.getInventory();
        ItemStack stack = new ItemStack(itemId, amount, damage);
        int leftoverAmount = inv.addItem(stack).values().stream().mapToInt(ItemStack::getAmount).sum();
        player.updateInventory();
        return amount - leftoverAmount;
    }

    public boolean buyItem(String itemName, Material id, byte data, double price, int amount) throws CivException {
        if (!this.getTreasury().hasEnough(price)) {
            throw new CivException(CivSettings.localize.localizedString("resident_notEnoughMoney") + " " + CivSettings.CURRENCY_NAME);
        }

        int bought = giveItem(id, data, amount);
        this.getTreasury().withdraw(price);
        if (bought == amount) {
            throw new CivException(CivSettings.localize.localizedString("resident_buyInvenFull"));
        }
        takeItem(id, data, bought);
        return true;
    }

    public Civilization getCiv() {
        if (this.getTown() == null) {
            return null;
        }
        return this.getTown().getCiv();
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
        if (this.getTown() == null) {
            return;
        }
        for (Buildable struct : this.getTown().invalidStructures) {
            CivMessage.send(player, String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD +
                    CivSettings.localize.localizedString("var_resident_structInvalidAlert1", struct.getDisplayName(), struct.getCenterLocation()) +
                    " " + CivSettings.localize.localizedString("resident_structInvalidAlert2") + " " + struct.getInvalidReason());
        }

        /* Show any event messages. */
        if (this.getTown().getActiveEvent() != null) {
            CivMessage.send(player, ChatColor.YELLOW + CivSettings.localize.localizedString("var_resident_eventNotice1", this.getTown().getActiveEvent().configRandomEvent.name));
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
        previewTask.runTaskTimer(CivCraft.getPlugin(), 0, previewTask.period);
    }

    public void undoPreview() {
        if (this.previewUndo == null) {
            this.previewUndo = new ConcurrentHashMap<>();
            return;
        }

        if (this.previewTask != null) {
            previewTask.cancel();
        }

        try {
            Player player = CivGlobal.getPlayer(this);
            PlayerBlockChangeUtil util = new PlayerBlockChangeUtil();
            for (BlockCoord coord : this.previewUndo.keySet()) {
                SimpleBlock sb = this.previewUndo.get(coord);
                util.addUpdateBlock(player.getName(), coord, sb.getMaterialData());
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


    public boolean isBanned() {
        return this.getPlayer().isBanned();
    }

    public void setBanned(boolean banned, String reason, Date expires, String bannedBy) {
        if (banned) {
            Bukkit.getBanList(BanList.Type.NAME).addBan(this.getPlayer().getName(), reason, expires, bannedBy);
        } else {
            Bukkit.getBanList(BanList.Type.NAME).pardon(this.getPlayer().getName());
        }
    }

    public double getSpyExposure() {
        return spyExposure;
    }

    public void setSpyExposure(double spyExposure) {
        this.spyExposure = spyExposure;
        try {
            CivGlobal.getPlayer(this).setExp((float) (spyExposure / MAX_SPY_EXPOSURE));
        } catch (CivException ignored) {
        }

    }

    public boolean isPerformingMission() {
        return performingMission;
    }

    public void setPerformingMission(boolean performingMission) {
        this.performingMission = performingMission;
    }

    public void setRejoinCooldown(Town town) {
        String value = String.valueOf(town.getCiv().getUUID());
        String key = getCooldownKey();
        CivGlobal.getSessionDB().add(key, value, NamedObject.NULL_UUID, NamedObject.NULL_UUID, NamedObject.NULL_UUID);
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

        int cooldownHours = CivSettings.civConfig.getInt("global.join_civ_cooldown", 12);
        long cooldownTime = (long) cooldownHours * 60 * 60 * 1000; /*convert hours to milliseconds. */

        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getCooldownKey());
        if (!entries.isEmpty()) {
            Civilization oldCiv = Civilization.getCivFromUUID(UUID.fromString(entries.get(0).value));
            if (oldCiv == null) {
                /* Hmm, old civ is gone. */
                cleanupCooldown();
                return;
            }

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

    public boolean isControlBlockInstantBreak() {
        return controlBlockInstantBreak;
    }

    public void setControlBlockInstantBreak(boolean controlBlockInstantBreak) {
        this.controlBlockInstantBreak = controlBlockInstantBreak;
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

        return !now.after(expire);
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
                            ChatColor.GREEN + CivSettings.localize.localizedString("var_resident_tradeWait1", ChatColor.AQUA + resident.getName()),
                            ChatColor.GRAY + " " + CivSettings.localize.localizedString("resident_tradeWait2"));
                    inv.setItem(i, guiStack);
                } else if ((i - start) == 7) {
                    ItemStack guiStack = LoreGuiItem.build(CivSettings.CURRENCY_NAME + " " + CivSettings.localize.localizedString("resident_tradeOffered"),
                            Material.NETHER_BRICK_ITEM, 0,
                            ChatColor.YELLOW + "0 " + CivSettings.CURRENCY_NAME);
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
                            ChatColor.GOLD + CivSettings.localize.localizedString("resident_tradeClicktoConfirm"));
                    inv.setItem(i, guiStack);

                } else if ((i - start) == 0) {
                    ItemStack guiStack = LoreGuiItem.build(CivSettings.localize.localizedString("resident_tradeRemove") + " " + CivSettings.CURRENCY_NAME,
                            Material.NETHER_BRICK_ITEM, 0,
                            ChatColor.GOLD + CivSettings.localize.localizedString("resident_tradeRemove100") + " " + CivSettings.CURRENCY_NAME,
                            ChatColor.GOLD + CivSettings.localize.localizedString("resident_tradeRemove1000") + " " + CivSettings.CURRENCY_NAME);
                    inv.setItem(i, guiStack);
                } else if ((i - start) == 1) {
                    ItemStack guiStack = LoreGuiItem.build(CivSettings.localize.localizedString("resident_tradeAdd") + " " + CivSettings.CURRENCY_NAME,
                            Material.GOLD_INGOT, 0,
                            ChatColor.GOLD + CivSettings.localize.localizedString("resident_tradeAdd100") + " " + CivSettings.CURRENCY_NAME,
                            ChatColor.GOLD + CivSettings.localize.localizedString("resident_tradeAdd1000") + " " + CivSettings.CURRENCY_NAME);
                    inv.setItem(i, guiStack);
                } else if ((i - start) == 7) {
                    ItemStack guiStack = LoreGuiItem.build(CivSettings.CURRENCY_NAME + " " + CivSettings.localize.localizedString("resident_tradeOffered"),
                            Material.NETHER_BRICK_ITEM, 0,
                            ChatColor.YELLOW + "0 " + CivSettings.CURRENCY_NAME);
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

        if (craftMat == null || craftMat.getConfigMaterial().required_tech == null) {
            return true;
        }

        if (!this.hasTown()) {
            return false;
        }

        /* Parse technoloies */
        for (String tech : craftMat.getConfigMaterial().required_tech.split(",")) {
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


    public String getItemMode() {
        return itemMode;
    }

    public void setItemMode(String itemMode) {
        this.itemMode = itemMode;
    }

    public void toggleItemMode() {
        if (this.itemMode.equals("all")) {
            this.itemMode = "rare";
            CivMessage.send(this, ChatColor.GREEN + CivSettings.localize.localizedString("resident_toggleItemRare"));
        } else if (this.itemMode.equals("rare")) {
            this.itemMode = "none";
            CivMessage.send(this, ChatColor.GREEN + CivSettings.localize.localizedString("resident_toggleItemNone"));
        } else {
            this.itemMode = "all";
            CivMessage.send(this, ChatColor.GREEN + CivSettings.localize.localizedString("resident_toggleItemAll"));
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
        } catch (CivException e) {
            e.printStackTrace();
        }
    }

    public void clearInventory() {
        try {
            Player player = CivGlobal.getPlayer(this);
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
        } catch (CivException e) {
            e.printStackTrace();
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

    public String getUUIDString() {
        return getUUID().toString();
    }

    public double getWalkingModifier() {
        return walkingModifier;
    }

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


    public boolean hasEnlightenment() {
        if (this.hasTown()) {
            return this.getTown().hasTechnology("tech_enlightenment");
        }
        return false;
    }


    /* NativeTown вернётся после изменения системы резидентов, я ебал*/


    public Player getPlayer() {
        return player;
    }

    public ConfigBuildableInfo getPendingBuildable() {
        return this.pendingBuildableInfo;
    }

    public void setPendingBuildable(ConfigBuildableInfo p) {
        this.pendingBuildableInfo = p;
    }
}
