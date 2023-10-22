package com.avrgaming.civcraft.camp;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.*;
import com.avrgaming.civcraft.permission.PlotPermissions;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.RespawnLocationHolder;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.template.Template.TemplateType;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.FireworkEffectPlayer;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.SimpleBlock.Type;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.civcraft.war.WarRegen;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

public class WarCamp extends Buildable implements RespawnLocationHolder {

    public static final String RESTORE_NAME = "special:WarCamps";
    private final ArrayList<BlockCoord> respawnPoints = new ArrayList<>();
    protected HashMap<BlockCoord, ControlPoint> controlPoints = new HashMap<>();

    public static void newCamp(Resident resident, ConfigBuildableInfo info) {

        TaskMaster.syncTask(() -> {
            Player player;
            try {
                player = CivGlobal.getPlayer(resident);
            } catch (CivException e) {
                return;
            }

            try {
                if (!resident.hasTown()) {
                    throw new CivException(CivSettings.localize.localizedString("warcamp_notInCiv"));
                }

                if (!resident.getCiv().getLeaderGroup().hasMember(resident) && !resident.getCiv().getAdviserGroup().hasMember(resident)) {
                    throw new CivException(CivSettings.localize.localizedString("warcamp_mustHaveRank"));
                }

                int warCampMax = CivSettings.warConfig.getInt("warcamp.max", 3);

                if (resident.getCiv().getWarCamps().size() >= warCampMax) {
                    throw new CivException(CivSettings.localize.localizedString("var_warcamp_maxReached", warCampMax));
                }

                LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(player.getInventory().getItemInMainHand());
                if (craftMat == null || !craftMat.hasComponent("FoundWarCamp")) {
                    throw new CivException(CivSettings.localize.localizedString("warcamp_missingItem"));
                }

                WarCamp camp = new WarCamp(resident, player.getLocation(), info);
                camp.buildCamp(player, player.getLocation());
                resident.getCiv().addWarCamp(camp);

                CivMessage.sendSuccess(player, CivSettings.localize.localizedString("warcamp_createSuccess"));
                camp.setWarCampBuilt();
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            } catch (CivException e) {
                CivMessage.sendError(player, e.getMessage());
            }
        });
    }

    public String getSessionKey() {
        return this.getCiv().getName() + ":warcamp:built";
    }

    public void setWarCampBuilt() {
        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getSessionKey());
        Date now = new Date();
        if (entries.isEmpty()) {
            CivGlobal.getSessionDB().add(getSessionKey(), String.valueOf(now.getTime()), this.getCiv().getUUID(), this.getTown().getUUID(), NamedObject.NULL_UUID);
        } else {
            CivGlobal.getSessionDB().update(entries.get(0).request_id, entries.get(0).key, String.valueOf(now.getTime()));
        }
    }

    public int isWarCampCooldownLeft() {
        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getSessionKey());
        Date now = new Date();
        long minsLeft = 0;
        if (entries.isEmpty()) {
            return 0;
        } else {
            Date then = new Date(Long.parseLong(entries.get(0).value));
            int rebuild_timeout = CivSettings.warConfig.getInt("warcamp.rebuild_timeout", 30);

            minsLeft = (then.getTime() + ((long) rebuild_timeout * 60 * 1000)) - now.getTime();
            minsLeft /= 1000;
            minsLeft /= 60;
            if (now.getTime() > (then.getTime() + ((long) rebuild_timeout * 60 * 1000))) {
                return 0;
            }
            return (int) minsLeft;
        }
    }

    public WarCamp(Resident resident, Location loc, ConfigBuildableInfo info) {
        this.setCorner(new BlockCoord(loc));
        this.setTown(resident.getTown());
        this.info = info;
    }

    public void buildCamp(Player player, Location center) throws CivException {

        String templateFile = CivSettings.warConfig.getString("warcamp.template", "warcamp");
        Resident resident = CivGlobal.getResident(player);

        /* Load in the template. */
        Template tpl;
        try {
            String templatePath = Template.getTemplateFilePath(templateFile, TemplateType.STRUCTURE, "default");
            this.setTemplateName(templatePath);
            tpl = Template.getTemplate(templatePath, center);
        } catch (IOException | CivException e) {
            e.printStackTrace();
            throw new CivException("Internal Error.");
        }


        corner.setFromLocation(this.repositionCenter(center, tpl.dir(), tpl.size_x, tpl.size_z));
        checkBlockPermissionsAndRestrictions(player, corner.getBlock(), tpl.size_x, tpl.size_y, tpl.size_z);
        buildWarCampFromTemplate(tpl, corner);
        processCommandSigns(tpl, corner);
        this.saveNow();

        resident.save();

    }

    private void processCommandSigns(Template tpl, BlockCoord corner) {
        for (BlockCoord relativeCoord : tpl.commandBlockRelativeLocations) {
            SimpleBlock sb = tpl.blocks[relativeCoord.getX()][relativeCoord.getY()][relativeCoord.getZ()];
            BlockCoord absCoord = new BlockCoord(corner.getBlock().getRelative(relativeCoord.getX(), relativeCoord.getY(), relativeCoord.getZ()));

            switch (sb.command) {
                case "/respawn" -> {
                    this.respawnPoints.add(absCoord);
                    BlockCoord coord = new BlockCoord(absCoord);
                    Block block1 = coord.getBlock();
                    block1.setType(Material.AIR);
                    this.addStructureBlock(new BlockCoord(absCoord), false);
                    coord = new BlockCoord(absCoord);
                    coord.setY(absCoord.getY() + 1);
                    Block block = coord.getBlock();
                    block.setType(Material.AIR);
                    this.addStructureBlock(coord, false);
                }
                case "/control" -> this.createControlPoint(absCoord);
            }
        }
    }

    protected void checkBlockPermissionsAndRestrictions(Player player, Block centerBlock, int regionX, int regionY, int regionZ) throws CivException {

        if (!War.isWarTime()) {
            throw new CivException(CivSettings.localize.localizedString("warcamp_notWarTime"));
        }

        if (player.getLocation().getY() >= 200) {
            throw new CivException(CivSettings.localize.localizedString("camp_checkTooHigh"));
        }

        if ((regionY + centerBlock.getLocation().getBlockY()) >= 255) {
            throw new CivException(CivSettings.localize.localizedString("camp_checkWayTooHigh"));
        }

        if (player.getLocation().getY() < CivGlobal.minBuildHeight) {
            throw new CivException(CivSettings.localize.localizedString("cannotBuild_toofarUnderground"));
        }

        int minsLeft = this.isWarCampCooldownLeft();
        if (minsLeft > 0) {
            throw new CivException(CivSettings.localize.localizedString("var_warcamp_oncooldown", minsLeft));
        }

        if (!player.isOp()) {
            Buildable.validateDistanceFromSpawn(centerBlock.getLocation());
        }

        int yTotal = 0;
        int yCount = 0;

        for (int x = 0; x < regionX; x++) {
            for (int y = 0; y < regionY; y++) {
                for (int z = 0; z < regionZ; z++) {
                    Block b = centerBlock.getRelative(x, y, z);

                    if (b.getType() == Material.CHEST) {
                        throw new CivException(CivSettings.localize.localizedString("cannotBuild_chestInWay"));
                    }

                    BlockCoord coord = new BlockCoord(b);
                    ChunkCoord chunkCoord = new ChunkCoord(coord.getLocation());

                    TownChunk tc = CivGlobal.getTownChunk(chunkCoord);
                    if (tc != null && !tc.perms.hasPermission(PlotPermissions.Type.DESTROY, CivGlobal.getResident(player))) {
                        // Make sure we have permission to destroy any block in this area.
                        throw new CivException(CivSettings.localize.localizedString("cannotBuild_needPermissions") + " " + b.getX() + "," + b.getY() + "," + b.getZ());
                    }

                    if (CivGlobal.getProtectedBlock(coord) != null) {
                        throw new CivException(CivSettings.localize.localizedString("cannotBuild_protectedInWay"));
                    }

                    if (CivGlobal.getStructureBlock(coord) != null) {
                        throw new CivException(CivSettings.localize.localizedString("cannotBuild_structureInWay"));
                    }

                    if (CivGlobal.getFarmChunk(chunkCoord) != null) {
                        throw new CivException(CivSettings.localize.localizedString("cannotBuild_farmInWay"));
                    }

//                    if (CivGlobal.getWallChunk(chunkCoord) != null) {
//                        throw new CivException(CivSettings.localize.localizedString("cannotBuild_wallInWay"));
//                    }

                    if (CivGlobal.getCampBlock(coord) != null) {
                        throw new CivException(CivSettings.localize.localizedString("cannotBuild_campinWay"));
                    }

                    yTotal += b.getWorld().getHighestBlockYAt(centerBlock.getX() + x, centerBlock.getZ() + z);
                    yCount++;

//                    if (CivGlobal.getRoadBlock(coord) != null) {
//                        throw new CivException(CivSettings.localize.localizedString("warcamp_cannotBuildOnRoad"));
//                    }
                }
            }
        }

        double highestAverageBlock = (double) yTotal / (double) yCount;

        if (((centerBlock.getY() > (highestAverageBlock + 10)) ||
                (centerBlock.getY() < (highestAverageBlock - 10)))) {
            throw new CivException(CivSettings.localize.localizedString("cannotBuild_toofarUnderground"));
        }
    }

    private void buildWarCampFromTemplate(Template tpl, BlockCoord corner) {
        Block cornerBlock = corner.getBlock();
        for (int x = 0; x < tpl.size_x; x++) {
            for (int y = 0; y < tpl.size_y; y++) {
                for (int z = 0; z < tpl.size_z; z++) {
                    Block nextBlock = cornerBlock.getRelative(x, y, z);

                    if (tpl.blocks[x][y][z].specialType == Type.COMMAND) {
                        continue;
                    }

                    if (tpl.blocks[x][y][z].specialType == Type.LITERAL) {
                        // Adding a command block for literal sign placement
                        tpl.blocks[x][y][z].command = "/literal";
                        tpl.commandBlockRelativeLocations.add(new BlockCoord(cornerBlock.getWorld().getName(), x, y, z));
                        continue;
                    }

                    try {
                        if (nextBlock.getType() != tpl.blocks[x][y][z].getType()) {
                            /* XXX Save it as a war block so it's automatically removed when war time ends. */
                            WarRegen.saveBlock(nextBlock, WarCamp.RESTORE_NAME, false);
                            nextBlock.setData(tpl.blocks[x][y][z].getMaterialData().getData()); //TODO
                        }

                        if (nextBlock.getType() != Material.AIR) {
                            this.addStructureBlock(new BlockCoord(nextBlock.getLocation()), true);
                        }
                    } catch (Exception e) {
                        CivLog.error(e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public void load(ResultSet rs) {

    }

    @Override
    public void save() {

    }

    @Override
    public void saveNow() {

    }

    public void createControlPoint(BlockCoord absCoord) {

        Location centerLoc = absCoord.getLocation();

        /* Build the bedrock tower. */
        //for (int i = 0; i < 1; i++) {
        Block b = centerLoc.getBlock();
        WarRegen.saveBlock(b, WarCamp.RESTORE_NAME, false);
        b.setType(Material.FENCE);
        b.setData((byte) 0);

        StructureBlock sb = new StructureBlock(new BlockCoord(b), this);
        this.addStructureBlock(sb.getCoord(), true);
        //}

        /* Build the control block. */
        b = centerLoc.getBlock().getRelative(0, 1, 0);
        WarRegen.saveBlock(b, WarCamp.RESTORE_NAME, false);
        b.setType(Material.OBSIDIAN);

        sb = new StructureBlock(new BlockCoord(b), this);
        this.addStructureBlock(sb.getCoord(), true);

        int townhallControlHitpoints = CivSettings.warConfig.getInt("warcamp.control_block_hitpoints", 20);

        BlockCoord coord = new BlockCoord(b);
        this.controlPoints.put(coord, new ControlPoint(coord, this, townhallControlHitpoints));
    }

    @Override
    public void onDamage(int amount, World world, Player player, BlockCoord coord, BuildableDamageBlock hit) {
        ControlPoint cp = this.controlPoints.get(coord);
        Resident resident = CivGlobal.getResident(player);

        if (cp != null) {
            if (!cp.isDestroyed()) {

                if (resident.isControlBlockInstantBreak()) {
                    cp.damage(cp.getHitpoints());
                } else {
                    cp.damage(amount);
                }

                if (cp.isDestroyed()) {
                    onControlBlockDestroy(cp, world, player, (StructureBlock) hit);
                } else {
                    onControlBlockHit(cp, world, player, (StructureBlock) hit);
                }
            } else {
                CivMessage.send(player, ChatColor.RED + CivSettings.localize.localizedString("camp_controlBlockAlreadyDestroyed"));
            }

        } else {
            CivMessage.send(player, ChatColor.RED + CivSettings.localize.localizedString("structure_cannotDamage") + " " + this.getDisplayName() + ", " + CivSettings.localize.localizedString("structure_targetControlBlocks"));
        }
    }

    public void onControlBlockDestroy(ControlPoint cp, World world, Player player, StructureBlock hit) {
        //Should always have a resident and a town at this point.
        Resident attacker = CivGlobal.getResident(player);

        Block block = hit.getCoord().getLocation().getBlock();
        block.setType(Material.AIR);
        world.playSound(hit.getCoord().getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0f, -1.0f);
        world.playSound(hit.getCoord().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        FireworkEffect effect = FireworkEffect.builder().with(org.bukkit.FireworkEffect.Type.BURST).withColor(Color.OLIVE).withColor(Color.RED).withTrail().withFlicker().build();
        FireworkEffectPlayer fePlayer = new FireworkEffectPlayer();
        for (int i = 0; i < 3; i++) {
            try {
                fePlayer.playFirework(world, hit.getCoord().getLocation(), effect);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        boolean allDestroyed = true;
        for (ControlPoint c : this.controlPoints.values()) {
            if (!c.isDestroyed()) {
                allDestroyed = false;
                break;
            }
        }

        if (allDestroyed) {
            this.onWarCampDestroy();
        } else {
            CivMessage.sendCiv(attacker.getTown().getCiv(), ChatColor.GREEN + CivSettings.localize.localizedString("warcamp_enemyControlBlockDestroyed") + " " + getCiv().getName() + CivSettings.localize.localizedString("warcamp_name"));
            CivMessage.sendCiv(getCiv(), ChatColor.RED + CivSettings.localize.localizedString("warcamp_ownControlBlockDestroyed"));
        }

    }

    private void onWarCampDestroy() {
        CivMessage.sendCiv(this.getCiv(), ChatColor.RED + CivSettings.localize.localizedString("warcamp_ownDestroyed"));
        this.getCiv().getWarCamps().remove(this);

        for (BlockCoord coord : this.structureBlocks.keySet()) {
            CivGlobal.removeStructureBlock(coord);
        }
        this.structureBlocks.clear();

        this.fancyDestroyStructureBlocks();
        setWarCampBuilt();
    }

    public void onControlBlockHit(ControlPoint cp, World world, Player player, StructureBlock hit) {
        world.playSound(hit.getCoord().getLocation(), Sound.BLOCK_ANVIL_USE, 0.2f, 1);
        world.playEffect(hit.getCoord().getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
        CivMessage.sendActionBar(player, CivData.getStringForBar(CivData.TaskType.CONTROL, cp.getHitpoints(), cp.getMaxHitpoints()));


        CivMessage.send(player, ChatColor.GRAY + CivSettings.localize.localizedString("warcamp_hitControlBlock") + " (" + cp.getHitpoints() + " / " + cp.getMaxHitpoints() + ")");
        CivMessage.sendCiv(getCiv(), ChatColor.YELLOW + CivSettings.localize.localizedString("warcamp_controlBlockUnderAttack"));
    }

    @Override
    public String getRespawnName() {
        return "WarCamp\n(" + this.corner.getX() + "," + this.corner.getY() + "," + this.corner.getZ() + ")";
    }

    @Override
    public BlockCoord getRandomRevivePoint() {
        if (this.respawnPoints.isEmpty()) {
            return new BlockCoord(this.getCorner());
        }
        Random rand = new Random();
        int index = rand.nextInt(this.respawnPoints.size());
        return this.respawnPoints.get(index);
    }

    public void onWarEnd() {

        /* blocks are cleared by war regen, but structure blocks need to be cleared. */
        for (BlockCoord coord : this.structureBlocks.keySet()) {
            CivGlobal.removeStructureBlock(coord);
        }

        this.structureBlocks.clear();
    }
}
