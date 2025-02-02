/*
 * AVRGAMING LLC
 * __________________
 *
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.structure;

import com.avrgaming.civcraft.components.NonMemberFeeComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigGrocerLevel;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.object.Town;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.sql.SQLException;
import java.util.UUID;

public class Grocer extends Structure {

    private int level = 1;

    private final NonMemberFeeComponent nonMemberFeeComponent;

    protected Grocer(Location center, String id, Town town) throws CivException {
        super(center, id, town);
        nonMemberFeeComponent = new NonMemberFeeComponent(this);
        nonMemberFeeComponent.onSave();
        setLevel(town.saved_grocer_levels);
    }

    public Grocer(UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(uuid, nbt);
        nonMemberFeeComponent = new NonMemberFeeComponent(this);
        nonMemberFeeComponent.onLoad();
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getNonResidentFee() {
        return nonMemberFeeComponent.getFeeRate();
    }

    public void setNonResidentFee(double nonResidentFee) {
        this.nonMemberFeeComponent.setFeeRate(nonResidentFee);
    }

    private String getNonResidentFeeString() {
        return "Fee: " + ((int) (getNonResidentFee() * 100) + "%");
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

    public void sign_buy_material(Player player, String itemName, Material id, byte data, int amount, double price) {
        Resident resident;
        int payToTown = (int) Math.round(price * this.getNonResidentFee());
        try {

            resident = CivGlobal.getResident(player.getName());
            Town t = resident.getTown();

            if (t == this.getTown()) {
                // Pay no taxes! You're a member.
                resident.buyItem(itemName, id, data, price, amount);
                CivMessage.send(player, ChatColor.GREEN + CivSettings.localize.localizedString("var_grocer_msgBought", amount, itemName, price + " " + CivSettings.CURRENCY_NAME));
            } else {
                // Pay non-resident taxes
                resident.buyItem(itemName, id, data, price + payToTown, amount);
                getTown().depositDirect(payToTown);
                CivMessage.send(player, ChatColor.GREEN + CivSettings.localize.localizedString("var_grocer_msgBought", amount, itemName, price, CivSettings.CURRENCY_NAME));
                CivMessage.send(player, ChatColor.YELLOW + CivSettings.localize.localizedString("var_grocer_msgPaidTaxes", this.getTown().getName(), payToTown + " " + CivSettings.CURRENCY_NAME));
            }

        } catch (CivException e) {
            CivMessage.send(player, ChatColor.RED + e.getMessage());
        }
    }


    @Override
    public void updateSignText() {
        int count;

        for (count = 0; count < level; count++) {
            StructureSign sign = getSignFromSpecialId(count);
            if (sign == null) {
                CivLog.error("sign from special id was null, id:" + count);
                return;
            }
            ConfigGrocerLevel grocerlevel = CivSettings.grocerLevels.get(count + 1);

            sign.setText(CivSettings.localize.localizedString("grocer_sign_buy") + "\n" + grocerlevel.itemName + "\n" +
                    CivSettings.localize.localizedString("grocer_sign_for") + " " + grocerlevel.price + " " + CivSettings.CURRENCY_NAME + "\n" +
                    getNonResidentFeeString());

            sign.update();
        }

        for (; count < getSigns().size(); count++) {
            StructureSign sign = getSignFromSpecialId(count);
            if (sign == null) {
                CivLog.error("sign from special id was null, id:" + count);
                return;
            }
            sign.setText(CivSettings.localize.localizedString("grocer_sign_empty"));
            sign.update();
        }

    }

    @Override
    public void processSignAction(Player player, StructureSign sign, PlayerInteractEvent event) {
        int special_id = Integer.parseInt(sign.getAction());
        if (special_id < this.level) {
            ConfigGrocerLevel grocerlevel = CivSettings.grocerLevels.get(special_id + 1);
            sign_buy_material(player, grocerlevel.itemName, grocerlevel.item.getType(),
                    grocerlevel.item.getData().getData(), grocerlevel.item.getAmount(), grocerlevel.price);
        } else {
            CivMessage.send(player, ChatColor.RED + CivSettings.localize.localizedString("grocer_sign_needUpgrade"));
        }
    }


}
