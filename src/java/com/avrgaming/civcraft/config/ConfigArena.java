package com.avrgaming.civcraft.config;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.util.BlockCoord;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConfigArena {
    public String id;
    public String name;
    public String world_source;
    public int control_block_hp;
    public LinkedList<ConfigArenaTeam> teams;


    public static void loadConfig(FileConfiguration cfg, Map<String, ConfigArena> config_arenas) {
        config_arenas.clear();
        for (File file : Objects.requireNonNull(new File("arenas/").listFiles(File::isDirectory))) {
            ConfigArena arena = new ConfigArena();
            arena.id = file.getName();
            File config_file = new File("arenas/" + arena.id + "/config.yml");
            if (!config_file.exists()) {
                continue;
            }
            FileConfiguration config = YamlConfiguration.loadConfiguration(config_file);
            arena.name = config.getString("name");
            arena.world_source = arena.id;
            arena.control_block_hp = config.getInt("control_block_hp");

            arena.teams = new LinkedList<>();
            for (Map<?, ?> tm : config.getMapList("teams")) {
                ConfigArenaTeam team = new ConfigArenaTeam();

                team.number = (Integer) tm.get("number");
                team.name = (String) tm.get("name");

                /* Set up control blocks. */
                team.controlPoints = new LinkedList<>();
                for (Object obj : (List<?>) tm.get("controlblocks")) {
                    String[] coords = ((String) obj).split(",");
                    BlockCoord bcoord = new BlockCoord(arena.world_source,
                            Integer.parseInt(coords[0]),
                            Integer.parseInt(coords[1]),
                            Integer.parseInt(coords[2]));


                    team.controlPoints.add(bcoord);
                }

                /* Set up revive points. */
                team.revivePoints = new LinkedList<>();
                for (Object obj : (List<?>) tm.get("revivepoints")) {
                    String[] coords = ((String) obj).split(",");
                    BlockCoord bcoord = new BlockCoord(arena.world_source,
                            Integer.parseInt(coords[0]),
                            Integer.parseInt(coords[1]),
                            Integer.parseInt(coords[2]));


                    team.revivePoints.add(bcoord);
                }

                /* Set up respawn points. */
                team.respawnPoints = new LinkedList<>();
                for (Object obj : (List<?>) tm.get("respawnpoints")) {
                    String[] coords = ((String) obj).split(",");
                    BlockCoord bcoord = new BlockCoord(arena.world_source,
                            Integer.parseInt(coords[0]),
                            Integer.parseInt(coords[1]),
                            Integer.parseInt(coords[2]));


                    team.respawnPoints.add(bcoord);
                }

                /* Set up chest points. */
                team.chests = new LinkedList<>();
                for (Object obj : (List<?>) tm.get("chests")) {
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
            config_arenas.put(arena.id, arena);
        }

        CivLog.info("Loaded " + config_arenas.size() + " arenas.");
    }
}
