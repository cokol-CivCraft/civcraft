package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class University extends Structure {

	
	protected University(Location center, String id, Town town) throws CivException {
		super(center, id, town);
	}

	public University(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public void loadSettings() {
		super.loadSettings();

	}
	
	@Override
	public String getMarkerIconName() {
		return "bronzestar";
	}
	
	private StructureSign getSignFromSpecialId(int special_id) {
		for (StructureSign sign : getSigns()) {
			int id = Integer.valueOf(sign.getAction());
			if (id == special_id) {
				return sign;
			}
		}
		return null;
	}
	
	@Override
	public void updateSignText() {
		int count = 0;
		for (; count < getSigns().size(); count++) {
			StructureSign sign = getSignFromSpecialId(count);
			if (sign == null) {
				CivLog.error("University sign was null");
				return;
			}
			
			sign.setText("\n"+CivSettings.localize.localizedString("university_sign")+"\n"+
					this.getTown().getName());
			
			sign.update();
		}
	}
	
	@Override
	public void processSignAction(Player player, StructureSign sign, PlayerInteractEvent event) {
		CivMessage.send(player, CivColor.Green+CivSettings.localize.localizedString("university_sign")+" "+this.getTown().getName());
	}


}
