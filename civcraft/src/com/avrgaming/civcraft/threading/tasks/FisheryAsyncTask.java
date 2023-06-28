package com.avrgaming.civcraft.threading.tasks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.exception.CivTaskAbortException;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.StructureChest;
import com.avrgaming.civcraft.structure.FishHatchery;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.sync.request.UpdateInventoryRequest.Action;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.MultiInventory;

public class FisheryAsyncTask extends CivAsyncTask {

	FishHatchery fishHatchery;
	
	public static HashSet<String> debugTowns = new HashSet<String>();

	public static void debug(FishHatchery fishHatchery, String msg) {
		if (debugTowns.contains(fishHatchery.getTown().getName())) {
			CivLog.warning("FishHatcheryDebug:"+fishHatchery.getTown().getName()+":"+msg);
		}
	}	
	
	public FisheryAsyncTask(Structure fishHatchery) {
		this.fishHatchery = (FishHatchery)fishHatchery;
	}
	
	public void processFisheryUpdate() {
		if (!fishHatchery.isActive()) {
			debug(fishHatchery, "Fish Hatchery inactive...");
			return;
		}
		
		debug(fishHatchery, "Processing Fish Hatchery...");
		// Grab each CivChest object we'll require.
		ArrayList<StructureChest> sources = fishHatchery.getAllChestsById(0);
		sources.addAll(fishHatchery.getAllChestsById(1));
		sources.addAll(fishHatchery.getAllChestsById(2));
		sources.addAll(fishHatchery.getAllChestsById(3));
		ArrayList<StructureChest> destinations = fishHatchery.getAllChestsById(4);
		
		if (sources.size() != 4 || destinations.size() != 2) {
			CivLog.error("Bad chests for fish hatchery in town:"+fishHatchery.getTown().getName()+" sources:"+sources.size()+" dests:"+destinations.size());
			return;
		}
		
		// Make sure the chunk is loaded before continuing. Also, add get chest and add it to inventory.
		MultiInventory source_inv0 = new MultiInventory();
		MultiInventory source_inv1 = new MultiInventory();
		MultiInventory source_inv2 = new MultiInventory();
		MultiInventory source_inv3 = new MultiInventory();
		MultiInventory dest_inv = new MultiInventory();

		try {
			for (StructureChest src : sources) {
				//this.syncLoadChunk(src.getCoord().getWorldname(), src.getCoord().getX(), src.getCoord().getZ());				
				Inventory tmp;
				try {
					tmp = this.getChestInventory(src.getCoord().getWorldname(), src.getCoord().getX(), src.getCoord().getY(), src.getCoord().getZ(), false);
				} catch (CivTaskAbortException e) {
					//e.printStackTrace();
					CivLog.warning("Fish Hatchery:"+e.getMessage());
					return;
				}
				if (tmp == null) {
					fishHatchery.skippedCounter++;
					return;
				}
				switch(src.getChestId()){
				case 0: source_inv0.addInventory(tmp);
			 	break;
				case 1: source_inv1.addInventory(tmp);
			 	break;
				case 2: source_inv2.addInventory(tmp);
			 	break;
				case 3: source_inv3.addInventory(tmp);
			 	break;
				}
			}
			
			boolean full = true;
			for (StructureChest dst : destinations) {
				//this.syncLoadChunk(dst.getCoord().getWorldname(), dst.getCoord().getX(), dst.getCoord().getZ());
				Inventory tmp;
				try {
					tmp = this.getChestInventory(dst.getCoord().getWorldname(), dst.getCoord().getX(), dst.getCoord().getY(), dst.getCoord().getZ(), false);
				} catch (CivTaskAbortException e) {
					//e.printStackTrace();
					CivLog.warning("Fish Hatchery:"+e.getMessage());
					return;
				}
				if (tmp == null) {
					fishHatchery.skippedCounter++;
					return;
				}
				dest_inv.addInventory(tmp);
				
				for (ItemStack stack : tmp.getContents()) {
					if (stack == null) {
						full = false;
						break;
					}
				}
			}
			
			if (full) {
				/* Quarry destination chest is full, stop processing. */
				return;
			}
			
		} catch (InterruptedException e) {
			return;
		}

		debug(fishHatchery, "Processing Fish Hatchery:"+fishHatchery.skippedCounter+1);
		ItemStack[] contents0 = source_inv0.getContents();
		ItemStack[] contents1 = source_inv1.getContents();
		ItemStack[] contents2 = source_inv2.getContents();
		ItemStack[] contents3 = source_inv3.getContents();
		for (int i = 0; i < fishHatchery.skippedCounter+1; i++) {
		
			for(ItemStack stack : contents0) {
				if (stack == null) {
					continue;
				}
				
				if (ItemManager.getId(stack) == CivData.FISHING_ROD) {
					try {
						short damage = ItemManager.getData(stack);
						this.updateInventory(Action.REMOVE, source_inv0, ItemManager.createItemStack(CivData.FISHING_ROD, 1, damage));
						damage++;
						if (damage < 64) {
						this.updateInventory(Action.ADD, source_inv0, ItemManager.createItemStack(CivData.FISHING_ROD, 1, damage));
						}
					} catch (InterruptedException e) {
						return;
					}
					
					ItemStack newItem;
					
					newItem = this.getFishForBiome();
					
					//Try to add the new item to the dest chest, if we cant, oh well.
					try {
						debug(fishHatchery, "Updating inventory:"+newItem);
						this.updateInventory(Action.ADD, dest_inv, newItem);
					} catch (InterruptedException e) {
						return;
					}
					break;
				}
			}
			
			if (this.fishHatchery.getLevel() >= 2)
			{
				for(ItemStack stack : contents1) {
					if (stack == null) {
						continue;
					}
					
					if (ItemManager.getId(stack) == CivData.FISHING_ROD) {
						try {
							short damage = ItemManager.getData(stack);
							this.updateInventory(Action.REMOVE, source_inv1, ItemManager.createItemStack(CivData.FISHING_ROD, 1, damage));
							damage++;
							if (damage < 64) {
							this.updateInventory(Action.ADD, source_inv1, ItemManager.createItemStack(CivData.FISHING_ROD, 1, damage));
							}
						} catch (InterruptedException e) {
							return;
						}
						
						ItemStack newItem;
						
						newItem = this.getFishForBiome();
						
						//Try to add the new item to the dest chest, if we cant, oh well.
						try {
							debug(fishHatchery, "Updating inventory:"+newItem);
							this.updateInventory(Action.ADD, dest_inv, newItem);
						} catch (InterruptedException e) {
							return;
						}
						break;
					}
				}
			}
			
			if (this.fishHatchery.getLevel() >= 3)
			{
				for(ItemStack stack : contents2) {
					if (stack == null) {
						continue;
					}
					
					if (ItemManager.getId(stack) == CivData.FISHING_ROD) {
						try {
							short damage = ItemManager.getData(stack);
							this.updateInventory(Action.REMOVE, source_inv2, ItemManager.createItemStack(CivData.FISHING_ROD, 1, damage));
							damage++;
							if (damage < 64) {
							this.updateInventory(Action.ADD, source_inv2, ItemManager.createItemStack(CivData.FISHING_ROD, 1, damage));
							}
						} catch (InterruptedException e) {
							return;
						}
						
						ItemStack newItem;
						
						newItem = this.getFishForBiome();
						
						//Try to add the new item to the dest chest, if we cant, oh well.
						try {
							debug(fishHatchery, "Updating inventory:"+newItem);
							this.updateInventory(Action.ADD, dest_inv, newItem);
						} catch (InterruptedException e) {
							return;
						}
						break;
					}
				}
			}
			if (this.fishHatchery.getLevel() >= 4)
			{
				for(ItemStack stack : contents3) {
					if (stack == null) {
						continue;
					}
					
					if (ItemManager.getId(stack) == CivData.FISHING_ROD) {
						try {
							short damage = ItemManager.getData(stack);
							this.updateInventory(Action.REMOVE, source_inv3, ItemManager.createItemStack(CivData.FISHING_ROD, 1, damage));
							damage++;
							if (damage < 64) {
							this.updateInventory(Action.ADD, source_inv3, ItemManager.createItemStack(CivData.FISHING_ROD, 1, damage));
							}
						} catch (InterruptedException e) {
							return;
						}
						
						ItemStack newItem = this.getFishForBiome();
						
						//Try to add the new item to the dest chest, if we cant, oh well.
						try {
							debug(fishHatchery, "Updating inventory:"+newItem);
							this.updateInventory(Action.ADD, dest_inv, newItem);
							fishHatchery.getCorner().getBlock().getWorld().playSound(fishHatchery.getCenterLocation().getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1f, 1f);
						} catch (InterruptedException e) {
							return;
						}
						break;
					}
				}
			}
		}	
		fishHatchery.skippedCounter = 0;
	}
	
	private int getBiome() {
		Biome biome = this.fishHatchery.getBiome();
		switch (biome) {
			case BIRCH_FOREST_HILLS:
			case MUTATED_BIRCH_FOREST:
			case MUTATED_BIRCH_FOREST_HILLS:
			case MUTATED_TAIGA_COLD:
			case MUTATED_EXTREME_HILLS:
			case MUTATED_EXTREME_HILLS_WITH_TREES:
			case ICE_MOUNTAINS:
			case MUTATED_JUNGLE_EDGE:
			case JUNGLE_HILLS:
			case MUTATED_JUNGLE:
			case MUTATED_MESA:
			case MUTATED_MESA_CLEAR_ROCK:
			case MUTATED_MESA_ROCK:
			case MUTATED_SAVANNA:
			case MUTATED_SAVANNA_ROCK:
			case SMALLER_EXTREME_HILLS:
			case MUTATED_SWAMPLAND:
			case MUTATED_TAIGA:
				return 1;
			case BIRCH_FOREST:
			case EXTREME_HILLS:
			case FOREST:
			case FOREST_HILLS:
			case ICE_FLATS:
			case MUTATED_ICE_FLATS:
			case JUNGLE:
			case JUNGLE_EDGE:
			case MUTATED_REDWOOD_TAIGA:
			case MUTATED_REDWOOD_TAIGA_HILLS:
			case REDWOOD_TAIGA:
			case REDWOOD_TAIGA_HILLS:
			case ROOFED_FOREST:
			case MESA:
			case MESA_CLEAR_ROCK:
			case MESA_ROCK:
			case EXTREME_HILLS_WITH_TREES:
			case SAVANNA:
			case SAVANNA_ROCK:
			case TAIGA:
			case TAIGA_HILLS:
				return 2;
			case BEACHES:
			case COLD_BEACH:
			case TAIGA_COLD:
			case DEEP_OCEAN:
			case DESERT:
			case DESERT_HILLS:
			case MUTATED_DESERT:
			case FROZEN_OCEAN:
			case FROZEN_RIVER:
			case MUSHROOM_ISLAND:
			case MUSHROOM_ISLAND_SHORE:
			case OCEAN:
			case PLAINS:
			case RIVER:
			case STONE_BEACH:
			case SWAMPLAND:
				return 3;
			default:
				return 0;
		}
	}
	
	private ItemStack getFishForBiome() {
		

		Random rand = new Random();
		int maxRand = FishHatchery.MAX_CHANCE;
		int baseRand = rand.nextInt(maxRand);
		ItemStack newItem = null;
		int fisheryLevel = this.fishHatchery.getLevel();
		int fishTier;
		if (baseRand < ((int)((fishHatchery.getChance(FishHatchery.FISH_T4_RATE))*maxRand)) && fisheryLevel >= 4) {
			fishTier = 4;
		} else if (baseRand < ((int)((fishHatchery.getChance(FishHatchery.FISH_T3_RATE))*maxRand)) && fisheryLevel >= 3) {
			fishTier = 3;
		} else if (baseRand < ((int)((fishHatchery.getChance(FishHatchery.FISH_T2_RATE))*maxRand)) && fisheryLevel >= 2) {
			fishTier = 2;
		} else if (baseRand < ((int)((fishHatchery.getChance(FishHatchery.FISH_T1_RATE))*maxRand))) {
			fishTier = 1;
		} else {
			fishTier = 0;
		}
		int biome = getBiome();

		int randMax = 100;
		int biomeRand = rand.nextInt(randMax);
		
		switch (fishTier) {
			case 0: //Fish Tier 0
				if (biomeRand >= 95) { 
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_pufferfish"));
				} else if (biomeRand > 85) {
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_clownfish"));
				} else if (biomeRand > 75) {
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_salmon"));
				} else if (biomeRand > 50) {
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_fish"));
				} else {
					int junkRand = rand.nextInt(randMax);
					if (junkRand > 90)
					{
						newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_minnows"));
					}else if (junkRand > 70) {
						newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_tadpole"));
					} else if (junkRand > 50) {
						newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_seaweed"));
					} else if (junkRand > 30) {
						newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_tangled_string"));
					} else {
						newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_pond_scum"));
					}
				}
				break;
			case 1: //Fish Tier 1
				switch (biome) {
				case 0: //Not ranked
					newItem = ItemManager.createItemStack(CivData.FISH_RAW, 1);
					break;

				case 1: //Mountains
					if (biomeRand < 90) {
						newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_brown_trout"));
					} else {
						newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_gag_grouper"));
					}
					break;

				case 2: //Flatter Lands
					if (biomeRand < 90) {
						newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_atlantic_striped_bass"));
					} else {
						newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_arrowtooth_flounder"));
					}
					break;

				case 3: // Oceans, Mushroom, Swamps, Ice
					if (biomeRand < 90) {
						newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_atlantic_cod"));
					} else {
						newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_atlantic_surfclam"));
					}
					break;
			}
				break;

			case 2: //Fish Tier 2
				switch (biome) {
				case 0: //Not ranked
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_fish"));
					break;

				case 1: //Mountains
					if (biomeRand < 90) {
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_brook_trout"));
					} else {
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_red_grouper"));
					}
					break;

				case 2: //Flatter Lands
					if (biomeRand < 90) {
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_pacific_ocean_perch"));
					} else {
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_summer_flounder"));
					}
					break;

				case 3: // Oceans, Mushroom, Swamps, Ice
					if (biomeRand < 90) {
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_pacific_cod"));
					} else {
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_ocean_quahog"));
					}
					break;
			}
				break;

			case 3: //Fish Tier 3
				switch (biome) {
				case 0: //Not ranked
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_fish"));
					break;

				case 1: //Mountains
					if (biomeRand < 80) {
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_cutthroat_trout"));
					} else {
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_black_sea_bass"));
					}
					break;

				case 2: //Flatter Lands
					if (biomeRand < 80) {
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_acadian_redfish"));
					} else {
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_winter_flounder"));
					}
					break;

				case 3: // Oceans, Mushroom, Swamps, Ice
					if (biomeRand < 80) {
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_lingcod"));
					} else {
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_northern_quahog"));
					}
					break;
			}
				break;

			case 4: //Fish Tier 4
				switch (biome) {
				case 0: //Not ranked
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_fish"));
					break;

				case 1: //Mountains
					if (biomeRand < 80) {
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_rainbow_trout"));
					} else {
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_wreckfish"));
					}
					break;

				case 2: //Flatter Lands
					if (biomeRand < 80) {
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_widow_rockfish"));
					} else {
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_yellowtail_flounder"));
					}
					break;

				case 3: // Oceans, Mushroom, Swamps, Ice
					if (biomeRand < 80) {
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_sablefish"));
					} else {
					newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_geoduck"));
					}
					break;
			}
				break;
				
		}
		if (newItem == null)
		{
			CivLog.debug("Fish Hatchery: newItem was null");
			newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_fish_fish"));
		}

		return newItem;
		
	}
	
	@Override
	public void run() {
		if (this.fishHatchery.lock.tryLock()) {
			try {
				try {
					processFisheryUpdate();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} finally {
				this.fishHatchery.lock.unlock();
			}
		} else {
			debug(this.fishHatchery, "Failed to get lock while trying to start task, aborting.");
		}
	}

}