/*
 * AVRGAMING LLC
 * __________________
 *
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,TICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.template;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.threading.sync.SyncBuildUpdateTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.EnumBlockRotation;
import net.minecraft.server.v1_12_R1.IBlockData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import static java.lang.Math.abs;

public class Template {
    /* Handles the processing of CivTemplates which store cubiods of blocks for later use. */
    public enum TemplateType {
        STRUCTURE,
        WONDER,
    }

    public SimpleBlock[][][] blocks;
    public int size_x;
    public int size_y;
    public int size_z;
    private String strTheme;
    private BlockFace dir;
    private String filepath;
    private final Queue<SimpleBlock> sbs; //Blocks to add to main sync task queue;

    /* Save the command block locations when we init the template, so we dont have to search for them later. */
    public ArrayList<BlockCoord> commandBlockRelativeLocations = new ArrayList<>();
    public LinkedList<BlockCoord> doorRelativeLocations = new LinkedList<>();
    public LinkedList<BlockCoord> attachableLocations = new LinkedList<>();
    public static HashSet<Material> attachableTypes = new HashSet<>();

    public static void initAttachableTypes() {
        attachableTypes.add(Material.SAPLING);
        attachableTypes.add(Material.BED);
        attachableTypes.add(Material.BED_BLOCK);
        attachableTypes.add(Material.POWERED_RAIL);
        attachableTypes.add(Material.DETECTOR_RAIL);
        attachableTypes.add(Material.LONG_GRASS);
        attachableTypes.add(Material.DEAD_BUSH);
        attachableTypes.add(Material.YELLOW_FLOWER);
        attachableTypes.add(Material.RED_ROSE);
        attachableTypes.add(Material.BROWN_MUSHROOM);
        attachableTypes.add(Material.RED_MUSHROOM);
        attachableTypes.add(Material.TORCH);
        attachableTypes.add(Material.REDSTONE_WIRE);
        attachableTypes.add(Material.WHEAT);
        attachableTypes.add(Material.LADDER);
        attachableTypes.add(Material.RAILS);
        attachableTypes.add(Material.LEVER);
        attachableTypes.add(Material.STONE_PLATE);
        attachableTypes.add(Material.WOOD_PLATE);
        attachableTypes.add(Material.REDSTONE_TORCH_ON);
        attachableTypes.add(Material.REDSTONE_TORCH_OFF);
        attachableTypes.add(Material.STONE_BUTTON);
        attachableTypes.add(Material.CACTUS);
        attachableTypes.add(Material.SUGAR_CANE);
        attachableTypes.add(Material.DIODE_BLOCK_OFF);
        attachableTypes.add(Material.DIODE_BLOCK_ON);
        attachableTypes.add(Material.TRAP_DOOR);
        attachableTypes.add(Material.PUMPKIN_STEM);
        attachableTypes.add(Material.MELON_STEM);
        attachableTypes.add(Material.VINE);
        attachableTypes.add(Material.WATER_LILY);
        attachableTypes.add(Material.BREWING_STAND);
        attachableTypes.add(Material.COCOA);
        attachableTypes.add(Material.TRIPWIRE);
        attachableTypes.add(Material.TRIPWIRE_HOOK);
        attachableTypes.add(Material.FLOWER_POT);
        attachableTypes.add(Material.CARROT);
        attachableTypes.add(Material.POTATO);
        attachableTypes.add(Material.WOOD_BUTTON);
        attachableTypes.add(Material.ANVIL);
        attachableTypes.add(Material.GOLD_PLATE);
        attachableTypes.add(Material.IRON_PLATE);
        attachableTypes.add(Material.REDSTONE_COMPARATOR_ON);
        attachableTypes.add(Material.REDSTONE_COMPARATOR_OFF);
        attachableTypes.add(Material.DAYLIGHT_DETECTOR);
        attachableTypes.add(Material.ACTIVATOR_RAIL);
    }

    public static boolean isAttachable(Material blockID) {
        return attachableTypes.contains(blockID);
    }


    public Template() {
        sbs = new LinkedList<>();
    }

    public void updateBlocksQueue(Queue<SimpleBlock> sbs) {
        SyncBuildUpdateTask.queueSimpleBlock(sbs);
    }

    public static String getTemplateFilePath(Buildable buildable, String theme) {
        TemplateType type = buildable instanceof Wonder ? TemplateType.WONDER : TemplateType.STRUCTURE;
        return Template.getTemplateFilePath(buildable.getTemplateBaseName(), type, theme);
    }

    public static String getTemplateFilePath(String template_file, TemplateType type, String theme) {
        return switch (type) {
            case STRUCTURE -> getStructureFilePath(template_file, theme);
            case WONDER -> getWonderFilePath(template_file);
        };
    }

    public static String getStructureFilePath(String template_file, String theme) {
        return ("templates/themes/" + theme + "/" + template_file.replaceAll(" ", "_") + ".def").toLowerCase();
    }

    public static String getWonderFilePath(String template_file) {
        return ("templates/wonders/" + template_file.replaceAll(" ", "_") + ".def").toLowerCase();
    }

    public static final MaterialData SCAFFOLDING_BLOCK = new MaterialData(Material.BEDROCK);


    public void buildPreviewScaffolding(Location center, Player player) {
        Resident resident = CivGlobal.getResident(player);

        resident.undoPreview();

        for (int y = 0; y < this.size_y; y++) {
            Block b = center.getBlock().getRelative(0, y, 0);
            ItemManager.sendBlockChange(player, b.getLocation(), SCAFFOLDING_BLOCK);
            resident.previewUndo.put(new BlockCoord(b.getLocation()), new SimpleBlock(b));

            b = center.getBlock().getRelative(this.size_x - 1, y, this.size_z - 1);
            ItemManager.sendBlockChange(player, b.getLocation(), SCAFFOLDING_BLOCK);
            resident.previewUndo.put(new BlockCoord(b.getLocation()), new SimpleBlock(b));

            b = center.getBlock().getRelative(this.size_x - 1, y, 0);
            ItemManager.sendBlockChange(player, b.getLocation(), SCAFFOLDING_BLOCK);
            resident.previewUndo.put(new BlockCoord(b.getLocation()), new SimpleBlock(b));

            b = center.getBlock().getRelative(0, y, this.size_z - 1);
            ItemManager.sendBlockChange(player, b.getLocation(), SCAFFOLDING_BLOCK);
            resident.previewUndo.put(new BlockCoord(b.getLocation()), new SimpleBlock(b));

        }

        for (int x = 0; x < this.size_x; x++) {
            Block b = center.getBlock().getRelative(x, this.size_y - 1, 0);
            ItemManager.sendBlockChange(player, b.getLocation(), SCAFFOLDING_BLOCK);
            resident.previewUndo.put(new BlockCoord(b.getLocation()), new SimpleBlock(b));

            b = center.getBlock().getRelative(x, this.size_y - 1, this.size_z - 1);
            ItemManager.sendBlockChange(player, b.getLocation(), SCAFFOLDING_BLOCK);
            resident.previewUndo.put(new BlockCoord(b.getLocation()), new SimpleBlock(b));

        }

        for (int z = 0; z < this.size_z; z++) {
            Block b = center.getBlock().getRelative(0, this.size_y - 1, z);
            ItemManager.sendBlockChange(player, b.getLocation(), SCAFFOLDING_BLOCK);
            resident.previewUndo.put(new BlockCoord(b.getLocation()), new SimpleBlock(b));

            b = center.getBlock().getRelative(this.size_x - 1, this.size_y - 1, z);
            ItemManager.sendBlockChange(player, b.getLocation(), SCAFFOLDING_BLOCK);
            resident.previewUndo.put(new BlockCoord(b.getLocation()), new SimpleBlock(b));
        }

//        for (int z = 0; z < this.size_z; z++) {
//            for (int x = 0; x < this.size_x; x++) {
//                Block b = center.getBlock().getRelative(x, 0, z);
//                ItemManager.sendBlockChange(player, b.getLocation(), Material.BEDROCK, 0);
//                resident.previewUndo.put(new BlockCoord(b.getLocation()), new SimpleBlock(b.getType(), b.getData()));
//            }
//        }
    }

    public void buildScaffolding(Location center) {


        for (int y = 0; y < this.size_y; y++) {
            Block b = center.getBlock().getRelative(0, y, 0);
            b.getState().setData(SCAFFOLDING_BLOCK);

            b = center.getBlock().getRelative(this.size_x - 1, y, this.size_z - 1);
            b.getState().setData(SCAFFOLDING_BLOCK);

            b = center.getBlock().getRelative(this.size_x - 1, y, 0);
            b.getState().setData(SCAFFOLDING_BLOCK);

            b = center.getBlock().getRelative(0, y, this.size_z - 1);
            b.getState().setData(SCAFFOLDING_BLOCK);
        }

        for (int x = 0; x < this.size_x; x++) {
            Block b = center.getBlock().getRelative(x, this.size_y - 1, 0);
            b.getState().setData(SCAFFOLDING_BLOCK);

            b = center.getBlock().getRelative(x, this.size_y - 1, this.size_z - 1);
            b.getState().setData(SCAFFOLDING_BLOCK);
        }

        for (int z = 0; z < this.size_z; z++) {
            Block b = center.getBlock().getRelative(0, this.size_y - 1, z);
            b.getState().setData(SCAFFOLDING_BLOCK);

            b = center.getBlock().getRelative(this.size_x - 1, this.size_y - 1, z);
            b.getState().setData(SCAFFOLDING_BLOCK);
        }

        for (int z = 0; z < this.size_z; z++) {
            for (int x = 0; x < this.size_x; x++) {
                Block b = center.getBlock().getRelative(x, 0, z);
                b.getState().setData(SCAFFOLDING_BLOCK);
            }
        }

    }

    public void removeScaffolding(Location center) {
        for (int y = 0; y < this.size_y; y++) {
            Block b = center.getBlock().getRelative(0, y, 0);
            if (b.getState().getData() == SCAFFOLDING_BLOCK) {
                b.getState().setData(new MaterialData(Material.AIR));
            }

            b = center.getBlock().getRelative(this.size_x - 1, y, this.size_z - 1);
            if (b.getState().getData() == SCAFFOLDING_BLOCK) {
                b.getState().setData(new MaterialData(Material.AIR));
            }

            b = center.getBlock().getRelative(this.size_x - 1, y, 0);
            if (b.getState().getData() == SCAFFOLDING_BLOCK) {
                b.getState().setData(new MaterialData(Material.AIR));

            }

            b = center.getBlock().getRelative(0, y, this.size_z - 1);
            if (b.getState().getData() == SCAFFOLDING_BLOCK) {
                b.getState().setData(new MaterialData(Material.AIR));

            }
        }

        for (int x = 0; x < this.size_x; x++) {
            Block b = center.getBlock().getRelative(x, this.size_y - 1, 0);
            if (b.getState().getData() == SCAFFOLDING_BLOCK) {
                b.getState().setData(new MaterialData(Material.AIR));
            }

            b = center.getBlock().getRelative(x, this.size_y - 1, this.size_z - 1);
            if (b.getState().getData() == SCAFFOLDING_BLOCK) {
                b.getState().setData(new MaterialData(Material.AIR));
            }

        }

        for (int z = 0; z < this.size_z; z++) {
            Block b = center.getBlock().getRelative(0, this.size_y - 1, z);
            if (b.getState().getData() == SCAFFOLDING_BLOCK) {
                b.getState().setData(new MaterialData(Material.AIR));
            }

            b = center.getBlock().getRelative(this.size_x - 1, this.size_y - 1, z);
            if (b.getState().getData() == SCAFFOLDING_BLOCK) {
                b.getState().setData(new MaterialData(Material.AIR));
            }

        }

    }

    @SuppressWarnings("deprecation")
    public void saveUndoTemplate(String string, String subdir, Location center) throws IOException {

        String filepath = "templates/undo/" + subdir;
        File undo_tpl_file = new File(filepath);
        if (undo_tpl_file.mkdirs()) {
            CivLog.debug("Created " + filepath);
        }

        FileWriter writer = new FileWriter(undo_tpl_file.getAbsolutePath() + "/" + string);
        writer.write(this.size_x + ";" + this.size_y + ";" + this.size_z + "\n");
        for (int x = 0; x < this.size_x; x++) {
            for (int y = 0; y < this.size_y; y++) {
                for (int z = 0; z < this.size_z; z++) {
                    Block b = center.getBlock().getRelative(x, y, z);
                    writer.write(x + ":" + y + ":" + z + "," + b.getTypeId() + ":" + b.getData());
                    if (b.getState() instanceof Sign) {
                        writer.write("," + String.join(",", ((Sign) b.getState()).getLines()));
                    }
                    writer.write("\n");


                }
            }
        }
        writer.close();

        for (int x = 0; x < this.size_x; x++) {
            for (int y = 0; y < this.size_y; y++) {
                for (int z = 0; z < this.size_z; z++) {
                    // Must set to air in a different loop, since setting to air can break attachables.
                    center.getBlock().getRelative(x, y, z).getState().setData(new MaterialData(Material.AIR));
                }
            }
        }
    }

    public void initUndoTemplate(String structureHash, String subdir) throws IOException, CivException {
        String filepath = "templates/undo/" + subdir + "/" + structureHash;

        BufferedReader reader = new BufferedReader(new FileReader(filepath));

        String line = reader.readLine();
        if (line == null) {
            reader.close();
            throw new CivException(CivSettings.localize.localizedString("template_invalidFile") + " " + filepath);
        }

        String[] split = line.split(";");
        size_x = Integer.parseInt(split[0]);
        size_y = Integer.parseInt(split[1]);
        size_z = Integer.parseInt(split[2]);
        getTemplateBlocks(reader, size_x, size_y, size_z);
        reader.close();
    }

    public void setDirection(Location center) {
        setDirection(getDirection(center));
    }

    public void setDirection(BlockFace face) {
        if (face == null) {
            dir = BlockFace.SOUTH;
            return;
        }
        dir = switch (face) {
            case EAST -> BlockFace.EAST;
            case WEST -> BlockFace.WEST;
            case NORTH -> BlockFace.NORTH;
            default -> BlockFace.SOUTH;
        };
    }

    public static BlockFace getDirection(Location center) {
        return parseDirection(center).getOppositeFace();
    }

    public void resumeTemplate(String templatePath, Buildable buildable) throws IOException, CivException {
        this.setFilepath(templatePath);
        load_template(templatePath);
        buildable.setTotalBlockCount(size_x * size_y * size_z);
    }

    public void initTemplate(Location center, ConfigBuildableInfo info, String theme) throws CivException, IOException {
        this.setDirection(center);
        String templatePath = Template.getTemplateFilePath(info.template_base_name, TemplateType.STRUCTURE, theme);
        this.setFilepath(templatePath);
        load_template(templatePath);
    }

    public void initTemplate(Buildable buildable, String theme) throws IOException, CivException {

        setDirection(buildable.dir);

        // Find the template file.
        this.setTheme(theme);
        String templatePath = Template.getTemplateFilePath(buildable, theme);
        this.setFilepath(templatePath);
        load_template(templatePath);
        buildable.setTotalBlockCount(size_x * size_y * size_z);
    }

    public void initTemplate(Buildable buildable) throws CivException, IOException {
        initTemplate(buildable, "default");
    }

    public static Template getTemplate(String filepath, Location dirLoc) throws IOException, CivException {
        Template tpl = new Template();
        tpl.load_template(filepath);

        if (dirLoc != null) {
            tpl.setDirection(dirLoc);
        } else {
            tpl.setDirection(BlockFace.SOUTH);
        }
        return tpl;
    }

    private EnumBlockRotation getRotation() {
        if (dir == null) {
            return EnumBlockRotation.NONE;
        }
        return switch (dir) {
            case EAST -> EnumBlockRotation.COUNTERCLOCKWISE_90;
            case NORTH -> EnumBlockRotation.CLOCKWISE_180;
            case WEST -> EnumBlockRotation.CLOCKWISE_90;
            default -> EnumBlockRotation.NONE;
        };
    }

    public void load_template(String filepath) throws IOException, CivException {
        File templateFile = new File(filepath);
        BufferedReader reader = new BufferedReader(new FileReader(templateFile));

        // Read first line and get size.
        String line = reader.readLine();
        if (line == null) {
            reader.close();
            throw new CivException(CivSettings.localize.localizedString("template_invalidFile") + " " + filepath);
        }

        String[] split = line.split(";");
        BlockPosition size = new BlockPosition(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2])).a(getRotation());
        size_x = abs(size.getX());
        size_y = abs(size.getY());
        size_z = abs(size.getZ());
        getTemplateBlocks(reader, size_x, size_y, size_z);
        this.filepath = filepath;
        reader.close();
    }

    @SuppressWarnings("deprecation")
    private void getTemplateBlocks(BufferedReader reader, int regionX, int regionY, int regionZ) throws NumberFormatException, IOException {

        SimpleBlock[][][] blocks = new SimpleBlock[regionX][regionY][regionZ];

        // Read blocks from file.
        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;

            String[] locTypeSplit = line.split(",");
            String location = locTypeSplit[0];
            String type = locTypeSplit[1];

            //Parse location
            String[] locationSplit = location.split(":");

            BlockPosition pos = new BlockPosition(
                    Integer.parseInt(locationSplit[0]),
                    Integer.parseInt(locationSplit[1]),
                    Integer.parseInt(locationSplit[2])
            ).a(getRotation());
            BlockPosition correction = new BlockPosition(1, 0, 1).a(getRotation());
            int blockX = pos.getX() - (correction.getX() - 1) / 2 * (regionX - 1);
            int blockY = pos.getY();
            int blockZ = pos.getZ() - (correction.getZ() - 1) / 2 * (regionZ - 1);

            // Parse type
            String[] typeSplit = type.split(":");

            net.minecraft.server.v1_12_R1.Block var1 = net.minecraft.server.v1_12_R1.Block.REGISTRY.getId(Integer.parseInt(typeSplit[0]));
            IBlockData var2 = var1.fromLegacyData(Integer.parseInt(typeSplit[1])).a(getRotation());

            SimpleBlock block = new SimpleBlock(Material.getMaterial(Integer.parseInt(typeSplit[0])), var2.getBlock().toLegacyData(var2));


            if (CivData.isDoor(block.getType())) {
                this.doorRelativeLocations.add(new BlockCoord("", blockX, blockY, blockZ));
            }

            // look for signs.
            if (block.getType().getData() == Material.WALL_SIGN.getData() && locTypeSplit.length > 2) {

                // The first character on special signs needs to be a /.
                if (locTypeSplit[2] != null && !locTypeSplit[2].isEmpty() && locTypeSplit[2].charAt(0) == '/') {
                    block.specialType = SimpleBlock.Type.COMMAND;

                    // Got a command, save it.
                    block.command = locTypeSplit[2];

                    // Save any key values we find.
                    if (locTypeSplit.length > 3) {
                        for (int i = 3; i < locTypeSplit.length; i++) {
                            if (locTypeSplit[i] == null || locTypeSplit[i].isEmpty()) {
                                continue;
                            }

                            String[] keyvalue = locTypeSplit[i].split(":", 2);
                            if (keyvalue.length < 2) {
                                CivLog.warning("Invalid keyvalue:" + locTypeSplit[i] + " in template:" + this.filepath);
                                continue;
                            }
                            block.keyvalues.put(keyvalue[0].trim(), keyvalue[1].trim());
                        }
                    }

                    /* This block coord does not point to a location in a world, just a template. */
                    this.commandBlockRelativeLocations.add(new BlockCoord("", blockX, blockY, blockZ));

                } else {
                    block.specialType = SimpleBlock.Type.LITERAL;

                    // Literal sign, copy the sign into the simple block
                    for (int i = 0; i < 4; i++) {
                        try {
                            block.message[i] = locTypeSplit[i + 2];
                        } catch (ArrayIndexOutOfBoundsException e) {
                            block.message[i] = "";
                        }
                    }
                }
            }

            if (isAttachable(block.getType())) {
                this.attachableLocations.add(new BlockCoord("", blockX, blockY, blockZ));
            }

            blocks[blockX][blockY][blockZ] = block;

        }

        this.blocks = blocks;
    }

    public static BlockFace parseDirection(Location loc) {
        double rotation = ((loc.getYaw() - 90) % 360 + 360.0) % 360;

        if (45 <= rotation && rotation < 135) {
            return BlockFace.SOUTH;
        } else if (135 <= rotation && rotation < 225) {
            return BlockFace.WEST;
        } else if (225 <= rotation && rotation < 315) {
            return BlockFace.NORTH;
        } else {
            return BlockFace.EAST;
        }
    }

    public void deleteUndoTemplate(String string, String subdir) {
        String filepath = "templates/undo/" + subdir + "/" + string;
        if (new File(filepath).delete()) {
            CivLog.debug("Deleted " + filepath);
        }
    }

    public void deleteInProgessTemplate(String string, Town town) {
        String filepath = "templates/inprogress/" + town.getName() + "/" + string;
        if (new File(filepath).delete()) {
            CivLog.debug("Deleted " + filepath);
        }
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public void buildUndoTemplate(Template tpl, Block centerBlock) {

        for (int x = 0; x < tpl.size_x; x++) {
            for (int y = 0; y < tpl.size_y; y++) {
                for (int z = 0; z < tpl.size_z; z++) {
                    Block b = centerBlock.getRelative(x, y, z);

                    SimpleBlock sb = tpl.blocks[x][y][z];
                    if (CivSettings.restrictedUndoBlocks.contains(sb.getType())) {
                        continue;
                    }
                    sb.x = x + centerBlock.getX();
                    sb.y = y + centerBlock.getY();
                    sb.z = z + centerBlock.getZ();
                    sb.worldname = centerBlock.getWorld().getName();
//						sb.buildable = buildable;

                    sbs.add(sb);

                    if (b.getState() instanceof Sign s2) {
                        s2.setLine(0, tpl.blocks[x][y][z].message[0]);
                        s2.setLine(1, tpl.blocks[x][y][z].message[1]);
                        s2.setLine(2, tpl.blocks[x][y][z].message[2]);
                        s2.setLine(3, tpl.blocks[x][y][z].message[3]);
                        s2.update();
                    }
                }
            }
        }
        updateBlocksQueue(sbs);
    }

    public BlockFace dir() {
        return dir;
    }

    public String getTheme() {
        return strTheme;
    }

    public void setTheme(String strTheme) {
        this.strTheme = strTheme;
    }

}
