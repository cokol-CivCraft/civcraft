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

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.object.StructureChest;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.MultiInventory;
import com.avrgaming.civcraft.util.SimpleBlock;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

public class Granary extends Structure {
    private final MultiInventory dest_inv = new MultiInventory();
    private double iron = 0.0;
    private double gold = 0.0;
    private double diamond = 0.0;
    private double emerald = 0.0;
    private double tungsten = 0.0;
    private double chromium = 0.0;

    protected Granary(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    public Granary(int id, UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(id, uuid, nbt);
    }

    public boolean isFree() {
        for (StructureChest ab : getAllChestsById(1)) {
            Chest chest = (Chest) ab.getCoord().getBlock().getState();
            if (chest.getBlockInventory().firstEmpty() >= 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onPostBuild(BlockCoord absCoord, SimpleBlock commandBlock) {
        this.getTown().addGranary(this);
    }

    @Override
    public void onDestroy() {
        this.getTown().removeGranary(this);
        super.onDestroy();
    }

    public void putResources(double iron, double gold, double diamond, double emerald, double tungsten, double chromium) {
        this.iron += iron;
        this.gold += gold;
        this.diamond += diamond;
        this.emerald += emerald;
        this.tungsten += tungsten;
        this.chromium += chromium;
    }

    private boolean initWonder() {
        if (this.getCiv().hasWonder("w_hanginggardens")) {
            for (StructureChest sch : this.getCiv().getWonder("w_hanginggardens").getAllChestsById(1)) {
                Chest chest = (Chest) sch.getCoord().getBlock().getState();
                dest_inv.addInventory(chest.getBlockInventory());
            }
        }
        return !dest_inv.isFull();
    }

    public boolean initGranary() {
        for (StructureChest sch : this.getAllChestsById(1)) {
            Chest ch = (Chest) sch.getCoord().getBlock().getState();
            dest_inv.addInventory(ch.getBlockInventory());
        }
        return dest_inv.isFull();
    }

    public void spawnResources() {
        int spawnIron = (int) Math.min(64, iron);
        int spawnGold = (int) Math.min(64, gold);
        int spawnDiamond = (int) Math.min(64, diamond);
        int spawnEmerald = (int) Math.min(64, emerald);
        int spawnTungsten = (int) Math.min(64, tungsten);
        int spawnChromium = (int) Math.min(64, chromium);
        ArrayList<ItemStack> newItems = new ArrayList<>();
        newItems.add(new ItemStack(Material.IRON_INGOT, spawnIron));
        newItems.add(new ItemStack(Material.GOLD_INGOT, spawnGold));
        newItems.add(new ItemStack(Material.DIAMOND, spawnDiamond));
        newItems.add(new ItemStack(Material.EMERALD, spawnEmerald));
        newItems.add(LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_tungsten_ore"), spawnTungsten));
        newItems.add(LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_chromium_ore"), spawnChromium));
        if (!initGranary() && !initWonder()) {
            this.getTown().saveGranaryResources(this.iron, this.gold, this.diamond, this.emerald, this.tungsten, this.chromium);
        } else {
            for (ItemStack newItem : newItems) {
                dest_inv.addItemStack(newItem);
            }
            this.iron -= spawnIron;
            this.gold -= spawnGold;
            this.diamond -= spawnDiamond;
            this.emerald -= spawnEmerald;
            this.tungsten -= spawnTungsten;
            this.chromium -= spawnChromium;
        }
    }

    public String getResources() {
        return ChatColor.DARK_GRAY.toString() + this.iron + ChatColor.LIGHT_PURPLE + " / "
                + ChatColor.GOLD + this.gold + ChatColor.LIGHT_PURPLE + " / "
                + ChatColor.AQUA + this.diamond + ChatColor.LIGHT_PURPLE + " / "
                + ChatColor.GREEN + this.emerald + ChatColor.LIGHT_PURPLE + " / "
                + ChatColor.BLUE + this.tungsten + ChatColor.LIGHT_PURPLE + " / "
                + ChatColor.GRAY + this.chromium;
    }

}
