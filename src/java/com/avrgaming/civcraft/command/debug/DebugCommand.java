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
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.command.debug;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.command.admin.AdminTownCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuff;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.config.ConfigTradeGood;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.event.EventTimer;
import com.avrgaming.civcraft.event.GoodieRepoEvent;
import com.avrgaming.civcraft.exception.AlreadyRegisteredException;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancementSoulBound;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.lorestorage.LoreStoreage;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.*;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.populators.TradeGoodPopulator;
import com.avrgaming.civcraft.siege.Cannon;
import com.avrgaming.civcraft.structure.*;
import com.avrgaming.civcraft.structure.wonders.GrandShipIngermanland;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.tasks.TradeGoodSignCleanupTask;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.template.TemplateStream;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.*;
import com.avrgaming.civcraft.threading.timers.DailyTimer;
import com.avrgaming.civcraft.tutorial.CivTutorial;
import com.avrgaming.civcraft.util.*;
import gpl.AttributeUtil;
import org.bukkit.*;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DebugCommand extends CommandBase {

    @Override
    public void init() {
        command = "/dbg";
        displayName = "Debug";

        register_sub("resident", this::resident_cmd, "[name] - prints out the resident identified by name.");
        register_sub("stopvalidate", this::stopvalidate_cmd, "stops structure validator");
        register_sub("town", this::town_cmd, "[name] - prints out the town identified by name.");
        register_sub("townchunk", this::townchunk_cmd, " gets the town chunk you are standing in and prints it.");
        register_sub("newday", this::newday_cmd, "Runs the new day code, collects taxes ... etc.");
        register_sub("civ", this::civ_cmd, "[name] prints out civ info.");
        register_sub("map", this::map_cmd, "shows a town chunk map of the current area.");
        register_sub("culturechunk", this::culturechunk_cmd, "gets the culture chunk you are standing in and prints it.");
        register_sub("runculture", this::runculture_cmd, "runs the culture process algorithm.");
        register_sub("repo", this::repo_cmd, "repos all goods back to outpost.");
        register_sub("show", this::show_cmd, "shows entity ids in this chunk.");
        register_sub("moveframes", this::moveframes_cmd, "[x] [y] [z] moves item frames in this chunk to x,y,z");
        register_sub("frame", this::frame_cmd, "gets player's town and shows the goodie frames in this town.");
        register_sub("makeframe", this::makeframe_cmd, "[loc] [direction]");
        register_sub("dupe", this::dupe_cmd, "duplicates the item in your hand.");
        register_sub("test", this::test_cmd, "Run test suite commands.");
        register_sub("printgoodie", this::printgoodie_cmd, "[id] - prints the goodie in memory with this id.");
        register_sub("repogoodie", this::repogoodie_cmd, "[id] - repos the goodie with id.");
        register_sub("firework", this::firework_cmd, "fires off a firework here.");
        register_sub("sound", this::sound_cmd, "[name] [pitch]");
        register_sub("arrow", this::arrow_cmd, "[power] change arrow's power.");
        register_sub("processculture", this::processculture_cmd, "forces a culture reprocess");
        register_sub("givebuff", this::givebuff_cmd, "[id] gives this id buff to a town.");
        register_sub("unloadchunk", this::unloadchunk_cmd, "[x] [z] - unloads this chunk.");
        register_sub("setspeed", this::setspeed_cmd, "[speed] - set your speed to this");
        register_sub("tradegenerate", this::tradegenerate_cmd, "generates trade goods at picked locations");
        register_sub("createtradegood", this::createtradegood_cmd, "[good_id] - creates a trade goodie here.");
        register_sub("cleartradesigns", this::cleartradesigns_cmd, "clears extra trade signs above trade outpots");
        register_sub("restoresigns", this::restoresigns_cmd, "restores all structure signs");
        register_sub("regentradegoodchunk", this::regentradegoodchunk_cmd, "regens every chunk that has a trade good in it");
        register_sub("quickcodereload", this::quickcodereload_cmd, "Reloads the quick code plugin");
        register_sub("loadbans", null, "Loads bans from ban list into global table"); // TODO Сокол, не понятно. Удалять?
        register_sub("setallculture", this::setallculture_cmd, "[amount] - sets all towns culture in the world to this amount.");
        register_sub("timers", this::timers_cmd, "show all the timer information.");
        register_sub("shownbt", null, "shows the current nbt data for this item in the logs");
        register_sub("addnbt", null, "adds a custom tag to the item in your hand.");
        register_sub("loretest", this::loretest_cmd, "tests if the magic lore is set.");
        register_sub("loreset", this::loreset_cmd, "adds magic lore tag.");
        register_sub("giveold", this::giveold_cmd, "[name] [first lore]");
        register_sub("farm", this::farm_cmd, "show debug commands for farms");
        register_sub("flashedges", this::flashedges_cmd, "[town] flash edge blocks for town.");
        register_sub("refreshchunk", this::refreshchunk_cmd, "refreshes the chunk you're standing in.. for science.");
        register_sub("touches", this::touches_cmd, "[town] - prints a list of friendly touches for this town's culture.");
        register_sub("listconquered", this::listconquered_cmd, "shows a list of conquered civilizations.");
        register_sub("camp", this::camp_cmd, "Debugs camps.");
        register_sub("blockinfo", this::blockinfo_cmd, "[x] [y] [z] shows block info for this block.");
        register_sub("trommel", this::trommel_cmd, "[name] - turn on this town's trommel debugging.");
        register_sub("quarry", this::quarry_cmd, "[name] - turn on this town's quarry debugging.");
        register_sub("fishery", this::fishery_cmd, "[name] - turn on this town's Fish Hatchery debugging.");
        register_sub("mobgrinder", this::mobgrinder_cmd, "[name] - turn on this town's mob grinder debugging.");
        register_sub("fakeresidents", this::fakeresidents_cmd, "[town] [count] - Adds this many fake residents to a town.");
        register_sub("clearresidents", this::cleartradesigns_cmd, "[town] - clears this town of it's random residents.");
        register_sub("biomehere", this::biomehere_cmd, "- shows you biome info where you're standing.");
        register_sub("scout", this::scout_cmd, "[civ] - enables debugging for scout towers in this civ.");
        register_sub("getit", null, "gives you an item.");
        register_sub("showinv", this::showinv_cmd, "shows you an inventory");
        register_sub("showcraftinv", this::showcraftinv_cmd, "shows you crafting inventory");
        register_sub("setspecial", this::setspecial_cmd, "sets special stuff");
        register_sub("getspecial", this::getspecial_cmd, "gets the special stuff");
        register_sub("setcivnbt", this::setcivnbt_cmd, "[key] [value] - adds this key.");
        register_sub("getcivnbt", this::getcivnbt_cmd, "[key] - gets this key");
        register_sub("getmid", this::getmid_cmd, "Gets the MID of this item.");
        register_sub("getdura", this::getdura_cmd, "gets the durability of an item");
        register_sub("setdura", this::setdura_cmd, "sets the durability of an item");
        register_sub("togglebookcheck", this::togglebookcheck_cmd, "Toggles checking for enchanted books on and off.");
        register_sub("setexposure", this::setexposure_cmd, "[int] sets your exposure to this ammount.");
        register_sub("colorme", this::colorme_cmd, "[hex] adds nbt color value to item held.");
        register_sub("sql", this::sql_cmd, "Show SQLController health info.");
        register_sub("templatetest", this::templatetest_cmd, "tests out some new template stream code.");
        register_sub("buildspawn", this::buildspawn_cmd, "[civname] [capitolname] Builds spawn from spawn template.");
        register_sub("matmap", this::matmap_cmd, "prints the material map.");
        register_sub("ping", this::ping_cmd, "print something.");
        register_sub("datebypass", this::datebypass_cmd, "Bypasses certain date restrictions");
        register_sub("spawn", null, "remote entities test");
        register_sub("heal", this::heal_cmd, "heals you....");
        register_sub("skull", this::skull_cmd, "[player] [title]");
        register_sub("packet", this::packet_cmd, "sends custom auth packet.");
        register_sub("disablemap", this::disablemap_cmd, "disables zan's minimap");
        register_sub("world", this::world_cmd, "Show world debug options");
        register_sub("cannon", this::cannon_cmd, "builds a war cannon.");
        register_sub("saveinv", this::saveinv_cmd, "save an inventory");
        register_sub("restoreinv", this::restoreinv_cmd, "restore your inventory.");
        register_sub("arenainfo", this::arenainfo_cmd, "Shows arena info for this player.");
        register_sub("setnativetown", this::setNativeTown_cmd, "[Town] [Resident]");
        register_sub("seritem", this::seritem_cmd, "serialize ItemStack in hand");
    }

    private void seritem_cmd() throws CivException {
        ItemStack stack = getPlayer().getInventory().getItemInMainHand();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("item", stack);
        getPlayer().sendMessage("in console");
        CivLog.info("\n" + yaml.saveToString());
    }

    public void stopvalidate_cmd() {
        if (!CivCraft.getIsValidate()) {
            CivCraft.setIsValidate(true);
            CivMessage.sendSuccess(sender, "StructureValidator is turned ON!");
        } else {
            CivCraft.setIsValidate(false);
            CivMessage.sendError(sender, "StructureValidator is turned OFF!");
        }
    }

    public void arenainfo_cmd() throws CivException {
        Resident resident = getResident();
        String arenaName = "";

        if (resident.getTeam() != null && resident.getTeam().getCurrentArena() != null) {
            arenaName = resident.getTeam().getCurrentArena().getInstanceName();
        }


        CivMessage.send(sender, "InsideArena:" + resident.isInsideArena() + " Team Active arena:" + arenaName);
    }

    public void saveinv_cmd() throws CivException {
        Resident resident = getResident();
        resident.saveInventory();
        CivMessage.sendSuccess(resident, "saved inventory.");
    }

    public void restoreinv_cmd() throws CivException {
        Resident resident = getResident();
        resident.restoreInventory();
        CivMessage.sendSuccess(resident, "restore inventory.");
    }

    public void cannon_cmd() throws CivException {
        Resident resident = getResident();
        Cannon.newCannon(resident, CivGlobal.getPlayer(resident).getLocation());

        CivMessage.sendSuccess(resident, "built cannon.");
    }

    public void world_cmd() {
        DebugWorldCommand cmd = new DebugWorldCommand();
        cmd.onCommand(sender, null, "world", this.stripArgs(args, 1));
    }

    public void disablemap_cmd() throws CivException {
        Player player = getPlayer();
        player.sendMessage("�3�6�3�6�3�6�e");
        player.sendMessage("�3�6�3�6�3�6�d");
        CivMessage.sendSuccess(player, "Disabled.");
    }

    public void packet_cmd() throws CivException {
        Player player = getPlayer();
        player.sendPluginMessage(CivCraft.getPlugin(), "CAC", "Test Message".getBytes());
        CivMessage.sendSuccess(player, "Sent test message");
    }


    public void skull_cmd() throws CivException {
        Player player = getPlayer();
        String playerName = getNamedString(1, "Enter a player name");
        String message = getNamedString(2, "Enter a title.");

        ItemStack skull = ItemManager.spawnPlayerHead(playerName, message);
        player.getInventory().addItem(skull);
        CivMessage.sendSuccess(player, "Added skull item.");
    }

    public void heal_cmd() throws CivException {
        Player player = getPlayer();
        double maxHP = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        player.setHealth(maxHP);
        player.setFoodLevel(50);
        CivMessage.send(player, "Healed....");
    }

    public void datebypass_cmd() {
        CivGlobal.debugDateBypass = !CivGlobal.debugDateBypass;
        CivMessage.send(sender, "Date bypass is now:" + CivGlobal.debugDateBypass);
    }

    public void ping_cmd() {
        CivMessage.send(sender, "test....");
    }

    public void matmap_cmd() throws CivException {
        Player player = getPlayer();
        for (String mid : LoreMaterial.materialMap.keySet()) {
            CivMessage.send(player, mid);
            LoreMaterial mat = LoreMaterial.materialMap.get(mid);
            CivLog.info("material map:" + mid + " mat:" + mat);
        }

    }

    public void buildspawn_cmd() throws CivException {
        /* First create a new Civilization and spawn capitol */
        String civName = getNamedString(1, "Enter a Civ name/");
        String capitolName = getNamedString(2, "Enter a capitol name.");
        Resident resident = getResident();

        try {
            /* Build a spawn civ. */
            Civilization spawnCiv = new Civilization(civName, capitolName, resident);
            spawnCiv.saveNow();

            /* Build a spawn capitol */
            Town spawnCapitol = new Town(capitolName, resident, spawnCiv);
            spawnCapitol.saveNow();

            PermissionGroup leaders = new PermissionGroup(spawnCiv, "leaders");
            spawnCiv.addGroup(leaders);
            leaders.addMember(resident);
            spawnCiv.setLeader(resident);
            spawnCiv.setLeaderGroup(leaders);
            leaders.save();

            PermissionGroup advisers = new PermissionGroup(spawnCiv, "advisers");
            spawnCiv.addGroup(advisers);
            spawnCiv.setAdviserGroup(advisers);
            advisers.save();

            PermissionGroup mayors = new PermissionGroup(spawnCapitol, "mayors");
            spawnCapitol.addGroup(mayors);
            spawnCapitol.setMayorGroup(mayors);
            mayors.addMember(resident);
            mayors.save();

            PermissionGroup assistants = new PermissionGroup(spawnCapitol, "assistants");
            spawnCapitol.addGroup(assistants);
            spawnCapitol.setAssistantGroup(assistants);
            assistants.save();

            PermissionGroup residents = new PermissionGroup(spawnCapitol, "residents");
            spawnCapitol.addGroup(residents);
            spawnCapitol.setDefaultGroup(residents);
            residents.save();

            spawnCiv.addTown(spawnCapitol);
            spawnCiv.setCapitolName(spawnCapitol.getName());

            spawnCiv.setAdminCiv(true);
            spawnCiv.save();
            spawnCapitol.save();
            resident.save();

            CivGlobal.addTown(spawnCapitol);
            CivGlobal.addCiv(spawnCiv);

            /* Setup leader and adivsers groups. */
            try {
                spawnCapitol.addResident(resident);
            } catch (AlreadyRegisteredException e) {
                e.printStackTrace();
                return;
            }

            TaskMaster.syncTask(() -> {
                try {

                    /* Initialize the spawn template */
                    Template tpl = new Template();
                    try {
                        tpl.load_template("templates/spawn.def");
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new CivException("IO Error.");
                    }

                    ConfigBuildableInfo info = new ConfigBuildableInfo();
                    info.tile_improvement = false;
                    info.templateYShift = 0;
                    Location center = Buildable.repositionCenterStatic(getPlayer().getLocation(), info,
                            Template.getDirection(getPlayer().getLocation()), tpl.size_x, tpl.size_z);

                    CivMessage.send(sender, "Building from " + 0 + "," + 0 + "," + 0);
                    for (int y = 0; y < tpl.size_y; y++) {
                        for (int x = 0; x < tpl.size_x; x++) {
                            for (int z = 0; z < tpl.size_z; z++) {
                                BlockCoord next = new BlockCoord(center);
                                next.setX(next.getX() + x);
                                next.setY(next.getY() + y);
                                next.setZ(next.getZ() + z);

                                SimpleBlock sb = tpl.blocks[x][y][z];

                                switch (sb.specialType) {
                                    case COMMAND -> {
                                        String buildableName = sb.command.replace("/", "");
                                        info = null;
                                        for (ConfigBuildableInfo buildInfo : CivSettings.structures.values()) {
                                            if (buildInfo.displayName.equalsIgnoreCase(buildableName)) {
                                                info = buildInfo;
                                                break;
                                            }
                                        }
                                        if (info == null) {
                                            try {
                                                new SimpleBlock(Material.AIR, 0).setTo(next.getBlock());
                                                continue;
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                continue;
                                            }
                                        }
                                        CivMessage.send(sender, "Setting up " + buildableName);
                                        String[] split = sb.getKeyValueString().split(",")[0].split(":");
                                        String dir = split[0];
                                        int yShift = Integer.parseInt(split[1]);
                                        Location loc = next.getLocation();
                                        loc.setY(loc.getY() + yShift);
                                        Structure struct = (Structure) MetaStructure.newStructOrWonder(loc, info, spawnCapitol);
                                        if (struct instanceof Capitol) {
                                            AdminTownCommand.claimradius(spawnCapitol, center, 15);
                                        }
                                        struct.setTemplateName("templates/themes/medieval/" + info.template_base_name + ".def");
                                        struct.bindStructureBlocks();
                                        struct.setComplete(true);
                                        struct.setHitpoints(info.max_hp);
                                        CivGlobal.addStructure(struct);
                                        spawnCapitol.addStructure(struct);
                                        Template tplStruct;
                                        try {
                                            tplStruct = Template.getTemplate(struct.getSavedTemplatePath(), null);
                                            TaskMaster.syncTask(new PostBuildSyncTask(tplStruct, struct));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            throw new CivException("IO Exception.");
                                        }
                                        struct.save();
                                        spawnCapitol.save();
                                    }
                                    case LITERAL -> {
                                        try {
                                            sb.setTo(next.getLocation().getBlock());
                                            Sign s = (Sign) next.getBlock().getState();
                                            for (int j = 0; j < 4; j++) {
                                                s.setLine(j, sb.message[j]);
                                            }

                                            s.update(false, false);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    default -> {
                                        try {
                                            sb.setTo(next.getLocation().getBlock());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                    }

                    CivMessage.send(sender, "Finished building.");

                    spawnCapitol.addAccumulatedCulture(60000000);
                    spawnCapitol.save();

                } catch (CivException e) {
                    e.printStackTrace();
                    CivMessage.send(sender, e.getMessage());
                }
            });
        } catch (InvalidNameException e) {
            throw new CivException(e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CivException("Internal DB Error.");
        }

    }

    public static TemplateStream tplStream = null;

    public void templatetest_cmd() throws CivException {
        Player player = getPlayer();
        String filename = getNamedString(1, "Enter a filename");
        Integer yLayer = getNamedInteger(2);

        if (tplStream == null) {
            try {
                tplStream = new TemplateStream(filename);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        try {
            tplStream.getBlocksForLayer(yLayer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        tplStream.debugBuildBlocksHere(player.getLocation());

    }

    public void sql_cmd() {
        HashMap<String, String> stats = new HashMap<>();
        CivMessage.send(sender, "--------------------------");

        for (String key : SQLUpdate.statSaveRequests.keySet()) {
            Integer requests = SQLUpdate.statSaveRequests.get(key);
            Integer completes = SQLUpdate.statSaveCompletions.get(key);

            if (requests == null) {
                requests = 0;
            }

            if (completes == null) {
                completes = 0;
            }

            CivMessage.send(sender, key + " requested:" + requests + " completed:" + completes);
        }

        CivMessage.send(sender, makeInfoString(stats, ChatColor.DARK_GREEN, ChatColor.GREEN));
    }

    public void colorme_cmd() throws CivException {
        Player player = getPlayer();
        String hex = getNamedString(1, "color code");
        long value = Long.decode(hex);

        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand == null || inHand.getType() == Material.AIR) {
            throw new CivException("please have an item in your hand.");
        }

        AttributeUtil attrs = new AttributeUtil(inHand);
        attrs.setColor(value);
        player.getInventory().setItemInMainHand(attrs.getStack());
        CivMessage.sendSuccess(player, "Set color.");
    }

    public void setexposure_cmd() throws CivException {
        Resident resident = getResident();
        Player player = getPlayer();
        Double exp = getNamedDouble(1);
        resident.setSpyExposure(exp);

        CivMessage.sendSuccess(player, "Set Exposure.");
    }

    public void togglebookcheck_cmd() {
        CivGlobal.checkForBooks = !CivGlobal.checkForBooks;
        CivMessage.sendSuccess(sender, "Check for books is:" + CivGlobal.checkForBooks);
    }

    public void setcivnbt_cmd() throws CivException {
        Player player = getPlayer();
        String key = getNamedString(1, "key");
        String value = getNamedString(2, "value");

        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand == null) {
            throw new CivException("You must have an item in hand.");
        }

        AttributeUtil attrs = new AttributeUtil(inHand);
        attrs.setCivCraftProperty(key, value);
        player.getInventory().setItemInMainHand(attrs.getStack());
        CivMessage.sendSuccess(player, "Set property.");

    }

    public void getcivnbt_cmd() throws CivException {
        Player player = getPlayer();
        String key = getNamedString(1, "key");

        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand == null) {
            throw new CivException("You must have an item in hand.");
        }

        AttributeUtil attrs = new AttributeUtil(inHand);
        String value = attrs.getCivCraftProperty(key);
        CivMessage.sendSuccess(player, "got property:" + value);

    }

    public void getdura_cmd() throws CivException {
        Player player = getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();
        CivMessage.send(player, "Durability:" + inHand.getDurability());
        CivMessage.send(player, "MaxDura:" + inHand.getType().getMaxDurability());

    }

    public void setdura_cmd() throws CivException {
        Player player = getPlayer();
        Integer dura = getNamedInteger(1);

        ItemStack inHand = player.getInventory().getItemInMainHand();
        inHand.setDurability(dura.shortValue());

        CivMessage.send(player, "Set Durability:" + inHand.getDurability());
        CivMessage.send(player, "MaxDura:" + inHand.getType().getMaxDurability());

    }

    public void getmid_cmd() throws CivException {
        Player player = getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand == null) {
            throw new CivException("You need an item in your hand.");
        }

        CivMessage.send(player, "MID:" + LoreMaterial.getMID(inHand));
    }

    public void setspecial_cmd() throws CivException {
        Player player = getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand == null) {
            throw new CivException("You need an item in your hand.");
        }

        //	AttributeUtil attrs = new AttributeUtil(inHand);
        //	attrs.setCivCraftProperty("customId", "testMyCustomId");
        ItemStack stack = LoreMaterial.addEnhancement(inHand, new LoreEnhancementSoulBound());
        player.getInventory().setItemInMainHand(stack);
        CivMessage.send(player, "Set it.");
    }

    public void getspecial_cmd() throws CivException {
        Player player = getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand == null) {
            throw new CivException("You need an item in your hand.");
        }

        AttributeUtil attrs = new AttributeUtil(inHand);
        String value = attrs.getCivCraftProperty("soulbound");

        CivMessage.send(player, "Got:" + value);
    }

    public void showinv_cmd() throws CivException {
        CivTutorial.spawnGuiBook(getPlayer());
    }

    public void showcraftinv_cmd() throws CivException {
        CivTutorial.showCraftingHelp(getPlayer());
    }

    public void scout_cmd() throws CivException {
        Civilization civ = getNamedCiv(1);

        if (!civ.scoutDebug) {
            civ.scoutDebug = true;
            civ.scoutDebugPlayer = getPlayer().getName();
            CivMessage.sendSuccess(sender, "Enabled scout tower debugging in " + civ.getName());
        } else {
            civ.scoutDebug = false;
            civ.scoutDebugPlayer = null;
            CivMessage.sendSuccess(sender, "Disabled scout tower debugging in " + civ.getName());
        }
    }

    public void biomehere_cmd() throws CivException {
        Player player = getPlayer();

        Biome biome = player.getWorld().getBiome(player.getLocation().getBlockX(), player.getLocation().getBlockZ());
        CivMessage.send(player, "Got biome:" + biome.name());
    }

    public void clearresidents_cmd() throws CivException {
        Town town = getNamedTown(1);

        ArrayList<Resident> removeUs = new ArrayList<>();
        for (Resident resident : town.getResidents()) {
            if (resident.getName().startsWith("RANDOM_")) {
                removeUs.add(resident);
            }
        }

        for (Resident resident : removeUs) {
            town.removeResident(resident);
        }
    }

    public void fakeresidents_cmd() throws CivException {
        Town town = getNamedTown(1);
        Integer count = getNamedInteger(2);

        for (int i = 0; i < count; i++) {
            SecureRandom random = new SecureRandom();
            String name = (new BigInteger(130, random).toString(32));

            try {

                Resident fake = new Resident(UUID.randomUUID(), "RANDOM_" + name);
                town.addResident(fake);
                town.addFakeResident(fake);
            } catch (AlreadyRegisteredException | InvalidNameException e) {
                //ignore
            }
        }
        CivMessage.sendSuccess(sender, "Added " + count + " residents.");
    }

    public void trommel_cmd() throws CivException {
        Town town = getNamedTown(1);

        if (TrommelAsyncTask.debugTowns.contains(town.getName())) {
            TrommelAsyncTask.debugTowns.remove(town.getName());
        } else {
            TrommelAsyncTask.debugTowns.add(town.getName());
        }

        CivMessage.send(sender, "Trommel toggled.");
    }

    public void quarry_cmd() throws CivException {
        Town town = getNamedTown(1);

        if (QuarryAsyncTask.debugTowns.contains(town.getName())) {
            QuarryAsyncTask.debugTowns.remove(town.getName());
        } else {
            QuarryAsyncTask.debugTowns.add(town.getName());
        }

        CivMessage.send(sender, "Quarry toggled.");
    }

    public void fishery_cmd() throws CivException {
        Town town = getNamedTown(1);

        if (FisheryAsyncTask.debugTowns.contains(town.getName())) {
            FisheryAsyncTask.debugTowns.remove(town.getName());
        } else {
            FisheryAsyncTask.debugTowns.add(town.getName());
        }

        CivMessage.send(sender, "Fish Hatchery toggled.");
    }

    public void mobgrinder_cmd() throws CivException {
        Town town = getNamedTown(1);

        if (MobGrinderAsyncTask.debugTowns.contains(town.getName())) {
            MobGrinderAsyncTask.debugTowns.remove(town.getName());
        } else {
            MobGrinderAsyncTask.debugTowns.add(town.getName());
        }

        CivMessage.send(sender, "Mob Grinder toggled.");
    }

    public void blockinfo_cmd() throws CivException {
        int x = getNamedInteger(1);
        int y = getNamedInteger(2);
        int z = getNamedInteger(3);

        Block b = Bukkit.getWorld("world").getBlockAt(x, y, z);

        CivMessage.send(sender, "type:" + b.getType() + " data:" + b.getData() + " name:" + b.getType().name());

    }

    public void camp_cmd() {
        DebugCampCommand cmd = new DebugCampCommand();
        cmd.onCommand(sender, null, "farm", this.stripArgs(args, 1));
    }

    public void listconquered_cmd() {
        CivMessage.sendHeading(sender, "Conquered Civs");
        StringBuilder out = new StringBuilder();
        for (Civilization civ : CivGlobal.getConqueredCivs()) {
            out.append(civ.getName()).append(", ");
        }
        CivMessage.send(sender, out.toString());
    }

    public void touches_cmd() throws CivException {
        Town town = getNamedTown(1);

        CivMessage.sendHeading(sender, "Touching Towns");
        StringBuilder out = new StringBuilder();
        for (Town t : town.townTouchList) {
            out.append(t.getName()).append(", ");
        }

        if (town.touchesCapitolCulture(new HashSet<>())) {
            CivMessage.send(sender, ChatColor.GREEN + "Touches capitol.");
        } else {
            CivMessage.send(sender, ChatColor.RED + "Does NOT touch capitol.");
        }

        CivMessage.send(sender, out.toString());
    }

    public void refreshchunk_cmd() throws CivException {
        Player you = getPlayer();
        ChunkCoord coord = new ChunkCoord(you.getLocation());

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getWorld().unloadChunk(coord.getX(), coord.getZ());
            player.getWorld().loadChunk(coord.getX(), coord.getZ());
        }
    }

    public void flashedges_cmd() throws CivException {
        Town town = getNamedTown(1);

        for (TownChunk chunk : town.savedEdgeBlocks) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    Block b = Bukkit.getWorld("world").getHighestBlockAt(((chunk.getChunkCoord().getX() + x << 4) + x),
                            ((chunk.getChunkCoord().getZ() << 4) + z));
                    Bukkit.getWorld("world").playEffect(b.getLocation(), Effect.MOBSPAWNER_FLAMES, 1);
                }
            }
        }
        CivMessage.sendSuccess(sender, "flashed");
    }

    public void farm_cmd() {
        DebugFarmCommand cmd = new DebugFarmCommand();
        cmd.onCommand(sender, null, "farm", this.stripArgs(args, 1));
    }

    public void giveold_cmd() throws CivException {
        Player player = getPlayer();

        if (args.length < 3) {
            throw new CivException("Enter name and first lore line.");
        }

        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand == null) {
            return;
        }


        ItemMeta meta = inHand.getItemMeta();
        meta.setDisplayName(args[1]);

        ArrayList<String> lore = new ArrayList<>();
        lore.add(this.combineArgs(this.stripArgs(args, 2)));
        meta.setLore(lore);

        inHand.setItemMeta(meta);


//			HashMap<String, String> loremap = new HashMap<String, String>();
//
//			loremap.put("outpost", "world,-513,65,2444");
//			loremap.put("town", "Arendal");
//			loremap.put("expires", "6/14/2013 2:00PM PDT");
//
//			LoreStoreage.saveLoreMap("Trade Goodie", loremap, inHand);
//			LoreStoreage.setItemName("Pelts", inHand);
        //	LoreStoreage.setMatID(1337, inHand);
    }

    public void loretest_cmd() throws CivException {
        Player player = getPlayer();

        org.bukkit.inventory.ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand != null) {
            ItemMeta meta = inHand.getItemMeta();
            List<String> newLore = meta.getLore();
            if (newLore != null && !newLore.isEmpty() && newLore.get(0).equalsIgnoreCase("RJMAGIC")) {
                CivMessage.sendSuccess(player, "found magic lore");
            } else {
                CivMessage.sendSuccess(player, "No magic lore.");
            }
        }
    }

    public void loreset_cmd() throws CivException {
        Player player = getPlayer();

        org.bukkit.inventory.ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand != null) {
//			HashMap<String, String> loremap = new HashMap<String, String>();
//			
//			loremap.put("outpost", "world,-513,65,2444");
//			loremap.put("town", "Arendal");
//			loremap.put("expires", "6/14/2013 2:00PM PDT");
//		
//			LoreStoreage.saveLoreMap("Trade Goodie", loremap, inHand);
//			LoreStoreage.setItemName("Pelts", inHand);
            LoreStoreage.setMatID(1337, inHand);
        }
    }

//	public void shownbt_cmd() throws CivException {
//		Player player = getPlayer();
//		
//		org.bukkit.inventory.ItemStack inHand = player.getInventory().getItemInMainHand();
//		if (inHand != null) {
//			NBT.debugPrintItemTags(inHand);
//		}
//	}

    //	public void addnbt_cmd() throws CivException {
//		Player player = getPlayer();
//		
//		org.bukkit.inventory.ItemStack inHand = player.getInventory().getItemInMainHand();
//		if (inHand != null) {
//			NBT.addCustomTag("RJTEST", 1337, inHand);
//		}
//	}
    public void timers_cmd() {

        CivMessage.sendHeading(sender, "Timers");
        SimpleDateFormat sdf = new SimpleDateFormat("M/dd h:mm:ss a z");

        CivMessage.send(sender, "Now:" + sdf.format(new Date()));
        for (EventTimer timer : EventTimer.timers.values()) {


            CivMessage.send(sender, timer.getName());
            CivMessage.send(sender, "    next:" + sdf.format(timer.getNext()));
            if (timer.getLast().getTime().getTime() == 0) {
                CivMessage.send(sender, "    last: never");
            } else {
                CivMessage.send(sender, "    last:" + sdf.format(timer.getLast()));
            }

        }

    }

    public void setallculture_cmd() throws CivException {
        Integer culture = getNamedInteger(1);

        for (Town town : CivGlobal.getTowns()) {
            town.addAccumulatedCulture(culture);
            town.save();
        }

        CivGlobal.processCulture();
        CivMessage.sendSuccess(sender, "Set all town culture to " + culture + " points.");
    }

    public void quickcodereload_cmd() {

        Bukkit.getPluginManager().getPlugin("QuickCode");


    }

    public void regentradegoodchunk_cmd() {

        World world = Bukkit.getWorld("world");

        for (ChunkCoord coord : CivGlobal.tradeGoodPreGenerator.goodPicks.keySet()) {

            world.regenerateChunk(coord.getX(), coord.getZ());
            CivMessage.send(sender, "Regened:" + coord);
        }
    }

    public void restoresigns_cmd() {

        CivMessage.send(sender, "restoring....");
        for (StructureSign sign : CivGlobal.getStructureSigns()) {

            BlockCoord bcoord = sign.getCoord();
            Block block = bcoord.getBlock();
            block.getState().setData(new org.bukkit.material.Sign(Material.WALL_SIGN));
            block.setData((byte) sign.getDirection());

            Sign s = (Sign) block.getState();
            String[] lines = sign.getText().split("\n");

            if (lines.length > 0) {
                s.setLine(0, lines[0]);
            }
            if (lines.length > 1) {
                s.setLine(1, lines[1]);
            }

            if (lines.length > 2) {
                s.setLine(2, lines[2]);
            }

            if (lines.length > 3) {
                s.setLine(3, lines[3]);
            }
            s.update();


        }


    }

    public void cleartradesigns_cmd() throws CivException {
        CivMessage.send(sender, "Starting task");

        if (args.length < 3) {
            throw new CivException("bad arg count");
        }

        try {
            int xoff = Integer.parseInt(args[1]);
            int zoff = Integer.parseInt(args[2]);
            TaskMaster.syncTask(new TradeGoodSignCleanupTask(getPlayer().getName(), xoff, zoff));

        } catch (NumberFormatException e) {
            throw new CivException("Bad number format");
        }

    }

    public void tradegenerate_cmd() {
        String playerName;

        if (sender instanceof Player) {
            playerName = sender.getName();
        } else {
            playerName = null;
        }

        CivMessage.send(sender, "Starting Trade Generation task...");
        TaskMaster.asyncTask(new TradeGoodPostGenTask(playerName, 0), 0);
    }

    public void createtradegood_cmd() throws CivException {
        if (args.length < 2) {
            throw new CivException("Enter trade goodie id");
        }

        ConfigTradeGood good = CivSettings.goods.get(args[1]);
        if (good == null) {
            throw new CivException("Unknown trade good id:" + args[1]);
        }

        BlockCoord coord = new BlockCoord(getPlayer().getLocation());
        TradeGoodPopulator.buildTradeGoodie(good, coord, getPlayer().getLocation().getWorld(), false);
        CivMessage.sendSuccess(sender, "Created a " + good.name + " here.");
    }

    public void generate_cmd() throws CivException {
        if (args.length < 5) {
            throw new CivException("Enter chunk coords to generate.");
        }

        try {
            int startX = Integer.parseInt(args[1]);
            int startZ = Integer.parseInt(args[2]);
            int stopX = Integer.parseInt(args[3]);
            int stopZ = Integer.parseInt(args[4]);

            TaskMaster.syncTask(new ChunkGenerateTask(startX, startZ, stopX, stopZ));

        } catch (NumberFormatException e) {
            throw new CivException(e.getMessage());
        }


    }

    public void setspeed_cmd() throws CivException {
        Player player = getPlayer();

        if (args.length < 2) {
            throw new CivException("Enter a speed.");
        }

        player.setWalkSpeed(Float.parseFloat(args[1]));
        CivMessage.sendSuccess(player, "speed changed");
    }

    public void unloadchunk_cmd() throws CivException {
        if (args.length < 3) {
            throw new CivException("Enter an x and z");
        }

        this.getPlayer().getWorld().unloadChunk(Integer.parseInt(args[1]), Integer.parseInt(args[2]));

        CivMessage.sendSuccess(sender, "unloaded.");
    }

    public void givebuff_cmd() throws CivException {
        if (args.length < 2) {
            throw new CivException("Enter the buff id");
        }

        ConfigBuff buff = CivSettings.buffs.get(args[1]);
        if (buff == null) {
            throw new CivException("No buff id:" + args[1]);
        }

        getSelectedTown().getBuffManager().addBuff(buff.id, buff.id, "Debug");
        CivMessage.sendSuccess(sender, "Gave buff " + buff.name + " to town");
    }

    public void processculture_cmd() {
        CivGlobal.processCulture();
        CivMessage.sendSuccess(sender, "Forced process of culture");
    }

    public void arrow_cmd() throws CivException {
        if (args.length < 2) {
            throw new CivException("/arrow [power]");
        }


        for (Town town : CivGlobal.getTowns()) {
            for (Structure struct : town.getStructures()) {
                if (struct instanceof ArrowTower) {
                    ((ArrowTower) struct).setPower(Float.parseFloat(args[1]));
                }
            }
            for (Wonder wonder : town.getWonders()) {
                if (wonder instanceof GrandShipIngermanland) {
                    ((GrandShipIngermanland) wonder).setArrorPower(Float.parseFloat(args[1]));
                }
            }
        }


    }

    public void sound_cmd() throws CivException {
        Player player = getPlayer();

        if (args.length < 3) {
            throw new CivException("Enter sound enum name and pitch.");
        }

        player.getWorld().playSound(player.getLocation(), Sound.valueOf(args[1].toUpperCase()), 1.0f, Float.parseFloat(args[2]));
    }

    public void firework_cmd() throws CivException {
        Player player = getPlayer();

        FireworkEffectPlayer fw = new FireworkEffectPlayer();
        try {
            fw.playFirework(player.getWorld(), player.getLocation(), FireworkEffect.builder().withColor(Color.RED).flicker(true).with(Type.BURST).build());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void repogoodie_cmd() throws CivException {
        if (args.length < 2) {
            throw new CivException("Enter the id of the goodie you want to repo.");
        }

        for (BonusGoodie goodie : CivGlobal.getBonusGoodies()) {
            if (goodie.getId() == Integer.parseInt(args[1])) {
                CivMessage.send(sender, "Repo'd Goodie " + goodie.getId() + " (" + goodie.getDisplayName() + ")");
                goodie.replenish();
                return;
            }
        }


    }

    public void printgoodie_cmd() throws CivException {

        if (args.length < 2) {
            throw new CivException("Enter the id of the goodie you want to inspect.");
        }

        for (BonusGoodie goodie : CivGlobal.getBonusGoodies()) {
            if (goodie.getId() != Integer.parseInt(args[1])) {
                continue;
            }
            CivMessage.sendHeading(sender, "Goodie " + goodie.getId() + " (" + goodie.getDisplayName() + ")");

            if (goodie.getItem() != null) {
                CivMessage.send(sender, "Item: " + goodie.getItem().getUniqueId() + " loc:" + goodie.getItem().getLocation());
            } else {
                CivMessage.send(sender, "Item: null");
            }

            if (goodie.getFrame() != null) {
                CivMessage.send(sender, "Frame: " + goodie.getFrame().getUUID() + " loc:" + goodie.getFrame().getLocation());
            } else {
                CivMessage.send(sender, "Frame: null");
            }

            if (goodie.getHolder() != null) {
                CivMessage.send(sender, "Holder: " + goodie.getHolder().toString());
            } else {
                CivMessage.send(sender, "holder: null");
            }

            org.bukkit.inventory.ItemStack stack = goodie.getStack();
            if (stack != null) {
                CivMessage.send(sender, "Stack: " + stack);
            } else {
                CivMessage.send(sender, "Stack: null");
            }
            return;
        }
        CivMessage.send(sender, "No goodie found.");
    }

    public void test_cmd() {
        DebugTestCommand cmd = new DebugTestCommand();
        cmd.onCommand(sender, null, "test", this.stripArgs(args, 1));
    }

    public void dupe_cmd() throws CivException {
        Player player = getPlayer();

        ItemStack stack = player.getInventory().getItemInMainHand();
        if (player.getInventory().getItemInMainHand() == null || stack.getType() == Material.AIR) {
            throw new CivException("No item in hand.");
        }

        player.getInventory().addItem(player.getInventory().getItemInMainHand());
        CivMessage.sendSuccess(player, player.getInventory().getItemInMainHand().getType().name() + "duplicated.");
    }

    public void makeframe_cmd() throws CivException {
        if (args.length > 3) {
            throw new CivException("Provide a x,y,z and a direction (n,s,e,w)");
        }

        String locationString = "world," + args[1];
        BlockFace face = switch (args[2]) {
            case "n" -> BlockFace.NORTH;
            case "s" -> BlockFace.SOUTH;
            case "e" -> BlockFace.EAST;
            case "w" -> BlockFace.WEST;
            default -> throw new CivException("Invalid direction, use n,s,e,w");
        };

        Location loc = CivGlobal.getLocationFromHash(locationString);
        new ItemFrameStorage(loc, face);
        CivMessage.send(sender, "Created frame.");
    }

    public void show_cmd() throws CivException {
        Player player = getPlayer();
        Chunk chunk = player.getLocation().getChunk();

        for (Entity entity : chunk.getEntities()) {
            CivMessage.send(player, "E:" + entity.getType().name() + " UUID:" + entity.getUniqueId().toString());
            CivLog.info("E:" + entity.getType().name() + " UUID:" + entity.getUniqueId().toString());
        }
    }

    public void moveframes_cmd() throws CivException {
        Player player = getPlayer();
        Chunk chunk = player.getLocation().getChunk();

        //	int x = this.getNamedInteger(1);
        //	int y = this.getNamedInteger(2);
        //	int z = this.getNamedInteger(3);

        //	Location loc = new Location(player.getWorld(), x, y, z);

        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof ItemFrame) {
                CivMessage.send(sender, "Teleported...");
                entity.teleport(entity.getLocation());
            }
        }

    }

    public void frame_cmd() throws CivException {
        Town town = getSelectedTown();

        TownHall townhall = town.getTownHall();
        if (townhall == null) {
            throw new CivException("No town hall?");
        }

        for (ItemFrameStorage itemstore : townhall.getGoodieFrames()) {
            String itemString = "empty";

            if (!itemstore.isEmpty()) {
                BonusGoodie goodie = CivGlobal.getBonusGoodie(itemstore.getItem());
                itemString = goodie.getDisplayName();
            }
            CivMessage.send(sender, "GoodieFrame UUID:" + itemstore.getUUID() + " item:" + itemString);
        }

    }

    public void repo_cmd() {
        GoodieRepoEvent.repoProcess();
    }

    public void culturechunk_cmd() {
        if (sender instanceof Player player) {

            CultureChunk cc = CivGlobal.getCultureChunk(player.getLocation());

            if (cc == null) {
                CivMessage.send(sender, "No culture chunk found here.");
                return;
            }

            CivMessage.send(sender, "loc:" + cc.getChunkCoord() + " town:" + cc.getTown().getName() + " civ:" + cc.getCiv().getName() +
                    " distanceToNearest:" + cc.getDistanceToNearestEdge(cc.getTown().savedEdgeBlocks));
        }
    }

    public void runculture_cmd() {
        TaskMaster.asyncTask("cultureProcess", new CultureProcessAsyncTask(), 0);
        CivMessage.sendSuccess(sender, "Processed culture.");
    }

    //	public void addculture_cmd() throws CivException {
//		if (args.length < 3) {
//			throw new CivException("enter the town, then level you want to set.");
//		}
//		
//		Town town = getNamedTown(1);
//
//		try {
//			
//			int level = Integer.valueOf(args[2]);
//			town.addCulture(level);
//			CivMessage.sendSuccess(sender, "Added "+args[2]+" culture to town "+args[1]);
//		} catch (NumberFormatException e) {
//			throw new CivException(args[2]+" is not a number.");
//		}
//		
//	}
    public void map_cmd() throws CivException {
        Player player = getPlayer();

        CivMessage.send(player, AsciiMap.getMapAsString(player.getLocation()));

    }

    public void civ_cmd() throws CivException {
        if (args.length < 2) {
            throw new CivException("Specify a civ name.");
        }

        Civilization civ = getNamedCiv(1);

        CivMessage.sendHeading(sender, "Civ " + civ.getName());
        CivMessage.send(sender, "id:" + civ.getUUID() + " debt: " + civ.getTreasury().getDebt() + " balance:" + civ.getTreasury().getBalance());
    }

    public void newday_cmd() {
        CivMessage.send(sender, "Starting a new day...");
        TaskMaster.syncTask(new DailyTimer(), 0);
    }

    @Override
    public void showHelp() {
        showBasicHelp();
    }

    public void town_cmd() throws CivException {
        if (args.length < 2) {
            CivMessage.sendError(sender, "Specifiy a town name.");
            return;
        }

        Town town = getNamedTown(1);

        CivMessage.sendHeading(sender, "Town " + town.getName());
        CivMessage.send(sender, "id:" + town.getUUID() + " level: " + town.getLevel());

    }

    public void townchunk_cmd() {
        if (sender instanceof Player player) {

            TownChunk tc = CivGlobal.getTownChunk(player.getLocation());

            if (tc == null) {
                CivMessage.send(sender, "No town chunk found here.");
                return;
            }

            CivMessage.send(sender, "id:" + tc.getId() + " coord:" + tc.getChunkCoord());
        }
    }

    public void resident_cmd() throws CivException {
        if (args.length < 2) {
            CivMessage.sendError(sender, "Specifiy a resident name.");
            return;
        }

        Resident res = getNamedResident(1);

        CivMessage.sendHeading(sender, "Resident " + res.getName());
        CivMessage.send(sender, "id: " + res.getId() + " lastOnline: " + res.getLastOnline() + " registered: " + res.getRegistered());
        CivMessage.send(sender, "debt: " + res.getTreasury().getDebt());

    }

    @Override
    public void permissionCheck() throws CivException {
        if (sender instanceof Player) {
            if (sender.isOp()) {
                return;
            }
        } else {
            return;
        }
        throw new CivException("Only OP can do this.");
    }

    @Override
    public void doDefaultAction() {
        showHelp();
    }

    public void setNativeTown_cmd() throws CivException {
        Town t = getNamedTown(2);
        Resident r = getNamedResident(3);
        if (r == null) {
            r = getResident();
        }
        r.setNativeTown(t);
    }

}
