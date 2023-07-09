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
package com.avrgaming.civcraft.command;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.threading.tasks.BuildAsyncTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.war.War;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.text.DecimalFormat;

public class BuildCommand extends CommandBase {

    @Override
    public void init() {
        command = "/build";
        displayName = CivSettings.localize.localizedString("cmd_build_Desc");
        sendUnknownToDefault = true;

        register_sub("list", this::list_cmd, CivSettings.localize.localizedString("cmd_build_listDesc"));
        register_sub("progress", this::progress_cmd, CivSettings.localize.localizedString("cmd_build_progressDesc"));
        register_sub("repairnearest", this::repairnearest_cmd, CivSettings.localize.localizedString("cmd_build_repairnearestDesc"));
        register_sub("undo", this::undo_cmd, CivSettings.localize.localizedString("cmd_build_undoDesc"));
        register_sub("demolish", this::demolish_cmd, CivSettings.localize.localizedString("cmd_build_demolishDesc"));
        register_sub("demolishnearest", this::demolishnearest_cmd, CivSettings.localize.localizedString("cmd_build_demolishnearestDesc"));
        register_sub("refreshnearest", this::refreshnearest_cmd, CivSettings.localize.localizedString("cmd_build_refreshnearestDesc"));
        register_sub("validatenearest", this::validatenearest_cmd, CivSettings.localize.localizedString("cmd_build_validateNearestDesc"));
        // commands.put("preview", "shows a preview of this structure at this location.");
        register_sub("d", this::demolish_cmd, null); // demolish
        register_sub("p", this::progress_cmd, null); // progress (TODO: add approx time [~7h])
        register_sub("l", this::list_cmd, null); // list
        register_sub("u", this::undo_cmd, null); // undo
    }


    @SuppressWarnings("unused")
    public void validatenearest_cmd() throws CivException {
        Player player = getPlayer();
        Resident resident = getResident();
        Buildable buildable = CivGlobal.getNearestBuildable(player.getLocation());

        if (buildable.getTown() != resident.getTown()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_build_validateNearestYourTownOnly"));
        }

        if (War.isWarTime()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_build_validatenearestNotDuringWar"));
        }

        if (buildable.isIgnoreFloating()) {
            throw new CivException(CivSettings.localize.localizedString("var_cmd_build_validateNearestExempt", buildable.getDisplayName()));
        }

        CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_cmd_build_validateNearestSuccess", buildable.getDisplayName(), buildable.getCenterLocation()));
        buildable.validate(player);
    }

    @SuppressWarnings("unused")
    public void refreshnearest_cmd() throws CivException {
        Town town = getSelectedTown();
        Resident resident = getResident();
        town.refreshNearestBuildable(resident);
    }

    @SuppressWarnings("unused")
    public void repairnearest_cmd() throws CivException {
        Town town = getSelectedTown();
        Player player = getPlayer();

        if (War.isWarTime()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_build_repairNotDuringWar"));
        }

        Structure nearest = town.getNearestStrucutre(player.getLocation());

        if (nearest == null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_build_Invalid"));
        }

        if (!nearest.isDestroyed()) {
            throw new CivException(CivSettings.localize.localizedString("var_cmd_build_repairNotDestroyed", nearest.getDisplayName(), nearest.getCorner()));
        }

        if (!town.getCiv().hasTechnology(nearest.getRequiredTechnology())) {
            throw new CivException(CivSettings.localize.localizedString("var_cmd_build_repairMissingTech", nearest.getDisplayName(), nearest.getCorner()));
        }

        if (args.length < 2 || !args[1].equalsIgnoreCase("yes")) {
            CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("var_cmd_build_repairConfirmPrompt",
                    CivColor.Yellow + nearest.getDisplayName() + CivColor.LightGreen, CivColor.Yellow + nearest.getCorner() + CivColor.LightGreen, CivColor.Yellow + nearest.getRepairCost() + CivColor.LightGreen, CivColor.Yellow + CivSettings.CURRENCY_NAME + CivColor.LightGreen));
            CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("cmd_build_repairConfirmPrompt2"));
            return;
        }

        town.repairStructure(nearest);
        CivMessage.sendSuccess(player, nearest.getDisplayName() + " " + CivSettings.localize.localizedString("Repaired"));
    }

    @SuppressWarnings("unused")
    public void demolishnearest_cmd() throws CivException {
        Town town = getSelectedTown();
        Player player = getPlayer();

        Structure nearest = town.getNearestStrucutre(player.getLocation());

        if (nearest == null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_build_Invalid"));
        }

        if (args.length < 2 || !args[1].equalsIgnoreCase("yes")) {
            CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("var_cmd_build_demolishNearestConfirmPrompt", CivColor.Yellow + nearest.getDisplayName() + CivColor.LightGreen,
                    CivColor.Yellow + nearest.getCorner() + CivColor.LightGreen));
            CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("cmd_build_demolishNearestConfirmPrompt2"));

            nearest.flashStructureBlocks();
            return;
        }

        town.demolish(nearest, false);
        CivMessage.sendSuccess(player, nearest.getDisplayName() + " at " + nearest.getCorner() + " " + CivSettings.localize.localizedString("adcmd_build_demolishComplete"));
    }


    public void demolish_cmd() throws CivException {
        Town town = getSelectedTown();


        if (args.length < 2) {
            CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_build_demolishHeader"));
            for (Structure struct : town.getStructures()) {
                CivMessage.send(sender, CivSettings.localize.localizedString("var_cmd_build_demolish", struct.getDisplayName(), CivColor.Yellow + struct.getCorner().toString() + CivColor.White));
            }
            return;
        }

        try {
            BlockCoord coord = new BlockCoord(args[1]);
            Structure struct = town.getStructure(coord);
            if (struct == null) {
                CivMessage.send(sender, CivColor.Rose + " " + CivSettings.localize.localizedString("NoStructureAt") + " " + args[1]);
                return;
            }
            struct.getTown().demolish(struct, false);
            CivMessage.sendTown(struct.getTown(), struct.getDisplayName() + " " + CivSettings.localize.localizedString("adcmd_build_demolishComplete"));
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            CivMessage.sendError(sender, CivSettings.localize.localizedString("cmd_build_demolishFormatError"));
        }
    }

    @SuppressWarnings("unused")
    public void d_cmd() {
        try {
            demolish_cmd();
        } catch (CivException e) {
            e.printStackTrace();
        }
    }

    public void undo_cmd() throws CivException {
        Town town = getSelectedTown();
        town.processUndo();
    }

    @SuppressWarnings("unused")
    public void u_cmd() {
        try {
            undo_cmd();
        } catch (CivException e) {
            e.printStackTrace();
        }
    }

    public void progress_cmd() throws CivException {
        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_build_undoHeader"));
        Town town = getSelectedTown();
        for (BuildAsyncTask task : town.build_tasks) {
            Buildable b = task.buildable;
            DecimalFormat df = new DecimalFormat();
            double total = b.getHammerCost();
            double current = b.getBuiltHammers();
            double builtPercentage = Math.round(current / total * 100);

            CivMessage.send(sender, CivColor.LightPurple + b.getDisplayName() + ": " + CivColor.Yellow + builtPercentage + "% (" + df.format(current) + "/" + total + ")" +
                    CivColor.LightPurple + " Blocks " + CivColor.Yellow + "(" + b.builtBlockCount + "/" + b.getTotalBlockCount() + ")");

            //CivMessage.send(sender, CivColor.LightPurple+b.getDisplayName()+" "+CivColor.Yellow+"("+
            //	b.builtBlockCount+" / "+b.getTotalBlockCount()+")");
        }

    }

    @SuppressWarnings("unused")
    public void p_cmd() {
        try {
            progress_cmd();
        } catch (CivException e) {
            e.printStackTrace();
        }
    }

    public void list_available_structures() throws CivException {
        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_build_listHeader"));
        Town town = getSelectedTown();
        for (ConfigBuildableInfo sinfo : CivSettings.structures.values()) {
            if (!sinfo.isAvailable(town)) {
                continue;
            }
            String leftString;
            if (sinfo.limit == 0) {
                leftString = CivSettings.localize.localizedString("Unlimited");
            } else {
                leftString = String.valueOf(sinfo.limit - town.getStructureTypeCount(sinfo.id));
            }

            CivMessage.send(sender, CivColor.LightPurple + sinfo.displayName + " " +
                    CivColor.Yellow +
                    CivSettings.localize.localizedString("Cost") + " " + sinfo.cost + " " +
                    CivSettings.localize.localizedString("Upkeep") + " " + sinfo.upkeep + " " + CivSettings.localize.localizedString("Hammers") + " " + sinfo.hammer_cost + " " +
                    CivSettings.localize.localizedString("Remaining") + " " + leftString);
        }
    }

    public void list_available_wonders() throws CivException {
        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_build_listWondersHeader"));
        Town town = getSelectedTown();
        for (ConfigBuildableInfo sinfo : CivSettings.wonders.values()) {
            if (!sinfo.isAvailable(town)) {
                continue;
            }
            String leftString;
            if (sinfo.limit == 0) {
                leftString = CivSettings.localize.localizedString("Unlimited");
            } else {
                leftString = String.valueOf(sinfo.limit - town.getStructureTypeCount(sinfo.id));
            }

            if (Wonder.isWonderAvailable(sinfo.id)) {
                CivMessage.send(sender, CivColor.LightPurple + sinfo.displayName + " " +
                        CivColor.Yellow +
                        CivSettings.localize.localizedString("Cost") + " " + sinfo.cost + " " +
                        CivSettings.localize.localizedString("Upkeep") + " " + sinfo.upkeep + " " + CivSettings.localize.localizedString("Hammers") + " " + sinfo.hammer_cost + " " +
                        CivSettings.localize.localizedString("Remaining") + " " + leftString);
            } else {
                Wonder wonder = CivGlobal.getWonderByConfigId(sinfo.id);
                CivMessage.send(sender, CivColor.LightGray + sinfo.displayName + " Cost: " + sinfo.cost + " - " + CivSettings.localize.localizedString("var_cmd_build_listWonderAlreadyBuild", wonder.getTown().getName(), wonder.getTown().getCiv().getName()));
            }
        }
    }

    public void list_cmd() throws CivException {
        this.list_available_structures();
        this.list_available_wonders();
    }

    @SuppressWarnings("unused")
    public void l_cmd() {
        try {
            list_cmd();
        } catch (CivException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doDefaultAction() throws CivException {
        if (args.length == 0) {
            showHelp();
            return;
        }
        StringBuilder fullArgs = new StringBuilder();
        for (String arg : args) {
            fullArgs.append(arg).append(" ");
        }
        buildByName(fullArgs.toString().trim());
    }

    @SuppressWarnings("unused")
    public void preview_cmd() throws CivException {
        String fullArgs = this.combineArgs(this.stripArgs(args, 1));

        ConfigBuildableInfo sinfo = CivSettings.getBuildableInfoByName(fullArgs);
        if (sinfo == null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_build_defaultUnknownStruct") + " " + fullArgs);
        }

        Town town = getSelectedTown();
        if (sinfo.isWonder) {
            Wonder wonder = Wonder.newWonder(getPlayer().getLocation(), sinfo.id, town);
            try {
                wonder.buildPlayerPreview(getPlayer(), getPlayer().getLocation());
            } catch (IOException e) {
                e.printStackTrace();
                throw new CivException(CivSettings.localize.localizedString("internalIOException"));
            }
        } else {
            Structure struct = Structure.newStructure(getPlayer().getLocation(), sinfo.id, town);
            try {
                struct.buildPlayerPreview(getPlayer(), getPlayer().getLocation());
            } catch (IOException e) {
                e.printStackTrace();
                throw new CivException(CivSettings.localize.localizedString("internalIOException"));
            }
        }
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_build_showPreviewSuccess"));
    }


    private void buildByName(String fullArgs) throws CivException {
        ConfigBuildableInfo sinfo = CivSettings.getBuildableInfoByName(fullArgs);

        if (sinfo == null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_build_defaultUnknownStruct") + " " + fullArgs);
        }

        Town town = getSelectedTown();

        if (sinfo.isWonder) {
            Wonder wonder = Wonder.newWonder(getPlayer().getLocation(), sinfo.id, town);
            try {
                wonder.buildPlayerPreview(getPlayer(), getPlayer().getLocation());
            } catch (IOException e) {
                e.printStackTrace();
                throw new CivException(CivSettings.localize.localizedString("internalIOException"));
            }
        } else {
            Structure struct = Structure.newStructure(getPlayer().getLocation(), sinfo.id, town);
            try {
                struct.buildPlayerPreview(getPlayer(), getPlayer().getLocation());
            } catch (IOException e) {
                e.printStackTrace();
                throw new CivException(CivSettings.localize.localizedString("internalIOException"));
            }
        }
        getPlayer().playSound(getPlayer().getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 0.9f, 0.9f);

//		if (sinfo.isWonder) {
//			town.buildWonder(getPlayer(), sinfo.id, getPlayer().getLocation());
//		} else {
//			town.buildStructure(getPlayer(), sinfo.id, getPlayer().getLocation());
//		}
//		CivMessage.sendSuccess(sender, "Started building "+sinfo.displayName);
    }

    @Override
    public void showHelp() {
        showBasicHelp();
        CivMessage.send(sender, CivColor.LightPurple + command + " " + CivColor.Yellow + CivSettings.localize.localizedString("cmd_build_help1") + " " +
                CivColor.LightGray + CivSettings.localize.localizedString("cmd_build_help2"));
    }

    @Override
    public void permissionCheck() throws CivException {
        validMayorAssistantLeader();
    }

}
