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
import com.avrgaming.civcraft.config.*;
import com.avrgaming.civcraft.endgame.EndGameCondition;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.loregui.GuiActions;
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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

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
        register_sub("clearendgame", this::clearendgame_cmd, CivSettings.localize.localizedString("adcmd_clearEndGameDesc"));
        register_sub("arena", this::arena_cmd, CivSettings.localize.localizedString("adcmd_arenaDesc"));
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
        CivMessage.send(sender, ChatColor.GOLD + CivSettings.localize.localizedString("adcmd_reloadgovSuccess"));
    }


    public void reloadac_cmd() {

        CivMessage.send(sender, ChatColor.GOLD + CivSettings.localize.localizedString("adcmd_reloadacSuccess"));
    }


    public void clearendgame_cmd() throws CivException {
        String key = getNamedString(1, "enter key.");
        Civilization civ = getNamedCiv(2);

        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(key);
        if (entries.isEmpty()) {
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
                    identifier = Material.COD;
                } else if (cat.name.contains("Catalyst")) {
                    identifier = Material.BOOK;
                } else if (cat.name.contains("Gear")) {
                    identifier = Material.IRON_SWORD;
                } else if (cat.name.contains("Materials")) {
                    identifier = Material.OAK_STAIRS;
                } else if (cat.name.contains("Tools")) {
                    identifier = Material.IRON_SHOVEL;
                } else if (cat.name.contains("Eggs")) {
                    identifier = Material.GHAST_SPAWN_EGG;
                } else {
                    identifier = Material.WRITTEN_BOOK;
                }
                ItemStack infoRec = LoreGuiItem.build(cat.name,
                        identifier,
                        0,
                        String.valueOf(ChatColor.AQUA) + cat.materials.size() + " Items",
                        ChatColor.GOLD + "<Click To Open>");
                infoRec = LoreGuiItem.setAction(infoRec, GuiActions.OpenInventory);
                infoRec = LoreGuiItem.setActionData(infoRec, "invType", "showGuiInv");
                infoRec = LoreGuiItem.setActionData(infoRec, "invName", cat.name + " Spawn");
                spawnInventory.addItem(infoRec);

                /* Build a new GUI Inventory. */
                Inventory inv = Bukkit.createInventory(player, LoreGuiItem.MAX_INV_SIZE, cat.name + " Spawn");
                for (ConfigMaterial mat : cat.materials.values()) {
                    LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterialFromId(mat.id);
                    ItemStack stack = LoreMaterial.spawn(craftMat);
                    stack = LoreGuiItem.asGuiItem(stack);
                    stack = LoreGuiItem.setAction(stack, GuiActions.SpawnItem);
                    inv.addItem(stack);
                    LoreGuiItemListener.guiInventories.put(cat.name + " Spawn", inv);
                }
            }


        }

        player.openInventory(spawnInventory);
    }


    public void arena_cmd() {
        AdminArenaCommand cmd = new AdminArenaCommand();
        cmd.onCommand(sender, null, "arena", this.stripArgs(args, 1));
    }

    public void item_cmd() {
        AdminItemCommand cmd = new AdminItemCommand();
        cmd.onCommand(sender, null, "camp", this.stripArgs(args, 1));
    }

    public void timer_cmd() {
        AdminTimerCommand cmd = new AdminTimerCommand();
        cmd.onCommand(sender, null, "camp", this.stripArgs(args, 1));
    }

    public void camp_cmd() {
        AdminCampCommand cmd = new AdminCampCommand();
        cmd.onCommand(sender, null, "camp", this.stripArgs(args, 1));
    }

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

        Class<?> c;
        try {
            c = Class.forName(unit.class_name);
            Method m = c.getMethod("spawn", Inventory.class, Town.class);
            m.invoke(null, player.getInventory(), town);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException
                 | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new CivException(e.getCause().getMessage());
        }


        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_spawnUnitSuccess", unit.name));
    }

    public void server_cmd() {
        CivMessage.send(sender, Bukkit.getServer().getName());
    }

    public void recover_cmd() {
        AdminRecoverCommand cmd = new AdminRecoverCommand();
        cmd.onCommand(sender, null, "recover", this.stripArgs(args, 1));
    }

    public void town_cmd() {
        AdminTownCommand cmd = new AdminTownCommand();
        cmd.onCommand(sender, null, "town", this.stripArgs(args, 1));
    }

    public void civ_cmd() {
        AdminCivCommand cmd = new AdminCivCommand();
        cmd.onCommand(sender, null, "civ", this.stripArgs(args, 1));
    }

    public void res_cmd() {
        AdminResCommand cmd = new AdminResCommand();
        cmd.onCommand(sender, null, "war", this.stripArgs(args, 1));
    }

    public void chat_cmd() {
        AdminChatCommand cmd = new AdminChatCommand();
        cmd.onCommand(sender, null, "war", this.stripArgs(args, 1));
    }

    public void war_cmd() {
        AdminWarCommand cmd = new AdminWarCommand();
        cmd.onCommand(sender, null, "war", this.stripArgs(args, 1));
    }

    public void lag_cmd() {
        AdminLagCommand cmd = new AdminLagCommand();
        cmd.onCommand(sender, null, "war", this.stripArgs(args, 1));
    }

    public void build_cmd() {
        AdminBuildCommand cmd = new AdminBuildCommand();
        cmd.onCommand(sender, null, "war", this.stripArgs(args, 1));
    }

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
        if (!sender.isOp()) {
            throw new CivException(CivSettings.localize.localizedString("adcmd_NotAdmin"));
        }
    }

    @Override
    public void doLogging() {
        CivLog.adminlog(sender.getName(), "/ad " + this.combineArgs(args));
    }

}
