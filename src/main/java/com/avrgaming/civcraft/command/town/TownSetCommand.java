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
package com.avrgaming.civcraft.command.town;


import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.*;
import org.bukkit.entity.Player;

public class TownSetCommand extends CommandBase {

    @Override
    public void init() {
        command = "/town set";
        displayName = CivSettings.localize.localizedString("cmd_town_set_name");

        register_sub("bankfee", this::bankfee_cmd, CivSettings.localize.localizedString("cmd_town_set_bankfeeDesc"));
        register_sub("storefee", this::storefee_cmd, CivSettings.localize.localizedString("cmd_town_set_storefeeDesc"));
        register_sub("grocerfee", this::grocerfee_cmd, CivSettings.localize.localizedString("cmd_town_set_grocerfeeDesc"));
        register_sub("libraryfee", this::libraryfee_cmd, CivSettings.localize.localizedString("cmd_town_set_libraryfeeDesc"));
        register_sub("blacksmithfee", this::blacksmithfee_cmd, CivSettings.localize.localizedString("cmd_town_set_blacksmithfeeDesc"));
        register_sub("stablefee", this::stablefee_cmd, CivSettings.localize.localizedString("cmd_town_set_stablefeeDesc"));

        register_sub("scoutrate", this::scoutrate_cmd, CivSettings.localize.localizedString("cmd_town_set_scoutrateDesc"));

    }

    public void stablefee_cmd() throws CivException {
        Town town = getSelectedTown();
        Integer feeInt = getNamedInteger(1);

        Structure struct = town.findStructureByConfigId("s_stable");
        if (struct == null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_set_stablefeeNone"));
        }
        if (feeInt < Stable.FEE_MIN || feeInt > Stable.FEE_MAX) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_set_stablefeeRates"));
        }
        Stable stable = (Stable) struct;
        stable.setNonResidentFee((double) feeInt / 100);
        stable.updateSignText();

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_set_feeSuccess", feeInt));
    }

    public void scoutrate_cmd() throws CivException {
        Town town = getSelectedTown();
        Integer rate = getNamedInteger(1);

        if (rate != 10 && rate != 30 && rate != 60) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_set_scoutrateRates"));
        }

        for (Structure struct : town.getStructures()) {
            if (struct instanceof ScoutTower) {
                ((ScoutTower) struct).setReportSeconds(rate);
            } else if (struct instanceof ScoutShip) {
                ((ScoutShip) struct).setReportSeconds(rate);
            }
        }

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_set_scoutrateSuccess", rate));
    }

    public void blacksmithfee_cmd() throws CivException {
        Town town = getSelectedTown();
        Integer feeInt = getNamedInteger(1);

        if (feeInt < 5 || feeInt > 15) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_set_rate5to15"));
        }

        Structure struct = town.findStructureByConfigId("s_blacksmith");
        if (struct == null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_set_blacksmithfeeNone"));
        }

        ((Blacksmith) struct).setNonResidentFee(((double) feeInt / 100));
        struct.updateSignText();

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_set_feeSuccess", feeInt));
    }


    public void libraryfee_cmd() throws CivException {
        Town town = getSelectedTown();
        Integer feeInt = getNamedInteger(1);

        if (feeInt < 5 || feeInt > 15) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_set_rate5to15"));
        }

        Structure struct = town.findStructureByConfigId("s_library");
        if (struct == null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_set_libraryfeeNone"));
        }

        ((Library) struct).setNonResidentFee(((double) feeInt / 100));
        struct.updateSignText();

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_set_feeSuccess", feeInt));
    }

    public void grocerfee_cmd() throws CivException {
        Town town = getSelectedTown();
        Integer feeInt = getNamedInteger(1);

        if (feeInt < 5 || feeInt > 15) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_set_rate5to15"));
        }

        Structure struct = town.findStructureByConfigId("s_grocer");
        if (struct == null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_set_grocerfeeNone"));
        }

        ((Grocer) struct).setNonResidentFee(((double) feeInt / 100));
        struct.updateSignText();

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_set_feeSuccess", feeInt));

    }

    public void storefee_cmd() throws CivException {
        Town town = getSelectedTown();
        Integer feeInt = getNamedInteger(1);

        if (feeInt < 5 || feeInt > 15) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_set_rate5to15"));
        }

        Structure struct = town.findStructureByConfigId("s_store");
        if (struct == null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_set_storefeeNone"));
        }

        ((Store) struct).setNonResidentFee(((double) feeInt / 100));
        struct.updateSignText();

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_set_feeSuccess", feeInt));

    }

    public void bankfee_cmd() throws CivException {
        Town town = getSelectedTown();
        Integer feeInt = getNamedInteger(1);

        if (feeInt < 5 || feeInt > 15) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_set_rate5to15"));
        }

        Structure struct = town.findStructureByConfigId("s_bank");
        if (struct == null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_set_bankfeeNone"));
        }

        ((Bank) struct).setNonResidentFee(((double) feeInt / 100));
        struct.updateSignText();

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_set_feeSuccess", feeInt));

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
        Town town = getSelectedTown();
        Player player = getPlayer();

        if (!town.playerIsInGroupName("mayors", player) && !town.playerIsInGroupName("assistants", player)) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_claimNoPerm"));
        }
    }

}
