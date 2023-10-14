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
package com.avrgaming.civcraft.main;

import com.avrgaming.civcraft.arena.ArenaTeam;
import com.avrgaming.civcraft.camp.Camp;
import com.avrgaming.civcraft.camp.CampBlock;
import com.avrgaming.civcraft.camp.WarCamp;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.database.SQLController;
import com.avrgaming.civcraft.endgame.EndGameCondition;
import com.avrgaming.civcraft.event.EventTimer;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.object.*;
import com.avrgaming.civcraft.object.Relation.Status;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.populators.TradeGoodPreGenerate;
import com.avrgaming.civcraft.questions.QuestionBaseTask;
import com.avrgaming.civcraft.questions.QuestionResponseInterface;
import com.avrgaming.civcraft.randomevents.RandomEvent;
import com.avrgaming.civcraft.sessiondb.SessionDatabase;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.*;
import com.avrgaming.civcraft.structure.farm.FarmChunk;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.*;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.BukkitObjects;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.ItemFrameStorage;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.civcraft.war.WarRegen;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class CivGlobal {
    private static boolean useEconomy;
    public static Economy econ;

    private static final Map<String, QuestionBaseTask> questions = new ConcurrentHashMap<>();
    public static Map<String, CivQuestionTask> civQuestions = new ConcurrentHashMap<>();
    private static final Map<String, Resident> residents = new ConcurrentHashMap<>();
    private static final Map<UUID, Resident> residentsViaUUID = new ConcurrentHashMap<>();

    private static final Map<String, Town> towns = new ConcurrentHashMap<>();
    private static final Map<String, Civilization> civs = new ConcurrentHashMap<>();
    private static final Map<String, Civilization> conqueredCivs = new ConcurrentHashMap<>();
    private static final Map<String, Civilization> adminCivs = new ConcurrentHashMap<>();
    private static final Map<ChunkCoord, TownChunk> townChunks = new ConcurrentHashMap<>();
    private static final Map<ChunkCoord, CultureChunk> cultureChunks = new ConcurrentHashMap<>();
    private static final Map<ChunkCoord, Boolean> persistChunks = new ConcurrentHashMap<>();
    private static final Map<BlockCoord, Structure> structures = new ConcurrentHashMap<>();
    private static final Map<BlockCoord, Wonder> wonders = new ConcurrentHashMap<>();
    private static final Map<BlockCoord, StructureBlock> structureBlocks = new ConcurrentHashMap<>();
    //private static Map<BlockCoord, LinkedList<StructureBlock>> structureBlocksIn2D = new ConcurrentHashMap<BlockCoord, LinkedList<StructureBlock>>();
    private static final Map<String, HashSet<Buildable>> buildablesInChunk = new ConcurrentHashMap<>();
    private static final Map<BlockCoord, CampBlock> campBlocks = new ConcurrentHashMap<>();
    private static final Map<BlockCoord, StructureSign> structureSigns = new ConcurrentHashMap<>();
    private static final Map<BlockCoord, StructureChest> structureChests = new ConcurrentHashMap<>();
    private static final Map<BlockCoord, TradeGood> tradeGoods = new ConcurrentHashMap<>();
    private static final Map<BlockCoord, ProtectedBlock> protectedBlocks = new ConcurrentHashMap<>();
    private static final Map<ChunkCoord, FarmChunk> farmChunks = new ConcurrentHashMap<>();
    private static final Queue<FarmChunk> farmChunkUpdateQueue = new LinkedList<>();
    private static final Queue<FarmChunk> farmGrowQueue = new LinkedList<>();
    private static final Map<UUID, ItemFrameStorage> protectedItemFrames = new ConcurrentHashMap<>();
    private static final Map<BlockCoord, BonusGoodie> bonusGoodies = new ConcurrentHashMap<>();
    //    private static final Map<ChunkCoord, HashSet<Wall>> wallChunks = new ConcurrentHashMap<>();
//    private static final Map<BlockCoord, RoadBlock> roadBlocks = new ConcurrentHashMap<>();
    private static final Map<BlockCoord, CustomMapMarker> customMapMarkers = new ConcurrentHashMap<>();
    private static final Map<String, Camp> camps = new ConcurrentHashMap<>();
    private static final Map<ChunkCoord, Camp> campChunks = new ConcurrentHashMap<>();
    public static HashSet<BlockCoord> vanillaGrowthLocations = new HashSet<>();
    private static final Map<BlockCoord, Market> markets = new ConcurrentHashMap<>();
    public static HashSet<String> researchedTechs = new HashSet<>();
    public static Map<Integer, Boolean> CivColorInUse = new ConcurrentHashMap<>();
    public static TradeGoodPreGenerate tradeGoodPreGenerator = new TradeGoodPreGenerate();

    //TODO fix the duplicate score issue...
    public static TreeMap<Integer, Civilization> civilizationScores = new TreeMap<>();
    public static TreeMap<Integer, Town> townScores = new TreeMap<>();

    public static HashMap<String, Date> playerFirstLoginMap = new HashMap<>();
    public static HashSet<String> banWords = new HashSet<>();

    //TODO convert this to completely static?
    private static SessionDatabase sdb;

    public static boolean trommelsEnabled = true;
    public static boolean quarriesEnabled = true;
    public static boolean fisheryEnabled = true;
    public static boolean mobGrinderEnabled = true;
    public static boolean towersEnabled = true;
    public static boolean growthEnabled = true;
    public static Boolean banWordsAlways = false;
    public static boolean banWordsActive = false;
    public static boolean scoringEnabled = true;
    public static boolean warningsEnabled = true;
    public static boolean tradeEnabled = true;
    public static boolean loadCompleted = false;
    public static boolean speedChunks = false;
    public static int minBuildHeight = 1;

    public static ArrayList<Town> orphanTowns = new ArrayList<>();
    public static ArrayList<Civilization> orphanCivs = new ArrayList<>();

    public static boolean checkForBooks = true;
    public static boolean debugDateBypass = false;

    public static int highestCivEra = 0;

    public static void loadGlobals() throws SQLException {

		/*
		 Don't use CivSettings.getBoolean() to prevent error when using old config
		 Must be loaded before residents are loaded
		  */
        useEconomy = CivSettings.civConfig.getBoolean("global.use_vault");

        CivLog.heading("Loading CivCraft Objects From Database");

        sdb = new SessionDatabase();
        loadCamps();
        loadCivs();
        loadRelations();
        loadTowns();
        loadResidents();
        loadPermissionGroups();
        loadTownChunks();
        loadStructures();
        loadTradeGoods();
        loadTradeGoodies();
        loadRandomEvents();
        loadProtectedBlocks();
        loadTeams();
        EventTimer.loadGlobalEvents();
        EndGameCondition.init();
        War.init();

        CivLog.heading("--- Done <3 ---");

        /* Load in upgrades after all of our objects are loaded, resolves dependencies */
        processUpgrades();
        processCulture();

        /* Finish with an onLoad event. */
        onLoadTask postBuildSyncTask = new onLoadTask();
        TaskMaster.syncTask(postBuildSyncTask);

        /* Check for orphan civs now */
        for (Civilization civ : civs.values()) {
            Town capitol = civ.getTown(civ.getCapitolName());

            if (capitol == null) {
                orphanCivs.add(civ);
            }

        }
	/*	ScoreboardManager manager = Bukkit.getScoreboardManager(); // TODO ?!?
		CivGlobal.globalBoard = manager.getNewScoreboard();
		Team team = globalBoard.registerNewTeam("everybody");
		team.setPrefix(":PREFIX:");
		team.setSuffix(":SUFFIX:");
		team.setDisplayName("EveryBody");
		team.setCanSeeFriendlyInvisibles(false);
		team.setAllowFriendlyFire(false);
		
		globalBoard.registerNewObjective("showciv", "dummy");
		Objective objective = globalBoard.getObjective("showciv");
		objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
		objective.setDisplayName("OBJECTIVE NAME HERE");
		
		globalBoard.registerNewObjective("showciv2", "dummy");
		Objective objective2 = globalBoard.getObjective("showciv2");
		objective2.setDisplaySlot(DisplaySlot.BELOW_NAME);
		objective2.setDisplayName("OBJECTIVE2 NAME HERE");*/

        checkForInvalidStructures();

        minBuildHeight = CivSettings.civConfig.getInt("global.min_build_height", 1);
        speedChunks = CivSettings.civConfig.getBoolean("global.speed_check_chunks", false);

        loadCompleted = true;
    }

    public static void checkForInvalidStructures() {
        Iterator<Entry<BlockCoord, Structure>> iter = CivGlobal.getStructureIterator();
        while (iter.hasNext()) {
            Structure struct = iter.next().getValue();
            if (struct instanceof Capitol) {
                if (struct.getTown().getMotherCiv() == null) {
                    if (!struct.getTown().isCapitol()) {
                        struct.markInvalid();
                        struct.setInvalidReason("Capitol structures can only exist in the civilization's capitol. Use '/build town hall' to build a town-hall instead.");
                    }
                }
            }
        }
    }

    private static void loadTradeGoods() {

    }

    private static void loadTeams() throws SQLException {
        ResultSet rs = null;
        PreparedStatement ps = null;

        try {
            ps = SQLController.getGameConnection().prepareStatement("SELECT * FROM " + SQLController.tb_prefix + ArenaTeam.TABLE_NAME);
            rs = ps.executeQuery();

            while (rs.next()) {
                try {
                    new ArenaTeam(rs);
                } catch (InvalidNameException e) {
                    e.printStackTrace();
                }
            }

            Collections.sort(ArenaTeam.teamRankings);
            Collections.reverse(ArenaTeam.teamRankings); //Lazy method.

            CivLog.info("Loaded " + ArenaTeam.arenaTeams.size() + " Arena Teams");
        } finally {
            SQLController.close(rs, ps);
        }
    }

    private static void loadTradeGoodies() throws SQLException {
        ResultSet rs = null;
        PreparedStatement ps = null;

        try {
            ps = SQLController.getGameConnection().prepareStatement("SELECT * FROM " + SQLController.tb_prefix + TradeGood.TABLE_NAME);
            rs = ps.executeQuery();

            while (rs.next()) {
                TradeGood good;
                try {
                    good = new TradeGood(rs);
                    tradeGoods.put(good.getCoord(), good);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            CivLog.info("Loaded " + tradeGoods.size() + " Trade Goods");
        } finally {
            SQLController.close(rs, ps);
        }
    }

    private static void processUpgrades() {
        for (Town town : towns.values()) {
            try {
                town.loadUpgrades();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String localizedEraString(int era) {
        String newEra = "";
        switch (era) {
            case 0 -> //ANCIENT
                    newEra = "announce_ancientEra";
            case 1 -> //CLASSICAL
                    newEra = "announce_classicalEra";
            case 2 -> //MEDIEVAL
                    newEra = "announce_medievalEra";
            case 3 -> //RENAISSANCE
                    newEra = "announce_renaissanceEra";
            case 4 -> //INDUSTRIAL
                    newEra = "announce_industrialEra";
            case 5 -> //MODERN
                    newEra = "announce_modernEra";
            case 6 -> //ATOMIC
                    newEra = "announce_atomicEra";
            case 7 -> //INFORMATION
                    newEra = "announce_informationEra";
            default -> {
            }
        }
        return CivSettings.localize.localizedString(newEra);
    }

    public static void setCurrentEra(int era, Civilization civ) {
        if (era > highestCivEra && !civ.isAdminCiv()) {
            highestCivEra = era;
            CivMessage.globalTitle(ChatColor.DARK_GREEN + localizedEraString(highestCivEra), ChatColor.GREEN + CivSettings.localize.localizedString("var_announce_newEraCiv", civ.getName()));

        }
    }

    private static void loadCivs() throws SQLException {
        ResultSet rs = null;
        PreparedStatement ps = null;

        try {
            ps = SQLController.getGameConnection().prepareStatement("SELECT * FROM " + SQLController.tb_prefix + Civilization.TABLE_NAME);
            rs = ps.executeQuery();
            int count = 0;

            while (rs.next()) {
                try {
                    Civilization civ = new Civilization(rs);

                    if (highestCivEra < civ.getCurrentEra() && !civ.isAdminCiv()) {
                        highestCivEra = civ.getCurrentEra();
                    }

                    if (!civ.isConquered()) {
                        CivGlobal.addCiv(civ);
                    } else {
                        CivGlobal.addConqueredCiv(civ);
                    }
                    count++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            CivLog.info("Loaded " + count + " Civs");
        } finally {
            SQLController.close(rs, ps);
        }

    }

    private static void loadRelations() throws SQLException {
        ResultSet rs = null;
        PreparedStatement ps = null;

        try {
            ps = SQLController.getGameConnection().prepareStatement("SELECT * FROM " + SQLController.tb_prefix + Relation.TABLE_NAME);
            rs = ps.executeQuery();
            int count = 0;

            while (rs.next()) {
                try {
                    new Relation(rs);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                count++;
            }

            CivLog.info("Loaded " + count + " Relations");
        } finally {
            SQLController.close(rs, ps);
        }
    }

    public static void loadPermissionGroups() throws SQLException {
        ResultSet rs = null;
        PreparedStatement ps = null;

        try {
            ps = SQLController.getGameConnection().prepareStatement("SELECT * FROM " + SQLController.tb_prefix + PermissionGroup.TABLE_NAME);
            rs = ps.executeQuery();
            int count = 0;

            while (rs.next()) {
                try {
                    new PermissionGroup(rs);
                    count++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            CivLog.info("Loaded " + count + " PermissionGroups");
        } finally {
            SQLController.close(rs, ps);
        }
    }

    public static void loadResidents() throws SQLException {
        ResultSet rs = null;
        PreparedStatement ps = null;

        try {
            ps = SQLController.getGameConnection().prepareStatement("SELECT * FROM " + SQLController.tb_prefix + Resident.TABLE_NAME);
            rs = ps.executeQuery();

            while (rs.next()) {
                Resident res;
                try {
                    res = new Resident(rs);
                    CivGlobal.addResident(res);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            CivLog.info("Loaded " + residents.size() + " Residents");
        } finally {
            SQLController.close(rs, ps);
        }
    }

    public static void loadTowns() throws SQLException {
        ResultSet rs = null;
        PreparedStatement ps = null;

        try {
            ps = SQLController.getGameConnection().prepareStatement("SELECT * FROM " + SQLController.tb_prefix + Town.TABLE_NAME);
            rs = ps.executeQuery();

            while (rs.next()) {
                try {
                    Town town = new Town(rs);
                    towns.put(town.getName().toLowerCase(), town);
                    WarRegen.restoreBlocksFor(town.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            WarRegen.restoreBlocksFor(WarCamp.RESTORE_NAME);
            CivLog.info("Loaded " + towns.size() + " Towns");
        } finally {
            SQLController.close(rs, ps);
        }
    }

    public static void loadCamps() throws SQLException {
        ResultSet rs = null;
        PreparedStatement ps = null;

        try {
            ps = SQLController.getGameConnection().prepareStatement("SELECT * FROM " + SQLController.tb_prefix + Camp.TABLE_NAME);
            rs = ps.executeQuery();

            while (rs.next()) {
                try {
                    Camp camp = new Camp(rs);
                    CivGlobal.addCamp(camp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            SQLController.close(rs, ps);
        }

        CivLog.info("Loaded " + camps.size() + " Camps");
    }

    public static void loadTownChunks() throws SQLException {
        ResultSet rs = null;
        PreparedStatement ps = null;

        try {
            ps = SQLController.getGameConnection().prepareStatement("SELECT * FROM " + SQLController.tb_prefix + TownChunk.TABLE_NAME);
            rs = ps.executeQuery();

            while (rs.next()) {
                TownChunk tc;
                try {
                    tc = new TownChunk(rs);
                    townChunks.put(tc.getChunkCoord(), tc);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            CivLog.info("Loaded " + townChunks.size() + " TownChunks");
        } finally {
            SQLController.close(rs, ps);
        }
    }

    public static void loadStructures() throws SQLException {
        ResultSet rs = null;
        PreparedStatement ps = null;

        try {
            ps = SQLController.getGameConnection().prepareStatement("SELECT * FROM " + SQLController.tb_prefix + Structure.TABLE_NAME);
            rs = ps.executeQuery();

            while (rs.next()) {
                try {
                    MetaStructure struct = MetaStructure.newStructOrWonder(rs);
                    if (struct instanceof Wonder) {
                        wonders.put(struct.getCorner(), (Wonder) struct);
                    } else {
                        structures.put(struct.getCorner(), (Structure) struct);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            CivLog.info("Loaded " + structures.size() + " Structures");
            CivLog.info("Loaded " + wonders.size() + " Wonders");
        } finally {
            SQLController.close(rs, ps);
        }
    }

    public static void loadRandomEvents() throws SQLException {
        ResultSet rs = null;
        PreparedStatement ps = null;

        try {
            ps = SQLController.getGameConnection().prepareStatement("SELECT * FROM " + SQLController.tb_prefix + RandomEvent.TABLE_NAME);
            rs = ps.executeQuery();

            int count = 0;
            while (rs.next()) {
                try {
                    new RandomEvent(rs);
                    count++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            CivLog.info("Loaded " + count + " Active Random Events");
        } finally {
            SQLController.close(rs, ps);
        }
    }

    public static void loadProtectedBlocks() throws SQLException {
        ResultSet rs = null;
        PreparedStatement ps = null;

        try {
            ps = SQLController.getGameConnection().prepareStatement("SELECT * FROM " + SQLController.tb_prefix + ProtectedBlock.TABLE_NAME);
            rs = ps.executeQuery();

            int count = 0;
            while (rs.next()) {
                try {
                    ProtectedBlock pb = new ProtectedBlock(rs);
                    CivGlobal.addProtectedBlock(pb);
                    count++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            CivLog.info("Loaded " + count + " Protected Blocks");
        } finally {
            SQLController.close(rs, ps);
        }
    }
    public static Player getPlayer(Resident resident) throws CivException {
        Player player = Bukkit.getPlayer(resident.getUUID());
        if (player == null)
            throw new CivException("No player named" + " " + resident.getName());
        return player;
    }

    //TODO make lookup via ID faster(use hashtable)
    public static Resident getResidentFromId(int id) {
        for (Resident resident : residents.values()) {
            if (resident.getId() == id) {
                return resident;
            }
        }
        return null;
    }

    public static Resident getResident(Player player) {
        return residents.get(player.getName());
    }

    public static Resident getResident(Resident resident) {
        return residents.get(resident.getName());
    }

    public static boolean hasResident(String name) {
        return residents.containsKey(name);
    }

    public static void addResident(Resident res) {
        residents.put(res.getName(), res);
        residentsViaUUID.put(res.getUUID(), res);
    }

    public static void removeResident(Resident res) {
        residents.remove(res.getName());
        residentsViaUUID.remove(res.getUUID());
    }

    public static Resident getResident(String name) {
        return residents.get(name);
    }

    public static Resident getResidentViaUUID(UUID uuid) {
        return residentsViaUUID.get(uuid);
    }

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
	
	public static void addTown(Town town) {
		towns.put(town.getName().toLowerCase(), town);
	}

    public static TownChunk getTownChunk(Location location) {
        ChunkCoord coord = new ChunkCoord(location);
        return townChunks.get(coord);
    }

    public static void addTownChunk(TownChunk tc) {
        townChunks.put(tc.getChunkCoord(), tc);
    }

    public static void addCiv(Civilization civ) {
        civs.put(civ.getName().toLowerCase(), civ);
        if (civ.isAdminCiv()) {
            addAdminCiv(civ);
        }
    }

    public static Civilization getCiv(String name) {
        return civs.get(name.toLowerCase());
    }

    public static PermissionGroup getPermissionGroup(Town town, Integer id) {
        return town.getGroupFromId(id);
    }

    //TODO make lookup via ID faster(use hashtable)

    public static TownChunk getTownChunk(ChunkCoord coord) {
        return townChunks.get(coord);
    }

    public static PermissionGroup getPermissionGroupFromName(Town town, String name) {
        for (PermissionGroup grp : town.getGroups()) {
            if (grp.getName().equalsIgnoreCase(name)) {
                return grp;
            }
        }
        return null;
    }

    public static void questionPlayer(Player fromPlayer, Player toPlayer, String question, long timeout,
                                      QuestionResponseInterface finishedFunction) throws CivException {

        PlayerQuestionTask task = (PlayerQuestionTask) questions.get(toPlayer.getName());
        if (task != null) {
            /* Player already has a question pending. Lets deny this question until it times out
             * this will allow questions to come in on a pseduo 'first come first serve' and
             * prevents question spamming.
             */
            throw new CivException(CivSettings.localize.localizedString("civGlobal_hasPendingRequest"));
        }

        task = new PlayerQuestionTask(toPlayer, fromPlayer, question, timeout, finishedFunction);
        questions.put(toPlayer.getName(), task);
        TaskMaster.asyncTask("", task, 0);
    }

    public static void questionLeaders(Player fromPlayer, Civilization toCiv, String question, long timeout,
                                       QuestionResponseInterface finishedFunction) throws CivException {

        CivLeaderQuestionTask task = (CivLeaderQuestionTask) questions.get("civ:" + toCiv.getName());
        if (task != null) {
            /* Player already has a question pending. Lets deny this question until it times out
             * this will allow questions to come in on a pseduo 'first come first serve' and
             * prevents question spamming.
             */
            throw new CivException(CivSettings.localize.localizedString("civGlobal_civHasPendingRequest"));
        }

        task = new CivLeaderQuestionTask(toCiv, fromPlayer, question, timeout, finishedFunction);
        questions.put("civ:" + toCiv.getName(), task);
        TaskMaster.asyncTask("", task, 0);
    }

    public static QuestionBaseTask getQuestionTask(String string) {
        return questions.get(string);
    }

    public static void removeQuestion(String name) {
        questions.remove(name);
    }

    public static Collection<Town> getTowns() {
        return towns.values();
    }

    public static Collection<Resident> getResidents() {
        return residents.values();
    }

    public static Civilization getCivFromId(int id) {
        for (Civilization civ : civs.values()) {
            if (civ.getId() == id) {
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
            removeAdminCiv(civilization);
        }
    }

    public static void removeTown(Town town) {
        towns.remove(town.getName().toLowerCase());
    }

    public static Collection<PermissionGroup> getGroups() {
        ArrayList<PermissionGroup> groups = new ArrayList<>();

        for (Town t : towns.values()) {
            for (PermissionGroup grp : t.getGroups()) {
                if (grp != null) {
                    groups.add(grp);
                }
            }
        }

        for (Civilization civ : civs.values()) {
            if (civ.getLeaderGroup() != null) {
                groups.add(civ.getLeaderGroup());
            }
            if (civ.getAdviserGroup() != null) {
                groups.add(civ.getAdviserGroup());
            }
        }

        return groups;
    }

    public static Player getPlayer(String name) throws CivException {
        Resident res = CivGlobal.getResident(name);
        if (res == null)
            throw new CivException(CivSettings.localize.localizedString("var_civGlobal_noResident", name));
        Player player = Bukkit.getPlayer(res.getUUID());
        if (player == null)
            throw new CivException(CivSettings.localize.localizedString("var_civGlobal_noPlayer", name));
        return player;
    }

    public static void addCultureChunk(CultureChunk cc) {
        cultureChunks.put(cc.getChunkCoord(), cc);
    }

    public static CultureChunk getCultureChunk(ChunkCoord coord) {
        return cultureChunks.get(coord);
    }

    public static void removeCultureChunk(CultureChunk cc) {
        cultureChunks.remove(cc.getChunkCoord());
    }

    public static CultureChunk getCultureChunk(Location location) {
        ChunkCoord coord = new ChunkCoord(location);
        return getCultureChunk(coord);
    }

    public static void processCulture() {
        TaskMaster.asyncTask("culture-process", new CultureProcessAsyncTask(), 0);
    }

    public static void addPersistChunk(Location location, boolean b) {
        ChunkCoord coord = new ChunkCoord(location);
        persistChunks.put(coord, b);
    }

    public static boolean isPersistChunk(Location location) {
        ChunkCoord coord = new ChunkCoord(location);
        return persistChunks.get(coord);
    }

    public static Boolean isPersistChunk(Chunk chunk) {
        ChunkCoord coord = new ChunkCoord(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        return persistChunks.get(coord);
    }

    public static void addPersistChunk(String worldname, int x, int z, boolean b) {
        ChunkCoord coord = new ChunkCoord(worldname, x, z);
        persistChunks.put(coord, b);
    }

    public static Location getLocationFromHash(String hash) {
        String[] split = hash.split(",");
        return new Location(BukkitObjects.getWorld(split[0]), Double.parseDouble(split[1]),
                Double.parseDouble(split[2]),
                Double.parseDouble(split[3]));
    }

    public static void removeStructure(Structure structure) {
        structures.remove(structure.getCorner());
    }

    public static void addStructure(Structure structure) {
        structures.put(structure.getCorner(), structure);
    }

    public static Structure getStructure(BlockCoord center) {
        return structures.get(center);
    }

    public static void addStructureBlock(BlockCoord coord, Buildable owner, boolean damageable) {
        StructureBlock sb = new StructureBlock(coord, owner);
        sb.setDamageable(damageable);
        structureBlocks.put(coord, sb);

        String key = getXYKey(coord);
        HashSet<Buildable> buildables = buildablesInChunk.get(key);
        if (buildables == null) {
            buildables = new HashSet<>();
        }
        buildables.add(owner);
        buildablesInChunk.put(key, buildables);

    }

    public static void removeStructureBlock(BlockCoord coord) {
        StructureBlock sb = structureBlocks.get(coord);
        if (sb == null) {
            return;
        }
        structureBlocks.remove(coord);

        String key = getXYKey(coord);
        HashSet<Buildable> buildables = buildablesInChunk.get(key);
        if (buildables != null) {
            buildables.remove(sb.getOwner());
            if (!buildables.isEmpty()) {
                buildablesInChunk.put(key, buildables);
            } else {
                buildablesInChunk.remove(key);
            }
        }

    }

    public static StructureBlock getStructureBlock(BlockCoord coord) {
        return structureBlocks.get(coord);
    }

    public static HashSet<Buildable> getBuildablesAt(BlockCoord coord) {
        return buildablesInChunk.get(getXYKey(coord));
    }

    public static String getXYKey(BlockCoord coord) {
        return coord.getX() + ":" + coord.getZ() + ":" + coord.getWorldname();
    }

    public static Structure getStructureById(int id) {
        for (Structure struct : structures.values()) {
            if (struct.getId() == id) {
                return struct;
            }
        }
        return null;
    }

    public static StructureSign getStructureSign(BlockCoord coord) {
        return structureSigns.get(coord);
    }

    public static void addStructureSign(StructureSign sign) {
        structureSigns.put(sign.getCoord(), sign);
    }

    public static void addStructureChest(StructureChest structChest) {
        structureChests.put(structChest.getCoord(), structChest);
    }

    public static StructureChest getStructureChest(BlockCoord coord) {
        return structureChests.get(coord);
    }

    public static Iterator<Entry<BlockCoord, Structure>> getStructureIterator() {
        return structures.entrySet().iterator();
    }

    public static void addTradeGood(TradeGood good) {
        tradeGoods.put(good.getCoord(), good);
    }

    public static TradeGood getTradeGood(BlockCoord coord) {
        return tradeGoods.get(coord);
    }

    public static Collection<TradeGood> getTradeGoods() {
        return tradeGoods.values();
    }

    public static void addProtectedBlock(ProtectedBlock pb) {
        protectedBlocks.put(pb.getCoord(), pb);
    }

    public static ProtectedBlock getProtectedBlock(BlockCoord coord) {
        return protectedBlocks.get(coord);
    }

    public static SessionDatabase getSessionDB() {
        return sdb;
    }

    public static int getLeftoverSize(HashMap<Integer, ItemStack> leftovers) {
        int total = 0;
        for (ItemStack stack : leftovers.values()) {
            total += stack.getAmount();
        }
        return total;
    }

    public static int getSecondsBetween(long t1, long t2) {
        return (int) ((t2 - t1) / 1000);
    }

    public static boolean testFileFlag(String filename) {
        File f = new File(filename);
        return f.exists();
    }

    public static boolean hasTimeElapsed(SessionEntry se, double seconds) {
        long now = System.currentTimeMillis();
        int secondsBetween = getSecondsBetween(se.time, now);

        // First determine the time between two events.
        return !(secondsBetween < seconds);
    }

    public static void removeStructureSign(StructureSign structureSign) {
        structureSigns.remove(structureSign.getCoord());
    }

    public static void removeStructureChest(StructureChest structureChest) {
        structureChests.remove(structureChest.getCoord());
    }

    public static void addFarmChunk(ChunkCoord coord, FarmChunk fc) {
        farmChunks.put(coord, fc);
        CivGlobal.queueFarmChunk(fc);
        farmGrowQueue.add(fc);
    }

    public static FarmChunk getFarmChunk(ChunkCoord coord) {
        return farmChunks.get(coord);
    }

    public static Collection<FarmChunk> getFarmChunks() {
        return farmChunks.values();
    }

    public static Date getNextUpkeepDate() {

        EventTimer daily = EventTimer.timers.get("daily");
        return daily.getNext().getTime();

    }

    public static void removeTownChunk(TownChunk tc) {
        if (tc.getChunkCoord() != null) {
            townChunks.remove(tc.getChunkCoord());
        }
    }

    public static Date getNextHourlyTickDate() {
        EventTimer hourly = EventTimer.timers.get("hourly");
        return hourly.getNext().getTime();
    }

    public static void removeFarmChunk(ChunkCoord coord) {
        FarmChunk fc = getFarmChunk(coord);
        if (fc != null) {
            CivGlobal.dequeueFarmChunk(fc);
            farmGrowQueue.remove(fc);
        }
        farmChunks.remove(coord);
    }

    public static void addProtectedItemFrame(ItemFrameStorage framestore) {
        protectedItemFrames.put(framestore.getUUID(), framestore);
        ItemFrameStorage.attachedBlockMap.put(framestore.getAttachedBlock(), framestore);
    }

    public static ItemFrameStorage getProtectedItemFrame(UUID id) {
        return protectedItemFrames.get(id);
    }

    public static void removeProtectedItemFrame(UUID id) {

        CivLog.debug("Remove ID: " + id);
        if (id == null) {
            return;
        }
        ItemFrameStorage store = getProtectedItemFrame(id);
        if (store == null) {
            return;
        }
        ItemFrameStorage.attachedBlockMap.remove(store.getAttachedBlock());
        protectedItemFrames.remove(id);
    }

    public static void addBonusGoodie(BonusGoodie goodie) {
        bonusGoodies.put(goodie.getOutpost().getCorner(), goodie);
    }

    public static BonusGoodie getBonusGoodie(BlockCoord bcoord) {
        return bonusGoodies.get(bcoord);
    }

    public static Entity getEntityAtLocation(Location loc) {

        Chunk chunk = loc.getChunk();
        for (Entity entity : chunk.getEntities()) {
            if (entity.getLocation().getBlock().equals(loc.getBlock())) {
                return entity;
            }

        }
        return null;
    }

    public static boolean isBonusGoodie(ItemStack item) {
        if (item == null) {
            return false;
        }

        if (item.getType() == Material.AIR) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        if (!meta.hasLore() || meta.getLore().size() < BonusGoodie.LoreIndex.values().length) {
            return false;
        }

        return meta.getLore().get(BonusGoodie.LoreIndex.TYPE.ordinal()).equals(BonusGoodie.LORE_TYPE);

    }

    public static BonusGoodie getBonusGoodie(ItemStack item) {
        if (!isBonusGoodie(item)) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();

        String outpostLocation = meta.getLore().get(BonusGoodie.LoreIndex.OUTPOSTLOCATION.ordinal());
        BlockCoord bcoord = new BlockCoord(outpostLocation);
        return getBonusGoodie(bcoord);
    }

    public static Collection<BonusGoodie> getBonusGoodies() {
        return bonusGoodies.values();
    }

    public static void checkForDuplicateGoodies() {
        // Look through protected item frames and repo and duplicates we find.
        HashMap<String, Boolean> outpostsInFrames = new HashMap<>();

        for (ItemFrameStorage fs : protectedItemFrames.values()) {
            try {
                if (fs.noFrame() || fs.isEmpty()) {
                    continue;
                }
            } catch (CivException e) {
                e.printStackTrace();
                continue;
            }

            BonusGoodie goodie = getBonusGoodie(fs.getItem());
            if (goodie == null) {
                continue;
            }

            if (outpostsInFrames.containsKey(goodie.getOutpost().getCorner().toString())) {
                //	CivMessage.sendTown(goodie.getOutpost().getTown(), CivColor.Rose+"WARNING: "+CivColor.Yellow+"Duplicate goodie item detected for good "+
                //			goodie.getDisplayName()+" at outpost "+goodie.getOutpost().getCorner().toString()+
                //			". Item was reset back to outpost.");
                fs.clearItem();
            } else {
                outpostsInFrames.put(goodie.getOutpost().getCorner().toString(), true);
            }

        }

    }

    /*
     * Empty, duplicate frames can cause endless headaches by
     * making item frames show items that are unobtainable.
     * This function attempts to correct the issue by finding any
     * duplicate, empty frames and removing them.
     */

    public static Entity getEntityClassFromUUID(World world, Class<?> c, UUID id) {
        for (Entity e : world.getEntitiesByClasses(c)) {
            if (e.getUniqueId().equals(id)) {
                return e;
            }
        }
        return null;
    }

    public static Date getNextRandomEventTime() {
        EventTimer repo = EventTimer.timers.get("random");
        return repo.getNext().getTime();
    }

    public static Date getNextRepoTime() {
        EventTimer repo = EventTimer.timers.get("repo-goodies");
        return repo.getNext().getTime();
    }

    public static Buildable getNearestBuildable(Location location) {
        Buildable nearest = null;
        double lowest_distance = Double.MAX_VALUE;

        for (Buildable struct : structures.values()) {
            Location loc = new Location(Bukkit.getWorld("world"), struct.getCenterLocation().getX(), struct.getCorner().getLocation().getY(), struct.getCenterLocation().getZ());
            double distance = loc.distance(location);
            if (distance < lowest_distance) {
                lowest_distance = distance;
                nearest = struct;
            }
        }

        for (Buildable wonder : wonders.values()) {
            Location loc = new Location(Bukkit.getWorld("world"), wonder.getCenterLocation().getX(), wonder.getCorner().getLocation().getY(), wonder.getCenterLocation().getZ());
            double distance = loc.distance(location);
            if (distance < lowest_distance) {
                lowest_distance = distance;
                nearest = wonder;
            }
        }

        return nearest;
    }


    public static void setAggressor(Civilization civ, Civilization otherCiv, Civilization aggressor) {
        civ.getDiplomacyManager().setAggressor(aggressor, otherCiv);
        otherCiv.getDiplomacyManager().setAggressor(aggressor, civ);
    }

    public static void setRelation(Civilization civ, Civilization otherCiv, Status status) {
        if (civ.getId() == otherCiv.getId()) {
            return;
        }

        civ.getDiplomacyManager().setRelation(otherCiv, status, null);
        otherCiv.getDiplomacyManager().setRelation(civ, status, null);

        String out = "";
        switch (status) {
            case NEUTRAL ->
                    out += ChatColor.GRAY + CivSettings.localize.localizedString("civGlobal_relation_Neutral") + ChatColor.WHITE;
            case HOSTILE ->
                    out += ChatColor.YELLOW + CivSettings.localize.localizedString("civGlobal_relation_Hostile") + ChatColor.WHITE;
            case WAR ->
                    out += ChatColor.RED + CivSettings.localize.localizedString("civGlobal_relation_War") + ChatColor.WHITE;
            case PEACE ->
                    out += ChatColor.GREEN + CivSettings.localize.localizedString("civGlobal_relation_Peace") + ChatColor.WHITE;
            case ALLY ->
                    out += ChatColor.DARK_GREEN + CivSettings.localize.localizedString("civGlobal_relation_Allied") + ChatColor.WHITE;
            default -> {
            }
        }
        CivMessage.global(CivSettings.localize.localizedString("var_civGlobal_relation_isNow", civ.getName(), out, otherCiv.getName()));
        CivGlobal.updateTagsBetween(civ, otherCiv);
    }

    private static void updateTagsBetween(Civilization civ, Civilization otherCiv) {
        TaskMaster.asyncTask(new UpdateTagBetweenCivsTask(civ, otherCiv), 0);
    }

    public static void requestRelation(Civilization fromCiv, Civilization toCiv, String question,
                                       long timeout, QuestionResponseInterface finishedFunction) throws CivException {

        CivQuestionTask task = civQuestions.get(toCiv.getName());
        if (task != null) {
            /* Civ already has a question pending. Lets deny this question until it times out
             * this will allow questions to come in on a pseduo 'first come first serve' and
             * prevents question spamming.
             */
            throw new CivException(CivSettings.localize.localizedString("civGlobal_civHasPendingRequest"));
        }

        task = new CivQuestionTask(toCiv, fromCiv, question, timeout, finishedFunction);
        civQuestions.put(toCiv.getName(), task);
        TaskMaster.asyncTask("", task, 0);
    }

    public static void removeRequest(String name) {
        civQuestions.remove(name);
    }

    public static CivQuestionTask getCivQuestionTask(Civilization senderCiv) {
        return civQuestions.get(senderCiv.getName());
    }

    public static void checkForExpiredRelations() {
        Date now = new Date();

        ArrayList<Relation> deletedRelations = new ArrayList<>();
        for (Civilization civ : CivGlobal.getCivs()) {
            for (Relation relation : civ.getDiplomacyManager().getRelations()) {
                if (relation.getExpireDate() != null && now.after(relation.getExpireDate())) {
                    deletedRelations.add(relation);
                }
            }
        }

        for (Relation relation : deletedRelations) {
            try {
                relation.delete();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static boolean willInstantBreak(Material type) {

        switch (type) {
            case CROPS:
            case DIODE:
            case DIODE_BLOCK_ON:
            case FIRE:
            case FLOWER_POT:
            case FLOWER_POT_ITEM:
            case DIODE_BLOCK_OFF:
            case DEAD_BUSH:
            case BED_BLOCK:
            case BROWN_MUSHROOM:
            case GLASS:
            case GRASS:
            case LEAVES:
            case LEVER:
            case LONG_GRASS:
            case MELON_STEM:
            case NETHER_STALK:
            case NETHER_WARTS:
            case PUMPKIN_STEM:
            case REDSTONE:
            case REDSTONE_TORCH_OFF:
            case REDSTONE_TORCH_ON:
            case REDSTONE_WIRE:
            case SAPLING:
            case SKULL:
            case SKULL_ITEM:
            case SNOW:
            case SUGAR_CANE_BLOCK:
            case THIN_GLASS:
            case TNT:
            case TORCH:
            case TRIPWIRE:
            case TRIPWIRE_HOOK:
            case VINE:
            case WATER_LILY:
            case YELLOW_FLOWER:
                return true;
            default:
                return false;
        }
    }

    public static String updateTag(Player namedPlayer, Player player) { //TODO?
        Resident namedRes = CivGlobal.getResident(namedPlayer);
        Resident playerRes = CivGlobal.getResident(player);

        if (CivGlobal.isMutualOutlaw(namedRes, playerRes)) {
            return ChatColor.DARK_GRAY + namedPlayer.getName();
        }

        if (namedRes == null || !namedRes.hasTown()) {
            return namedPlayer.getName();
        }

        if (playerRes == null || !playerRes.hasTown()) {
            return namedPlayer.getName();
        }
        String color = String.valueOf(ChatColor.WHITE);
        if (namedRes.getTown().getCiv() == playerRes.getTown().getCiv()) {
            color = String.valueOf(ChatColor.GREEN);
        } else {

            Relation.Status status = playerRes.getTown().getCiv().getDiplomacyManager().getRelationStatus(namedRes.getTown().getCiv());
            switch (status) {
                case PEACE -> color = String.valueOf(ChatColor.AQUA);
                case ALLY -> color = String.valueOf(ChatColor.GREEN);
                case HOSTILE -> color = String.valueOf(ChatColor.YELLOW);
                case WAR -> color = String.valueOf(ChatColor.RED);
                default -> {
                }
            }
        }

        return color + namedPlayer.getName();
    }


    public static void addWonder(Wonder wonder) {
        wonders.put(wonder.getCorner(), wonder);
    }

    public static Wonder getWonder(BlockCoord coord) {
        return wonders.get(coord);
    }

    public static void removeWonder(Wonder wonder) {
        if (wonder.getCorner() != null) {
            wonders.remove(wonder.getCorner());
        }
    }

    public static Collection<Wonder> getWonders() {
        return wonders.values();
    }

    public static Wonder getWonderByConfigId(String id) {
        for (Wonder wonder : wonders.values()) {
            if (wonder.getConfigId().equals(id)) {
                return wonder;
            }
        }
        return null;
    }

    public static Wonder getWonderById(int id) {
        for (Wonder wonder : wonders.values()) {
            if (wonder.getId() == id) {
                return wonder;
            }
        }

        return null;
    }

    /*
     * Gets a TreeMap of the civilizations sorted based on the distance to
     * the provided town. Ignores the civilization the town belongs to.
     */
    public static TreeMap<Double, Civilization> findNearestCivilizations(Town town) {

        TownHall townhall = town.getTownHall();
        TreeMap<Double, Civilization> returnMap = new TreeMap<>();

        if (townhall == null) {
            return returnMap;
        }

        for (Civilization civ : CivGlobal.getCivs()) {
            if (civ == town.getCiv()) {
                continue;
            }

            // Get shortest distance of any of this civ's towns.
            double shortestDistance = Double.MAX_VALUE;
            for (Town t : civ.getTowns()) {
                TownHall tempTownHall = t.getTownHall();
                if (tempTownHall == null) {
                    continue;
                }

                double tmpDistance = tempTownHall.getCorner().distanceSquared(townhall.getCorner());
                if (tmpDistance < shortestDistance) {
                    shortestDistance = tmpDistance;
                }
            }

            // Now insert the shortest distance into the tree map.
            returnMap.put(shortestDistance, civ);
        }

        // Map returned will be sorted.
        return returnMap;
    }

    @SuppressWarnings("deprecation")
    public static OfflinePlayer getFakeOfflinePlayer(String name) {
        return Bukkit.getOfflinePlayer(name);
    }

    public static Collection<CultureChunk> getCultureChunks() {
        return cultureChunks.values();
    }


    public static Collection<TownChunk> getTownChunks() {
        return townChunks.values();
    }

    public static Integer getScoreForCiv(Civilization civ) {
        for (Entry<Integer, Civilization> entry : civilizationScores.entrySet()) {
            if (civ == entry.getValue()) {
                return entry.getKey();
            }
        }
        return 0;
    }

    public static Collection<StructureSign> getStructureSigns() {
        return structureSigns.values();
    }

    public static boolean isMutualOutlaw(Resident defenderResident, Resident attackerResident) {

        if (defenderResident == null || attackerResident == null) {
            return false;
        }

        if (defenderResident.hasTown() && defenderResident.getTown().isOutlaw(attackerResident.getName())) {
            return true;
        }

        return attackerResident.hasTown() && attackerResident.getTown().isOutlaw(defenderResident.getName());
    }

    public static boolean isOutlawHere(Resident resident, TownChunk tc) {
        if (tc == null) {
            return false;
        }

        if (tc.getTown() == null) {
            return false;
        }

        return tc.getTown().isOutlaw(resident.getName());
    }

    public static Date getTodaysSpawnRegenDate() {
        Calendar now = Calendar.getInstance();
        Calendar nextSpawn = Calendar.getInstance();

        int hourOfDay = CivSettings.civConfig.getInt("global.regen_spawn_hour", 0);

        nextSpawn.set(Calendar.HOUR_OF_DAY, hourOfDay);
        nextSpawn.set(Calendar.MINUTE, 0);
        nextSpawn.set(Calendar.SECOND, 0);

        if (nextSpawn.after(now)) {
            return nextSpawn.getTime();
        }

        nextSpawn.add(Calendar.DATE, 1);
        nextSpawn.set(Calendar.HOUR_OF_DAY, hourOfDay);
        nextSpawn.set(Calendar.MINUTE, 0);
        nextSpawn.set(Calendar.SECOND, 0);

        return nextSpawn.getTime();
    }

    public static void addConqueredCiv(Civilization civ) {
        conqueredCivs.put(civ.getName().toLowerCase(), civ);
    }

    public static void removeConqueredCiv(Civilization civ) {
        conqueredCivs.remove(civ.getName().toLowerCase());
    }

    public static Civilization getConqueredCiv(String name) {
        return conqueredCivs.get(name.toLowerCase());
    }

    public static Collection<Civilization> getConqueredCivs() {
        return conqueredCivs.values();
    }

    public static Civilization getConqueredCivFromId(int id) {
        for (Civilization civ : getConqueredCivs()) {
            if (civ.getId() == id) {
                return civ;
            }
        }
        return null;
    }

    public static Camp getCamp(String name) {
        return camps.get(name.toLowerCase());
    }

    public static void addCamp(Camp camp) {
        camps.put(camp.getName().toLowerCase(), camp);
    }

    public static void removeCamp(String name) {
        camps.remove(name.toLowerCase());
    }

    public static void addCampBlock(CampBlock cb) {
        campBlocks.put(cb.getCoord(), cb);

        ChunkCoord coord = new ChunkCoord(cb.getCoord());
        campChunks.put(coord, cb.getCamp());
    }

    public static CampBlock getCampBlock(BlockCoord bcoord) {
        return campBlocks.get(bcoord);
    }

    public static void removeCampBlock(BlockCoord bcoord) {
        campBlocks.remove(bcoord);
    }

    public static Collection<Camp> getCamps() {
        return camps.values();
    }

    public static Camp getCampFromChunk(ChunkCoord coord) {
        return campChunks.get(coord);
    }

    public static void removeCampChunk(ChunkCoord coord) {
        campChunks.remove(coord);
    }

    public static Collection<Market> getMarkets() {
        return markets.values();
    }

    public static void addMarket(Market market) {
        markets.put(market.getCorner(), market);
    }

    public static void removeMarket(Market market) {
        markets.remove(market.getCorner());
    }

    public static Camp getCampFromId(int campID) {
        for (Camp camp : camps.values()) {
            if (camp.getId() == campID) {
                return camp;
            }
        }
        return null;
    }

    public static Collection<Structure> getStructures() {
        return structures.values();
    }

    public static void dequeueFarmChunk(FarmChunk fc) {
        farmChunkUpdateQueue.remove(fc);
    }

    public static void queueFarmChunk(FarmChunk fc) {
        farmChunkUpdateQueue.add(fc);
    }

    public static FarmChunk pollFarmChunk() {
        return farmChunkUpdateQueue.poll();
    }

    public static boolean farmChunkValid(FarmChunk fc) {
        return farmChunks.containsKey(fc.getCoord());
    }

    public static Collection<Civilization> getAdminCivs() {
        return adminCivs.values();
    }

    public static void addAdminCiv(Civilization civ) {
        adminCivs.put(civ.getName(), civ);
    }

    public static void removeAdminCiv(Civilization civ) {
        adminCivs.remove(civ.getName());
    }

    public static boolean isCasualMode() {
        return CivSettings.civConfig.getBoolean("global.casual_mode");
    }

    public static Economy getEconomy() {
        return Optional.ofNullable((econ = Bukkit.getServicesManager().getRegistration(Economy.class).getProvider())).orElse(econ);
    }

    public static EconObject createEconObject(SQLObject holder) {
        if (useEconomy && holder instanceof Resident) {
            return new VaultEconObject(holder, ((Resident) holder).getUUID());
        }
        return new EconObject(holder);
    }
}
