package com.avrgaming.civcraft.populators;

import java.sql.SQLException;
import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.generator.BlockPopulator;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMobSpawner;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.ProtectedBlock;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.object.MobSpawner;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.ItemManager;

public class MobSpawnerPopulator extends BlockPopulator {
    
    //private static final int RESOURCE_CHANCE = 400; 
    private static final int FLAG_HEIGHT = 3;
//    private static final double MIN_DISTANCE = 400.0;
    

    public static void buildMobSpawner(ConfigMobSpawner spawner, BlockCoord coord, World world, boolean sync) {
        MobSpawner newSpawner = new MobSpawner(spawner, coord);            
        CivGlobal.addMobSpawner(newSpawner);

        BlockFace direction = null;
        Block top = null;
        Random random = new Random();
        int dir = random.nextInt(4);
        if (dir == 0) {
            direction = BlockFace.NORTH;
        } else if (dir == 1) {
            direction = BlockFace.EAST;
        } else if (dir == 2) {
            direction = BlockFace.SOUTH;
        } else {
            direction = BlockFace.WEST;
        }

        //clear any stack goodies
        for (int y = coord.getY(); y < 256; y++) {
            top = world.getBlockAt(coord.getX(), y, coord.getZ());
            if (top.getType() == Material.BEDROCK) {
                top.setType(Material.AIR);
            }
        }
        
        for (int y = coord.getY(); y < coord.getY() + FLAG_HEIGHT-1; y++) {
            top = world.getBlockAt(coord.getX(), y, coord.getZ());
            top.setType(Material.NETHER_WART_BLOCK);

            ProtectedBlock pb = new ProtectedBlock(new BlockCoord(top), ProtectedBlock.Type.MOB_SPAWNER_MARKER);
            CivGlobal.addProtectedBlock(pb);
            if (sync) {
            try {
                pb.saveNow();
            } catch (SQLException e) {
                CivLog.warning("Unable to Protect Mob Spawner Block");
                e.printStackTrace();
            }    
            } else {
                pb.save();
            }
        }
        
        top = world.getBlockAt(coord.getX(), coord.getY()+FLAG_HEIGHT-1, coord.getZ());
        top.setType(Material.BEDROCK);
        
        ProtectedBlock pb = new ProtectedBlock(new BlockCoord(top), ProtectedBlock.Type.MOB_SPAWNER_MARKER);
        CivGlobal.addProtectedBlock(pb);
        if (sync) {
        try {
            pb.saveNow();
        } catch (SQLException e) {
            CivLog.warning("Unable to Protect Mob Spawner Block");
            e.printStackTrace();
        }    
        } else {
            pb.save();
        }

        Block signBlock = top.getRelative(direction);
        signBlock.setType(Material.WALL_SIGN);
        //TODO make sign a structure sign?
                //          Civ.protectedBlockTable.put(Civ.locationHash(signBlock.getLocation()), 
        //                  new ProtectedBlock(signBlock, null, null, null, ProtectedBlock.Type.TRADE_MARKER));

        BlockState state = signBlock.getState();

        if (state instanceof Sign) {
            Sign sign = (Sign)state;
            org.bukkit.material.Sign data = (org.bukkit.material.Sign)state.getData();

            data.setFacingDirection(direction);
            sign.setLine(0, CivSettings.localize.localizedString("MobSpawnerSign_Heading"));
            sign.setLine(1, "----");
            sign.setLine(2, spawner.name);
            sign.setLine(3, "");
            sign.update(true);

            StructureSign structSign = new StructureSign(new BlockCoord(signBlock), null);
            structSign.setAction("");
            structSign.setType("");
            structSign.setText(sign.getLines());
            structSign.setDirection(ItemManager.getData(sign.getData()));
            CivGlobal.addStructureSign(structSign);
            ProtectedBlock pbsign = new ProtectedBlock(new BlockCoord(signBlock), ProtectedBlock.Type.MOB_SPAWNER_MARKER);
            CivGlobal.addProtectedBlock(pbsign);
            if (sync) {
                try {
                    pbsign.saveNow();
                    structSign.saveNow();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                pbsign.save();
                structSign.save();
            }
        }
        
        if (sync) {
            try {
            	newSpawner.saveNow();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
        	newSpawner.save();
        }
    }

    public boolean checkForDuplicateMobSpawner(String worldName, int centerX, int centerY, int centerZ) {
        /* 
         * Search downward to bedrock for any mob spawners here. If we find one, don't generate. 
         */
        
        BlockCoord coord = new BlockCoord(worldName, centerX, centerY, centerZ);
        for (int y = centerY; y > 0; y--) {
            coord.setY(y);          
            
            if (CivGlobal.getMobSpawner(coord) != null) {
                /* Already a mob spawner here. DONT Generate it. */
                return true;
            }       
        }
        return false;
    }
    
    @Override
    public void populate(World world, Random random, Chunk source) {
        
        ChunkCoord cCoord = new ChunkCoord(source);
        MobSpawnerPick pick = CivGlobal.mobSpawnerPreGenerator.spawnerPicks.get(cCoord);
        if (pick != null) {
            int centerX = (source.getX() << 4) + 8;
            int centerZ = (source.getZ() << 4) + 8;
            int centerY = world.getHighestBlockYAt(centerX, centerZ);
            BlockCoord coord = new BlockCoord(world.getName(), centerX, centerY, centerZ);

            if (checkForDuplicateMobSpawner(world.getName(), centerX, centerY, centerZ)) {
                return;
            }

            // Determine if we should be a water good.
            ConfigMobSpawner spawner;
            if (world.getBlockAt(centerX, centerY - 1, centerZ).getType() == Material.STATIONARY_WATER ||
                    world.getBlockAt(centerX, centerY - 1, centerZ).getType() == Material.WATER) {
                spawner = pick.waterPick;
            } else {
                spawner = pick.landPick;
            }

            // Randomly choose a land or water good.
            if (spawner == null) {
                System.out.println("Could not find suitable mob spawner type during populate! aborting.");
                return;
            }
            
            // Create a copy and save it in the global hash table.
            buildMobSpawner(spawner, coord, world, false);
        }
    
    }

}