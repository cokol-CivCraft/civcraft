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

import com.avrgaming.civcraft.components.NonMemberFeeComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.components.Catalyst;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.tasks.NotificationTask;
import com.avrgaming.civcraft.util.BukkitObjects;
import com.avrgaming.civcraft.util.TimeTools;
import gpl.AttributeUtil;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class Blacksmith extends Structure {

    private static final long COOLDOWN = 5;
    //private static final double BASE_CHANCE = 0.8;
    public static int SMELT_TIME_SECONDS = 3600 * 3;
    public static double YIELD_RATE = 1.25;

    private Date lastUse = new Date();

    private final NonMemberFeeComponent nonMemberFeeComponent;


    protected Blacksmith(Location center, String id, Town town)
            throws CivException {
        super(center, id, town);
        nonMemberFeeComponent = new NonMemberFeeComponent(this);
        nonMemberFeeComponent.onSave();
    }

    private long getCooldown() {
        return CivSettings.structureConfig.getInt("blacksmith.cooldown", 1);
    }

    public Blacksmith(UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(uuid, nbt);
        nonMemberFeeComponent = new NonMemberFeeComponent(this);
        nonMemberFeeComponent.onLoad();
    }

    public double getNonResidentFee() {
        return nonMemberFeeComponent.getFeeRate();
    }

    public void setNonResidentFee(double nonResidentFee) {
        this.nonMemberFeeComponent.setFeeRate(nonResidentFee);
    }

    private String getNonResidentFeeString() {
        return CivSettings.localize.localizedString("Fee:") + " " + ((int) (this.nonMemberFeeComponent.getFeeRate() * 100) + "%");
    }

    @Override
    public void processSignAction(Player player, StructureSign sign, PlayerInteractEvent event) throws CivException {
        int special_id = Integer.parseInt(sign.getAction());
        Date now = new Date();
        long diff = (now.getTime() - lastUse.getTime()) / 1000;

        if (diff < getCooldown()) {
            throw new CivException(CivSettings.localize.localizedString("var_blacksmith_onCooldown", (Blacksmith.COOLDOWN - diff)));
        }

        lastUse = now;

        switch (special_id) {
            case 0 -> this.deposit_forge(player);
            case 1 ->
                    this.perform_forge(player, CivSettings.structureConfig.getDouble("blacksmith.forge_cost", 2500.0));
            case 2 -> this.depositSmelt(player, player.getInventory().getItemInMainHand());
            case 3 -> this.withdrawSmelt(player);
        }

    }

    @Override
    public void updateSignText() {
        double cost = CivSettings.structureConfig.getDouble("blacksmith.forge_cost", 2500.0);

        for (StructureSign sign : getSigns()) {
            switch (Integer.parseInt(sign.getAction())) {
                case 0 -> sign.setText(CivSettings.localize.localizedString("blacksmith_sign_catalyst"));
                case 1 ->
                        sign.setText(CivSettings.localize.localizedString("blacksmith_sign_forgeCost") + " " + cost + CivSettings.CURRENCY_NAME + "\n" +
                                getNonResidentFeeString());
                case 2 -> sign.setText(CivSettings.localize.localizedString("blacksmith_sign_depositOre"));
                case 3 -> sign.setText(CivSettings.localize.localizedString("blacksmith_sign_withdrawOre"));
            }

            sign.update();
        }
    }

    public String getkey(Player player, Structure struct, String tag) {
        return player.getUniqueId().toString() + "_" + struct.getConfigId() + "_" + struct.getCorner().toString() + "_" + tag;
    }

    public void saveItem(ItemStack item, String key) {

        StringBuilder value = new StringBuilder(item.getType() + ":");

        for (Enchantment e : item.getEnchantments().keySet()) {
            value.append(e.getName()).append(",").append(item.getEnchantmentLevel(e));
            value.append(":");
        }

        sessionAdd(key, value.toString());
    }

    public void saveCatalyst(LoreCraftableMaterial craftMat, String key) {
        String value = craftMat.getConfigId();
        sessionAdd(key, value);
    }

    public static boolean canSmelt(Material material) {
        return switch (material) {
            case GOLD_ORE, IRON_ORE -> true;
            default -> false;
        };
    }

    /*
     * Converts the ore id's into the ingot id's
     */
    public static Material convertType(Material bmaterial) {
        return switch (bmaterial) {
            case GOLD_ORE -> Material.GOLD_INGOT;
            case IRON_ORE -> Material.IRON_INGOT;
            default -> null;
        };
    }

    /*
     * Deposit forge will take the current item in the player's hand
     * and deposit its information into the sessionDB. It will store the
     * item's id, data, and damage.
     */
    public void deposit_forge(Player player) throws CivException {

        ItemStack item = player.getInventory().getItemInMainHand();

        String key = this.getkey(player, this, "forge");
        ArrayList<SessionEntry> sessions = CivGlobal.getSessionDB().lookup(key);

        if (sessions == null || sessions.isEmpty()) {
            /* Validate that the item being added is a catalyst */
            LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(item);
            if (craftMat == null || !craftMat.hasComponent("Catalyst")) {
                throw new CivException(CivSettings.localize.localizedString("blacksmith_deposit_notCatalyst"));
            }

            /* Item is a catalyst. Add it to the session DB. */
            saveCatalyst(craftMat, key);
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                //	player.getInventory().remove(item);
            }
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);

            CivMessage.sendSuccess(player, CivSettings.localize.localizedString("blacksmith_deposit_success"));
        } else {
            /* Catalyst already in blacksmith, withdraw it. */
            LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterialFromId(sessions.get(0).value);
            if (craftMat == null) {
                throw new CivException(CivSettings.localize.localizedString("blacksmith_deposit_errorWithdraw"));
            }

            ItemStack stack = LoreMaterial.spawn(craftMat);
            HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
            if (!leftovers.isEmpty()) {
                for (ItemStack is : leftovers.values()) {
                    player.getWorld().dropItem(player.getLocation(), is);
                }
            }
            CivGlobal.getSessionDB().delete_all(key);
            CivMessage.sendSuccess(player, CivSettings.localize.localizedString("blacksmith_deposit_withdrawSuccess"));
        }
    }

    /*
     * Perform forge will perform the over-enchantment algorithm and determine
     * if this player is worthy of a higher level pick. If successful it will
     * give the player the newly created pick.
     */
    public void perform_forge(Player player, double cost) throws CivException {

        /* Try and retrieve any catalyst in the forge. */
        String key = getkey(player, this, "forge");
        ArrayList<SessionEntry> sessions = CivGlobal.getSessionDB().lookup(key);

        /* Search for free catalyst. */
        AttributeUtil attrs = new AttributeUtil(player.getInventory().getItemInMainHand());
        Catalyst catalyst;


        String freeStr = attrs.getCivCraftProperty("freeCatalyst");
        if (freeStr == null) {
            /* No free enhancements on item, search for catalyst. */
            if (sessions == null || sessions.isEmpty()) {
                throw new CivException(CivSettings.localize.localizedString("blacksmith_forge_noCatalyst"));
            }

            LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterialFromId(sessions.get(0).value);
            if (craftMat == null) {
                throw new CivException(CivSettings.localize.localizedString("blacksmith_forge_missingCatalyst"));
            }

            catalyst = (Catalyst) craftMat.getComponent("Catalyst");
            if (catalyst == null) {
                throw new CivException(CivSettings.localize.localizedString("blacksmith_forge_missingCatalyst"));
            }
        } else {
            String[] split = freeStr.split(":");
            double level = Double.parseDouble(split[0]);
            String mid = split[1];

            LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterialFromId(mid);
            if (craftMat == null) {
                throw new CivException(CivSettings.localize.localizedString("blacksmith_forge_missingCatalyst"));
            }

            catalyst = (Catalyst) craftMat.getComponent("Catalyst");
            if (catalyst == null) {
                throw new CivException(CivSettings.localize.localizedString("blacksmith_forge_missingCatalyst"));
            }

            /* reduce level and reset item. */
            level--;

            String[] lore = attrs.getLore();
            for (int i = 0; i < lore.length; i++) {
                String str = lore[i];
                if (str.contains("free enhancements")) {
                    if (level != 0) {
                        lore[i] = ChatColor.AQUA + CivSettings.localize.localizedString("var_blacksmith_forge_loreFreeEnchancements", level);
                    } else {
                        lore[i] = "";
                    }
                    break;
                }
            }
            attrs.setLore(lore);

            if (level != 0) {
                attrs.setCivCraftProperty("freeCatalyst", level + ":" + mid);
            } else {
                attrs.removeCivCraftProperty("freeCatalyst");
            }

            player.getInventory().setItemInMainHand(attrs.getStack());

        }

        ItemStack enhancedItem = catalyst.getEnchantedItem(player.getInventory().getItemInMainHand());

        if (enhancedItem == null) {
            throw new CivException(CivSettings.localize.localizedString("blacksmith_forge_invalidItem"));
        }

        /* Consume the enhancement. */
        CivGlobal.getSessionDB().delete_all(key);

        if (!catalyst.enchantSuccess(enhancedItem)) {
            /*
             * There is a one in third chance that our item will break.
             * Sucks, but this is what happened here.
             */
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR, 1, (short) 0));
            CivMessage.sendError(player, CivSettings.localize.localizedString("blacksmith_forge_failed"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_HURT, 1.0f, 1.0f);
        } else {
            player.getInventory().setItemInMainHand(enhancedItem);
            CivMessage.sendSuccess(player, CivSettings.localize.localizedString("blacksmith_forge_success"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
        }
    }

    /*
     * Take the itemstack in hand and deposit it into
     * the session DB.
     */
    public void depositSmelt(Player player, ItemStack itemsInHand) throws CivException {

        // Make sure that the item is a valid smelt type.
        if (!Blacksmith.canSmelt(itemsInHand.getType())) {
            throw new CivException(CivSettings.localize.localizedString("blacksmith_smelt_onlyOres"));
        }

        // Only members can use the smelter
        Resident res = CivGlobal.getResident(player.getName());
        if (!res.hasTown() || this.getTown() != res.getTown()) {
            throw new CivException(CivSettings.localize.localizedString("blacksmith_smelt_notMember"));
        }

        String value = convertType(itemsInHand.getType()) + ":" + (itemsInHand.getAmount() * Blacksmith.YIELD_RATE);
        String key = getkey(player, this, "smelt");

        // Store entry in session DB
        sessionAdd(key, value);

        // Take ore away from player.
        player.getInventory().removeItem(itemsInHand);
        //BukkitTools.sch
        // Schedule a message to notify the player when the smelting is finished.
        BukkitObjects.scheduleAsyncDelayedTask(new NotificationTask(player.getName(),
                        ChatColor.GREEN + CivSettings.localize.localizedString("var_blacksmith_smelt_asyncNotify", itemsInHand.getAmount(), CivData.getDisplayName(itemsInHand.getType()))),
                TimeTools.toTicks(SMELT_TIME_SECONDS));

        CivMessage.send(player, ChatColor.GREEN + CivSettings.localize.localizedString("var_blacksmith_smelt_depositSuccess", itemsInHand.getAmount(), CivData.getDisplayName(itemsInHand.getType())));

        player.updateInventory();
    }


    /*
     * Queries the sessionDB for entries for this player
     * When entries are found, their inserted time is compared to
     * the current time, if they have been in long enough each
     * itemstack is sent to the players inventory.
     *
     * For each itemstack ready to withdraw try to place it in the
     * players inventory. If there is not enough space, take the
     * leftovers and place them back in the sessionDB.
     * If there are no leftovers, delete the sessionDB entry.
     */
    public void withdrawSmelt(Player player) throws CivException {

        String key = getkey(player, this, "smelt");

        // Only members can use the smelter
        Resident res = CivGlobal.getResident(player.getName());
        if (!res.hasTown() || this.getTown() != res.getTown()) {
            throw new CivException(CivSettings.localize.localizedString("blacksmith_smelt_notMember"));
        }

        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(key);

        if (entries == null || entries.isEmpty()) {
            throw new CivException(CivSettings.localize.localizedString("blacksmith_smelt_nothingInSmelter"));
        }

        Inventory inv = player.getInventory();

        for (SessionEntry se : entries) {
            String[] split = se.value.split(":");
            Material itemId = Material.getMaterial(split[0]);
            double amount = Double.parseDouble(split[1]);
            long now = System.currentTimeMillis();
            int secondsBetween = CivGlobal.getSecondsBetween(se.time, now);

            // First determine the time between two events.
            if (secondsBetween < Blacksmith.SMELT_TIME_SECONDS) {
                DecimalFormat df1 = new DecimalFormat("0.##");

                double timeLeft = ((double) Blacksmith.SMELT_TIME_SECONDS - (double) secondsBetween) / (double) 60;
                //Date finish = new Date(now+(secondsBetween*1000));
                CivMessage.send(player, ChatColor.YELLOW + CivSettings.localize.localizedString("var_blacksmith_smelt_inProgress1", amount, CivData.getDisplayName(itemId), df1.format(timeLeft)));
                continue;
            }

            ItemStack stack = new ItemStack(itemId, (int) amount, (short) 0);
            HashMap<Integer, ItemStack> leftovers = inv.addItem(stack);

            // If this stack was successfully withdrawn, delete it from the DB.
            if (leftovers.isEmpty()) {
                CivGlobal.getSessionDB().delete(se.request_id, se.key);
                CivMessage.send(player, CivSettings.localize.localizedString("var_cmd_civ_withdrawSuccess", amount, CivData.getDisplayName(itemId)));

                break;
            }
            // We do not have space in our inventory, inform the player.
            CivMessage.send(player, ChatColor.RED + CivSettings.localize.localizedString("blacksmith_smelt_notEnoughInvenSpace"));

            // If the leftover size is the same as the size we are trying to withdraw, do nothing.
            int leftoverAmount = CivGlobal.getLeftoverSize(leftovers);

            if (leftoverAmount == amount) {
                continue;
            }

            if (leftoverAmount == 0) {
                //just in case we somehow get an entry with 0 items in it.
                CivGlobal.getSessionDB().delete(se.request_id, se.key);
                continue;
            }
            // Some of the items were deposited into the players inventory but the sessionDB
            // still has the full amount stored, update the db to only contain the leftovers.
            CivGlobal.getSessionDB().update(se.request_id, se.key, itemId + ":" + leftoverAmount);

            // only withdraw one item at a time.
            break;
        }

        player.updateInventory();
    }

}
