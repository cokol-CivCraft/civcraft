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
package com.avrgaming.civcraft.listener;

import com.avrgaming.civcraft.cache.ArrowFiredCache;
import com.avrgaming.civcraft.cache.CivCache;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigRemovedRecipes;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.items.ItemDurabilityEntry;
import com.avrgaming.civcraft.items.components.Catalyst;
import com.avrgaming.civcraft.listener.armor.ArmorType;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.lorestorage.ItemChangeResult;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.ItemManager;
import gpl.AttributeUtil;
import gpl.HorseModifier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.material.MaterialData;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

@SuppressWarnings("deprecation")
public class CustomItemManager implements Listener {

    public static HashMap<String, LinkedList<ItemDurabilityEntry>> itemDuraMap = new HashMap<>();
    public static boolean duraTaskScheduled = false;


    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreakSpawnItems(BlockBreakEvent event) {
        if (event.getBlock().getType().equals(Material.LAPIS_ORE)) {
            if (event.getPlayer().getInventory().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH)) {
                return;
            }

            event.setCancelled(true);

            Block block = event.getBlock();
            block.setType(Material.AIR);
            block.setData((byte) 0);

            try {
                Random rand = new Random();

                int min = CivSettings.getInteger(CivSettings.materialsConfig, "tungsten_min_drop");
                int max;
                if (event.getPlayer().getInventory().getItemInMainHand().containsEnchantment(Enchantment.LOOT_BONUS_BLOCKS)) {
                    max = CivSettings.getInteger(CivSettings.materialsConfig, "tungsten_max_drop_with_fortune");
                } else {
                    max = CivSettings.getInteger(CivSettings.materialsConfig, "tungsten_max_drop");
                }

                int randAmount = rand.nextInt(min + max);
                randAmount -= min;
                if (randAmount <= 0) {
                    randAmount = 1;
                }

                for (int i = 0; i < randAmount; i++) {
                    ItemStack stack = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_tungsten_ore"));
                    event.getPlayer().getWorld().dropItemNaturally(event.getBlock().getLocation(), stack);
                }

            } catch (InvalidConfiguration e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack stack = event.getPlayer().getInventory().getItemInMainHand();
        if (stack == null || stack.getType().equals(Material.AIR)) {
            return;
        }

        LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
        if (craftMat == null) {
            return;
        }

        craftMat.onBlockPlaced(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack stack = null;
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            stack = event.getPlayer().getInventory().getItemInOffHand();
        } else {
            stack = event.getPlayer().getInventory().getItemInMainHand();
        }
        if (stack == null) {
            return;
        }

        LoreMaterial material = LoreMaterial.getMaterial(stack);
        if (material != null) {
            material.onInteract(event);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }

        ItemStack stack = event.getPlayer().getInventory().getItemInMainHand();
        if (stack == null) {
            return;
        }

        LoreMaterial material = LoreMaterial.getMaterial(stack);
        if (material != null) {
            material.onInteractEntity(event);
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemHeld(PlayerItemHeldEvent event) {
        if (event.isCancelled()) {
            return;
        }

        ItemStack stack = event.getPlayer().getInventory().getItemInMainHand();
        if (stack == null) {
            return;
        }

        LoreMaterial material = LoreMaterial.getMaterial(stack);
        if (material != null) {
            material.onHold(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void OnPlayerDropItem(PlayerDropItemEvent event) {
        if (event.isCancelled()) {
            return;
        }
        ItemStack stack = event.getItemDrop().getItemStack();

        if (LoreMaterial.isCustom(stack)) {
            LoreMaterial.getMaterial(stack).onItemDrop(event);
            return;
        }

        String custom = isCustomDrop(stack);

        if (custom != null) {
            event.setCancelled(true);
        }
    }

    private static String isCustomDrop(ItemStack stack) {
        if (stack == null || stack.getTypeId() != 166) {
            return null;
        }

        if (LoreGuiItem.isGUIItem(stack)) {
            return null;
        }

        return stack.getItemMeta().getDisplayName();
    }

    /*
     * Prevent the player from using goodies in crafting recipies.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void OnCraftItemEvent(CraftItemEvent event) {
        for (ItemStack stack : event.getInventory().getMatrix()) {
            if (stack != null) {

                if (LoreMaterial.isCustom(stack)) {
                    LoreMaterial.getMaterial(stack).onItemCraft(event);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void OnPlayerItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            ItemStack stack = event.getItem().getItemStack();

            if (LoreMaterial.isCustom(stack)) {
                LoreMaterial.getMaterial(stack).onItemPickup(event);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void OnItemSpawn(ItemSpawnEvent event) {
        ItemStack stack = event.getEntity().getItemStack();

        if (LoreMaterial.isCustom(stack)) {
            LoreMaterial.getMaterial(stack).onItemSpawn(event);
            return;
        }

        String custom = isCustomDrop(stack);

        if (custom != null) {
            ItemStack newStack = LoreMaterial.spawn(LoreMaterial.materialMap.get(custom), stack.getAmount());
            event.getEntity().getWorld().dropItemNaturally(event.getLocation(), newStack);
            event.setCancelled(true);
            return;
        }

        if (isUnwantedVanillaItem(stack)) {
            if (!stack.getType().equals(Material.HOPPER) &&
                    !stack.getType().equals(Material.HOPPER_MINECART)) {
                event.setCancelled(true);
                event.getEntity().remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDefenseAndAttack(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player defendingPlayer = null;
        if (event.getEntity() instanceof Player) {
            defendingPlayer = (Player) event.getEntity();
        }

        if (event.getDamager() instanceof LightningStrike) {
            /* Return after Tesla tower does damage, do not apply armor defense. */
            try {
                event.setDamage(CivSettings.getInteger(CivSettings.warConfig, "tesla_tower.damage"));
                return;
            } catch (InvalidConfiguration e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (event.getDamager() instanceof Arrow) {
            LivingEntity shooter = (LivingEntity) ((Arrow) event.getDamager()).getShooter();

            if (shooter instanceof Player) {
                ItemStack inHand = ((Player) shooter).getInventory().getItemInMainHand();
                if (LoreMaterial.isCustom(inHand)) {
                    LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(inHand);
                    craftMat.onRangedAttack(event, inHand);
                }
            } else {
                ArrowFiredCache afc = CivCache.arrowsFired.get(event.getDamager().getUniqueId());
                if (afc != null) {
                    /* Arrow was fired by a tower. */
                    afc.setHit(true);
                    afc.destroy(event.getDamager());
                    if (defendingPlayer != null) {
                        Resident defenderResident = CivGlobal.getResident(defendingPlayer);
                        if (defenderResident != null && defenderResident.hasTown() &&
                                defenderResident.getTown().getCiv() == afc.getFromTower().getTown().getCiv()) {
                            /* Prevent friendly fire from arrow towers. */
                            event.setCancelled(true);
                            return;
                        }
                    }

                    /* Return after arrow tower does damage, do not apply armor defense. */
                    event.setDamage(afc.getFromTower().getDamage());
                    return;
                }
            }
        } else if (event.getDamager() instanceof Player) {
            ItemStack inHand = ((Player) event.getDamager()).getInventory().getItemInMainHand();
            LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(inHand);
            if (craftMat != null) {
                craftMat.onAttack(event, inHand);
            } else {
                /* Non-civcraft items only do 0.5 damage. */
                event.setDamage(0.5);
            }
        }

        if (event.getEntity() instanceof Horse) {
            if (HorseModifier.isCivCraftHorse((LivingEntity) event.getEntity())) {
                //Horses take 50% damage from all sources.
                event.setDamage(event.getDamage() / 2.0);
            }
        }

        if (defendingPlayer == null) {
//			if (event.getEntity() instanceof LivingEntity) {
//				if (MobLib.isMobLibEntity((LivingEntity) event.getEntity())) {
//					MobComponent.onDefense(event.getEntity(), event);
//				}	
//			}
        } else {
            /* Search equipt items for defense event. */
            for (ItemStack stack : defendingPlayer.getEquipment().getArmorContents()) {
                if (LoreMaterial.isCustom(stack)) {
                    LoreMaterial.getMaterial(stack).onDefense(event, stack);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void OnInventoryClose(InventoryCloseEvent event) {
        for (ItemStack stack : event.getInventory().getContents()) {
            if (stack == null) {
                continue;
            }

            if (LoreMaterial.isCustom(stack)) {
                LoreMaterial.getMaterial(stack).onInventoryClose(event);
            }
        }

        for (ItemStack stack : event.getPlayer().getInventory()) {
            if (stack == null) {
                continue;
            }

            if (LoreMaterial.isCustom(stack)) {
                LoreMaterial.getMaterial(stack).onInventoryClose(event);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void OnInventoryOpen(InventoryOpenEvent event) {
        for (ItemStack stack : event.getInventory().getContents()) {
            if (stack == null) {
                continue;
            }

            if (LoreMaterial.isCustom(stack)) {
                LoreCraftableMaterial.getMaterial(stack).onInventoryOpen(event, stack);
            }
        }

        for (ItemStack stack : event.getPlayer().getInventory()) {
            if (stack == null) {
                continue;
            }

            if (LoreMaterial.isCustom(stack)) {
                LoreMaterial.getMaterial(stack).onInventoryOpen(event, stack);
            }
        }

        for (ItemStack stack : event.getPlayer().getInventory().getArmorContents()) {
            if (stack == null) {
                continue;
            }

            if (LoreMaterial.isCustom(stack)) {
                LoreMaterial.getMaterial(stack).onInventoryOpen(event, stack);
            }
        }
    }

    /*
     * Returns false if item is destroyed.
     */
    private boolean processDurabilityChanges(PlayerDeathEvent event, ItemStack stack, int i) {
        LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
        if (craftMat != null) {
            ItemChangeResult result = craftMat.onDurabilityDeath(event, stack);
            if (result != null) {
                if (!result.destroyItem) {
                    event.getEntity().getInventory().setItem(i, result.stack);
                } else {
                    event.getEntity().getInventory().setItem(i, new ItemStack(Material.AIR));
                    event.getDrops().remove(stack);
                    return false;
                }
            }
        }

        return true;
    }

    private boolean processArmorDurabilityChanges(PlayerDeathEvent event, ItemStack stack, int i) {
        LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
        if (craftMat != null) {
            ItemChangeResult result = craftMat.onDurabilityDeath(event, stack);
            if (result != null) {
                if (!result.destroyItem) {
                    replaceItem(event, stack, result.stack);
                } else {
                    replaceItem(event, stack, new ItemStack(Material.AIR));
                    event.getDrops().remove(stack);
                    return false;
                }
            }
        }

        return true;
    }

    private void replaceItem(PlayerDeathEvent event, ItemStack oldItem, ItemStack newItem) {
        ArmorType type = ArmorType.matchType(oldItem);
        switch (type) {
            case HELMET -> event.getEntity().getInventory().setHelmet(newItem);
            case CHESTPLATE -> event.getEntity().getInventory().setChestplate(newItem);
            case LEGGINGS -> event.getEntity().getInventory().setLeggings(newItem);
            case BOOTS -> event.getEntity().getInventory().setBoots(newItem);
        }

    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeathEvent(PlayerDeathEvent event) {
        HashMap<Integer, ItemStack> noDrop = new HashMap<>();
        ItemStack[] armorNoDrop = new ItemStack[4];

        /* Search and execute any enhancements */
        for (int i = 0; i < event.getEntity().getInventory().getSize(); i++) {
            ItemStack stack = event.getEntity().getInventory().getItem(i);
            if (stack == null) {
                continue;
            }

            if (!processDurabilityChanges(event, stack, i)) {
                /* Don't process anymore more enhancements on items after its been destroyed. */
                continue;
            }

            if (!LoreMaterial.hasEnhancements(stack)) {
                continue;
            }

            AttributeUtil attrs = new AttributeUtil(stack);
            for (LoreEnhancement enhance : attrs.getEnhancements()) {
                if (enhance.onDeath(event, stack)) {
                    /* Stack is not going to be dropped on death. */
                    noDrop.put(i, stack);
                }
            }
        }

        /* Search for armor, apparently it doesnt show up in the normal inventory. */
        ItemStack[] contents = event.getEntity().getInventory().getArmorContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null) {
                continue;
            }

            if (!processArmorDurabilityChanges(event, stack, i)) {
                /* Don't process anymore more enhancements on items after its been destroyed. */
                continue;
            }

            if (!LoreMaterial.hasEnhancements(stack)) {
                continue;
            }

            AttributeUtil attrs = new AttributeUtil(stack);
            for (LoreEnhancement enhance : attrs.getEnhancements()) {
                if (enhance.onDeath(event, stack)) {
                    /* Stack is not going to be dropped on death. */
                    armorNoDrop[i] = stack;
                }
            }
        }


        //event.getEntity().getInventory().getArmorContents()
        if (Boolean.parseBoolean(Bukkit.getWorld("world").getGameRuleValue("keepInventory"))) {
            return;
        }
        TaskMaster.syncTask(() -> {
            try {
                Player player = CivGlobal.getPlayer(event.getEntity().getName());
                PlayerInventory inv = player.getInventory();
                for (Integer slot : noDrop.keySet()) {
                    ItemStack stack = noDrop.get(slot);
                    inv.setItem(slot, stack);
                }

                inv.setArmorContents(armorNoDrop);
            } catch (CivException e) {
                e.printStackTrace();
            }
        });


    }

    @EventHandler(priority = EventPriority.LOW)
    public void OnEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }

        /* Remove any vanilla item IDs that can't be crafted from vanilla drops. */
        LinkedList<ItemStack> removed = new LinkedList<>();
        for (ItemStack stack : event.getDrops()) {
            Integer key = stack.getTypeId();

            if (CivSettings.removedRecipies.containsKey(key)) {
                if (!LoreMaterial.isCustom(stack)) {
                    removed.add(stack);
                }
            }
        }

        event.getDrops().removeAll(removed);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onItemPickup(PlayerPickupItemEvent event) {

        ItemStack stack4 = event.getItem().getItemStack();
        if (stack4.getTypeId() == Material.SLIME_BALL.getId()) {
            LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(event.getItem().getItemStack());
            if (craftMat == null) {
                /* Found a vanilla slime ball. */
                LoreCraftableMaterial slime = LoreCraftableMaterial.getCraftMaterialFromId("mat_vanilla_slime");
                ItemStack newStack = LoreCraftableMaterial.spawn(slime);
                newStack.setAmount(event.getItem().getItemStack().getAmount());
                event.getPlayer().getInventory().addItem(newStack);
                event.getPlayer().updateInventory();
                event.getItem().remove();
                event.setCancelled(true);
            }
        } else {
            ItemStack stack3 = event.getItem().getItemStack();
            if (stack3.getTypeId() == Material.ENDER_PEARL.getId()) {
                LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(event.getItem().getItemStack());
                if (craftMat == null) {
                    /* Found a vanilla ender peral. */
                    LoreCraftableMaterial slime = LoreCraftableMaterial.getCraftMaterialFromId("mat_ender_pearl");
                    ItemStack newStack = LoreCraftableMaterial.spawn(slime);
                    newStack.setAmount(event.getItem().getItemStack().getAmount());
                    event.getPlayer().getInventory().addItem(newStack);
                    event.getPlayer().updateInventory();
                    event.getItem().remove();
                    event.setCancelled(true);
                }
            } else {
                ItemStack stack2 = event.getItem().getItemStack();
                if (stack2.getTypeId() == Material.TNT.getId()) {
                    LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(event.getItem().getItemStack());
                    if (craftMat == null) {
                        /* Found a vanilla tnt. */
                        LoreCraftableMaterial slime = LoreCraftableMaterial.getCraftMaterialFromId("mat_vanilla_tnt");
                        ItemStack newStack = LoreCraftableMaterial.spawn(slime);
                        newStack.setAmount(event.getItem().getItemStack().getAmount());
                        event.getPlayer().getInventory().addItem(newStack);
                        event.getPlayer().updateInventory();
                        event.getItem().remove();
                        event.setCancelled(true);
                    }
                } else {
                    ItemStack stack1 = event.getItem().getItemStack();
                    if (stack1.getTypeId() == Material.RAW_FISH.getId()
                            && ItemManager.getData(event.getItem().getItemStack()) ==
                            new MaterialData(Material.RAW_FISH.getId(), (byte) CivData.CLOWNFISH).getData()) {
                        LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(event.getItem().getItemStack());
                        if (craftMat == null) {
                            /* Found a vanilla clownfish. */
                            LoreCraftableMaterial clown = LoreCraftableMaterial.getCraftMaterialFromId("mat_vanilla_clownfish");
                            ItemStack newStack = LoreCraftableMaterial.spawn(clown);
                            newStack.setAmount(event.getItem().getItemStack().getAmount());
                            event.getPlayer().getInventory().addItem(newStack);
                            event.getPlayer().updateInventory();
                            event.getItem().remove();
                            event.setCancelled(true);
                        }
                    } else {
                        ItemStack stack = event.getItem().getItemStack();
                        if (stack.getTypeId() == Material.RAW_FISH.getId() && ItemManager.getData(event.getItem().getItemStack()) ==
                                new MaterialData(Material.RAW_FISH.getId(), (byte) CivData.PUFFERFISH).getData()) {
                            LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(event.getItem().getItemStack());
                            if (craftMat == null) {
                                /* Found a vanilla pufferfish. */
                                LoreCraftableMaterial clown = LoreCraftableMaterial.getCraftMaterialFromId("mat_vanilla_pufferfish");
                                ItemStack newStack = LoreCraftableMaterial.spawn(clown);
                                newStack.setAmount(event.getItem().getItemStack().getAmount());
                                event.getPlayer().getInventory().addItem(newStack);
                                event.getPlayer().updateInventory();
                                event.getItem().remove();
                                event.setCancelled(true);
                            }
                        }
                    }
                }
            }
        }
    }

    /* Called when we click on an object, used for conversion to fix up reverse compat problems. */
    public void convertLegacyItem(InventoryClickEvent event) {
        ItemStack stack3 = event.getCurrentItem();
        boolean currentEmpty = (event.getCurrentItem() == null) || (stack3.getTypeId() == Material.AIR.getId());

        if (currentEmpty) {
            return;
        }

        ItemStack stack2 = event.getCurrentItem();
        if (stack2.getTypeId() == Material.SLIME_BALL.getId()) {
            LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(event.getCurrentItem());
            if (craftMat == null) {
                /* Found a vanilla slime ball. */
                LoreCraftableMaterial slime = LoreCraftableMaterial.getCraftMaterialFromId("mat_vanilla_slime");
                ItemStack newStack = LoreCraftableMaterial.spawn(slime);
                newStack.setAmount(event.getCurrentItem().getAmount());
                event.setCurrentItem(newStack);
            }
        }

        ItemStack stack1 = event.getCurrentItem();
        if (stack1.getTypeId() == Material.RAW_FISH.getId() && ItemManager.getData(event.getCurrentItem()) ==
                new MaterialData(Material.RAW_FISH.getId(), (byte) CivData.CLOWNFISH).getData()) {
            LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(event.getCurrentItem());
            if (craftMat == null) {
                /* Found a vanilla slime ball. */
                LoreCraftableMaterial clown = LoreCraftableMaterial.getCraftMaterialFromId("mat_vanilla_clownfish");
                ItemStack newStack = LoreCraftableMaterial.spawn(clown);
                newStack.setAmount(event.getCurrentItem().getAmount());
                event.setCurrentItem(newStack);
            }
        }

        ItemStack stack = event.getCurrentItem();
        if (stack.getTypeId() == Material.RAW_FISH.getId() && ItemManager.getData(event.getCurrentItem()) ==
                new MaterialData(Material.RAW_FISH.getId(), (byte) CivData.PUFFERFISH).getData()) {
            LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(event.getCurrentItem());
            if (craftMat == null) {
                /* Found a vanilla slime ball. */
                LoreCraftableMaterial clown = LoreCraftableMaterial.getCraftMaterialFromId("mat_vanilla_pufferfish");
                ItemStack newStack = LoreCraftableMaterial.spawn(clown);
                newStack.setAmount(event.getCurrentItem().getAmount());
                event.setCurrentItem(newStack);
            }
        }
    }

    /*
     * Track the location of the goodie.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void OnInventoryClick(InventoryClickEvent event) {
        ItemStack stack1 = event.getCurrentItem();
        boolean currentEmpty = (event.getCurrentItem() == null) || (stack1.getTypeId() == Material.AIR.getId());
        ItemStack stack = event.getCursor();
        boolean cursorEmpty = (event.getCursor() == null) || (stack.getTypeId() == Material.AIR.getId());

        if (currentEmpty && cursorEmpty) {
            return;
        }

        convertLegacyItem(event);

        if (event.getRawSlot() < 0) {
            //I guess this means "drop the item?"

            //CivLog.debug("GET RAW WAS NEGATIVE, cancel this event it should be invalid.");
            //event.setResult(Result.DENY);
            //event.setCancelled(true);

            //try {
            //	Player player = CivGlobal.getPlayer(event.getWhoClicked().getName());
            //	player.updateInventory();
            //} catch (CivException e) {
            //}

            return;
        }

        InventoryView view = event.getView();
        Inventory clickedInv;
        Inventory otherInv;

        if (view.getType().equals(InventoryType.CRAFTING)) {
            //This is the player's own inventory. For some reason it requires
            //special treatment. The 'top' inventory is the 2x2 crafting
            //area plus the output. During shift click, items do not go there
            //so the otherInv should always be the player's inventory aka the bottom.
            if (event.getRawSlot() <= 4) {
                clickedInv = view.getTopInventory();
                otherInv = view.getBottomInventory();
            } else {
                clickedInv = view.getBottomInventory();
                otherInv = view.getBottomInventory();
            }
        } else {
            if (event.getRawSlot() == view.convertSlot(event.getRawSlot())) {
                //Clicked in the top holder
                clickedInv = view.getTopInventory();
                otherInv = view.getBottomInventory();
            } else {
                clickedInv = view.getBottomInventory();
                otherInv = view.getTopInventory();
            }
        }

        LoreMaterial current = LoreMaterial.getMaterial(event.getCurrentItem());
        LoreMaterial cursor = LoreMaterial.getMaterial(event.getCursor());

        if (event.isShiftClick()) {
            // Shift click is _always_ current item.
            //	CustomItemStack is = new CustomItemStack(event.getCurrentItem());
            if (current != null) {
                //if (is.isCustomItem() && (is.getMaterial() instanceof CustomMaterialExtended)) {
                // Calling onInvShiftClick Event.
                //((CustomMaterialExtended)is.getMaterial()).onInvShiftClick(event, clickedInv, otherInv, is.getItem());
                current.onInvShiftClick(event, clickedInv, otherInv, event.getCurrentItem());
                //}
            }

        } else {

            if (!currentEmpty && !cursorEmpty) {
                //CustomItemStack currentIs = new CustomItemStack(event.getCurrentItem());
                //CustomItemStack cursorIs = new CustomItemStack(event.getCursor());

                if (current != null) {
                    current.onInvItemSwap(event, clickedInv, event.getCursor(), event.getCurrentItem());
                }

                if (cursor != null) {
                    cursor.onInvItemSwap(event, clickedInv, event.getCursor(), event.getCurrentItem());
                }
            } else if (!currentEmpty) {
                // This is a pickup event.
                //CustomItemStack is = new CustomItemStack(event.getCurrentItem());
                if (current != null) {
                    // Calling onInvItemPickup Event.
                    current.onInvItemPickup(event, clickedInv, event.getCurrentItem());
                }
            } else {
                // Implied !cursorEmpty
                if (cursor != null) {
                    // Calling onInvItemDrop Event.
                    cursor.onInvItemDrop(event, clickedInv, event.getCursor());
                }

            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void OnPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {

        LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(event.getPlayer().getInventory().getItemInMainHand());
        if (craftMat == null) {
            return;
        }

        craftMat.onPlayerInteractEntityEvent(event);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void OnPlayerLeashEvent(PlayerLeashEntityEvent event) {
        LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(event.getPlayer().getInventory().getItemInMainHand());
        if (craftMat == null) {
            return;
        }

        craftMat.onPlayerLeashEvent(event);
    }


    @EventHandler(priority = EventPriority.LOW)
    public void onItemDurabilityChange(PlayerItemDamageEvent event) {
        ItemStack stack = event.getItem();

        LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
        if (craftMat == null) {
            return;
        }
        craftMat.onItemDurabilityChange(event);
    }

    private static boolean isUnwantedVanillaItem(ItemStack stack) {
        if (stack == null) {
            return false;
        }

        LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
        if (craftMat != null) {
            /* Assume that if it's custom. It's good to go. */
            return false;
        }

        if (LoreGuiItem.isGUIItem(stack)) {
            return false;
        }

        ConfigRemovedRecipes removed = CivSettings.removedRecipies.get(stack.getTypeId());
        if (removed == null && !stack.getType().equals(Material.ENCHANTED_BOOK)) {
            /* Check for badly enchanted tools */
            if (stack.containsEnchantment(Enchantment.FIRE_ASPECT) &&
                    stack.getEnchantmentLevel(Enchantment.FIRE_ASPECT) > 2) {
                stack.removeEnchantment(Enchantment.FIRE_ASPECT);
                // Remove any fire aspect above this amount
            } else if (stack.containsEnchantment(Enchantment.LOOT_BONUS_MOBS) &&
                    stack.getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS) > 3) {
                stack.removeEnchantment(Enchantment.LOOT_BONUS_MOBS);
                // Only allow looting 3
            } else if (stack.containsEnchantment(Enchantment.LOOT_BONUS_BLOCKS) &&
                    stack.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS) > 3) {
                stack.removeEnchantment(Enchantment.LOOT_BONUS_BLOCKS);
                // Only allow fortune 1
            } else if (stack.containsEnchantment(Enchantment.DIG_SPEED) &&
                    stack.getEnchantmentLevel(Enchantment.DIG_SPEED) > 5) {
                stack.removeEnchantment(Enchantment.DIG_SPEED);
                // only allow effiencey 5
            } else {
                /* Not in removed list, so allow it. */
                return false;
            }
        }
        return true;
    }

    public static void removeUnwantedVanillaItems(Player player, Inventory inv) {
        if (player.isOp()) {
            /* Allow OP to carry vanilla stuff. */
            return;
        }
        boolean sentMessage = false;

        for (ItemStack stack : inv.getContents()) {
            if (!isUnwantedVanillaItem(stack)) {
                continue;
            }

            inv.remove(stack);
            CivLog.info("Removed vanilla item:" + stack + " from " + player.getName());
            if (!sentMessage) {
                CivMessage.send(player, ChatColor.GRAY + CivSettings.localize.localizedString("customItem_restrictedItemsRemoved"));
                sentMessage = true;
            }
        }

        /* Also check the player's equipt. */
        ItemStack[] contents = player.getEquipment().getArmorContents();
        boolean foundBad = false;
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null) {
                continue;
            }

            LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
            if (craftMat != null) {
                /* Assume we are good if we are custom. */
                continue;
            }

            ConfigRemovedRecipes removed = CivSettings.removedRecipies.get(stack.getTypeId());
            if (removed == null && !stack.getType().equals(Material.ENCHANTED_BOOK)) {
                /* Not in removed list, so allow it. */
                continue;
            }

            CivLog.info("Removed vanilla item:" + stack + " from " + player.getName() + " from armor.");
            contents[i] = new ItemStack(Material.AIR);
            foundBad = true;
            if (!sentMessage) {
                CivMessage.send(player, ChatColor.GRAY + CivSettings.localize.localizedString("customItem_restrictedItemsRemoved"));
                sentMessage = true;
            }
        }
        if (foundBad) {
            player.getEquipment().setArmorContents(contents);
        }

        if (sentMessage) {
            player.updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void OnInventoryHold(PlayerItemHeldEvent event) {

        ItemStack stack = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (stack == null) {
            return;
        }

        LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
        if (craftMat == null) {
            return;
        }

        craftMat.onHold(event);
    }

//	/* Prevent books from being inside an inventory. */
    /* Prevent vanilla gear from being used. */

    public LoreCraftableMaterial getCompatibleCatalyst(LoreCraftableMaterial craftMat) {
        /* Setup list of catalysts to refund. */
        LinkedList<LoreMaterial> cataList = new LinkedList<>();
        cataList.add(LoreMaterial.materialMap.get("mat_common_attack_catalyst"));
        cataList.add(LoreMaterial.materialMap.get("mat_common_defense_catalyst"));
        cataList.add(LoreMaterial.materialMap.get("mat_uncommon_attack_catalyst"));
        cataList.add(LoreMaterial.materialMap.get("mat_uncommon_defense_catalyst"));
        cataList.add(LoreMaterial.materialMap.get("mat_rare_attack_catalyst"));
        cataList.add(LoreMaterial.materialMap.get("mat_rare_defense_catalyst"));
        cataList.add(LoreMaterial.materialMap.get("mat_legendary_attack_catalyst"));
        cataList.add(LoreMaterial.materialMap.get("mat_legendary_defense_catalyst")); // TODO, ADD MYSTIC CATALYSTS (100% chance)

        for (LoreMaterial mat : cataList) {
            LoreCraftableMaterial cMat = (LoreCraftableMaterial) mat;

            Catalyst cat = (Catalyst) cMat.getComponent("Catalyst");
            String allowedMats = cat.getString("allowed_materials");
            String[] matSplit = allowedMats.split(",");

            for (String mid : matSplit) {
                if (mid.trim().equalsIgnoreCase(craftMat.getId())) {
                    return cMat;
                }
            }

        }
        return null;
    }


}
