package com.avrgaming.civcraft.questions;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.FoundTownSync;
import org.bukkit.ChatColor;

public class TownNewRequest implements QuestionResponseInterface {

    public Resident resident;
    public Resident leader;
    public Civilization civ;
    public String name;

    @Override
    public void processResponse(String param) {
        if (!param.equalsIgnoreCase("accept")) {
            CivMessage.send(resident, ChatColor.GRAY + CivSettings.localize.localizedString("var_newTown_declined", leader.getName()));
            return;
        }
        CivMessage.send(civ, ChatColor.GREEN + CivSettings.localize.localizedString("newTown_accepted1", leader.getName(), name));
        TaskMaster.syncTask(new FoundTownSync(resident));
    }

    @Override
    public void processResponse(String response, Resident responder) {
        this.leader = responder;
        processResponse(response);
    }
}
