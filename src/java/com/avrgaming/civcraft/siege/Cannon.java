package com.avrgaming.civcraft.siege;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.template.Template.TemplateType;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.FireWorkTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.SimpleBlock.Type;
import com.avrgaming.civcraft.util.TimeTools;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.civcraft.war.WarRegen;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class Cannon extends Buildable {

    public static HashMap<BlockCoord, Cannon> fireSignLocations = new HashMap<>();
    public static HashMap<BlockCoord, Cannon> angleSignLocations = new HashMap<>();
    public static HashMap<BlockCoord, Cannon> powerSignLocations = new HashMap<>();
    public static HashMap<BlockCoord, Cannon> cannonBlocks = new HashMap<>();
    private BlockCoord fireSignLocation;
    private BlockCoord angleSignLocation;
    private BlockCoord powerSignLocation;
    private Location cannonLocation;
    private final Vector direction = new Vector(0, 0, 0);
    public static final String RESTORE_NAME = "special:Cannons";
    public static final double STEP = 1.0f;
    public BlockFace signDirection;
    public static final double minAngle = -35.0f;
    public static final double maxAngle = 35.0f;
    private double angle = 0.0f;
    public static double minPower = 0.0f;
    public static double maxPower = 50.0f;
    private double power = 0.0f;
    private int tntLoaded = 0;
    private int shotCooldown = 0;
    private int hitpoints = 0;
    private static int maxHeight = 135;
    private Resident owner;
    private final HashSet<BlockCoord> blocks = new HashSet<>();
    public static int tntCost;
    public static int maxCooldown;
    public static int maxHitpoints;
    public static int baseStructureDamage;
    private boolean angleFlip = false;
    static {
        tntCost = CivSettings.warConfig.getInt("cannon.tnt_cost", 3);
        maxHeight = CivSettings.warConfig.getInt("cannon.height", 135);
        minPower = CivSettings.warConfig.getDouble("cannon.min_power", 0);
        maxPower = CivSettings.warConfig.getDouble("cannon.max_power", 50);
        maxCooldown = CivSettings.warConfig.getInt("cannon.cooldown", 30);
        maxHitpoints = CivSettings.warConfig.getInt("cannon.hitpoints", 50);
        baseStructureDamage = CivSettings.warConfig.getInt("cannon.structure_damage", 100);
    }

    public static void newCannon(Resident resident, Location loc) throws CivException {

        Player player = CivGlobal.getPlayer(resident);

        Cannon cannon = new Cannon();
        cannon.buildCannon(player, loc);

    }

    public static void cleanupAll() {
        cannonBlocks.clear();
        powerSignLocations.clear();
        angleSignLocations.clear();
        fireSignLocations.clear();
    }

    private static void removeAllValues(Cannon cannon, HashMap<BlockCoord, Cannon> map) {
        LinkedList<BlockCoord> removeUs = new LinkedList<>();
        for (BlockCoord bcoord : map.keySet()) {
            Cannon c = map.get(bcoord);
            if (c == cannon) {
                removeUs.add(bcoord);
            }
        }

        for (BlockCoord bcoord : removeUs) {
            map.remove(bcoord);
        }
    }

    public void cleanup() {
        removeAllValues(this, cannonBlocks);
        removeAllValues(this, powerSignLocations);
        removeAllValues(this, angleSignLocations);
        removeAllValues(this, fireSignLocations);
    }

    public int getTntCost() {
        int tnt = tntCost;
        if (owner.getCiv() != null && owner.getCiv().hasWonder("w_council_of_eight")) {
            tnt -= 1;
        }
        return tnt;
    }

    public void buildCannon(Player player, Location center) throws CivException {
        String templateFile = CivSettings.warConfig.getString("cannon.template", "cannon");

        /* Load in the template. */
        Template tpl;
        try {
            String templatePath = Template.getTemplateFilePath(templateFile, TemplateType.STRUCTURE, "default");
            this.setTemplateName(templatePath);
            tpl = Template.getTemplate(templatePath, center);
        } catch (IOException | CivException e) {
            e.printStackTrace();
            throw new CivException(CivSettings.localize.localizedString("internalCommandException"));
        }

        corner = new BlockCoord(center);
        corner.setFromLocation(this.repositionCenter(center, tpl.dir(), tpl.size_x, tpl.size_z));
        checkBlockPermissionsAndRestrictions(player, corner.getBlock(), tpl.size_x, tpl.size_y, tpl.size_z);
        buildCannonFromTemplate(tpl, corner);
        processCommandSigns(tpl, corner);
        this.hitpoints = maxHitpoints;
        this.owner = CivGlobal.getResident(player);

        this.saveNow();

    }

    protected void checkBlockPermissionsAndRestrictions(Player player, Block centerBlock, int regionX, int regionY, int regionZ) throws CivException {

        if (!War.isWarTime()) {
            throw new CivException(CivSettings.localize.localizedString("buildCannon_NotWar"));
        }

        if (player.getLocation().getY() > maxHeight) {
            throw new CivException(CivSettings.localize.localizedString("cannon_build_tooHigh"));
        }

        if ((regionY + centerBlock.getLocation().getBlockY()) >= 255) {
            throw new CivException(CivSettings.localize.localizedString("cannon_build_overHeightLimit"));
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

                    if (CivGlobal.getProtectedBlock(coord) != null) {
                        throw new CivException(CivSettings.localize.localizedString("cannotBuild_protectedInWay"));
                    }

                    if (CivGlobal.getStructureBlock(coord) != null) {
                        throw new CivException(CivSettings.localize.localizedString("cannotBuild_structureInWay"));
                    }

                    if (CivGlobal.getCampBlock(coord) != null) {
                        throw new CivException(CivSettings.localize.localizedString("cannotBuild_campinWay"));
                    }

                    if (Cannon.cannonBlocks.containsKey(coord)) {
                        throw new CivException(CivSettings.localize.localizedString("cannon_build_cannonInWay"));
                    }

                    yTotal += b.getWorld().getHighestBlockYAt(centerBlock.getX() + x, centerBlock.getZ() + z);
                    yCount++;

                }
            }
        }

        double highestAverageBlock = (double) yTotal / (double) yCount;

        if (((centerBlock.getY() > (highestAverageBlock + 10)) ||
                (centerBlock.getY() < (highestAverageBlock - 10)))) {
            throw new CivException(CivSettings.localize.localizedString("cannotBuild_toofarUnderground"));
        }
    }

    private void updateAngleSign(Block block) {
        Sign sign = (Sign) block.getState();
        sign.setLine(0, "YAW");
        sign.setLine(1, String.valueOf(this.angle));

        double a = this.angle;

        if (a > 0) {
            sign.setLine(2, "==>");
        } else if (a < 0) {
            sign.setLine(2, "<==");
        } else {
            sign.setLine(2, "");
        }

        sign.setLine(3, "");
        sign.update();
    }

    private void updatePowerSign(Block block) {
        Sign sign = (Sign) block.getState();
        sign.setLine(0, "PITCH");
        sign.setLine(1, String.valueOf(this.power));
        sign.setLine(2, "");
        sign.setLine(3, "");
        sign.update();
    }

    private void updateFireSign(Block block) {
        Sign sign = (Sign) block.getState();
        sign.setLine(0, CivSettings.localize.localizedString("cannon_fire"));
        boolean loaded = false;

        if (this.tntLoaded >= tntCost) {
            sign.setLine(1, String.valueOf(ChatColor.AQUA) + ChatColor.BOLD + CivSettings.localize.localizedString("cannon_Loaded"));
            loaded = true;
        } else {
            sign.setLine(1, ChatColor.GOLD + "(" + this.tntLoaded + "/" + tntCost + ") TNT");
        }

        if (this.shotCooldown > 0) {
            sign.setLine(2, ChatColor.LIGHT_PURPLE + CivSettings.localize.localizedString("cannon_cooldownWait") + " " + this.shotCooldown);
        } else {
            if (loaded) {
                sign.setLine(2, ChatColor.AQUA + CivSettings.localize.localizedString("cannon_ready"));
            } else {
                sign.setLine(2, ChatColor.GREEN + CivSettings.localize.localizedString("cannon_addTNT"));
            }
        }

        sign.setLine(3, "");
        sign.update();
    }

    private void processCommandSigns(Template tpl, BlockCoord corner) {
        for (BlockCoord relativeCoord : tpl.commandBlockRelativeLocations) {
            SimpleBlock sb = tpl.blocks[relativeCoord.getX()][relativeCoord.getY()][relativeCoord.getZ()];
            if (!(sb.getMaterialData() instanceof org.bukkit.material.Sign)) {
                continue;
            }
            BlockCoord absCoord = new BlockCoord(corner.getBlock().getRelative(relativeCoord.getX(), relativeCoord.getY(), relativeCoord.getZ()));
            BlockCoord coord = new BlockCoord(absCoord);
            switch (sb.command) {
                case "/fire" -> {
                    this.setFireSignLocation(coord);
                    sb.setTo(coord);
                    updateFireSign(coord.getBlock());
                    Cannon.fireSignLocations.put(coord, this);
                }
                case "/angle" -> {
                    this.setAngleSignLocation(coord);
                    sb.setTo(coord);
                    updateAngleSign(coord.getBlock());
                    Cannon.angleSignLocations.put(coord, this);
                }
                case "/power" -> {
                    this.setPowerSignLocation(coord);
                    sb.setTo(coord);
                    updatePowerSign(coord.getBlock());
                    Cannon.powerSignLocations.put(coord, this);
                }
                case "/cannon" -> {
                    this.cannonLocation = coord.getLocation();
                    switch (((org.bukkit.material.Sign) sb.getMaterialData()).getFacing()) {
                        case EAST -> {
                            cannonLocation.add(1, 0, 0);
                            direction.setX(1.0f);
                            direction.setY(0.0f);
                            direction.setZ(0.0f);
                        }
                        case WEST -> {
                            cannonLocation.add(-1, 0, 0);
                            this.angleFlip = true;
                            direction.setX(-1.0f);
                            direction.setY(0.0f);
                            direction.setZ(0.0f);
                        }
                        case NORTH -> {
                            cannonLocation.add(0, 0, -1);
                            direction.setX(0.0f);
                            direction.setY(0.0f);
                            direction.setZ(-1.0f);
                        }
                        case SOUTH -> {
                            cannonLocation.add(0, 0, 1);
                            this.angleFlip = true;
                            direction.setX(0.0f);
                            direction.setY(0.0f);
                            direction.setZ(1.0f);
                        }
                    }
                    signDirection = ((org.bukkit.material.Sign) sb.getMaterialData()).getFacing();
                }
            }
        }
    }

    @Override
    public void build(Player player, Location centerLoc, Template tpl) {
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

    private void buildCannonFromTemplate(Template tpl, BlockCoord corner) {
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
                            /* Save it as a war block so it's automatically removed when war time ends. */
                            WarRegen.saveBlock(nextBlock, Cannon.RESTORE_NAME, false);
                            tpl.blocks[x][y][z].setTo(nextBlock);
                        }

                        if (nextBlock.getType() != Material.AIR) {
                            BlockCoord b = new BlockCoord(nextBlock.getLocation());
                            cannonBlocks.put(b, this);
                            blocks.add(b);
                        }
                    } catch (Exception e) {
                        CivLog.error(e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    protected Location repositionCenter(Location center, BlockFace dir, double x_size, double z_size) {
        Location loc = center.clone();

        switch (dir) {
            case EAST -> {
                loc.setZ(loc.getZ() - (z_size / 2));
                loc.setX(loc.getX() + SHIFT_OUT);
            }
            case WEST -> {
                loc.setZ(loc.getZ() - (z_size / 2));
                loc.setX(loc.getX() - (SHIFT_OUT + x_size));
            }
            case NORTH -> {
                loc.setX(loc.getX() - (x_size / 2));
                loc.setZ(loc.getZ() - (SHIFT_OUT + z_size));
            }
            case SOUTH -> {
                loc.setX(loc.getX() - (x_size / 2));
                loc.setZ(loc.getZ() + SHIFT_OUT);
            }
        }

        return loc;
    }

    public BlockCoord getFireSignLocation() {
        return fireSignLocation;
    }

    public void setFireSignLocation(BlockCoord fireSignLocation) {
        this.fireSignLocation = fireSignLocation;
    }

    public BlockCoord getAngleSignLocation() {
        return angleSignLocation;
    }

    public void setAngleSignLocation(BlockCoord angleSignLocation) {
        this.angleSignLocation = angleSignLocation;
    }

    public BlockCoord getPowerSignLocation() {
        return powerSignLocation;
    }

    public void setPowerSignLocation(BlockCoord powerSignLocation) {
        this.powerSignLocation = powerSignLocation;
    }

    private void validateUse(Player player) throws CivException {
        if (this.hitpoints == 0) {
            throw new CivException(CivSettings.localize.localizedString("cannon_destroyed"));
        }

        Resident resident = CivGlobal.getResident(player);

        if (resident.getCiv() != owner.getCiv()) {
            throw new CivException(CivSettings.localize.localizedString("cannon_notMember"));
        }
    }

    public void processFire(PlayerInteractEvent event) throws CivException {
        validateUse(event.getPlayer());

        if (this.shotCooldown > 0) {
            CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("cannon_waitForCooldown"));
            return;
        }

        if (this.tntLoaded < getTntCost()) {
            if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
                ItemStack stack = event.getPlayer().getInventory().getItemInMainHand();
                if (stack != null && stack.getType() == Material.TNT) {
                    if (stack.getAmount() >= 2) {
                        stack.setAmount(stack.getAmount() - 1);
                    } else {
                        stack.setType(Material.AIR);
                    }
                    this.tntLoaded++;
                    CivMessage.sendSuccess(event.getPlayer(), CivSettings.localize.localizedString("cannon_addedTNT"));
                    updateFireSign(fireSignLocation.getBlock());
                    return;
                }
                CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("cannon_notLoaded"));
            } else {
                event.setCancelled(true);
            }
            return;
        } else {

            CivMessage.send(event.getPlayer(), CivSettings.localize.localizedString("cannon_fireAway"));
            cannonLocation.setDirection(direction);
            Resident resident = CivGlobal.getResident(event.getPlayer());
            CannonProjectile proj = new CannonProjectile(this, cannonLocation.clone(), resident);
            proj.fire();
            this.tntLoaded = 0;
            this.shotCooldown = getCooldown();

            class SyncTask implements Runnable {
                final Cannon cannon;

                public SyncTask(Cannon cannon) {
                    this.cannon = cannon;
                }

                @Override
                public void run() {
                    if (cannon.decrementCooldown()) {
                        return;
                    }

                    TaskMaster.syncTask(new SyncTask(cannon), TimeTools.toTicks(1));
                }
            }
            TaskMaster.syncTask(new SyncTask(this), TimeTools.toTicks(1));
        }

        updateFireSign(fireSignLocation.getBlock());

    }

    public boolean decrementCooldown() {
        this.shotCooldown--;
        this.updateFireSign(fireSignLocation.getBlock());

        return this.shotCooldown <= 0;
    }

    public void processAngle(PlayerInteractEvent event) throws CivException {
        validateUse(event.getPlayer());

        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            this.angle -= STEP;
            if (this.angle < minAngle) {
                this.angle = minAngle;
            }
        } else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            this.angle += STEP;
            if (this.angle > maxAngle) {
                this.angle = maxAngle;
            }
        }

        double a = this.angleFlip ? -this.angle : this.angle;

        if (signDirection == BlockFace.EAST || signDirection == BlockFace.WEST) {
            direction.setZ(a / 100);
        } else {
            // NORTH/SOUTH
            direction.setX(a / 100);
        }

        updateAngleSign(this.angleSignLocation.getBlock());
    }

    public void processPower(PlayerInteractEvent event) throws CivException {
        validateUse(event.getPlayer());

        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            this.power -= STEP;
            if (this.power < minPower) {
                this.power = minPower;
            }
            if (event.getPlayer().isSneaking()) {
                this.power -= STEP * 10;
            }
        } else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            this.power += STEP;
            if (this.power > maxPower) {
                this.power = maxPower;
            }
            if (event.getPlayer().isSneaking()) {
                this.power += STEP * 10;
            }
        }

        direction.setY(this.power / 100);
        updatePowerSign(this.powerSignLocation.getBlock());
    }

    public void onHit(BlockBreakEvent event) {
        Resident resident = CivGlobal.getResident(event.getPlayer());

        if (!resident.hasTown()) {
            CivMessage.sendError(resident, CivSettings.localize.localizedString("cannon_onHit_NotAtWar"));
            return;
        }

        if (resident.getCiv() == owner.getCiv()) {
            CivMessage.sendError(resident, CivSettings.localize.localizedString("cannon_onHit_ownCannon"));
            return;
        }

        if (!resident.getCiv().getDiplomacyManager().atWarWith(owner.getCiv())) {
            CivMessage.sendError(resident, CivSettings.localize.localizedString("cannon_onHit_NotWarringCiv") + "(" + owner.getCiv().getName() + ")");
            return;
        }

        if (this.hitpoints == 0) {
            CivMessage.sendError(resident, CivSettings.localize.localizedString("cannon_onHit_alreadyDestroyed"));
            return;
        }

        this.hitpoints--;

        if (hitpoints <= 0) {
            destroy();
            CivMessage.send(event.getPlayer(), String.valueOf(ChatColor.GREEN) + ChatColor.BOLD + CivSettings.localize.localizedString("cannon_onHit_Destroyed"));
            CivMessage.sendCiv(owner.getCiv(), ChatColor.YELLOW + CivSettings.localize.localizedString("var_cannon_onHit_DestroyAlert",
                    cannonLocation.getBlockX() + "," + cannonLocation.getBlockY() + "," + cannonLocation.getBlockZ()));
            return;
        }

        CivMessage.send(event.getPlayer(), ChatColor.YELLOW + CivSettings.localize.localizedString("cannon_onHit_doDamage") + " (" + this.hitpoints + "/" + maxHitpoints + ")");
        CivMessage.sendCiv(owner.getCiv(), ChatColor.GRAY + CivSettings.localize.localizedString("var_cannon_onHit_doDamageAlert", hitpoints + "/" + maxHitpoints,
                cannonLocation.getBlockX() + "," + cannonLocation.getBlockY() + "," + cannonLocation.getBlockZ()));
    }

    private void launchExplodeFirework(Location loc) {
        FireworkEffect fe = FireworkEffect.builder().withColor(Color.RED).withColor(Color.ORANGE).flicker(false).with(org.bukkit.FireworkEffect.Type.BALL).build();
        TaskMaster.syncTask(new FireWorkTask(fe, loc.getWorld(), loc, 3), 0);
    }

    private void destroy() {
        for (BlockCoord b : blocks) {
            launchExplodeFirework(b.getCenteredLocation());
            if (b.getBlock().getType().equals(Material.COAL_BLOCK)) {
                BlockState state = b.getBlock().getState();
                state.setType(Material.GRAVEL);
                state.update(true, false);
            } else {
                BlockState state = b.getBlock().getState();
                state.setType(Material.AIR);
                state.update(true, false);
            }
        }

        BlockState state1 = fireSignLocation.getBlock().getState();
        state1.setType(Material.AIR);
        state1.update(true, false);
        BlockState state2 = angleSignLocation.getBlock().getState();
        state2.setType(Material.AIR);
        state2.update(true, false);
        BlockState state3 = powerSignLocation.getBlock().getState();
        state3.setType(Material.AIR);
        state3.update(true, false);

        blocks.clear();
        this.cleanup();
    }

    public int getTntLoaded() {
        return tntLoaded;
    }

    public void setTntLoaded(int tntLoaded) {
        this.tntLoaded = tntLoaded;
    }

    public int getCooldown() {
        int scd = shotCooldown;
        if (owner.getCiv().hasWonder("w_grand_ship_ingermanland")) {
            scd = -10;
        }
        if (owner.getCiv().hasWonder("w_chichen_itza")) {
            scd *= 0.75;
        }
        return Math.max(scd, 1);
    }

    public void setCooldown(int cooldown) {
        this.shotCooldown = cooldown;
    }

    public int getDamage() {
        int bsd = baseStructureDamage;
        if (owner.getTown().getBuffManager().hasBuff("wonder_trade_colossus")) {
            bsd *= 1.0 + owner.getTown().getBuffManager().getEffectiveDouble("wonder_trade_colossus");
        }
        return bsd;
    }


}
