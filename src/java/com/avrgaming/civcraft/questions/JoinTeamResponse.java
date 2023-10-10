package com.avrgaming.civcraft.questions;

import com.avrgaming.civcraft.arena.ArenaTeam;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class JoinTeamResponse implements QuestionResponseInterface {

    public ArenaTeam team;
    public Resident resident;
    public Player sender;

    @Override
    public void processResponse(String param) {
        if (!param.equalsIgnoreCase("accept")) {
            CivMessage.send(sender, ChatColor.GRAY + CivSettings.localize.localizedString("var_joinTeam_Declined", resident.getName()));
            return;
        }
        CivMessage.send(sender, ChatColor.GRAY + CivSettings.localize.localizedString("var_joinTeam_accepted", resident.getName()));

        try {
            ArenaTeam.addMember(team.getName(), resident);
        } catch (CivException e) {
            CivMessage.sendError(sender, e.getMessage());
            return;
        }

        CivMessage.sendTeam(team, CivSettings.localize.localizedString("var_joinTeam_Alert", resident.getName()));
    }

    @Override
    public void processResponse(String response, Resident responder) {
        processResponse(response);
    }

}
