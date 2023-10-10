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
import com.avrgaming.civcraft.endgame.EndConditionDiplomacy;
import com.avrgaming.civcraft.endgame.EndConditionScience;
import com.avrgaming.civcraft.endgame.EndGameCondition;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.Granary;
import com.avrgaming.civcraft.util.DecimalHelper;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class CivInfoCommand extends CommandBase {

    @Override
    public void init() {
        command = "/civ info";
        displayName = CivSettings.localize.localizedString("cmd_civ_info_name");

        register_sub("upkeep", this::upkeep_cmd, CivSettings.localize.localizedString("cmd_civ_info_upkeepDesc"));
        register_sub("taxes", this::taxes_cmd, CivSettings.localize.localizedString("cmd_civ_info_taxesDesc"));
        register_sub("beakers", this::beakers_cmd, CivSettings.localize.localizedString("cmd_civ_info_beakersDesc"));
        register_sub("online", this::online_cmd, CivSettings.localize.localizedString("cmd_civ_info_onlineDesc"));
        register_sub("granary", this::granary_cmd, CivSettings.localize.localizedString("cmd_civ_info_granaryDesc"));
    }

    public void granary_cmd() throws CivException {
        StringBuilder s = new StringBuilder();
        Civilization civ = getSenderCiv();
        for (Town t : civ.getTowns()) {
            s.append(ChatColor.GOLD + " [").append(t.getName()).append("] ");
            for (Granary g : t.getGranaries()) {
                s.append(g.getResources());
            }
        }
        CivMessage.send(sender, s.toString());
    }

    public void online_cmd() throws CivException {
        Civilization civ = getSenderCiv();

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("var_cmd_civ_info_onlineHeading", civ.getName()));
        StringBuilder out = new StringBuilder();
        for (Resident resident : civ.getOnlineResidents()) {
            out.append(resident.getName()).append(" ");
        }
        CivMessage.send(sender, out.toString());
    }

    public void beakers_cmd() throws CivException {
        Civilization civ = getSenderCiv();

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_info_beakersHeading"));
        ArrayList<String> out = new ArrayList<>();

        for (Town t : civ.getTowns()) {
            for (Buff b : t.getBuffManager().getEffectiveBuffs(Buff.SCIENCE_RATE)) {
                out.add(ChatColor.DARK_GREEN + CivSettings.localize.localizedString("From") + " " + b.getSource() + ": " + ChatColor.GREEN + b.getDisplayDouble());
            }
        }
		
	/*	for (Town t : civ.getTowns()) {
			for (BonusGoodie goodie : t.getEffectiveBonusGoodies()) {
				try {
					double bonus = Double.valueOf(goodie.getBonusValue("beaker_bonus"));
					out.add(CivColor.Green+"From Goodie "+goodie.getDisplayName()+": "+CivColor.LightGreen+(bonus*100)+"%");
					
				} catch (NumberFormatException e) {
					//Ignore this goodie might not have the bonus.
				}
				
				try {
					double bonus = Double.valueOf(goodie.getBonusValue("extra_beakers"));
					out.add(CivColor.Green+"From Goodie "+goodie.getDisplayName()+": "+CivColor.LightGreen+bonus);
					
				} catch (NumberFormatException e) {
					//Ignore this goodie might not have the bonus.
				}				
			}
		}*/

        out.add(ChatColor.AQUA + "------------------------------------");
        out.add(ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Total") + " " + ChatColor.GREEN + df.format(civ.getBeakers()));
        CivMessage.send(sender, out);
    }

    public void taxes_cmd() throws CivException {
        Civilization civ = getSenderCiv();

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_info_taxesHeading"));
        for (Town t : civ.getTowns()) {
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Town") + " " + ChatColor.GREEN + t.getName() + ChatColor.DARK_GREEN +
                    CivSettings.localize.localizedString("Total") + " " + ChatColor.GREEN + civ.lastTaxesPaidMap.get(t.getName()));
        }

    }

    private double getTownTotalLastTick(Town town, Civilization civ) {
        double total = 0;
        for (String key : civ.lastUpkeepPaidMap.keySet()) {
            String townName = key.split(",")[0];

            if (townName.equalsIgnoreCase(town.getName())) {
                total += civ.lastUpkeepPaidMap.get(key);
            }
        }
        return total;
    }

    public void upkeep_cmd() throws CivException {
        Civilization civ = getSenderCiv();

        if (args.length < 2) {
            CivMessage.sendHeading(sender, civ.getName() + CivSettings.localize.localizedString("cmd_civ_info_upkeepHeading"));

            for (Town town : civ.getTowns()) {
                CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Town") + " " + ChatColor.GREEN + town.getName() + ChatColor.DARK_GREEN +
                        CivSettings.localize.localizedString("Total") + " " + ChatColor.GREEN + getTownTotalLastTick(town, civ));
            }
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("WarColon") + " " + ChatColor.GREEN + df.format(civ.getWarUpkeep()));

            CivMessage.send(sender, ChatColor.GRAY + CivSettings.localize.localizedString("cmd_civ_info_upkeepHeading2"));
            CivMessage.send(sender, ChatColor.GRAY + CivSettings.localize.localizedString("cmd_civ_info_upkeepHeading3"));

        } else {

            Town town = civ.getTown(args[1]);
            if (town == null) {
                throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_info_upkeepTownInvalid", args[1]));
            }

            CivMessage.sendHeading(sender, CivSettings.localize.localizedString("var_cmd_civ_info_upkeepTownHeading1", town.getName()));
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Base") + " " + ChatColor.GREEN + civ.getUpkeepPaid(town, "base"));
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Distance") + " " + ChatColor.GREEN + civ.getUpkeepPaid(town, "distance"));
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("DistanceUpkeep") + " " + ChatColor.GREEN + civ.getUpkeepPaid(town, "distanceUpkeep"));
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Debt") + " " + ChatColor.GREEN + civ.getUpkeepPaid(town, "debt"));
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Total") + " " + ChatColor.GREEN + getTownTotalLastTick(town, civ));

            CivMessage.send(sender, ChatColor.GRAY + CivSettings.localize.localizedString("cmd_civ_info_upkeepHeading2"));
        }


    }


    @Override
    public void doDefaultAction() throws CivException {
        show_info();
        CivMessage.send(sender, ChatColor.GRAY + CivSettings.localize.localizedString("cmd_civ_info_help"));
    }

    public static void show(CommandSender sender, Resident resident, Civilization civ) {

        boolean isOP = false;
        if (sender instanceof Player) {
            try {
                if (CivGlobal.getPlayer(resident).isOp()) {
                    isOP = true;
                }
            } catch (CivException e) {
                /* Allow console to display. */
            }
        } else {
            /* We're the console. */
            isOP = true;
        }


        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("var_cmd_civ_info_showHeading", civ.getName()));

        CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Score") + " " + ChatColor.GREEN + civ.getScore() +
                ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("Towns") + " " + ChatColor.GREEN + civ.getTownCount());
        if (civ.getLeaderGroup() == null) {
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Leaders") + " " + ChatColor.RED + "NONE");
        } else {
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Leaders") + " " + ChatColor.GREEN + civ.getLeaderGroup().getMembersString());
        }

        if (civ.getAdviserGroup() == null) {
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Advisors") + " " + ChatColor.RED + "NONE");
        } else {
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Advisors") + " " + ChatColor.GREEN + civ.getAdviserGroup().getMembersString());
        }

        if (civ.hasResident(resident)) {
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("cmd_civ_info_showTax") + " " + ChatColor.GREEN + civ.getIncomeTaxRateString() +
                    ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("cmd_civ_info_showScience") + " " + ChatColor.GREEN + DecimalHelper.formatPercentage(civ.getSciencePercentage()));
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Beakers") + " " + ChatColor.GREEN + civ.getBeakers() +
                    ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("Online") + " " + ChatColor.GREEN + civ.getOnlineResidents().size());
        }

        if (resident == null || civ.getLeaderGroup().hasMember(resident) || civ.getAdviserGroup().hasMember(resident) || isOP) {
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Treasury") + " " + ChatColor.GREEN + civ.getTreasury().getBalance() + ChatColor.DARK_GREEN + " " + CivSettings.CURRENCY_NAME);
        }

        if (civ.getTreasury().inDebt()) {
            CivMessage.send(sender, ChatColor.YELLOW + CivSettings.localize.localizedString("InDebt") + " " + civ.getTreasury().getDebt() + " Coins.");
            CivMessage.send(sender, ChatColor.YELLOW + civ.getDaysLeftWarning());
        }

        for (EndGameCondition endCond : EndGameCondition.endConditions) {
            for (SessionEntry entry : CivGlobal.getSessionDB().lookup(endCond.getSessionKey())) {
                if (civ == EndGameCondition.getCivFromSessionData(entry.value)) {
                    int daysLeft = endCond.getDaysToHold() - endCond.getDaysHeldFromSessionData(entry.value);

                    CivMessage.send(sender,
                            CivSettings.localize.localizedString("var_cmd_civ_info_daysTillVictoryNew",
                                    String.valueOf(ChatColor.AQUA) + ChatColor.BOLD + civ.getName() + ChatColor.WHITE,
                                    String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + daysLeft + ChatColor.WHITE,
                                    String.valueOf(ChatColor.LIGHT_PURPLE) + ChatColor.BOLD + endCond.getVictoryName() + ChatColor.WHITE
                            ));
                    break;
                }
            }
        }

        Integer votes = EndConditionDiplomacy.getVotesFor(civ);
        if (votes > 0) {
            CivMessage.send(sender, CivSettings.localize.localizedString("var_cmd_civ_votesHeading", String.valueOf(ChatColor.AQUA) + ChatColor.BOLD + civ.getName() + ChatColor.WHITE,
                    String.valueOf(ChatColor.LIGHT_PURPLE) + ChatColor.BOLD + votes + ChatColor.WHITE));
        }

        Double beakers = EndConditionScience.getBeakersFor(civ);
        if (beakers > 0) {
            DecimalFormat df = new DecimalFormat("#.#");
            CivMessage.send(sender, CivSettings.localize.localizedString("var_cmd_civ_info_showBeakersTowardEnlight", String.valueOf(ChatColor.AQUA) + ChatColor.BOLD + civ.getName() + ChatColor.WHITE,
                    String.valueOf(ChatColor.LIGHT_PURPLE) + ChatColor.BOLD + df.format(beakers) + ChatColor.WHITE));
        }

        StringBuilder out = new StringBuilder(ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Towns") + " ");
        for (Town town : civ.getTowns()) {
            if (town.isCapitol()) {
                out.append(ChatColor.GOLD).append(town.getName());
            } else if (town.getMotherCiv() != null) {
                out.append(ChatColor.YELLOW).append(town.getName());
            } else {
                out.append(ChatColor.WHITE).append(town.getName());
            }
            out.append(", ");
        }

        CivMessage.send(sender, out.toString());
    }

    public void show_info() throws CivException {
        show(sender, getResident(), getSenderCiv());
    }

    @Override
    public void showHelp() {
        showBasicHelp();
    }

    @Override
    public void permissionCheck() {

    }


}
