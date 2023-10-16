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
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;

public class AdminChatCommand extends CommandBase {

    @Override
    public void init() {
        command = "/ad chat";
        displayName = CivSettings.localize.localizedString("adcmd_chat_name");

        register_sub("tc", this::tc_cmd, CivSettings.localize.localizedString("adcmd_chat_tcDesc"));
        register_sub("cc", this::cc_cmd, CivSettings.localize.localizedString("adcmd_chat_ccDesc"));
        register_sub("cclisten", this::cclisten_cmd, CivSettings.localize.localizedString("adcmd_chat_cclisten"));
        register_sub("tclisten", this::tclisten_cmd, CivSettings.localize.localizedString("adcmd_chat_tclisten"));
        register_sub("listenoff", this::listenoff_cmd, CivSettings.localize.localizedString("adcmd_chat_listenOffDesc"));
        register_sub("cclistenall", this::cclistenall_cmd, CivSettings.localize.localizedString("adcmd_chat_listenAllDesc"));
        register_sub("tclistenall", this::tclistenall_cmd, CivSettings.localize.localizedString("adcmd_chat_tclistenAllDesc"));
        register_sub("banwordon", this::banwordon_cmd, CivSettings.localize.localizedString("adcmd_chat_banWordOnDesc"));
        register_sub("banwordoff", this::banwordoff_cmd, CivSettings.localize.localizedString("adcmd_chat_banWordOffDesc"));
        register_sub("banwordadd", this::banwordadd_cmd, CivSettings.localize.localizedString("admcd_chat_banwordaddDesc"));
        register_sub("banwordremove", this::banwordremove_cmd, CivSettings.localize.localizedString("adcmd_chat_banwordremoveDesc"));
        register_sub("banwordtoggle", this::banwordtoggle, CivSettings.localize.localizedString("adcmd_chat_banwordToggleDesc"));

    }

    public void tclistenall_cmd() throws CivException {
        Resident resident = getResident();

        for (Town t : Town.getTowns()) {
            CivMessage.addExtraTownChatListener(t, resident.getName());
        }

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_tclistenAllSuccess"));
    }

    public void cclistenall_cmd() throws CivException {
        Resident resident = getResident();

        for (Civilization civ : Civilization.getCivs()) {
            CivMessage.addExtraCivChatListener(civ, resident.getName());
        }

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_cclistenAllSuccess"));
    }

    public void listenoff_cmd() throws CivException {
        Resident resident = getResident();

        for (Town t : Town.getTowns()) {
            CivMessage.removeExtraTownChatListener(t, resident.getName());
        }

        for (Civilization civ : Civilization.getCivs()) {
            CivMessage.removeExtraCivChatListener(civ, resident.getName());
        }

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_listenOffSuccess"));
    }

    public void cclisten_cmd() throws CivException {
        if (args.length < 2) {
            throw new CivException(CivSettings.localize.localizedString("EnterCivName"));
        }

        Resident resident = getResident();

        Civilization civ = getNamedCiv(1);

        for (String str : CivMessage.getExtraCivChatListeners(civ)) {
            if (str.equalsIgnoreCase(resident.getName())) {
                CivMessage.removeExtraCivChatListener(civ, str);
                CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_noLongerListenCiv") + " " + civ.getName());
                return;
            }
        }

        CivMessage.addExtraCivChatListener(civ, resident.getName());
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_listenCivSuccess") + " " + civ.getName());
    }

    public void tclisten_cmd() throws CivException {
        if (args.length < 2) {
            throw new CivException(CivSettings.localize.localizedString("EnterTownName"));
        }

        Resident resident = getResident();

        Town town = getNamedTown(1);

        for (String str : CivMessage.getExtraTownChatListeners(town)) {
            if (str.equalsIgnoreCase(resident.getName())) {
                CivMessage.removeExtraTownChatListener(town, str);
                CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_noLongerListenTown") + " " + town.getName());
                return;
            }
        }

        CivMessage.addExtraTownChatListener(town, resident.getName());
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_listenTownSuccess") + " " + town.getName());
    }

    public void tc_cmd() throws CivException {
        Resident resident = getResident();
        if (args.length < 2) {
            resident.setTownChat(false);
            resident.setTownChatOverride(null);
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_noLongerChattingInTown"));
            return;
        }

        Town town = getNamedTown(1);

        resident.setTownChat(true);
        resident.setTownChatOverride(town);
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_nowChattingInTown") + " " + town.getName());
    }

    public void cc_cmd() throws CivException {
        Resident resident = getResident();
        if (args.length < 2) {
            resident.setCivChat(false);
            resident.setCivChatOverride(null);
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_noLongerChattingInCiv"));
            return;
        }

        Civilization civ = getNamedCiv(1);

        resident.setCivChat(true);
        resident.setCivChatOverride(civ);
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_nowChattingInCiv") + " " + civ.getName());
    }

    public void banwordon_cmd() {
        CivGlobal.banWordsActive = true;
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_banwordsActivated"));
    }

    public void banwordoff_cmd() {
        CivGlobal.banWordsActive = false;
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_chat_banwordsDeactivated"));

    }

    public void banwordadd_cmd() throws CivException {
        if (args.length < 2) {
            throw new CivException(CivSettings.localize.localizedString("adcmd_chat_addBanwordPrompt"));
        }

        CivGlobal.banWords.add(args[1]);
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_chat_banwordadded", args[1]));
    }

    public void banwordremove_cmd() throws CivException {
        if (args.length < 2) {
            throw new CivException(CivSettings.localize.localizedString("adcmd_chat_removeBanwordPrompt"));
        }

        CivGlobal.banWords.remove(args[1]);
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_chat_banwordremoved", args[1]));
    }

    public void banwordtoggle() {

        CivGlobal.banWordsAlways = !CivGlobal.banWordsAlways;
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("admcd_chat_banwordAlways") + " " + CivGlobal.banWordsAlways);
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
