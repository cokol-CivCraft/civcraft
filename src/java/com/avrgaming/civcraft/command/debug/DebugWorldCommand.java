package com.avrgaming.civcraft.command.debug;

import com.avrgaming.civcraft.arena.ArenaManager;
import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigArena;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.ChunkCoord;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;

public class DebugWorldCommand extends CommandBase {

    @Override
    public void init() {
        command = "/dbg world";
        displayName = "Debug World";

        register_sub("create", this::create_cmd, "[name] - creates a new test world with this name.");
        register_sub("tp", this::tp_cmd, "[name] teleports you to spawn at the specified world.");
        register_sub("list", this::list_cmd, "Lists worlds according to bukkit.");
        register_sub("createarena", this::createarena_cmd, "[name] - creates and arena with the given name");
    }

    public void createarena_cmd() throws CivException {
        String name = getNamedString(1, "enter a arena name.");

        ConfigArena arena = CivSettings.arenas.get(name);
        ArenaManager.createArena(arena);
        CivMessage.sendSuccess(sender, "Created arena:" + arena.name);
    }

    public void list_cmd() {
        CivMessage.sendHeading(sender, "Worlds");
        for (World world : Bukkit.getWorlds()) {
            CivMessage.send(sender, world.getName());
        }
    }

    public void create_cmd() throws CivException {
        String name = getNamedString(1, "enter a world name");

        WorldCreator wc = new WorldCreator(name);
        wc.environment(Environment.NORMAL);
        wc.type(WorldType.FLAT);
        wc.generateStructures(false);

        World world = Bukkit.getServer().createWorld(wc);
        world.setSpawnFlags(false, false);
        ChunkCoord.addWorld(world);

        CivMessage.sendSuccess(sender, "World " + name + " created.");

    }

    public void tp_cmd() throws CivException {
        String name = getNamedString(1, "enter a world name");
        Player player = getPlayer();

        World world = Bukkit.getWorld(name);
        player.teleport(world.getSpawnLocation());

        CivMessage.sendSuccess(sender, "Teleported to spawn at world:" + name);
    }


    @Override
    public void doDefaultAction() {
        showHelp();
    }

    @Override
    public void showHelp() {
        showBasicHelp();
    }

    @Override
    public void permissionCheck() {

    }

}
