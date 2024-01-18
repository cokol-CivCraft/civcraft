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

import com.avrgaming.civcraft.components.Component;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.config.ConfigTemplate;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.interactive.InteractiveBuildCommand;
import com.avrgaming.civcraft.loregui.GuiActions;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.*;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.structurevalidation.StructureValidator;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.template.Template.TemplateType;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.BuildAsyncTask;
import com.avrgaming.civcraft.threading.tasks.BuildUndoTask;
import com.avrgaming.civcraft.threading.tasks.PostBuildSyncTask;
import com.avrgaming.civcraft.tutorial.CivTutorial;
import com.avrgaming.civcraft.util.*;
import com.avrgaming.civcraft.util.SimpleBlock.Type;
import com.avrgaming.civcraft.war.War;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Buildable extends SQLObject {
    private Town town;
    protected BlockCoord corner;
    public BlockFace dir;
    public ConfigBuildableInfo info = new ConfigBuildableInfo(); //Blank buildable info for buildables which do not have configs.
    protected int hitpoints;

    private int totalBlockCount = 0;
    private boolean complete = false;
    private boolean enabled = true;

    private String templateName;
    private int templateX;
    private int templateY;
    private int templateZ;

    // Number of blocks to shift the structure away from us when built.
    public static final double SHIFT_OUT = 0;
    public static final int MIN_DISTANCE = 7;

    private final Map<BlockCoord, StructureSign> structureSigns = new ConcurrentHashMap<>();
    private final Map<BlockCoord, StructureChest> structureChests = new ConcurrentHashMap<>();

    /* Used to keep track of which blocks belong to this buildable so they can be removed when the buildable is removed. */
    protected Map<BlockCoord, Boolean> structureBlocks = new ConcurrentHashMap<>();
    private BlockCoord centerLocation;

    // XXX this is a bad hack to get the townchunks to load in the proper order when saving asynchronously
    public ArrayList<TownChunk> townChunksToSave = new ArrayList<>();
    public ArrayList<Component> attachedComponents = new ArrayList<>();

    private boolean valid = true;
    public static double validPercentRequirement = 0.8;
    public static HashSet<Buildable> invalidBuildables = new HashSet<>();
    public HashMap<Integer, BuildableLayer> layerValidPercentages = new HashMap<>();
    public boolean validated = false;

    private String invalidReason = "";

    public String invalidLayerMessage = "";

    public Town getTown() {
        return town;
    }

    public void setTown(Town town) {
        this.town = town;
    }

    public boolean isOnWater(Biome b) {
        return switch (b) {
            case BEACHES, COLD_BEACH, STONE_BEACH, RIVER, FROZEN_RIVER, OCEAN, DEEP_OCEAN, FROZEN_OCEAN, SWAMPLAND, MUTATED_SWAMPLAND ->
                    true;
            default -> false;
        };
    }

    public Civilization getCiv() {
        if (this.getTown() == null) {
            return null;
        }
        return this.getTown().getCiv();
    }

    public String getConfigId() {
        return info.id;
    }

    public String getTemplateBaseName() {
        return info.template_base_name;
    }

    public String getDisplayName() {
        return info.displayName;
    }


    public int getMaxHitPoints() {
        return info.max_hp;
    }


    public double getCost() {
        return info.cost;
    }

    public int getRegenRate() {
        return Objects.requireNonNullElse(this.info.regenRate, 0);

    }

    public double getUpkeepCost() {
        return info.upkeep;
    }


    public int getTemplateYShift() {
        return info.templateYShift;
    }


    public String getRequiredUpgrade() {
        return info.require_upgrade;
    }


    public String getRequiredTechnology() {
        return info.require_tech;
    }


    public String getUpdateEvent() {
        return info.update_event;
    }

    public int getPoints() {
        if (info.points != null) {
            return info.points;
        }
        return 0;
    }

    public String getEffectEvent() {
        return info.effect_event;
    }

    public String getOnBuildEvent() {
        return info.onBuild_event;
    }

    public boolean allowDemolish() {
        return info.allow_demolish;
    }

    public boolean isTileImprovement() {
        return info.tile_improvement;
    }

    public boolean isActive() {
        return this.isComplete() && (this instanceof TownHall || !isDestroyed()) && isEnabled();
    }

    public int getTotalBlockCount() {
        return totalBlockCount;
    }

    public void setTotalBlockCount(int totalBlockCount) {
        this.totalBlockCount = totalBlockCount;
    }

    public boolean isDestroyed() {
        return (hitpoints == 0) && (this.getMaxHitPoints() != 0);
    }

    public boolean isDestroyable() {
        return (info.destroyable != null) && (info.destroyable);
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public BlockCoord getCorner() {
        return corner;
    }

    public void setCorner(BlockCoord center) {
        this.corner = center;
    }

    public BlockCoord getCenterLocation() {
        if (this.centerLocation == null) {
            int centerX = getCorner().getX() + (getTemplateX() / 2);
            int centerY = getCorner().getY() + (getTemplateY() / 2);
            int centerZ = getCorner().getZ() + (getTemplateZ() / 2);

            this.centerLocation = new BlockCoord(this.getCorner().getWorldname(), centerX, centerY, centerZ);
        }

        return this.centerLocation;
    }

    public int getHitpoints() {
        return hitpoints;
    }

    public void setHitpoints(int hitpoints) {
        this.hitpoints = hitpoints;
    }

    public void bindStructureBlocks() {
        // Called mostly on a reload, determines which blocks should be protected based on the corner
        // location and the template's size. We need to verify that each block is a part of the template.
        // We might be able to restore broken/missing structures from here in the future.
        if (isDestroyable())
            return;

        Template tpl;
        try {
            tpl = Template.getTemplate(this.getSavedTemplatePath(), null);
        } catch (IOException | CivException e) {
            e.printStackTrace();
            return;
        }

        this.setTemplateX(tpl.size_x);
        this.setTemplateY(tpl.size_y);
        this.setTemplateZ(tpl.size_z);
        this.setCorner(getCorner());

        for (int y = 0; y < this.getTemplateY(); y++) {
            for (int z = 0; z < this.getTemplateZ(); z++) {
                for (int x = 0; x < this.getTemplateX(); x++) {
                    int relx = getCorner().getX() + x;
                    int rely = getCorner().getY() + y;
                    int relz = getCorner().getZ() + z;

                    BlockCoord coord = new BlockCoord(this.getCorner().getWorldname(), (relx), (rely), (relz));

                    if (tpl.blocks[x][y][z].getType() == Material.AIR) {
                        continue;
                    }

                    if (tpl.blocks[x][y][z].specialType == SimpleBlock.Type.COMMAND) {
                        continue;
                    }

                    this.addStructureBlock(coord, y != 0);
                }
            }
        }

        this.save();
    }

    public static void buildPlayerPreview(Player player, ConfigBuildableInfo info, Town town) throws CivException, IOException {

        /* Look for any custom template perks and ask the player if they want to use them. */
        Resident resident = CivGlobal.getResident(player);
        ArrayList<ConfigTemplate> perkList = info.getTemplates();
        if (perkList.isEmpty()) {
            Template tpl = new Template();
            MetaStructure struct = MetaStructure.newStructOrWonder(player.getLocation(), info, town);
            try {
                tpl.initTemplate(struct);
            } catch (CivException | IOException e) {
                e.printStackTrace();
                throw e;
            }

            struct.buildPlayerPreview(player, tpl);
            return;
        }
        /* Store the pending buildable. */
        resident.pendingBuildable = new BuildTaskInstance(info, town);

        /* Build an inventory full of templates to select. */
        Inventory inv = Bukkit.getServer().createInventory(player, CivTutorial.MAX_CHEST_SIZE * 9);
        ItemStack infoRec = LoreGuiItem.build(
                CivSettings.localize.localizedString("buildable_lore_default") + " " + info.displayName,
                Material.WRITTEN_BOOK,
                0,
                ChatColor.GOLD + CivSettings.localize.localizedString("loreGui_template_clickToBuild"));
        infoRec = LoreGuiItem.setAction(infoRec, GuiActions.BuildWithTemplate);
        inv.addItem(infoRec);

        for (ConfigTemplate perk : perkList) {
            infoRec = LoreGuiItem.build(
                    perk.display_name,
                    perk.type_id,
                    perk.data,
                    ChatColor.GOLD + "<Click To Build>"
            );
            infoRec = LoreGuiItem.setAction(infoRec, GuiActions.BuildWithTemplate);
            infoRec = LoreGuiItem.setActionData(infoRec, "theme", perk.theme);
            inv.addItem(infoRec);
        }

        /* We will resume by calling buildPlayerPreview with the template when a gui item is clicked. */
        player.openInventory(inv);


    }


    public void buildPlayerPreview(Player player, Template tpl) throws CivException {
        Location centerLoc = repositionCenter(player.getLocation(), tpl.dir(), tpl.size_x, tpl.size_z);
        tpl.buildPreviewScaffolding(centerLoc, player);

        this.setCorner(new BlockCoord(centerLoc));

        CivMessage.sendHeading(player, CivSettings.localize.localizedString("buildable_preview_heading"));
        CivMessage.send(player, String.valueOf(ChatColor.YELLOW) + ChatColor.BOLD + CivSettings.localize.localizedString("buildable_preview_prompt1"));
        CivMessage.send(player, String.valueOf(ChatColor.GREEN) + ChatColor.BOLD + CivSettings.localize.localizedString("buildable_preview_prompt2"));
        Resident resident = CivGlobal.getResident(player);

        if (!War.isWarTime() && CivSettings.showPreview) {
            resident.startPreviewTask(tpl, centerLoc.getBlock(), player.getUniqueId());
        }

        /* Run validation on position. */
        //validate(player, this, tpl, centerLoc, null);
        this.templateName = tpl.getFilepath();
        TaskMaster.asyncTask(new StructureValidator(player, this), 0);
        resident.setInteractiveMode(new InteractiveBuildCommand(this.getTown(), this, player.getLocation(), tpl));
    }

    /*
     * This function is called before we build structures that do not have a town yet.
     * This includes Capitols, Camps, and Town Halls.
     */

    public static void buildVerifyStatic(Player player, ConfigBuildableInfo info, Location centerLoc, CallbackInterface callback) throws CivException {

        Resident resident = CivGlobal.getResident(player);
        /* Look for any custom template perks and ask the player if they want to use them. */
        ArrayList<ConfigTemplate> perkList = info.getTemplates();
        if (perkList.isEmpty()) {
            String path = Template.getTemplateFilePath(info.template_base_name, TemplateType.STRUCTURE, "default");

            Template tpl;
            try {
                tpl = Template.getTemplate(path, player.getLocation());
            } catch (IOException | CivException e) {
                e.printStackTrace();
                return;
            }

            centerLoc = repositionCenterStatic(centerLoc, info, tpl.dir(), tpl.size_x, tpl.size_z);
            //validate(player, null, tpl, centerLoc, callback);
            TaskMaster.asyncTask(new StructureValidator(player, tpl.getFilepath(), centerLoc, callback), 0);
            return;
        }

        /* Store the pending buildable. */
        resident.pendingBuildableInfo = info;
        resident.pendingCallback = callback;

        /* Build an inventory full of templates to select. */
        Inventory inv = Bukkit.getServer().createInventory(player, CivTutorial.MAX_CHEST_SIZE * 9);
        ItemStack infoRec = LoreGuiItem.build("Default " + info.displayName,
                Material.WRITTEN_BOOK,
                0, ChatColor.GOLD + CivSettings.localize.localizedString("loreGui_template_clickToBuild"));
        infoRec = LoreGuiItem.setAction(infoRec, GuiActions.BuildWithDefaultPersonalTemplate);
        inv.addItem(infoRec);

        for (ConfigTemplate perk : perkList) {
            infoRec = LoreGuiItem.build(perk.display_name,
                    perk.type_id,
                    perk.data,
                    ChatColor.GOLD + CivSettings.localize.localizedString("loreGui_template_clickToBuild")
            );
            infoRec = LoreGuiItem.setAction(infoRec, GuiActions.BuildWithPersonalTemplate);
            infoRec = LoreGuiItem.setActionData(infoRec, "theme", perk.theme);
            inv.addItem(infoRec);
            player.openInventory(inv);
        }
        /* We will resume by calling buildPlayerPreview with the template when a gui item is clicked. */


    }

    public void undoFromTemplate() throws CivException {
        for (BuildAsyncTask task : this.getTown().build_tasks) {
            if (task.buildable == this) {
                task.abort();
            }
        }
        String filepath = "templates/undo/" + this.getTown().getName() + "/" + this.getCorner().toString();
        File f = new File(filepath);
        if (!f.exists()) {
            throw new CivException(CivSettings.localize.localizedString("internalIOException") + " " + CivSettings.localize.localizedString("FileNotFound") + " " + filepath);
        }
        BuildUndoTask task = new BuildUndoTask(filepath, this.getCorner().toString(), this.getCorner(), 0, this.getTown().getName());

        this.town.undo_tasks.add(task);
        BukkitObjects.scheduleAsyncDelayedTask(task, 0);
    }

    public void unbindStructureBlocks() {
        for (BlockCoord coord : this.structureBlocks.keySet()) {
            CivGlobal.removeStructureBlock(coord);
        }
    }

    /*
     * XXX this is called only on structures which do not have towns yet.
     * For Example Capitols, Camps and Town Halls.
     */
    public static Location repositionCenterStatic(Location center, ConfigBuildableInfo info, BlockFace dir, double x_size, double z_size) throws CivException {
        Location loc = new Location(center.getWorld(),
                center.getX(), center.getY(), center.getZ(),
                center.getYaw(), center.getPitch());


        // Reposition tile improvements
        if (info.tile_improvement) {
            // just put the center at 0,0 of this chunk?
            loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
        } else {
            switch (dir) {
                case EAST -> {
                    loc.setZ(loc.getZ() - (z_size / 2));
                    loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
                    loc.setX(loc.getX() + SHIFT_OUT);
                }
                case WEST -> {
                    loc.setZ(loc.getZ() - (z_size / 2));
                    loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
                    loc.setX(loc.getX() - (SHIFT_OUT + x_size));
                }
                case NORTH -> {
                    loc.setX(loc.getX() - (x_size / 2));
                    loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
                    loc.setZ(loc.getZ() - (SHIFT_OUT + z_size));
                }
                case SOUTH -> {
                    loc.setX(loc.getX() - (x_size / 2));
                    loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
                    loc.setZ(loc.getZ() + SHIFT_OUT);
                }
            }
        }
        if (info.templateYShift != 0) {
            // Y-Shift based on the config, this allows templates to be built underground.
            loc.setY(loc.getY() + info.templateYShift);

            if (loc.getY() < 1) {
                throw new CivException(CivSettings.localize.localizedString("buildable_TooCloseToBedrock"));
            }
        }

        return loc;
    }

    protected Location repositionCenter(Location center, BlockFace dir, double x_size, double z_size) throws CivException {
        Location loc = new Location(center.getWorld(),
                center.getX(), center.getY(), center.getZ(),
                center.getYaw(), center.getPitch());


        // Reposition tile improvements
        if (this.isTileImprovement()) {
            // just put the center at 0,0 of this chunk?
            loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
        } else {
            switch (dir) {
                case EAST -> {
                    loc.setZ(loc.getZ() - (z_size / 2));
                    loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
                    loc.setX(loc.getX() + SHIFT_OUT);
                }
                case WEST -> {
                    loc.setZ(loc.getZ() - (z_size / 2));
                    loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
                    loc.setX(loc.getX() - (SHIFT_OUT + x_size));
                }
                case NORTH -> {
                    loc.setX(loc.getX() - (x_size / 2));
                    loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
                    loc.setZ(loc.getZ() - (SHIFT_OUT + z_size));
                }
                case SOUTH -> {
                    loc.setX(loc.getX() - (x_size / 2));
                    loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
                    loc.setZ(loc.getZ() + SHIFT_OUT);
                }
            }
        }
        if (this.getTemplateYShift() != 0) {
            // Y-Shift based on the config, this allows templates to be built underground.
            loc.setY(loc.getY() + this.getTemplateYShift());

            if (loc.getY() < 1) {
                throw new CivException(CivSettings.localize.localizedString("buildable_TooCloseToBedrock"));
            }
        }

        return loc;
    }

    public static void validateDistanceFromSpawn(Location loc) throws CivException {
        /* Check distance from spawn. */
        double requiredDistance = CivSettings.civConfig.getDouble("global.distance_from_spawn", 1000.0);

        for (Civilization civ : CivGlobal.getAdminCivs()) {
            Location townHallLoc = civ.getCapitolTownHallLocation();
            if (townHallLoc == null) {
                continue;
            }

            double distance = townHallLoc.distance(loc);
            if (distance < requiredDistance) {
                throw new CivException(CivSettings.localize.localizedString("var_buildable_toocloseToSpawn1", requiredDistance));
            }

        }
    }

    public void onCheck() throws CivException {
        /* Override in children */
    }


    public synchronized void buildRepairTemplate(Template tpl, Block centerBlock) {
        for (int x = 0; x < tpl.size_x; x++) {
            for (int y = 0; y < tpl.size_y; y++) {
                for (int z = 0; z < tpl.size_z; z++) {
                    Block b = centerBlock.getRelative(x, y, z);

                    if (tpl.blocks[x][y][z].specialType == Type.COMMAND) {
                        BlockState state = b.getState();
                        state.setType(Material.AIR);
                        state.update(true, false);
                    } else {
                        tpl.blocks[x][y][z].setTo(b);
                    }

                    if (b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN_POST) {
                        Sign s2 = (Sign) b.getState();
                        s2.setLine(0, tpl.blocks[x][y][z].message[0]);
                        s2.setLine(1, tpl.blocks[x][y][z].message[1]);
                        s2.setLine(2, tpl.blocks[x][y][z].message[2]);
                        s2.setLine(3, tpl.blocks[x][y][z].message[3]);
                        s2.update();
                    }
                }
            }
        }
    }


    public int getTemplateX() {
        return templateX;
    }

    public void setTemplateX(int templateX) {
        this.templateX = templateX;
    }

    public int getTemplateY() {
        return templateY;
    }

    public void setTemplateY(int templateY) {
        this.templateY = templateY;
    }

    public int getTemplateZ() {
        return templateZ;
    }

    public void setTemplateZ(int templateZ) {
        this.templateZ = templateZ;
    }

    public void addStructureSign(StructureSign s) {
        this.structureSigns.put(s.getCoord(), s);
    }

    public Collection<StructureSign> getSigns() {
        return this.structureSigns.values();
    }

    public StructureSign getSign(BlockCoord coord) {
        return this.structureSigns.get(coord);
    }

    public void processSignAction(Player player, StructureSign sign, PlayerInteractEvent event) throws CivException {
        CivLog.info("No Sign action for this buildable?:" + this.getDisplayName());
    }

    public String getSavedTemplatePath() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public void addStructureChest(StructureChest chest) {
        this.structureChests.put(chest.getCoord(), chest);
    }

    public ArrayList<StructureChest> getAllChestsById(int id) {
        ArrayList<StructureChest> chests = new ArrayList<>();

        for (StructureChest chest : this.structureChests.values()) {
            if (chest.getChestId() == id) {
                chests.add(chest);
            }
        }

        return chests;
    }

    public Collection<StructureChest> getChests() {
        return this.structureChests.values();
    }

    public void addStructureBlock(BlockCoord coord, boolean damageable) {
        //CivLog.debug("Added structure block:"+this);
        CivGlobal.addStructureBlock(coord, this, damageable);

        // all we really need is it's key, we'll put in true
        // to make sure this structureBlocks collection isnt
        // abused.
        this.structureBlocks.put(coord, true);

    }


    /* SessionDB helpers */
    public void sessionAdd(String key, String value) {
        CivGlobal.getSessionDB().add(key, value, this.getCiv().getUUID(), this.getTown().getUUID(), this.getUUID());
    }

    /*
     * Damages this structure by this amount. If hitpoints go to 0, structure is demolished.
     */
    public int getHitPoints() {
        return hitpoints;
    }

    public int getDamagePercentage() {
        return (int) ((double) hitpoints / (double) this.getMaxHitPoints() * 100);
    }

    public void damage(int amount) {
        if (hitpoints == 0)
            return;
        hitpoints -= amount;

        if (hitpoints <= 0) {
            hitpoints = 0;
            onDestroy();
        }
    }

    public void onDestroy() {
        //can be overriden in subclasses.
        CivMessage.global(CivSettings.localize.localizedString("var_buildable_destroyedAlert", this.getDisplayName(), this.getTown().getName()));
        this.hitpoints = 0;
        this.fancyDestroyStructureBlocks();
        this.save();
    }

    public void onDamage(int amount, World world, Player player, BlockCoord coord, BuildableDamageBlock hit) {
        if (!this.getCiv().getDiplomacyManager().isAtWar()) {
            return;
        }
        boolean wasTenPercent = false;
        if (hit.getOwner().isDestroyed()) {
            if (player != null) {
                CivMessage.sendError(player, CivSettings.localize.localizedString("var_buildable_alreadyDestroyed", hit.getOwner().getDisplayName()));
            }
            return;
        }

        if (!hit.getOwner().isComplete() && !(hit.getOwner() instanceof Wonder)) {
            if (player != null) {
                CivMessage.sendError(player, CivSettings.localize.localizedString("var_buildable_underConstruction", hit.getOwner().getDisplayName()));
            }
            return;
        }

        if ((hit.getOwner().getDamagePercentage() % 10) == 0) {
            wasTenPercent = true;
        }

        this.damage(amount);

        world.playSound(hit.getCoord().getLocation(), Sound.BLOCK_ANVIL_USE, 0.2f, 1);
        world.playEffect(hit.getCoord().getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
        if (player != null) {
            CivMessage.sendActionBar(player, CivData.getStringForBar(CivData.TaskType.STRUCTURE, this.getHitPoints(), this.getMaxHitPoints()));
        }

        if ((hit.getOwner().getDamagePercentage() % 10) == 0 && !wasTenPercent) {
            if (player != null) {
                onDamageNotification(player, hit);
            }
        }

        if (player != null) {
            Resident resident = CivGlobal.getResident(player);
            if (resident.isCombatInfo()) {
                CivMessage.send(player, ChatColor.GRAY + CivSettings.localize.localizedString("var_buildable_OnDamageSuccess", hit.getOwner().getDisplayName(), (hit.getOwner().hitpoints + "/" + hit.getOwner().getMaxHitPoints())));
            }
        }

    }

    public void onDamageNotification(Player player, BuildableDamageBlock hit) {
        CivMessage.send(player, ChatColor.GRAY + CivSettings.localize.localizedString("var_buildable_OnDamageSuccess", hit.getOwner().getDisplayName(), (hit.getOwner().getDamagePercentage() + "%")));

        CivMessage.sendTown(hit.getTown(), ChatColor.YELLOW + CivSettings.localize.localizedString("var_buildable_underAttackAlert", hit.getOwner().getDisplayName(), hit.getOwner().getCorner(), hit.getOwner().getDamagePercentage()));
    }

    public Map<BlockCoord, Boolean> getStructureBlocks() {
        return this.structureBlocks;
    }


    public boolean isAvailable() {
        return info.isAvailable(this.getTown());
    }

    public void fancyDestroyStructureBlocks() {
        TaskMaster.syncTask(() -> {
            for (BlockCoord coord : structureBlocks.keySet()) {

                if (CivGlobal.getStructureChest(coord) != null) {
                    continue;
                }

                if (CivGlobal.getStructureSign(coord) != null) {
                    continue;
                }
                switch (coord.getBlock().getType()) {
                    case AIR, CHEST, SIGN_POST, WALL_SIGN -> {
                        continue;
                    }
                }

                if (CivSettings.alwaysCrumble.contains(coord.getBlock().getType())) {
                    BlockState block = coord.getBlock().getState();
                    block.setType(Material.COBBLESTONE);
                    block.update(true, false);
                    continue;
                }

                Random rand = new Random();

                // Each block has a 70% chance to turn into Air
                if (rand.nextInt(100) <= 70) {
                    BlockState block = coord.getBlock().getState();
                    block.setType(Material.AIR);
                    block.update(true, false);
                    continue;
                }

                // Each block has a 30% chance to turn into gravel
                if (rand.nextInt(100) <= 30) {
                    BlockState block = coord.getBlock().getState();
                    block.setType(Material.GRAVEL);
                    block.update(true, false);
                    continue;
                }


                // Each block has a 10% chance of starting a fire
                if (rand.nextInt(100) <= 10) {
                    BlockState block = coord.getBlock().getState();
                    block.setType(Material.FIRE);
                    block.update(true, false);
                    continue;
                }

                // Each block has a 1% chance of launching an explosion effect
                if (rand.nextInt(100) <= 1) {
                    FireworkEffect effect = FireworkEffect.builder().with(FireworkEffect.Type.BURST).withColor(Color.ORANGE).withColor(Color.RED).withTrail().withFlicker().build();
                    FireworkEffectPlayer fePlayer = new FireworkEffectPlayer();
                    for (int i = 0; i < 3; i++) {
                        try {
                            fePlayer.playFirework(coord.getBlock().getWorld(), coord.getLocation(), effect);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    public void onPostBuild(BlockCoord absCoord, SimpleBlock commandBlock) {
    }

    public void processRegen() {
        if (this.validated && !this.isValid()) {
            /* Do not regen invalid structures. */
            return;
        }

        int regenRate = this.getRegenRate() + this.getTown().getBuffManager().getEffectiveInt("buff_chichen_itza_regen_rate");

        if (regenRate != 0) {
            if ((this.getHitpoints() != this.getMaxHitPoints()) &&
                    (this.getHitpoints() != 0)) {
                this.setHitpoints(this.getHitpoints() + regenRate);

                if (this.getHitpoints() > this.getMaxHitPoints()) {
                    this.setHitpoints(this.getMaxHitPoints());
                }
            }
        }
    }

    /*
     * Plays a fire effect on all of the structure blocks for this structure.
     */
    public void flashStructureBlocks() {
        World world = structureBlocks.keySet().iterator().next().getLocation().getWorld();
        for (BlockCoord coord : structureBlocks.keySet()) {
            world.playEffect(coord.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
        }
    }

    public void updateSignText() {
    }

    public void repairFromTemplate() throws IOException, CivException {
        Template tpl = Template.getTemplate(this.getSavedTemplatePath(), this.corner.getCenteredLocation());
        this.buildRepairTemplate(tpl, this.getCorner().getBlock());
        TaskMaster.syncTask(new PostBuildSyncTask(tpl, this));
    }

    public boolean isValid() {
        return valid;
    }

    public String getInvalidReason() {
        return invalidReason;
    }

    public void setInvalidReason(String invalidReason) {
        this.invalidReason = invalidReason;
    }

    public static Material getBlockIDFromSnapshotMap(HashMap<ChunkCoord, ChunkSnapshot> snapshots, int absX, int absY, int absZ, String worldName) throws CivException {

        int chunkX = ChunkCoord.castToChunkX(absX);
        int chunkZ = ChunkCoord.castToChunkZ(absZ);
        ChunkSnapshot snapshot = snapshots.get(new ChunkCoord(worldName, chunkX, chunkZ));
        if (snapshot == null) {
            throw new CivException("Snapshot for chunk " + chunkX + ", " + chunkZ + " in " + worldName + " not found for abs:" + absX + "," + absZ);
        }

        int blockChunkX = absX % 16;
        int blockChunkZ = absZ % 16;

        if (blockChunkX < 0) {
            blockChunkX += 16;
        }

        if (blockChunkZ < 0) {
            blockChunkZ += 16;
        }

        return snapshot.getBlockType(blockChunkX, absY, blockChunkZ);
    }

    public static double getReinforcementRequirementForLevel(int level) {
        if (level > 10) {
            return Buildable.validPercentRequirement * 0.3;
        }

        return Buildable.validPercentRequirement;
    }


    public boolean isIgnoreFloating() {
        return info.ignore_floating;
    }

    //public static ReentrantLock validateLock = new ReentrantLock();
    public void validate(Player player) {
        TaskMaster.asyncTask(new StructureValidator(player, this), 0);
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    @Override
    public void delete() throws SQLException {
        this.setEnabled(false);
        for (Component comp : this.attachedComponents) {
            comp.destroyComponent();
        }
    }

    protected List<HashMap<String, String>> getComponentInfoList() {
        return info.components;
    }

    public Component getComponent(String name) {

        for (Component comp : this.attachedComponents) {
            if (comp.getName().equals(name)) {
                return comp;
            }
        }
        return null;
    }

    public void loadSettings() {
    }

    public static double getReinforcementValue(Material material) {
        return switch (material) {
            case STATIONARY_WATER, WATER, STATIONARY_LAVA, LAVA, AIR, WEB -> 0;
            case IRON_BLOCK, EMERALD_BLOCK, DIAMOND_BLOCK, GOLD_BLOCK, LAPIS_BLOCK -> 4;
            case SMOOTH_BRICK -> 3;
            case STONE, COAL_BLOCK, REDSTONE_BLOCK, NETHER_WART_BLOCK -> 2;
            case GRAVEL -> 1.25;
            case OBSIDIAN -> 8;
            case SPONGE -> 2.75;
            default -> 1;
        };
    }

    public boolean canRestoreFromTemplate() {
        return true;
    }


    public void onInvalidPunish() {
        BlockCoord center = this.getCenterLocation();

        int damage = (int) (this.getMaxHitPoints() * CivSettings.warConfig.getDouble("war.invalid_hourly_penalty", 0.1));
        if (damage <= 0) {
            damage = 10;
        }

        this.damage(damage);

        DecimalFormat df = new DecimalFormat("###");
        CivMessage.sendTown(this.getTown(), ChatColor.RED + CivSettings.localize.localizedString("var_buildable_cannotSupport", this.getDisplayName(), (center.getX() + "," + center.getY() + "," + center.getZ())));
        CivMessage.sendTown(this.getTown(), ChatColor.RED + CivSettings.localize.localizedString("var_buildable_cannotSupportDamage", df.format(CivSettings.warConfig.getDouble("war.invalid_hourly_penalty", 0.1) * 100), (this.hitpoints + "/" + this.getMaxHitPoints())));
        CivMessage.sendTown(this.getTown(), ChatColor.RED + this.invalidLayerMessage);
        CivMessage.sendTown(this.getTown(), ChatColor.RED + CivSettings.localize.localizedString("buildable_validationPrompt"));
        this.save();

    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public record BuildTaskInstance(ConfigBuildableInfo info, Town town) {
    }
}
