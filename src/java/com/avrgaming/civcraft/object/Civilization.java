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
 * from AVRGAMING LLC. */
package com.avrgaming.civcraft.object;

import com.avrgaming.civcraft.camp.WarCamp;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigGovernment;
import com.avrgaming.civcraft.config.ConfigReligion;
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.database.SQLController;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.endgame.EndConditionScience;
import com.avrgaming.civcraft.endgame.EndGameCondition;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Relation.Status;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.structure.Capitol;
import com.avrgaming.civcraft.structure.RespawnLocationHolder;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.TownHall;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.UpdateTechBar;
import com.avrgaming.civcraft.threading.timers.BeakerTimer;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.DateUtil;
import com.avrgaming.civcraft.util.ItemManager;
import net.minecraft.server.v1_12_R1.NBTCompressedStreamTools;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.NBTTagList;
import net.minecraft.server.v1_12_R1.NBTTagString;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Double.min;

public class Civilization extends SQLObject {

    public static final Map<String, Civilization> civs = new ConcurrentHashMap<>();
    private final Map<String, ConfigTech> techs = new ConcurrentHashMap<>();

    private int color;
    private int daysInDebt = 0;
    private int currentEra = 0;
    private double incomeTaxRate;
    private double sciencePercentage;
    private ConfigTech researchTech = null;
    private double researchProgress = 0.0;

    private EconObject treasury;
    private PermissionGroup leaderGroup;
    private PermissionGroup adviserGroup;

    /* Strings used for reverse lookups. */
    private String leaderName;
    private String leaderGroupName = "leaders";
    private String advisersGroupName = "advisers";
    private String capitolName;
    private ConfigReligion civReligion;
    private final ConcurrentHashMap<String, Town> towns = new ConcurrentHashMap<>();
    private ConfigGovernment government;

    private double baseBeakers = 1.0;

    public static final int HEX_COLOR_MAX = 16777215;
    public static final int HEX_COLOR_TOLERANCE = 40;

    /* Store information to display about last upkeep paid. */
    public HashMap<String, Double> lastUpkeepPaidMap = new HashMap<>();

    /* Store information about last tick's taxes */
    public HashMap<String, Double> lastTaxesPaidMap = new HashMap<>();

    /* Used to prevent spam of tech % complete message. */
    private int lastTechPercentage = 0;

    private Town capitol;

    private final DiplomacyManager diplomacyManager = new DiplomacyManager(this);

    private boolean adminCiv = false;
    private boolean conquered = false;

    private Date conquer_date = null;
    private Date created_date = null;

    public boolean scoutDebug = false;
    public String scoutDebugPlayer = null;

    public String messageOfTheDay = "";

    private final LinkedList<WarCamp> warCamps = new LinkedList<>();

    public Civilization(String name, String capitolName, Resident leader) throws InvalidNameException {
        this.setName(name);
        this.leaderName = leader.getUUID().toString();
        this.setCapitolName(capitolName);
        this.capitol = Town.getTown(capitolName);

        this.government = CivSettings.governments.get("gov_tribalism");
        this.color = this.pickCivColor();
        this.setTreasury(CivGlobal.createEconObject(this));
        this.getTreasury().setBalance(0, false);
        this.created_date = new Date();
        this.civReligion = CivSettings.religions.get("rel_paganism");
        loadSettings();
    }

    public Civilization(ResultSet rs) throws SQLException, InvalidNameException {
        this.load(rs);
        loadSettings();
    }

    public static void newCiv(String name, String capitolName, Resident resident,
                              Player player, Location loc) throws CivException {

        ItemStack stack = player.getInventory().getItemInMainHand();
        /*
         * Verify we have the correct item somewhere in our inventory.
         */
        LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
        if (craftMat == null || !craftMat.hasComponent("FoundCivilization")) {
            throw new CivException(CivSettings.localize.localizedString("civ_found_notItem"));
        }

        if (getByName(name) != null) {
            throw new CivException(CivSettings.localize.localizedString("var_civ_found_civExists", name));
        }

        if (Town.getTown(capitolName) != null) {
            throw new CivException(CivSettings.localize.localizedString("var_civ_found_townExists", capitolName));
        }

        if (resident.hasCamp()) {
            throw new CivException(CivSettings.localize.localizedString("civ_found_mustleavecamp"));
        }

        //Test that we are not too close to another civ
        int min_distance = CivSettings.civConfig.getInt("civ.min_distance", 15);
        ChunkCoord foundLocation = new ChunkCoord(loc);

        for (CultureChunk cc : CivGlobal.getCultureChunks()) {
            double dist = foundLocation.distance(cc.getChunkCoord());
            if (dist <= min_distance) {
                DecimalFormat df = new DecimalFormat();
                throw new CivException(CivSettings.localize.localizedString("var_civ_found_errorTooClose1", cc.getCiv().getName(), df.format(dist), min_distance));
            }
        }

        try {
            Civilization civ = new Civilization(name, capitolName, resident);
            try {
                civ.saveNow();
            } catch (SQLException e) {
                CivLog.error("Caught exception:" + e.getMessage() + " error code:" + e.getErrorCode());
                if (e.getMessage().contains("Duplicate entry")) {
                    SQLController.deleteByName(name, TABLE_NAME);
                    throw new CivException(CivSettings.localize.localizedString("civ_found_databaseException"));
                }
            }

            // Create permission groups for civs.
            PermissionGroup leadersGroup = new PermissionGroup(civ, "leaders");
            leadersGroup.addMember(resident);
            civ.setLeaderGroup(leadersGroup);

            PermissionGroup adviserGroup = new PermissionGroup(civ, "advisers");
            civ.setAdviserGroup(adviserGroup);

            /* Save this civ in the db and hashtable. */
            try {
                Town.newTown(resident, capitolName, civ, true, true, loc);
            } catch (CivException e) {
                e.printStackTrace();
                civ.delete();
                throw e;
            }

            addCiv(civ);
            ItemStack newStack = new ItemStack(Material.AIR);
            player.getInventory().setItemInMainHand(newStack);
            CivMessage.globalTitle(CivSettings.localize.localizedString("var_civ_found_successTitle", civ.getName()), CivSettings.localize.localizedString("var_civ_found_successSubTitle", civ.getCapitolName(), player.getName()));

        } catch (InvalidNameException e) {
            throw new CivException(CivSettings.localize.localizedString("var_civ_found_invalidName", name));
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CivException(CivSettings.localize.localizedString("internalDatabaseException"));
        }

    }

    public static String TABLE_NAME = "CIVILIZATIONS";

    public static void init() throws SQLException {
        if (!SQLController.hasTable(TABLE_NAME)) {
            String table_create = "CREATE TABLE " + SQLController.tb_prefix + TABLE_NAME + " (" +
                    "`uuid` VARCHAR(36) NOT NULL," +
                    "`nbt` BLOB," +
                    "PRIMARY KEY (`uuid`)" + ")";

            SQLController.makeTable(table_create);
            CivLog.info("Created " + TABLE_NAME + " table");
        } else {
            CivLog.info(TABLE_NAME + " table OK!");
            SQLController.makeCol("nbt", "BLOB", TABLE_NAME);
        }
    }

    public static void addCiv(Civilization civ) {
        civs.put(civ.getName().toLowerCase(), civ);
        if (civ.isAdminCiv()) {
            CivGlobal.addAdminCiv(civ);
        }
    }

    public static Civilization getByName(String name) {
        return civs.get(name.toLowerCase());
    }

    public static Civilization getCivFromUUID(UUID uuid) {
        for (Civilization civ : civs.values()) {
            if (civ.getUUID().equals(uuid)) {
                return civ;
            }
        }
        return null;
    }

    public static Collection<Civilization> getCivs() {
        return civs.values();
    }

    public static void removeCiv(Civilization civilization) {
        civs.remove(civilization.getName().toLowerCase());
        if (civilization.isAdminCiv()) {
            CivGlobal.removeAdminCiv(civilization);
        }
    }

    @Override
    public void load(ResultSet rs) throws SQLException, InvalidNameException {
        this.setUUID(UUID.fromString(rs.getString("uuid")));
        var data = new ByteArrayInputStream(rs.getBytes("nbt"));
        NBTTagCompound nbt;
        try {
            nbt = NBTCompressedStreamTools.a(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.setName(nbt.getString("name"));
        //		Resident res = CivGlobal.getResidentViaUUID(UUID.fromString(resUUID));
        leaderName = nbt.getString("leaderName");


        capitolName = nbt.getString("capitolName");
        this.addGroup(new PermissionGroup(this, nbt.getCompound("leaderGroup")));
        this.addGroup(new PermissionGroup(this, nbt.getCompound("advisersGroup")));
        daysInDebt = nbt.getInt("daysInDebt");
        this.color = nbt.getInt("color");
        this.setResearchTech(CivSettings.techs.get(nbt.getString("researchTech")));
        this.setResearchProgress(nbt.getDouble("researchProgress"));
        this.setGovernment(nbt.getString("government_id"));
        this.loadKeyValueString(nbt.getString("lastUpkeepTick"), this.lastUpkeepPaidMap);
        this.loadKeyValueString(nbt.getString("lastTaxesTick"), this.lastTaxesPaidMap);
        this.setSciencePercentage(nbt.getDouble("science_percentage"));
        this.setCivReligion(CivSettings.religions.get(nbt.getString("religion_id")));
        NBTTagList researched_techs_list = nbt.getList("researched_techs", new NBTTagString("").getTypeId());
        for (int i = 0; i < researched_techs_list.size(); i++) {
            ConfigTech tech = CivSettings.techs.get(researched_techs_list.getString(i));
            if (tech == null) {
                continue;
            }
            CivGlobal.researchedTechs.add(tech.id().toLowerCase());
            this.techs.put(tech.id(), tech);
        }
        this.adminCiv = nbt.getBoolean("adminCiv");
        this.conquered = nbt.getBoolean("conquered");
        long ctime = nbt.getLong("conquered_date");
        this.incomeTaxRate = loadIncomeTaxRate();
        if (ctime == 0) {
            this.conquer_date = null;
        } else {
            this.conquer_date = new Date(ctime);
        }

        String motd = nbt.getString("motd");
        if (motd.isEmpty()) {
            this.messageOfTheDay = null; //Forever in the past.
        } else {
            this.messageOfTheDay = motd;
        }

        this.created_date = new Date(nbt.getLong("created_date"));

        this.setTreasury(CivGlobal.createEconObject(this));
        this.getTreasury().setBalance(nbt.getDouble("coins"), false);
        this.getTreasury().setDebt(nbt.getDouble("debt"));

        for (ConfigTech tech : this.getTechs()) {
            if (tech.era() > this.getCurrentEra()) {
                this.setCurrentEra(tech.era());
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
        NBTTagCompound nbt = new NBTTagCompound();

        nbt.setString("name", this.getName());
        nbt.setString("leaderName", this.getLeader().getUUIDString());

        nbt.setString("capitolName", this.capitolName);
        nbt.set("leaderGroup", new NBTTagCompound());
        this.getLeaderGroup().saveToNBT(nbt.getCompound("leaderGroup"));
        nbt.set("advisersGroup", new NBTTagCompound());
        this.getAdviserGroup().saveToNBT(nbt.getCompound("advisersGroup"));
        nbt.setDouble("debt", this.getTreasury().getDebt());
        nbt.setDouble("coins", this.getTreasury().getBalance());
        nbt.setInt("daysInDebt", this.daysInDebt);
        nbt.setDouble("science_percentage", this.getSciencePercentage());
        nbt.setInt("color", this.getColor());
        nbt.setString("religion_id", this.getReligion().id);
        //hashmap.put("taxrate", this.getIncomeTaxRate());
        if (this.getResearchTech() != null) {
            nbt.setString("researchTech", this.getResearchTech().id());
        }
        nbt.setDouble("researchProgress", this.getResearchProgress());
        nbt.setString("government_id", this.getGovernment().id);
        nbt.setString("lastUpkeepTick", this.saveKeyValueString(this.lastUpkeepPaidMap));
        nbt.setString("lastTaxesTick", this.saveKeyValueString(this.lastTaxesPaidMap));
        NBTTagList researched_techs_list = new NBTTagList();
        for (ConfigTech tech : this.techs.values()) {
            researched_techs_list.add(new NBTTagString(tech.id()));
        }
        nbt.set("researched_techs", researched_techs_list);
        if (this.adminCiv) {
            nbt.setBoolean("adminCiv", true);
        }
        if (this.conquered) {
            nbt.setBoolean("conquered", true);
            nbt.setLong("conquered_date", this.conquer_date.getTime());
        }

        if (this.messageOfTheDay != null) {
            nbt.setString("motd", this.messageOfTheDay);
        }

        nbt.setLong("created_date", this.created_date.getTime());

        var data = new ByteArrayOutputStream();
        try {
            NBTCompressedStreamTools.a(nbt, data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        hashmap.put("nbt", data.toByteArray());

        SQLController.updateNamedObject(this, hashmap, TABLE_NAME);
    }

    public double loadIncomeTaxRate() {
        return getGovernment().maximum_tax_rate + getReligion().tax_rate;
    }

    private void loadKeyValueString(String string, HashMap<String, Double> map) {

        String[] keyvalues = string.split(";");

        for (String keyvalue : keyvalues) {
            try {
                String key = keyvalue.split(":")[0];
                String value = keyvalue.split(":")[1];

                map.put(key, Double.valueOf(value));
            } catch (ArrayIndexOutOfBoundsException e) {
                // forget it then.
            }
        }

    }

    private String saveKeyValueString(HashMap<String, Double> map) {
        StringBuilder out = new StringBuilder();

        for (String key : map.keySet()) {
            double value = map.get(key);
            out.append(key).append(":").append(value).append(";");
        }
        return out.toString();
    }

    public boolean hasTechnology(String require_tech) {

        if (require_tech != null) {
            String[] split = require_tech.split(":");
            for (String str : split) {
                if (!hasTech(str)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean hasTech(String configId) {
        if (configId == null || configId.isEmpty()) {
            return true;
        }

        return techs.containsKey(configId);
    }

    public void addTech(ConfigTech tech) {
        if (tech.era() > this.getCurrentEra()) {
            this.setCurrentEra(tech.era());
        }

        CivGlobal.researchedTechs.add(tech.id().toLowerCase());
        techs.put(tech.id(), tech);

        for (Town town : this.getTowns()) {
            town.onTechUpdate();
        }

    }

    public void removeTech(ConfigTech t) {
        removeTech(t.id());
    }

    public void removeTech(String configId) {
        techs.remove(configId);

        for (Town town : this.getTowns()) {
            town.onTechUpdate();
        }
    }

    public ConfigGovernment getGovernment() {
        return government;
    }

    public void setGovernment(String gov_id) {
        this.government = CivSettings.governments.get(gov_id);

        if (this.getSciencePercentage() > this.government.maximum_tax_rate) {
            this.setSciencePercentage(this.government.maximum_tax_rate);
        }

    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setMotd(String message) {
        this.messageOfTheDay = message;
    }

    public String MOTD() {
        return this.messageOfTheDay;
    }

    public Resident getLeader() {
        return CivGlobal.getResidentViaUUID(UUID.fromString(leaderName));
    }

    public void setLeader(Resident leader) {
        this.leaderName = leader.getUUID().toString();
    }

    @Override
    public void delete() throws SQLException {

        /* First delete all of our groups. */
        if (this.leaderGroup != null) {
        }

        if (this.adviserGroup != null) {
        }

        /* Delete all of our towns. */
        for (Town town : getTowns()) {
            town.delete();
        }

        /* Delete all relationships with other civs. */
        this.diplomacyManager.deleteAllRelations();

        SQLController.deleteNamedObject(this, TABLE_NAME);
        removeCiv(this);
        if (this.isConquered()) {
            CivGlobal.removeConqueredCiv(this);
        }
    }

    public EconObject getTreasury() {
        return treasury;
    }

    public void setTreasury(EconObject treasury) {
        this.treasury = treasury;
    }

    public String getLeaderGroupName() {
        return this.leaderGroupName;
    }

    public void setLeaderGroupName(String leaderGroupName) {
        this.leaderGroupName = "leaders";
    }

    public String getAdvisersGroupName() {
        return this.advisersGroupName;
    }

    public void setAdvisersGroupName(String advisersGroupName) {
        this.advisersGroupName = "advisers";
    }

    public double getIncomeTaxRate() {
        return incomeTaxRate;
    }

    public void setIncomeTaxRate(double taxRate) {
        this.incomeTaxRate = taxRate;
    }

    public Town getTown(String name) {
        return towns.get(name.toLowerCase());
    }

    public void addTown(Town town) {
        towns.put(town.getName().toLowerCase(), town);
    }

    public int getTownCount() {
        return towns.size();
    }

    public String getIncomeTaxRateString() {
        return (this.incomeTaxRate * 100) + "%";
    }

    public void loadSettings() {
        this.baseBeakers = CivSettings.civConfig.getDouble("civ.base_beaker_rate", 1.0);
    }

    public String getCapitolName() {
        return capitolName;
    }

    public void setCapitolName(String capitolName) {
        this.capitolName = capitolName;
    }

    public void addGroup(PermissionGroup grp) {

        if (grp.getName().equalsIgnoreCase(this.leaderGroupName)) {
            this.setLeaderGroup(grp);
        } else if (grp.getName().equalsIgnoreCase(this.advisersGroupName)) {
            this.setAdviserGroup(grp);
        }
    }

    public PermissionGroup getLeaderGroup() {
        return leaderGroup;
    }

    public void setLeaderGroup(PermissionGroup leaderGroup) {
        this.leaderGroup = leaderGroup;
    }

    public PermissionGroup getAdviserGroup() {
        return adviserGroup;
    }

    public void setAdviserGroup(PermissionGroup adviserGroup) {
        this.adviserGroup = adviserGroup;
    }

    public Collection<Town> getTowns() {
        return this.towns.values();
    }

    public double getWarUpkeep() {
        double upkeep = 0;
        boolean doublePenalty = false;

        /* calculate war upkeep from being an aggressor. */
        for (Relation relation : this.getDiplomacyManager().getRelations()) {
            if (relation.getStatus() != Status.WAR) {
                continue;
            }
            if (relation.getAggressor() != this) {
                continue;
            }
            int ourScore = CivGlobal.getScoreForCiv(this);
            int theirScore = CivGlobal.getScoreForCiv(relation.getOtherCiv());
            int scoreDiff = ourScore - theirScore;
            double thisWarUpkeep = CivSettings.warConfig.getDouble("war.upkeep_per_war", 3000.0);
            if (scoreDiff > 0) {
                double war_penalty = CivSettings.warConfig.getDouble("war.upkeep_per_war_multiplier", 0.005);

                thisWarUpkeep += (scoreDiff) * war_penalty;
            }

            /* Try to find notredame in ourenemies buff list or their allies list. */
            ArrayList<Civilization> allies = new ArrayList<>();
            allies.add(relation.getOtherCiv());
            for (Relation relation2 : relation.getOtherCiv().getDiplomacyManager().getRelations()) {
                if (relation2.getStatus() == Status.ALLY) {
                    allies.add(relation2.getOtherCiv());
                }
            }

            for (Civilization civ : allies) {
                for (Town t : civ.getTowns()) {
                    if (t.getBuffManager().hasBuff("buff_notre_dame_extra_war_penalty")) {
                        doublePenalty = true;
                        break;
                    }
                }
            }

            if (doublePenalty) {
                thisWarUpkeep *= 2;
            }

            upkeep += thisWarUpkeep;
        }

        return upkeep;
    }

    public double getWarUnhappiness() {
        double happy = 0;

        /* calculate war upkeep from being an aggressor. */
        for (Relation relation : this.getDiplomacyManager().getRelations()) {
            if (relation.getStatus() != Status.WAR) {
                continue;
            }
            if (relation.getAggressor() != this) {
                continue;
            }

            int ourScore = CivGlobal.getScoreForCiv(this);
            int theirScore = CivGlobal.getScoreForCiv(relation.getOtherCiv());
            int scoreDiff = ourScore - theirScore;
            double thisWarUpkeep = CivSettings.happinessConfig.getDouble("happiness.per_war", 1.0);
            if (scoreDiff > 0) {
                double war_penalty = CivSettings.happinessConfig.getDouble("happiness.per_war_score", 0.0001);
                double addedFromPoints = scoreDiff * war_penalty;
                thisWarUpkeep += Math.min(CivSettings.happinessConfig.getDouble("happiness.per_war_score_max", 3.0), addedFromPoints);
            }

            happy += thisWarUpkeep;

        }

        return happy;
    }


    public double getDistanceUpkeepAtLocation(Location capitolTownHallLoc, Location townHallLoc, boolean touching) {
        double town_distance_base_upkeep = CivSettings.civConfig.getDouble("civ.town_distance_base_upkeep", 100.0);
        double distance_multiplier_touching = CivSettings.civConfig.getDouble("civ.town_distance_multiplier", 0.3);
        double distance_multiplier_not_touching = CivSettings.civConfig.getDouble("civ.town_distance_multiplier_outside_culture", 0.9);
        double maxDistanceUpkeep = CivSettings.civConfig.getDouble("civ.town_distance_upkeep_max", 500000.0);

        double distance = capitolTownHallLoc.distance(townHallLoc);
        double distanceUpkeep = town_distance_base_upkeep * (Math.pow(distance, touching ? distance_multiplier_touching : distance_multiplier_not_touching));

        return Math.round(min(distanceUpkeep, maxDistanceUpkeep));
    }

    public double getDistanceHappiness(Location capitolTownHallLoc, Location townHallLoc, boolean touching) {
        double town_distance_base_happy = CivSettings.happinessConfig.getDouble("happiness.distance_base", 0.01);
        double distance_multiplier_touching = CivSettings.happinessConfig.getDouble("happiness.distance_multiplier", 0.75);
        double distance_multiplier_not_touching = CivSettings.happinessConfig.getDouble("happiness.distance_multiplier_outside_culture", 1.05);
        double maxDistanceHappiness = CivSettings.happinessConfig.getDouble("happiness.distance_max", 15.0);
        double distance = capitolTownHallLoc.distance(townHallLoc);
        double distance_happy = town_distance_base_happy * (Math.pow(distance, touching ? distance_multiplier_touching : distance_multiplier_not_touching));

        return Math.round(min(distance_happy, maxDistanceHappiness));
    }

    public Location getCapitolTownHallLocation() {
        Town capitol = this.getTown(capitolName);
        if (capitol == null) {
            return null;
        }

        for (Structure struct : capitol.getStructures()) {
            if (struct instanceof Capitol) {
                return struct.getCorner().getLocation();
            }
        }

        return null;
    }

    public Capitol getCapitolStructure() {
        Town capitol = this.getTown(capitolName);
        if (capitol == null) {
            return null;
        }

        for (Structure struct : capitol.getStructures()) {
            if (struct instanceof Capitol) {
                return (Capitol) struct;
            }
        }

        return null;
    }

    public double payUpkeep() throws CivException {
        double upkeep = 0;
        this.lastUpkeepPaidMap.clear();

        if (this.isAdminCiv()) {
            return 0;
        }
        Town capitol = this.getTown(capitolName);
        if (capitol == null) {
            throw new CivException("Civilization found with no capitol!");
        }

        for (Town town : this.getTowns()) {
            /* Calculate upkeep from extra towns, obviously ignore the capitol itself. */
            if (!this.getCapitolName().equals(town.getName())) {
                /* Base upkeep per town. */
                upkeep += CivSettings.civConfig.getDouble("civ.town_upkeep", 500.0);
                lastUpkeepPaidMap.put(town.getName() + ",base", upkeep);

            }
        }

        upkeep += this.getWarUpkeep();

        if (this.getTreasury().hasEnough(upkeep)) {
            /* Have plenty on our coffers, pay the lot and clear all of these towns' debt. */
            this.getTreasury().withdraw(upkeep);
        } else {
            /* Doh! We don't have enough money to pay for our upkeep, go into debt. */
            double diff = upkeep - this.getTreasury().getBalance();
            this.getTreasury().setDebt(this.getTreasury().getDebt() + diff);
            this.getTreasury().withdraw(this.getTreasury().getBalance());
        }

        return upkeep;
    }


    public int getDaysInDebt() {
        return daysInDebt;
    }

    public void setDaysInDebt(int daysInDebt) {
        this.daysInDebt = daysInDebt;
    }

    public void warnDebt() {
        CivMessage.global(CivSettings.localize.localizedString("var_civ_debtAnnounce", this.getName(), this.getTreasury().getDebt(), CivSettings.CURRENCY_NAME));
    }


    public void incrementDaysInDebt() {
        daysInDebt++;

        if (daysInDebt >= CivSettings.CIV_DEBT_GRACE_DAYS) {
            if (daysInDebt >= CivSettings.CIV_DEBT_SELL_DAYS) {
                if (daysInDebt >= CivSettings.CIV_DEBT_TOWN_SELL_DAYS) {
                    CivMessage.global(CivSettings.localize.localizedString("var_civ_fellIntoRuin", this.getName()));
                    try {
                        this.delete();
                        return;
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // warn sell..
        CivMessage.global(CivSettings.localize.localizedString("var_civ_debtGlobalAnnounce", this.getName()) + " " + getDaysLeftWarning());
    }

    public String getDaysLeftWarning() {

        if (daysInDebt < CivSettings.CIV_DEBT_GRACE_DAYS) {
            return CivSettings.localize.localizedString("var_civ_daystillSaleAnnounce", (CivSettings.CIV_DEBT_GRACE_DAYS - daysInDebt));
        }

        if (daysInDebt < CivSettings.CIV_DEBT_SELL_DAYS) {
            return CivSettings.localize.localizedString("var_civ_isForSale1", this.getName(), (CivSettings.CIV_DEBT_SELL_DAYS - daysInDebt));

        }

        if (daysInDebt < CivSettings.CIV_DEBT_TOWN_SELL_DAYS) {
            return CivSettings.localize.localizedString("var_civ_isForSale2", this.getName(), (CivSettings.CIV_DEBT_TOWN_SELL_DAYS - daysInDebt));
        }

        return "";
    }

    private int pickCivColor() {
        int max_retries = 10;
        Random rand = new Random();
        boolean found = false;
        int c = 0;
        //Tries to get CivColor that are not the same.
        for (int i = 0; i < max_retries; i++) {
            c = rand.nextInt(HEX_COLOR_MAX); //Must clip at a 24 bit integer.
            if (!testColorForCloseness(c)) {
                continue; //reject this color, try again.
            }
            found = true;
            break;
        }

        //If we couldn't find a close color withing the max retries, pick any old color as a failsafe.
        if (!found) {
            c = rand.nextInt();
            CivLog.error(CivSettings.localize.localizedString("civ_colorExhaustion"));
        }

        return c;
    }

    private boolean testColorForCloseness(int c) {
        int tolerance = HEX_COLOR_TOLERANCE; //out of 255 CivColor, 40 is about a 15% difference.

        if (simpleColorDistance(c, 0xFF0000) < tolerance) {
            return false; //never accept pure red, or anything close to it, used for town markers.
        }

        if (simpleColorDistance(c, 0xFFFFFF) < tolerance) {
            return false; // not too bright.
        }

        if (simpleColorDistance(c, 0x000000) < tolerance) {
            return false; //not too dark/
        }

        //Check all the currently held CivColor.
        for (int c2 : CivGlobal.CivColorInUse.keySet()) {
            if (simpleColorDistance(c, c2) < tolerance) {
                return false; //if this color is too close to any other color, reject it.
            }
        }
        return true;
    }

    private int simpleColorDistance(int color1, int color2) {

        int red1 = color1 & 0xFF0000;
        int red2 = color2 & 0xFF0000;
        double redPower = Math.pow((red1 - red2), 2);

        int green1 = color1 & 0x00FF00;
        int green2 = color2 & 0x00FF00;
        double greenPower = Math.pow((green1 - green2), 2);

        int blue1 = color1 & 0x0000FF;
        int blue2 = color2 & 0x0000FF;
        double bluePower = Math.pow((blue1 - blue2), 2);

        return (int) Math.sqrt(redPower + greenPower + bluePower);
    }

    public String getCultureDescriptionString() {
        String out = "";

        out += "<b>" + this.getName() + "</b>";

        return out;
    }

    public double getBaseBeakers() {
        return this.baseBeakers;
    }

    public double getBeakers() {
        double total = 0;

        for (Town town : this.getTowns()) {
            total += town.getBeakers().total;
        }

        total += baseBeakers;

        return total;
    }

    public double getFaith() {
        double f = 0;
        for (Town t : getTowns()) {
            f += t.getFaith().total;
        }
        return f;
    }

    public void setBaseBeakers(double beakerRate) {
        this.baseBeakers = beakerRate;
    }

    public void addBeakers(double beakers) {
        if (beakers == 0) {
            return;
        }

        TaskMaster.asyncTask(new UpdateTechBar(this), 0);
        setResearchProgress(getResearchProgress() + beakers);

        if (getResearchProgress() >= getResearchTech().getAdjustedBeakerCost(this)) {
            CivMessage.sendCiv(this, CivSettings.localize.localizedString("var_civ_research_Discovery", getResearchTech().name()));
            this.addTech(this.getResearchTech());
            this.setResearchProgress(0);
            this.setResearchTech(null);

            return;
        }

        int percentageComplete = (int) ((getResearchProgress() / this.getResearchTech().getAdjustedBeakerCost(this)) * 100);
        if ((percentageComplete % 10) == 0) {

            if (percentageComplete != lastTechPercentage) {
                CivMessage.sendCiv(this, CivSettings.localize.localizedString("var_civ_research_currentProgress", getResearchTech().name(), percentageComplete));
                lastTechPercentage = percentageComplete;
                for (Player p : this.getOnlinePlayers()) {
                    CivMessage.sendActionBar(p, CivData.getStringForBar(CivData.TaskType.TECH, percentageComplete, 100));
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_BOTTLE_THROW, 1.25f, 1.25f);
                }
            }
        }
    }

    public void startTechnologyResearch(ConfigTech tech) throws CivException {
        if (this.getResearchTech() != null) {
            throw new CivException(CivSettings.localize.localizedString("var_civ_research_switchAlert1", this.getResearchTech().name()));
        }
        double cost = tech.getAdjustedTechCost(this);

        if (!this.getTreasury().hasEnough(cost)) {
            throw new CivException(CivSettings.localize.localizedString("var_civ_research_notEnoughMoney", cost, CivSettings.CURRENCY_NAME));
        }

        if (this.hasTech(tech.id())) {
            throw new CivException(CivSettings.localize.localizedString("civ_research_alreadyDone"));
        }

        if (!tech.isAvailable(this)) {
            throw new CivException(CivSettings.localize.localizedString("civ_research_missingRequirements"));
        }

        this.setResearchTech(tech);
        this.setResearchProgress(0.0);

        this.getTreasury().withdraw(cost);
        TaskMaster.asyncTask(new UpdateTechBar(this), 0);
    }

    public ConfigTech getResearchTech() {
        return researchTech;
    }

    public void setResearchTech(ConfigTech researchTech) {
        this.researchTech = researchTech;
    }

    public double getResearchProgress() {
        return researchProgress;
    }

    public void setResearchProgress(double researchProgress) {
        this.researchProgress = researchProgress;
    }

    public void changeGovernment(Civilization civ, ConfigGovernment gov, boolean force) throws CivException {
        if (civ.getGovernment() == gov && !force) {
            throw new CivException(CivSettings.localize.localizedString("var_civ_gov_already", gov.displayName));
        }

        if (civ.getGovernment().id.equals("gov_anarchy")) {
            throw new CivException(CivSettings.localize.localizedString("civ_gov_errorAnarchy"));
        }

        boolean noanarchy = this.hasWonder("w_notre_dame");

        if (!noanarchy) {
            String key = "changegov_" + this.getUUID();
            String value = gov.id;

            sessionAdd(key, value);

            // Set the town's government to anarchy in the meantime
            civ.setGovernment("gov_anarchy");
            CivMessage.global(CivSettings.localize.localizedString("var_civ_gov_anachyAlert", this.getName()));
        } else {
            civ.setGovernment(gov.id);
            CivMessage.global(CivSettings.localize.localizedString("var_civ_gov_success", civ.getName(), CivSettings.governments.get(gov.id).displayName));
        }
    }

    public String getUpkeepPaid(Town town, String type) {
        String out = "";

        if (lastUpkeepPaidMap.containsKey(town.getName() + "," + type)) {
            out += lastUpkeepPaidMap.get(town.getName() + "," + type);
        } else {
            out += "0";
        }

        return out;
    }

    public void taxPayment(Town town, double amount) {

        Double townPaid = this.lastTaxesPaidMap.get(town.getName());
        if (townPaid == null) {
            townPaid = amount;
        } else {
            townPaid += amount;
        }
        this.lastTaxesPaidMap.put(town.getName(), townPaid);
        double beakerAmount = amount * this.sciencePercentage;
        amount -= beakerAmount;
        this.getTreasury().deposit(amount);

        double coins_per_beaker = CivSettings.civConfig.getDouble("civ.coins_per_beaker", 10);

        for (Town t : this.getTowns()) {
            if (t.getBuffManager().hasBuff("buff_greatlibrary_double_tax_beakers")) {
                coins_per_beaker /= 2;
            }
        }

        DecimalFormat df = new DecimalFormat("#.#");
        double totalBeakers = Double.parseDouble(df.format(beakerAmount / coins_per_beaker));
        if (totalBeakers == 0) {
            return;
        }

        if (this.researchTech != null) {
            this.addBeakers(totalBeakers);
            return;
        }
        EndGameCondition scienceVictory = EndGameCondition.getEndCondition("end_science");
        if (scienceVictory == null) {
            CivLog.error("Couldn't find science victory, not configured?");
            return;
        }
        if (scienceVictory.isActive(this)) {
            /*
             * We've got an active science victory, lets add these beakers
             * to the total stored on "the enlightenment"
             */
            ((EndConditionScience) scienceVictory).addExtraBeakersToCiv(this, totalBeakers);
        } else {
            this.getTreasury().deposit(totalBeakers);
        }
    }

    public double getSciencePercentage() {
        return sciencePercentage;
    }

    public void setSciencePercentage(double sciencePercentage) {
        if (sciencePercentage > 1.0) {
            sciencePercentage = 1.0;
        }

        this.sciencePercentage = sciencePercentage;
    }

    public Collection<ConfigTech> getTechs() {
        return this.techs.values();
    }

    public void depositFromResident(Resident resident, Double amount) throws CivException {

        if (!resident.getTreasury().hasEnough(amount)) {
            throw new CivException(CivSettings.localize.localizedString("var_civ_deposit_NotEnough", CivSettings.CURRENCY_NAME));
        }

        if (this.getTreasury().inDebt()) {
            if (this.getTreasury().getDebt() >= amount) {
                this.getTreasury().setDebt(this.getTreasury().getDebt() - amount);
                resident.getTreasury().withdraw(amount);
            } else if (this.getTreasury().getDebt() < amount) {
                double leftAmount = amount - this.getTreasury().getDebt();
                this.getTreasury().setDebt(0);
                this.getTreasury().deposit(leftAmount);
                resident.getTreasury().withdraw(amount);
            }

            if (!this.getTreasury().inDebt()) {
                this.daysInDebt = 0;
                CivMessage.global(CivSettings.localize.localizedString("var_civ_deposit_cleardebt", this.getName()));
            }
        } else {
            this.getTreasury().deposit(amount);
            resident.getTreasury().withdraw(amount);
        }
    }

    public void sessionAdd(String key, String value) {
        CivGlobal.getSessionDB().add(key, value, this.getUUID(), NamedObject.NULL_UUID, NamedObject.NULL_UUID);
    }

    public void sessionDeleteAll(String key) {
        CivGlobal.getSessionDB().delete_all(key);
    }

    public void sessionUpdateInsert(String key, String value) {
        //CivGlobal.getSessionDB().updateInsert(key, value, this.getId(), 0, 0);
    }

    public DiplomacyManager getDiplomacyManager() {
        return diplomacyManager;
    }

    public void onDefeat(Civilization attackingCiv) {
        /*
         * The entire civilization has been defeated. We need to give our towns to the attacker.
         * Meanwhile, our civilization will become dormant. We will NOT remember who the attacker
         * was, if we revolt we will declare war on anyone who owns our remaining towns.
         *
         * We will hand over all of our native towns, as well as any conquered towns we might have.
         * Those towns when they revolt will revolt against whomever owns them.
         */

        for (Town town : this.getTowns()) {
            town.onDefeat(attackingCiv);
        }

        /* Remove any old relationships this civ may have had. */
        for (Relation relation : new LinkedList<>(this.getDiplomacyManager().getRelations())) {
            try {
                if (relation.getStatus() == Relation.Status.WAR) {
                    relation.setStatus(Relation.Status.NEUTRAL);
                }
                relation.delete();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }


        /* Remove ourselves from the main global civ list and into a special conquered list. */
        removeCiv(this);
        CivGlobal.addConqueredCiv(this);
        this.conquered = true;
        this.conquer_date = new Date();
    }

    public boolean isConquered() {
        return conquered;
    }


    public void regenControlBlocks() {
        for (Town t : getTowns()) {
            t.getTownHall().regenControlBlocks();
        }
    }

    public boolean isAdminCiv() {
        return adminCiv;
    }

    public void setAdminCiv(boolean bool) {
        adminCiv = bool;
        if (adminCiv) {
            CivGlobal.addAdminCiv(this);
        } else {
            CivGlobal.removeAdminCiv(this);
        }
    }

    public void repositionPlayers(String reason) {
        if (!this.getDiplomacyManager().isAtWar()) {
            return;
        }

        for (Town t : this.getTowns()) {
            TownHall townhall = t.getTownHall();
            if (townhall == null) {
                CivLog.error("Town hall was null for " + t.getName() + " when trying to reposition players.");
                continue;
            }

            for (Resident resident : t.getResidents()) {
                //if (townhall.isActive()) {
                BlockCoord revive = townhall.getRandomRevivePoint();

                try {
                    Player player = CivGlobal.getPlayer(resident);
                    ChunkCoord coord = new ChunkCoord(player.getLocation());
                    CultureChunk cc = CivGlobal.getCultureChunk(coord);
                    if (cc != null && cc.getCiv() != this &&
                            cc.getCiv().getDiplomacyManager().atWarWith(this)) {
                        CivMessage.send(player, ChatColor.LIGHT_PURPLE + reason);
                        player.teleport(revive.getLocation());
                    }


                } catch (CivException e) {
                    // player not online....
                }
            }
        }
    }

    public boolean isTownsForSale() {
        return daysInDebt >= CivSettings.CIV_DEBT_SELL_DAYS;
    }

    public boolean isForSale() {
        if (this.getTownCount() == 0) {
            return false;
        }

        return daysInDebt >= CivSettings.CIV_DEBT_GRACE_DAYS;
    }

    public double getTotalSalePrice() {
        double price = CivSettings.scoreConfig.coins_per_point * this.getTechScore();
        for (Town town : this.getTowns()) {
            price += town.getForSalePrice();
        }
        return price;
    }

    public void buyCiv(Civilization civ) throws CivException {
        if (!this.getTreasury().hasEnough(civ.getTotalSalePrice())) {
            throw new CivException(CivSettings.localize.localizedString("civ_buy_notEnough") + " " + CivSettings.CURRENCY_NAME);
        }

        this.getTreasury().withdraw(civ.getTotalSalePrice());
        this.mergeInCiv(civ);
    }

    public int getTechScore() {
        int points = 0;
        // Count technologies.
        for (ConfigTech tech : this.getTechs()) {
            points += tech.points();
        }
        return points;
    }

    public int getScore() {
        int points = 0;
        for (Town t : this.getTowns()) {
            points += t.getScore();
        }

        points += getTechScore();

        return points;
    }

    public boolean hasResident(@Nullable Resident resident) {
        if (resident == null) {
            return false;
        }

        for (Town t : this.getTowns()) {
            if (t.hasResident(resident)) {
                return true;
            }
        }

        return false;
    }

    public void removeTown(Town town) {
        this.towns.remove(town.getName().toLowerCase());
    }

    public void mergeInCiv(Civilization oldciv) {
        if (oldciv == this) {
            return;
        }

        /* Grab each town underneath and add it to us. */
        for (Town town : oldciv.getTowns()) {
            town.changeCiv(this);
            town.setDebt(0);
            town.setDaysInDebt(0);
        }

        if (!oldciv.towns.isEmpty()) {
            CivLog.error("CIV SOMEHOW STILL HAS TOWNS AFTER WE GAVE THEM ALL AWAY WTFWTFWTFWTF.");
            this.towns.clear();
        }

        try {
            oldciv.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        CivGlobal.processCulture();
    }

    public void buyTown(Town town) throws CivException {

        if (!this.getTreasury().hasEnough(town.getForSalePrice())) {
            throw new CivException(CivSettings.localize.localizedString("civ_buy_notEnough") + " " + CivSettings.CURRENCY_NAME);
        }

        this.getTreasury().withdraw(town.getForSalePrice());
        town.changeCiv(this);
        town.setMotherCiv(null);
        town.setDebt(0);
        town.setDaysInDebt(0);
        CivGlobal.processCulture();
        CivMessage.global(CivSettings.localize.localizedString("var_civ_buyTown_Success1", this.getName(), this.getName()));

    }

    public double getRevolutionFee() {
        double base_coins = CivSettings.warConfig.getDouble("revolution.base_cost", 50000.0);
        double coins_per_town = CivSettings.warConfig.getDouble("revolution.coins_per_town", 10000.0);
        double coins_per_point = CivSettings.warConfig.getDouble("revolution.coins_per_point", 0.1);
        double max_fee = CivSettings.warConfig.getDouble("revolution.maximum_fee", 1000000.0);

        double total_coins = base_coins;

        double motherCivPoints = this.getTechScore();
        for (Town town : Town.getTowns()) {
            if (town.getMotherCiv() == this) {
                motherCivPoints += town.getScore();
                total_coins += coins_per_town;
            }
        }

        total_coins += motherCivPoints * coins_per_point;

        if (total_coins > max_fee) {
            total_coins = max_fee;
        }

        return total_coins;

    }

    public void setConquered(boolean b) {
        this.conquered = b;
    }

    public Collection<Resident> getOnlineResidents() {

        LinkedList<Resident> residents = new LinkedList<>();
        for (Town t : this.getTowns()) {
            residents.addAll(t.getOnlineResidents());
        }

        return residents;
    }

    public Date getConqueredDate() {
        return this.conquer_date;
    }

    public void capitulate() {
        for (Town town : Town.getTowns()) {
            if (town.getMotherCiv() == this) {
                town.setMotherCiv(null);
            }
        }

        try {
            this.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        CivMessage.global(CivSettings.localize.localizedString("var_civ_capitulate_Success1", this.getName()));
    }

    /*
     * populates the sources with happiness sources.
     */
    public double getCivWideUnhappiness(HashMap<String, Double> sources) {
        double total = 0;

        /* Get happiness per town. */
        double per_town = CivSettings.happinessConfig.getDouble("happiness.per_town", 0.75);
        double per_captured_town = CivSettings.happinessConfig.getDouble("happiness.per_captured_town", 1.66);

        double happy_town = 0;
        double happy_captured_town = 0;
        for (Town town : this.getTowns()) {
            if (town.getMotherCiv() == null) {
                if (!town.isCapitol()) {
                    happy_town += per_town;
                }
            } else {
                happy_captured_town += per_captured_town;
            }
        }

        total += happy_town;
        sources.put("Towns", happy_town);

        total += happy_captured_town;
        sources.put("Captured Towns", happy_captured_town);

        /* Get unhappiness from wars. */
        double war_happy = this.getWarUnhappiness();
        total += war_happy;
        sources.put("War", war_happy);

        return total;
    }

    /*
     * Gets distance happiness for a town.
     */
    public double getDistanceHappiness(Town town) {
        Structure capitolTownHall = this.getCapitolStructure();
        Structure townHall = town.getTownHall();
        if (capitolTownHall == null || townHall == null) {
            return 0.0;
        }
        Location loc_cap = capitolTownHall.getCorner().getLocation();
        Location loc_town = townHall.getCorner().getLocation();
        if (town.getMotherCiv() != null && town.getMotherCiv() != this) {
            return 0;
        }

        return this.getDistanceHappiness(loc_cap, loc_town, town.touchesCapitolCulture(new HashSet<>()));
    }

    public void declareAsWinner(EndGameCondition end) {
        String out = CivSettings.localize.localizedString("var_civ_victory_end1", this.getName(), end.getVictoryName());
        CivGlobal.getSessionDB().add("endgame:winningCiv", out, NamedObject.NULL_UUID, NamedObject.NULL_UUID, NamedObject.NULL_UUID);
        CivMessage.global(out);
    }

    public void winConditionWarning(EndGameCondition end, int daysLeft) {
        CivMessage.global(CivSettings.localize.localizedString("var_civ_victory_end2", this.getName(), end.getVictoryName(), daysLeft));
    }

    public double getPercentageConquered() {

        int totalCivs = getCivs().size() + CivGlobal.getConqueredCivs().size();
        int conqueredCivs = 1; /* Your civ already counts */

        for (Civilization civ : CivGlobal.getConqueredCivs()) {
            Town capitol = Town.getTown(civ.getCapitolName());
            if (capitol == null) {
                /* Invalid civ? */
                totalCivs--;
                continue;
            }

            if (capitol.getCiv() == this) {
                conqueredCivs++;
            }
        }

        return (double) conqueredCivs / (double) totalCivs;
    }

    public void processUnusedBeakers() {

        EndGameCondition scienceVictory = EndGameCondition.getEndCondition("end_science");
        if (scienceVictory == null) {
            CivLog.error("Couldn't find science victory, not configured?");
        } else {
            if (scienceVictory.isActive(this)) {
                /*
                 * We've got an active science victory, lets add these beakers
                 * to the total stored on "the enlightenment"
                 */
                double beakerTotal = this.getBeakers() / BeakerTimer.BEAKER_PERIOD;
                ((EndConditionScience) scienceVictory).addExtraBeakersToCiv(this, beakerTotal);
                return;
            }
        }

        for (Town town : this.towns.values()) {
            town.addUnusedBeakers(town.getBeakers().total / BeakerTimer.BEAKER_PERIOD);
        }
    }

    public boolean areLeadersInactive() {
        int leader_inactive_days = CivSettings.civConfig.getInt("civ.leader_inactive_days", 7);

        for (Resident resident : this.getLeaderGroup().getMemberList()) {
            if (resident.isInactiveForDays(leader_inactive_days)) {
                return false;
            }
        }

        return true;
    }

    public void rename(String name) throws CivException, InvalidNameException {

        Civilization other = getByName(name);
        if (other != null) {
            throw new CivException(CivSettings.localize.localizedString("civ_rename_errorExists"));
        }

        other = CivGlobal.getConqueredCiv(name);
        if (other != null) {
            throw new CivException(CivSettings.localize.localizedString("civ_rename_errorExists"));
        }

        if (this.conquered) {
            CivGlobal.removeConqueredCiv(this);
        } else {
            removeCiv(this);
        }

        String oldName = this.getName();
        this.setName(name);

        if (this.conquered) {
            CivGlobal.addConqueredCiv(this);
        } else {
            addCiv(this);
        }

        CivMessage.global(CivSettings.localize.localizedString("var_civ_rename_success1", oldName, this.getName()));
    }

    public ArrayList<RespawnLocationHolder> getAvailableRespawnables() {
        ArrayList<RespawnLocationHolder> respawns = new ArrayList<>();

        for (Town town : this.getTowns()) {
            TownHall townhall = town.getTownHall();
            if (townhall != null && townhall.isActive()) {
                if (!townhall.getTown().isCapitol() && town.defeated) {
                    /* Do not respawn at defeated towns. */
                    continue;
                }

                respawns.add(townhall);
            }
        }

        respawns.addAll(this.warCamps);

        return respawns;

    }

    public void addWarCamp(WarCamp camp) {
        this.warCamps.add(camp);
    }

    public LinkedList<WarCamp> getWarCamps() {
        return this.warCamps;
    }

    public void onWarEnd() {
        for (WarCamp camp : this.warCamps) {
            camp.onWarEnd();
        }

        for (Town town : towns.values()) {
            TownHall th = town.getTownHall();
            if (th != null) {
                th.setHitpoints(th.getMaxHitPoints());
                th.save();
            }
        }
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
            throw new CivException(CivSettings.localize.localizedString("var_civ_gift_tooyoung1", this.getName(), min_gift_age));
        }
    }

    public void clearAggressiveWars() {
        /* If this civ is the aggressor in any wars. Cancel the(?), this happens when civs go into debt. */
        LinkedList<Relation> removeUs = new LinkedList<>();
        for (Relation relation : this.getDiplomacyManager().getRelations()) {
            if (relation.getStatus().equals(Relation.Status.WAR)) {
                if (relation.getAggressor() == this) {
                    removeUs.add(relation);
                }
            }
        }

        for (Relation relation : removeUs) {
            this.getDiplomacyManager().deleteRelation(relation);
            CivMessage.global(CivSettings.localize.localizedString("var_civ_debt_endWar", this.getName(), relation.getOtherCiv().getName()));
        }

    }

    public int getMergeCost() {
        int total = 0;
        for (Town town : this.towns.values()) {
            total += town.getGiftCost();
        }

        return total;
    }

    public Structure getNearestStructureInTowns(Location loc) {
        Structure nearest = null;
        double lowest_distance = Double.MAX_VALUE;

        for (Town town : towns.values()) {
            for (Structure struct : town.getStructures()) {
                double distance = struct.getCenterLocation().getLocation().distance(loc);
                if (distance < lowest_distance) {
                    lowest_distance = distance;
                    nearest = struct;
                }
            }
        }

        return nearest;
    }

    public ItemStack getRandomLeaderSkull(String message) {
        Random rand = new Random();
        int i = rand.nextInt(this.getLeaderGroup().getMemberCount());
        int count = 0;
        Resident resident = CivGlobal.getResident(this.getLeader());

        for (Resident res : this.getLeaderGroup().getMemberList()) {
            if (count == i) {
                resident = res;
                break;
            }
        }

        String leader = "";
        if (resident != null) {
            leader = resident.getName();
        }

        return ItemManager.spawnPlayerHead(leader, message + " (" + leader + ")");
    }

    public int getCurrentEra() {
        return currentEra;
    }

    public void setCurrentEra(int currentEra) {
        this.currentEra = currentEra;

        if (this.currentEra > CivGlobal.highestCivEra && !this.isAdminCiv()) {
            CivGlobal.setCurrentEra(this.currentEra, this);
        }
    }

    public ArrayList<Player> getOnlinePlayers() {
        ArrayList<Player> online = new ArrayList<>();
        for (Town t : getTowns()) {
            online.addAll(t.getOnlinePlayers());
        }
        return online;
    }

    public ConfigReligion getReligion() {
        return civReligion;
    }

    public void setCivReligion(ConfigReligion cr) {
        this.civReligion = cr;
    }

    public boolean hasWonder(String id) {
        for (Town t : this.getTowns()) {
            for (Wonder w : t.getWonders()) {
                if (w.getConfigId().equalsIgnoreCase(id)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Wonder getWonder(String id) {
        for (Town t : this.getTowns()) {
            for (Wonder w : t.getWonders()) {
                if (w.getConfigId().equalsIgnoreCase(id)) {
                    return w;
                }
            }
        }
        return null;
    }

    public boolean hasEnlightenment() {
        return this.hasTechnology("tech_enlightenment");
    }

    public int getTownIndex(Town t) {
        int i = 0;
        if (t.isCapitol()) {
            return 0;
        }
        for (Town town : this.getTowns()) {
            if (town.equals(t)) {
                return i;
            } else {
                i++;
            }
        }
        return i;
    }

    public Town getTownByIndex(int index) {
        int i = 0;
        for (Town t : this.getTowns()) {
            if (i == index) {
                return t;
            } else {
                i++;
            }
        }
        return null;
    }

    public Town getCapitol() {
        return capitol;
    }

    public Capitol getKapa() {
        return (Capitol) capitol.getStructureByType("s_capitol");
    }
    // получаем саму капу для работы с ней
}
