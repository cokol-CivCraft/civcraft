package com.avrgaming.civcraft.questions;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import org.bukkit.ChatColor;

public class CapitulateRequest implements QuestionResponseInterface {

    public Town capitulator;
    public String from;
    public String to;
    public String playerName;

    @Override
    public void processResponse(String param) {
        if (!param.equalsIgnoreCase("accept")) {
            CivMessage.send(playerName, ChatColor.GRAY + CivSettings.localize.localizedString("var_RequestDecline", to));
            return;
        }
        capitulator.capitulate();
        CivMessage.global(CivSettings.localize.localizedString("var_capitulateAccept", from, to));
    }

    @Override
    public void processResponse(String response, Resident responder) {
        processResponse(response);
    }
}
