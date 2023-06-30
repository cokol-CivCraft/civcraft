// changelog:
// https://github.com/ataranlen/civcraft/pull/73/commits/a34dd3a5bbc71b503f8dcc386fce3801abd25900

package com.avrgaming.civcraft.arena;


import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigArena;
import com.avrgaming.civcraft.config.ConfigArenaTeam;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;

public class Arena {
	public ConfigArena config;
	public int instanceID;
	
	private HashMap<Integer, ArenaTeam> teams = new HashMap<Integer, ArenaTeam>();
	private HashMap<Integer, Integer> teamIDmap = new HashMap<Integer, Integer>();
	private HashMap<Integer, Integer> teamHP = new HashMap<Integer, Integer>();
	private HashMap<String, Inventory> playerInvs = new HashMap<String, Inventory>();
	public  HashMap<String, Scoreboard> scoreboards = new HashMap<String, Scoreboard>();
	public  HashMap<String, Objective> objectives = new HashMap<String, Objective>();
	public int timeleft;
	public boolean ended = false;
		
	int teamCount = 0;
	
	public static int nextInstanceID = 0;
	
	public Arena(ConfigArena a) throws CivException {
		this.config = a;
		
		/* Search for a free instance id. */
		boolean found = false;
		int id = 0;
		for (int i = 0; i < ArenaManager.MAX_INSTANCES; i++) {
			String possibleName = getInstanceName(id, config);
			if (ArenaManager.activeArenas.containsKey(possibleName)) {
				id++;
			} else {
				found = true;
				break;
			}
		}
		
		if (!found) {
			throw new CivException("Couldn't find a free instance ID!");
		}
		
		try {
			this.timeleft = CivSettings.getInteger(CivSettings.arenaConfig, "timeout");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
		this.instanceID = id;
				
	}

	public static String getInstanceName(int id, ConfigArena config) {
		String instanceWorldName = config.world_source+"_"+"instance_"+id;
		return instanceWorldName;
	}
	
	public String getInstanceName() {
		return getInstanceName(this.instanceID, config);
	}
	
	@SuppressWarnings("deprecation")
	public void addTeam(ArenaTeam team) throws CivException {
		this.scoreboards.put(team.getName(), ArenaManager.scoreboardManager.getNewScoreboard());
		
		teams.put(teamCount, team);
		teamIDmap.put(team.getId(), teamCount);
		teamHP.put(teamCount, config.teams.get(teamCount).controlPoints.size());
		team.setScoreboardTeam(getScoreboard(team.getName()).registerNewTeam(team.getName()));
		team.getScoreboardTeam().setAllowFriendlyFire(false);
		if (teamCount == 0) {
			team.setTeamColor(CivColor.Blue);
		} else {
			team.setTeamColor(CivColor.Gold);
		}
		for (Resident r : team.teamMembers) {
			Player p = CivGlobal.getPlayer(r);
			p.setHealth(p.getMaxHealth());
			p.setFoodLevel(20);
			p.setGameMode(GameMode.SURVIVAL);
			p.setDisplayName(team.getTeamColor() + CivColor.BOLD + p.getDisplayName());
		}
		
		for (Resident resident : team.teamMembers) {
			try {
			CivGlobal.getPlayer(resident);
			} catch (CivException e) {
				continue;
			}
			
			try {
				teleportToRandomRevivePoint(resident, teamCount);
				createInventory(resident, teamCount);
				team.getScoreboardTeam().addPlayer(Bukkit.getOfflinePlayer(resident.getUUID()));
			} catch (CivException e) {
				e.printStackTrace();
			}
		}
		
		
		teamCount++;
	}
	
	private void createInventory(Resident resident, int teamnumber) {
		Player player;
			try {
				player = CivGlobal.getPlayer(resident);

				Inventory inv = Bukkit.createInventory(player, 9 * 6, resident.getName() + "'s Gear");
				if (teamnumber == 0) {
					for (int i = 0; i < 3; i++) {
						addCivCraftItemToInventory("mat_arena_iron_sword", inv);
						addCivCraftItemToInventory("mat_arena_iron_boots", inv);
						addCivCraftItemToInventory("mat_arena_iron_chestplate", inv);
						addCivCraftItemToInventory("mat_arena_iron_leggings", inv);
						addCivCraftItemToInventory("mat_arena_iron_helmet", inv);

						addCivCraftItemToInventory("mat_arena_hunting_bow", inv);
						addCivCraftItemToInventory("mat_arena_leather_boots", inv);
						addCivCraftItemToInventory("mat_arena_leather_chestplate", inv);
						addCivCraftItemToInventory("mat_arena_leather_leggings", inv);
						addCivCraftItemToInventory("mat_arena_leather_helmet", inv);
						addCivCraftItemToInventory("mat_vanilla_diamond_pickaxe", inv);
					}
				} else {
					for (int i = 0; i < 3; i++) {
						addCivCraftItemToInventory("mat_arena_iron_sword_two", inv);
						addCivCraftItemToInventory("mat_arena_iron_boots_two", inv);
						addCivCraftItemToInventory("mat_arena_iron_chestplate_two", inv);
						addCivCraftItemToInventory("mat_arena_iron_leggings_two", inv);
						addCivCraftItemToInventory("mat_arena_iron_helmet_two", inv);

						addCivCraftItemToInventory("mat_arena_hunting_bow_two", inv);
						addCivCraftItemToInventory("mat_arena_leather_boots_two", inv);
						addCivCraftItemToInventory("mat_arena_leather_chestplate_two", inv);
						addCivCraftItemToInventory("mat_arena_leather_leggings_two", inv);
						addCivCraftItemToInventory("mat_arena_leather_helmet_two", inv);
						addCivCraftItemToInventory("mat_vanilla_diamond_pickaxe", inv);
					}
				}
					playerInvs.put(resident.getName(), inv);
			} catch(CivException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

    private void addCivCraftItemToInventory(String id, Inventory inv) {
		LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterialFromId(id);
		ItemStack stack = LoreCraftableMaterial.spawn(craftMat);
		ItemStack sis = new ItemStack(Material.GOLDEN_CARROT, 16);
		stack = LoreCraftableMaterial.addEnhancement(stack, LoreEnhancement.enhancements.get("LoreEnhancementArenaItem"));
        stack = LoreCraftableMaterial.addEnhancement(stack, LoreEnhancement.enhancements.get("LoreEnhancementSoulBound"));
		stack = LoreCraftableMaterial.addEnhancement(stack, LoreEnhancement.enhancements.get("LoreEnhancementUnbreaking"));
		switch (stack.getType()) {
			case BOW:
				stack.addEnchantment(Enchantment.ARROW_INFINITE, 1);
				ItemStack is = new ItemStack(Material.ARROW, 5);
				inv.addItem(is);
				break;
			case DIAMOND_PICKAXE:
				stack.addEnchantment(Enchantment.DIG_SPEED, 5);

				break;

		}
		inv.addItem(stack);
		inv.addItem(sis);
	}
	
	private ConfigArenaTeam getConfigTeam(int id) throws CivException {
		for (ConfigArenaTeam ct : config.teams) {
			if (ct.number == id) {
				return ct;
			}
		}
		throw new CivException("Couldn't find configuration for team id:"+id);
	}
	
	public void teleportToRandomRevivePoint(Resident r, int teamID) throws CivException {
		ConfigArenaTeam ct = getConfigTeam(teamID);
		Random rand = new Random();
		int index = rand.nextInt(ct.revivePoints.size());
		
		int i = 0;
		for (BlockCoord coord : ct.revivePoints) {
			
			if (index == i) {
				try {
					Player player = CivGlobal.getPlayer(r);
					coord.setWorldname(this.getInstanceName());
					player.teleport(coord.getLocation());
				} catch (CivException e) {
					// Player offline..
					e.printStackTrace();
				}
			}
			i++;
		}

	}

	public void returnPlayers() {
		for (ArenaTeam team : teams.values()) {
			for (Resident r : team.teamMembers) {
				try {
					try {
						/* Only set inside arena to false if the player is online. */
						Player player = CivGlobal.getPlayer(r);
						player.setScoreboard(ArenaManager.scoreboardManager.getNewScoreboard());
						
						r.setInsideArena(false);
						r.restoreInventory();
						r.teleportHome();
						r.save();
						CivMessage.send(r, CivColor.LightGray+CivSettings.localize.localizedString("arena_endedTeleport"));
					} catch (CivException e) {
						/* player not online, inside arena is set true */
					}
				} catch (Exception e) {
					// Continue if there was an error restoring one player.
					r.teleportHome();
					e.printStackTrace();
				}
			}
		}
	}

	public Collection<ArenaTeam> getTeams() {
		return teams.values();
	}
	
	public ArenaTeam getTeamFromID(int id) {
		return teams.get(id);
	}
	
	public void onControlBlockDestroy(int teamID, ArenaTeam attackingTeam) {
		Integer hp = teamHP.get(teamID);
		hp--;
		teamHP.put(teamID, hp);
		
		ArenaTeam team = teams.get(teamID);
		
		if (hp <= 0) {
			ArenaManager.declareVictor(this, team, attackingTeam);
		}
		
	}

	public void clearTeams() {
		for (ArenaTeam team : teams.values()) {
			team.setCurrentArena(null);
		}
		
		teams.clear();
	}

	public Location getRespawnLocation(Resident resident) {
		int teamID = teamIDmap.get(resident.getTeam().getId());
		for (int i = 0; i < config.teams.size(); i++) {
			ConfigArenaTeam configTeam = config.teams.get(i);
			if (configTeam.number == teamID) {
				Random rand = new Random();
				int index = rand.nextInt(configTeam.respawnPoints.size());
				return configTeam.respawnPoints.get(index).getCenteredLocation();
			}
		}
		
		return null;
	}

	public BlockCoord getRandomReviveLocation(Resident resident) {
		int teamID = teamIDmap.get(resident.getTeam().getId());
		for (int i = 0; i < config.teams.size(); i++) {
			ConfigArenaTeam configTeam = config.teams.get(i);
			if (configTeam.number == teamID) {
				Random rand = new Random();
				int index = rand.nextInt(configTeam.respawnPoints.size());
				return configTeam.revivePoints.get(index);
			}
		}
		
		return null;
	}

	public Inventory getInventory(Resident resident) {
		return playerInvs.get(resident.getName());
	}
	
	public Scoreboard getScoreboard(String name) {
		return this.scoreboards.get(name);
	}

	public void decrementScoreForTeamID(int teamID) {
		ArenaTeam team = getTeamFromID(teamID);
		
		for (ArenaTeam t : this.teams.values()) {
			Objective obj = this.objectives.get(t.getName());
			
			for (ArenaTeam t2 : this.teams.values()) {
				Score score = obj.getScore(t2.getTeamScoreboardName());
				if (t2.getName().equals(team.getName())) {
					score.setScore(score.getScore() - 1);
				}
			}
		}
	}

	public void decrementTimer() {
		if (timeleft <= 0) {
			if (!ended) {
				CivMessage.sendArena(this, CivSettings.localize.localizedString("arena_timeUp"));
				ArenaManager.declareDraw(this);
				ended = true;
			}
		} else {
			this.timeleft--;

			for (ArenaTeam team : this.teams.values()) {
				Objective obj = objectives.get(team.getName());
				if (obj != null) {
					Score score = obj.getScore("TimeLeft");
					score.setScore(timeleft);
				}
			}
		}
	}
	
}
