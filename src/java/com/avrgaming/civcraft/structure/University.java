package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.object.Town;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.sql.SQLException;
import java.util.UUID;

public class University extends Structure {


    protected University(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    public University(UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(uuid, nbt);
    }

    @Override
    public void loadSettings() {
        super.loadSettings();

    }

    private StructureSign getSignFromSpecialId(int special_id) {
        for (StructureSign sign : getSigns()) {
            int id = Integer.parseInt(sign.getAction());
            if (id == special_id) {
                return sign;
            }
        }
        return null;
    }

    @Override
    public void updateSignText() {
        for (int count = 0; count < getSigns().size(); count++) {
            StructureSign sign = getSignFromSpecialId(count);
            if (sign == null) {
                CivLog.error("University sign was null");
                return;
            }

            sign.setText("\n" + CivSettings.localize.localizedString("university_sign") + "\n" +
                    this.getTown().getName());

            sign.update();
        }
    }

    @Override
    public void processSignAction(Player player, StructureSign sign, PlayerInteractEvent event) {
        CivMessage.send(player, ChatColor.DARK_GREEN + CivSettings.localize.localizedString("university_sign") + " " + this.getTown().getName());
    }


}
