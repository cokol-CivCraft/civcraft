package com.avrgaming.civcraft.arena;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Sound;
import org.bukkit.World;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.FireworkEffectPlayer;
import com.avrgaming.civcraft.util.ItemManager;

public class ArenaControlBlock {
	public BlockCoord coord;
	public int teamID;
	public int maxHP;
	public int curHP;
	public Arena arena;
	
	public ArenaControlBlock(BlockCoord c, int teamID, int maxHP, Arena arena) {
		this.coord = c;
		this.teamID = teamID;
		this.maxHP = maxHP;
		this.curHP = maxHP;
		this.arena = arena;
	}
	
	public void onBreak(Resident resident) {
		if (resident.getTeam() == arena.getTeamFromID(teamID)) {
			CivMessage.sendError(resident, CivSettings.localize.localizedString("arena_cannotDamage"));
			return;
		}
		
		if (curHP == 0) {
			return;
		}
		
		curHP--;
		
		arena.decrementScoreForTeamID(teamID);	
		
		if (curHP <= 0) {
			/* Destroy control block. */
			explode();
			arena.onControlBlockDestroy(teamID, resident.getTeam());

		} else {
			CivMessage.sendTeam(resident.getTeam(), CivSettings.localize.localizedString("var_arena_playerHitControlBlock",CivColor.LightGreen+resident.getName(),(curHP+" / "+maxHP)));
			CivMessage.sendTeam(arena.getTeamFromID(teamID), CivColor.Rose+CivSettings.localize.localizedString("var_arena_announceHitControlBlock",resident.getName(),(curHP+" / "+maxHP)));
		}
		
	}
	
	public void explode() {
		World world = Bukkit.getWorld(coord.getWorldname());
		ItemManager.setTypeId(coord.getLocation().getBlock(), CivData.AIR);
		world.playSound(coord.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0f, -1.0f);
		world.playSound(coord.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
		
		FireworkEffect effect = FireworkEffect.builder().with(Type.BURST).withColor(Color.YELLOW).withColor(Color.RED).withTrail().withFlicker().build();
		FireworkEffectPlayer fePlayer = new FireworkEffectPlayer();
		for (int i = 0; i < 3; i++) {
			try {
				fePlayer.playFirework(world, coord.getLocation(), effect);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
