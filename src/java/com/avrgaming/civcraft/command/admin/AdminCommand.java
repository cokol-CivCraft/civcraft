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
package com.avrgaming.civcraft.command.admin;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.command.ReportChestsTask;
import com.avrgaming.civcraft.command.ReportPlayerInventoryTask;
import com.avrgaming.civcraft.config.*;
import com.avrgaming.civcraft.endgame.EndGameCondition;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.lorestorage.LoreGuiItemListener;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

public class AdminCommand extends CommandBase {

    @Override
    public void init() {
        command = "/ad";
        displayName = CivSettings.localize.localizedString("adcmd_Name");

        register_sub("perm", this::perm_cmd, CivSettings.localize.localizedString("adcmd_permDesc"));
        register_sub("sbperm", this::sbperm_cmd, CivSettings.localize.localizedString("adcmd_adpermDesc"));
        register_sub("cbinstantbreak", this::cbinstantbreak_cmd, CivSettings.localize.localizedString("adcmd_cbinstantbreakDesc"));

        register_sub("recover", this::recover_cmd, CivSettings.localize.localizedString("adcmd_recoverDesc"));
        register_sub("server", this::server_cmd, CivSettings.localize.localizedString("adcmd_serverDesc"));
        register_sub("spawnunit", this::spawnunit_cmd, CivSettings.localize.localizedString("adcmd_spawnUnitDesc"));

        register_sub("chestreport", this::chestreport_cmd, CivSettings.localize.localizedString("adcmd_chestReportDesc"));
        register_sub("playerreport", this::playerreport_cmd, CivSettings.localize.localizedString("adcmd_playerreportDesc"));

        register_sub("civ", this::civ_cmd, CivSettings.localize.localizedString("adcmd_civDesc"));
        register_sub("town", this::town_cmd, CivSettings.localize.localizedString("adcmd_townDesc"));
        register_sub("war", this::war_cmd, CivSettings.localize.localizedString("adcmd_warDesc"));
        register_sub("lag", this::lag_cmd, CivSettings.localize.localizedString("adcmd_lagdesc"));
        register_sub("camp", this::camp_cmd, CivSettings.localize.localizedString("adcmd_campDesc"));
        register_sub("chat", this::chat_cmd, CivSettings.localize.localizedString("adcmd_chatDesc"));
        register_sub("res", this::res_cmd, CivSettings.localize.localizedString("adcmd_resDesc"));
        register_sub("build", this::build_cmd, CivSettings.localize.localizedString("adcmd_buildDesc"));
        register_sub("items", this::items_cmd, CivSettings.localize.localizedString("adcmd_itemsDesc"));
        register_sub("item", this::item_cmd, CivSettings.localize.localizedString("adcmd_itemDesc"));
        register_sub("timer", this::timer_cmd, CivSettings.localize.localizedString("adcmd_timerDesc"));
        register_sub("road", this::road_cmd, CivSettings.localize.localizedString("adcmd_roadDesc"));
        register_sub("clearendgame", this::clearendgame_cmd, CivSettings.localize.localizedString("adcmd_clearEndGameDesc"));
        register_sub("endworld", this::endworld_cmd, CivSettings.localize.localizedString("adcmd_endworldDesc"));
        register_sub("arena", this::arena_cmd, CivSettings.localize.localizedString("adcmd_arenaDesc"));
        register_sub("perk", this::perk_cmd, CivSettings.localize.localizedString("adcmd_perkDesc"));
        register_sub("reloadgov", this::reloadgov_cmd, CivSettings.localize.localizedString("adcmd_reloadgovDesc"));
        register_sub("reloadac", this::reloadac_cmd, CivSettings.localize.localizedString("adcmd_reloadacDesc"));
    }


    public void reloadgov_cmd() throws CivException {
        try {
            CivSettings.reloadGovConfigFiles();
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            throw new CivException(e.getMessage());
        }
        for (Civilization civ : CivGlobal.getCivs()) {
            ConfigGovernment gov = civ.getGovernment();

            civ.setGovernment(gov.id);
        }
        CivMessage.send(sender, CivColor.Gold + CivSettings.localize.localizedString("adcmd_reloadgovSuccess"));
    }


    public void reloadac_cmd() {

        CivMessage.send(sender, CivColor.Gold + CivSettings.localize.localizedString("adcmd_reloadacSuccess"));
    }


    public void perk_cmd() {
        AdminPerkCommand cmd = new AdminPerkCommand();
        cmd.onCommand(sender, null, "perk", this.stripArgs(args, 1));
    }


    public void endworld_cmd() {
        CivGlobal.endWorld = !CivGlobal.endWorld;
        if (CivGlobal.endWorld) {
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_endworldOn"));
        } else {
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_endworldOff"));
        }
    }


    public void clearendgame_cmd() throws CivException {
        String key = getNamedString(1, "enter key.");
        Civilization civ = getNamedCiv(2);

        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(key);
        if (entries.size() == 0) {
            throw new CivException(CivSettings.localize.localizedString("adcmd_clearEndGameNoKey"));
        }

        for (SessionEntry entry : entries) {
            if (EndGameCondition.getCivFromSessionData(entry.value) == civ) {
                CivGlobal.getSessionDB().delete(entry.request_id, entry.key);
                CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_clearEndGameSuccess", civ.getName()));
            }
        }
    }


    public void cbinstantbreak_cmd() throws CivException {
        Resident resident = getResident();

        resident.setControlBlockInstantBreak(!resident.isControlBlockInstantBreak());
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_cbinstantbreakSuccess") + resident.isControlBlockInstantBreak());
    }

    public static Inventory spawnInventory = null;


    public void items_cmd() throws CivException {
        Player player = getPlayer();

        if (spawnInventory == null) {
            spawnInventory = Bukkit.createInventory(player, LoreGuiItem.MAX_INV_SIZE, CivSettings.localize.localizedString("adcmd_itemsHeader"));

            /* Build the Category Inventory. */
            for (ConfigMaterialCategory cat : ConfigMaterialCategory.getCategories()) {
                Material identifier;
                if (cat.name.contains("Fish")) {
                    identifier = Material.RAW_FISH;
                } else if (cat.name.contains("Catalyst")) {
                    identifier = Material.BOOK;
                } else if (cat.name.contains("Gear")) {
                    identifier = Material.IRON_SWORD;
                } else if (cat.name.contains("Materials")) {
                    identifier = Material.WOOD_STEP;
                } else if (cat.name.contains("Tools")) {
                    identifier = Material.IRON_SPADE;
                } else if (cat.name.contains("Eggs")) {
                    identifier = Material.MONSTER_EGG;
                } else {
                    identifier = Material.WRITTEN_BOOK;
                }
                ItemStack infoRec = LoreGuiItem.build(cat.name,
                        identifier,
                        0,
                        CivColor.LightBlue + cat.materials.size() + " Items",
                        CivColor.Gold + "<Click To Open>");
                infoRec = LoreGuiItem.setAction(infoRec, "OpenInventory");
                infoRec = LoreGuiItem.setActionData(infoRec, "invType", "showGuiInv");
                infoRec = LoreGuiItem.setActionData(infoRec, "invName", cat.name + " Spawn");
                spawnInventory.addItem(infoRec);

                /* Build a new GUI Inventory. */
                Inventory inv = Bukkit.createInventory(player, LoreGuiItem.MAX_INV_SIZE, cat.name + " Spawn");
                for (ConfigMaterial mat : cat.materials.values()) {
                    LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterialFromId(mat.id);
                    ItemStack stack = LoreMaterial.spawn(craftMat);
                    stack = LoreGuiItem.asGuiItem(stack);
                    stack = LoreGuiItem.setAction(stack, "SpawnItem");
                    inv.addItem(stack);
                    LoreGuiItemListener.guiInventories.put(inv.getName(), inv);
                }
            }


        }

        player.openInventory(spawnInventory);
    }


    public void arena_cmd() {
        AdminArenaCommand cmd = new AdminArenaCommand();
        cmd.onCommand(sender, null, "arena", this.stripArgs(args, 1));
    }


    public void road_cmd() {
        AdminRoadCommand cmd = new AdminRoadCommand();
        cmd.onCommand(sender, null, "camp", this.stripArgs(args, 1));
    }


    public void item_cmd() {
        AdminItemCommand cmd = new AdminItemCommand();
        cmd.onCommand(sender, null, "camp", this.stripArgs(args, 1));
    }

    @SuppressWarnings("unused")
    public void timer_cmd() {
        AdminTimerCommand cmd = new AdminTimerCommand();
        cmd.onCommand(sender, null, "camp", this.stripArgs(args, 1));
    }

    @SuppressWarnings("unused")
    public void camp_cmd() {
        AdminCampCommand cmd = new AdminCampCommand();
        cmd.onCommand(sender, null, "camp", this.stripArgs(args, 1));
    }

    @SuppressWarnings("unused")
    public void playerreport_cmd() {

        LinkedList<OfflinePlayer> offplayers = new LinkedList<>();
        Collections.addAll(offplayers, Bukkit.getOfflinePlayers());

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_playerreportHeader"));
        CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_ReportStarted"));
        TaskMaster.syncTask(new ReportPlayerInventoryTask(sender, offplayers), 0);
    }

    @SuppressWarnings("unused")
    public void chestreport_cmd() throws CivException {
        Integer radius = getNamedInteger(1);
        Player player = getPlayer();

        LinkedList<ChunkCoord> coords = new LinkedList<>();
        for (int x = -radius; x < radius; x++) {
            for (int z = -radius; z < radius; z++) {
                ChunkCoord coord = new ChunkCoord(player.getLocation());
                coord.setX(coord.getX() + x);
                coord.setZ(coord.getZ() + z);

                coords.add(coord);
            }
        }

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_chestReportHeader"));
        CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_ReportStarted"));
        TaskMaster.syncTask(new ReportChestsTask(sender, coords), 0);
    }

    @SuppressWarnings("unused")
    public void spawnunit_cmd() throws CivException {
        if (args.length < 2) {
            throw new CivException(CivSettings.localize.localizedString("adcmd_spawnUnitPrompt"));
        }

        ConfigUnit unit = CivSettings.units.get(args[1]);
        if (unit == null) {
            throw new CivException(CivSettings.localize.localizedString("var_adcmd_spawnUnitInvalid", args[1]));
        }

        Player player = getPlayer();
        Town town = getNamedTown(2);

//		if (args.length > 2) {
//			try {
//				player = CivGlobal.getPlayer(args[2]);
//			} catch (CivException e) {
//				throw new CivException("Player "+args[2]+" is not online.");
//			}
//		} else {
//			player = getPlayer();
//		}

        Class<?> c;
        try {
            c = Class.forName(unit.class_name);
            Method m = c.getMethod("spawn", Inventory.class, Town.class);
            m.invoke(null, player.getInventory(), town);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException
                 | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new CivException(e.getMessage());
        }


        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_spawnUnitSuccess", unit.name));
    }

    @SuppressWarnings("unused")
    public void server_cmd() {
        CivMessage.send(sender, Bukkit.getServerName());
    }

    @SuppressWarnings("unused")
    public void recover_cmd() {
        AdminRecoverCommand cmd = new AdminRecoverCommand();
        cmd.onCommand(sender, null, "recover", this.stripArgs(args, 1));
    }

    @SuppressWarnings("unused")
    public void town_cmd() {
        AdminTownCommand cmd = new AdminTownCommand();
        cmd.onCommand(sender, null, "town", this.stripArgs(args, 1));
    }

    @SuppressWarnings("unused")
    public void civ_cmd() {
        AdminCivCommand cmd = new AdminCivCommand();
        cmd.onCommand(sender, null, "civ", this.stripArgs(args, 1));
    }

    @SuppressWarnings("unused")
    public void setfullmessage_cmd() {
        if (args.length < 2) {
            CivMessage.send(sender, CivSettings.localize.localizedString("Current") + CivGlobal.fullMessage);
            return;
        }

        synchronized (CivGlobal.maxPlayers) {
            CivGlobal.fullMessage = args[1];
        }

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("SetTo") + args[1]);

    }

    @SuppressWarnings("unused")
    public void res_cmd() {
        AdminResCommand cmd = new AdminResCommand();
        cmd.onCommand(sender, null, "war", this.stripArgs(args, 1));
    }

    @SuppressWarnings("unused")
    public void chat_cmd() {
        AdminChatCommand cmd = new AdminChatCommand();
        cmd.onCommand(sender, null, "war", this.stripArgs(args, 1));
    }

    @SuppressWarnings("unused")
    public void war_cmd() {
        AdminWarCommand cmd = new AdminWarCommand();
        cmd.onCommand(sender, null, "war", this.stripArgs(args, 1));
    }

    @SuppressWarnings("unused")
    public void lag_cmd() {
        AdminLagCommand cmd = new AdminLagCommand();
        cmd.onCommand(sender, null, "war", this.stripArgs(args, 1));
    }

    @SuppressWarnings("unused")
    public void build_cmd() {
        AdminBuildCommand cmd = new AdminBuildCommand();
        cmd.onCommand(sender, null, "war", this.stripArgs(args, 1));
    }

    @SuppressWarnings("unused")
    public void perm_cmd() throws CivException {
        Resident resident = getResident();

        if (resident.isPermOverride()) {
            resident.setPermOverride(false);
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_permOff"));
            return;
        }

        resident.setPermOverride(true);
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_permOn"));

    }

    @SuppressWarnings("unused")
    public void sbperm_cmd() throws CivException {
        Resident resident = getResident();
        if (resident.isSBPermOverride()) {
            resident.setSBPermOverride(false);
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_sbpermOff"));
            return;
        }

        resident.setSBPermOverride(true);
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_sbpermOn"));
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
    public void permissionCheck() throws CivException {

        if (sender instanceof Player && sender.hasPermission(CivSettings.MINI_ADMIN)) {
            return;

        }


        if (!sender.isOp()) {
            throw new CivException(CivSettings.localize.localizedString("adcmd_NotAdmin"));
        }
    }

    @Override
    public void doLogging() {
        CivLog.adminlog(sender.getName(), "/ad " + this.combineArgs(args));
    }

}
