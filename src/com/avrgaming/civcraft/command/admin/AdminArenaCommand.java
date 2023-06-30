package com.avrgaming.civcraft.command.admin;

import com.avrgaming.civcraft.arena.Arena;
import com.avrgaming.civcraft.arena.ArenaManager;
import com.avrgaming.civcraft.arena.ArenaTeam;
import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;

public class AdminArenaCommand extends CommandBase {

	@Override
	public void init() {
		command = "/ad arena";
		displayName = CivSettings.localize.localizedString("adcmd_arena_Name");
				
		commands.put("list", CivSettings.localize.localizedString("adcmd_arena_listDesc"));
		commands.put("end", CivSettings.localize.localizedString("adcmd_arena_listDesc"));
		commands.put("messageall", CivSettings.localize.localizedString("adcmd_arena_msgAllDesc"));
		commands.put("message", CivSettings.localize.localizedString("adcmd_arena_msgdesc"));
		commands.put("enable", CivSettings.localize.localizedString("adcmd_arena_enableDesc"));
		commands.put("disable", CivSettings.localize.localizedString("adcmd_arena_disableDesc"));
	}

	public void enable_cmd() {
		ArenaManager.enabled = true;
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_arena_Enabled"));
	}
	
	public void disable_cmd() {
		ArenaManager.enabled = false;
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_arena_disabled"));
	}
	
	public void list_cmd() {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_arena_activeArenas"));
		for (Arena arena : ArenaManager.activeArenas.values()) {
			String teams = "";
			for (ArenaTeam team : arena.getTeams()) {
				teams += team.getName()+", ";
			}

			CivMessage.send(sender, arena.getInstanceName()+": "+CivSettings.localize.localizedString("adcmd_arena_activeArenasTeams")+" "+teams);
		}
	}
	
	public void messageall_cmd() {
		String message = this.combineArgs(this.stripArgs(args, 1));
		for (Arena arena : ArenaManager.activeArenas.values()) {
			CivMessage.sendArena(arena, CivColor.Rose+CivSettings.localize.localizedString("adcmd_arena_adminMessage")+CivColor.RESET+message);
		}
		CivMessage.send(sender, CivColor.Rose+CivSettings.localize.localizedString("adcmd_arena_adminMessage")+CivColor.RESET+message);
	}
	
	public void message_cmd() throws CivException {
		String id = getNamedString(1, CivSettings.localize.localizedString("adcmd_arena_enterInstanceName"));
		String message = this.combineArgs(this.stripArgs(args, 2));

		Arena arena = ArenaManager.activeArenas.get(id);
		if (arena == null) {
			throw new CivException(CivSettings.localize.localizedString("adcmd_arena_arenaIDNotFound"));
		}
		
		CivMessage.sendArena(arena, CivColor.Rose+"ADMIN:"+CivColor.RESET+message);
		CivMessage.send(sender, CivColor.Rose+"ADMIN:"+CivColor.RESET+message);

	}
	
	public void end_cmd() throws CivException {
		String id = getNamedString(1, CivSettings.localize.localizedString("adcmd_arena_enterInstanceName"));
		
		Arena arena = ArenaManager.activeArenas.get(id);
		if (arena == null) {
			throw new CivException(CivSettings.localize.localizedString("adcmd_arena_arenaIDNotFound"));
		}
		
		CivMessage.sendArena(arena, CivColor.Rose+CivSettings.localize.localizedString("adcmd_arena_endDraw"));
		ArenaManager.declareDraw(arena);
		
	}
	
	@Override
	public void doDefaultAction() throws CivException {
		showHelp();
	}

	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() throws CivException {
		
	}

}
