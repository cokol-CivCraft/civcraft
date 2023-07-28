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
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.StoreMaterial;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.object.Town;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class Store extends Structure {

    private int level = 1;

    private final NonMemberFeeComponent nonMemberFeeComponent;

    ArrayList<StoreMaterial> materials = new ArrayList<>();

    protected Store(Location center, String id, Town town) throws CivException {
        super(center, id, town);
        nonMemberFeeComponent = new NonMemberFeeComponent(this);
        nonMemberFeeComponent.onSave();
        setLevel(town.saved_store_level);
    }

    protected Store(ResultSet rs) throws SQLException, CivException {
        super(rs);
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
        nonMemberFeeComponent.setFeeRate(nonResidentFee);
    }

    private String getNonResidentFeeString() {
        return "Fee: " + ((int) (nonMemberFeeComponent.getFeeRate() * 100) + "%");
    }

    public void addStoreMaterial(StoreMaterial mat) throws CivException {
        if (materials.size() >= 4) {
            throw new CivException(CivSettings.localize.localizedString("store_isFull"));
        }
        materials.add(mat);
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
        int count = 0;


        // iterate through materials, set signs using array...

        for (StoreMaterial mat : this.materials) {
            StructureSign sign = getSignFromSpecialId(count);
            if (sign == null) {
                CivLog.error("sign from special id was null, id:" + count);
                return;
            }

            sign.setText(CivSettings.localize.localizedString("var_store_sign_buy", mat.name, ((int) mat.price + " " + CivSettings.CURRENCY_NAME), getNonResidentFeeString()));
            sign.update();
            count++;
        }

        // We've finished with all of the materials, update the empty signs to show correct text.
        for (; count < getSigns().size(); count++) {
            StructureSign sign = getSignFromSpecialId(count);
            sign.setText(CivSettings.localize.localizedString("store_sign_empty"));
            sign.update();
        }
    }

    @Override
    public void processSignAction(Player player, StructureSign sign, PlayerInteractEvent event) {
        int special_id = Integer.parseInt(sign.getAction());
        if (special_id < this.materials.size()) {
            StoreMaterial mat = this.materials.get(special_id);
            sign_buy_material(player, mat.name, mat.type, mat.data, 64, mat.price);
        } else {
            CivMessage.send(player, ChatColor.RED + CivSettings.localize.localizedString("store_buy_empty"));
        }
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
                CivMessage.send(player, ChatColor.GREEN + CivSettings.localize.localizedString("var_market_buy", amount, itemName, price, CivSettings.CURRENCY_NAME));
            } else {
                // Pay non-resident taxes
                resident.buyItem(itemName, id, data, price + payToTown, amount);
                getTown().depositDirect(payToTown);
                CivMessage.send(player, ChatColor.YELLOW + CivSettings.localize.localizedString("var_taxes_paid", payToTown, CivSettings.CURRENCY_NAME));
            }

        } catch (CivException e) {
            CivMessage.send(player, ChatColor.RED + e.getMessage());
        }
    }

    @Override
    public String getDynmapDescription() {
        StringBuilder out = new StringBuilder("<u><b>" + this.getDisplayName() + "</u></b><br/>");
        if (this.materials.isEmpty()) {
            out.append(CivSettings.localize.localizedString("store_dynmap_nothingStocked"));
        } else {
            for (StoreMaterial mat : this.materials) {
                out.append(CivSettings.localize.localizedString("var_store_dynmap_item", mat.name, mat.price)).append("<br/>");
            }
        }
        return out.toString();
    }

    @Override
    public String getMarkerIconName() {
        return "bricks";
    }

    public void reset() {
        this.materials.clear();
        this.updateSignText();
    }

}
