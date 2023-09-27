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
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.TownHall;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class CivResearchCommand extends CommandBase {

    @Override
    public void init() {
        command = "/civ research";
        displayName = CivSettings.localize.localizedString("cmd_civ_research_name");

        register_sub("list", this::list_cmd, CivSettings.localize.localizedString("cmd_civ_research_listDesc"));
        register_sub("progress", this::progress_cmd, CivSettings.localize.localizedString("cmd_civ_research_progressDesc"));
        register_sub("on", this::on_cmd, CivSettings.localize.localizedString("cmd_civ_research_onDesc"));
        register_sub("change", this::change_cmd, CivSettings.localize.localizedString("cmd_civ_research_changeDesc"));
        register_sub("finished", this::finished_cmd, CivSettings.localize.localizedString("cmd_civ_research_finishedDesc"));
        register_sub("era", this::era_cmd, CivSettings.localize.localizedString("cmd_civ_research_eraDesc"));
        register_sub("e", this::era_cmd, null);
        register_sub("f", this::finished_cmd, null);
        register_sub("l", this::list_cmd, null);
        register_sub("p", this::progress_cmd, null);
    }

    public void change_cmd() throws CivException {
        Civilization civ = getSenderCiv();

        if (args.length < 2) {
            list_cmd();
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_research_changePrompt"));
        }

        String techname = combineArgs(stripArgs(args, 1));
        ConfigTech tech = CivSettings.getTechByName(techname);
        if (tech == null) {
            throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_research_NotFound", techname));
        }

        if (!civ.getTreasury().hasEnough(tech.getAdjustedTechCost(civ))) {
            throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_research_NotEnough1", CivSettings.CURRENCY_NAME, tech.name()));
        }

        if (!tech.isAvailable(civ)) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_research_NotAllowedNow"));
        }

        if (civ.getResearchTech() != null) {
            civ.setResearchProgress(0);
            CivMessage.send(sender, ChatColor.RED + CivSettings.localize.localizedString("var_cmd_civ_research_lostProgress1", civ.getResearchTech().name()));
            civ.setResearchTech(null);
        }

        civ.startTechnologyResearch(tech);
        if (sender instanceof Player p) {
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.1f, 1.0f);
        }
        CivMessage.sendCiv(civ, CivSettings.localize.localizedString("var_cmd_civ_research_start", tech.name()));
    }

    public void finished_cmd() throws CivException {
        Civilization civ = getSenderCiv();

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_research_finishedHeading"));
        StringBuilder out = new StringBuilder();
        for (ConfigTech tech : civ.getTechs()) {
            out.append(tech.name()).append(", ");
        }
        CivMessage.send(sender, out.toString());
    }

    public void on_cmd() throws CivException {
        Civilization civ = getSenderCiv();

        if (args.length < 2) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_research_onPrompt"));
        }

        Town capitol = CivGlobal.getTown(civ.getCapitolName());
        if (capitol == null) {
            throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_research_missingCapitol", civ.getCapitolName()) + " " + CivSettings.localize.localizedString("internalCommandException"));
        }

        TownHall townhall = capitol.getTownHall();
        if (townhall == null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_research_missingTownHall"));
        }

        if (!townhall.isActive()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_research_incompleteTownHall"));
        }

        String techname = combineArgs(stripArgs(args, 1));
        ConfigTech tech = CivSettings.getTechByName(techname);
        if (tech == null) {
            throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_research_NotFound", techname));
        }

        civ.startTechnologyResearch(tech);
        if (sender instanceof Player p) {
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.1f, 1.0f);
        }
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_research_start", tech.name()));
    }

    public void progress_cmd() throws CivException {
        Civilization civ = getSenderCiv();

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_research_current"));

        if (civ.getResearchTech() != null) {
            int percentageComplete = (int) ((civ.getResearchProgress() / civ.getResearchTech().getAdjustedBeakerCost(civ)) * 100);
            CivMessage.send(sender, CivSettings.localize.localizedString("var_cmd_civ_research_current", civ.getResearchTech().name(), percentageComplete, (civ.getResearchProgress() + " / " + civ.getResearchTech().getAdjustedBeakerCost(civ))));
        } else {
            CivMessage.send(sender, CivSettings.localize.localizedString("cmd_civ_research_NotAnything"));
        }

    }

    public void list_cmd() throws CivException {
        Civilization civ = getSenderCiv();
        ArrayList<ConfigTech> techs = ConfigTech.getAvailableTechs(civ);

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_research_Available"));
        for (ConfigTech tech : techs) {
            CivMessage.send(sender, tech.name() + ChatColor.GRAY + " " + CivSettings.localize.localizedString("Cost") + " " +
                    ChatColor.YELLOW + tech.getAdjustedTechCost(civ) + ChatColor.GRAY + " " + CivSettings.localize.localizedString("Beakers") + " " +
                    ChatColor.YELLOW + tech.getAdjustedBeakerCost(civ));
        }

    }

    public void era_cmd() throws CivException {
        Civilization civ = getSenderCiv();

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_research_era"));
        CivMessage.send(sender, ChatColor.WHITE + CivSettings.localize.localizedString("var_cmd_civ_research_currentEra", ChatColor.AQUA + CivGlobal.localizedEraString(civ.getCurrentEra())));
        CivMessage.send(sender, ChatColor.WHITE + CivSettings.localize.localizedString("var_cmd_civ_research_highestEra", ChatColor.AQUA + CivGlobal.localizedEraString(CivGlobal.highestCivEra)));

        double eraRate = ConfigTech.eraRate(civ);
        if (eraRate == 0.0) {
            CivMessage.send(sender, ChatColor.YELLOW + CivSettings.localize.localizedString("cmd_civ_research_eraNoDiscount"));
        } else {
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("var_cmd_civ_research_eraDiscount", (eraRate * 100), CivSettings.CURRENCY_NAME));

        }
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
        Resident resident = getResident();
        Civilization civ = getSenderCiv();

        if (!civ.getLeaderGroup().hasMember(resident) && !civ.getAdviserGroup().hasMember(resident)) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_research_notLeader"));
        }
    }

}
