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

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.*;
import com.avrgaming.civcraft.siege.CannonProjectile;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.FireworkEffectPlayer;
import com.avrgaming.civcraft.util.ItemFrameStorage;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.civcraft.war.WarStats;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.*;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class TownHall extends Structure implements RespawnLocationHolder {

    //TODO make this configurable.
    public static int MAX_GOODIE_FRAMES = 8;

    private final BlockCoord[] techbar = new BlockCoord[10];

    private BlockCoord technameSign;
    private byte technameSignData; //Hold the sign's orientation

    private BlockCoord techdataSign;
    private byte techdataSignData; //Hold the sign's orientation

    private final ArrayList<ItemFrameStorage> goodieFrames = new ArrayList<>();
    private final ArrayList<BlockCoord> respawnPoints = new ArrayList<>();
    private final ArrayList<BlockCoord> revivePoints = new ArrayList<>();
    protected HashMap<BlockCoord, ControlPoint> controlPoints = new HashMap<>();

    public ArrayList<BlockCoord> nextGoodieFramePoint = new ArrayList<>();
    public ArrayList<Integer> nextGoodieFrameDirection = new ArrayList<>();

    protected TownHall(Location center, String id, Town town)
            throws CivException {
        super(center, id, town);
    }

    public TownHall(UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(uuid, nbt);
    }
    @Override
    public void delete() throws SQLException {
        if (this.getTown() == null) {
            super.delete();
            return;
        }
        /* Remove any protected item frames. */
        for (ItemFrameStorage framestore : goodieFrames) {
            BonusGoodie goodie = CivGlobal.getBonusGoodie(framestore.getItem());
            if (goodie != null) {
                goodie.replenish();
            }

            CivGlobal.removeProtectedItemFrame(framestore.getFrameID());
        }
        super.delete();
    }

    public void addTechBarBlock(BlockCoord coord, int index) {
        techbar[index] = coord;
    }

    public BlockCoord getTechBarBlockCoord(int i) {
        return techbar[i];

    }

    public BlockCoord getTechnameSign() {
        return technameSign;
    }

    public void setTechnameSign(BlockCoord technameSign) {
        this.technameSign = technameSign;
    }

    public BlockCoord getTechdataSign() {
        return techdataSign;
    }

    public void setTechdataSign(BlockCoord techdataSign) {
        this.techdataSign = techdataSign;
    }

    public byte getTechdataSignData() {
        return techdataSignData;
    }

    public void setTechdataSignData(byte techdataSignData) {
        this.techdataSignData = techdataSignData;
    }

    public byte getTechnameSignData() {
        return technameSignData;
    }

    public void setTechnameSignData(byte technameSignData) {
        this.technameSignData = technameSignData;
    }

    public BlockCoord getTechBar(int i) {
        return techbar[i];
    }

    public void createGoodieItemFrame(BlockCoord absCoord, int slotId, int direction) {
        if (slotId >= MAX_GOODIE_FRAMES) {
            return;
        }

        /*
         * Make sure there isn't another frame here. We have the position of the sign, but the entity's
         * position is the block it's attached to. We'll use the direction from the sign data to determine
         * which direction to look for the entity.
         */
        Block attachedBlock;
        BlockFace facingDirection;

        switch (direction) {
            case CivData.DATA_SIGN_EAST -> {
                attachedBlock = absCoord.getBlock();
                facingDirection = BlockFace.EAST;
            }
            case CivData.DATA_SIGN_WEST -> {
                attachedBlock = absCoord.getBlock();
                facingDirection = BlockFace.WEST;
            }
            case CivData.DATA_SIGN_NORTH -> {
                attachedBlock = absCoord.getBlock();
                facingDirection = BlockFace.NORTH;
            }
            case CivData.DATA_SIGN_SOUTH -> {
                attachedBlock = absCoord.getBlock();
                facingDirection = BlockFace.SOUTH;
            }
            default -> {
                CivLog.error("Bad sign data for /itemframe sign in town hall.");
                return;
            }
        }

        Block itemFrameBlock = absCoord.getBlock();
        if (itemFrameBlock.getType() != Material.AIR) {
            itemFrameBlock.setType(Material.AIR);
        }

        ItemFrameStorage itemStore;
        ItemFrame frame = null;
        Entity entity = CivGlobal.getEntityAtLocation(absCoord.getBlock().getLocation());
        if (!(entity instanceof ItemFrame)) {
            itemStore = new ItemFrameStorage(attachedBlock.getLocation(), facingDirection);
        } else {
            try {
                frame = (ItemFrame) entity;
                itemStore = new ItemFrameStorage(frame, attachedBlock.getLocation());
            } catch (CivException e) {
                e.printStackTrace();
                return;
            }
            if (facingDirection != BlockFace.EAST) {
                itemStore.setFacingDirection(facingDirection);
            }
        }

        itemStore.setBuildable(this);
        goodieFrames.add(itemStore);

    }

    public ArrayList<ItemFrameStorage> getGoodieFrames() {
        return this.goodieFrames;
    }

    public void setRespawnPoint(BlockCoord absCoord) {
        this.respawnPoints.add(absCoord);
    }

    public BlockCoord getRandomRespawnPoint() { // FOR WAR-ROOM
        if (this.respawnPoints.isEmpty()) {
            return null;
        }

        Random rand = new Random();
        return this.respawnPoints.get(rand.nextInt(this.respawnPoints.size()));

    }

    public int getRespawnTime() {
        int baseRespawn = CivSettings.warConfig.getInt("war.respawn_time", 30);
        int controlRespawn = CivSettings.warConfig.getInt("war.control_block_respawn_time", 15);
        int invalidRespawnPenalty = CivSettings.warConfig.getInt("war.invalid_respawn_penalty", 2);

        int totalRespawn = baseRespawn;
        for (ControlPoint cp : this.controlPoints.values()) {
            if (cp.isDestroyed()) {
                totalRespawn += controlRespawn;
            }
        }

        if (this.validated && !this.isValid()) {
            totalRespawn += invalidRespawnPenalty * 60;
        }

        // Search for any town in our civ with the medicine goodie.
        for (Town t : this.getCiv().getTowns()) {
            if (t.getBuffManager().hasBuff(Buff.MEDICINE)) {
                int respawnTimeBonus = t.getBuffManager().getEffectiveInt(Buff.MEDICINE);
                totalRespawn = Math.max(1, (totalRespawn - respawnTimeBonus));
                break;
            }
        }


        return totalRespawn;
    }

    public void setRevivePoint(BlockCoord absCoord) {
        this.revivePoints.add(absCoord);
    }

    public BlockCoord getRandomRevivePoint() { // FOR STRUCTURE
        if (this.revivePoints.isEmpty() || !this.isComplete()) {
            return new BlockCoord(this.getCorner());
        }
        Random rand = new Random();
        int index = rand.nextInt(this.revivePoints.size());
        return this.revivePoints.get(index);

    }

    public void createControlPoint(BlockCoord absCoord) {

        Location centerLoc = absCoord.getLocation();

        /* Build the bedrock tower. */
        //for (int i = 0; i < 1; i++) {
        Block b = centerLoc.getBlock();
        b.getState().setData(new MaterialData(Material.COBBLESTONE));

        StructureBlock sb = new StructureBlock(new BlockCoord(b), this);
        this.addStructureBlock(sb.getCoord(), true);
        //}

        /* Build the control block. */
        b = centerLoc.getBlock().getRelative(0, 1, 0);
        b.setType(Material.OBSIDIAN);
        sb = new StructureBlock(new BlockCoord(b), this);
        this.addStructureBlock(sb.getCoord(), true);

        int townhallControlHitpoints = CivSettings.warConfig.getInt("war.control_block_hitpoints_townhall", 20);

        BlockCoord coord = new BlockCoord(b);
        this.controlPoints.put(coord, new ControlPoint(coord, this, townhallControlHitpoints));
    }

    public void onControlBlockDestroy(ControlPoint cp, World world, Player player, StructureBlock hit) {
        //Should always have a resident and a town at this point.
        Resident attacker = CivGlobal.getResident(player);

        hit.getCoord().getBlock().setType(Material.AIR);
        world.playSound(hit.getCoord().getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0f, -1.0f);
        world.playSound(hit.getCoord().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        FireworkEffect effect = FireworkEffect.builder().with(Type.BURST).withColor(Color.YELLOW).withColor(Color.RED).withTrail().withFlicker().build();
        FireworkEffectPlayer fePlayer = new FireworkEffectPlayer();
        for (int i = 0; i < 3; i++) {
            try {
                fePlayer.playFirework(world, hit.getCoord().getLocation(), effect);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        boolean allDestroyed = true;
        for (ControlPoint controlPoint : this.controlPoints.values()) {
            if (!controlPoint.isDestroyed()) {
                allDestroyed = false;
                break;
            }
        }
        CivMessage.sendTownSound(hit.getTown(), Sound.AMBIENT_CAVE, 1.0f, 0.5f);

        if (!allDestroyed) {
            CivMessage.sendTown(hit.getTown(), ChatColor.RED + CivSettings.localize.localizedString("townHall_controlBlockDestroyed"));
            CivMessage.sendCiv(attacker.getTown().getCiv(), ChatColor.GREEN + CivSettings.localize.localizedString("var_townHall_didDestroyCB", hit.getTown().getName()));
            CivMessage.sendCiv(hit.getTown().getCiv(), ChatColor.RED + CivSettings.localize.localizedString("var_townHall_civMsg_controlBlockDestroyed", hit.getTown().getName()));
            return;
        }

        if (this.getTown().getCiv().getCapitolName().equals(this.getTown().getName())) {
            CivMessage.globalTitle(ChatColor.AQUA + CivSettings.localize.localizedString("var_townHall_destroyed_isCap", this.getTown().getCiv().getName()), CivSettings.localize.localizedString("var_townHall_destroyed_isCap2", attacker.getCiv().getName()));
            for (Town town : this.getTown().getCiv().getTowns()) {
                town.defeated = true;
            }

            War.transferDefeated(this.getTown().getCiv(), attacker.getTown().getCiv());
            WarStats.logCapturedCiv(attacker.getTown().getCiv(), this.getTown().getCiv());
            War.saveDefeatedCiv(this.getCiv(), attacker.getTown().getCiv());

            if (CivGlobal.isCasualMode()) {
                for (ItemStack stack : player.getInventory().addItem(this.getCiv().getRandomLeaderSkull(CivSettings.localize.localizedString("var_townHall_victoryOverItem", this.getCiv().getName()))).values()) {
                    player.getWorld().dropItem(player.getLocation(), stack);
                }
            }

        } else {
            CivMessage.global(String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.localize.localizedString("var_townHall_destroyed", getTown().getName(), this.getCiv().getName(), attacker.getCiv().getName()));
            //this.getTown().onDefeat(attacker.getTown().getCiv());
            this.getTown().defeated = true;
            //War.defeatedTowns.put(this.getTown().getName(), attacker.getTown().getCiv());
            WarStats.logCapturedTown(attacker.getTown().getCiv(), this.getTown());
            War.saveDefeatedTown(this.getTown().getName(), attacker.getTown().getCiv());
        }


    }

    public void onControlBlockCannonDestroy(ControlPoint cp, Player player, StructureBlock hit) {
        //Should always have a resident and a town at this point.
        Resident attacker = CivGlobal.getResident(player);

        hit.getCoord().getLocation().getBlock().setType(Material.AIR);

        boolean allDestroyed = true;
        for (ControlPoint controlPoint : this.controlPoints.values()) {
            if (!controlPoint.isDestroyed()) {
                allDestroyed = false;
                break;
            }
        }
        CivMessage.sendTownSound(hit.getTown(), Sound.AMBIENT_CAVE, 1.0f, 0.5f);

        if (!allDestroyed) {
            CivMessage.sendTown(hit.getTown(), ChatColor.RED + CivSettings.localize.localizedString("townHall_controlBlockDestroyed"));
            CivMessage.sendCiv(attacker.getTown().getCiv(), ChatColor.GREEN + CivSettings.localize.localizedString("var_townHall_didDestroyCB", hit.getTown().getName()));
            CivMessage.sendCiv(hit.getTown().getCiv(), ChatColor.RED + CivSettings.localize.localizedString("var_townHall_civMsg_controlBlockDestroyed", hit.getTown().getName()));
            return;
        }

        if (this.getTown().getCiv().getCapitolName().equals(this.getTown().getName())) {
            CivMessage.globalTitle(ChatColor.AQUA + CivSettings.localize.localizedString("var_townHall_destroyed_isCap", this.getTown().getCiv().getName()), CivSettings.localize.localizedString("var_townHall_destroyed_isCap2", attacker.getCiv().getName()));
            for (Town town : this.getTown().getCiv().getTowns()) {
                town.defeated = true;
            }

            War.transferDefeated(this.getTown().getCiv(), attacker.getTown().getCiv());
            WarStats.logCapturedCiv(attacker.getTown().getCiv(), this.getTown().getCiv());
            War.saveDefeatedCiv(this.getCiv(), attacker.getTown().getCiv());

            if (CivGlobal.isCasualMode()) {
                HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(this.getCiv().getRandomLeaderSkull(CivSettings.localize.localizedString("var_townHall_victoryOverItem_withCannon", this.getCiv().getName())));
                for (ItemStack stack : leftovers.values()) {
                    player.getWorld().dropItem(player.getLocation(), stack);
                }
            }

        } else {
            CivMessage.global(String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.localize.localizedString("var_townHall_destroyed", getTown().getName(), this.getCiv().getName(), attacker.getCiv().getName()));
            //this.getTown().onDefeat(attacker.getTown().getCiv());
            this.getTown().defeated = true;
            //War.defeatedTowns.put(this.getTown().getName(), attacker.getTown().getCiv());
            WarStats.logCapturedTown(attacker.getTown().getCiv(), this.getTown());
            War.saveDefeatedTown(this.getTown().getName(), attacker.getTown().getCiv());
        }
    }

    public void onControlBlockHit(ControlPoint cp, World world, Player player, StructureBlock hit) {
        world.playSound(hit.getCoord().getLocation(), Sound.BLOCK_ANVIL_USE, 0.2f, 1);
        world.playEffect(hit.getCoord().getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
        CivMessage.sendActionBar(player, CivData.getStringForBar(CivData.TaskType.CONTROL, cp.getHitpoints(), cp.getMaxHitpoints()));

        CivMessage.send(player, ChatColor.GRAY + CivSettings.localize.localizedString("var_townHall_damagedControlBlock", ("(" + cp.getHitpoints() + " / " + cp.getMaxHitpoints() + ")")));
        CivMessage.sendTown(hit.getTown(), ChatColor.YELLOW + CivSettings.localize.localizedString("townHall_cbUnderAttack"));
    }

    @Override
    public void onDamage(int amount, World world, Player player, BlockCoord coord, BuildableDamageBlock hit) {

        ControlPoint controlPoint = this.controlPoints.get(coord);
        Resident resident = CivGlobal.getResident(player);

        if (!resident.canDamageControlBlock()) {
            CivMessage.send(player, ChatColor.RED + CivSettings.localize.localizedString("townHall_damageCB_invalid"));
            return;
        }

        if (controlPoint == null) {
            CivMessage.send(player, ChatColor.RED + CivSettings.localize.localizedString("var_townHall_damage_notCB", this.getDisplayName()));
            return;
        }

        if (controlPoint.isDestroyed()) {
            CivMessage.send(player, ChatColor.RED + CivSettings.localize.localizedString("townHall_damageCB_destroyed"));
            return;
        }

        if (resident.isControlBlockInstantBreak()) {
            controlPoint.damage(controlPoint.getHitpoints());
        } else {
            controlPoint.damage(amount);
        }

        if (controlPoint.isDestroyed()) {
            onControlBlockDestroy(controlPoint, world, player, (StructureBlock) hit);
        } else {
            onControlBlockHit(controlPoint, world, player, (StructureBlock) hit);
        }
    }

    public void regenControlBlocks() {
        for (BlockCoord coord : this.controlPoints.keySet()) {
            coord.getBlock().setType(Material.OBSIDIAN);

            ControlPoint controlPoint = this.controlPoints.get(coord);
            controlPoint.setHitpoints(controlPoint.getMaxHitpoints());
        }
    }

    public int getTechBarSize() {
        return techbar.length;
    }

    @Override
    public void onLoad() {
        // We must load goodies into the frame as we find them from the trade outpost's
        // onLoad() function, otherwise we run into timing issues over which loads first.
    }

    @Override
    public void onPreBuild(Location loc) throws CivException {
        TownHall oldTownHall = this.getTown().getTownHall();
        if (oldTownHall == null) {
            this.autoClaim = true;
            return;
        }
        ChunkCoord coord = new ChunkCoord(loc);
        TownChunk townChunk = CivGlobal.getTownChunk(coord);
        if (townChunk == null || townChunk.getTown() != this.getTown()) {
            throw new CivException(CivSettings.localize.localizedString("townHall_preBuild_outsideBorder"));
        }

        if (War.isWarTime()) {
            throw new CivException(CivSettings.localize.localizedString("townHall_preBuild_duringWar"));
        }

        this.getTown().clearBonusGoods();

        try {
            this.getTown().demolish(oldTownHall, true);
        } catch (CivException e) {
            e.printStackTrace();
        }
        CivMessage.sendTown(this.getTown(), CivSettings.localize.localizedString("var_townHall_preBuild_Success", this.getDisplayName()));
        this.autoClaim = false;
    }

    @Override
    public void onInvalidPunish() {
        int invalid_respawn_penalty = CivSettings.warConfig.getInt("war.invalid_respawn_penalty", 2);
        CivMessage.sendTown(this.getTown(), String.valueOf(ChatColor.RED) + ChatColor.BOLD + CivSettings.localize.localizedString("var_townHall_invalidPunish", invalid_respawn_penalty));
    }

    @Override
    public String getRespawnName() {
        return this.getDisplayName() + "\n" + this.getTown().getName();
    }

    public HashMap<BlockCoord, ControlPoint> getControlPoints() {
        return this.controlPoints;
    }

    public void onCannonDamage(int damage, CannonProjectile projectile) {
        if (!this.getCiv().getDiplomacyManager().isAtWar()) {
            return;
        }
        this.hitpoints -= damage;

//		Resident resident = projectile.whoFired;
        if (hitpoints <= 0) {
            for (BlockCoord coord : this.controlPoints.keySet()) {
                ControlPoint controlPoint = this.controlPoints.get(coord);
                if (controlPoint == null) {
                    continue;
                }
                if (controlPoint.getHitpoints() > CannonProjectile.controlBlockHP) {
                    controlPoint.damage(controlPoint.getHitpoints() - 1);
                    this.hitpoints = this.getMaxHitPoints() / 2;
//						StructureBlock hit = CivGlobal.getStructureBlock(coord);
//						onControlBlockCannonDestroy(controlPoint, CivGlobal.getPlayer(resident), hit);
                    CivMessage.sendCiv(getCiv(), CivSettings.localize.localizedString("var_townHall_cannonHit_destroyCB", this.getDisplayName(), CannonProjectile.controlBlockHP));
                    CivMessage.sendCiv(getCiv(), CivSettings.localize.localizedString("var_townHall_cannonHit_regen", this.getDisplayName(), this.getMaxHitPoints() / 2));
                    return;
                }
            }


            CivMessage.sendCiv(getCiv(), CivSettings.localize.localizedString("var_townHall_cannonHit_destroyed", this.getDisplayName()));
            hitpoints = 0;
        }

        CivMessage.sendCiv(getCiv(), CivSettings.localize.localizedString("var_townHall_cannonHit", this.getDisplayName(), ("(" + this.hitpoints + "/" + this.getMaxHitPoints() + ")")));
    }

    public void onTNTDamage(int damage) {
        if (!this.getCiv().getDiplomacyManager().isAtWar()) {
            return;
        }
        if (hitpoints >= damage + 1) {
            this.hitpoints -= damage;
            CivMessage.sendCiv(getCiv(), CivSettings.localize.localizedString("var_townHall_tntHit", this.getDisplayName(), ("(" + this.hitpoints + "/" + this.getMaxHitPoints() + ")")));
        }

    }
}
