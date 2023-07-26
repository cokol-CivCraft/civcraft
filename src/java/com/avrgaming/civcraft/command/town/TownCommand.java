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
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.config.ConfigCultureBiomeInfo;
import com.avrgaming.civcraft.config.ConfigCultureLevel;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
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
import com.avrgaming.civcraft.tutorial.CivTutorial;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.global.perks.Perk;
import com.avrgaming.global.perks.components.CustomTemplate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

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
//		register_sub();("capture", this::capture_cmd, "[town] - instantly captures this town if they have a missing or illegally placed town hall during WarTime.");
        register_sub("capitulate", this::capitulate_cmd, CivSettings.localize.localizedString("cmd_town_capitulateDesc"));
        register_sub("survey", this::survey_cmd, CivSettings.localize.localizedString("cmd_town_surveyDesc"));
        register_sub("templates", this::templates_cmd, CivSettings.localize.localizedString("cmd_town_templatesDesc"));
        register_sub("event", this::event_cmd, CivSettings.localize.localizedString("cmd_town_eventDesc"));
        register_sub("claimmayor", this::claimmayor_cmd, CivSettings.localize.localizedString("cmd_town_claimmayorDesc"));
//		register_sub();("movestructure, this::movestructure", "[coord] [town] moves the structure specified by the coord to the specfied town.");
        register_sub("enablestructure", this::enablestructure_cmd, CivSettings.localize.localizedString("cmd_town_enableStructureDesc"));
        register_sub("location", this::location_cmd, CivSettings.localize.localizedString("cmd_town_locationDesc"));
        register_sub("e", this::event_cmd, null); // event
        register_sub("s", this::s_cmd, null); // select
        register_sub("l", this::list_cmd, null); // list
        register_sub("w", this::w_cmd, null); // withdraw
        register_sub("d", this::d_cmd, null); // deposit
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
                CivMessage.send(sender, CivColor.LightGreen + CivColor.BOLD + town.getName() + " - ");
                CivMessage.send(sender, CivColor.Rose + CivColor.BOLD + CivSettings.localize.localizedString("cmd_civ_locationMissingTownHall"));
            } else {
                CivMessage.send(sender, CivColor.LightGreen + CivColor.BOLD + town.getName() + " - ");
                CivMessage.send(sender, CivColor.LightGreen + CivSettings.localize.localizedString("Location") + " " + CivColor.LightPurple + townhall.getCorner());
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

//	public void movestructure_cmd() throws CivException {
//		Town town = getSelectedTown();
//		Resident resident = getResident();
//		String coordString = getNamedString(1, "Coordinate of structure. Example: world,555,65,444");
//		Town targetTown = getNamedTown(2);
//		Structure struct;
//		
//		try {
//			struct = CivGlobal.getStructure(new BlockCoord(coordString));
//		} catch (Exception e) {
//			throw new CivException("Invalid structure coordinate. Example: world,555,65,444");
//		}
//
//		if (struct instanceof TownHall || struct instanceof Capitol) {
//			throw new CivException("Cannot move town halls or capitols.");
//		}
//		
//		if (War.isWarTime()) {
//			throw new CivException("Cannot move structures during war time.");
//		}
//		
//		if (struct == null) {
//			throw new CivException("Structure at:"+coordString+" is not found.");
//		}
//		
//		if (!resident.getCiv().getLeaderGroup().hasMember(resident)) {
//			throw new CivException("You must be the civ's leader in order to do this.");
//		}
//		
//		if (town.getCiv() != targetTown.getCiv()) {
//			throw new CivException("You can only move structures between towns in your own civ.");
//		}
//		
//		town.removeStructure(struct);
//		targetTown.addStructure(struct);
//		struct.setTown(targetTown);
//		struct.save();
//		
//		CivMessage.sendSuccess(sender, "Moved structure "+coordString+" to town "+targetTown.getName());
//	}

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
        town.getMayorGroup().save();
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_claimmayorSuccess", town.getName()));
        CivMessage.sendTown(town, CivSettings.localize.localizedString("var_cmd_town_claimmayorSuccess2", resident.getName()));
    }

    @EventHandler
    public void event_cmd() {
        TownEventCommand cmd = new TownEventCommand();
        cmd.onCommand(sender, null, "event", this.stripArgs(args, 1));
    }

    public void e_cmd() {
        event_cmd();
    }

    public void templates_cmd() throws CivException {
        Player player = getPlayer();
        Town town = getSelectedTown();
        Inventory inv = Bukkit.getServer().createInventory(player, CivTutorial.MAX_CHEST_SIZE * 9, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_templatesHeading"));

        for (ConfigBuildableInfo info : CivSettings.structures.values()) {
            for (Perk p : CustomTemplate.getTemplatePerksForBuildable(town, info.template_base_name)) {

                ItemStack stack = LoreGuiItem.build(p.configPerk.display_name,
                        p.configPerk.type_id,
                        p.configPerk.data,
                        CivColor.Gray + CivSettings.localize.localizedString("cmd_town_templateProvider") + " " + CivColor.LightBlue + p.provider);
                inv.addItem(stack);
            }
        }

        player.openInventory(inv);
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

        outList.add(CivColor.LightBlue + CivSettings.localize.localizedString("cmd_town_biomeList"));
        //int totalBiomes = 0;
        StringBuilder out = new StringBuilder();
        for (String biome : biomes.keySet()) {
            Integer count = biomes.get(biome);
            out.append(CivColor.Green).append(biome).append(": ").append(CivColor.LightGreen).append(count).append(CivColor.Green).append(", ");
            //totalBiomes += count;
        }
        outList.add(out.toString());
        //	outList.add(CivColor.Green+"Biome Count: "+CivColor.LightGreen+totalBiomes);

        outList.add(CivColor.LightBlue + CivSettings.localize.localizedString("cmd_town_totals"));
        outList.add(CivColor.Green + " " + CivSettings.localize.localizedString("cmd_town_happiness") + " " + CivColor.LightGreen + df.format(happiness) +
                CivColor.Green + " " + CivSettings.localize.localizedString("Hammers") + " " + CivColor.LightGreen + df.format(hammers) +
                CivColor.Green + " " + CivSettings.localize.localizedString("cmd_town_growth") + " " + CivColor.LightGreen + df.format(growth) +
                CivColor.Green + " " + CivSettings.localize.localizedString("Beakers") + " " + CivColor.LightGreen + df.format(beakers));
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
            CivMessage.send(sender, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_town_capitulatePrompt1", town.getCiv().getName()));
            CivMessage.send(sender, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("cmd_town_capitulateConfirm"));
            return;
        }

        /* Town is capitulating, no longer need a mother civ. */
        town.setMotherCiv(null);
        town.save();

        CivMessage.global(CivSettings.localize.localizedString("var_cmd_town_capitulateSuccess1", town.getName(), town.getCiv().getName()));
    }
//	public void capture_cmd() throws CivException {
//		this.validLeaderAdvisor();
//		
//		if (!War.isWarTime()) {
//			throw new CivException("Can only use this command during war time.");
//		}
//		
//		Town town = getNamedTown(1);
//		Civilization civ = getSenderCiv();
//		
//		if (town.getCiv().isAdminCiv()) {
//			throw new CivException("Cannot capture spawn town.");
//		}
//		
//		TownHall townhall = town.getTownHall();
//		if (townhall != null && townhall.isValid()) {
//			throw new CivException("Cannot capture, this town has a valid town hall.");
//		}
//		
//		if (town.claimed) {
//			throw new CivException("Town has already been claimed this war time.");
//		}
//		
//		if (town.getMotherCiv() != null) {
//			throw new CivException("Cannot capture a town already captured by another civ!");
//		}
//		
//		if (town.isCapitol()) {
//			town.getCiv().onDefeat(civ);
//			CivMessage.global("The capitol civilization of "+town.getCiv().getName()+" had an illegal or missing town hall and was claimed by "+civ.getName());
//		} else {
//			town.onDefeat(civ);
//			CivMessage.global("The town of "+town.getName()+" had an illegal or missing town hall and was claimed by "+civ.getName());
//		}
//		
//		town.claimed = true;
//		
//	}

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

    public void s_cmd() throws CivException {
        if (args.length <= 1) {
            throw new CivException(CivSettings.localize.localizedString("EnterTownName"));
        } else {
            select_cmd();
        }
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
        grp.save();
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
                CivMessage.send(sender, i + ") " + CivColor.Gold + CivGlobal.townScores.get(score).getName() + CivColor.White + " - " + score);
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
        for (Town town : CivGlobal.getTowns()) {
            out.append(town.getName()).append("(").append(town.getCiv().getName()).append(")").append(", ");
        }

        CivMessage.send(sender, out.toString());
    }

    public void l_cmd() {
        list_cmd();
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
                CivMessage.send(CivGlobal.getPlayer(residentToKick), CivColor.Yellow + CivSettings.localize.localizedString("cmd_town_evictAlert"));
            } catch (CivException e) {
                //Player not online.
            }
            if (residentToKick.getNativeTown() == town) {
                residentToKick.setNativeTown(0);
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
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    Location ourCapLoc = civ.getCapitolTownHallLocation();

                    if (ourCapLoc == null) {
                        return;
                    }

                    double potentialDistanceLow;
                    double potentialDistanceHigh;
                    try {
                        if (town.getTownHall() != null) {
                            Location theirTownHallLoc = town.getTownHall().getCenterLocation().getLocation();
                            potentialDistanceLow = civ.getDistanceUpkeepAtLocation(ourCapLoc, theirTownHallLoc, true);
                            potentialDistanceHigh = civ.getDistanceUpkeepAtLocation(ourCapLoc, theirTownHallLoc, false);

                            CivMessage.send(player, CivColor.Yellow + CivSettings.localize.localizedString("var_cmd_town_showCost1", potentialDistanceLow));
                            CivMessage.send(player, CivColor.Yellow + CivSettings.localize.localizedString("cmd_town_showCost3"));
                            CivMessage.send(player, CivColor.Yellow + CivSettings.localize.localizedString("var_cmd_town_showCost3", potentialDistanceHigh, CivSettings.CURRENCY_NAME));
                        } else {
                            CivMessage.send(player, CivColor.Yellow + CivSettings.localize.localizedString("cmd_town_showNoTownHall"));
                        }
                    } catch (InvalidConfiguration e) {
                        e.printStackTrace();
                        CivMessage.sendError(sender, CivSettings.localize.localizedString("internalException"));
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
            CivMessage.send(sender, CivColor.LightGray + CivSettings.localize.localizedString("cmd_town_leaveTownChat"));
        }

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_leaveSuccess", town.getName()));
        CivMessage.sendTown(town, CivSettings.localize.localizedString("var_cmd_town_leaveBroadcast", resident.getName()));

        town.save();
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

    public void up_cmd() {
        upgrade_cmd();
    }

    public void u_cmd() {
        upgrade_cmd();
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
            amount = Math.floor(amount);

            if (!town.getTreasury().payTo(resident.getTreasury(), Double.parseDouble(args[1]))) {
                throw new CivException(CivSettings.localize.localizedString("cmd_town_withdrawNotEnough"));
            }
        } catch (NumberFormatException e) {
            throw new CivException(args[1] + " " + CivSettings.localize.localizedString("cmd_enterNumerError2"));
        }
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_withdrawSuccess", args[1], CivSettings.CURRENCY_NAME));
    }

    public void w_cmd() throws CivException {
        if (args.length < 2) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_withdrawPrompt"));
        } else {
            withdraw_cmd();
        }
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

    public void d_cmd() throws CivException {
        if (args.length < 2) {
            throw new CivException(CivSettings.localize.localizedString("cmd_civ_despositPrompt"));
        } else {
            deposit_cmd();
        }
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
                CivMessage.send(resPlayer, CivColor.Yellow + CivSettings.localize.localizedString("var_cmd_town_addAlertError1", player.getName(), town.getName()));
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

        CivMessage.sendSuccess(sender, CivColor.LightGray + CivSettings.localize.localizedString("var_cmd_town_addSuccess", args[1], town.getName()));
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

    public void i_cmd() {
        info_cmd();
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

        TownChunk.claim(town, player, false);
    }

    public void unclaim_cmd() throws CivException {
        Town town = getSelectedTown();
        Player player = getPlayer();
        Resident resident = getResident();
        TownChunk tc = this.getStandingTownChunk();
        if (town.getCiv().isAdminCiv()) {
            if (player.hasPermission(CivSettings.MODERATOR) && !player.hasPermission(CivSettings.MINI_ADMIN)) {
                throw new CivException(CivSettings.localize.localizedString("cmd_town_claimNoPerm"));
            }
        }

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
        if (tc.isOutpost()) {
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_unclaimOutpostSuccess", tc.getCenterString()));
        } else {
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_unclaimSuccess", tc.getCenterString()));
        }

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

    public void m_cmd() throws CivException {
        members_cmd();
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
