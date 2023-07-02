package com.avrgaming.civcraft.threading.tasks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.avrgaming.civcraft.exception.CivTaskAbortException;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.StructureChest;
import com.avrgaming.civcraft.structure.Quarry;
import com.avrgaming.civcraft.structure.Quarry.Mineral;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.sync.request.UpdateInventoryRequest.Action;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.MultiInventory;

public class QuarryAsyncTask extends CivAsyncTask {

	Quarry quarry;
	
	public static HashSet<String> debugTowns = new HashSet<String>();

	public static void debug(Quarry quarry, String msg) {
		if (debugTowns.contains(quarry.getTown().getName())) {
			CivLog.warning("QuarryDebug:"+quarry.getTown().getName()+":"+msg);
		}
	}	
	
	public QuarryAsyncTask(Structure quarry) {
		this.quarry = (Quarry)quarry;
	}
	
//	private Boolean hasSilkTouch(ItemStack stack) {
//
//		if (stack.hasItemMeta()) {
//			ItemMeta testEnchantMeta = stack.getItemMeta();
//			if (testEnchantMeta.hasEnchant(Enchantment.SILK_TOUCH)) {
//
//				debug(quarry, "Pickaxe has SILK_TOUCH");
//				return true;
//				
//			}
//		}
//		return false;
//	}
	
	private int checkDigSpeed(ItemStack stack) {

		if (stack.hasItemMeta()) {
			ItemMeta testEnchantMeta = stack.getItemMeta();
			if (testEnchantMeta.hasEnchant(Enchantment.DIG_SPEED)) {

				debug(quarry, "Pickaxe has DIG_SPEED lvl: "+testEnchantMeta.getEnchantLevel(Enchantment.DIG_SPEED));
				return testEnchantMeta.getEnchantLevel(Enchantment.DIG_SPEED)+1;
				
			}
		}
		return 1;
	}
	
	public void processQuarryUpdate() {
		if (!quarry.isActive()) {
			debug(quarry, "quarry inactive...");
			return;
		}
		
		debug(quarry, "Processing Quarry...");
		// Grab each CivChest object we'll require.
		ArrayList<StructureChest> sources = quarry.getAllChestsById(0);
		ArrayList<StructureChest> destinations = quarry.getAllChestsById(1);
		
		if (sources.size() != 2 || destinations.size() != 2) {
			CivLog.error("Bad chests for quarry in town:"+quarry.getTown().getName()+" sources:"+sources.size()+" dests:"+destinations.size());
			return;
		}
		
		// Make sure the chunk is loaded before continuing. Also, add get chest and add it to inventory.
		MultiInventory source_inv = new MultiInventory();
		MultiInventory dest_inv = new MultiInventory();

		try {
			for (StructureChest src : sources) {
				//this.syncLoadChunk(src.getCoord().getWorldname(), src.getCoord().getX(), src.getCoord().getZ());				
				Inventory tmp;
				try {
					tmp = this.getChestInventory(src.getCoord().getWorldname(), src.getCoord().getX(), src.getCoord().getY(), src.getCoord().getZ(), false);
				} catch (CivTaskAbortException e) {
					//e.printStackTrace();
					CivLog.warning("Quarry:"+e.getMessage());
					return;
				}
				if (tmp == null) {
					quarry.skippedCounter++;
					return;
				}
				source_inv.addInventory(tmp);
			}
			
			boolean full = true;
			for (StructureChest dst : destinations) {
				//this.syncLoadChunk(dst.getCoord().getWorldname(), dst.getCoord().getX(), dst.getCoord().getZ());
				Inventory tmp;
				try {
					tmp = this.getChestInventory(dst.getCoord().getWorldname(), dst.getCoord().getX(), dst.getCoord().getY(), dst.getCoord().getZ(), false);
				} catch (CivTaskAbortException e) {
					//e.printStackTrace();
					CivLog.warning("Quarry:"+e.getMessage());
					return;
				}
				if (tmp == null) {
					quarry.skippedCounter++;
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

		debug(quarry, "Processing quarry:"+quarry.skippedCounter+1);
		ItemStack[] contents = source_inv.getContents();
		for (int i = 0; i < quarry.skippedCounter+1; i++) {
		
			for(ItemStack stack : contents) {
				if (stack == null) {
					continue;
				}
				int modifier = checkDigSpeed(stack);

                if (stack.getTypeId() == Material.WOOD_PICKAXE.getId()) {
					try {
						short damage = ItemManager.getData(stack);
						this.updateInventory(Action.REMOVE, source_inv, stack);
						damage+= modifier;
						stack.setDurability(damage);
						if (damage < 59 && stack.getAmount() == 1) {
							this.updateInventory(Action.ADD, source_inv, stack);
						}
					} catch (InterruptedException e) {
						return;
					}
					
					// Attempt to get special resources
					Random rand = new Random();
					int randMax1 = Quarry.MAX_CHANCE;
					int rand1 = rand.nextInt(randMax1);
					ItemStack newItem;
					
					if (rand1 < ((int)((quarry.getChance(Mineral.COAL)/2)))) {
						newItem = ItemManager.createItemStack(Material.COAL.getId(), modifier);
					} else if (rand1 < ((int)((quarry.getChance(Mineral.OTHER)/2)))) {
						newItem = getOther(modifier);
					} else if (rand1 < ((int)((quarry.getChance(Mineral.COBBLESTONE)/2)))) {
						newItem = ItemManager.createItemStack(Material.COBBLESTONE.getId(), modifier);
					} else {
						newItem = getJunk(modifier);
					}
					
					//Try to add the new item to the dest chest, if we cant, oh well.
					try {
						debug(quarry, "Updating inventory:"+newItem);
						this.updateInventory(Action.ADD, dest_inv, newItem);
						quarry.getCorner().getBlock().getWorld().playSound(quarry.getCenterLocation().getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.2f, 1.2f);

					} catch (InterruptedException e) {
						return;
					}
					break;
				}
                if (this.quarry.getLevel() >= 2 && stack.getTypeId() == Material.STONE_PICKAXE.getId()) {
                    try {
                        short damage = ItemManager.getData(stack);
                        this.updateInventory(Action.REMOVE, source_inv, stack);
                        damage += modifier;
                        stack.setDurability(damage);
                        if (damage < 131 && stack.getAmount() == 1) {
                            this.updateInventory(Action.ADD, source_inv, stack);
                        }
                    } catch (InterruptedException e) {
                        return;
                    }

// Attempt to get special resources
                    Random rand = new Random();
                    int randMax1 = Quarry.MAX_CHANCE;
                    int rand1 = rand.nextInt(randMax1);
                    ItemStack newItem;

                    if (rand1 < ((int) ((quarry.getChance(Mineral.GOLD))))) {
                        newItem = ItemManager.createItemStack(Material.GOLD_INGOT.getId(), modifier);
                    } else if (rand1 < ((int) ((quarry.getChance(Mineral.IRON))))) {
                        newItem = ItemManager.createItemStack(Material.IRON_INGOT.getId(), modifier);
                    } else if (rand1 < ((int) ((quarry.getChance(Mineral.COAL))))) {
                        newItem = ItemManager.createItemStack(Material.COAL.getId(), modifier);
                    } else if (rand1 < ((int) ((quarry.getChance(Mineral.OTHER))))) {
                        newItem = getOther(modifier);
                    } else if (rand1 < ((int) ((quarry.getChance(Mineral.COBBLESTONE) / 2)))) {
                        newItem = ItemManager.createItemStack(Material.COBBLESTONE.getId(), modifier);
                    } else {
                        newItem = getJunk(modifier);
                    }

//Try to add the new item to the dest chest, if we cant, oh well.
                    try {
                        debug(quarry, "Updating inventory:" + newItem);
                        this.updateInventory(Action.ADD, dest_inv, newItem);
                        quarry.getCorner().getBlock().getWorld().playSound(quarry.getCenterLocation().getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.2f, 1.2f);

                    } catch (InterruptedException e) {
                        return;
                    }
                    break;
                }
                if (this.quarry.getLevel() >= 3 && stack.getTypeId() == Material.IRON_PICKAXE.getId()) {
                    try {
                        short damage = ItemManager.getData(stack);
                        this.updateInventory(Action.REMOVE, source_inv, stack);
                        damage += modifier;
                        stack.setDurability(damage);
                        if (damage < 250 && stack.getAmount() == 1) {
                            this.updateInventory(Action.ADD, source_inv, stack);
                        }
                    } catch (InterruptedException e) {
                        return;
                    }

// Attempt to get special resources
                    Random rand = new Random();
                    int randMax1 = Quarry.MAX_CHANCE;
                    int rand1 = rand.nextInt(randMax1);
                    ItemStack newItem;

                    if (rand1 < ((int) ((quarry.getChance(Mineral.RARE))))) {
                        newItem = getRare(modifier);
                    } else if (rand1 < ((int) ((quarry.getChance(Mineral.TUNGSTEN))))) {
                        newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_tungsten_ore"), modifier);
                    } else if (rand1 < ((int) ((quarry.getChance(Mineral.GOLD))))) {
                        newItem = ItemManager.createItemStack(Material.GOLD_INGOT.getId(), modifier);
                    } else if (rand1 < ((int) ((quarry.getChance(Mineral.REDSTONE))))) {
                        int itemRand = rand.nextInt(5) + 1;
                        newItem = ItemManager.createItemStack(Material.REDSTONE.getId(), itemRand * modifier);
                    } else if (rand1 < ((int) ((quarry.getChance(Mineral.IRON))))) {
                        newItem = ItemManager.createItemStack(Material.IRON_INGOT.getId(), modifier);
                    } else if (rand1 < ((int) ((quarry.getChance(Mineral.COAL))))) {
                        newItem = ItemManager.createItemStack(Material.COAL.getId(), modifier);
                    } else if (rand1 < ((int) ((quarry.getChance(Mineral.OTHER))))) {
                        newItem = getOther(modifier);
                    } else if (rand1 < ((int) ((quarry.getChance(Mineral.COBBLESTONE) / 2)))) {
                        newItem = ItemManager.createItemStack(Material.COBBLESTONE.getId(), modifier);
                    } else {
                        newItem = getJunk(modifier);
                    }

//Try to add the new item to the dest chest, if we cant, oh well.
                    try {
                        debug(quarry, "Updating inventory:" + newItem);
                        this.updateInventory(Action.ADD, dest_inv, newItem);
                        quarry.getCorner().getBlock().getWorld().playSound(quarry.getCenterLocation().getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.2f, 1.2f);
                    } catch (InterruptedException e) {
                        return;
                    }
                    break;
                }
                if (stack.getTypeId() == Material.GOLD_PICKAXE.getId()) {
					try {
						short damage = ItemManager.getData(stack);
						this.updateInventory(Action.REMOVE, source_inv, stack);
						damage+= modifier;
						stack.setDurability(damage);
						if (damage < 32 && stack.getAmount() == 1) {
							this.updateInventory(Action.ADD, source_inv, stack);
						}
					} catch (InterruptedException e) {
						return;
					}
					
					// Attempt to get special resources
					Random rand = new Random();
					int randMax1 = Quarry.MAX_CHANCE;
					int rand1 = rand.nextInt(randMax1);
					ItemStack newItem;
					
					if (rand1 < ((int)((quarry.getChance(Mineral.COAL)/2)))) {
						newItem = ItemManager.createItemStack(Material.COAL.getId(), modifier);
					} else if (rand1 < ((int)((quarry.getChance(Mineral.OTHER)/2)))) {
						newItem = getOther(modifier);
					} else {
						newItem = ItemManager.createItemStack(Material.COBBLESTONE.getId(), modifier);
					}
					
					//Try to add the new item to the dest chest, if we cant, oh well.
					try {
						debug(quarry, "Updating inventory:"+newItem);
						this.updateInventory(Action.ADD, dest_inv, newItem);
						quarry.getCorner().getBlock().getWorld().playSound(quarry.getCenterLocation().getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.2f, 1.2f);

					} catch (InterruptedException e) {
						return;
					}
					break;
				}
                if (this.quarry.getLevel() >= 4 && stack.getTypeId() == Material.DIAMOND_PICKAXE.getId()) {
                    try {
                        short damage = ItemManager.getData(stack);
                        this.updateInventory(Action.REMOVE, source_inv, stack);
                        damage += modifier;
                        stack.setDurability(damage);
                        if (damage < 1561 && stack.getAmount() == 1) {
                            this.updateInventory(Action.ADD, source_inv, stack);
                        }
                    } catch (InterruptedException e) {
                        return;
                    }

// Attempt to get special resources
                    Random rand = new Random();
                    int randMax1 = Quarry.MAX_CHANCE;
                    int rand1 = rand.nextInt(randMax1);
                    ItemStack newItem;

                    if (rand1 < ((int) ((quarry.getChance(Mineral.RARE))))) {
                        newItem = getRare(modifier);
                    } else if (rand1 < ((int) ((quarry.getChance(Mineral.TUNGSTEN))))) {
                        newItem = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_tungsten_ore"), modifier);
                    } else if (rand1 < ((int) ((quarry.getChance(Mineral.GOLD))))) {
                        newItem = ItemManager.createItemStack(Material.GOLD_INGOT.getId(), modifier);
                    } else if (rand1 < ((int) ((quarry.getChance(Mineral.REDSTONE))))) {
                        int itemRand = rand.nextInt(5) + 1;
                        newItem = ItemManager.createItemStack(Material.REDSTONE.getId(), itemRand * modifier);
                    } else if (rand1 < ((int) ((quarry.getChance(Mineral.IRON))))) {
                        newItem = ItemManager.createItemStack(Material.IRON_INGOT.getId(), modifier);
                    } else if (rand1 < ((int) ((quarry.getChance(Mineral.COAL))))) {
                        newItem = ItemManager.createItemStack(Material.COAL.getId(), modifier);
                    } else if (rand1 < ((int) ((quarry.getChance(Mineral.OTHER))))) {
                        newItem = getOther(modifier);
                    } else if (rand1 < ((int) ((quarry.getChance(Mineral.COBBLESTONE) / 2)))) {
                        newItem = ItemManager.createItemStack(Material.COBBLESTONE.getId(), modifier);
                    } else {
                        newItem = getJunk(modifier);
                    }

//Try to add the new item to the dest chest, if we cant, oh well.
                    try {
                        debug(quarry, "Updating inventory:" + newItem);
                        this.updateInventory(Action.ADD, dest_inv, newItem);
                        quarry.getCorner().getBlock().getWorld().playSound(quarry.getCenterLocation().getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.2f, 1.2f);

                    } catch (InterruptedException e) {
                        return;
                    }
                    break;
                }
            }
		}	
	quarry.skippedCounter = 0;
	}
	
	private ItemStack getJunk(int modifier) {
		int randMax = 10;
		Random rand = new Random();
		int rand2 = rand.nextInt(randMax);
		if (rand2 < (2) && !quarry.getTown().getBuffManager().hasBuff(quarry.NO_DIRT)) {
			return ItemManager.createItemStack(Material.DIRT.getId(), modifier, (short) CivData.PODZOL);
		} else if (rand2 < (5)) {
			return ItemManager.createItemStack(Material.DIRT.getId(), modifier, (short) CivData.COARSE_DIRT);
		} else {
			return ItemManager.createItemStack(Material.DIRT.getId(), modifier);
		}
	}
	
	private ItemStack getOther(int modifier) {
		int randMax = Quarry.MAX_CHANCE;
		Random rand = new Random();
		int rand2 = rand.nextInt(randMax);
		if (rand2 < (randMax/8)) {
			return ItemManager.createItemStack(Material.STONE.getId(), modifier, (short) CivData.ANDESITE);
		} else if (rand2 < (randMax/5)) {
			return ItemManager.createItemStack(Material.STONE.getId(), modifier, (short) CivData.DIORITE);
		} else {
			return ItemManager.createItemStack(Material.STONE.getId(), modifier, (short) CivData.GRANITE);
		}
	}
	
	private ItemStack getRare(int modifier) {
		int randMax = Quarry.MAX_CHANCE;
		Random rand = new Random();
		int rand2 = rand.nextInt(randMax);
		if (rand2 < (randMax/5)) {
			return ItemManager.createItemStack(Material.EMERALD.getId(), modifier);
		} else {
			return ItemManager.createItemStack(Material.DIAMOND.getId(), modifier);
		}
	}
	
	
	
	@Override
	public void run() {
		if (this.quarry.lock.tryLock()) {
			try {
				try {
					processQuarryUpdate();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} finally {
				this.quarry.lock.unlock();
			}
		} else {
			debug(this.quarry, "Failed to get lock while trying to start task, aborting.");
		}
	}

}
