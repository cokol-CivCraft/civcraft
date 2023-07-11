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
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.*;
import com.avrgaming.civcraft.object.Relation.Status;
import com.avrgaming.civcraft.structure.*;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.util.CivColor;
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
        register_sub("disabled", this::disabled_cmd, CivSettings.localize.localizedString("cmd_town_info_disabledDesc"));
    }

    public void disabled_cmd() throws CivException {
        Town town = getSelectedTown();

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_info_disabledHeading"));
        LinkedList<String> out = new LinkedList<>();
        boolean showhelp = false;

        for (Buildable buildable : town.getDisabledBuildables()) {
            showhelp = true;
            out.add(CivColor.Green + buildable.getDisplayName() + CivColor.LightGreen + " " + CivSettings.localize.localizedString("Coord") + buildable.getCorner().toString());
        }

        if (showhelp) {
            out.add(CivColor.LightGray + CivSettings.localize.localizedString("cmd_town_info_disabledHelp1"));
            out.add(CivColor.LightGray + CivSettings.localize.localizedString("cmd_town_info_disabledHelp2"));
            out.add(CivColor.LightGray + CivSettings.localize.localizedString("cmd_town_info_disabledHelp3"));
            out.add(CivColor.LightGray + CivSettings.localize.localizedString("cmd_town_info_disabledHelp4"));
            out.add(CivColor.LightGray + CivSettings.localize.localizedString("cmd_town_info_disabledHelp5"));
        }

        CivMessage.send(sender, out);
    }

    public void area_cmd() throws CivException {
        Town town = getSelectedTown();

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_info_areaHeading"));
        HashMap<String, Integer> biomes = new HashMap<>();

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

            hammers += cc.getHammers();
            growth += cc.getGrowth();
            happiness += cc.getHappiness();
            beakers += cc.getBeakers();
        }

        CivMessage.send(sender, CivColor.LightBlue + CivSettings.localize.localizedString("cmd_town_biomeList"));
        StringBuilder out = new StringBuilder();
        //int totalBiomes = 0;
        for (String biome : biomes.keySet()) {
            Integer count = biomes.get(biome);
            out.append(CivColor.Green).append(biome).append(": ").append(CivColor.LightGreen).append(count).append(CivColor.Green).append(", ");
            //	totalBiomes += count;
        }
        CivMessage.send(sender, out.toString());

        //CivMessage.send(sender, CivColor.Green+"Biome Count:"+CivColor.LightGreen+totalBiomes);

        CivMessage.send(sender, CivColor.LightBlue + "Totals");
        CivMessage.send(sender, CivColor.Green + " " + CivSettings.localize.localizedString("cmd_town_happiness") + " " + CivColor.LightGreen + df.format(happiness) +
                CivColor.Green + " " + CivSettings.localize.localizedString("Hammers") + " " + CivColor.LightGreen + df.format(hammers) +
                CivColor.Green + " " + CivSettings.localize.localizedString("cmd_town_growth") + " " + CivColor.LightGreen + df.format(growth) +
                CivColor.Green + " " + CivSettings.localize.localizedString("Beakers") + " " + CivColor.LightGreen + df.format(beakers));

    }

    public void beakers_cmd() throws CivException {
        Town town = getSelectedTown();

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_info_beakersHeading"));

        AttrSource beakerSources = town.getBeakers();
        CivMessage.send(sender, beakerSources.getSourceDisplayString(CivColor.Green, CivColor.LightGreen));
//		CivMessage.send(sender, beakerSources.getRateDisplayString(CivColor.Green, CivColor.LightGreen));
        CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_civ_gov_infoBeaker") + " " + CivColor.LightGreen + (town.getBeakerRate().total * 100 + "%"));
        CivMessage.send(sender, beakerSources.getTotalDisplayString(CivColor.Green, CivColor.LightGreen));

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
            out.add(CivColor.Green + source + ": " + CivColor.LightGreen + df.format(value));
        }
        out.add(CivColor.LightPurple + CivSettings.localize.localizedString("Total") + " " + CivColor.LightGreen + df.format(happySources.total));


        out.add(CivMessage.buildSmallTitle(CivSettings.localize.localizedString("cmd_town_info_happinessUnhappy")));
        AttrSource unhappySources = town.getUnhappiness();
        for (String source : unhappySources.sources.keySet()) {
            Double value = unhappySources.sources.get(source);
            out.add(CivColor.Green + source + ": " + CivColor.LightGreen + value);
        }
        out.add(CivColor.LightPurple + CivSettings.localize.localizedString("Total") + " " + CivColor.LightGreen + df.format(unhappySources.total));

        out.add(CivMessage.buildSmallTitle(CivSettings.localize.localizedString("Total")));
        ConfigHappinessState state = town.getHappinessState();
        out.add(CivColor.LightGreen + df.format(town.getHappinessPercentage() * 100) + "%" + CivColor.Green + " " + CivSettings.localize.localizedString("cmd_town_info_happinessState") + " " + CivColor.valueOf(state.color) + state.name);
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
            out.add(CivColor.Green + CivSettings.localize.localizedString("var_BuffsFrom", (CivColor.LightGreen + buff.getDisplayName() + CivColor.Green), CivColor.LightGreen + buff.getSource()));
        }

        CivMessage.send(sender, out);
    }

    public void growth_cmd() throws CivException {
        Town town = getSelectedTown();
        AttrSource growthSources = town.getGrowth();

        CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_growthHeading"));
        CivMessage.send(sender, growthSources.getSourceDisplayString(CivColor.Green, CivColor.LightGreen));
        CivMessage.send(sender, growthSources.getRateDisplayString(CivColor.Green, CivColor.LightGreen));
        CivMessage.send(sender, growthSources.getTotalDisplayString(CivColor.Green, CivColor.LightGreen));
    }

    public void goodies_cmd() throws CivException {
        Town town = getSelectedTown();
        CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_goodiesHeading"));
        //	HashSet<BonusGoodie> effectiveGoodies = town.getEffectiveBonusGoodies();

        for (BonusGoodie goodie : town.getBonusGoodies()) {
            CivMessage.send(sender, CivColor.LightGreen + goodie.getDisplayName());
            String goodBonuses = goodie.getBonusDisplayString();

            String[] split = goodBonuses.split(";");
            for (String str : split) {
                CivMessage.send(sender, "    " + CivColor.LightPurple + str);
            }

        }
    }

    public void hammers_cmd() throws CivException {
        Town town = getSelectedTown();

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_info_hammersHeading"));
        AttrSource hammerSources = town.getHammers();

        CivMessage.send(sender, hammerSources.getSourceDisplayString(CivColor.Green, CivColor.LightGreen));
        CivMessage.send(sender, hammerSources.getRateDisplayString(CivColor.Green, CivColor.LightGreen));
        CivMessage.send(sender, hammerSources.getTotalDisplayString(CivColor.Green, CivColor.LightGreen));
    }

    public void culture_cmd() throws CivException {
        Town town = getSelectedTown();
        AttrSource cultureSources = town.getCulture();

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_info_cultureHeading"));

        CivMessage.send(sender, cultureSources.getSourceDisplayString(CivColor.Green, CivColor.LightGreen));
        CivMessage.send(sender, cultureSources.getRateDisplayString(CivColor.Green, CivColor.LightGreen));
        CivMessage.send(sender, cultureSources.getTotalDisplayString(CivColor.Green, CivColor.LightGreen));

    }


    public void rates_cmd() throws CivException {
        Town town = getSelectedTown();

        CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_ratesHeading"));

        DecimalFormat df = new DecimalFormat("#,###.#");

        CivMessage.send(sender,
                CivColor.Green + " " + CivSettings.localize.localizedString("cmd_civ_gov_infoGrowth") + " " + CivColor.LightGreen + df.format(town.getGrowthRate().total * 100) +
                        CivColor.Green + " " + CivSettings.localize.localizedString("cmd_civ_gov_infoCulture") + " " + CivColor.LightGreen + df.format(town.getCultureRate().total * 100) +
                        CivColor.Green + " " + CivSettings.localize.localizedString("cmd_civ_gov_infoCottage") + " " + CivColor.LightGreen + df.format(town.getCottageRate() * 100) +
                        CivColor.Green + " " + CivSettings.localize.localizedString("Temple") + " " + CivColor.LightGreen + df.format(town.getTempleRate() * 100) +
                        CivColor.Green + " " + CivSettings.localize.localizedString("cmd_civ_gov_infoTrade") + " " + CivColor.LightGreen + df.format(town.getTradeRate() * 100) +
                        CivColor.Green + " " + CivSettings.localize.localizedString("cmd_civ_gov_infoBeaker") + " " + CivColor.LightGreen + df.format(town.getBeakerRate().total * 100)
        );

    }

    public void trade_cmd() throws CivException {
        Town town = getSelectedTown();

        ArrayList<String> out = new ArrayList<>();
        CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_tradeHeading"));
        out.add(CivColor.Green + CivSettings.localize.localizedString("cmd_town_info_tradeMultiplier") + " " + CivColor.LightGreen + df.format(town.getTradeRate()));
        boolean maxedCount = false;
        int goodMax;
        try {
            goodMax = CivSettings.getInteger(CivSettings.goodsConfig, "trade_good_multiplier_max");
        } catch (InvalidConfiguration e) {
            e.printStackTrace();
            throw new CivException(CivSettings.localize.localizedString("internalException"));
        }


        if (town.getBonusGoodies().size() > 0) {
            for (BonusGoodie goodie : town.getBonusGoodies()) {
                TradeGood good = goodie.getOutpost().getGood();

                int count = TradeGood.getTradeGoodCount(goodie, town) - 1;
                String countString = String.valueOf(count);
                if (count > goodMax) {
                    maxedCount = true;
                    count = goodMax;
                    countString = CivColor.LightPurple + count + CivColor.Yellow;
                }

                CultureChunk cc = CivGlobal.getCultureChunk(goodie.getOutpost().getCorner().getLocation());
                if (cc == null) {
                    out.add(CivColor.Rose + goodie.getDisplayName() + " - " + CivSettings.localize.localizedString("cmd_town_info_tradeOutside"));
                } else {
                    out.add(CivColor.LightGreen + goodie.getDisplayName() + "(" + goodie.getOutpost().getCorner() + ")" + CivColor.Yellow + " " +
                            TradeGood.getBaseValue(good) + " * (1.0 + (0.5 * " + (countString) + ") = " + df.format(TradeGood.getTradeGoodValue(goodie, town)));
                }
            }
        } else {
            out.add(CivColor.Rose + CivSettings.localize.localizedString("cmd_town_info_tradeNone"));
        }

        out.add(CivColor.LightBlue + "=================================================");
        if (maxedCount) {
            out.add(CivColor.LightPurple + CivSettings.localize.localizedString("cmd_town_info_tradecolorMax"));
        }
        out.add(CivColor.LightGray + CivSettings.localize.localizedString("cmd_town_info_tradeBaseValue") + " * ( 100% + ( 50% * MIN(ExtraGoods," + goodMax + ") )) = " + CivSettings.localize.localizedString("cmd_town_info_tradeGoodValue"));
        out.add(CivColor.Green + CivSettings.localize.localizedString("cmd_town_info_tradeTotal") + " " + CivColor.Yellow + df.format(TradeGood.getTownBaseGoodPaymentViaGoodie(town)) + " * " + df.format(town.getTradeRate()) + " = "
                + df.format(TradeGood.getTownTradePayment(town)));

        CivMessage.send(sender, out);
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
            CivMessage.send(sender, CivColor.Green + structName + " " + CivSettings.localize.localizedString("cmd_town_info_structuresUpkeep") + " " + CivColor.LightGreen + upkeep);

        }

        CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_stucturesWonders"));
        for (Wonder wonder : town.getWonders()) {
            CivMessage.send(sender, CivColor.Green + wonder.getDisplayName() + " " + CivSettings.localize.localizedString("cmd_town_info_structuresUpkeep") + " " + CivColor.LightGreen + wonder.getUpkeepCost());
        }

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
                color = CivColor.LightGreen;
            } else {
                color = CivColor.Rose;
            }

            double coins = cottage.getCoinsGenerated();
            if (town.getCiv().hasTechnology("tech_taxation")) {
                double taxation_bonus;
                try {
                    taxation_bonus = CivSettings.getDouble(CivSettings.techsConfig, "taxation_cottage_buff");
                    coins *= taxation_bonus;
                } catch (InvalidConfiguration e) {
                    e.printStackTrace();
                }
            }

            if (!struct.isDestroyed()) {
                out.add(color + "Cottage (" + struct.getCorner() + ")");
                out.add(CivColor.Green + "    " + CivSettings.localize.localizedString("Level") + " " + CivColor.Yellow + cottage.getLevel() +
                        CivColor.Green + " " + CivSettings.localize.localizedString("count") + " " + CivColor.Yellow + "(" + cottage.getCount() + "/" + cottage.getMaxCount() + ")");
                out.add(CivColor.Green + "   " + CivSettings.localize.localizedString("base") + " " + CivSettings.CURRENCY_NAME + ": " + CivColor.Yellow + coins +
                        CivColor.Green + " " + CivSettings.localize.localizedString("LastResult") + " " + CivColor.Yellow + cottage.getLastResult().name());
            } else {
                out.add(color + "Cottage" + " (" + struct.getCorner() + ")");
                out.add(CivColor.Rose + "    " + CivSettings.localize.localizedString("DESTROYED"));
            }

            total += coins;

        }
        out.add(CivColor.Green + "----------------------------");
        out.add(CivColor.Green + CivSettings.localize.localizedString("SubTotal") + " " + CivColor.Yellow + total);
        out.add(CivColor.Green + CivSettings.localize.localizedString("cmd_civ_gov_infoCottage") + " " + CivColor.Yellow + df.format(town.getCottageRate() * 100) + "%");
        total *= town.getCottageRate();
        out.add(CivColor.Green + CivSettings.localize.localizedString("Total") + " " + CivColor.Yellow + df.format(total) + " " + CivSettings.CURRENCY_NAME);

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
                color = CivColor.LightGreen;
            } else {
                color = CivColor.Rose;
            }

            double culture = temple.getCultureGenerated();

            if (!struct.isDestroyed()) {
                out.add(color + CivSettings.localize.localizedString("cmd_town_info_templeName") + " (" + struct.getCorner() + ")");
                out.add(CivColor.Green + "    " + CivSettings.localize.localizedString("Level") + " " + CivColor.Yellow + temple.getLevel() +
                        CivColor.Green + " " + CivSettings.localize.localizedString("count") + " " + CivColor.Yellow + "(" + temple.getCount() + "/" + temple.getMaxCount() + ")");
                out.add(CivColor.Green + "    " + CivSettings.localize.localizedString("baseCulture") + " " + CivColor.Yellow + culture +
                        CivColor.Green + " " + CivSettings.localize.localizedString("LastResult") + " " + CivColor.Yellow + temple.getLastResult().name());
            } else {
                out.add(color + CivSettings.localize.localizedString("cmd_town_info_templeName") + " " + "(" + struct.getCorner() + ")");
                out.add(CivColor.Rose + "    " + CivSettings.localize.localizedString("DESTROYED"));
            }

            total += culture;

        }
        out.add(CivColor.Green + "----------------------------");
        out.add(CivColor.Green + CivSettings.localize.localizedString("SubTotal") + " " + CivColor.Yellow + total);
        out.add(CivColor.Green + CivSettings.localize.localizedString("Temple") + " " + CivColor.Yellow + df.format(town.getTempleRate() * 100) + "%");
        total *= town.getTempleRate();
        out.add(CivColor.Green + CivSettings.localize.localizedString("Total") + " " + CivColor.Yellow + df.format(total) + " " + CivSettings.localize.localizedString("Culture"));

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
                color = CivColor.LightGreen;
            } else {
                color = CivColor.Rose;
            }

            out.add(color + CivSettings.localize.localizedString("cmd_town_info_mineName") + " (" + struct.getCorner() + ")");
            out.add(CivColor.Green + "    " + CivSettings.localize.localizedString("Level") + " " + CivColor.Yellow + mine.getLevel() +
                    CivColor.Green + " " + CivSettings.localize.localizedString("count") + " " + CivColor.Yellow + "(" + mine.getCount() + "/" + mine.getMaxCount() + ")");
            out.add(CivColor.Green + "    " + CivSettings.localize.localizedString("hammersPerTile") + " " + CivColor.Yellow + mine.getBonusHammers());
            out.add(CivColor.Green + "    " + CivSettings.localize.localizedString("LastResult") + " " + CivColor.Yellow + mine.getLastResult().name());

            total += mine.getBonusHammers(); //XXX estimate based on tile radius of 1.

        }
        out.add(CivColor.Green + "----------------------------");
        out.add(CivColor.Green + CivSettings.localize.localizedString("SubTotal") + " " + CivColor.Yellow + total);
        out.add(CivColor.Green + CivSettings.localize.localizedString("Total") + " " + CivColor.Yellow + df.format(total) + " " + CivSettings.localize.localizedString("cmd_town_info_mineHammersInfo"));

        CivMessage.send(sender, out);
    }

    public void upkeep_cmd() throws CivException {
        Town town = getSelectedTown();

        CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_upkeepHeading"));
        CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("baseUpkeep") + " " + CivColor.LightGreen + town.getBaseUpkeep());

        try {
            CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_town_info_spreadUpkeep") + " " + CivColor.LightGreen + town.getSpreadUpkeep());
        } catch (InvalidConfiguration e) {
            e.printStackTrace();
            throw new CivException(CivSettings.localize.localizedString("internalException"));
        }

        CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("StructureUpkeep") + " " + CivColor.LightGreen + town.getStructureUpkeep());

        try {
            CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Subtotal") + " " + CivColor.LightGreen + town.getTotalUpkeep() +
                    CivColor.Green + " " + CivSettings.localize.localizedString("cmd_civ_gov_infoUpkeep") + " " + CivColor.LightGreen + town.getGovernment().upkeep_rate);
        } catch (InvalidConfiguration e) {
            e.printStackTrace();
            throw new CivException(CivSettings.localize.localizedString("internalException"));
        }
        CivMessage.send(sender, CivColor.LightGray + "---------------------------------");
        try {
            CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Total") + " " + CivColor.LightGreen + town.getTotalUpkeep() * town.getCiv().getGovernment().upkeep_rate);
        } catch (InvalidConfiguration e) {
            e.printStackTrace();
            throw new CivException(CivSettings.localize.localizedString("internalException"));
        }

    }

    public static void show(CommandSender sender, Resident resident, Town town, Civilization civ, CommandBase parent) throws CivException {

        DecimalFormat df = new DecimalFormat();

        boolean isAdmin = resident == null;

        CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_info_showHeading"));
        ConfigTownLevel level = CivSettings.townLevels.get(town.getLevel());

        CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Civilization") + " " + CivColor.LightGreen + town.getCiv().getName());
        CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("TownLevel") + " " + CivColor.LightGreen + town.getLevel() + " (" + town.getLevelTitle() + ") " +
                CivColor.Green + CivSettings.localize.localizedString("Score") + " " + CivColor.LightGreen + town.getScore());

        if (town.getMayorGroup() == null) {
            CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Mayors") + " " + CivColor.Rose + CivSettings.localize.localizedString("none"));
        } else {
            CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Mayors") + " " + CivColor.LightGreen + town.getMayorGroup().getMembersString());
        }

        if (town.getAssistantGroup() == null) {
            CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Assitants") + " " + CivColor.Rose + CivSettings.localize.localizedString("none"));
        } else {
            CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Assitants") + " " + CivColor.LightGreen + town.getAssistantGroup().getMembersString());
        }

        if (resident == null || civ.hasResident(resident) || isAdmin) {

            String color = CivColor.LightGreen;
            int maxTileImprovements = level.tile_improvements;
            if (town.getBuffManager().hasBuff("buff_mother_tree_tile_improvement_bonus")) {
                maxTileImprovements *= 2;
            }

            if (town.getTileImprovementCount() > maxTileImprovements) {
                color = CivColor.Rose;
            }

            CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Plots") + " " + CivColor.LightGreen + "(" + town.getTownChunks().size() + "/" + town.getMaxPlots() + ") " +
                    CivColor.Green + " " + CivSettings.localize.localizedString("TileImprovements") + " " + CivColor.LightGreen + "(" + color + town.getTileImprovementCount() + CivColor.LightGreen + "/" + maxTileImprovements + ")");


            //CivMessage.send(sender, CivColor.Green+"Outposts: "+CivColor.LightGreen+town.getOutpostChunks().size()+" "+
            CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Growth") + " " + CivColor.LightGreen + df.format(town.getGrowth().total) + " " +
                    CivColor.Green + CivSettings.localize.localizedString("Hammers") + " " + CivColor.LightGreen + df.format(town.getHammers().total) + " " +
                    CivColor.Green + CivSettings.localize.localizedString("Beakers") + " " + CivColor.LightGreen + df.format(town.getBeakers().total));


            CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Members") + " " + CivColor.LightGreen + town.getResidentCount() + " " +
                    CivColor.Green + CivSettings.localize.localizedString("TaxRate") + " " + CivColor.LightGreen + town.getTaxRateString() + " " +
                    CivColor.Green + CivSettings.localize.localizedString("FlatTax") + " " + CivColor.LightGreen + town.getFlatTax() + " " + CivSettings.CURRENCY_NAME);

            HashMap<String, String> info = new HashMap<>();
//			info.put("Happiness", CivColor.White+"("+CivColor.LightGreen+"H"+CivColor.Yellow+town.getHappinessTotal()
//					+CivColor.White+"/"+CivColor.Rose+"U"+CivColor.Yellow+town.getUnhappinessTotal()+CivColor.White+") = "+
//					CivColor.LightGreen+df.format(town.getHappinessPercentage()*100)+"%");
            info.put(CivSettings.localize.localizedString("Happiness"), CivColor.LightGreen + df.format(Math.floor(town.getHappinessPercentage() * 100)) + "%");
            ConfigHappinessState state = town.getHappinessState();
            info.put(CivSettings.localize.localizedString("State"), CivColor.valueOf(state.color) + state.name);
            CivMessage.send(sender, parent.makeInfoString(info, CivColor.Green, CivColor.LightGreen));


            ConfigCultureLevel clc = CivSettings.cultureLevels.get(town.getCultureLevel());
            CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Culture") + " " + CivColor.LightGreen + CivSettings.localize.localizedString("Level") + " " + clc.level + " (" + town.getAccumulatedCulture() + "/" + clc.amount + ")" +
                    CivColor.Green + " " + CivSettings.localize.localizedString("Online") + " " + CivColor.LightGreen + town.getOnlineResidents().size());

        }

        if (town.getBonusGoodies().size() > 0) {
            StringBuilder goodies = new StringBuilder();
            for (BonusGoodie goodie : town.getBonusGoodies()) {
                goodies.append(goodie.getDisplayName()).append(",");
            }
            CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Goodies") + " " + CivColor.LightGreen + goodies);
        }

        if (resident == null || town.isInGroup("mayors", resident) || town.isInGroup("assistants", resident) ||
                civ.getLeaderGroup().hasMember(resident) || civ.getAdviserGroup().hasMember(resident) || isAdmin) {
            try {
                CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Treasury") + " " + CivColor.LightGreen + town.getBalance() + CivColor.Green + " " + CivSettings.CURRENCY_NAME + " " + CivSettings.localize.localizedString("cmd_town_info_structuresUpkeep") + " " + CivColor.LightGreen + town.getTotalUpkeep() * town.getGovernment().upkeep_rate);
                Structure bank = town.getStructureByType("s_bank");
                if (bank != null) {
                    CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_town_info_showBankInterest") + " " + CivColor.LightGreen + df.format(((Bank) bank).getInterestRate() * 100) + "%" +
                            CivColor.Green + " " + CivSettings.localize.localizedString("cmd_town_info_showBankPrinciple") + " " + CivColor.LightGreen + town.getTreasury().getPrincipalAmount());
                } else {
                    CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_town_info_showBankInterest") + " " + CivColor.LightGreen + CivSettings.localize.localizedString("cmd_town_info_showBankNoBank") + " " +
                            CivColor.Green + CivSettings.localize.localizedString("cmd_town_info_showBankPrinciple") + " " + CivColor.LightGreen + CivSettings.localize.localizedString("cmd_town_info_showBankNoBank"));
                }
            } catch (InvalidConfiguration e) {
                e.printStackTrace();
                throw new CivException(CivSettings.localize.localizedString("internalException"));
            }
        }

        if (town.inDebt()) {
            CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Debt") + " " + CivColor.Yellow + town.getDebt() + " " + CivSettings.CURRENCY_NAME);
            CivMessage.send(sender, CivColor.Yellow + CivSettings.localize.localizedString("cmd_town_info_showInDebt"));
        }

        if (town.getMotherCiv() != null) {
            CivMessage.send(sender, CivColor.Yellow + CivSettings.localize.localizedString("var_cmd_town_info_showYearn", CivColor.LightPurple + town.getMotherCiv().getName() + CivColor.Yellow));
        }

        if (town.hasDisabledStructures()) {
            CivMessage.send(sender, CivColor.Rose + CivSettings.localize.localizedString("cmd_town_info_showDisabled"));
        }

        if (isAdmin) {
            TownHall townhall = town.getTownHall();
            if (townhall == null) {
                CivMessage.send(sender, CivColor.LightPurple + CivSettings.localize.localizedString("cmd_town_info_showNoTownHall"));
            } else {
                CivMessage.send(sender, CivColor.LightPurple + CivSettings.localize.localizedString("Location") + " " + townhall.getCorner());
            }

            StringBuilder wars = new StringBuilder();
            for (Relation relation : town.getCiv().getDiplomacyManager().getRelations()) {
                if (relation.getStatus() == Status.WAR) {
                    wars.append(relation.getOtherCiv().getName()).append(", ");
                }
            }

            CivMessage.send(sender, CivColor.LightPurple + CivSettings.localize.localizedString("cmd_town_info_showWars") + " " + wars);

        }

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
        CivMessage.send(sender, CivColor.LightGray + CivSettings.localize.localizedString("cmd_town_info_showHelp"));
    }

    @Override
    public void showHelp() {
        showBasicHelp();
    }

    @Override
    public void permissionCheck() {
    }

}
