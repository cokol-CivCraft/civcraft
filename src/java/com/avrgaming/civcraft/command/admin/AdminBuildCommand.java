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
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.BuildableLayer;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.util.BlockCoord;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminBuildCommand extends CommandBase {

    @Override
    public void init() {
        command = "/ad build";
        displayName = CivSettings.localize.localizedString("adcmd_build_Name");
        register_sub("unbuild", this::unbuild_cmd, CivSettings.localize.localizedString("adcmd_build_unbuildDesc"));
        register_sub("demolish", this::demolish_cmd, CivSettings.localize.localizedString("adcmd_build_demolishDesc"));
        register_sub("repair", this::repair_cmd, CivSettings.localize.localizedString("adcmd_build_repairDesc"));
        register_sub("destroywonder", this::destroywonder_cmd, CivSettings.localize.localizedString("adcmd_build_destroyWonderDesc"));
        register_sub("destroynearest", this::destroynearest_cmd, CivSettings.localize.localizedString("adcmd_build_destroyNearestDesc"));
        register_sub("validatenearest", this::validatenearest_cmd, CivSettings.localize.localizedString("adcmd_build_valideateNearestDesc"));
//		register_sub("changenearest", this::changenearest_cmd, CivSettings.localize.localizedString("adcmd_build_changeNearestDesc"));
        register_sub("validateall", this::validateall_cmd, CivSettings.localize.localizedString("adcmd_build_validateAllDesc"));
        register_sub("listinvalid", this::listinvalid_cmd, CivSettings.localize.localizedString("adcmd_build_listInvalidDesc"));
        register_sub("showbuildable", this::showbuildable_cmd, CivSettings.localize.localizedString("adcmd_build_showBuildableDesc"));

//		register_sub("repairwonder", this::repairwonder_cmd, "Fixes the nearest wonder, requires confirmation.");
    }


    public void showbuildable_cmd() throws CivException {
        String locString = getNamedString(1, CivSettings.localize.localizedString("adcmd_build_showBuildableLocation"));

        for (Buildable buildable : Buildable.invalidBuildables) {
            if (buildable.getCorner().toString().equalsIgnoreCase(locString)) {

                for (Integer y : buildable.layerValidPercentages.keySet()) {
                    BuildableLayer layer = buildable.layerValidPercentages.get(y);

                    double percentage = (double) layer.current / (double) layer.max;
                    CivMessage.send(sender, "y:" + y + " %:" + percentage + " (" + layer.current + "/" + layer.max + ")");
                }
            }
        }
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("Finished"));
    }


    public void listinvalid_cmd() {
        for (Buildable buildable : Buildable.invalidBuildables) {
            CivMessage.send(sender, buildable.getDisplayName() + " @ " + buildable.getCorner() + " -> " + buildable.getTown().getName());
        }
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("Finished"));
    }


    public void validateall_cmd() {
        Buildable.invalidBuildables.clear();

        for (Structure struct : CivGlobal.getStructures()) {
            if (struct.isStrategic()) {
                struct.validate(null);
            }
        }

        for (Wonder wonder : CivGlobal.getWonders()) {
            if (wonder.isStrategic()) {
                wonder.validate(null);
            }
        }

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_build_validateAll"));
    }


    public void validatenearest_cmd() throws CivException {
        Player player = getPlayer();
        Town town = getNamedTown(1);
        Buildable buildable = town.getNearestBuildable(player.getLocation());

        if (args.length < 3 || !args[2].equalsIgnoreCase("yes")) {
            CivMessage.send(player, String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.localize.localizedString("var_adcmd_build_wouldValidate", buildable.getDisplayName(), buildable.getCorner()));
            return;
        }

        buildable.validate(player);
    }

//	public void changenearest_cmd() throws CivException {
//		Player player = getPlayer();
//		Town town = getNamedTown(1);
//		Buildable buildable = town.getNearestBuildable(player.getLocation());
//		
//		if (args.length < 3)
//		{
//			CivMessage.send(player, CivColor.Red+ChatColor.BOLD+CivSettings.localize.localizedString("adcmd_build_wouldChangeTheme_NoTheme"));
//
//			return;
//		}
//		
//		if (args.length < 4 || !args[3].equalsIgnoreCase("yes")) {
//			CivMessage.send(player, CivColor.Yellow+ChatColor.BOLD+CivSettings.localize.localizedString("var_adcmd_build_wouldChangeTheme",buildable.getDisplayName(),buildable.getCorner()));
//			return;
//		}
//	}


    public void destroynearest_cmd() throws CivException {

        Town town = getNamedTown(1);
        Player player = getPlayer();

        Buildable struct = town.getNearestStrucutreOrWonderInprogress(player.getLocation());

        if (args.length < 3 || !args[2].equalsIgnoreCase("yes")) {
            CivMessage.send(player, String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.localize.localizedString("var_adcmd_build_wouldDestroy", struct.getDisplayName(), struct.getCorner()));
            return;
        }

        struct.onDestroy();
        CivMessage.send(player, struct.getDisplayName() + " " + CivSettings.localize.localizedString("adcmd_build_destroyed"));
    }


    public void destroywonder_cmd() throws CivException {
        Town town = getNamedTown(1);

        if (args.length < 2) {
            throw new CivException(CivSettings.localize.localizedString("adcmd_build_enterWonderID"));
        }

        Wonder wonder = null;
        for (Wonder w : town.getWonders()) {
            if (w.getConfigId().equals(args[2])) {
                wonder = w;
                break;
            }
        }

        if (wonder == null) {
            throw new CivException(CivSettings.localize.localizedString("adcmd_build_wonderDoesNotExist") + " " + args[2]);
        }

        wonder.fancyDestroyStructureBlocks();
        try {
            wonder.getTown().removeWonder(wonder);
            wonder.fancyDestroyStructureBlocks();
            wonder.unbindStructureBlocks();
            wonder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_build_destroyed"));
    }


    public void repair_cmd() throws CivException {
        Player player = getPlayer();

        Buildable nearest = CivGlobal.getNearestBuildable(player.getLocation());

        if (nearest == null) {
            throw new CivException(CivSettings.localize.localizedString("adcmd_build_StructNotFound"));
        }

        if (args.length < 2 || !args[1].equalsIgnoreCase("yes")) {
            CivMessage.send(player, ChatColor.GREEN + CivSettings.localize.localizedString("var_adcmd_build_repairConfirmPrompt", ChatColor.YELLOW + nearest.getDisplayName(), nearest.getCorner()));
            CivMessage.send(player, ChatColor.GRAY + CivSettings.localize.localizedString("adcmd_build_toConfirm"));
            return;
        }

        try {
            nearest.repairFromTemplate();
        } catch (IOException e) {
            e.printStackTrace();
            throw new CivException(CivSettings.localize.localizedString("interactive_refresh_exception") + nearest.getSavedTemplatePath() + " ?");
        }
        CivMessage.sendSuccess(player, nearest.getDisplayName() + " " + CivSettings.localize.localizedString("Repaired"));

    }


    public void unbuild_cmd() throws CivException {

        if (args.length < 2) {
            throw new CivException(CivSettings.localize.localizedString("adcmd_build_unbuildPrompt"));
        }

        Town town = getNamedTown(1);

        if (args.length < 3) {
            CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_build_unbuildHeading"));
            for (Structure struct : town.getStructures()) {
                CivMessage.send(sender, struct.getDisplayName() + ": " + ChatColor.YELLOW + struct.getId() +
                        ChatColor.WHITE + " - " + CivSettings.localize.localizedString("Location") + " " + ChatColor.YELLOW + struct.getCorner().toString());
            }
            return;
        }

        String id = args[2];

        Connection context = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        Structure struct = null;

        try {
            context = SQL.getGameConnection();
            ps = context.prepareStatement("SELECT * FROM " + SQL.tb_prefix + Structure.TABLE_NAME + " WHERE id = " + id);
            rs = ps.executeQuery();

            while (rs.next()) {
                try {
                    Structure structure = Structure.newStructure(rs);
                    struct = CivGlobal.getStructure(structure.getCorner());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new CivException(e.getMessage());
        } finally {
            SQL.close(rs, ps, context);
        }


        if (struct == null) {
            CivMessage.send(sender, ChatColor.RED + CivSettings.localize.localizedString("NoStructureAt") + " " + args[2]);
            return;
        }

        struct.getTown().demolish(struct, true);


        CivMessage.sendTown(struct.getTown(), struct.getDisplayName() + " " + CivSettings.localize.localizedString("adcmd_build_demolishComplete"));
    }


    public void demolish_cmd() throws CivException {

        if (args.length < 2) {
            throw new CivException(CivSettings.localize.localizedString("adcmd_build_demolishPrompt"));
        }

        Town town = getNamedTown(1);

        if (args.length < 3) {
            CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_build_unbuildHeading"));
            for (Structure struct : town.getStructures()) {
                CivMessage.send(sender, CivSettings.localize.localizedString("var_cmd_build_demolish", struct.getDisplayName(), ChatColor.YELLOW + struct.getCorner().toString() + ChatColor.WHITE));
            }
            return;
        }

        BlockCoord coord = new BlockCoord(args[2]);
        Structure struct = town.getStructure(coord);
        if (struct == null) {
            CivMessage.send(sender, ChatColor.RED + CivSettings.localize.localizedString("NoStructureAt") + " " + args[2]);
            return;
        }

        struct.getTown().demolish(struct, true);


        CivMessage.sendTown(struct.getTown(), struct.getDisplayName() + " " + CivSettings.localize.localizedString("adcmd_build_demolishComplete"));
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
