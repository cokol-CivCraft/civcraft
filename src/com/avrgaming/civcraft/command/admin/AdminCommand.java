/*************************************************************************
 *
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.command.ReportChestsTask;
import com.avrgaming.civcraft.command.ReportPlayerInventoryTask;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigGovernment;
import com.avrgaming.civcraft.config.ConfigMaterial;
import com.avrgaming.civcraft.config.ConfigMaterialCategory;
import com.avrgaming.civcraft.config.ConfigUnit;
import com.avrgaming.civcraft.endgame.EndGameCondition;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
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

public class AdminCommand extends CommandBase {

    @Override
    public void init() {
        command = "/ad";
        displayName = CivSettings.localize.localizedString("adcmd_Name");

        commands.put("perm", CivSettings.localize.localizedString("adcmd_permDesc"));
        commands.put("sbperm", CivSettings.localize.localizedString("adcmd_adpermDesc"));
        commands.put("cbinstantbreak", CivSettings.localize.localizedString("adcmd_cbinstantbreakDesc"));

        commands.put("recover", CivSettings.localize.localizedString("adcmd_recoverDesc"));
        commands.put("server", CivSettings.localize.localizedString("adcmd_serverDesc"));
        commands.put("spawnunit", CivSettings.localize.localizedString("adcmd_spawnUnitDesc"));

        commands.put("chestreport", CivSettings.localize.localizedString("adcmd_chestReportDesc"));
        commands.put("playerreport", CivSettings.localize.localizedString("adcmd_playerreportDesc"));

        commands.put("civ", CivSettings.localize.localizedString("adcmd_civDesc"));
        commands.put("town", CivSettings.localize.localizedString("adcmd_townDesc"));
        commands.put("war", CivSettings.localize.localizedString("adcmd_warDesc"));
        commands.put("lag", CivSettings.localize.localizedString("adcmd_lagdesc"));
        commands.put("camp", CivSettings.localize.localizedString("adcmd_campDesc"));
        commands.put("chat", CivSettings.localize.localizedString("adcmd_chatDesc"));
        commands.put("res", CivSettings.localize.localizedString("adcmd_resDesc"));
        commands.put("build", CivSettings.localize.localizedString("adcmd_buildDesc"));
        commands.put("items", CivSettings.localize.localizedString("adcmd_itemsDesc"));
        commands.put("item", CivSettings.localize.localizedString("adcmd_itemDesc"));
        commands.put("timer", CivSettings.localize.localizedString("adcmd_timerDesc"));
        commands.put("road", CivSettings.localize.localizedString("adcmd_roadDesc"));
        commands.put("clearendgame", CivSettings.localize.localizedString("adcmd_clearEndGameDesc"));
        commands.put("endworld", CivSettings.localize.localizedString("adcmd_endworldDesc"));
        commands.put("arena", CivSettings.localize.localizedString("adcmd_arenaDesc"));
        commands.put("perk", CivSettings.localize.localizedString("adcmd_perkDesc"));
        commands.put("reloadgov", CivSettings.localize.localizedString("adcmd_reloadgovDesc"));
        commands.put("reloadac", CivSettings.localize.localizedString("adcmd_reloadacDesc"));
        commands.put("heartbeat", CivSettings.localize.localizedString("adcmd_heartbeatDesc"));
    }

    @SuppressWarnings("unused")
    public void reloadgov_cmd() throws IOException, InvalidConfigurationException, InvalidConfiguration {
        CivSettings.reloadGovConfigFiles();
        for (Civilization civ : CivGlobal.getCivs()) {
            ConfigGovernment gov = civ.getGovernment();

            civ.setGovernment(gov.id);
        }
        CivMessage.send(sender, CivColor.Gold + CivSettings.localize.localizedString("adcmd_reloadgovSuccess"));
    }

    @SuppressWarnings("unused")
    public void reloadac_cmd() throws IOException, InvalidConfigurationException, InvalidConfiguration {
        CivSettings.reloadNoCheat();
        CivMessage.send(sender, CivColor.Gold + CivSettings.localize.localizedString("adcmd_reloadacSuccess"));
    }

    @SuppressWarnings("unused")
    public void perk_cmd() {
        AdminPerkCommand cmd = new AdminPerkCommand();
        cmd.onCommand(sender, null, "perk", this.stripArgs(args, 1));
    }

    @SuppressWarnings("unused")
    public void endworld_cmd() {
        CivGlobal.endWorld = !CivGlobal.endWorld;
        if (CivGlobal.endWorld) {
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_endworldOn"));
        } else {
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_endworldOff"));
        }
    }

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    public void cbinstantbreak_cmd() throws CivException {
        Resident resident = getResident();

        resident.setControlBlockInstantBreak(!resident.isControlBlockInstantBreak());
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_cbinstantbreakSuccess") + resident.isControlBlockInstantBreak());
    }

    public static Inventory spawnInventory = null;

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    public void arena_cmd() {
        AdminArenaCommand cmd = new AdminArenaCommand();
        cmd.onCommand(sender, null, "arena", this.stripArgs(args, 1));
    }

    @SuppressWarnings("unused")
    public void road_cmd() {
        AdminRoadCommand cmd = new AdminRoadCommand();
        cmd.onCommand(sender, null, "camp", this.stripArgs(args, 1));
    }

    @SuppressWarnings("unused")
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
