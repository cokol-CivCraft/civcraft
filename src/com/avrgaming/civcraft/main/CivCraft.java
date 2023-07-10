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

import com.avrgaming.civcraft.arena.ArenaListener;
import com.avrgaming.civcraft.arena.ArenaManager;
import com.avrgaming.civcraft.arena.ArenaTimer;
import com.avrgaming.civcraft.command.*;
import com.avrgaming.civcraft.command.admin.AdminCommand;
import com.avrgaming.civcraft.command.camp.CampCommand;
import com.avrgaming.civcraft.command.civ.CivChatCommand;
import com.avrgaming.civcraft.command.civ.CivCommand;
import com.avrgaming.civcraft.command.debug.DebugCommand;
import com.avrgaming.civcraft.command.market.MarketCommand;
import com.avrgaming.civcraft.command.plot.PlotCommand;
import com.avrgaming.civcraft.command.resident.ResidentCommand;
import com.avrgaming.civcraft.command.team.TeamCommand;
import com.avrgaming.civcraft.command.town.TownChatCommand;
import com.avrgaming.civcraft.command.town.TownCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.endgame.EndConditionNotificationTask;
import com.avrgaming.civcraft.event.EventTimerTask;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.fishing.FishingListener;
import com.avrgaming.civcraft.listener.*;
import com.avrgaming.civcraft.listener.armor.ArmorListener;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancementArenaItem;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterialListener;
import com.avrgaming.civcraft.lorestorage.LoreGuiItemListener;
import com.avrgaming.civcraft.populators.MobSpawnerPopulator;
import com.avrgaming.civcraft.populators.TradeGoodPopulator;
import com.avrgaming.civcraft.randomevents.RandomEventSweeper;
import com.avrgaming.civcraft.sessiondb.SessionDBAsyncTimer;
import com.avrgaming.civcraft.siege.CannonListener;
import com.avrgaming.civcraft.structure.Farm;
import com.avrgaming.civcraft.structure.farm.FarmGrowthSyncTask;
import com.avrgaming.civcraft.structure.farm.FarmPreCachePopulateTimer;
import com.avrgaming.civcraft.structurevalidation.StructureValidationChecker;
import com.avrgaming.civcraft.structurevalidation.StructureValidationPunisher;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.sync.*;
import com.avrgaming.civcraft.threading.tasks.ArrowProjectileTask;
import com.avrgaming.civcraft.threading.tasks.ProjectileComponentTimer;
import com.avrgaming.civcraft.threading.tasks.ScoutTowerTask;
import com.avrgaming.civcraft.threading.timers.*;
import com.avrgaming.civcraft.trade.TradeInventoryListener;
import com.avrgaming.civcraft.util.BukkitObjects;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.TimeTools;
import com.avrgaming.civcraft.war.WarListener;
import com.avrgaming.global.scores.CalculateScoreTimer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.sql.SQLException;

public final class CivCraft extends JavaPlugin {

    private static JavaPlugin plugin;
    private boolean isError = false;
    public static boolean isDisable = false;

    public static boolean isValidate = true;

    public static void setIsValidate(boolean b) {
        isValidate = b;
    }

    public static boolean getIsValidate() {
        return isValidate;
    }

    private void startTimers() {

        TaskMaster.asyncTask("SQLUpdate", new SQLUpdate(), 0);

        // Sync Timers
        TaskMaster.syncTimer(SyncBuildUpdateTask.class.getName(),
                new SyncBuildUpdateTask(), 0, 1);

        TaskMaster.syncTimer(SyncUpdateChunks.class.getName(),
                new SyncUpdateChunks(), 0, TimeTools.toTicks(1));

        TaskMaster.syncTimer(SyncLoadChunk.class.getName(),
                new SyncLoadChunk(), 0, 1);

        TaskMaster.syncTimer(SyncGetChestInventory.class.getName(),
                new SyncGetChestInventory(), 0, 1);

        TaskMaster.syncTimer(SyncUpdateInventory.class.getName(),
                new SyncUpdateInventory(), 0, 1);

        TaskMaster.syncTimer(SyncGrowTask.class.getName(),
                new SyncGrowTask(), 0, 1);

        TaskMaster.syncTimer(PlayerLocationCacheUpdate.class.getName(),
                new PlayerLocationCacheUpdate(), 0, 10);

        TaskMaster.asyncTimer("RandomEventSweeper", new RandomEventSweeper(), 0, TimeTools.toTicks(10));

        // Structure event timers
        TaskMaster.asyncTimer("UpdateEventTimer", new UpdateEventTimer(), TimeTools.toTicks(1));
        TaskMaster.asyncTimer("UpdateMinuteEventTimer", new UpdateMinuteEventTimer(), TimeTools.toTicks(20));
        TaskMaster.asyncTimer("RegenTimer", new RegenTimer(), TimeTools.toTicks(5));

        TaskMaster.asyncTimer("BeakerTimer", new BeakerTimer(60), TimeTools.toTicks(60));
        TaskMaster.syncTimer("UnitTrainTimer", new UnitTrainTimer(), TimeTools.toTicks(1));
        TaskMaster.asyncTimer("ReduceExposureTimer", new ReduceExposureTimer(), 0, TimeTools.toTicks(5));

        try {
            double arrow_firerate = CivSettings.getDouble(CivSettings.warConfig, "arrow_tower.fire_rate");
            TaskMaster.syncTimer("arrowTower", new ProjectileComponentTimer(), (int) (arrow_firerate * 20));
            TaskMaster.asyncTimer("ScoutTowerTask", new ScoutTowerTask(), TimeTools.toTicks(1));

        } catch (InvalidConfiguration e) {
            e.printStackTrace();
            return;
        }
        TaskMaster.syncTimer("arrowhomingtask", new ArrowProjectileTask(), 5);

        // Global Event timers
        TaskMaster.syncTimer("FarmCropCache", new FarmPreCachePopulateTimer(), TimeTools.toTicks(30));

        TaskMaster.asyncTimer("FarmGrowthTimer",
                new FarmGrowthSyncTask(), TimeTools.toTicks(Farm.GROW_RATE));

        TaskMaster.asyncTimer("announcer", new AnnouncementTimer("tips.txt", 5), 0, TimeTools.toTicks(60 * 60));
        TaskMaster.asyncTimer("announcerwar", new AnnouncementTimer("war.txt", 60), 0, TimeTools.toTicks(60 * 60));

        TaskMaster.asyncTimer("ChangeGovernmentTimer", new ChangeGovernmentTimer(), TimeTools.toTicks(60));
        TaskMaster.asyncTimer("CalculateScoreTimer", new CalculateScoreTimer(), 0, TimeTools.toTicks(60));

        TaskMaster.asyncTimer(PlayerProximityComponentTimer.class.getName(),
                new PlayerProximityComponentTimer(), TimeTools.toTicks(1));

        TaskMaster.asyncTimer(EventTimerTask.class.getName(), new EventTimerTask(), TimeTools.toTicks(5));

//		if (PlatinumManager.isEnabled()) {
//			TaskMaster.asyncTimer(PlatinumManager.class.getName(), new PlatinumManager(), TimeTools.toTicks(5));
//		}

        TaskMaster.syncTimer("WindmillTimer", new WindmillTimer(), TimeTools.toTicks(60));
        TaskMaster.asyncTimer("EndGameNotification", new EndConditionNotificationTask(), TimeTools.toTicks(3600));

        TaskMaster.asyncTask(new StructureValidationChecker(), TimeTools.toTicks(120));
        TaskMaster.asyncTimer("StructureValidationPunisher", new StructureValidationPunisher(), TimeTools.toTicks(3600));
        TaskMaster.asyncTimer("SessionDBAsyncTimer", new SessionDBAsyncTimer(), 10);

        TaskMaster.syncTimer("ArenaTimer", new ArenaManager(), TimeTools.toTicks(30));
        TaskMaster.syncTimer("ArenaTimeoutTimer", new ArenaTimer(), TimeTools.toTicks(1));

    }

    private void registerEvents() {
        final PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new BlockListener(), this);
        pluginManager.registerEvents(new ChatListener(), this);
        pluginManager.registerEvents(new BonusGoodieManager(), this);
        pluginManager.registerEvents(new MarkerPlacementManager(), this);
        pluginManager.registerEvents(new CustomItemManager(), this);
        pluginManager.registerEvents(new PlayerListener(), this);
        pluginManager.registerEvents(new DebugListener(), this);
        pluginManager.registerEvents(new LoreCraftableMaterialListener(), this);
        pluginManager.registerEvents(new LoreGuiItemListener(), this);

        boolean useEXPAsCurrency;
        try {
            useEXPAsCurrency = CivSettings.getBoolean(CivSettings.civConfig, "global.use_exp_as_currency");

        } catch (InvalidConfiguration e) {
            useEXPAsCurrency = true;
            CivLog.error("Unable to check if EXP should be enabled. Disabling.");
            e.printStackTrace();
        }

        if (useEXPAsCurrency) {
            pluginManager.registerEvents(new DisableXPListener(), this);
        }
        pluginManager.registerEvents(new TradeInventoryListener(), this);
        pluginManager.registerEvents(new ArenaListener(), this);
        pluginManager.registerEvents(new CannonListener(), this);
        pluginManager.registerEvents(new WarListener(), this);
        pluginManager.registerEvents(new FishingListener(), this);
        pluginManager.registerEvents(new LoreEnhancementArenaItem(), this);

        pluginManager.registerEvents(new ArmorListener(getConfig().getStringList("blocked")), this);
    }


    @Override
    public void onEnable() {
        setPlugin(this);

        this.saveDefaultConfig();

        CivLog.init(this);
        BukkitObjects.initialize(this);

        //Load World Populators
        BukkitObjects.getWorlds().get(0).getPopulators().add(new TradeGoodPopulator());
        BukkitObjects.getWorlds().get(0).getPopulators().add(new MobSpawnerPopulator());

        try {
            CivSettings.init(this);

            SQL.initialize();
            SQL.initCivObjectTables();
            ChunkCoord.buildWorldList();
            CivGlobal.loadGlobals();


        } catch (InvalidConfiguration | SQLException | IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            setError(true);
            return;
            //TODO disable plugin?
        }

        // Init commands
        registerCommand("town", new TownCommand());
        registerCommand("resident", new ResidentCommand());
        registerCommand("dbg", new DebugCommand());
        registerCommand("plot", new PlotCommand());
        getCommand("accept").setExecutor(new AcceptCommand());
        getCommand("deny").setExecutor(new DenyCommand());
        registerCommand("civ", new CivCommand());
        getCommand("tc").setExecutor(new TownChatCommand());
        getCommand("cc").setExecutor(new CivChatCommand());
        //getCommand("gc").setExecutor(new GlobalChatCommand());
        registerCommand("ad", new AdminCommand());
        registerCommand("econ", new EconCommand());
        getCommand("pay").setExecutor(new PayCommand());
        registerCommand("build", new BuildCommand());
        registerCommand("market", new MarketCommand());
        getCommand("select").setExecutor(new SelectCommand());
        getCommand("here").setExecutor(new HereCommand());
        registerCommand("camp", new CampCommand());
        registerCommand("trade", new TradeCommand());
        getCommand("kill").setExecutor(new KillCommand());
        registerCommand("team", new TeamCommand());

        registerEvents();


        startTimers();

        //creativeInvPacketManager.init(this);
    }

    public void registerCommand(String name, CommandBase command) {
        getCommand(name).setExecutor(command);
        getCommand(name).setTabCompleter(command);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        isDisable = true;
        SQLUpdate.save();
    }

    public boolean hasPlugin(String name) {
        return getServer().getPluginManager().getPlugin(name) != null;
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean isError) {
        this.isError = isError;
    }


    public static JavaPlugin getPlugin() {
        return plugin;
    }


    public static void setPlugin(JavaPlugin plugin) {
        CivCraft.plugin = plugin;
    }


}
