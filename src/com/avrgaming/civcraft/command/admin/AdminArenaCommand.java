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


        register_sub("list", this::list_cmd, CivSettings.localize.localizedString("adcmd_arena_listDesc"));
        register_sub("end", this::end_cmd, CivSettings.localize.localizedString("adcmd_arena_listDesc"));
        register_sub("messageall", this::messageall_cmd, CivSettings.localize.localizedString("adcmd_arena_msgAllDesc"));
        register_sub("message", this::message_cmd, CivSettings.localize.localizedString("adcmd_arena_msgdesc"));
        register_sub("enable", this::enable_cmd, CivSettings.localize.localizedString("adcmd_arena_enableDesc"));
        register_sub("disable", this::disable_cmd, CivSettings.localize.localizedString("adcmd_arena_disableDesc"));
    }
	@SuppressWarnings("unused")
	public void enable_cmd() {
		ArenaManager.enabled = true;
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_arena_Enabled"));
	}

	@SuppressWarnings("unused")
	public void disable_cmd() {
		ArenaManager.enabled = false;
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_arena_disabled"));
	}
	
	public void list_cmd() {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_arena_activeArenas"));
		for (Arena arena : ArenaManager.activeArenas.values()) {
			StringBuilder teams = new StringBuilder();
			for (ArenaTeam team : arena.getTeams()) {
                teams.append(team.getName()).append(", ");
			}

			CivMessage.send(sender, arena.getInstanceName()+": "+CivSettings.localize.localizedString("adcmd_arena_activeArenasTeams")+" "+teams);
		}
	}

	@SuppressWarnings("unused")
	public void messageall_cmd() {
		String message = this.combineArgs(this.stripArgs(args, 1));
		for (Arena arena : ArenaManager.activeArenas.values()) {
			CivMessage.sendArena(arena, CivColor.Rose+CivSettings.localize.localizedString("adcmd_arena_adminMessage")+CivColor.RESET+message);
		}
		CivMessage.send(sender, CivColor.Rose+CivSettings.localize.localizedString("adcmd_arena_adminMessage")+CivColor.RESET+message);
	}

	@SuppressWarnings("unused")
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

	@SuppressWarnings("unused")
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
    public void doDefaultAction() {
        showHelp();
    }

	@Override
	public void showHelp() {
		showBasicHelp();
	}

    @Override
    public void permissionCheck() {

    }

}
