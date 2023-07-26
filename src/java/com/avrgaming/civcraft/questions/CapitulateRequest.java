package com.avrgaming.civcraft.questions;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class CapitulateRequest implements QuestionResponseInterface {

	public Town capitulator;
	public String from;
	public String to;
	public String playerName;
	
	@Override
	public void processResponse(String param) {
		if (param.equalsIgnoreCase("accept")) {
			capitulator.capitulate();
			CivMessage.global(CivSettings.localize.localizedString("var_capitulateAccept",from,to));
		} else {
			CivMessage.send(playerName, CivColor.LightGray+CivSettings.localize.localizedString("var_RequestDecline",to));
		}
	}

	@Override
	public void processResponse(String response, Resident responder) {
		processResponse(response);		
	}
}
