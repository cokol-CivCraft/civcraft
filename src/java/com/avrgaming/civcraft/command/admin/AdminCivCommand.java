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
import com.avrgaming.civcraft.command.civ.CivInfoCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigGovernment;
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.endgame.EndConditionDiplomacy;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Relation;
import com.avrgaming.civcraft.object.Relation.Status;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import org.bukkit.ChatColor;

import java.sql.SQLException;

public class AdminCivCommand extends CommandBase {

    @Override
    public void init() {
        command = "/ad civ";
        displayName = CivSettings.localize.localizedString("adcmd_civ_name");

        register_sub("disband", this::disband_cmd, CivSettings.localize.localizedString("adcmd_civ_disbandDesc"));
        register_sub("addleader", this::addleader_cmd, CivSettings.localize.localizedString("adcmd_civ_addLeaderDesc"));
        register_sub("addadviser", this::addadviser_cmd, CivSettings.localize.localizedString("adcmd_civ_addAdvisorDesc"));
        register_sub("rmleader", this::rmleader_cmd, CivSettings.localize.localizedString("adcmd_civ_rmLeaderDesc"));
        register_sub("rmadviser", this::rmadviser_cmd, CivSettings.localize.localizedString("adcmd_civ_rmAdvisorDesc"));
        register_sub("givetech", this::givetech_cmd, CivSettings.localize.localizedString("adcmd_civ_giveTechDesc"));
        register_sub("beakerrate", this::beakerrate_cmd, CivSettings.localize.localizedString("adcmd_civ_beakerRateDesc"));
        register_sub("toggleadminciv", this::toggleadminciv_cmd, CivSettings.localize.localizedString("adcmd_civ_toggleadminCivDesc"));
        register_sub("alltech", this::alltech_cmd, CivSettings.localize.localizedString("adcmd_civ_alltechDesc"));
        register_sub("setrelation", this::setrelation_cmd, CivSettings.localize.localizedString("adcmd_civ_setRelationDesc"));
        register_sub("info", this::info_cmd, CivSettings.localize.localizedString("adcmd_civ_infoDesc"));
        register_sub("merge", this::merge_cmd, CivSettings.localize.localizedString("adcmd_civ_mergeDesc"));
        register_sub("setgov", this::setgov_cmd, CivSettings.localize.localizedString("adcmd_civ_setgovDesc"));
        register_sub("bankrupt", this::bankrupt_cmd, CivSettings.localize.localizedString("adcmd_civ_bankruptDesc"));
        register_sub("conquered", this::conquered_cmd, CivSettings.localize.localizedString("adcmd_civ_concqueredDesc"));
        register_sub("unconquer", this::unconquer_cmd, CivSettings.localize.localizedString("adcmd_civ_unconquerDesc"));
        register_sub("liberate", this::liberate_cmd, CivSettings.localize.localizedString("adcmd_civ_liberateDesc"));
        register_sub("setvotes", this::setvotes_cmd, CivSettings.localize.localizedString("adcmd_civ_setvotesDesc"));
        register_sub("rename", this::rename_cmd, CivSettings.localize.localizedString("adcmd_civ_renameDesc"));
    }

    public void liberate_cmd() throws CivException {
        Civilization motherCiv = getNamedCiv(1);

        /* Liberate the civ. */
        for (Town town : Town.getTowns()) {
            if (town.getMotherCiv() == motherCiv) {
                town.changeCiv(motherCiv);
                town.setMotherCiv(null);
            }
        }

        motherCiv.setConquered(false);
        CivGlobal.removeConqueredCiv(motherCiv);
        Civilization.addCiv(motherCiv);
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_liberateSuccess") + " " + motherCiv.getName());
    }

    public void rename_cmd() throws CivException {
        Civilization civ = getNamedCiv(1);
        String name = getNamedString(2, CivSettings.localize.localizedString("adcmd_civ_newNamePrompt"));

        if (args.length < 3) {
            throw new CivException(CivSettings.localize.localizedString("adcmd_civ_renameUseUnderscores"));
        }

        try {
            civ.rename(name);
        } catch (InvalidNameException e) {
            e.printStackTrace();
            throw new CivException(e.getMessage());
        }
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_renameCivSuccess"));
    }

    public void setvotes_cmd() throws CivException {
        Civilization civ = getNamedCiv(1);
        Integer votes = getNamedInteger(2);
        EndConditionDiplomacy.setVotes(civ, votes);
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_setVotesSuccess", civ.getName(), votes));
    }

    public void conquered_cmd() throws CivException {
        Civilization civ = getNamedCiv(1);
        civ.setConquered(true);
        Civilization.removeCiv(civ);
        CivGlobal.addConqueredCiv(civ);

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_conqueredSuccess"));
    }

    public void unconquer_cmd() throws CivException {
        String conquerCiv = this.getNamedString(1, "conquered civ");

        Civilization civ = CivGlobal.getConqueredCiv(conquerCiv);
        if (civ == null) {
            civ = Civilization.getByName(conquerCiv);
        }

        if (civ == null) {
            throw new CivException(CivSettings.localize.localizedString("var_adcmd_civ_NoCivByThatNane", conquerCiv));
        }

        civ.setConquered(false);
        CivGlobal.removeConqueredCiv(civ);
        Civilization.addCiv(civ);

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_unconquerSuccess"));
    }


    public void bankrupt_cmd() throws CivException {
        Civilization civ = getNamedCiv(1);

        if (args.length < 3) {
            CivMessage.send(sender, String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.localize.localizedString("adcmd_civ_bankruptConfirmPrompt"));
            CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_civ_bankruptConfirmCmd"));
        }

        civ.getTreasury().setBalance(0);

        for (Town town : civ.getTowns()) {
            town.getTreasury().setBalance(0);

            for (Resident resident : town.getResidents()) {
                resident.getTreasury().setBalance(0);
                resident.save();
            }
        }

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_bankruptSuccess", civ.getName()));
    }

    public void setgov_cmd() throws CivException {
        Civilization civ = getNamedCiv(1);

        if (args.length < 3) {
            throw new CivException(CivSettings.localize.localizedString("adcmd_civ_setgovPrompt"));
        }

        ConfigGovernment gov = CivSettings.governments.get(args[2]);
        if (gov == null) {
            throw new CivException(CivSettings.localize.localizedString("adcmd_civ_setGovInvalidGov") + " gov_monarchy, gov_depostism... etc");
        }
        // Remove any anarchy timers
        String key = "changegov_" + civ.getUUID();
        CivGlobal.getSessionDB().delete_all(key);

        civ.setGovernment(gov.id);
        CivMessage.global(CivSettings.localize.localizedString("var_adcmd_civ_setGovSuccessBroadcast", civ.getName(), CivSettings.governments.get(gov.id).displayName));
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_setGovSuccess"));

    }

    public void merge_cmd() throws CivException {
        Civilization oldciv = getNamedCiv(1);
        Civilization newciv = getNamedCiv(2);

        if (oldciv == newciv) {
            throw new CivException(CivSettings.localize.localizedString("adcmd_civ_mergeSameError"));
        }

        newciv.mergeInCiv(oldciv);
        CivMessage.global(CivSettings.localize.localizedString("var_adcmd_civ_mergeSuccess", oldciv.getName(), newciv.getName()));
    }

    public void info_cmd() throws CivException {
        Civilization civ = getNamedCiv(1);

        CivInfoCommand cmd = new CivInfoCommand();
        cmd.senderCivOverride = civ;
        cmd.onCommand(sender, null, "info", this.stripArgs(args, 2));
    }


    public void setrelation_cmd() throws CivException {
        if (args.length < 4) {
            throw new CivException(CivSettings.localize.localizedString("Usage") + " [civ] [otherCiv] [NEUTRAL|HOSTILE|WAR|PEACE|ALLY]");
        }

        Civilization civ = getNamedCiv(1);
        Civilization otherCiv = getNamedCiv(2);

        Relation.Status status = Relation.Status.valueOf(args[3].toUpperCase());

        CivGlobal.setRelation(civ, otherCiv, status);
        if (status.equals(Status.WAR)) {
            CivGlobal.setAggressor(civ, otherCiv, civ);
            CivGlobal.setAggressor(otherCiv, civ, civ);
        }
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_setrelationSuccess", civ.getName(), otherCiv.getName(), status.name()));

    }

    public void alltech_cmd() throws CivException {
        Civilization civ = getNamedCiv(1);

        for (ConfigTech tech : CivSettings.techs.values()) {
            civ.addTech(tech);
        }

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_alltechSuccess"));
    }

    public void toggleadminciv_cmd() throws CivException {
        Civilization civ = getNamedCiv(1);

        civ.setAdminCiv(!civ.isAdminCiv());
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_toggleAdminCivSuccess", civ.getName(), civ.isAdminCiv()));
    }

    public void beakerrate_cmd() throws CivException {
        Civilization civ = getNamedCiv(1);
        Double amount = getNamedDouble(2);

        civ.setBaseBeakers(amount);

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_beakerRateSuccess", civ.getName(), amount));
    }

    public void givetech_cmd() throws CivException {
        Civilization civ = getNamedCiv(1);

        if (args.length < 3) {
            throw new CivException(CivSettings.localize.localizedString("adcmd_civ_giveTechPrompt"));
        }

        ConfigTech tech = CivSettings.techs.get(args[2]);
        if (tech == null) {
            throw new CivException(CivSettings.localize.localizedString("adcmd_civ_giveTechInvalid") + args[2]);
        }

        if (civ.hasTechnology(tech.id())) {
            throw new CivException(CivSettings.localize.localizedString("var_adcmd_civ_giveTechAlreadyhas", civ.getName(), tech.id()));
        }

        civ.addTech(tech);

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_giveTechSuccess", tech.name(), civ.getName()));

    }

    public void rmadviser_cmd() throws CivException {
        Civilization civ = getNamedCiv(1);
        Resident resident = getNamedResident(2);

        if (civ.getAdviserGroup().hasMember(resident)) {
            civ.getAdviserGroup().removeMember(resident);
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_rmAdvisorSuccess", resident.getName(), civ.getName()));
        } else {
            CivMessage.sendError(sender, CivSettings.localize.localizedString("var_adcmd_civ_rmAdvisorNotInGroup", resident.getName(), civ.getName()));
        }
    }

    public void rmleader_cmd() throws CivException {
        Civilization civ = getNamedCiv(1);
        Resident resident = getNamedResident(2);

        if (civ.getLeaderGroup().hasMember(resident)) {
            civ.getLeaderGroup().removeMember(resident);
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_rmLeaderSuccess", resident.getName(), civ.getName()));
        } else {
            CivMessage.sendError(sender, CivSettings.localize.localizedString("var_adcmd_civ_rmLeaderNotInGroup", resident.getName(), civ.getName()));
        }
    }

    public void addadviser_cmd() throws CivException {
        Civilization civ = getNamedCiv(1);
        Resident resident = getNamedResident(2);

        civ.getAdviserGroup().addMember(resident);

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_addAdvisorSuccess", resident.getName(), civ.getName()));
    }

    public void addleader_cmd() throws CivException {
        Civilization civ = getNamedCiv(1);
        Resident resident = getNamedResident(2);

        civ.getLeaderGroup().addMember(resident);

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_addLeaderSuccess", resident.getName(), civ.getName()));
    }

    public void disband_cmd() throws CivException {
        Civilization civ = getNamedCiv(1);

        CivMessage.sendCiv(civ, CivSettings.localize.localizedString("adcmd_civ_disbandAlert"));
        try {
            civ.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_disbandSuccess"));
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
        //Admin is checked in parent command
    }

}
