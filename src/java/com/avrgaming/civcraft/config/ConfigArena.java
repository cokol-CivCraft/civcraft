package com.avrgaming.civcraft.config;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.util.BlockCoord;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ConfigArena {
    public String id;
    public String name;
    public String world_source;
    public int control_block_hp;
    public LinkedList<ConfigArenaTeam> teams;


    public static void loadConfig(FileConfiguration cfg, Map<String, ConfigArena> config_arenas) {
        config_arenas.clear();

        List<Map<?, ?>> cottage_list = cfg.getMapList("arenas");

        for (Map<?, ?> cl : cottage_list) {
            @SuppressWarnings("unchecked")
            List<Map<?, ?>> teams_map_list = (List<Map<?, ?>>) cl.get("teams");

            ConfigArena arena = new ConfigArena();
            arena.id = (String) cl.get("id");
            arena.name = (String) cl.get("name");
            arena.world_source = (String) cl.get("world_folder");
            arena.control_block_hp = (Integer) cl.get("control_block_hp");

            if (teams_map_list != null) {
                arena.teams = new LinkedList<>();
                for (Map<?, ?> tm : teams_map_list) {
                    ConfigArenaTeam team = new ConfigArenaTeam();

                    team.number = (Integer) tm.get("number");
                    team.name = (String) tm.get("name");

                    /* Set up control blocks. */
                    team.controlPoints = new LinkedList<>();
                    List<?> someList = (List<?>) tm.get("controlblocks");
                    for (Object obj : someList) {
                        String[] coords = ((String) obj).split(",");
                        BlockCoord bcoord = new BlockCoord(arena.world_source,
                                Integer.parseInt(coords[0]),
                                Integer.parseInt(coords[1]),
                                Integer.parseInt(coords[2]));


                        team.controlPoints.add(bcoord);
                    }

                    /* Set up revive points. */
                    team.revivePoints = new LinkedList<>();
                    someList = (List<?>) tm.get("revivepoints");
                    for (Object obj : someList) {
                        String[] coords = ((String) obj).split(",");
                        BlockCoord bcoord = new BlockCoord(arena.world_source,
                                Integer.parseInt(coords[0]),
                                Integer.parseInt(coords[1]),
                                Integer.parseInt(coords[2]));


                        team.revivePoints.add(bcoord);
                    }

                    /* Set up respawn points. */
                    team.respawnPoints = new LinkedList<>();
                    someList = (List<?>) tm.get("respawnpoints");
                    for (Object obj : someList) {
                        String[] coords = ((String) obj).split(",");
                        BlockCoord bcoord = new BlockCoord(arena.world_source,
                                Integer.parseInt(coords[0]),
                                Integer.parseInt(coords[1]),
                                Integer.parseInt(coords[2]));


                        team.respawnPoints.add(bcoord);
                    }

                    /* Set up chest points. */
                    team.chests = new LinkedList<>();
                    someList = (List<?>) tm.get("chests");
                    for (Object obj : someList) {
                        String[] coords = ((String) obj).split(",");
                        BlockCoord bcoord = new BlockCoord(arena.world_source,
                                Integer.parseInt(coords[0]),
                                Integer.parseInt(coords[1]),
                                Integer.parseInt(coords[2]));


                        team.chests.add(bcoord);
                    }

                    String respawnSignStr = (String) tm.get("respawnsign");
                    String[] respawnSplit = respawnSignStr.split(",");
                    team.respawnSign = new BlockCoord(arena.world_source,
                            Integer.parseInt(respawnSplit[0]),
                            Integer.parseInt(respawnSplit[1]),
                            Integer.parseInt(respawnSplit[2]));

                    arena.teams.add(team);
                }
            }


            config_arenas.put(arena.id, arena);
        }

        CivLog.info("Loaded " + config_arenas.size() + " arenas.");
    }
}
