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
package com.avrgaming.civcraft.command.civ;


import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Relation;
import com.avrgaming.civcraft.object.Relation.Status;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.questions.CapitulateRequest;
import com.avrgaming.civcraft.questions.ChangeRelationResponse;
import com.avrgaming.civcraft.threading.tasks.CivQuestionTask;
import com.avrgaming.civcraft.war.War;
import org.bukkit.ChatColor;

import java.util.HashSet;

public class CivDiplomacyCommand extends CommandBase {
    public static final long INVITE_TIMEOUT = 30000; //30 seconds

    @Override
    public void init() {
        command = "/civ dip";
        displayName = CivSettings.localize.localizedString("cmd_civ_dip_name");

        register_sub("show", this::show_cmd, CivSettings.localize.localizedString("cmd_civ_dip_showDesc"));
        register_sub("declare", this::declare_cmd, CivSettings.localize.localizedString("cmd_civ_dip_declareDesc"));
        register_sub("request", this::request_cmd, CivSettings.localize.localizedString("cmd_civ_dip_requestDesc"));
        register_sub("gift", this::gift_cmd, CivSettings.localize.localizedString("cmd_civ_dip_giftDesc"));
        register_sub("global", this::global_cmd, CivSettings.localize.localizedString("cmd_civ_dip_globalDesc"));
        register_sub("wars", this::wars_cmd, CivSettings.localize.localizedString("cmd_civ_dip_warsDesc"));
        register_sub("respond", this::respond_cmd, CivSettings.localize.localizedString("cmd_civ_dip_respondDesc"));
        register_sub("liberate", this::liberate_cmd, CivSettings.localize.localizedString("cmd_civ_dip_liberateDesc"));
        register_sub("capitulate", this::capitulate_cmd, CivSettings.localize.localizedString("cmd_civ_dip_capitulateDesc"));
    }

    public void capitulate_cmd() throws CivException {
        if (War.isWarTime()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_errorDuringWar"));
        }
        Town town = getNamedTown(1);
        Resident resident = getResident();
        boolean entireCiv = false;

        Civilization motherCiv = town.getMotherCiv();

        if (motherCiv == null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_capitulateErrorNoMother"));
        }

        if (!town.getMotherCiv().getLeaderGroup().hasMember(resident)) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_capitulateErrorNotLeader"));
        }

        if (town.getMotherCiv().getCapitolName().equals(town.getName())) {
            entireCiv = true;
        }

        CapitulateRequest capitulateResponse = new CapitulateRequest();

        if (args.length < 3 || !args[2].equalsIgnoreCase("yes")) {
            if (entireCiv) {
                CivMessage.send(sender, String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_civ_dip_capitulateConfirm1", town.getCiv().getName()));
                CivMessage.send(sender, String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_civ_dip_capitulateConfirm3", town.getName()));
            } else {
                CivMessage.send(sender, String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_civ_dip_capitulateConfirm1b", town.getCiv().getName()));
                CivMessage.send(sender, String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_civ_dip_capitulateConfirm3", town.getName()));
            }
            return;
        }

        String requestMessage;
        if (entireCiv) {
            requestMessage = String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_civ_dip_capitulateRequest1", motherCiv.getName());
            capitulateResponse.from = town.getMotherCiv().getName();
        } else {
            capitulateResponse.from = "Town of " + town.getName();
            requestMessage = String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.localize.localizedString("cmd_civ_dip_capitulateRequest1b", town.getName());
        }

        capitulateResponse.playerName = resident.getName();
        capitulateResponse.capitulator = town;
        capitulateResponse.to = town.getCiv().getName();

        CivGlobal.requestRelation(motherCiv, town.getCiv(), requestMessage, INVITE_TIMEOUT, capitulateResponse);
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_civ_dip_capitulateSuccess"));

    }

    public void liberate_cmd() throws CivException {
        if (War.isWarTime()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_errorDuringWar"));
        }
        this.validLeader();
        Town town = getNamedTown(1);
        Civilization civ = getSenderCiv();

        if (town.getCiv() != civ) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_liberateNotInCiv"));
        }

        Civilization motherCiv = town.getMotherCiv();
        if (motherCiv == null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_liberateNotCaptured"));
        }

        if (town.getName().equals(motherCiv.getCapitolName())) {
            Civilization capitolOwnerCiv = town.getCiv();

            /* Liberate the civ. */
            for (Town t : Town.getTowns()) {
                if (t.getMotherCiv() == motherCiv && t.getCiv() == capitolOwnerCiv) {
                    t.changeCiv(motherCiv);
                    t.setMotherCiv(null);
                    t.save();
                }
            }

            motherCiv.setConquered(false);
            CivGlobal.removeConqueredCiv(motherCiv);
            Civilization.addCiv(motherCiv);
            ;
            CivMessage.global(CivSettings.localize.localizedString("var_cmd_civ_liberateSuccess1", motherCiv.getName(), civ.getName()));
        } else {
            if (motherCiv.isConquered()) {
                throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_liberateError1", town.getName()));
            }

            /* Liberate just the town. */
            town.changeCiv(motherCiv);
            town.setMotherCiv(null);
            CivMessage.global(CivSettings.localize.localizedString("var_cmd_town_liberateSuccess", town.getName(), civ.getName(), motherCiv.getName()));
        }
    }

    public void gift_cmd() throws CivException {
        CivDiplomacyGiftCommand cmd = new CivDiplomacyGiftCommand();
        if (War.isWarTime()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_errorDuringWar"));
        }
        cmd.onCommand(sender, null, "gift", this.stripArgs(args, 1));
    }


    public void global_cmd() {
        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_globalHeading"));

        for (Civilization civ : Civilization.getCivs()) {
            for (Relation relation : civ.getDiplomacyManager().getRelations()) {
                CivMessage.send(sender, civ.getName() + ": " + relation.toString());
            }
        }
    }

    public void wars_cmd() {
        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_warsHeading"));
        HashSet<String> usedRelations = new HashSet<>();

        for (Civilization civ : Civilization.getCivs()) {
            for (Relation relation : civ.getDiplomacyManager().getRelations()) {
                if (relation.getStatus().equals(Status.WAR)) {
                    if (!usedRelations.contains(relation.getPairKey())) {
                        CivMessage.send(sender,
                                String.valueOf(ChatColor.AQUA) + ChatColor.BOLD + relation.getCiv().getName() + ChatColor.RED + " <-- " + CivSettings.localize.localizedString("WAR") + " --> " + ChatColor.AQUA + ChatColor.BOLD + relation.getOtherCiv().getName());
                        usedRelations.add(relation.getPairKey());
                    }
                }
            }
        }
    }

    public void respond_cmd() throws CivException {
        validLeaderAdvisor();
        if (War.isWarTime()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_errorDuringWar"));
        }

        if (args.length < 2) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_respondPrompt"));
        }

        CivQuestionTask task = CivGlobal.getCivQuestionTask(getSenderCiv());
        if (task == null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_respondNoRequest"));
        }

        if (args[1].equalsIgnoreCase("yes")) {
            synchronized (task) {
                task.setResponse("accept");
                task.notifyAll();
            }
        } else if (args[1].equalsIgnoreCase("no")) {
            synchronized (task) {
                task.setResponse("decline");
                task.notifyAll();
            }
        } else {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_respondPrompt"));
        }

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_civ_dip_respondSuccess"));
    }

    public void request_cmd() throws CivException {
        validLeaderAdvisor();
        Civilization ourCiv = getSenderCiv();
        if (War.isWarTime()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_errorDuringWar"));
        }

        if (args.length < 3) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_requestPrompt"));
        }

        Civilization otherCiv = getNamedCiv(1);

        if (ourCiv.getUUID().equals(otherCiv.getUUID())) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_requestSameCiv"));
        }

        try {
            Relation.Status status = Relation.Status.valueOf(args[2].toUpperCase());
            Relation.Status currentStatus = ourCiv.getDiplomacyManager().getRelationStatus(otherCiv);

            if (currentStatus == status) {
                throw new CivException(CivSettings.localize.localizedString("var_AlreadyStatusWithCiv", status.name(), otherCiv.getName()));
            }

            String message = String.valueOf(ChatColor.GREEN) + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_civ_dip_requestHasRequested", ourCiv.getName()) + " ";
            switch (status) {
                case NEUTRAL -> message += CivSettings.localize.localizedString("cmd_civ_dip_requestNeutral");
                case PEACE -> message += CivSettings.localize.localizedString("cmd_civ_dip_requestPeace");
                case ALLY -> {
                    message += CivSettings.localize.localizedString("cmd_civ_dip_requestAlly");
                    if (War.isWithinWarDeclareDays()) {
                        if (ourCiv.getDiplomacyManager().isAtWar() || otherCiv.getDiplomacyManager().isAtWar()) {
                            throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_dip_requestErrorWar1", War.getTimeDeclareDays()));
                        }
                    }
                }
                case WAR -> {
                    if (!CivGlobal.isCasualMode()) {
                        throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_requestErrorCasual"));
                    }
                    message += CivSettings.localize.localizedString("cmd_civ_dip_requestWar");
                }
                default -> throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_requestPrompt"));
            }
            message += ". " + CivSettings.localize.localizedString("cmd_civ_dip_requestQuestion");

            ChangeRelationResponse relationresponse = new ChangeRelationResponse();
            relationresponse.fromCiv = ourCiv;
            relationresponse.toCiv = otherCiv;
            relationresponse.status = status;

            CivGlobal.requestRelation(ourCiv, otherCiv,
                    message,
                    INVITE_TIMEOUT, relationresponse);

            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_civ_dip_requestSuccess"));
        } catch (IllegalArgumentException e) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_requestInvalid"));
        }

    }

    public void declare_cmd() throws CivException {
        validLeaderAdvisor();
        Civilization ourCiv = getSenderCiv();
        if (War.isWarTime()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_errorDuringWar"));
        }

        if (args.length < 3) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_declarePrompt"));
        }

        Civilization otherCiv = getNamedCiv(1);

        if (ourCiv.getUUID().equals(otherCiv.getUUID())) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_declareYourself"));
        }

        if (otherCiv.isAdminCiv()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_declareAdmin"));
        }

        try {
            Relation.Status status = Relation.Status.valueOf(args[2].toUpperCase());
            Relation.Status currentStatus = ourCiv.getDiplomacyManager().getRelationStatus(otherCiv);
            //boolean aidingAlly = false;

            if (currentStatus == status) {
                throw new CivException(CivSettings.localize.localizedString("var_AlreadyStatusWithCiv", status.name(), otherCiv.getName()));
            }

            switch (status) {
                case HOSTILE -> {
                    if (currentStatus == Status.WAR) {
                        throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_dip_declareAtWar", status.name()));
                    }
                }
                case WAR -> {
                    if (CivGlobal.isCasualMode()) {
                        throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_declareCasual"));
                    }
                    if (War.isWarTime()) {
                        throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_errorDuringWar"));
                    }
                    if (War.isWithinWarDeclareDays()) {
                        if (War.isCivAggressorToAlly(otherCiv, ourCiv)) {
                            if (War.isWithinAllyDeclareHours()) {
                                throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_dip_declareTooCloseToWar1", War.getAllyDeclareHours()));
                            }  //aidingAlly = true;

                        } else {
                            throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_dip_declareTooCloseToWar2", War.getTimeDeclareDays()));
                        }
                    }
                    if (ourCiv.getTreasury().inDebt()) {
                        throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_declareInDebt"));
                    }
                }
                default -> throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_declareInvalid"));
            }

            CivGlobal.setRelation(ourCiv, otherCiv, status);
            //Boolean aidingAlly is in commentaries a couple lines higher (2 times)
            CivGlobal.setAggressor(ourCiv, otherCiv, ourCiv);

        } catch (IllegalArgumentException e) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_declareUnknown"));
        }

    }

    public void show_cmd() throws CivException {
        if (args.length < 2) {
            show(getSenderCiv());
            return;
        }

        Civilization civ = getNamedCiv(1);

        show(civ);
    }

    public void show(Civilization civ) {
        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("var_cmd_civ_dip_showHeading", ChatColor.YELLOW + civ.getName()));

        for (Relation relation : civ.getDiplomacyManager().getRelations()) {
            if (relation.getStatus() == Relation.Status.NEUTRAL) {
                continue;
            }
            CivMessage.send(sender, relation.toString());
        }

        int warCount = civ.getDiplomacyManager().getWarCount();
        if (warCount != 0) {
            CivMessage.send(sender, ChatColor.RED + CivSettings.localize.localizedString("var_cmd_civ_dip_showSuccess1", warCount));
        }
        CivMessage.send(sender, ChatColor.GRAY + CivSettings.localize.localizedString("cmd_civ_dip_showNeutral"));
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
