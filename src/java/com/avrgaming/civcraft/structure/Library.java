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

import com.avrgaming.civcraft.components.AttributeBiome;
import com.avrgaming.civcraft.components.NonMemberFeeComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.LibraryEnchantment;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.object.Town;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class Library extends Structure {

    private int level;
    public AttributeBiome cultureBeakers;

    ArrayList<LibraryEnchantment> enchantments = new ArrayList<>();

    private final NonMemberFeeComponent nonMemberFeeComponent;

    public static Enchantment getEnchantFromString(String name) {
        switch (name.toLowerCase()) {
            case "protection":
                return Enchantment.PROTECTION_ENVIRONMENTAL;
            case "fire_protection":
                return Enchantment.PROTECTION_FIRE;
            case "feather_falling":
                return Enchantment.PROTECTION_FALL;
            case "blast_protection":
                return Enchantment.PROTECTION_EXPLOSIONS;
            case "projectile_protection":
                return Enchantment.PROTECTION_PROJECTILE;
            case "respiration":
                return Enchantment.OXYGEN;
            case "aqua_affinity":
                return Enchantment.WATER_WORKER;
            case "sharpness":
                return Enchantment.DAMAGE_ALL;
            case "smite":
                return Enchantment.DAMAGE_UNDEAD;
            case "bane_of_arthropods":
                return Enchantment.DAMAGE_ARTHROPODS;
            case "knockback":
                return Enchantment.KNOCKBACK;
            case "fire_aspect":
                return Enchantment.FIRE_ASPECT;
            case "looting":
                return Enchantment.LOOT_BONUS_MOBS;
            case "efficiency":
                return Enchantment.DIG_SPEED;
            case "silk_touch":
                return Enchantment.SILK_TOUCH;
            case "unbreaking":
                return Enchantment.DURABILITY;
            case "fortune":
                return Enchantment.LOOT_BONUS_BLOCKS;
            case "power":
                return Enchantment.ARROW_DAMAGE;
            case "punch":
                return Enchantment.ARROW_KNOCKBACK;
            case "flame":
                return Enchantment.ARROW_FIRE;
            case "infinity":
                return Enchantment.ARROW_INFINITE;
            case "mending":
                return Enchantment.MENDING;
            case "lure":
                return Enchantment.LURE;
            case "frost_walker":
                return Enchantment.FROST_WALKER;
            case "depth_strider":
                return Enchantment.DEPTH_STRIDER;
            case "curse_of_vanishing":
                return Enchantment.VANISHING_CURSE;
            case "curse_of_binding":
                return Enchantment.BINDING_CURSE;
            case "thorns":
                return Enchantment.THORNS;
            case "sweeping_edge":
                return Enchantment.SWEEPING_EDGE;
            case "luck_of_the_sea":
                return Enchantment.LUCK;
            default:
                return null;

        }
    }

    public double getNonResidentFee() {
        return this.nonMemberFeeComponent.getFeeRate();
    }

    public void setNonResidentFee(double nonResidentFee) {
        this.nonMemberFeeComponent.setFeeRate(nonResidentFee);
    }

    private String getNonResidentFeeString() {
        return "Fee: " + ((int) (getNonResidentFee() * 100) + "%");
    }

    protected Library(Location center, String id, Town town)
            throws CivException {
        super(center, id, town);
        nonMemberFeeComponent = new NonMemberFeeComponent(this);
        nonMemberFeeComponent.onSave();
        setLevel(town.saved_library_level);
    }

    public Library(ResultSet rs) throws SQLException, CivException {
        super(rs);
        nonMemberFeeComponent = new NonMemberFeeComponent(this);
        nonMemberFeeComponent.onLoad();
    }

    @Override
    public void loadSettings() {
        super.loadSettings();
    }

    public int getLevel() {
        return level;
    }


    public void setLevel(int level) {
        this.level = level;
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

        for (LibraryEnchantment enchant : this.enchantments) {
            StructureSign sign = getSignFromSpecialId(count);
            if (sign == null) {
                CivLog.error("sign from special id was null, id:" + count);
                return;
            }
            sign.setText(enchant.displayName + "\n" +
                    "Level " + enchant.level + "\n" +
                    getNonResidentFeeString() + "\n" +
                    "For " + enchant.price);
            sign.update();
            count++;
        }

        for (; count < getSigns().size(); count++) {
            StructureSign sign = getSignFromSpecialId(count);
            sign.setText("Library Slot\nEmpty");
            sign.update();
        }
    }

    public void validateEnchantment(ItemStack item, LibraryEnchantment ench) throws CivException {
        if (ench.enchant != null) {

            if (!ench.enchant.canEnchantItem(item)) {
                throw new CivException(CivSettings.localize.localizedString("library_enchant_cannotEnchant"));
            }

            if (item.containsEnchantment(ench.enchant) && item.getEnchantmentLevel(ench.enchant) >= ench.level) {
                throw new CivException(CivSettings.localize.localizedString("library_enchant_hasEnchant"));
            }


        } else {
            if (!ench.enhancement.canEnchantItem(item)) {
                throw new CivException(CivSettings.localize.localizedString("library_enchant_cannotEnchant"));
            }

            if (ench.enhancement.hasEnchantment(item)) {
                throw new CivException(CivSettings.localize.localizedString("library_enchant_hasEnchantment"));
            }
        }
    }

    public ItemStack addEnchantment(ItemStack item, LibraryEnchantment ench) {
        if (ench.enchant != null) {
            item.addUnsafeEnchantment(ench.enchant, ench.level);
        } else {
            item = LoreMaterial.addEnhancement(item, ench.enhancement);
        }
        return item;
    }

    public void add_enchantment_to_tool(Player player, StructureSign sign, PlayerInteractEvent event) throws CivException {
        int special_id = Integer.parseInt(sign.getAction());

        if (!event.hasItem()) {
            CivMessage.send(player, ChatColor.RED + CivSettings.localize.localizedString("library_enchant_itemNotInHand"));
            return;
        }
        ItemStack item = event.getItem();

        if (special_id >= this.enchantments.size()) {
            throw new CivException(CivSettings.localize.localizedString("library_enchant_notReady"));
        }


        LibraryEnchantment ench = this.enchantments.get(special_id);
        this.validateEnchantment(item, ench);

        int payToTown = (int) Math.round(ench.price * getNonResidentFee());
        Resident resident;

        resident = CivGlobal.getResident(player.getName());
        Town t = resident.getTown();
        if (t == this.getTown()) {
            // Pay no taxes! You're a member.
            payToTown = 0;
        }

        // Determine if resident can pay.
        if (!resident.getTreasury().hasEnough(ench.price + payToTown)) {
            CivMessage.send(player, ChatColor.RED + CivSettings.localize.localizedString("var_library_enchant_cannotAfford", ench.price + payToTown, CivSettings.CURRENCY_NAME));
            return;
        }

        // Take money, give to server, TEH SERVER HUNGERS ohmnom nom
        resident.getTreasury().withdraw(ench.price);

        // Send money to town for non-resident fee
        if (payToTown != 0) {
            getTown().depositDirect(payToTown);
            CivMessage.send(player, ChatColor.YELLOW + " " + CivSettings.localize.localizedString("var_taxes_paid", payToTown, CivSettings.CURRENCY_NAME));
        }

        // Successful payment, process enchantment.
        ItemStack newStack = this.addEnchantment(item, ench);
        player.getInventory().setItemInMainHand(newStack);
        CivMessage.send(player, ChatColor.GREEN + CivSettings.localize.localizedString("var_library_enchantment_added", ench.displayName));
    }

    @Override
    public void processSignAction(Player player, StructureSign sign, PlayerInteractEvent event) {
        try {
            add_enchantment_to_tool(player, sign, event);
        } catch (CivException e) {
            CivMessage.send(player, ChatColor.RED + e.getMessage());
        }
    }

    @Override
    public String getDynmapDescription() {
        StringBuilder out = new StringBuilder("<u><b>" + this.getDisplayName() + "</u></b><br/>");

        if (this.enchantments.size() == 0) {
            out.append(CivSettings.localize.localizedString("library_dynmap_nothingStocked"));
        } else {
            for (LibraryEnchantment mat : this.enchantments) {
                out.append(CivSettings.localize.localizedString("var_library_dynmap_item", mat.displayName, mat.price)).append("<br/>");
            }
        }
        return out.toString();
    }


    public ArrayList<LibraryEnchantment> getEnchants() {
        return enchantments;
    }


    public void addEnchant(LibraryEnchantment enchant) throws CivException {
        if (enchantments.size() >= 4) {
            throw new CivException(CivSettings.localize.localizedString("library_full"));
        }
        enchantments.add(enchant);
    }

    @Override
    public String getMarkerIconName() {
        return "bookshelf";
    }

    public void reset() {
        this.enchantments.clear();
        this.updateSignText();
    }

}
