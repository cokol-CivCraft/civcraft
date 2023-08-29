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
package com.avrgaming.civcraft.items.components;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancementAttack;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import gpl.AttributeUtil;
import gpl.AttributeUtil.Attribute;
import gpl.AttributeUtil.AttributeType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;


public class Attack extends ItemComponent {

    @Override
    public void onPrepareCreate(AttributeUtil attrs) {

        // Add generic attack damage of 0 to clear the default lore on item.
        attrs.add(Attribute.newBuilder().name("Attack").
                type(AttributeType.GENERIC_ATTACK_DAMAGE).
                amount(0).
                build());
        attrs.addLore(String.valueOf(ChatColor.RED) + this.getDouble("value") + " " + CivSettings.localize.localizedString("itemLore_Attack"));
    }

    @Override
    public void onHold(PlayerItemHeldEvent event) {

        Resident resident = CivGlobal.getResident(event.getPlayer());
        if (!resident.hasTechForItem(event.getPlayer().getInventory().getItem(event.getNewSlot()))) {
            CivMessage.send(resident, ChatColor.RED + CivSettings.localize.localizedString("itemLore_Warning") + " - " + ChatColor.GRAY + CivSettings.localize.localizedString("itemLore_attackHalfDamage"));
        }
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event, ItemStack inHand) {
        AttributeUtil attrs = new AttributeUtil(inHand);
        Resident resident = CivGlobal.getResident(((Player) event.getDamager()));
        double dmg = this.getDouble("value");

        double extraAtt = 0.0;
        for (LoreEnhancement enh : attrs.getEnhancements()) {
            if (enh instanceof LoreEnhancementAttack) {
                extraAtt += ((LoreEnhancementAttack) enh).getExtraAttack(attrs);
            }
        }
        dmg += extraAtt;
        if (resident.getNativeTown() != null && resident.getNativeTown().getBuffManager().hasBuff("wonder_trade_chichen_itza")) {
            dmg += resident.getNativeTown().getBuffManager().getEffectiveDouble("wonder_trade_chichen_itza");
        }
        if (resident.hasEnlightenment()) {
            dmg += 1;
        }

        if (event.getDamager() instanceof Player) {
            if (!resident.hasTechForItem(inHand)) {
                dmg = dmg / 2;
            }
        }
        event.setDamage(Math.max(dmg, 0.5));
    }

}
