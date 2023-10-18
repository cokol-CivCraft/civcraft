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
import com.avrgaming.civcraft.config.ConfigCultureBiomeInfo;
import com.avrgaming.civcraft.config.ConfigCultureLevel;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.questions.JoinTownResponse;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.TownHall;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.war.War;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class TownCommand extends CommandBase {

    public static final long INVITE_TIMEOUT = 30000; //30 seconds

    public void init() {
        command = "/town";
        displayName = CivSettings.localize.localizedString("cmd_town_name");

        register_sub("claim", this::claim_cmd, CivSettings.localize.localizedString("cmd_town_claimDesc"));
        register_sub("unclaim", this::unclaim_cmd, CivSettings.localize.localizedString("cmd_town_unclaimDesc"));
        register_sub("group", this::group_cmd, CivSettings.localize.localizedString("cmd_town_groupDesc"));
        register_sub("upgrade", this::upgrade_cmd, CivSettings.localize.localizedString("cmd_town_upgradeDesc"));
        register_sub("info", this::info_cmd, CivSettings.localize.localizedString("cmd_town_infoDesc"));
        register_sub("add", this::add_cmd, CivSettings.localize.localizedString("cmd_town_addDesc"));
        register_sub("members", this::members_cmd, CivSettings.localize.localizedString("cmd_town_membersDesc"));
        register_sub("deposit", this::deposit_cmd, CivSettings.localize.localizedString("cmd_town_depositDesc"));
        register_sub("withdraw", this::withdraw_cmd, CivSettings.localize.localizedString("cmd_town_withdrawDesc"));
        register_sub("set", this::set_cmd, CivSettings.localize.localizedString("cmd_town_setDesc"));
        register_sub("leave", this::leave_cmd, CivSettings.localize.localizedString("cmd_town_leaveDesc"));
        register_sub("show", this::show_cmd, CivSettings.localize.localizedString("cmd_town_showDesc"));
        register_sub("evict", this::evict_cmd, CivSettings.localize.localizedString("cmd_town_evictDesc"));
        register_sub("list", this::list_cmd, CivSettings.localize.localizedString("cmd_town_listDesc"));
        register_sub("reset", this::reset_cmd, CivSettings.localize.localizedString("cmd_town_resetDesc"));
        register_sub("top5", this::top5_cmd, CivSettings.localize.localizedString("cmd_town_top5Desc"));
        register_sub("disbandtown", this::disbandtown_cmd, CivSettings.localize.localizedString("cmd_town_disbandtownDesc"));
        register_sub("outlaw", this::outlaw_cmd, CivSettings.localize.localizedString("cmd_town_outlawDesc"));
        register_sub("leavegroup", this::leavegroup_cmd, CivSettings.localize.localizedString("cmd_town_leavegroupDesc"));
        register_sub("select", this::select_cmd, CivSettings.localize.localizedString("cmd_town_selectDesc"));
        register_sub("capitulate", this::capitulate_cmd, CivSettings.localize.localizedString("cmd_town_capitulateDesc"));
        register_sub("survey", this::survey_cmd, CivSettings.localize.localizedString("cmd_town_surveyDesc"));
        register_sub("event", this::event_cmd, CivSettings.localize.localizedString("cmd_town_eventDesc"));
        register_sub("claimmayor", this::claimmayor_cmd, CivSettings.localize.localizedString("cmd_town_claimmayorDesc"));
        register_sub("enablestructure", this::enablestructure_cmd, CivSettings.localize.localizedString("cmd_town_enableStructureDesc"));
        register_sub("location", this::location_cmd, CivSettings.localize.localizedString("cmd_town_locationDesc"));
        register_sub("e", this::event_cmd, null); // event
        register_sub("s", this::select_cmd, null); // select
        register_sub("l", this::list_cmd, null); // list
        register_sub("w", this::withdraw_cmd, null); // withdraw
        register_sub("d", this::deposit_cmd, null); // deposit
        register_sub("up", this::upgrade_cmd, null); // upgrade -_-
        register_sub("i", this::info_cmd, null); // info
        register_sub("loc", this::location_cmd, null);
        register_sub("m", this::members_cmd, null); // members
        register_sub("u", this::upgrade_cmd, null); // upgrade
        register_sub("invite", this::invite_cmd, null); // add
        register_sub("kick", this::kick_cmd, null); // evict
    }

    public void location_cmd() throws CivException {
        Town town = getSelectedTown();
        Resident resident = getResident();
        if (resident.getTown() == town) {
            TownHall townhall = town.getTownHall();
            if (townhall == null) {
                CivMessage.send(sender, String.valueOf(ChatColor.GREEN) + ChatColor.BOLD + town.getName() + " - ");
                CivMessage.send(sender, String.valueOf(ChatColor.RED) + ChatColor.BOLD + CivSettings.localize.localizedString("cmd_civ_locationMissingTownHall"));
            } else {
                CivMessage.send(sender, String.valueOf(ChatColor.GREEN) + ChatColor.BOLD + town.getName() + " - ");
                CivMessage.send(sender, ChatColor.GREEN + CivSettings.localize.localizedString("Location") + " " + ChatColor.LIGHT_PURPLE + townhall.getCorner());
            }
        }
    }

    public void enablestructure_cmd() throws CivException {
        Town town = getSelectedTown();
        Resident resident = getResident();
        String coordString = getNamedString(1, CivSettings.localize.localizedString("cmd_town_enableStructurePrompt"));
        Structure struct;
        try {
            struct = CivGlobal.getStructure(new BlockCoord(coordString));
        } catch (Exception e) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_enableStructureInvalid"));
        }

        if (War.isWarTime()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_enableStructureWar"));
        }

        if (struct == null) {
            throw new CivException(CivSettings.localize.localizedString("var_cmd_town_enableStructureNotFound", coordString));
        }

        if (!resident.getCiv().getLeaderGroup().hasMember(resident)) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_enableStructureNotLead"));
        }

        if (!town.isStructureAddable(struct)) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_enableStructureOverLimit"));
        }

        /* Readding structure will make it valid. */
        town.removeStructure(struct);
        town.addStructure(struct);
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_town_enableStructureSuccess"));
    }

    public void claimmayor_cmd() throws CivException {
        Town town = getSelectedTown();
        Resident resident = getResident();

        if (resident.getTown() != town) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_claimMayorNotInTown"));
        }

        if (!town.areMayorsInactive()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_claimMayorNotInactive"));
        }

        town.getMayorGroup().addMember(resident);

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_claimmayorSuccess", town.getName()));
        CivMessage.sendTown(town, CivSettings.localize.localizedString("var_cmd_town_claimmayorSuccess2", resident.getName()));
    }

    // @EventHandler ???
    public void event_cmd() {
        TownEventCommand cmd = new TownEventCommand();
        cmd.onCommand(sender, null, "event", this.stripArgs(args, 1));
    }

    public static ArrayList<String> survey(Location loc, int culturelevel) {
        ChunkCoord start = new ChunkCoord(loc);
        if (culturelevel == 0 || culturelevel == 1) {
            culturelevel = 1;
        }
        ConfigCultureLevel lvl = CivSettings.cultureLevels.get(culturelevel);

        ArrayList<String> outList = new ArrayList<>();

        Queue<ChunkCoord> closedSet = new LinkedList<>();
        Queue<ChunkCoord> openSet = new LinkedList<>();
        openSet.add(start);
        /* Try to get the surrounding chunks and get their biome info. */
        //Enqueue all neighbors.
        while (!openSet.isEmpty()) {
            ChunkCoord node = openSet.poll();

            if (closedSet.contains(node)) {
                continue;
            }

            if (node.manhattanDistance(start) > lvl.chunks) {
                continue;
                //	break;
            }

            closedSet.add(node);

            //Enqueue all neighbors.
            int[][] offset = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int i = 0; i < 4; i++) {
                ChunkCoord nextCoord = new ChunkCoord(node.getWorldname(),
                        node.getX() + offset[i][0],
                        node.getZ() + offset[i][1]);

                if (closedSet.contains(nextCoord)) {
                    continue;
                }

                openSet.add(nextCoord);
            }
        }

        HashMap<String, Integer> biomes = new HashMap<>();

        //	double coins = 0.0;
        double hammers = 0.0;
        double growth = 0.0;
        double happiness = 0.0;
        double beakers = 0.0;
        DecimalFormat df = new DecimalFormat();

        for (ChunkCoord c : closedSet) {
            /* Increment biome counts. */
            Biome biome = c.getChunk().getWorld().getBiome(c.getX() * 16, c.getZ() * 16);

            if (!biomes.containsKey(biome.name())) {
                biomes.put(biome.name(), 1);
            } else {
                Integer value = biomes.get(biome.name());
                biomes.put(biome.name(), value + 1);
            }

            ConfigCultureBiomeInfo info = CivSettings.getCultureBiome(biome.name());

            //	coins += info.coins;
            hammers += info.hammers;
            growth += info.growth;
            happiness += info.happiness;
            beakers += info.beakers;
        }

        outList.add(ChatColor.AQUA + CivSettings.localize.localizedString("cmd_town_biomeList"));
        //int totalBiomes = 0;
        StringBuilder out = new StringBuilder();
        for (String biome : biomes.keySet()) {
            Integer count = biomes.get(biome);
            out.append(ChatColor.DARK_GREEN).append(biome).append(": ").append(ChatColor.GREEN).append(count).append(ChatColor.DARK_GREEN).append(", ");
            //totalBiomes += count;
        }
        outList.add(out.toString());
        //	outList.add(CivColor.Green+"Biome Count: "+CivColor.LightGreen+totalBiomes);

        outList.add(ChatColor.AQUA + CivSettings.localize.localizedString("cmd_town_totals"));
        outList.add(ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("cmd_town_happiness") + " " + ChatColor.GREEN + df.format(happiness) +
                ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("Hammers") + " " + ChatColor.GREEN + df.format(hammers) +
                ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("cmd_town_growth") + " " + ChatColor.GREEN + df.format(growth) +
                ChatColor.DARK_GREEN + " " + CivSettings.localize.localizedString("Beakers") + " " + ChatColor.GREEN + df.format(beakers));
        return outList;
    }

    public void survey_cmd() throws CivException {
        Player player = getPlayer();
        CivMessage.send(player, survey(player.getLocation(), Integer.parseInt(args[1])));
    }

    public void capitulate_cmd() throws CivException {
        this.validMayor();
        Town town = getSelectedTown();

        if (town.getMotherCiv() == null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_capitulateErrorNoMother"));
        }

        if (town.getMotherCiv().getCapitolName().equals(town.getName())) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_capitulateCapitol"));
        }

        if (args.length < 2 || !args[1].equalsIgnoreCase("yes")) {
            CivMessage.send(sender, String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_town_capitulatePrompt1", town.getCiv().getName()));
            CivMessage.send(sender, String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.localize.localizedString("cmd_town_capitulateConfirm"));
            return;
        }

        /* Town is capitulating, no longer need a mother civ. */
        town.setMotherCiv(null);

        CivMessage.global(CivSettings.localize.localizedString("var_cmd_town_capitulateSuccess1", town.getName(), town.getCiv().getName()));
    }
    public void select_cmd() throws CivException {
        Resident resident = getResident();
        Town selectTown = getNamedTown(1);

        if (resident.getSelectedTown() == null) {
            if (resident.getTown() == selectTown) {
                throw new CivException(CivSettings.localize.localizedString("var_cmd_town_selectedAlready", selectTown.getName()));
            }
        }

        if (resident.getSelectedTown() == selectTown) {
            throw new CivException(CivSettings.localize.localizedString("var_cmd_town_selectedAlready", selectTown.getName()));
        }

        selectTown.validateResidentSelect(resident);

        resident.setSelectedTown(selectTown);
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_selecteSuccess", selectTown.getName()));
    }

    public void leavegroup_cmd() throws CivException {
        Town town = getNamedTown(1);
        PermissionGroup grp = getNamedPermissionGroup(town, 2);
        Resident resident = getResident();

        if (!grp.hasMember(resident)) {
            throw new CivException(CivSettings.localize.localizedString("var_cmd_town_leavegroupNotIn1", grp.getName(), town.getName()));
        }

        if (grp == town.getMayorGroup() && grp.getMemberCount() == 1) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_leavegroupLastMayor"));
        }

        if (grp == town.getCiv().getLeaderGroup() && grp.getMemberCount() == 1) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_leavegroupLastLead"));
        }

        grp.removeMember(resident);

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_leavegroupSuccess", grp.getName(), town.getName()));
    }

    public void outlaw_cmd() {
        TownOutlawCommand cmd = new TownOutlawCommand();
        cmd.onCommand(sender, null, "outlaw", this.stripArgs(args, 1));
    }

    public void disbandtown_cmd() throws CivException {
        this.validMayor();
        Town town = this.getSelectedTown();

        if (town.getMotherCiv() != null) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_disbandtownConquered"));
        }

        if (town.isCapitol()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_disbandtownCapitol"));
        }

        if (town.mayorWantsToDisband) {
            town.mayorWantsToDisband = false;
            CivMessage.send(sender, CivSettings.localize.localizedString("cmd_civ_disbandtownErrorLeader"));
            return;
        }

        town.mayorWantsToDisband = true;


        if (town.leaderWantsToDisband) {
            CivMessage.sendCiv(town.getCiv(), CivSettings.localize.localizedString("Town") + " " + town.getName() + " " + CivSettings.localize.localizedString("cmd_civ_disbandtownSuccess"));
            town.disband();
        }

        CivMessage.send(sender, CivSettings.localize.localizedString("cmd_town_disbandtownSuccess"));
    }

    public void top5_cmd() {
        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_top5Heading"));
//		TreeMap<Integer, Town> scores = new TreeMap<Integer, Town>();
//		
//		for (Town town : CivGlobal.getTowns()) {
//			if (town.getCiv().isAdminCiv()) {
//				continue;
//			}
//			scores.put(town.getScore(), town);
//		}

        synchronized (CivGlobal.townScores) {
            int i = 1;
            for (Integer score : CivGlobal.townScores.descendingKeySet()) {
                CivMessage.send(sender, i + ") " + ChatColor.GOLD + CivGlobal.townScores.get(score).getName() + ChatColor.WHITE + " - " + score);
                i++;
                if (i > 5) {
                    break;
                }
            }
        }

    }

    public void list_cmd() {
        StringBuilder out = new StringBuilder();

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_listHeading"));
        for (Town town : Town.getTowns()) {
            out.append(town.getName()).append("(").append(town.getCiv().getName()).append(")").append(", ");
        }

        CivMessage.send(sender, out.toString());
    }

    public void evict_cmd() throws CivException {
        Town town = getSelectedTown();
        Resident resident = getResident();

        if (args.length < 2) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_evictPrompt"));
        }

        Resident residentToKick = getNamedResident(1);

        if (residentToKick.getTown() != town) {
            throw new CivException(CivSettings.localize.localizedString("var_cmd_town_evictNotInTown", args[1]));
        }

        if (!town.isInGroup("mayors", resident) && !town.isInGroup("assistants", resident)) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_evictNoPerms"));
        }

        if (town.isInGroup("mayors", residentToKick) || town.isInGroup("assistants", residentToKick)) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_evictDemoteFirst"));
        }

        if (!residentToKick.isLandOwner()) {
            town.removeResident(residentToKick);

            try {
                CivMessage.send(CivGlobal.getPlayer(residentToKick), ChatColor.YELLOW + CivSettings.localize.localizedString("cmd_town_evictAlert"));
            } catch (CivException e) {
                //Player not online.
            }
            CivMessage.sendTown(town, CivSettings.localize.localizedString("var_cmd_town_evictSuccess1", residentToKick.getName(), resident.getName()));
            return;
        }

		residentToKick.setDaysTilEvict(CivSettings.GRACE_DAYS);
		residentToKick.warnEvict();
		residentToKick.save();
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_evictAlert2",args[1],CivSettings.GRACE_DAYS));
	}
	public void kick_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException(CivSettings.localize.localizedString("cmd_town_evictPrompt"));
		} else {
			evict_cmd();
		}
	}

	public void show_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException(CivSettings.localize.localizedString("cmd_town_showPrompt"));
		}

		Town town = getNamedTown(1);
		if (sender instanceof Player) {
			TownInfoCommand.show(sender, getResident(), town, town.getCiv(), this);
		} else {
			TownInfoCommand.show(sender, null, town, town.getCiv(), this);
		}

        try {
            Civilization civ = getSenderCiv();
            if (town.getCiv() != civ) {
                if (sender instanceof Player player) {
                    Location ourCapLoc = civ.getCapitolTownHallLocation();

                    if (ourCapLoc == null) {
                        return;
                    }

                    double potentialDistanceLow;
                    double potentialDistanceHigh;
                    if (town.getTownHall() != null) {
                        Location theirTownHallLoc = town.getTownHall().getCenterLocation().getLocation();
                        potentialDistanceLow = civ.getDistanceUpkeepAtLocation(ourCapLoc, theirTownHallLoc, true);
                        potentialDistanceHigh = civ.getDistanceUpkeepAtLocation(ourCapLoc, theirTownHallLoc, false);

                        CivMessage.send(player, ChatColor.YELLOW + CivSettings.localize.localizedString("var_cmd_town_showCost1", potentialDistanceLow));
                        CivMessage.send(player, ChatColor.YELLOW + CivSettings.localize.localizedString("cmd_town_showCost3"));
                        CivMessage.send(player, ChatColor.YELLOW + CivSettings.localize.localizedString("var_cmd_town_showCost3", potentialDistanceHigh, CivSettings.CURRENCY_NAME));
                    } else {
                        CivMessage.send(player, ChatColor.YELLOW + CivSettings.localize.localizedString("cmd_town_showNoTownHall"));
                    }
                }
            }
        } catch (CivException e) {
            // Playe not part of a civ, thats ok dont show anything.
        }

    }

    public void leave_cmd() throws CivException {
        Town town = getSelectedTown();
        Resident resident = getResident();

        if (town != resident.getTown()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_leaveNotSelected"));
        }

        if (town.getMayorGroup().getMemberCount() == 1 &&
                town.getMayorGroup().hasMember(resident)) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_leaveOnlyMayor"));
        }

        town.removeResident(resident);
        if (resident.isCivChat()) {
            resident.setCivChat(false);
        }

        if (resident.isTownChat()) {
            resident.setTownChat(false);
            CivMessage.send(sender, ChatColor.GRAY + CivSettings.localize.localizedString("cmd_town_leaveTownChat"));
        }

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_leaveSuccess", town.getName()));
        CivMessage.sendTown(town, CivSettings.localize.localizedString("var_cmd_town_leaveBroadcast", resident.getName()));

        resident.save();
    }

    public void set_cmd() {
        TownSetCommand cmd = new TownSetCommand();
        cmd.onCommand(sender, null, "set", this.stripArgs(args, 1));
    }

    public void reset_cmd() {
        TownResetCommand cmd = new TownResetCommand();
        cmd.onCommand(sender, null, "reset", this.stripArgs(args, 1));
    }

    public void upgrade_cmd() {
        TownUpgradeCommand cmd = new TownUpgradeCommand();
        cmd.onCommand(sender, null, "upgrade", this.stripArgs(args, 1));
    }

    public void withdraw_cmd() throws CivException {
        if (args.length < 2) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_withdrawPrompt"));
        }

        Town town = getSelectedTown();
        Player player = getPlayer();
        Resident resident = getResident();

        if (!town.playerIsInGroupName("mayors", player)) {
            throw new CivException(CivSettings.localize.localizedString("cmd_MustBeMayor"));
        }

        try {
            double amount = Double.parseDouble(args[1]);
            if (amount < 1) {
                throw new CivException(amount + " " + CivSettings.localize.localizedString("cmd_enterNumerError2"));
            }

            if (!town.getTreasury().payTo(resident.getTreasury(), Math.floor(amount))) {
                throw new CivException(CivSettings.localize.localizedString("cmd_town_withdrawNotEnough"));
            }
        } catch (NumberFormatException e) {
            throw new CivException(args[1] + " " + CivSettings.localize.localizedString("cmd_enterNumerError2"));
        }
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_withdrawSuccess", args[1], CivSettings.CURRENCY_NAME));
    }

    public void deposit_cmd() throws CivException {
        if (args.length < 2) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_despositPrompt"));
        }

        Resident resident = getResident();
        Town town = getSelectedTown();
        Double amount = getNamedDouble(1);

        try {
            if (amount < 1) {
                throw new CivException(amount + " " + CivSettings.localize.localizedString("cmd_enterNumerError2"));
            }
            amount = Math.floor(amount);
            town.depositFromResident(amount, resident);

        } catch (NumberFormatException e) {
            throw new CivException(args[1] + " " + CivSettings.localize.localizedString("cmd_enterNumerError2"));
        }

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_despositSuccess", args[1], CivSettings.CURRENCY_NAME));
    }

    public void add_cmd() throws CivException {
        this.validMayorAssistantLeader();

        Resident newResident = getNamedResident(1);
        Player player = getPlayer();
        Town town = getSelectedTown();

        if (War.isWarTime()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_addWar"));
        }

        if (War.isWithinWarDeclareDays() && town.getCiv().getDiplomacyManager().isAtWar()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_addCloseToWar") + " " + War.getTimeDeclareDays() + " " + CivSettings.localize.localizedString("cmd_civ_dip_declareTooCloseToWar4"));
        }

        if (newResident.hasCamp()) {
            try {
                Player resPlayer = CivGlobal.getPlayer(newResident);
                CivMessage.send(resPlayer, ChatColor.YELLOW + CivSettings.localize.localizedString("var_cmd_town_addAlertError1", player.getName(), town.getName()));
            } catch (CivException e) {
                //player not online
            }
            throw new CivException(CivSettings.localize.localizedString("var_cmd_town_addhasCamp", newResident.getName()));
        }

        if (town.hasResident(newResident)) {
            throw new CivException(CivSettings.localize.localizedString("var_cmd_town_addInTown", newResident.getName()));
        }

        if (newResident.getTown() != null) {
            throw new CivException(CivSettings.localize.localizedString("var_cmd_town_addhasTown", newResident.getName(), newResident.getTown().getName()));
        }

        JoinTownResponse join = new JoinTownResponse();
        join.town = town;
        join.resident = newResident;
        join.sender = player;

        newResident.validateJoinTown(town);

        CivGlobal.questionPlayer(player, CivGlobal.getPlayer(newResident),
                CivSettings.localize.localizedString("var_cmd_town_addInvite", town.getName()),
                INVITE_TIMEOUT, join);

        CivMessage.sendSuccess(sender, ChatColor.GRAY + CivSettings.localize.localizedString("var_cmd_town_addSuccess", args[1], town.getName()));
    }

    public void invite_cmd() throws CivException {
        if (args.length <= 1) {
            throw new CivException(CivSettings.localize.localizedString("EnterResidentName"));
        } else {
            add_cmd();
        }
    }

    public void info_cmd() {
        TownInfoCommand cmd = new TownInfoCommand();
        cmd.onCommand(sender, null, "info", this.stripArgs(args, 1));
    }

    //	public void new_cmd() throws CivException {
//		if (!(sender instanceof Player)) {
//			return;
//		}
//		
//		Resident resident = CivGlobal.getResident((Player)sender);
//		
//		if (resident == null || !resident.hasTown()) {
//			throw new CivException("You are not part of a civilization.");
//		}
//		
//		ConfigUnit unit = Unit.getPlayerUnit((Player)sender);
//		if (unit == null || !unit.id.equals("u_settler")) {			
//			throw new CivException("You must be a settler in order to found a town.");
//		}
//		
//		CivMessage.sendHeading(sender, "Founding A New Town");
//		CivMessage.send(sender, CivColor.LightGreen+"This looks like a good place to settle!");
//		CivMessage.send(sender, " ");
//		CivMessage.send(sender, CivColor.LightGreen+ChatColor.BOLD+"What shall your new Town be called?");
//		CivMessage.send(sender, CivColor.LightGray+"(To cancel, type 'cancel')");
//		
//		resident.setInteractiveMode(new InteractiveTownName());
//
//	}

    public void claim_cmd() throws CivException {

        Player player = getPlayer();
        Town town = this.getSelectedTown();

        if (!town.playerIsInGroupName("mayors", player) && !town.playerIsInGroupName("assistants", player)) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_claimNoPerm"));
        }

//		boolean outpost = false;
//		if (args.length >= 2 && args[1].equalsIgnoreCase("outpost")) {
//			outpost = true;
//			CivMessage.send(player, "Claiming an outpost!");
//		}

        TownChunk.claim(town, player);
    }

    public void unclaim_cmd() throws CivException {
        Town town = getSelectedTown();
        Player player = getPlayer();
        Resident resident = getResident();
        TownChunk tc = this.getStandingTownChunk();

        if (!town.playerIsInGroupName("mayors", player) && !town.playerIsInGroupName("assistants", player)) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_claimNoPerm"));
        }

        if (town.getTownChunks().size() <= 1) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_unclaimError"));
        }

        if (!tc.getCanUnclaim()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_unclaim_errorTownHall"));
        }

        if (tc.getTown() != resident.getTown()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_unclaimNotInTown"));
        }

        if (tc.perms.getOwner() != null && tc.perms.getOwner() != resident) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_unclaimOtherRes"));
        }

        TownChunk.unclaim(tc);
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_unclaimSuccess", tc.getCenterString()));

    }

    public void group_cmd() {
        TownGroupCommand cmd = new TownGroupCommand();
        cmd.onCommand(sender, null, "group", this.stripArgs(args, 1));
    }


    public void members_cmd() throws CivException {
        Town town = this.getSelectedTown();

        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("var_town_membersHeading", town.getName()));
        StringBuilder out = new StringBuilder();
        for (Resident res : town.getResidents()) {
            out.append(res.getName()).append(", ");
        }
        CivMessage.send(sender, out.toString());
    }

    public void showHelp() {
        showBasicHelp();
    }

    @Override
    public void permissionCheck() {
    }

    @Override
    public void doDefaultAction() {
        //TODO make this an info command.
        showHelp();
    }

}
