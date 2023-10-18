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
package com.avrgaming.civcraft.object;

import com.avrgaming.civcraft.components.*;
import com.avrgaming.civcraft.config.*;
import com.avrgaming.civcraft.database.SQLController;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.exception.AlreadyRegisteredException;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.interactive.InteractiveBuildableRefresh;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.items.units.Unit;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.randomevents.RandomEvent;
import com.avrgaming.civcraft.structure.*;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.sync.SyncUpdateTags;
import com.avrgaming.civcraft.threading.tasks.BuildAsyncTask;
import com.avrgaming.civcraft.threading.tasks.BuildUndoTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.DateUtil;
import com.avrgaming.civcraft.util.ItemFrameStorage;
import com.avrgaming.civcraft.war.War;
import net.minecraft.server.v1_12_R1.NBTCompressedStreamTools;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Math.max;

public class Town extends SQLObject {

    public static final Map<String, Town> towns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Resident> residents = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Resident> fakeResidents = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<ChunkCoord, TownChunk> townChunks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ChunkCoord, CultureChunk> cultureChunks = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<BlockCoord, Wonder> wonders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BlockCoord, Structure> structures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BlockCoord, Buildable> disabledBuildables = new ConcurrentHashMap<>();

    private final ArrayList<Granary> granaryAL = new ArrayList<>();

    private int level;
    private Civilization civ;
    private Civilization motherCiv;
    private int daysInDebt;

    private String granaryResources;

    /* Hammers */
    private double baseHammers = 1.0;
    private double extraHammers;
    public Buildable currentStructureInProgress;
    public Buildable currentWonderInProgress;

    /* Culture */
    private int culture;

    private PermissionGroup defaultGroup;
    private PermissionGroup mayorGroup;
    private PermissionGroup assistantGroup;

    /* Beakers */
    private double unusedBeakers;

    // These are used to resolve reverse references after the database loads.
    private String defaultGroupName;
    private String mayorGroupName;
    private String assistantGroupName;

    public ArrayList<TownChunk> savedEdgeBlocks = new ArrayList<>();
    public HashSet<Town> townTouchList = new HashSet<>();

    private final ConcurrentHashMap<String, PermissionGroup> groups = new ConcurrentHashMap<>();
    private EconObject treasury;
    private final ConcurrentHashMap<String, ConfigTownUpgrade> upgrades = new ConcurrentHashMap<>();

    /* This gets populated periodically from a synchronous timer so it will be accessible from async tasks. */
    private ConcurrentHashMap<String, BonusGoodie> bonusGoodies = new ConcurrentHashMap<>();

    private BuffManager buffManager = new BuffManager();

    private boolean pvp = false;

    public ArrayList<BuildAsyncTask> build_tasks = new ArrayList<>();
    public ArrayList<BuildUndoTask> undo_tasks = new ArrayList<>();
    public Buildable lastBuildableBuilt = null;

    public boolean leaderWantsToDisband = false;
    public boolean mayorWantsToDisband = false;
    public HashSet<String> outlaws = new HashSet<>();

    public boolean claimed = false;
    public boolean defeated = false;
    public LinkedList<Buildable> invalidStructures = new LinkedList<>();

    /* XXX kind of a hacky way to save the bank's level information between build undo calls */
    public int saved_bank_level = 1;
    public int saved_store_level = 1;
    public int saved_library_level = 1;
    public int saved_trommel_level = 1;
    public int saved_tradeship_upgrade_levels = 1;
    public int saved_grocer_levels = 1;
    public int saved_quarry_level = 1;
    public int saved_fish_hatchery_level = 1;
    public double saved_bank_interest_amount = 0;

    /* Happiness Stuff */
    private double baseHappy = 0.0;
    private double baseUnhappy = 0.0;

    private RandomEvent activeEvent;

    /* Last time someone used /build refreshblocks, make sure they can do it only so often.	 */
    private Date lastBuildableRefresh = null;
    private Date created_date;

    /*
     * Time it takes before a new attribute is calculated
     * Otherwise its loaded from the cache.
     */
    public static final int ATTR_TIMEOUT_SECONDS = 5;

    public static Town getTown(String name) {
        if (name == null) {
            return null;
        }
        return towns.get(name.toLowerCase());
    }

    //TODO make lookup via ID faster(use hashtable)
    public static Town getTownFromId(int id) {
        if (id == 0) {
            return null;
        }
        for (Town t : towns.values()) {
            if (t.getId() == id) {
                return t;
            }
        }
        return null;
    }

    public static Town getTownFromUUID(UUID id) {
        if (id == null) {
            return null;
        }
        for (Town t : towns.values()) {
            if (t.getUUID().equals(id)) {
                return t;
            }
        }
        return null;
    }

    public static void addTown(Town town) {
        towns.put(town.getName().toLowerCase(), town);
    }

    public static Collection<Town> getTowns() {
        return towns.values();
    }

    public static void removeTown(Town town) {
        towns.remove(town.getName().toLowerCase());
    }

    public static class AttrCache {
        public Date lastUpdate;
        public AttrSource sources;
    }

    public HashMap<String, AttrCache> attributeCache = new HashMap<>();

    private double baseGrowth = 0.0;

    public static final String TABLE_NAME = "TOWNS";

    public static void init() throws SQLException {
        if (!SQLController.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQLController.tb_prefix + TABLE_NAME + " (" +
                    "`id` int(11) unsigned NOT NULL auto_increment," +
                    "`uuid` VARCHAR(36) NOT NULL," +
                    "`nbt` BLOB," +
                    "PRIMARY KEY (`id`)" + ")";

            SQLController.makeTable(table_create);
            CivLog.info("Created " + TABLE_NAME + " table");
        } else {
            CivLog.info(TABLE_NAME + " table OK!");
        }
    }

    @Override
    public void load(ResultSet rs) throws SQLException, InvalidNameException, CivException {
        this.setId(rs.getInt("id"));
        this.setUUID(UUID.fromString(rs.getString("uuid")));
        var data = new ByteArrayInputStream(rs.getBytes("nbt"));
        NBTTagCompound nbt;
        try {
            nbt = NBTCompressedStreamTools.a(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.setName(nbt.getString("name"));
        this.setLevel(nbt.getInt("level"));
        if (!nbt.getString("granaryResources").isEmpty()) {
            this.granaryResources = nbt.getString("granaryResources");
        }
        this.setCiv(Civilization.getCivFromUUID(UUID.fromString(nbt.getString("civ_uuid"))));

        String motherCivUUID = nbt.getString("mother_civ_uuid");
        if (!motherCivUUID.isEmpty()) {
            Civilization mother = CivGlobal.getConqueredCivFromUUID(UUID.fromString(motherCivUUID));
            if (mother == null) {
                mother = Civilization.getCivFromUUID(UUID.fromString(motherCivUUID));
            }

            if (mother == null) {
                CivLog.warning("Unable to find a mother civ with UUID:" + motherCivUUID + "!");
            } else {
                setMotherCiv(mother);
            }
        }
        NBTTagCompound perms = nbt.getCompound("permission_groups");
        for (String groups : perms.c()) {
            addGroup(new PermissionGroup(this, perms.getCompound(groups)));
        }


        if (this.getCiv() == null) {
            CivLog.error("TOWN:" + this.getName() + " WITHOUT A CIV, UUID was:" + nbt.getString("civ_uuid"));
            //this.delete();
            CivGlobal.orphanTowns.add(this);
            throw new CivException("Failed to load town, bad data.");
        }
        this.setDaysInDebt(nbt.getInt("daysInDebt"));
        this.setUpgradesFromString(nbt.getString("upgrades"));

        //this.setHomeChunk(rs.getInt("homechunk_id"));
        this.setExtraHammers(nbt.getDouble("extra_hammers"));
        this.setAccumulatedCulture(nbt.getInt("culture"));

        defaultGroupName = "residents";
        mayorGroupName = "mayors";
        assistantGroupName = "assistants";

        this.setTreasury(CivGlobal.createEconObject(this));
        this.getTreasury().setBalance(nbt.getDouble("coins"), false);
        this.setDebt(nbt.getDouble("debt"));

        String outlawRaw = nbt.getString("outlaws");
        if (outlawRaw != null) {
            String[] outlaws = outlawRaw.split(",");

            this.outlaws.addAll(Arrays.asList(outlaws));
        }

        this.setCreated(new Date(nbt.getLong("created_date")));

        this.getCiv().addTown(this);
    }

    @Override
    public void save() {
        SQLUpdate.add(this);
    }

    @Override
    public void saveNow() throws SQLException {
        HashMap<String, Object> hashmap = new HashMap<>();
        NBTTagCompound nbt = new NBTTagCompound();

        nbt.setString("name", this.getName());
        nbt.setString("civ_uuid", this.getCiv().getUUID().toString());

        if (this.motherCiv != null) {
            nbt.setString("mother_civ_uuid", this.motherCiv.getUUID().toString());
        }

        nbt.setString("defaultGroupName", this.getDefaultGroupName());
        nbt.setString("mayorGroupName", this.getMayorGroupName());
        nbt.setString("assistantGroupName", this.getAssistantGroupName());
        nbt.setInt("level", this.getLevel());
        nbt.setDouble("debt", this.getTreasury().getDebt());
        nbt.setInt("daysInDebt", this.getDaysInDebt());
        nbt.setDouble("extra_hammers", this.getExtraHammers());
        nbt.setInt("culture", this.getAccumulatedCulture());
        nbt.setString("upgrades", this.getUpgradesString());
        nbt.setDouble("coins", this.getTreasury().getBalance());
        nbt.setString("dbg_civ_name", this.getCiv().getName());
        NBTTagCompound perms = new NBTTagCompound();
        for (PermissionGroup groups : this.getGroups()) {
            perms.set(groups.getUUID().toString(), new NBTTagCompound());
            groups.saveToNBT(perms.getCompound(groups.getUUID().toString()));

        }
        nbt.set("permission_groups", perms);

        if (granaryResources != null) {
            nbt.setString("granaryResources", this.granaryResources);
        }

        nbt.setLong("created_date", this.created_date.getTime());

        StringBuilder outlaws = new StringBuilder();
        for (String outlaw : this.outlaws) {
            outlaws.append(outlaw).append(",");
        }
        nbt.setString("outlaws", outlaws.toString());
        var data = new ByteArrayOutputStream();
        try {
            NBTCompressedStreamTools.a(nbt, data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        hashmap.put("nbt", data.toByteArray());

        SQLController.updateNamedObject(this, hashmap, TABLE_NAME);
    }

    @Override
    public void delete() throws SQLException {
        /* Remove all of our residents from town. */
        for (Resident resident : this.residents.values()) {
            resident.setTown(null);
            /* Also forgive their debt, nobody to pay it to. */
            resident.getTreasury().setDebt(0);
            resident.saveNow();
        }

        /* Remove all structures in the town. */
        for (Structure struct : this.structures.values()) {
            struct.deleteSkipUndo();
        }

        /* Remove all town chunks. */
        if (this.getTownChunks() != null) {
            for (TownChunk tc : this.getTownChunks()) {
                tc.delete();
            }
        }

        for (Wonder wonder : wonders.values()) {
            wonder.unbindStructureBlocks();
            try {
                wonder.undoFromTemplate();
            } catch (CivException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                wonder.fancyDestroyStructureBlocks();
            }
            wonder.delete();
        }

        for (CultureChunk cc : this.cultureChunks.values()) {
            CivGlobal.removeCultureChunk(cc);
        }
        this.cultureChunks.clear();

        //TODO remove protected blocks?

        /* Remove any related SessionDB entries */
        CivGlobal.getSessionDB().deleteAllForTown(this);

        SQLController.deleteNamedObject(this, TABLE_NAME);
        removeTown(this);
    }


    public Town(String name, Resident mayor, Civilization civ) throws InvalidNameException {
        this.setName(name);
        this.setLevel(1);
        this.setCiv(civ);

        this.setDaysInDebt(0);
        this.setHammerRate(1.0);
        this.setExtraHammers(0);
        this.setAccumulatedCulture(0);
        this.setTreasury(CivGlobal.createEconObject(this));
        this.getTreasury().setBalance(0, false);
        this.created_date = new Date();

        loadSettings();
    }

    public Town(ResultSet rs) throws SQLException, InvalidNameException, CivException {
        this.load(rs);
        loadSettings();
    }

    public static Town newTown(Resident resident, String name, Civilization civ, boolean free, boolean capitol,
                               Location loc) throws CivException {
        try {

            if (War.isWarTime() && !free && civ.getDiplomacyManager().isAtWar()) {
                throw new CivException(CivSettings.localize.localizedString("town_found_errorAtWar"));
            }

            if (civ == null) {
                throw new CivException(CivSettings.localize.localizedString("town_found_errorNotInCiv"));
            }

            if (resident.getTown() != null && resident.getTown().isMayor(resident)) {
                throw new CivException(CivSettings.localize.localizedString("var_town_found_errorIsMayor", resident.getTown().getName()));
            }

            if (resident.hasCamp()) {
                throw new CivException(CivSettings.localize.localizedString("town_found_errorInCamp"));
            }

            Town existTown = getTown(name);
            if (existTown != null) {
                throw new CivException(CivSettings.localize.localizedString("var_town_found_errorNameExists", name));
            }

            Town newTown;
            try {
                newTown = new Town(name, resident, civ);
            } catch (InvalidNameException e) {
                throw new CivException(CivSettings.localize.localizedString("var_town_found_errorInvalidName", name));
            }

            Player player = CivGlobal.getPlayer(resident.getName());

            if (CivGlobal.getTownChunk(loc) != null) {
                throw new CivException(CivSettings.localize.localizedString("town_found_errorInTownChunk"));
            }

            CultureChunk cultrueChunk = CivGlobal.getCultureChunk(loc);
            if (cultrueChunk != null && cultrueChunk.getCiv() != resident.getCiv()) {
                throw new CivException(CivSettings.localize.localizedString("town_found_errorCivCulture"));
            }

            double minDistanceFriend = CivSettings.townConfig.getDouble("town.min_town_distance", 150.0);
            double minDistanceEnemy = CivSettings.townConfig.getDouble("town.min_town_distance_enemy", 300.0);

            for (Town town : getTowns()) {
                TownHall townhall = town.getTownHall();
                if (townhall == null) {
                    continue;
                }

                double dist = townhall.getCenterLocation().distance(new BlockCoord(player.getLocation()));
                double minDistance = minDistanceFriend;
                if (townhall.getCiv().getDiplomacyManager().atWarWith(civ)) {
                    minDistance = minDistanceEnemy;
                }

                if (dist < minDistance) {
                    DecimalFormat df = new DecimalFormat();
                    throw new CivException(CivSettings.localize.localizedString("var_town_found_errorTooClose", town.getName(), df.format(dist), minDistance));
                }
            }

            //Test that we are not too close to another civ
            int min_distance = CivSettings.civConfig.getInt("civ.min_distance", 15);
            ChunkCoord foundLocation = new ChunkCoord(loc);

            for (TownChunk cc : CivGlobal.getTownChunks()) {
                if (cc.getTown().getCiv() == newTown.getCiv()) {
                    continue;
                }

                double dist = foundLocation.distance(cc.getChunkCoord());
                if (dist <= min_distance) {
                    DecimalFormat df = new DecimalFormat();
                    throw new CivException(CivSettings.localize.localizedString("var_town_found_errorTooClose", cc.getTown().getName(), df.format(dist), min_distance));
                }
            }


            if (!free) {
                ConfigUnit unit = Unit.getPlayerUnit(player);
                if (unit == null || !unit.id.equals("u_settler")) {
                    throw new CivException(CivSettings.localize.localizedString("town_found_errorNotSettler"));
                }
            }
            newTown.saveNow();

            addTown(newTown);

            // Create permission groups for town.
            PermissionGroup residentsGroup;
            try {
                residentsGroup = new PermissionGroup(newTown, "residents");
                residentsGroup.addMember(resident);
                newTown.setDefaultGroup(residentsGroup);


                PermissionGroup mayorGroup = new PermissionGroup(newTown, "mayors");
                mayorGroup.addMember(resident);
                newTown.setMayorGroup(mayorGroup);

                PermissionGroup assistantGroup = new PermissionGroup(newTown, "assistants");
                newTown.setAssistantGroup(assistantGroup);
            } catch (InvalidNameException e2) {
                e2.printStackTrace();
                throw new CivException(CivSettings.localize.localizedString("internalCommandException"));
            }

            ChunkCoord cl = new ChunkCoord(loc);
            TownChunk tc = new TownChunk(newTown, cl);
            tc.perms.addGroup(residentsGroup);
            try {
                newTown.addTownChunk(tc);
            } catch (AlreadyRegisteredException e1) {
                throw new CivException(CivSettings.localize.localizedString("town_found_errorTownHasChunk"));
            }

            tc.save();
            CivGlobal.addTownChunk(tc);
            civ.addTown(newTown);

            try {

                ConfigBuildableInfo buildableInfo = CivSettings.structures.get(capitol ? "s_capitol" : "s_townhall");
                newTown.getTreasury().deposit(buildableInfo.cost);
                newTown.buildStructure(player, buildableInfo, loc, resident.desiredTemplate);
            } catch (CivException e) {
                civ.removeTown(newTown);
                newTown.delete();
                throw e;
            }

            if (!free) {
                ItemStack newStack = new ItemStack(Material.AIR);
                player.getInventory().setItemInMainHand(newStack);
                Unit.removeUnit(player);
            }

            try {
                if (resident.getTown() != null) {
                    CivMessage.sendTown(resident.getTown(), CivSettings.localize.localizedString("var_town_found_leftTown", resident.getName()));
                    resident.getTown().removeResident(resident);
                }
                newTown.addResident(resident);
            } catch (AlreadyRegisteredException e) {
                e.printStackTrace();
                throw new CivException(CivSettings.localize.localizedString("town_found_residentError"));
            }
            resident.saveNow();

            CivGlobal.processCulture();
            newTown.saveNow();
            return newTown;
        } catch (SQLException e2) {
            e2.printStackTrace();
            throw new CivException(CivSettings.localize.localizedString("internalDatabaseException"));
        }
    }

    public void loadSettings() {
        this.baseHammers = CivSettings.townConfig.getDouble("town.base_hammer_rate", 1.0);
        this.setBaseGrowth(CivSettings.townConfig.getDouble("town.base_growth_rate", 1.0));

    }

    private String getUpgradesString() {
        StringBuilder out = new StringBuilder();

        for (ConfigTownUpgrade upgrade : upgrades.values()) {
            out.append(upgrade.id).append(",");
        }

        return out.toString();
    }

    public ConfigTownUpgrade getUpgrade(String id) {
        return upgrades.get(id);
    }

    public boolean isMayor(Resident res) {
        return this.getMayorGroup().hasMember(res);
    }

    public int getResidentCount() {
        return residents.size();
    }

    public Collection<Resident> getResidents() {
        return residents.values();
    }

    public boolean hasResident(String name) {
        return residents.containsKey(name.toLowerCase());
    }

    public boolean hasResident(Resident res) {
        return hasResident(res.getName());
    }

    public void addResident(Resident res) throws AlreadyRegisteredException {
        String key = res.getName().toLowerCase();

        if (residents.containsKey(key)) {
            throw new AlreadyRegisteredException(res.getName() + " already a member of town " + this.getName());
        }

        res.setTown(this);

        residents.put(key, res);
        if (this.defaultGroup != null && !this.defaultGroup.hasMember(res)) {
            this.defaultGroup.addMember(res);
        }
        Player player = Bukkit.getPlayer(res.getUUID());
    }

    public void addTownChunk(TownChunk tc) throws AlreadyRegisteredException {

        if (townChunks.containsKey(tc.getChunkCoord())) {
            throw new AlreadyRegisteredException("TownChunk at " + tc.getChunkCoord() + " already registered to town " + this.getName());
        }
        townChunks.put(tc.getChunkCoord(), tc);
    }

    public Structure findStructureByName(String name) {
        for (Structure struct : structures.values()) {
            if (struct.getDisplayName().equalsIgnoreCase(name)) {
                return struct;
            }
        }
        return null;
    }

    public Structure findStructureByLocation(BlockCoord bc) {
        return structures.get(bc);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {

        this.level = level;
    }

    public Civilization getCiv() {
        return civ;
    }

    public void setCiv(Civilization civ) {
        this.civ = civ;
    }

    public int getAccumulatedCulture() {
        return culture;
    }

    public void setAccumulatedCulture(int culture) {
        this.culture = culture;
    }

    public AttrSource getCultureRate() {
        double rate = 1.0;
        HashMap<String, Double> rates = new HashMap<>();

        double newRate = getGovernment().culture_rate;
        rates.put("Government", newRate - rate);
        rate = newRate;

        ConfigHappinessState state = CivSettings.getHappinessState(this.getHappinessPercentage());
        newRate = rate * state.culture_rate();
        rates.put("Happiness", newRate - rate);
        rate = newRate;

        double structures = 0;
        if (this.getBuffManager().hasBuff("buff_art_appreciation")) {
            structures += this.getBuffManager().getEffectiveDouble("buff_art_appreciation");
        }
        rates.put("Great Works", structures);
        rate += structures;

        double additional = 0;
        if (this.getBuffManager().hasBuff("buff_fine_art")) {
            additional += this.getBuffManager().getEffectiveDouble(Buff.FINE_ART);
        }
        if (this.getBuffManager().hasBuff("buff_pyramid_culture")) {
            additional += this.getBuffManager().getEffectiveDouble("buff_pyramid_culture");
        }
        if (this.getBuffManager().hasBuff("wonder_trade_globe_theatre")) {
            additional += this.getGlobeTradeBuff(Attribute.TypeKeys.COINS);
        }

        rates.put("Wonders/Goodies", additional);
        rate += additional;

        return new AttrSource(rates, rate, null);
    }

    public AttrSource getCulture() {

        AttrCache cache = this.attributeCache.get("CULTURE");
        if (cache == null) {
            cache = new AttrCache();
            cache.lastUpdate = new Date();
        } else {
            Date now = new Date();
            if (now.getTime() > (cache.lastUpdate.getTime() + ATTR_TIMEOUT_SECONDS * 1000)) {
                cache.lastUpdate = now;
            } else {
                return cache.sources;
            }
        }

        double total = 0;
        HashMap<String, Double> sources = new HashMap<>();

        /* Grab any culture from goodies. */
        double goodieCulture = getBuffManager().getEffectiveInt(Buff.EXTRA_CULTURE);
        sources.put("Goodies", goodieCulture);
        total += goodieCulture;

        /* Grab beakers generated from structures with components. */
        double fromStructures = 0;
        for (Structure struct : this.structures.values()) {
            for (Component comp : struct.attachedComponents) {
                if (comp instanceof AttributeBase as) {
                    if (as.getString("attribute").equalsIgnoreCase("CULTURE")) {
                        fromStructures += as.getGenerated();
                    }
                }
            }
            if (struct instanceof Temple temple) {
                fromStructures += temple.getCultureGenerated();
            }
        }

        if (this.getBuffManager().hasBuff("buff_globe_theatre_culture_from_towns")) {
            int townCount = 0;
            for (Civilization civ : Civilization.getCivs()) {
                townCount += civ.getTownCount();
            }
            double culturePerTown = Double.parseDouble(CivSettings.buffs.get("buff_globe_theatre_culture_from_towns").value);

            double bonus = culturePerTown * townCount;

            CivMessage.sendTown(this, ChatColor.GREEN + CivSettings.localize.localizedString("var_town_GlobeTheatreCulture", String.valueOf(ChatColor.YELLOW) + bonus + ChatColor.GREEN, townCount));

            fromStructures += bonus;
        }

        total += fromStructures;
        sources.put("Structures", fromStructures);

        AttrSource rate = this.getCultureRate();
        total *= rate.total;

        if (total < 0) {
            total = 0;
        }

        AttrSource as = new AttrSource(sources, total, rate);
        cache.sources = as;
        this.attributeCache.put("CULTURE", cache);
        return as;
    }


    public void addAccumulatedCulture(double generated) {
        ConfigCultureLevel clc = CivSettings.cultureLevels.get(this.getCultureLevel());

        this.culture += generated;
        if (this.getCultureLevel() != CivSettings.getMaxCultureLevel()) {
            if (this.culture >= clc.amount) {
                CivGlobal.processCulture();
                CivMessage.sendCiv(this.civ, CivSettings.localize.localizedString("var_town_bordersExpanded", this.getName()));
            }
        }
    }


    public double getExtraHammers() {
        return extraHammers;
    }


    public void setExtraHammers(double extraHammers) {
        this.extraHammers = extraHammers;
    }

    private void setUpgradesFromString(String upgradeString) {
        for (String str : upgradeString.split(",")) {
            if (str == null || str.isEmpty()) {
                continue;
            }

            ConfigTownUpgrade upgrade = CivSettings.townUpgrades.get(str);
            if (upgrade == null) {
                CivLog.warning("Unknown town upgrade:" + str + " in town " + this.getName());
                continue;
            }

            this.upgrades.put(str, upgrade);
        }
    }

    public AttrSource getHammers() {
        double total = 0;

        AttrCache cache = this.attributeCache.get("HAMMERS");
        if (cache == null) {
            cache = new AttrCache();
            cache.lastUpdate = new Date();
        } else {
            Date now = new Date();
            if (now.getTime() > (cache.lastUpdate.getTime() + ATTR_TIMEOUT_SECONDS * 1000)) {
                cache.lastUpdate = now;
            } else {
                return cache.sources;
            }
        }

        HashMap<String, Double> sources = new HashMap<>();

        /* Wonders and Goodies. */
        double wonderGoodies = this.getBuffManager().getEffectiveInt(Buff.CONSTRUCTION);

        sources.put("Wonders/Goodies", wonderGoodies);
        total += wonderGoodies;

        double cultureHammers = this.getHammersFromCulture();
        sources.put("Culture Biomes", cultureHammers);
        total += cultureHammers;

        /* Grab hammers generated from structures with components. */
        double structures = 0;
        double mines = 0;
        for (Structure struct : this.structures.values()) {
            if (struct instanceof Mine mine) {
                mines += mine.getBonusHammers();
            }
            for (Component comp : struct.attachedComponents) {
                if (comp instanceof AttributeBase as) {
                    if (as.getString("attribute").equalsIgnoreCase("HAMMERS")) {
                        structures += as.getGenerated();
                    }
                }
            }
        }

        total += mines;
        sources.put("Mines", mines);

        total += structures;
        sources.put("Structures", structures);


        sources.put("Base Hammers", this.baseHammers);
        total += this.baseHammers;

        AttrSource rate = getHammerRate();
        total *= rate.total;

        if (total < this.baseHammers) {
            total = this.baseHammers;
        }

        AttrSource as = new AttrSource(sources, total, rate);
        cache.sources = as;
        this.attributeCache.put("HAMMERS", cache);
        return as;
    }

    public void setHammerRate(double hammerRate) {
        this.baseHammers = hammerRate;
    }

    public AttrSource getHammerRate() {
        double rate = 1.0;
        HashMap<String, Double> rates = new HashMap<>();
        ConfigHappinessState state = CivSettings.getHappinessState(this.getHappinessPercentage());

        /* Happiness */
        double newRate = rate * state.hammer_rate();
        rates.put("Happiness", newRate - rate);
        rate = newRate;

        double wonders = 0.0;
        if (this.getBuffManager().hasBuff("wonder_trade_globe_theatre")) {
            wonders += this.getGlobeTradeBuff(Attribute.TypeKeys.HAMMERS);
        }
        if (wonders != 0.0) {
            rates.put("Wonders", wonders);
        }
        rate += wonders;

        /* Government */
        newRate = rate * getGovernment().hammer_rate;
        rates.put("Government", newRate - rate);
        rate = newRate;

        double randomRate = RandomEvent.getHammerRate(this);
        newRate = rate * randomRate;
        rates.put("Random Events", newRate - rate);
        rate = newRate;

        /* Captured Town Penalty */
        if (this.motherCiv != null) {
            newRate = rate * CivSettings.warConfig.getDouble("war.captured_penalty", 0.50);
            rates.put("Captured Penalty", newRate - rate);
            rate = newRate;

        }
        return new AttrSource(rates, rate, null);
    }

    public PermissionGroup getDefaultGroup() {
        return defaultGroup;
    }

    public void setDefaultGroup(PermissionGroup defaultGroup) {
        this.defaultGroup = defaultGroup;
        this.groups.put(defaultGroup.getName(), defaultGroup);
    }

    public Collection<PermissionGroup> getGroups() {
        return groups.values();
    }

    public PermissionGroup getGroup(String name) {
        return groups.get(name);
    }

    public PermissionGroup getGroupFromId(Integer id) {
        for (PermissionGroup grp : groups.values()) {
            if (grp.getId() == id) {
                return grp;
            }
        }
        return null;
    }

    public void addGroup(PermissionGroup grp) {

        if (grp.getName().equalsIgnoreCase(this.defaultGroupName)) {
            this.defaultGroup = grp;
        } else if (grp.getName().equalsIgnoreCase(this.mayorGroupName)) {
            this.mayorGroup = grp;
        } else if (grp.getName().equalsIgnoreCase(this.assistantGroupName)) {
            this.assistantGroup = grp;
        }

        groups.put(grp.getName(), grp);

    }

    public void removeGroup(PermissionGroup grp) {
        groups.remove(grp.getName());
    }

    public boolean hasGroupNamed(String name) {
        for (PermissionGroup grp : groups.values()) {
            if (grp.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public PermissionGroup getGroupByName(String name) {
        for (PermissionGroup grp : groups.values()) {
            if (grp.getName().equalsIgnoreCase(name)) {
                return grp;
            }
        }
        return null;
    }

    public String getDefaultGroupName() {
        return "residents";
    }

    public PermissionGroup getMayorGroup() {
        return mayorGroup;
    }

    public void setMayorGroup(PermissionGroup mayorGroup) {
        this.mayorGroup = mayorGroup;
        this.groups.put(mayorGroup.getName(), mayorGroup);

    }

    public PermissionGroup getAssistantGroup() {
        return assistantGroup;
    }

    public void setAssistantGroup(PermissionGroup assistantGroup) {
        this.assistantGroup = assistantGroup;
        this.groups.put(assistantGroup.getName(), assistantGroup);

    }

    public String getMayorGroupName() {
        return "mayors";
    }

    public String getAssistantGroupName() {
        return "assistants";
    }

    public boolean isProtectedGroup(PermissionGroup grp) {
        return grp.isProtectedGroup();
    }

    public boolean playerIsInGroupName(String groupName, Player player) {
        PermissionGroup grp = this.getGroupByName(groupName);
        if (grp == null) {
            return false;
        }

        Resident resident = CivGlobal.getResident(player);
        return grp.hasMember(resident);
    }

    public EconObject getTreasury() {
        return treasury;
    }

    public void depositDirect(double amount) {
        this.treasury.deposit(amount);
    }

    public void depositTaxed(double amount) {

        double taxAmount = amount * this.getDepositCiv().getIncomeTaxRate();
        amount -= taxAmount;

        if (this.getMotherCiv() != null) {
            double capturePayment = amount * CivSettings.warConfig.getDouble("war.captured_penalty", 0.50);
            CivMessage.sendTown(this, ChatColor.YELLOW + CivSettings.localize.localizedString("var_town_capturePenalty1", (amount - capturePayment), CivSettings.CURRENCY_NAME, this.getCiv().getName()));
            amount = capturePayment;
        }

        this.treasury.deposit(amount);
        this.getDepositCiv().taxPayment(this, taxAmount);
    }

    public void withdraw(double amount) {
        this.treasury.withdraw(amount);
    }

    public boolean inDebt() {
        return this.treasury.inDebt();
    }

    public double getDebt() {
        return this.treasury.getDebt();
    }

    public void setDebt(double amount) {
        this.treasury.setDebt(amount);
    }

    public double getBalance() {
        return this.treasury.getBalance();
    }

    public boolean hasEnough(double amount) {
        return this.treasury.hasEnough(amount);
    }

    public void setTreasury(EconObject treasury) {
        this.treasury = treasury;
    }

    public String getLevelTitle() {
        ConfigTownLevel clevel = CivSettings.townLevels.get(this.level);
        if (clevel == null) {
            return "Unknown";
        } else {
            return clevel.title;
        }
    }

    public void purchaseUpgrade(ConfigTownUpgrade upgrade) throws CivException {
        if (!this.hasUpgrade(upgrade.require_upgrade)) {
            throw new CivException(CivSettings.localize.localizedString("town_missingUpgrades"));
        }

        if (!this.getTreasury().hasEnough(upgrade.cost)) {
            throw new CivException(CivSettings.localize.localizedString("var_town_missingFunds", upgrade.cost, CivSettings.CURRENCY_NAME));
        }

        if (!this.hasStructure(upgrade.require_structure)) {
            throw new CivException(CivSettings.localize.localizedString("town_missingStructures"));
        }

        this.getTreasury().withdraw(upgrade.cost);

        try {
            upgrade.processAction(this);
        } catch (CivException e) {
            //Something went wrong purchasing the upgrade, refund and throw again.
            this.getTreasury().deposit(upgrade.cost);
            throw e;
        }

        this.upgrades.put(upgrade.id, upgrade);
    }

    public Structure findStructureByConfigId(String require_structure) {

        for (Structure struct : this.structures.values()) {
            if (struct.getConfigId().equals(require_structure)) {
                return struct;
            }
        }

        return null;
    }

    public ConcurrentHashMap<String, ConfigTownUpgrade> getUpgrades() {
        return upgrades;
    }

    public boolean isPvp() {
        return pvp;
    }

    public void setPvp(boolean pvp) {
        this.pvp = pvp;
    }

    public String getPvpString() {
        if (!this.getCiv().getDiplomacyManager().isAtWar()) {
            if (pvp) {
                return ChatColor.GOLD + "[PvP]";
            } else {
                return ChatColor.DARK_AQUA + "[No PvP]";
            }
        } else {
            return ChatColor.DARK_RED + "[WAR-PvP]";
        }
    }

    private void kickResident(Resident resident) {
        /* Repo all this resident's plots. */
        for (TownChunk tc : townChunks.values()) {
            if (tc.perms.getOwner() == resident) {
                tc.perms.setOwner(null);
                tc.perms.replaceGroups(defaultGroup);
                tc.perms.resetPerms();
                tc.save();
            }
        }

        /* Clear resident's debt and remove from town. */
        resident.getTreasury().setDebt(0);
        resident.setDaysTilEvict(0);
        resident.setTown(null);
        resident.setRejoinCooldown(this);

        this.residents.remove(resident.getName().toLowerCase());

        resident.save();
        Player player = Bukkit.getPlayer(resident.getUUID());
    }

    public Collection<TownChunk> getTownChunks() {
        return this.townChunks.values();
    }

    public boolean isInGroup(String name, Resident resident) {
        PermissionGroup grp = this.getGroupByName(name);
        if (grp != null) {
            return grp.hasMember(resident);
        }
        return false;
    }

    public TownHall getTownHall() {
        for (Structure struct : this.structures.values()) {
            if (struct instanceof TownHall) {
                return (TownHall) struct;
            }
        }
        return null;
    }

    public double payUpkeep() {
        double upkeep = 0;
        if (this.getCiv().isAdminCiv()) {
            return 0;
        }
        upkeep += this.getBaseUpkeep();
        //upkeep += this.getSpreadUpkeep();
        upkeep += this.getStructureUpkeep();

        upkeep *= getGovernment().upkeep_rate;

        if (this.getBuffManager().hasBuff("buff_colossus_reduce_upkeep")) {
            upkeep = upkeep - (upkeep * this.getBuffManager().getEffectiveDouble("buff_colossus_reduce_upkeep"));
        }

        if (this.getBuffManager().hasBuff("debuff_colossus_leech_upkeep")) {
            double rate = this.getBuffManager().getEffectiveDouble("debuff_colossus_leech_upkeep");
            double amount = upkeep * rate;

            Wonder colossus = CivGlobal.getWonderByConfigId("w_colossus");
            if (colossus != null) {
                colossus.getTown().getTreasury().deposit(amount);
            } else {
                CivLog.warning("Unable to find Colossus wonder but debuff for leech upkeep was present!");
                //Colossus is "null", doesn't exist, we remove the buff in case of duplication
                this.getBuffManager().removeBuff("debuff_colossus_leech_upkeep");
            }
        }

        if (this.getTreasury().hasEnough(upkeep)) {
            this.getTreasury().withdraw(upkeep);
        } else {

            /* Couldn't pay the bills. Add to this town's debt,
             * civ may pay it later. */
            double diff = upkeep - this.getTreasury().getBalance();

            if (this.isCapitol()) {
                /* Capitol towns cannot be in debt, must pass debt on to civ. */
                if (this.getCiv().getTreasury().hasEnough(diff)) {
                    this.getCiv().getTreasury().withdraw(diff);
                } else {
                    diff -= this.getCiv().getTreasury().getBalance();
                    this.getCiv().getTreasury().setBalance(0);
                    this.getCiv().getTreasury().setDebt(this.getCiv().getTreasury().getDebt() + diff);
                }
            } else {
                this.getTreasury().setDebt(this.getTreasury().getDebt() + diff);
            }
            this.getTreasury().withdraw(this.getTreasury().getBalance());

        }

        return upkeep;
    }

    public double getBaseUpkeep() {
        ConfigTownLevel level = CivSettings.townLevels.get(this.level);
        return level.upkeep;
    }

    public double getStructureUpkeep() {
        double upkeep = 0;

        for (Structure struct : getStructures()) {
            upkeep += struct.getUpkeepCost();
        }
        return upkeep;
    }

    public void removeResident(Resident resident) {
        this.residents.remove(resident.getName().toLowerCase());

        /* Remove resident from any groups. */
        for (PermissionGroup group : groups.values()) {
            if (group.hasMember(resident)) {
                group.removeMember(resident);
            }
        }

        kickResident(resident);
    }

    public boolean canClaim() {

        return getMaxPlots() > townChunks.size();
    }

    public int getMaxPlots() {
        ConfigTownLevel lvl = CivSettings.townLevels.get(this.level);
        return lvl.plots;
    }

    public boolean hasUpgrade(String require_upgrade) {
        if (require_upgrade == null || require_upgrade.isEmpty())
            return true;

        return upgrades.containsKey(require_upgrade);
    }

    public boolean hasTechnology(String require_tech) {
        return this.getCiv().hasTechnology(require_tech);
    }

    public void removeCultureChunk(ChunkCoord coord) {
        this.cultureChunks.remove(coord);
    }

    public void removeCultureChunk(CultureChunk cc) {
        this.cultureChunks.remove(cc.getChunkCoord());
    }

    public void addCultureChunk(CultureChunk cc) {
        this.cultureChunks.put(cc.getChunkCoord(), cc);
    }

    public int getCultureLevel() {

        /* Get the first level */
        int bestLevel = 0;
        ConfigCultureLevel level = CivSettings.cultureLevels.get(0);

        while (this.culture >= level.amount) {
            level = CivSettings.cultureLevels.get(bestLevel + 1);
            if (level == null) {
                level = CivSettings.cultureLevels.get(bestLevel);
                break;
            }
            bestLevel++;
        }

        return level.level;
    }

    public Collection<CultureChunk> getCultureChunks() {
        return this.cultureChunks.values();
    }

    public Object getCultureChunk(ChunkCoord coord) {
        return this.cultureChunks.get(coord);
    }

    public void removeWonder(Buildable buildable) {
        if (!buildable.isComplete()) {
            this.removeBuildTask(buildable);
        }

        if (currentWonderInProgress == buildable) {
            currentWonderInProgress = null;
        }

        this.wonders.remove(buildable.getCorner());
    }

    public void addWonder(Buildable buildable) {
        if (buildable instanceof Wonder) {
            this.wonders.put(buildable.getCorner(), (Wonder) buildable);
        }
    }

    public int getStructureTypeCount(String id) {
        int count = 0;
        for (Structure struct : this.structures.values()) {
            if (struct.getConfigId().equalsIgnoreCase(id)) {
                count++;
            }
        }
        for (Wonder wonder : this.wonders.values()) {
            if (wonder.getConfigId().equalsIgnoreCase(id)) {
                count++;
            }
        }
        return count;
    }

    public void giveExtraHammers(double extra) {
        if (build_tasks.isEmpty()) {
            //Nothing is building, store the extra hammers for when a structure starts building.
            extraHammers = extra;
        } else {
            //Currently building structures ... divide them evenly between
            double hammers_per_task = extra / build_tasks.size();
            double leftovers = 0.0;

            for (BuildAsyncTask task : build_tasks) {
                leftovers += task.setExtraHammers(hammers_per_task);
            }

            extraHammers = leftovers;
        }
    }

    public void buildWonder(Player player, ConfigBuildableInfo info, Location center, Template tpl) throws CivException {

        if (!center.getWorld().getName().equals("world")) {
            throw new CivException(CivSettings.localize.localizedString("town_buildwonder_NotOverworld"));
        }

        Wonder wonder = (Wonder) MetaStructure.newStructOrWonder(center, info, this);

        if (!this.hasUpgrade(wonder.getRequiredUpgrade())) {
            throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorMissingUpgrade"));
        }

        if (!this.hasTechnology(wonder.getRequiredTechnology())) {
            throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorMissingTech"));
        }

        if (!wonder.isAvailable()) {
            throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorNotAvailable"));
        }

        wonder.canBuildHere(center, Structure.MIN_DISTANCE);

        if (!Wonder.isWonderAvailable(info.id)) {
            throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorBuiltElsewhere"));
        }

        if (CivGlobal.isCasualMode()) {
            /* Check for a wonder already in this civ. */
            for (Town town : this.getCiv().getTowns()) {
                for (Wonder w : town.getWonders()) {
                    if (w.getConfigId().equals(info.id)) {
                        throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorLimit1Casual"));
                    }
                }
            }
        }

        double cost = wonder.getCost();
        if (!this.getTreasury().hasEnough(cost)) {
            throw new CivException(CivSettings.localize.localizedString("var_town_buildwonder_errorTooPoor", wonder.getDisplayName(), cost, CivSettings.CURRENCY_NAME));
        }

        wonder.runCheck(center); //Throws exception if we can't build here.

        Buildable inProgress = getCurrentStructureInProgress();
        if (inProgress != null) {
            throw new CivException(CivSettings.localize.localizedString("var_town_buildwonder_errorCurrentlyBuilding", inProgress.getDisplayName()) + " " + CivSettings.localize.localizedString("town_buildwonder_errorOneAtATime"));
        } else {
            inProgress = getCurrentWonderInProgress();
            if (inProgress != null) {
                throw new CivException(CivSettings.localize.localizedString("var_town_buildwonder_errorCurrentlyBuilding", inProgress.getDisplayName()) + " " + CivSettings.localize.localizedString("town_buildwonder_errorOneWonderAtaTime"));
            }
        }

        try {
            wonder.build(player, center, tpl);
            if (this.getExtraHammers() > 0) {
                this.giveExtraHammers(this.getExtraHammers());
            }
        } catch (Exception e) {
            if (CivGlobal.testFileFlag("debug")) {
                e.printStackTrace();
            }
            throw new CivException(CivSettings.localize.localizedString("var_town_buildwonder_errorGeneric", e.getMessage()));
        }

        wonders.put(wonder.getCorner(), wonder);

        this.getTreasury().withdraw(cost);
        CivMessage.sendTown(this, ChatColor.YELLOW + CivSettings.localize.localizedString("var_town_buildwonder_success", wonder.getDisplayName()));
    }

    public void buildStructure(Player player, ConfigBuildableInfo info, Location center, Template tpl) throws CivException {


        Structure struct = (Structure) MetaStructure.newStructOrWonder(center, info, this);

        if (!this.hasUpgrade(struct.getRequiredUpgrade())) {
            throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorMissingUpgrade"));
        }

        if (!this.hasTechnology(struct.getRequiredTechnology())) {
            throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorMissingTech"));
        }

        if (!struct.isAvailable()) {
            throw new CivException(CivSettings.localize.localizedString("town_structure_errorNotAvaliable"));
        }

        struct.canBuildHere(center, Structure.MIN_DISTANCE);

        if (struct.getLimit() != 0) {
            if (getStructureTypeCount(info.id) >= struct.getLimit()) {
                throw new CivException(CivSettings.localize.localizedString("var_town_structure_errorLimitMet", struct.getLimit(), struct.getDisplayName()));
            }
        }

        double cost = struct.getCost();
        if (!this.getTreasury().hasEnough(cost)) {
            throw new CivException(CivSettings.localize.localizedString("var_town_buildwonder_errorTooPoor", struct.getDisplayName(), cost, CivSettings.CURRENCY_NAME));
        }

        struct.runCheck(center); //Throws exception if we can't build here.

        Buildable inProgress = getCurrentStructureInProgress();
        if (inProgress != null) {
            throw new CivException(CivSettings.localize.localizedString("var_town_buildwonder_errorCurrentlyBuilding", inProgress.getDisplayName()) + ". " + CivSettings.localize.localizedString("town_buildwonder_errorOneAtATime"));
        }

        try {
            /*
             * XXX if the template is null we need to just get the template first.
             * This should only happen for capitols and town halls since we need to
             * Make them use the structure preview code and they don't yet
             */
            if (tpl == null) {
                tpl = new Template();
                tpl.initTemplate(struct);
            }

            struct.build(player, center, tpl);

            // Go through and add any town chunks that were claimed to this list
            // of saved objects.
            for (TownChunk tc : struct.townChunksToSave) {
                tc.save();
            }
            struct.townChunksToSave.clear();

            if (this.getExtraHammers() > 0) {
                this.giveExtraHammers(this.getExtraHammers());
            }
        } catch (CivException e) {
            throw new CivException(CivSettings.localize.localizedString("var_town_buildwonder_errorGeneric", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            throw new CivException(CivSettings.localize.localizedString("internalCommandException"));
        }

        this.getTreasury().withdraw(cost);
        CivMessage.sendTown(this, ChatColor.YELLOW + CivSettings.localize.localizedString("var_town_buildwonder_success", struct.getDisplayName()));


        /* Good needs to be saved after structure to get proper structure id.*/
        if (struct instanceof TradeOutpost outpost) {
            if (outpost.getGood() != null) {
                outpost.getGood().save();
            }
        }

        //TODO fix this dependency nightmare! (the center is moved in build and needs to be resaved)
        //	} catch (SQLException e) {
        //		e.printStackTrace();
        //		throw new CivException("Internal database error");
        //	}
    }

    public boolean isStructureAddable(Structure struct) {
        int count = this.getStructureTypeCount(struct.getConfigId());

        if (struct.isTileImprovement()) {
            ConfigTownLevel level = CivSettings.townLevels.get(this.getLevel());
            int maxTileImprovements = level.tile_improvements;
            if (this.getBuffManager().hasBuff("buff_mother_tree_tile_improvement_bonus")) {
                maxTileImprovements *= 2;
            }
            return this.getTileImprovementCount() <= maxTileImprovements;
        } else return (struct.getLimit() == 0) || (count <= struct.getLimit());
    }

    public void addStructure(Structure struct) {
        this.structures.put(struct.getCorner(), struct);

        if (!isStructureAddable(struct)) {
            this.disabledBuildables.put(struct.getCorner(), struct);
            struct.setEnabled(false);
        } else {
            this.disabledBuildables.remove(struct.getCorner());
            struct.setEnabled(true);
        }

    }

    public Structure getStructureByType(String id) {
        for (Structure struct : this.structures.values()) {
            if (struct.getConfigId().equalsIgnoreCase(id)) {
                return struct;
            }
        }
        return null;
    }

    public void loadUpgrades() {

        for (ConfigTownUpgrade upgrade : this.upgrades.values()) {
            try {
                upgrade.processAction(this);
            } catch (CivException e) {
                //Ignore any exceptions here?
                CivLog.warning("Loading upgrade generated exception:" + e.getMessage());
            }
        }

    }

    public Collection<Structure> getStructures() {
        return this.structures.values();
    }

    public void processUndo() throws CivException {
        if (this.lastBuildableBuilt == null) {
            throw new CivException(CivSettings.localize.localizedString("town_undo_cannotFind"));
        }

//        if (!(this.lastBuildableBuilt instanceof Wall) &&
//                !(this.lastBuildableBuilt instanceof Road)) {
        throw new CivException(CivSettings.localize.localizedString("town_undo_notRoadOrWall"));
//        }
    }

    private void removeBuildTask(Buildable lastBuildableBuilt) {
        for (BuildAsyncTask task : this.build_tasks) {
            if (task.buildable == lastBuildableBuilt) {
                this.build_tasks.remove(task);
                task.abort();
                return;
            }
        }
    }

    public Structure getStructure(BlockCoord coord) {
        return structures.get(coord);
    }

    public void demolish(Structure struct, boolean isAdmin) throws CivException {

        if (!struct.allowDemolish() && !isAdmin) {
            throw new CivException(CivSettings.localize.localizedString("town_demolish_Cannot"));
        }

        try {
            struct.onDemolish();
            this.removeStructure(struct);
            struct.deleteSkipUndo();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CivException(CivSettings.localize.localizedString("internalDatabaseException"));
        }
    }

    public boolean hasStructure(String require_structure) {
        if (require_structure == null || require_structure.isEmpty()) {
            return true;
        }

        Structure struct = this.findStructureByConfigId(require_structure);
        return struct != null && struct.isActive();
    }

    public AttrSource getGrowthRate() {
        double rate = 1.0;
        HashMap<String, Double> rates = new HashMap<>();

        double newRate = rate * getGovernment().growth_rate;
        rates.put("Government", newRate - rate);
        rate = newRate;

        if (this.getCiv().hasTechnology("tech_fertilizer")) {
            double techRate = 0.3;
            rates.put("Technology", techRate);
            rate += techRate;
        }

        /* Wonders and Goodies. */
        double additional = this.getBuffManager().getEffectiveDouble(Buff.GROWTH_RATE);
        additional += this.getBuffManager().getEffectiveDouble("buff_hanging_gardens_growth");

        additional += this.getBuffManager().getEffectiveDouble("buff_mother_tree_growth");
        if (this.getBuffManager().hasBuff("wonder_trade_globe_theatre")) {
            additional += this.getGlobeTradeBuff(Attribute.TypeKeys.GROWTH);
        }

        double additionalGrapes = this.getBuffManager().getEffectiveDouble("buff_hanging_gardens_additional_growth");
        int grapeCount = 0;
        for (BonusGoodie goodie : this.getBonusGoodies()) {
            if (goodie.getConfigTradeGood().id.contains("grapes")) {
                grapeCount++;
            }
        }

        additional += (additionalGrapes * grapeCount);
        rates.put("Wonders/Goodies", additional);
        rate += additional;

        return new AttrSource(rates, rate, null);
    }

    public AttrSource getGrowth() {
        AttrCache cache = this.attributeCache.get("GROWTH");
        if (cache == null) {
            cache = new AttrCache();
            cache.lastUpdate = new Date();
        } else {
            Date now = new Date();
            if (now.getTime() > (cache.lastUpdate.getTime() + ATTR_TIMEOUT_SECONDS * 1000)) {
                cache.lastUpdate = now;
            } else {
                return cache.sources;
            }
        }

        double total = 0;
        HashMap<String, Double> sources = new HashMap<>();

        /* Grab any growth from culture. */
        double cultureSource = 0;
        for (CultureChunk cc : this.cultureChunks.values()) {
            try {
                cultureSource += cc.getGrowth();
            } catch (NullPointerException e) {
                e.printStackTrace();
                CivLog.debug(this.getName() + " - Culture Chunks: " + cc);
            }

        }

        sources.put("Culture Biomes", cultureSource);
        total += cultureSource;

        /* Grab any growth from structures. */
        double structures = 0;
        for (Structure struct : this.structures.values()) {
            for (Component comp : struct.attachedComponents) {
                if (comp instanceof AttributeBase as) {
                    if (as.getString("attribute").equalsIgnoreCase("GROWTH")) {
                        double h = as.getGenerated();
                        structures += h;
                    }
                }
            }
        }


        total += structures;
        sources.put("Structures", structures);

        sources.put("Base Growth", baseGrowth);
        total += baseGrowth;

        AttrSource rate = this.getGrowthRate();
        total *= rate.total;

        if (total < 0) {
            total = 0;
        }

        AttrSource as = new AttrSource(sources, total, rate);
        cache.sources = as;
        this.attributeCache.put("GROWTH", cache);
        return as;
    }

    public double getCottageRate() {
        double rate = getGovernment().cottage_rate;

        double additional = rate * this.getBuffManager().getEffectiveDouble(Buff.COTTAGE_RATE);
        rate += additional;

        /* Adjust for happiness state. */
        rate *= this.getHappinessState().coin_rate();
        return rate;
    }

    public double getTempleRate() {
        return 1.0;
    }

    public double getSpreadUpkeep() {
        double grace_distance = CivSettings.townConfig.getDouble("town.upkeep_town_block_grace_distance", 8.0);
        double base = CivSettings.townConfig.getDouble("town.upkeep_town_block_base", 10.0);
        double falloff = CivSettings.townConfig.getDouble("town.upkeep_town_block_falloff", 1.5);

        Structure townHall = this.getTownHall();
        if (townHall == null) {
            CivLog.error("No town hall for " + getName() + " while getting spread upkeep.");
            return 0.0;
        }

        ChunkCoord townHallChunk = new ChunkCoord(townHall.getCorner().getLocation());

        double total = 0.0;
        for (TownChunk tc : this.getTownChunks()) {
            if (tc.getChunkCoord().equals(townHallChunk))
                continue;

            double distance = tc.getChunkCoord().distance(townHallChunk);
            if (distance > grace_distance) {
                distance -= grace_distance;
                double upkeep = base * Math.pow(distance, falloff);

                total += upkeep;
            }

        }

        return Math.floor(total);
    }

    public double getTotalUpkeep() {
        return this.getBaseUpkeep() + this.getStructureUpkeep() + this.getSpreadUpkeep();
    }

    public double getTradeRate() {
        double rate = getGovernment().trade_rate;

        /* Grab changes from any rate components. */
        double fromStructures = 0.0;
        for (Structure struct : this.structures.values()) {
            for (Component comp : struct.attachedComponents) {
                if (comp instanceof AttributeRate as) {
                    if (as.getString("attribute").equalsIgnoreCase("TRADE")) {
                        fromStructures += as.getGenerated();
                    }
                }
            }
        }
        /* XXX TODO convert this into a 'source' rate so it can be displayed properly. */
        rate += fromStructures;

        double additional = rate * this.getBuffManager().getEffectiveDouble(Buff.TRADE);
        rate += additional;

        /* Adjust for happiness state. */
        rate *= this.getHappinessState().coin_rate();
        return rate;
    }

    public int getTileImprovementCount() {
        int count = 0;
        for (Structure struct : getStructures()) {
            if (struct.isTileImprovement()) {
                count++;
            }
        }
        return count;
    }

    public void removeTownChunk(TownChunk tc) {
        this.townChunks.remove(tc.getChunkCoord());
    }

    public Double getHammersFromCulture() {
        double hammers = 0;
        for (CultureChunk cc : this.cultureChunks.values()) {
            hammers += cc.getHammers();
        }
        return hammers;
    }

    public void setBonusGoodies(ConcurrentHashMap<String, BonusGoodie> bonusGoodies) {
        this.bonusGoodies = bonusGoodies;
    }

    public Collection<BonusGoodie> getBonusGoodies() {
        return this.bonusGoodies.values();
    }

    public void removeUpgrade(ConfigTownUpgrade upgrade) {
        this.upgrades.remove(upgrade.id);
    }

    public Structure getNearestStrucutre(Location location) {
        Structure nearest = null;
        double lowest_distance = Double.MAX_VALUE;

        for (Structure struct : getStructures()) {
            double distance = struct.getCenterLocation().getLocation().distance(location);
            if (distance < lowest_distance) {
                lowest_distance = distance;
                nearest = struct;
            }
        }

        return nearest;
    }

    public Buildable getNearestStrucutreOrWonderInprogress(Location location) {
        Buildable nearest = null;
        double lowest_distance = Double.MAX_VALUE;

        for (Structure struct : getStructures()) {
            double distance = struct.getCenterLocation().getLocation().distance(location);
            if (distance < lowest_distance) {
                lowest_distance = distance;
                nearest = struct;
            }
        }

        for (Wonder wonder : getWonders()) {
            if (wonder.isComplete()) {
                continue;
            }

            double distance = wonder.getCenterLocation().getLocation().distance(location);
            if (distance < lowest_distance) {
                lowest_distance = distance;
                nearest = wonder;
            }
        }

        return nearest;
    }

    public void removeStructure(Structure structure) {
        if (!structure.isComplete()) {
            this.removeBuildTask(structure);
        }

        if (currentStructureInProgress == structure) {
            currentStructureInProgress = null;
        }

        this.structures.remove(structure.getCorner());
        this.invalidStructures.remove(structure);
        this.disabledBuildables.remove(structure.getCorner());
    }

    /**
     * @return the buffManager
     */
    public BuffManager getBuffManager() {
        return buffManager;
    }

    /**
     * @param buffManager the buffManager to set
     */
    public void setBuffManager(BuffManager buffManager) {
        this.buffManager = buffManager;
    }

    public void repairStructure(Structure struct) throws CivException {
        struct.repairStructure();
    }

    public void onDefeat(Civilization attackingCiv) {
        /*
         * We've been defeated. If we don't have our mother civilization set, this means
         * this is the first time this town has been conquered.
         */
        if (this.getMotherCiv() == null) {
            /* Save our motherland in case we ever get liberated. */
            this.setMotherCiv(this.civ);
        } else {
            /* If we've been liberated by our motherland, set things right. */
            if (this.getMotherCiv() == attackingCiv) {
                this.setMotherCiv(null);
            }
        }

        this.changeCiv(attackingCiv);
    }

    public Civilization getDepositCiv() {
        //Get the civilization we are going to deposit taxes to.
        return this.getCiv();
    }

    public Collection<Wonder> getWonders() {
        return this.wonders.values();
    }

    public void onGoodiePlaceIntoFrame(ItemFrameStorage framestore, BonusGoodie goodie) {
        TownHall townhall = this.getTownHall();

        if (townhall == null) {
            return;
        }

        for (ItemFrameStorage fs : townhall.getGoodieFrames()) {
            if (fs == framestore) {
                this.bonusGoodies.put(goodie.getOutpost().getCorner().toString(), goodie);
                for (ConfigBuff cBuff : goodie.getConfigTradeGood().buffs.values()) {
                    String key = "tradegood:" + goodie.getOutpost().getCorner() + ":" + cBuff.id;

                    if (buffManager.hasBuffKey(key)) {
                        continue;
                    }

                    try {
                        buffManager.addBuff(key, cBuff.id, goodie.getDisplayName());
                    } catch (CivException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        for (Structure struct : this.structures.values()) {
            struct.onGoodieToFrame();
        }

        for (Wonder wonder : this.wonders.values()) {
            wonder.onGoodieToFrame();
        }

    }

    public void loadGoodiePlaceIntoFrame(TownHall townhall, BonusGoodie goodie) {
        this.bonusGoodies.put(goodie.getOutpost().getCorner().toString(), goodie);
        for (ConfigBuff cBuff : goodie.getConfigTradeGood().buffs.values()) {
            String key = "tradegood:" + goodie.getOutpost().getCorner() + ":" + cBuff.id;

            if (buffManager.hasBuffKey(key)) {
                continue;
            }

            try {
                buffManager.addBuff(key, cBuff.id, goodie.getDisplayName());
            } catch (CivException e) {
                e.printStackTrace();
            }
        }
    }

    public void removeGoodie(BonusGoodie goodie) {
        this.bonusGoodies.remove(goodie.getOutpost().getCorner().toString());
        for (ConfigBuff cBuff : goodie.getConfigTradeGood().buffs.values()) {
            String key = "tradegood:" + goodie.getOutpost().getCorner() + ":" + cBuff.id;
            buffManager.removeBuff(key);
        }
        if (goodie.getFrame() != null) {
            goodie.getFrame().clearItem();
        }
    }

    public void onGoodieRemoveFromFrame(ItemFrameStorage framestore, BonusGoodie goodie) {
        TownHall townhall = this.getTownHall();

        if (townhall == null) {
            return;
        }

        for (ItemFrameStorage fs : townhall.getGoodieFrames()) {
            if (fs == framestore) {
                removeGoodie(goodie);
            }
        }

        for (Structure struct : this.structures.values()) {
            struct.onGoodieFromFrame();
        }

        for (Wonder wonder : this.wonders.values()) {
            wonder.onGoodieToFrame();
        }
    }

    public int getUnitTypeCount(String id) {
        //TODO find unit limits.
        return 0;
    }

    public ArrayList<ConfigUnit> getAvailableUnits() {
        ArrayList<ConfigUnit> unitList = new ArrayList<>();

        for (ConfigUnit unit : CivSettings.units.values()) {
            if (unit.isAvailable(this)) {
                unitList.add(unit);
            }
        }
        return unitList;
    }

    public void onTechUpdate() {
        try {
            for (Structure struct : this.structures.values()) {
                if (struct.isActive()) {
                    struct.onTechUpdate();
                }
            }

            for (Wonder wonder : this.wonders.values()) {
                if (wonder.isActive()) {
                    wonder.onTechUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            //continue in case some structure/wonder had an error.
        }
    }

    public Buildable getNearestBuildable(Location location) {
        Buildable nearest = null;
        double distance = Double.MAX_VALUE;

        for (Structure struct : this.structures.values()) {
            double tmp = location.distance(struct.getCenterLocation().getLocation());
            if (tmp < distance) {
                nearest = struct;
                distance = tmp;
            }
        }

        for (Wonder wonder : this.wonders.values()) {
            double tmp = location.distance(wonder.getCenterLocation().getLocation());
            if (tmp < distance) {
                nearest = wonder;
                distance = tmp;
            }
        }

        return nearest;
    }

    public boolean isCapitol() {
        return this.getCiv().getCapitolName().equals(this.getName());
    }

    public boolean isForSale() {
        if (this.getCiv().isTownsForSale()) {
            return true;
        }

        if (!this.inDebt()) {
            return false;
        }

        return daysInDebt >= CivSettings.TOWN_DEBT_GRACE_DAYS;
    }

    public double getForSalePrice() {
        return CivSettings.scoreConfig.coins_per_point * this.getScore();
    }

    public int getScore() {
        int points = 0;

        // Count Structures
        for (Structure struct : this.getStructures()) {
            points += struct.getPoints();
        }

        // Count Wonders
        for (Wonder wonder : this.getWonders()) {
            points += wonder.getPoints();
        }

        // Count residents, town chunks, and culture chunks.
        // also coins.

        points += (int) ((double) CivSettings.scoreConfig.town_scores.resident * this.getResidents().size());
        points += (int) (CivSettings.scoreConfig.town_scores.town_chunk * this.getTownChunks().size());
        points += (int) (CivSettings.scoreConfig.town_scores.culture_chunk * this.cultureChunks.size());
        points += (int) (this.getTreasury().getBalance() * CivSettings.scoreConfig.town_scores.coins);

        return points;
    }

    public boolean isOutlaw(Resident res) {
        return this.outlaws.contains(res.getUUIDString());
    }

    public boolean isOutlaw(String name) {
        Resident res = CivGlobal.getResident(name);
        return this.outlaws.contains(res.getUUIDString());
    }

    public void addOutlaw(String name) {
        Resident res = CivGlobal.getResident(name);
        this.outlaws.add(res.getUUIDString());
        TaskMaster.syncTask(new SyncUpdateTags(res.getUUIDString(), this.residents.values()));
    }

    public void removeOutlaw(String name) {
        Resident res = CivGlobal.getResident(name);
        this.outlaws.remove(res.getUUIDString());
        TaskMaster.syncTask(new SyncUpdateTags(res.getUUIDString(), this.residents.values()));
    }

    public void changeCiv(Civilization newCiv) {

        /* Remove this town from its old civ. */
        Civilization oldCiv = this.civ;
        oldCiv.removeTown(this);

        /* Add this town to the new civ. */
        newCiv.addTown(this);

        /* Remove any outlaws which are in our new civ. */
        LinkedList<String> removeUs = new LinkedList<>();
        for (String outlaw : this.outlaws) {
            if (outlaw.length() >= 2) {
                Resident resident = CivGlobal.getResidentViaUUID(UUID.fromString(outlaw));
                if (newCiv.hasResident(resident)) {
                    removeUs.add(outlaw);
                }
            }
        }

        for (String outlaw : removeUs) {
            this.outlaws.remove(outlaw);
        }

        this.setCiv(newCiv);
        CivGlobal.processCulture();
    }

    public void validateResidentSelect(Resident resident) throws CivException {
        if (this.getMayorGroup() == null || this.getAssistantGroup() == null || this.getDefaultGroup() == null ||
                this.getCiv().getLeaderGroup() == null) {
            throw new CivException(CivSettings.localize.localizedString("town_validateSelect_error1"));
        }

        if (!this.getMayorGroup().hasMember(resident) && !this.getAssistantGroup().hasMember(resident) && !this.getDefaultGroup().hasMember(resident)
                && !this.getCiv().getLeaderGroup().hasMember(resident) && !this.getCiv().getAdviserGroup().hasMember(resident)) {
            throw new CivException(CivSettings.localize.localizedString("town_validateSelect_error2"));
        }
    }

    public void disband() {
        getCiv().removeTown(this);
        try {
            delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean touchesCapitolCulture(HashSet<Town> closedSet) {
        if (this.isCapitol()) {
            return true;
        }

        closedSet.add(this);

        for (Town t : this.townTouchList) {
            if (closedSet.contains(t)) {
                continue;
            }

            if (t.getCiv() != this.getCiv()) {
                continue;
            }

            if (t.touchesCapitolCulture(closedSet)) {
                return true;
            }
        }

        return false;
    }

    public void incrementDaysInDebt() {
        daysInDebt++;

        if (daysInDebt >= CivSettings.TOWN_DEBT_GRACE_DAYS) {
            if (daysInDebt >= CivSettings.TOWN_DEBT_SELL_DAYS) {
                this.disband();
                CivMessage.global(CivSettings.localize.localizedString("var_town_ruin1", this.getName()));
                return;
            }
        }

        CivMessage.global(CivSettings.localize.localizedString("var_town_inDebt", this.getName()) + getDaysLeftWarning());
    }

    public String getDaysLeftWarning() {

        if (daysInDebt < CivSettings.TOWN_DEBT_GRACE_DAYS) {
            return " " + CivSettings.localize.localizedString("var_town_inDebt_daysTilSale", (CivSettings.TOWN_DEBT_GRACE_DAYS - daysInDebt));
        }

        if (daysInDebt < CivSettings.TOWN_DEBT_SELL_DAYS) {
            return " " + CivSettings.localize.localizedString("var_town_inDebt_daysTilDelete", this.getName(), CivSettings.TOWN_DEBT_SELL_DAYS - daysInDebt);
        }

        return "";
    }

    public int getDaysInDebt() {
        return daysInDebt;
    }

    public void setDaysInDebt(int daysInDebt) {
        this.daysInDebt = daysInDebt;
    }

    public void depositFromResident(Double amount, Resident resident) throws CivException {
        if (!resident.getTreasury().hasEnough(amount)) {
            throw new CivException(CivSettings.localize.localizedString("var_config_marketItem_notEnoughCurrency", (amount + " " + CivSettings.CURRENCY_NAME)));
        }

        if (this.inDebt()) {
            if (this.getDebt() > amount) {
                this.getTreasury().setDebt(this.getTreasury().getDebt() - amount);
                resident.getTreasury().withdraw(amount);
            } else {
                double leftAmount = amount - this.getTreasury().getDebt();
                this.getTreasury().setDebt(0);
                this.getTreasury().deposit(leftAmount);
                resident.getTreasury().withdraw(amount);
            }

            if (!this.getTreasury().inDebt()) {
                this.daysInDebt = 0;
                CivMessage.global(CivSettings.localize.localizedString("town_ruin_nolongerInDebt", this.getName()));
            }
        } else {
            this.getTreasury().deposit(amount);
            resident.getTreasury().withdraw(amount);
        }
    }

    public Civilization getMotherCiv() {
        return motherCiv;
    }

    public void setMotherCiv(Civilization motherCiv) {
        this.motherCiv = motherCiv;
    }


    public Collection<Resident> getOnlineResidents() {
        LinkedList<Resident> residents = new LinkedList<>();
        for (Resident resident : this.getResidents()) {
            try {
                CivGlobal.getPlayer(resident);
                residents.add(resident);
            } catch (CivException e) {
                //player offline
            }
        }

        residents.addAll(this.fakeResidents.values());

        return residents;
    }

    public void capitulate() {
        if (this.getMotherCiv() == null) {
            return;
        }

        if (this.getMotherCiv().getCapitolName().equals(this.getName())) {
            this.getMotherCiv().capitulate();
            return;
        }

        /* Town is capitulating, no longer need a mother civ. */
        this.setMotherCiv(null);
        CivMessage.global(CivSettings.localize.localizedString("var_town_capitulate1", this.getName(), this.getCiv().getName()));
    }

    public ConfigGovernment getGovernment() {
        if (this.getCiv().getGovernment().id.equals("gov_anarchy")) {
            if (this.motherCiv != null && !this.motherCiv.getGovernment().id.equals("gov_anarchy")) {
                return this.motherCiv.getGovernment();
            }

            if (this.motherCiv != null) {
                return CivSettings.governments.get("gov_tribalism");
            }
        }

        return this.getCiv().getGovernment();
    }

    public AttrSource getFaith() {
        HashMap<String, Double> sources = new HashMap<>();

        /* Grab beakers generated from culture. */
        double fromCulture = this.cultureChunks.values().stream().mapToDouble(CultureChunk::getFaith).sum();
        sources.put("Culture Biomes", fromCulture);

        /* Grab beakers generated from structures with components. */
        double fromStructures = 0;
        for (Structure struct : this.structures.values()) {
            for (Component comp : struct.attachedComponents) {
                if (!(comp instanceof AttributeBase as)) {
                    continue;
                }
                if (as.getString("attribute").equalsIgnoreCase("FAITH")) {
                    fromStructures += as.getGenerated();
                }
            }
        }
        for (Wonder w : this.wonders.values()) {
            for (Component comp : w.attachedComponents) {
                if (!(comp instanceof AttributeBase as)) {
                    continue;
                }
                if (as.getString("attribute").equalsIgnoreCase("FAITH")) {
                    fromStructures += as.getGenerated();
                }
            }
        }

        sources.put("Structures", fromStructures);
        return new AttrSource(sources, max((fromCulture + fromStructures) * getFaithRate().total, 0), null);
    }

    public AttrSource getFaithRate() {
        HashMap<String, Double> rates = new HashMap<>();

        double happiness = this.getHappinessState().culture_rate();
        rates.put("Happiness", happiness);

        // я подозреваю что у нас будут баги с %
        double government = getGovernment().culture_rate;
        rates.put("Government", government);

        double techs = 0.0; //todo: это надо в конфиги вынести
        if (this.hasTechnology("tech_religion")) {
            techs += 0.1;
        }
        if (this.hasTechnology("tech_priesthood")) {
            techs += 0.2;
        }
        if (this.hasTechnology("tech_monarchy")) {
            techs += 0.1;
        }
        if (this.hasTechnology("tech_education")) {
            techs += 0.1;
        }
        if (this.hasTechnology("tech_writing")) {
            techs += 0.15;
        }
        rates.put("Technologies", techs);

        double wonders = 0.0;
        if (this.getCiv().hasWonder("w_notre_dame") || this.getCiv().hasWonder("w_mother_tree")) {
            wonders += 0.25;
        }
        if (this.getCiv().hasWonder("w_pyramid") || this.getCiv().hasWonder("w_hanginggardens")) {
            wonders += 0.25;
        }
        if (this.getBuffManager().hasBuff("wonder_trade_globe_theatre")) {
            wonders += this.getGlobeTradeBuff(Attribute.TypeKeys.FAITH);
        }
        wonders += this.getBuffManager().getEffectiveDouble("wonder_trade_notre_dame");

        rates.put("Wonders", wonders);
        return new AttrSource(rates, happiness * government + techs + wonders, null);
    }


    public AttrSource getBeakerRate() {
        double rate = 1.0;
        HashMap<String, Double> rates = new HashMap<>();

        ConfigHappinessState state = this.getHappinessState();
        double newRate = rate * state.beaker_rate();
        rates.put("Happiness", newRate - rate);
        rate = newRate;

        newRate = rate * getGovernment().beaker_rate;
        rates.put("Government", newRate - rate);
        rate = newRate;

        /* Additional rate increases from buffs. */
        /* Great Library buff is made to not stack with Science_Rate */
        double additional = rate * getBuffManager().getEffectiveDouble(Buff.SCIENCE_RATE);
        additional += rate * getBuffManager().getEffectiveDouble("buff_greatlibrary_extra_beakers");
        if (this.getBuffManager().hasBuff("wonder_trade_globe_theatre")) {
            additional += this.getGlobeTradeBuff(Attribute.TypeKeys.BEAKERS);
        }
        rate += additional;
        rates.put("Goodies/Wonders", additional);

        double education = 0.0;
        for (Structure struct : this.structures.values()) {
            for (Component comp : struct.attachedComponents) {
                if (comp instanceof AttributeBase as) {
                    if (as.getString("attribute").equalsIgnoreCase("BEAKERBOOST")) {
                        double boostPerRes = as.getGenerated();
                        int maxBoost = 0;

                        if (struct instanceof University) {
                            maxBoost = 5;
                        } else if (struct instanceof School || struct instanceof ResearchLab) {
                            maxBoost = 10;
                        }
                        int resCount = Math.min(this.getResidentCount(), maxBoost);
                        education += (boostPerRes * resCount);
                    }
                }
            }
        }

        rate += education;
        rates.put("Education", education);

        return new AttrSource(rates, rate, null);
    }

    public AttrSource getBeakers() {
        AttrCache cache = this.attributeCache.get("BEAKERS");
        if (cache == null) {
            cache = new AttrCache();
            cache.lastUpdate = new Date();
        } else {
            Date now = new Date();
            if (now.getTime() > (cache.lastUpdate.getTime() + ATTR_TIMEOUT_SECONDS * 1000)) {
                cache.lastUpdate = now;
            } else {
                return cache.sources;
            }
        }

        double beakers = 0;
        HashMap<String, Double> sources = new HashMap<>();

        /* Grab beakers generated from culture. */
        double fromCulture = 0;
        for (CultureChunk cc : this.cultureChunks.values()) {
            fromCulture += cc.getBeakers();
        }
        sources.put("Culture Biomes", fromCulture);
        beakers += fromCulture;

        /* Grab beakers generated from structures with components. */
        double fromStructures = 0;
        for (Structure struct : this.structures.values()) {
            for (Component comp : struct.attachedComponents) {
                if (comp instanceof AttributeBase as) {
                    if (as.getString("attribute").equalsIgnoreCase("BEAKERS")) {
                        fromStructures += as.getGenerated();
                    }
                }
            }
        }

        beakers += fromStructures;
        sources.put("Structures", fromStructures);

        /* Grab any extra beakers from buffs. */
        double wondersTrade = 0;

        //No more flat bonuses here, leaving it in case of new buffs

        beakers += wondersTrade;
        sources.put("Goodies/Wonders", wondersTrade);

        /* Make sure we never give out negative beakers. */
        beakers = max(beakers, 0);
        AttrSource rates = getBeakerRate();


        beakers = beakers * rates.total;

        if (beakers < 0) {
            beakers = 0;
        }

        AttrSource as = new AttrSource(sources, beakers, null);
        cache.sources = as;
        this.attributeCache.put("BEAKERS", cache);
        return as;
    }

    /*
     * Gets the basic amount of happiness for a town.
     */
    public AttrSource getHappiness() {
        HashMap<String, Double> sources = new HashMap<>();
        double total = 0;

        AttrCache cache = this.attributeCache.get("HAPPINESS");
        if (cache == null) {
            cache = new AttrCache();
            cache.lastUpdate = new Date();
        } else {
            Date now = new Date();
            if (now.getTime() > (cache.lastUpdate.getTime() + ATTR_TIMEOUT_SECONDS * 1000)) {
                cache.lastUpdate = now;
            } else {
                return cache.sources;
            }
        }

        /* Add happiness from town level. */
        double townlevel = CivSettings.townHappinessLevels.get(this.getLevel()).happiness;
        total += townlevel;
        sources.put("Base Happiness", townlevel);

        /* Grab any sources from buffs. */
        double goodiesWonders = this.buffManager.getEffectiveDouble("buff_hedonism");
        sources.put("Goodies/Wonders", goodiesWonders);
        total += goodiesWonders;

        /* Grab happiness from the number of trade goods socketed. */
        int tradeGoods = this.bonusGoodies.size();
        if (tradeGoods > 0) {
            sources.put("Trade Goods", (double) tradeGoods);
        }
        total += tradeGoods;

        /* Add in base happiness if it exists. */
        if (this.baseHappy != 0) {
            sources.put("Base Happiness", this.baseHappy);
            total += baseHappy;
        }

        /* Grab beakers generated from culture. */
        double fromCulture = 0;
        for (CultureChunk cc : this.cultureChunks.values()) {
            fromCulture += cc.getHappiness();
        }
        sources.put("Culture Biomes", fromCulture);
        total += fromCulture;

        /* Grab happiness generated from structures with components. */
        double structures = 0;
        for (Structure struct : this.structures.values()) {
            for (Component comp : struct.attachedComponents) {
                if (comp instanceof AttributeBase as) {
                    if (as.getString("attribute").equalsIgnoreCase("HAPPINESS")) {
                        structures += as.getGenerated();
                    }
                }
            }
        }
        total += structures;
        sources.put("Structures", structures);

        if (total < 0) {
            total = 0;
        }

        double randomEvent = RandomEvent.getHappiness(this);
        total += randomEvent;
        sources.put("Random Events", randomEvent);

        //TODO Governments

        AttrSource as = new AttrSource(sources, total, null);
        cache.sources = as;
        this.attributeCache.put("HAPPINESS", cache);
        return as;
    }

    /*
     * Gets the basic amount of happiness for a town.
     */
    public AttrSource getUnhappiness() {

        AttrCache cache = this.attributeCache.get("UNHAPPINESS");
        if (cache == null) {
            cache = new AttrCache();
            cache.lastUpdate = new Date();
        } else {
            Date now = new Date();
            if (now.getTime() > (cache.lastUpdate.getTime() + ATTR_TIMEOUT_SECONDS * 1000)) {
                cache.lastUpdate = now;
            } else {
                return cache.sources;
            }
        }

        HashMap<String, Double> sources = new HashMap<>();

        /* Get the unhappiness from the civ. */
        double total = this.getCiv().getCivWideUnhappiness(sources);


        /* Get unhappiness from residents. */
        double per_resident = CivSettings.happinessConfig.getDouble("happiness.per_resident", 0.25);
        HashSet<Resident> UnResidents = new HashSet<>();
        HashSet<Resident> NonResidents = new HashSet<>();
        for (PermissionGroup group : this.getGroups()) {
            for (Resident res : group.getMemberList()) {
                if (res.getCiv() == null) {
                    UnResidents.add(res);
                    continue;
                }
                if (res.getCiv() != this.getCiv()) {
                    NonResidents.add(res);
                }
            }
        }

        double happy_resident = per_resident * this.getResidents().size();
        double happy_Nonresident = (per_resident * 0.25) * NonResidents.size();
        double happy_Unresident = per_resident * UnResidents.size();
        sources.put("Residents", (happy_resident + happy_Nonresident + happy_Unresident));
        total += happy_resident + happy_Nonresident + happy_Unresident;

        /* Try to reduce war unhappiness via the component. */
        if (sources.containsKey("War")) {
            for (Structure struct : this.structures.values()) {
                for (Component comp : struct.attachedComponents) {
                    if (!comp.isActive()) {
                        continue;
                    }

                    if (comp instanceof AttributeWarUnhappiness warunhappyComp) {
                        double value = sources.get("War"); // Negative if a reduction
                        value += warunhappyComp.value;

                        if (value < 0) {
                            value = 0;
                        }

                        sources.put("War", value);
                    }
                }
            }
        }

        /* Get distance unhappiness from capitol. */
        if (this.getMotherCiv() == null && !this.isCapitol()) {
            double distance_unhappy = this.getCiv().getDistanceHappiness(this);
            total += distance_unhappy;
            sources.put("Distance To Capitol", distance_unhappy);
        }

        /* Add in base unhappiness if it exists. */
        if (this.baseUnhappy != 0) {
            sources.put("Base Unhappiness", this.baseUnhappy);
            total += this.baseUnhappy;
        }

        /* Grab unhappiness generated from structures with components. */
        double structures = 0;
        for (Structure struct : this.structures.values()) {
            for (Component comp : struct.attachedComponents) {
                if (!(comp instanceof AttributeBase as)) {
                    continue;
                }
                if (as.getString("attribute").equalsIgnoreCase("UNHAPPINESS")) {
                    structures += as.getGenerated();
                }
            }
        }
        total += structures;
        sources.put("Structures", structures);

        /* Grabe unhappiness from Random events. */
        double randomEvent = RandomEvent.getUnhappiness(this);
        total += randomEvent;
        if (randomEvent > 0) {
            sources.put("Random Events", randomEvent);
        }


        //TODO Spy Missions
        //TODO Governments

        if (total < 0) {
            total = 0;
        }

        AttrSource as = new AttrSource(sources, total, null);
        cache.sources = as;
        this.attributeCache.put("UNHAPPINESS", cache);
        return as;
    }

    /*
     * Gets the rate at which we will modify other stats
     * based on the happiness level.
     */
    public double getHappinessModifier() {
        return 1.0;
    }

    public double getHappinessPercentage() {
        return getHappiness().total / (getHappiness().total + getUnhappiness().total);
    }

    public ConfigHappinessState getHappinessState() {
        return CivSettings.getHappinessState(this.getHappinessPercentage());
    }

    public void setBaseHappiness(double happy) {
        this.baseHappy = happy;
    }

    public void setBaseUnhappy(double happy) {
        this.baseUnhappy = happy;
    }

    public double getBaseGrowth() {
        return baseGrowth;
    }

    public void setBaseGrowth(double baseGrowth) {
        this.baseGrowth = baseGrowth;
    }

    public Buildable getCurrentStructureInProgress() {
        return currentStructureInProgress;
    }

    public void setCurrentStructureInProgress(Buildable currentStructureInProgress) {
        this.currentStructureInProgress = currentStructureInProgress;
    }

    public Buildable getCurrentWonderInProgress() {
        return currentWonderInProgress;
    }

    public void setCurrentWonderInProgress(Buildable currentWonderInProgress) {
        this.currentWonderInProgress = currentWonderInProgress;
    }

    public void addFakeResident(Resident fake) {
        this.fakeResidents.put(fake.getName(), fake);
    }

    private static String lastMessage = null;

    public boolean processSpyExposure(Resident resident) {
        double exposure = resident.getSpyExposure();
        double percent = exposure / Resident.MAX_SPY_EXPOSURE;

        Player player;
        try {
            player = CivGlobal.getPlayer(resident);
        } catch (CivException e1) {
            e1.printStackTrace();
            return false;
        }

        String message = "";

        if (percent >= CivSettings.espionageConfig.getDouble("espionage.town_exposure_failure", 2.0)) {
            CivMessage.sendTown(this, String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.localize.localizedString("town_spy_thwarted"));
            return true;
        }

        if (percent > CivSettings.espionageConfig.getDouble("espionage.town_exposure_warning", 0.25)) {
            message += CivSettings.localize.localizedString("town_spy_currently") + " ";
        }

        if (percent > CivSettings.espionageConfig.getDouble("espionage.town_exposure_location", 0.5)) {
            message += CivSettings.localize.localizedString("var_town_spy_location", (player.getLocation().getBlockX() + ", " + player.getLocation().getBlockY() + ", " + player.getLocation().getBlockZ())) + " ";
        }

        if (percent > CivSettings.espionageConfig.getDouble("espionage.town_exposure_name", 0.75)) {
            message += CivSettings.localize.localizedString("var_town_spy_perpetrator", resident.getName());
        }

        if (!message.isEmpty()) {
            if (lastMessage == null || !lastMessage.equals(message)) {
                CivMessage.sendTown(this, String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + message);
                lastMessage = message;
            }
        }

        return false;
    }

    public RandomEvent getActiveEvent() {
        return activeEvent;
    }

    public void setActiveEvent(RandomEvent activeEvent) {
        this.activeEvent = activeEvent;
    }

    public double getUnusedBeakers() {
        return unusedBeakers;
    }

    public void setUnusedBeakers(double unusedBeakers) {
        this.unusedBeakers = unusedBeakers;
    }

    public void addUnusedBeakers(double more) {
        this.unusedBeakers += more;
    }

    public void markLastBuildableRefeshAsNow() {
        this.lastBuildableRefresh = new Date();
    }

    public void refreshNearestBuildable(Resident resident) throws CivException {
        if (!this.getMayorGroup().hasMember(resident)) {
            throw new CivException(CivSettings.localize.localizedString("town_refresh_errorNotMayor"));
        }

        if (this.lastBuildableRefresh != null) {
            int buildable_refresh_cooldown = CivSettings.townConfig.getInt("town.buildable_refresh_cooldown", 30);

            if (new Date().getTime() < this.lastBuildableRefresh.getTime() + ((long) buildable_refresh_cooldown * 60 * 1000)) {
                throw new CivException(CivSettings.localize.localizedString("var_town_refresh_wait1", buildable_refresh_cooldown));
            }
        }

        Buildable buildable = CivGlobal.getNearestBuildable(CivGlobal.getPlayer(resident).getLocation());
        if (buildable == null) {
            throw new CivException(CivSettings.localize.localizedString("town_refresh_couldNotFind"));
        }

        if (!buildable.isActive()) {
            throw new CivException(CivSettings.localize.localizedString("town_refresh_errorInProfress"));
        }

        if (War.isWarTime()) {
            throw new CivException(CivSettings.localize.localizedString("town_refresh_errorWar"));
        }

        if (buildable.getTown() != this) {
            throw new CivException(CivSettings.localize.localizedString("town_refresh_errorWrongTown"));
        }

        resident.setInteractiveMode(new InteractiveBuildableRefresh(buildable, resident.getName()));
    }

    public boolean areMayorsInactive() {
        int mayor_inactive_days = CivSettings.townConfig.getInt("town.mayor_inactive_days", 7);
        for (Resident resident : this.getMayorGroup().getMemberList()) {
            if (resident.isInactiveForDays(mayor_inactive_days)) {
                return false;
            }
        }

        return true;
    }

    public void rename(String name) throws CivException, InvalidNameException {
        if (getTown(name) != null) {
            throw new CivException(CivSettings.localize.localizedString("town_rename_errorExists"));
        }

        if (this.isCapitol()) {
            this.getCiv().setCapitolName(name);
        }

        String oldName = this.getName();

        removeTown(this);

        this.setName(name);

        addTown(this);

        CivMessage.global(CivSettings.localize.localizedString("var_town_rename_success1", oldName, this.getName()));
    }

    public void trimCultureChunks(HashSet<ChunkCoord> expanded) {

        LinkedList<ChunkCoord> removedKeys = new LinkedList<>();
        for (ChunkCoord coord : this.cultureChunks.keySet()) {
            if (!expanded.contains(coord)) {
                removedKeys.add(coord);
            }
        }

        for (ChunkCoord coord : removedKeys) {
            CivGlobal.removeCultureChunk(CivGlobal.getCultureChunk(coord));
            this.cultureChunks.remove(coord);
        }
    }

    public ChunkCoord getTownCultureOrigin() {
        /* Culture now only eminates from the town hall. */
        if (this.getTownHall() == null) {
            /* if no town hall, pick a 'random' town chunk' */
            return this.getTownChunks().iterator().next().getChunkCoord();
        }
        /* Grab town chunk from town hall location. */
        return new ChunkCoord(this.getTownHall().getCenterLocation());
    }

    public Date getCreated() {
        return created_date;
    }

    public void setCreated(Date created_date) {
        this.created_date = created_date;
    }

    public void validateGift() throws CivException {
        int min_gift_age = CivSettings.civConfig.getInt("civ.min_gift_age", 14);

        if (!DateUtil.isAfterDays(created_date, min_gift_age)) {
            throw new CivException(CivSettings.localize.localizedString("var_town_gift_errorAge1", this.getName(), min_gift_age));
        }
    }

    public int getGiftCost() {
        return CivSettings.civConfig.getInt("civ.gift_cost_per_town", 0);
    }

    public void clearBonusGoods() {
        this.bonusGoodies.clear();
    }

    public void processStructureFlipping(HashMap<ChunkCoord, Structure> centerCoords) {
        for (CultureChunk cc : this.cultureChunks.values()) {
            Structure struct = centerCoords.get(cc.getChunkCoord());
            if (struct == null) {
                continue;
            }

            if (struct.getCiv() == cc.getCiv()) {
                continue;
            }

            /*
             * There is a structure at this location that doesnt belong to us!
             * Grab it!
             */
            struct.getTown().removeStructure(struct);
            struct.getTown().addStructure(struct);
            struct.setTown(this);
        }
    }

    public boolean hasDisabledStructures() {
        return !this.disabledBuildables.isEmpty();
    }

    public ArrayList<Player> getOnlinePlayers() {
        ArrayList<Player> online = new ArrayList<>();
        for (Resident resident : this.getResidents()) {
            Player player;
            try {
                player = CivGlobal.getPlayer(resident);
            } catch (CivException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            if (player.isOnline()) {
                online.add(player);
            }
        }
        return online;
    }

    public Collection<Buildable> getDisabledBuildables() {
        return this.disabledBuildables.values();
    }

    public Granary getGranary(int granaryNumber) {
        return granaryAL.get(granaryNumber);
    }

    public ArrayList<Granary> getGranaries() {
        return granaryAL;
    }

    public void saveGranaryResources(double i1, double i2, double i3, double i4, double i5, double i6) {
        String s = granaryResources;
        if (s != null && s != "") {
            String[] c = s.split("/");
            double iron = Double.parseDouble(c[0]), gold = Double.parseDouble(c[1]), diamond = Double.parseDouble(c[2]), emerald = Double.parseDouble(c[3]), tungsten = Double.parseDouble(c[4]), chromium = Double.parseDouble(c[5]);
            iron += i1;
            gold += i2;
            diamond += i3;
            emerald += i4;
            tungsten += i5;
            chromium += i6;
            s = iron + "/" + gold + "/" + diamond + "/" + emerald + "/" + tungsten + "/" + chromium;
        } else {
            s = i1 + "/" + i2 + "/" + i3 + "/" + i4 + "/" + i5 + "/" + i6;
        }
        granaryResources = s;

    }

    public Granary getFreeGranary() {
        for (Granary granary : getGranaries()) {
            if (granary.isFree()) {
                return granary;
            }
        }
        return null;
    }

    public void addGranary(Granary g) {
        this.granaryAL.add(g);
    }

    public void removeGranary(Granary g) {
        this.granaryAL.remove(g);
    }

    public double getGlobeTradeBuff(Attribute.TypeKeys ab) {
        double globeTrade = 0.0;
        double modifier = this.getBuffManager().getEffectiveDouble("wonder_trade_globe_theatre");
        int i = this.getCiv().getTownIndex(this);
        for (Civilization allyCiv : this.getCiv().getDiplomacyManager().getAllies()) {
            Town t = allyCiv.getTownByIndex(i);
            switch (ab) {
                case BEAKERS -> globeTrade += t.getBeakerRate().total;
                case HAMMERS -> globeTrade += t.getHammerRate().total;
                case FAITH -> globeTrade += t.getFaithRate().total;
                case GROWTH -> globeTrade += t.getGrowthRate().total;
                case COINS -> globeTrade += t.getCultureRate().total;
                default -> {
                }
            }
        }
        return globeTrade * modifier;
    }

    public double getBuildSpeed() {
        double rate = 1.0;
        rate -= this.getBuffManager().getEffectiveDouble(Buff.BUILDSPEED);
        rate += this.getBuffManager().getEffectiveDouble(Buff.BUILDSLOW);

        return rate;
    }


}
