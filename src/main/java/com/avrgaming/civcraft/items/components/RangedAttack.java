package com.avrgaming.civcraft.items.components;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.items.units.Unit;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancementAttack;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import gpl.AttributeUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class RangedAttack extends ItemComponent {

    @Override
    public void onPrepareCreate(AttributeUtil attrs) {
        attrs.addLore(String.valueOf(ChatColor.RED) + this.getDouble("value") + " " + CivSettings.localize.localizedString("itemLore_RangedAttack"));
    }

    private static final double ARROW_MAX_VEL = 6.0;

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (Unit.isWearingAnyMetal(event.getPlayer())) {
            event.setCancelled(true);
            CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("itemLore_RangedAttack_errorMetal"));
        }
    }

    @Override
    public void onHold(PlayerItemHeldEvent event) {

        Resident resident = CivGlobal.getResident(event.getPlayer());
        if (!resident.hasTechForItem(event.getPlayer().getInventory().getItem(event.getNewSlot()))) {
            CivMessage.send(resident, ChatColor.RED + CivSettings.localize.localizedString("itemLore_Warning") + " - " + ChatColor.GRAY +
                    CivSettings.localize.localizedString("itemLore_attackHalfDamage"));
        }
    }

    public void onRangedAttack(EntityDamageByEntityEvent event, ItemStack inHand) {
        AttributeUtil attrs = new AttributeUtil(inHand);
        double dmg = this.getDouble("value");
        Player pp = null;
        if (event.getDamager() instanceof Arrow arrow) {
            if (arrow.getShooter() instanceof Player attacker) {
                if (Unit.isWearingAnyMetal(attacker)) {
                    event.setCancelled(true);
                    CivMessage.sendError(attacker, CivSettings.localize.localizedString("itemLore_RangedAttack_errorMetal"));
                    return;
                }
            }
        }
        if (event.getDamager() instanceof Arrow arrow) {
            if (arrow.getShooter() instanceof Player p) {
                pp = p;
            }
        }
        double extraAtt = 0.0;
        for (LoreEnhancement enh : attrs.getEnhancements()) {
            if (enh instanceof LoreEnhancementAttack) {
                extraAtt += ((LoreEnhancementAttack) enh).getExtraAttack(attrs);
            }
        }
        dmg += extraAtt;
        assert pp != null;
        Resident r = CivGlobal.getResident(pp);
        if (r != null && r.getCiv().hasWonder("w_himeji_castle")) {
            dmg += 2;
        }


        Vector vel = event.getDamager().getVelocity();
        double magnitudeSquared = Math.pow(vel.getX(), 2) + Math.pow(vel.getY(), 2) + Math.pow(vel.getZ(), 2);

        double percentage = magnitudeSquared / ARROW_MAX_VEL;
        double totalDmg = percentage * dmg;

        if (totalDmg > dmg) {
            totalDmg = dmg;
        }

        if (event.getDamager() instanceof Arrow arrow) {
            if (arrow.getShooter() instanceof Player) {
                Resident resident = CivGlobal.getResident(((Player) arrow.getShooter()));
                if (!resident.hasTechForItem(inHand)) {
                    totalDmg = totalDmg / 2;
                }
            }
        }

        event.setDamage(Math.max(totalDmg, 0.5));
    }


}
