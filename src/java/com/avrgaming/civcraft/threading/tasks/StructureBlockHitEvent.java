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
package com.avrgaming.civcraft.threading.tasks;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.BuildableDamageBlock;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.BlockCoord;
import gpl.AttributeUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class StructureBlockHitEvent implements Runnable {

    /*
     * Called when a structure block is hit, this async task quickly determines
     * if the block hit should take damage during war.
     *
     */
    String playerName;
    BlockCoord coord;
    BuildableDamageBlock dmgBlock;
    World world;

    public StructureBlockHitEvent(String player, BlockCoord coord, BuildableDamageBlock dmgBlock, World world) {
        this.playerName = player;
        this.coord = coord;
        this.dmgBlock = dmgBlock;
        this.world = world;
    }

    @Override
    public void run() {

        if (playerName == null) {
            return;
        }
        Player player;
        Resident r;
        try {
            player = CivGlobal.getPlayer(playerName);
            r = CivGlobal.getResident(player);
        } catch (CivException e) {
            //Player offline now?
            return;
        }
        if (dmgBlock.allowDamageNow(player)) {
            /* Do our damage. */
            int damage = 1;
            LoreMaterial material = LoreMaterial.getMaterial(player.getInventory().getItemInMainHand());
            if (material != null) {
                damage = material.onStructureBlockBreak(dmgBlock, damage);
            }

            if (player.getInventory().getItemInMainHand() != null && !player.getInventory().getItemInMainHand().getType().equals(Material.AIR)) {
                AttributeUtil attrs = new AttributeUtil(player.getInventory().getItemInMainHand());
                for (LoreEnhancement enhance : attrs.getEnhancements()) {
                    damage = enhance.onStructureBlockBreak(dmgBlock, damage);
                }
            }

            if (damage > 1) {
                CivMessage.send(player, ChatColor.YELLOW + CivSettings.localize.localizedString("var_StructureBlockHitEvent_punchoutDmg", (damage - 1)));
            }
            if (r.getTown().getBuffManager().hasBuff("wonder_trade_colossus") && !dmgBlock.getTown().getBuffManager().hasBuff(Buff.DEFENCE)) {
                Random rn = new Random();
                if (rn.nextInt(100) <= r.getTown().getBuffManager().getEffectiveDouble("wonder_trade_colossus") * 100) {
                    int p = rn.nextInt(5);
                    damage += p;
                    if (p != 0) {
                        CivMessage.send(player, ChatColor.GOLD + CivSettings.localize.localizedString("var_colossus_punchout" + p));
                    }
                }
            }

            dmgBlock.getOwner().onDamage(damage, world, player, dmgBlock.getCoord(), dmgBlock);
            // TaskMaster.asyncTask(new UpdateBlockUnderAttack(dmgBlock.getCoord().getBlock()), 0);
            Random ran = new Random();
            player.setCooldown(player.getInventory().getItemInMainHand().getType(), ran.nextInt(4, 6));
            if (dmgBlock.getCiv().hasWonder("w_himeji_castle")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 3, 0, false, false));
            }
        } else {
            CivMessage.sendErrorNoRepeat(player,
                    CivSettings.localize.localizedString("var_StructureBlockHitEvent_Invulnerable", dmgBlock.getOwner().getDisplayName(), dmgBlock.getTown().getName()));
        }
    }
}
