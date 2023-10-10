package com.avrgaming.civcraft.arena;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigArena;
import com.avrgaming.civcraft.config.ConfigArenaTeam;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.TimeTools;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.ScoreboardManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class ArenaManager implements Runnable {

    public static HashMap<BlockCoord, ArenaControlBlock> arenaControlBlocks = new HashMap<>();
    public static HashMap<BlockCoord, Arena> chests = new HashMap<>();
    public static HashMap<BlockCoord, Arena> respawnSigns = new HashMap<>();
    public static HashMap<String, Arena> activeArenas = new HashMap<>();

    public static Queue<ArenaTeam> teamQueue = new LinkedList<>();
    public static final int MAX_INSTANCES = 1;
    public static ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
    public static boolean enabled = true;

    @Override
    public void run() {

        /*
         * Set up an arena if there is room to do so.
         */
        if (activeArenas.size() < MAX_INSTANCES && enabled) {
            ArenaTeam team1 = teamQueue.poll();
            if (team1 == null) {
                /* No teams waiting in queue. Do nothing. */
                return;
            }

            ArenaTeam team2 = teamQueue.poll();
            if (team2 == null) {
                /* We need another team to start a match, requeue our team and wait. */
                CivMessage.sendTeam(team1, CivSettings.localize.localizedString("arena_noOtherTeams"));
                teamQueue.add(team1);
                return;
            }

            /* Choose a random arena. */
            Random rand = new Random();
            int index = rand.nextInt(CivSettings.arenas.size());

            int i = 0;
            ConfigArena arena = null;

            for (ConfigArena a : CivSettings.arenas.values()) {
                if (i == index) {
                    arena = a;
                    break;
                }
                i++;
            }

            if (arena == null) {
                CivLog.error("Couldn't find an arena configured....");
                return;
            }

            /* Create a new arena. */
            try {
                Arena activeArena = createArena(arena);
                CivMessage.sendTeam(team1, CivSettings.localize.localizedString("arena_enteringArenaIn10"));
                CivMessage.sendTeam(team2, CivSettings.localize.localizedString("arena_enteringArenaIn10"));

                TaskMaster.syncTask(() -> {
                    try {
                        addTeamToArena(team1, team2, activeArena);
                        addTeamToArena(team2, team1, activeArena);
                        startArenaMatch(activeArena, team1, team2);
                    } catch (CivException e) {
                        CivMessage.sendTeam(team1, CivSettings.localize.localizedString("arena_ErrorKicked"));
                        CivMessage.sendTeam(team2, CivSettings.localize.localizedString("arena_ErrorKicked"));

                        CivMessage.sendTeam(team1, CivSettings.localize.localizedString("arena_ErrorKickedMessage") + e.getMessage());
                        CivMessage.sendTeam(team2, CivSettings.localize.localizedString("arena_ErrorKickedMessage") + e.getMessage());

                        e.printStackTrace();
                    }

                }, TimeTools.toTicks(10));
            } catch (CivException e) {
                e.printStackTrace();
                return;
            }

        }

        /*
         * Iterate through all of the teams still waiting in the queue and notify
         * them of their position in line.
         */
        int i = 0;
        for (ArenaTeam team : teamQueue) {
            if (!enabled) {
                CivMessage.sendTeam(team, CivSettings.localize.localizedString("arena_disabled"));
            } else {
                if (i < 2) {
                    CivMessage.sendTeam(team, CivSettings.localize.localizedString("arena_waitingBusy"));
                } else {
                    CivMessage.sendTeam(team, CivSettings.localize.localizedString("arena_waitingQueue") + i);
                }
            }
            i++;
        }

    }

    public static void startArenaMatch(Arena activeArena, ArenaTeam team1, ArenaTeam team2) {

        /* Set up objectives.. */
        Objective points1 = activeArena.getScoreboard(team1.getName()).registerNewObjective("teampoints1", "dummy");
        Objective points2 = activeArena.getScoreboard(team2.getName()).registerNewObjective("teampoints2", "dummy");

        points1.setDisplaySlot(DisplaySlot.SIDEBAR);
        points1.setDisplayName("Team Hitpoints");
        points2.setDisplaySlot(DisplaySlot.SIDEBAR);
        points2.setDisplayName("Team Hitpoints");

        Score score1Team1 = points1.getScore(team1.getTeamScoreboardName());
        Score score1Team2 = points1.getScore(team2.getTeamScoreboardName());
        Score timeout1 = points1.getScore("TimeLeft");
        try {
            timeout1.setScore(CivSettings.arenaConfig.getInt("timeout", 1800));
        } catch (IllegalStateException e1) {
            timeout1.setScore(1800);
            e1.printStackTrace();
        }

        score1Team1.setScore(activeArena.config.teams.get(0).controlPoints.size() * activeArena.config.control_block_hp);
        score1Team2.setScore(activeArena.config.teams.get(1).controlPoints.size() * activeArena.config.control_block_hp);

        Score score2Team1 = points2.getScore(team1.getTeamScoreboardName());
        Score score2Team2 = points2.getScore(team2.getTeamScoreboardName());
        Score timeout2 = points1.getScore("TimeLeft");
        try {
            timeout2.setScore(CivSettings.arenaConfig.getInt("timeout", 1800));
        } catch (IllegalStateException e1) {
            timeout2.setScore(1800);
            e1.printStackTrace();
        }

        score2Team1.setScore(activeArena.config.teams.get(0).controlPoints.size() * activeArena.config.control_block_hp);
        score2Team2.setScore(activeArena.config.teams.get(1).controlPoints.size() * activeArena.config.control_block_hp);

        activeArena.objectives.put(team1.getName(), points1);
        activeArena.objectives.put(team2.getName(), points2);

        /* Save and clear inventories */
        for (Resident resident : team1.teamMembers) {
            resident.saveInventory();
            resident.clearInventory();
            resident.setInsideArena(true);
            resident.save();

            try {
                Player player = CivGlobal.getPlayer(resident);
                player.setScoreboard(activeArena.getScoreboard(resident.getTeam().getName()));
            } catch (CivException e) {
                //Player offline.
            }
        }

        for (Resident resident : team2.teamMembers) {
            resident.saveInventory();
            resident.clearInventory();
            resident.setInsideArena(true);
            resident.save();

            try {
                Player player = CivGlobal.getPlayer(resident);
                player.setScoreboard(activeArena.getScoreboard(resident.getTeam().getName()));
            } catch (CivException e) {
                //Player offline.
            }
        }

        CivMessage.sendArena(activeArena, CivSettings.localize.localizedString("arena_started"));
    }

    public static void addTeamToQueue(ArenaTeam team) throws CivException {
        if (teamQueue.contains(team)) {
            throw new CivException(CivSettings.localize.localizedString("arena_alreadyInQueue"));
        }

        for (Resident resident : team.teamMembers) {
            try {
                CivGlobal.getPlayer(resident);
            } catch (CivException e) {
                continue;
            }


            if (!resident.isUsesAntiCheat() && CivSettings.isUsingAC()) {
                throw new CivException(CivSettings.localize.localizedString("var_arena_errorMissingAntiCheat", resident.getName()));
            }
        }

        CivMessage.sendTeam(team, CivSettings.localize.localizedString("arena_addingTeamToQueue"));
        if (teamQueue.size() > 2) {
            CivMessage.sendTeam(team, CivSettings.localize.localizedString("arena_waitingQueue") + teamQueue.size());
        }
        teamQueue.add(team);
    }

    public static void addTeamToArena(ArenaTeam team, ArenaTeam otherTeam, Arena arena) throws CivException {
        arena.addTeam(team);
        team.setCurrentArena(arena);

        CivMessage.sendTeamHeading(team, CivSettings.localize.localizedString("arena_statsHeading"));
        CivMessage.sendTeam(team, CivSettings.localize.localizedString("arena_statsName") + " " + ChatColor.YELLOW + ChatColor.BOLD + arena.config.name);
        CivMessage.sendTeam(team, String.valueOf(ChatColor.GREEN) + ChatColor.BOLD + team.getName() + ChatColor.RESET + " VS " + ChatColor.RED + ChatColor.BOLD + otherTeam.getName());
        CivMessage.sendTeam(team, CivSettings.localize.localizedString("arena_statsScore") + " " + ChatColor.GREEN + team.getLadderPoints() + " " + getFavoredString(team, otherTeam));
        CivMessage.sendTeam(team, CivSettings.localize.localizedString("arena_statsTheirScore") + " " + ChatColor.GREEN + otherTeam.getLadderPoints() + " " + getFavoredString(otherTeam, team));
        CivMessage.sendTeam(team, CivSettings.localize.localizedString("arena_statsTheirTeam") + " " + otherTeam.getMemberListSaveString());
    }

    public static Arena createArena(ConfigArena arena) throws CivException {

        /* Copy world from source to prepare it. */
        File srcFolder = new File("arenas/" + arena.world_source);

        if (!srcFolder.exists()) {
            throw new CivException("No world source found at:" + arena.world_source);
        }

        Arena activeArena = new Arena(arena);
        String instanceWorldName = activeArena.getInstanceName();

        File destFolder = new File(instanceWorldName);

        try {
            FileUtils.deleteDirectory(destFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            copyFolder(srcFolder, destFolder);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        World world = createArenaWorld(instanceWorldName);
        createArenaControlPoints(arena, world, activeArena);

        activeArenas.put(instanceWorldName, activeArena);
        return activeArena;
    }

    public static void destroyArena(String instanceName) throws CivException {
        Arena arena = activeArenas.get(instanceName);
        if (arena == null) {
            throw new CivException("No arena with instance name:" + instanceName);
        }

        LinkedList<BlockCoord> removeUs = new LinkedList<>();
        for (BlockCoord bcoord : arenaControlBlocks.keySet()) {
            if (bcoord.getWorldname().equals(instanceName)) {
                removeUs.add(bcoord);
            }
        }
        for (BlockCoord bcoord : removeUs) {
            arenaControlBlocks.remove(bcoord);
        }
        removeUs.clear();

        for (BlockCoord bcoord : respawnSigns.keySet()) {
            if (bcoord.getWorldname().equals(instanceName)) {
                removeUs.add(bcoord);
            }
        }
        for (BlockCoord bcoord : removeUs) {
            respawnSigns.remove(bcoord);
        }
        removeUs.clear();

        for (BlockCoord bcoord : chests.keySet()) {
            if (bcoord.getWorldname().equals(instanceName)) {
                removeUs.add(bcoord);
            }
        }
        for (BlockCoord bcoord : removeUs) {
            chests.remove(bcoord);
        }

        arena.returnPlayers();
        arena.clearTeams();

        activeArenas.remove(instanceName);
        Bukkit.getServer().unloadWorld(instanceName, false);

        try {
            FileUtils.deleteDirectory(new File(instanceName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createArenaControlPoints(ConfigArena arena, World world, Arena activeArena) {

        // Create control points instead of full on structures. No block will be movable so we dont need
        // to store/create a billionty structure blocks. Only need to create ArenaControlPoint objects and determine
        // if they can be broken.

        for (ConfigArenaTeam team : arena.teams) {
            for (BlockCoord c : team.controlPoints) {
                BlockCoord bcoord = new BlockCoord(world.getName(), c.getX(), c.getY(), c.getZ());
                ArenaControlBlock acb = new ArenaControlBlock(bcoord, team.number, arena.control_block_hp, activeArena);
                arenaControlBlocks.put(bcoord, acb);
            }

            /* Create teleport signs. */
            BlockCoord coord = team.respawnSign;
            Location loc = coord.getCenteredLocation();
            loc.setWorld(world);

            if (loc.getBlock().getType().equals(Material.SIGN_POST) ||
                    loc.getBlock().getType().equals(Material.WALL_SIGN)) {
                Sign sign = (Sign) loc.getBlock().getState();
                sign.setLine(0, "");
                sign.setLine(1, "Respawn");
                sign.setLine(2, "At Arena");
                sign.setLine(3, "");

                sign.update();
                respawnSigns.put(new BlockCoord(loc), activeArena);
            } else {
                CivLog.error("Couldn't find sign for respawn sign for arena:" + arena.name);
            }

            for (BlockCoord c : team.chests) {
                BlockCoord bcoord = new BlockCoord(world.getName(), c.getX(), c.getY(), c.getZ());
                chests.put(bcoord, activeArena);

                bcoord.getBlock().setType(Material.ENDER_CHEST);
            }

        }

    }

    private static World createArenaWorld(String name) {
        World world = Bukkit.getServer().getWorld(name);
        if (world != null) {
            return world;
        }
        WorldCreator wc = new WorldCreator(name);
        wc.environment(Environment.NORMAL);
        wc.type(WorldType.FLAT);
        wc.generateStructures(false);

        World arena_world = Bukkit.getServer().createWorld(wc);
        arena_world.setAutoSave(false);
        arena_world.setSpawnFlags(false, false);
        arena_world.setKeepSpawnInMemory(false);
        ChunkCoord.addWorld(arena_world);

        return arena_world;
    }


    private static void copyFolder(File src, File dest) throws IOException {
        if (src.isDirectory()) {
            //if directory not exists, create it
            if (!dest.exists()) {
                dest.mkdir();
                CivLog.info("Directory copied from "
                        + src + "  to " + dest);
            }

            //list all the directory contents
            String[] files = src.list();

            for (String file : files) {
                //construct the src and dest file structure
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                //recursive copy
                copyFolder(srcFile, destFile);
            }

        } else {
            //if file, then copy it
            //Use bytes stream to support all file types
            InputStream in = Files.newInputStream(src.toPath());
            OutputStream out = Files.newOutputStream(dest.toPath());

            byte[] buffer = new byte[1024];

            int length;
            //copy the file content in bytes
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            in.close();
            out.close();
            CivLog.info("File copied from " + src + " to " + dest);
        }
    }

    public static String getFavoredString(ArenaTeam target, ArenaTeam other) {
        if (target.getLadderPoints() < other.getLadderPoints()) {
            return "";
        }

        int slightly_favored_points = CivSettings.arenaConfig.getInt("slightly_favored_points", 400);
        int favored_points = CivSettings.arenaConfig.getInt("favored_points", 1000);

        int diff = target.getLadderPoints() - other.getLadderPoints();
        if (diff > favored_points) {
            return ChatColor.RED + "Favored";
        } else if (diff > slightly_favored_points) {
            return ChatColor.YELLOW + "Slightly Favored";
        }

        return "";

    }


    public static void declareVictor(Arena arena, ArenaTeam loser, ArenaTeam winner) {
        CivMessage.sendArena(arena, CivSettings.localize.localizedString("var_arena_hasDefeated", String.valueOf(ChatColor.GREEN) + ChatColor.BOLD + winner.getName(), String.valueOf(ChatColor.RED) + ChatColor.BOLD + loser.getName()));
        CivMessage.sendArena(arena, CivSettings.localize.localizedString("arena_leavingIn10"));
        TaskMaster.syncTask(() -> {
            int base_points = CivSettings.arenaConfig.getInt("base_ladder_points", 100);
            int slightly_favored_points = CivSettings.arenaConfig.getInt("slightly_favored_points", 400);
            int favored_points = CivSettings.arenaConfig.getInt("favored_points", 1000);
            double slightly_favored_modifier = CivSettings.arenaConfig.getDouble("slightly_favored_modifier", 0.5);
            double favored_modifier = CivSettings.arenaConfig.getDouble("favored_modifier", 0.2);

            /* Calculate points. */
            int winnerDifference = winner.getLadderPoints() - loser.getLadderPoints();
            int points;

            if (winnerDifference > favored_points) {
                /* Winner was favored. */
                points = (int) (base_points * favored_modifier);
            } else if (winnerDifference > slightly_favored_points) {
                /* Winner was slightly favored. */
                points = (int) (base_points * slightly_favored_modifier);
            } else if (winnerDifference > 0) {
                /* Winner and loser were evenly matched. */
                points = base_points;
            } else if (winnerDifference < -favored_points) {
                /* Loser was favored. */
                points = base_points + (int) (base_points * (1 - favored_modifier));
            } else if (winnerDifference < -slightly_favored_points) {
                /* Loser was slightly favored. */
                points = base_points + (int) (base_points * (1 - slightly_favored_modifier));
            } else {
                points = base_points;
            }

            winner.setLadderPoints(winner.getLadderPoints() + points);
            loser.setLadderPoints(loser.getLadderPoints() - points);

            winner.save();
            loser.save();
            Civilization c = winner.getCivilization();
            c.getTreasury().deposit(c.getCurrentEra() * points * 150);
            Civilization bc = loser.getCivilization();
            bc.getTreasury().deposit(bc.getCurrentEra() * 1500);
            for (Resident r : winner.teamMembers) {
                r.clearRespawnTimeArena();
            }
            for (Resident r : loser.teamMembers) {
                r.clearRespawnTimeArena();
            }
            CivMessage.global(String.valueOf(ChatColor.GREEN) + ChatColor.BOLD + winner.getName() + "(+" + points + ")" + ChatColor.RESET + " defeated " +
                    ChatColor.RED + ChatColor.BOLD + loser.getName() + "(-" + points + ")" + ChatColor.RESET + " in Arena!");

            try {
                ArenaManager.destroyArena(arena.getInstanceName());
            } catch (CivException e) {
                e.printStackTrace();
            }
        }, TimeTools.toTicks(10));
    }

    public static void declareDraw(Arena arena) {
        CivMessage.sendArena(arena, CivSettings.localize.localizedString("arena_leavingIn10"));
        TaskMaster.syncTask(() -> {
            try {
                ArenaManager.destroyArena(arena.getInstanceName());
            } catch (CivException e) {
                e.printStackTrace();
            }
        }, TimeTools.toTicks(10));
    }


}
