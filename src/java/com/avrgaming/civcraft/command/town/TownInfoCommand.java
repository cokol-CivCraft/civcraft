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
import com.avrgaming.civcraft.config.ConfigCultureLevel;
import com.avrgaming.civcraft.config.ConfigHappinessState;
import com.avrgaming.civcraft.config.ConfigTownLevel;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.*;
import com.avrgaming.civcraft.object.Relation.Status;
import com.avrgaming.civcraft.structure.*;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class TownInfoCommand extends CommandBase {

    @Override
    public void init() {
        command = "/town info";
        displayName = CivSettings.localize.localizedString("cmd_town_info_name");

        register_sub("upkeep", this::upkeep_cmd, CivSettings.localize.localizedString("cmd_town_info_upkeepDesc"));
        register_sub("cottage", this::cottage_cmd, CivSettings.localize.localizedString("cmd_town_info_cottageDesc"));
        register_sub("temple", this::temple_cmd, CivSettings.localize.localizedString("cmd_town_info_templeDesc"));
        register_sub("structures", this::structures_cmd, CivSettings.localize.localizedString("cmd_town_info_structuresDesc"));
        register_sub("culture", this::culture_cmd, CivSettings.localize.localizedString("cmd_town_info_cultureDesc"));
        register_sub("trade", this::trade_cmd, CivSettings.localize.localizedString("cmd_town_info_tradeDesc"));
        register_sub("mine", this::mine_cmd, CivSettings.localize.localizedString("cmd_town_info_mineDesc"));
        register_sub("hammers", this::hammers_cmd, CivSettings.localize.localizedString("cmd_town_info_hammersDesc"));
        register_sub("goodies", this::goodies_cmd, CivSettings.localize.localizedString("cmd_town_info_goodiesDesc"));
        register_sub("rates", this::rates_cmd, CivSettings.localize.localizedString("cmd_town_info_ratesDesc"));
        register_sub("growth", this::growth_cmd, CivSettings.localize.localizedString("cmd_town_info_growthDesc"));
        register_sub("buffs", this::buffs_cmd, CivSettings.localize.localizedString("cmd_town_info_buffsDesc"));
        register_sub("online", this::online_cmd, CivSettings.localize.localizedString("cmd_town_info_onlineDesc"));
        register_sub("happiness", this::happiness_cmd, CivSettings.localize.localizedString("cmd_town_info_happinessDesc"));
        register_sub("beakers", this::beakers_cmd, CivSettings.localize.localizedString("cmd_town_info_beakersDesc"));
        register_sub("area", this::area_cmd, CivSettings.localize.localizedString("cmd_town_info_areaDesc"));
        register_sub("granary", this::granary_cmd, CivSettings.localize.localizedString("cmd_town_info_granaryDesc"));
		register_sub("disabled", this::disabled_cmd, CivSettings.localize.localizedString("cmd_town_info_disabledDesc"));
	}

    public void granary_cmd() throws CivException {
        Town t = getSelectedTown();
        StringBuilder s = new StringBuilder();
        for (Granary g : t.getGranaries()) {
            s.append(g.getResources());
		}
        CivMessage.send(sender, s.toString());
	}

	public void disabled_cmd() throws CivException {
		Town town = getSelectedTown();

		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_info_disabledHeading"));
		LinkedList<String> out = new LinkedList<>();
		boolean showhelp = false;

		for (Buildable buildable : town.getDisabledBuildables()) {
			showhelp = true;
			out.add(ChatColor.DARK_GREEN+buildable.getDisplayName()+ChatColor.GREEN+" "+CivSettings.localize.localizedString("Coord")+buildable.getCorner().toString());
		}

		if (showhelp) {
			out.add(ChatColor.GRAY + CivSettings.localize.localizedString("cmd_town_info_disabledHelp1"));
			out.add(ChatColor.GRAY + CivSettings.localize.localizedString("cmd_town_info_disabledHelp2"));
			out.add(ChatColor.GRAY + CivSettings.localize.localizedString("cmd_town_info_disabledHelp3"));
			out.add(ChatColor.GRAY + CivSettings.localize.localizedString("cmd_town_info_disabledHelp4"));
			out.add(ChatColor.GRAY + CivSettings.localize.localizedString("cmd_town_info_disabledHelp5"));
		}

		CivMessage.send(sender, out);
	}

	private String buildStringFromHash(HashMap<String, Integer> hm) {
		StringBuilder s = new StringBuilder();
		for (String ss : hm.keySet()) {
            s.append(ChatColor.GREEN).append(ss).append(ChatColor.GRAY).append("( ").append(hm.get(ss)).append(" ), ");
		}
        return s.toString();
	}

	public void area_cmd() throws CivException {
		Town town = getSelectedTown();

		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_info_areaHeading"));
		HashMap<String, Integer> biomes = new HashMap<>();
		HashMap<String, Integer> trades = new HashMap<>();

		double hammers = 0.0;
		double growth = 0.0;
		double happiness = 0.0;
		double beakers = 0.0;
		DecimalFormat df = new DecimalFormat();

		for (CultureChunk cc : town.getCultureChunks()) {
			/* Increment biome counts. */
			if (!biomes.containsKey(cc.getBiome().name())) {
				biomes.put(cc.getBiome().name(), 1);
			} else {
				Integer value = biomes.get(cc.getBiome().name());
				biomes.put(cc.getBiome().name(), value + 1);
			}
			for (TradeGood tg : CivGlobal.getTradeGoods()) {
				if (tg.getChunk().equals(cc.getChunkCoord().getChunk())) {
					trades.merge(tg.getName(), 1, Integer::sum);
				}
			}
            hammers += cc.getHammers();
            growth += cc.getGrowth();
            happiness += cc.getHappiness();
            beakers += cc.getBeakers();
        }

        CivMessage.send(sender, ChatColor.AQUA + CivSettings.localize.localizedString("cmd_town_biomeList"));
        StringBuilder out = new StringBuilder();
        //int totalBiomes = 0;
        for (String biome : biomes.keySet()) {
            Integer count = biomes.get(biome);
            out.append(ChatColor.DARK_GREEN).append(biome).append(": ").append(ChatColor.GREEN).append(count).append(ChatColor.DARK_GREEN).append(", ");
			//	totalBiomes += count;
		}
		CivMessage.send(sender, out.toString());

		//CivMessage.send(sender, CivColor.Green+"Biome Count:"+CivColor.LightGreen+totalBiomes);

		CivMessage.send(sender, ChatColor.AQUA + "Totals");
		CivMessage.send(sender, ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("cmd_town_happiness") + " " + ChatColor.GREEN + df.format(happiness) +
				ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("Hammers") + " " + ChatColor.GREEN + df.format(hammers) +
				ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("cmd_town_growth") + " " + ChatColor.GREEN + df.format(growth) +
				ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("Beakers") + " " + ChatColor.GREEN + df.format(beakers) +
				buildStringFromHash(trades));

	}

	public void beakers_cmd() throws CivException {
		Town town = getSelectedTown();

		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_info_beakersHeading"));

		AttrSource beakerSources = town.getBeakers();
		CivMessage.send(sender, beakerSources.getSourceDisplayString(String.valueOf(ChatColor.DARK_GREEN), String.valueOf(ChatColor.GREEN)));
//		CivMessage.send(sender, beakerSources.getRateDisplayString(CivColor.Green, CivColor.LightGreen));
        CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("cmd_civ_gov_infoBeaker") + " " + ChatColor.GREEN + (town.getBeakerRate().total * 100 + "%"));
        CivMessage.send(sender, beakerSources.getTotalDisplayString(String.valueOf(ChatColor.DARK_GREEN), String.valueOf(ChatColor.GREEN)));

    }

    public void happiness_cmd() throws CivException {
        Town town = getSelectedTown();

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_info_happinessHeading"));
        ArrayList<String> out = new ArrayList<>();

        out.add(CivMessage.buildSmallTitle(CivSettings.localize.localizedString("cmd_town_info_happinessSources")));
        AttrSource happySources = town.getHappiness();

        DecimalFormat df = new DecimalFormat();
        df.applyPattern("###,###");
        for (String source : happySources.sources.keySet()) {
            Double value = happySources.sources.get(source);
            out.add(ChatColor.DARK_GREEN + source + ": " + ChatColor.GREEN + df.format(value));
        }
        out.add(ChatColor.LIGHT_PURPLE + CivSettings.localize.localizedString("Total") + " " + ChatColor.GREEN + df.format(happySources.total));


        out.add(CivMessage.buildSmallTitle(CivSettings.localize.localizedString("cmd_town_info_happinessUnhappy")));
        AttrSource unhappySources = town.getUnhappiness();
        for (String source : unhappySources.sources.keySet()) {
            Double value = unhappySources.sources.get(source);
            out.add(ChatColor.DARK_GREEN + source + ": " + ChatColor.GREEN + value);
        }
        out.add(ChatColor.LIGHT_PURPLE + CivSettings.localize.localizedString("Total") + " " + ChatColor.GREEN + df.format(unhappySources.total));

        out.add(CivMessage.buildSmallTitle(CivSettings.localize.localizedString("Total")));
        ConfigHappinessState state = town.getHappinessState();
        out.add(ChatColor.GREEN + df.format(town.getHappinessPercentage() * 100) + "%" + ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("cmd_town_info_happinessState") + " " + state.color() + state.name());
        CivMessage.send(sender, out);


    }

    public void online_cmd() throws CivException {
        Town town = getSelectedTown();

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("var_cmd_town_info_onlineHeading", town.getName()));
        StringBuilder out = new StringBuilder();
        for (Resident resident : town.getOnlineResidents()) {
            out.append(resident.getName()).append(" ");
        }
        CivMessage.send(sender, out.toString());
    }

    public void buffs_cmd() throws CivException {
        Town town = getSelectedTown();

        CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_buffsHeading"));
        ArrayList<String> out = new ArrayList<>();

        for (Buff buff : town.getBuffManager().getAllBuffs()) {
            out.add(ChatColor.DARK_GREEN + CivSettings.localize.localizedString("var_BuffsFrom", (ChatColor.GREEN + buff.getDisplayName() + ChatColor.DARK_GREEN), ChatColor.GREEN + buff.getSource()));
        }

        CivMessage.send(sender, out);
    }

    public void growth_cmd() throws CivException {
        Town town = getSelectedTown();
        AttrSource growthSources = town.getGrowth();

        CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_growthHeading"));
        CivMessage.send(sender, growthSources.getSourceDisplayString(String.valueOf(ChatColor.DARK_GREEN), String.valueOf(ChatColor.GREEN)));
        CivMessage.send(sender, growthSources.getRateDisplayString(String.valueOf(ChatColor.DARK_GREEN), String.valueOf(ChatColor.GREEN)));
        CivMessage.send(sender, growthSources.getTotalDisplayString(String.valueOf(ChatColor.DARK_GREEN), String.valueOf(ChatColor.GREEN)));
    }

    public void goodies_cmd() throws CivException {
        Town town = getSelectedTown();
        CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_goodiesHeading"));
        //	HashSet<BonusGoodie> effectiveGoodies = town.getEffectiveBonusGoodies();

        for (BonusGoodie goodie : town.getBonusGoodies()) {
            CivMessage.send(sender, ChatColor.GREEN + goodie.getDisplayName());
            String goodBonuses = goodie.getBonusDisplayString();

            String[] split = goodBonuses.split(";");
            for (String str : split) {
                CivMessage.send(sender, "    " + ChatColor.LIGHT_PURPLE + str);
            }

        }
    }

    public void hammers_cmd() throws CivException {
        Town town = getSelectedTown();

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_info_hammersHeading"));
        AttrSource hammerSources = town.getHammers();

        CivMessage.send(sender, hammerSources.getSourceDisplayString(String.valueOf(ChatColor.DARK_GREEN), String.valueOf(ChatColor.GREEN)));
        CivMessage.send(sender, hammerSources.getRateDisplayString(String.valueOf(ChatColor.DARK_GREEN), String.valueOf(ChatColor.GREEN)));
        CivMessage.send(sender, hammerSources.getTotalDisplayString(String.valueOf(ChatColor.DARK_GREEN), String.valueOf(ChatColor.GREEN)));
    }

    public void culture_cmd() throws CivException {
        Town town = getSelectedTown();
        AttrSource cultureSources = town.getCulture();

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_info_cultureHeading"));

        CivMessage.send(sender, cultureSources.getSourceDisplayString(String.valueOf(ChatColor.DARK_GREEN), String.valueOf(ChatColor.GREEN)));
        CivMessage.send(sender, cultureSources.getRateDisplayString(String.valueOf(ChatColor.DARK_GREEN), String.valueOf(ChatColor.GREEN)));
        CivMessage.send(sender, cultureSources.getTotalDisplayString(String.valueOf(ChatColor.DARK_GREEN), String.valueOf(ChatColor.GREEN)));

    }


    public void rates_cmd() throws CivException {
        Town town = getSelectedTown();

        CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_ratesHeading"));

        DecimalFormat df = new DecimalFormat("#,###.#");

        CivMessage.send(sender,
                ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("cmd_civ_gov_infoGrowth") + " " + ChatColor.GREEN + df.format(town.getGrowthRate().total * 100) +
                        ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("cmd_civ_gov_infoCulture") + " " + ChatColor.GREEN + df.format(town.getCultureRate().total * 100) +
                        ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("cmd_civ_gov_infoCottage") + " " + ChatColor.GREEN + df.format(town.getCottageRate() * 100) +
                        ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("Temple") + " " + ChatColor.GREEN + df.format(town.getTempleRate() * 100) +
                        ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("cmd_civ_gov_infoTrade") + " " + ChatColor.GREEN + df.format(town.getTradeRate() * 100) +
                        ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("cmd_civ_gov_infoBeaker") + " " + ChatColor.GREEN + df.format(town.getBeakerRate().total * 100)
        );

    }

    public static void show(CommandSender sender, Resident resident, Town town, Civilization civ, CommandBase parent) throws CivException {

        DecimalFormat df = new DecimalFormat();

        boolean isAdmin = resident == null;

        CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_showHeading"));
        ConfigTownLevel level = CivSettings.townLevels.get(town.getLevel());

        CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Civilization") + " " + ChatColor.GREEN + town.getCiv().getName());
        CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("TownLevel") + " " + ChatColor.GREEN + town.getLevel() + " (" + town.getLevelTitle() + ") " +
                ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Score") + " " + ChatColor.GREEN + town.getScore());

        if (town.getMayorGroup() == null) {
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Mayors") + " " + ChatColor.RED + CivSettings.localize.localizedString("none"));
        } else {
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Mayors") + " " + ChatColor.GREEN + town.getMayorGroup().getMembersString());
        }

        if (town.getAssistantGroup() == null) {
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Assitants") + " " + ChatColor.RED + CivSettings.localize.localizedString("none"));
        } else {
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Assitants") + " " + ChatColor.GREEN + town.getAssistantGroup().getMembersString());
        }

        if (resident == null || civ.hasResident(resident)) {

            String color = String.valueOf(ChatColor.GREEN);
            int maxTileImprovements = level.tile_improvements;
            if (town.getBuffManager().hasBuff("buff_mother_tree_tile_improvement_bonus")) {
                maxTileImprovements *= 2;
            }

            if (town.getTileImprovementCount() > maxTileImprovements) {
                color = String.valueOf(ChatColor.RED);
            }

            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Plots") + " " + ChatColor.GREEN + "(" + town.getTownChunks().size() + "/" + town.getMaxPlots() + ") " +
                    ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("TileImprovements") + " " + ChatColor.GREEN + "(" + color + town.getTileImprovementCount() + ChatColor.GREEN + "/" + maxTileImprovements + ")");


            //CivMessage.send(sender, CivColor.Green+"Outposts: "+CivColor.LightGreen+town.getOutpostChunks().size()+" "+
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Growth") + " " + ChatColor.GREEN + df.format(town.getGrowth().total) + " " +
                    ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Hammers") + " " + ChatColor.GREEN + df.format(town.getHammers().total) + " " +
                    ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Beakers") + " " + ChatColor.GREEN + df.format(town.getBeakers().total));


            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Members") + " " + ChatColor.GREEN + town.getResidentCount() + " " + CivSettings.CURRENCY_NAME);

            HashMap<String, String> info = new HashMap<>();
//			info.put("Happiness", CivColor.White+"("+CivColor.LightGreen+"H"+CivColor.Yellow+town.getHappinessTotal()
//					+CivColor.White+"/"+CivColor.Rose+"U"+CivColor.Yellow+town.getUnhappinessTotal()+CivColor.White+") = "+
//					CivColor.LightGreen+df.format(town.getHappinessPercentage()*100)+"%");
            info.put(CivSettings.localize.localizedString("Happiness"), ChatColor.GREEN + df.format(Math.floor(town.getHappinessPercentage() * 100)) + "%");
            ConfigHappinessState state = town.getHappinessState();
            info.put(CivSettings.localize.localizedString("State"), state.color() + state.name());
            CivMessage.send(sender, parent.makeInfoString(info, ChatColor.DARK_GREEN, ChatColor.GREEN));


            ConfigCultureLevel clc = CivSettings.cultureLevels.get(town.getCultureLevel());
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Culture") + " " + ChatColor.GREEN + CivSettings.localize.localizedString("Level") + " " + clc.level + " (" + town.getAccumulatedCulture() + "/" + clc.amount + ")" +
                    ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("Online") + " " + ChatColor.GREEN + town.getOnlineResidents().size());

        }

        if (!town.getBonusGoodies().isEmpty()) {
            StringBuilder goodies = new StringBuilder();
            for (BonusGoodie goodie : town.getBonusGoodies()) {
                goodies.append(goodie.getDisplayName()).append(",");
            }
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Goodies") + " " + ChatColor.GREEN + goodies);
        }

        if (resident == null || town.isInGroup("mayors", resident) || town.isInGroup("assistants", resident) || civ.getLeaderGroup().hasMember(resident) || civ.getAdviserGroup().hasMember(resident)) {
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Treasury") + " " + ChatColor.GREEN + town.getBalance() + ChatColor.DARK_GREEN + " " + CivSettings.CURRENCY_NAME + " " + CivSettings.localize.localizedString("cmd_town_info_structuresUpkeep") + " " + ChatColor.GREEN + town.getTotalUpkeep() * town.getGovernment().upkeep_rate);
            Structure bank = town.getStructureByType("s_bank");
            if (bank != null) {
                CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("cmd_town_info_showBankInterest") + " " + ChatColor.GREEN + df.format(((Bank) bank).getInterestRate() * 100) + "%" +
                        ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("cmd_town_info_showBankPrinciple") + " " + ChatColor.GREEN + town.getTreasury().getPrincipalAmount());
            } else {
                CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("cmd_town_info_showBankInterest") + " " + ChatColor.GREEN + CivSettings.localize.localizedString("cmd_town_info_showBankNoBank") + " " +
                        ChatColor.DARK_GREEN + CivSettings.localize.localizedString("cmd_town_info_showBankPrinciple") + " " + ChatColor.GREEN + CivSettings.localize.localizedString("cmd_town_info_showBankNoBank"));
            }
        }

        if (town.inDebt()) {
            CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Debt") + " " + ChatColor.YELLOW + town.getDebt() + " " + CivSettings.CURRENCY_NAME);
            CivMessage.send(sender, ChatColor.YELLOW + CivSettings.localize.localizedString("cmd_town_info_showInDebt"));
        }

        if (town.getMotherCiv() != null) {
            CivMessage.send(sender, ChatColor.YELLOW + CivSettings.localize.localizedString("var_cmd_town_info_showYearn", ChatColor.LIGHT_PURPLE + town.getMotherCiv().getName() + ChatColor.YELLOW));
        }

        if (town.hasDisabledStructures()) {
            CivMessage.send(sender, ChatColor.RED + CivSettings.localize.localizedString("cmd_town_info_showDisabled"));
        }

        if (isAdmin) {
            TownHall townhall = town.getTownHall();
            if (townhall == null) {
                CivMessage.send(sender, ChatColor.LIGHT_PURPLE + CivSettings.localize.localizedString("cmd_town_info_showNoTownHall"));
            } else {
                CivMessage.send(sender, ChatColor.LIGHT_PURPLE + CivSettings.localize.localizedString("Location") + " " + townhall.getCorner());
            }

            StringBuilder wars = new StringBuilder();
            for (Relation relation : town.getCiv().getDiplomacyManager().getRelations()) {
                if (relation.getStatus() == Status.WAR) {
                    wars.append(relation.getOtherCiv().getName()).append(", ");
                }
            }

            CivMessage.send(sender, ChatColor.LIGHT_PURPLE + CivSettings.localize.localizedString("cmd_town_info_showWars") + " " + wars);

        }

    }

    public void showDebugStructureInfo(Town town) {

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_info_showDebug"));
        for (Structure struct : town.getStructures()) {
            CivMessage.send(sender, struct.getDisplayName() + ": " + CivSettings.localize.localizedString("cmd_town_info_showdebugCorner") + " " + struct.getCorner() + " " + CivSettings.localize.localizedString("cmd_town_info_showdebugCenter") + " " + struct.getCenterLocation());
        }
    }

    public void structures_cmd() throws CivException {
        Town town = getSelectedTown();

        if (args.length > 1) {
            if (args[1].equalsIgnoreCase("debug")) {
                showDebugStructureInfo(town);
                return;
            }
        }

        HashMap<String, Double> structsByName = new HashMap<>();
        for (Structure struct : town.getStructures()) {
            Double upkeep = structsByName.get(struct.getConfigId());
            if (upkeep == null) {
                structsByName.put(struct.getDisplayName(), struct.getUpkeepCost());
            } else {
                upkeep += struct.getUpkeepCost();
                structsByName.put(struct.getDisplayName(), upkeep);
            }
        }

        CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_structuresInfo"));
        for (String structName : structsByName.keySet()) {
            Double upkeep = structsByName.get(structName);
            CivMessage.send(sender, ChatColor.DARK_GREEN + structName + " " + CivSettings.localize.localizedString("cmd_town_info_structuresUpkeep") + " " + ChatColor.GREEN + upkeep);

        }

        CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_stucturesWonders"));
        for (Wonder wonder : town.getWonders()) {
            CivMessage.send(sender, ChatColor.DARK_GREEN + wonder.getDisplayName() + " " + CivSettings.localize.localizedString("cmd_town_info_structuresUpkeep") + " " + ChatColor.GREEN + wonder.getUpkeepCost());
        }

    }

    public void trade_cmd() throws CivException {
        Town town = getSelectedTown();

        ArrayList<String> out = new ArrayList<>();
        CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_tradeHeading"));
        out.add(ChatColor.DARK_GREEN + CivSettings.localize.localizedString("cmd_town_info_tradeMultiplier") + " " + ChatColor.GREEN + df.format(town.getTradeRate()));
        boolean maxedCount = false;
        int goodMax = CivSettings.goodsConfig.getInt("trade_good_multiplier_max", 3);


        if (!town.getBonusGoodies().isEmpty()) {
            for (BonusGoodie goodie : town.getBonusGoodies()) {
                TradeGood good = goodie.getOutpost().getGood();

                int count = TradeGood.getTradeGoodCount(goodie, town) - 1;
                String countString = String.valueOf(count);
                if (count > goodMax) {
                    maxedCount = true;
                    count = goodMax;
                    countString = String.valueOf(ChatColor.LIGHT_PURPLE) + count + ChatColor.YELLOW;
                }

                CultureChunk cc = CivGlobal.getCultureChunk(goodie.getOutpost().getCorner().getLocation());
                if (cc == null) {
                    out.add(ChatColor.RED + goodie.getDisplayName() + " - " + CivSettings.localize.localizedString("cmd_town_info_tradeOutside"));
                } else {
                    out.add(ChatColor.GREEN + goodie.getDisplayName() + "(" + goodie.getOutpost().getCorner() + ")" + ChatColor.YELLOW + " " +
                            TradeGood.getBaseValue(good) + " * (1.0 + (0.5 * " + (countString) + ") = " + df.format(TradeGood.getTradeGoodValue(goodie, town)));
                }
            }
        } else {
            out.add(ChatColor.RED + CivSettings.localize.localizedString("cmd_town_info_tradeNone"));
        }

        out.add(ChatColor.AQUA + "=================================================");
        if (maxedCount) {
            out.add(ChatColor.LIGHT_PURPLE + CivSettings.localize.localizedString("cmd_town_info_tradecolorMax"));
        }
        out.add(ChatColor.GRAY + CivSettings.localize.localizedString("cmd_town_info_tradeBaseValue") + " * ( 100% + ( 50% * MIN(ExtraGoods," + goodMax + ") )) = " + CivSettings.localize.localizedString("cmd_town_info_tradeGoodValue"));
        out.add(ChatColor.DARK_GREEN + CivSettings.localize.localizedString("cmd_town_info_tradeTotal") + " " + ChatColor.YELLOW + df.format(TradeGood.getTownBaseGoodPaymentViaGoodie(town)) + " * " + df.format(town.getTradeRate()) + " = "
                + df.format(TradeGood.getTownTradePayment(town)));

        CivMessage.send(sender, out);
    }

    public void temple_cmd() throws CivException {
        Town town = getSelectedTown();
        ArrayList<String> out = new ArrayList<>();

        CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_templeHeading"));
        double total = 0;

        for (Structure struct : town.getStructures()) {
            if (!struct.getConfigId().equals("s_temple")) {
                continue;
            }

            Temple temple = (Temple) struct;

            String color;
            if (struct.isActive()) {
                color = String.valueOf(ChatColor.GREEN);
            } else {
                color = String.valueOf(ChatColor.RED);
            }

            double culture = temple.getCultureGenerated();

            if (!struct.isDestroyed()) {
                out.add(color + CivSettings.localize.localizedString("cmd_town_info_templeName") + " (" + struct.getCorner() + ")");
                out.add(ChatColor.DARK_GREEN + "    " + CivSettings.localize.localizedString("Level") + " " + ChatColor.YELLOW + temple.getLevel() +
                        ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("count") + " " + ChatColor.YELLOW + "(" + temple.getCount() + "/" + temple.getMaxCount() + ")");
                out.add(ChatColor.DARK_GREEN + "    " + CivSettings.localize.localizedString("baseCulture") + " " + ChatColor.YELLOW + culture +
                        ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("LastResult") + " " + ChatColor.YELLOW + temple.getLastResult().name());
            } else {
                out.add(color + CivSettings.localize.localizedString("cmd_town_info_templeName") + " " + "(" + struct.getCorner() + ")");
                out.add(ChatColor.RED + "    " + CivSettings.localize.localizedString("DESTROYED"));
            }

            total += culture;

        }
        out.add(ChatColor.DARK_GREEN + "----------------------------");
        out.add(ChatColor.DARK_GREEN + CivSettings.localize.localizedString("SubTotal") + " " + ChatColor.YELLOW + total);
        out.add(ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Temple") + " " + ChatColor.YELLOW + df.format(town.getTempleRate() * 100) + "%");
        total *= town.getTempleRate();
        out.add(ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Total") + " " + ChatColor.YELLOW + df.format(total) + " " + CivSettings.localize.localizedString("Culture"));

        CivMessage.send(sender, out);
    }


    public void mine_cmd() throws CivException {
        Town town = getSelectedTown();
        ArrayList<String> out = new ArrayList<>();

        CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_mineHeading"));
        double total = 0;

        for (Structure struct : town.getStructures()) {
            if (!struct.getConfigId().equals("ti_mine")) {
                continue;
            }

            Mine mine = (Mine) struct;

            String color;
            if (struct.isActive()) {
                color = String.valueOf(ChatColor.GREEN);
            } else {
                color = String.valueOf(ChatColor.RED);
            }

            out.add(color + CivSettings.localize.localizedString("cmd_town_info_mineName") + " (" + struct.getCorner() + ")");
            out.add(ChatColor.DARK_GREEN + "    " + CivSettings.localize.localizedString("Level") + " " + ChatColor.YELLOW + mine.getLevel() +
                    ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("count") + " " + ChatColor.YELLOW + "(" + mine.getCount() + "/" + mine.getMaxCount() + ")");
            out.add(ChatColor.DARK_GREEN + "    " + CivSettings.localize.localizedString("hammersPerTile") + " " + ChatColor.YELLOW + mine.getBonusHammers());
            out.add(ChatColor.DARK_GREEN + "    " + CivSettings.localize.localizedString("LastResult") + " " + ChatColor.YELLOW + mine.getLastResult().name());

            total += mine.getBonusHammers(); //XXX estimate based on tile radius of 1.

        }
        out.add(ChatColor.DARK_GREEN + "----------------------------");
        out.add(ChatColor.DARK_GREEN + CivSettings.localize.localizedString("SubTotal") + " " + ChatColor.YELLOW + total);
        out.add(ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Total") + " " + ChatColor.YELLOW + df.format(total) + " " + CivSettings.localize.localizedString("cmd_town_info_mineHammersInfo"));

        CivMessage.send(sender, out);
    }

    public void cottage_cmd() throws CivException {
        Town town = getSelectedTown();
        ArrayList<String> out = new ArrayList<>();

        CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_cottageHeading"));
        double total = 0;

        for (Structure struct : town.getStructures()) {
            if (!struct.getConfigId().equals("ti_cottage")) {
                continue;
            }

            Cottage cottage = (Cottage) struct;

            String color;
            if (struct.isActive()) {
                color = String.valueOf(ChatColor.GREEN);
            } else {
                color = String.valueOf(ChatColor.RED);
            }

            double coins = cottage.getCoinsGenerated();
            if (town.getCiv().hasTechnology("tech_taxation")) {
                coins *= CivSettings.techsConfig.getDouble("taxation_cottage_buff", 2.0);
            }

            if (!struct.isDestroyed()) {
                out.add(color + "Cottage (" + struct.getCorner() + ")");
                out.add(ChatColor.DARK_GREEN + "    " + CivSettings.localize.localizedString("Level") + " " + ChatColor.YELLOW + cottage.getLevel() +
                        ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("count") + " " + ChatColor.YELLOW + "(" + cottage.getCount() + "/" + cottage.getMaxCount() + ")");
                out.add(ChatColor.DARK_GREEN + "   " + CivSettings.localize.localizedString("base") + " " + CivSettings.CURRENCY_NAME + ": " + ChatColor.YELLOW + coins +
                        ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("LastResult") + " " + ChatColor.YELLOW + cottage.getLastResult().name());
            } else {
                out.add(color + "Cottage" + " (" + struct.getCorner() + ")");
                out.add(ChatColor.RED + "    " + CivSettings.localize.localizedString("DESTROYED"));
            }

            total += coins;

        }
        out.add(ChatColor.DARK_GREEN + "----------------------------");
        out.add(ChatColor.DARK_GREEN + CivSettings.localize.localizedString("SubTotal") + " " + ChatColor.YELLOW + total);
        out.add(ChatColor.DARK_GREEN + CivSettings.localize.localizedString("cmd_civ_gov_infoCottage") + " " + ChatColor.YELLOW + df.format(town.getCottageRate() * 100) + "%");
        total *= town.getCottageRate();
        out.add(ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Total") + " " + ChatColor.YELLOW + df.format(total) + " " + CivSettings.CURRENCY_NAME);

        CivMessage.send(sender, out);
    }

    public void upkeep_cmd() throws CivException {
        Town town = getSelectedTown();

        CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_upkeepHeading"));
        CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("baseUpkeep") + " " + ChatColor.GREEN + town.getBaseUpkeep());

        CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("cmd_town_info_spreadUpkeep") + " " + ChatColor.GREEN + town.getSpreadUpkeep());

        CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("StructureUpkeep") + " " + ChatColor.GREEN + town.getStructureUpkeep());

        CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Subtotal") + " " + ChatColor.GREEN + town.getTotalUpkeep() +
                ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("cmd_civ_gov_infoUpkeep") + " " + ChatColor.GREEN + town.getGovernment().upkeep_rate);
        CivMessage.send(sender, ChatColor.GRAY + "---------------------------------");
        CivMessage.send(sender, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("Total") + " " + ChatColor.GREEN + town.getTotalUpkeep() * town.getCiv().getGovernment().upkeep_rate);

    }


    private void show_info() throws CivException {
        Civilization civ = getSenderCiv();
        Town town = getSelectedTown();
        Resident resident = getResident();

        show(sender, resident, town, civ, this);

    }

    @Override
    public void doDefaultAction() throws CivException {
        show_info();
        CivMessage.send(sender, ChatColor.GRAY + CivSettings.localize.localizedString("cmd_town_info_showHelp"));
    }

    @Override
    public void showHelp() {
        showBasicHelp();
    }

    @Override
    public void permissionCheck() {
    }

}
