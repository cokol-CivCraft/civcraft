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

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.StructureChest;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.wonders.TheHangingGardens;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.MultiInventory;
import com.avrgaming.civcraft.util.SimpleBlock;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class Granary extends Structure {
    private final MultiInventory dest_inv = new MultiInventory();
    private double iron = 0.0;
    private double gold = 0.0;
    private double diamond = 0.0;
    private double emerald = 0.0;
    private double tungsten = 0.0;
    private double chromium = 0.0;

    protected Granary(Location center, String id, Town town)
            throws CivException {
        super(center, id, town);
    }

    public Granary(ResultSet rs) throws SQLException, CivException {
        super(rs);
    }

    @Override
    public void onPostBuild(BlockCoord absCoord, SimpleBlock commandBlock) {
        this.getTown().addGranary(this);
    }

    @Override
    public void onDestroy() {
        //can be overriden in subclasses.
        CivMessage.global(CivSettings.localize.localizedString("var_buildable_destroyedAlert", this.getDisplayName(), this.getTown().getName()));
        this.hitpoints = 0;
        this.getTown().removeGranary(this);
        this.fancyDestroyStructureBlocks();
        this.save();
    }

    public void putResources(double i1, double i2, double i3, double i4, double i5, double i6) {
        this.iron += i1;
        this.gold += i2;
        this.diamond += i3;
        this.emerald += i4;
        this.tungsten += i5;
        this.chromium += i6;
    }

    private boolean initWonder() {
        if (this.getCiv().hasWonder("w_hanginggardens")) {
            TheHangingGardens thg = (TheHangingGardens) this.getCiv().getWonder("w_hanginggardens");
            for (StructureChest sch : thg.getAllChestsById(1)) {
                Chest ch = (Chest) sch.getCoord().getBlock().getState();
                Inventory tmp = ch.getBlockInventory();
                dest_inv.addInventory(tmp);
            }
        }
        return !dest_inv.isFull();
    }

    public boolean initGranary() {
        for (StructureChest sch : this.getAllChestsById(1)) {
            Chest ch = (Chest) sch.getCoord().getBlock().getState();
            Inventory tmp = ch.getBlockInventory();
            dest_inv.addInventory(tmp);
        }
        return dest_inv.isFull();
    }

    public void spawnResources() {
        ArrayList<ItemStack> newItems = new ArrayList<>();
        int spawnIron = (int) Math.min(64, iron);
        int spawnGold = (int) Math.min(64, gold);
        int spawnDiamond = (int) Math.min(64, diamond);
        int spawnEmerald = (int) Math.min(64, emerald);
        int spawnTungsten = (int) Math.min(64, tungsten);
        int spawnChromium = (int) Math.min(64, chromium);
        ItemStack ir = new ItemStack(Material.IRON_INGOT, spawnIron);
        ItemStack gd = new ItemStack(Material.GOLD_INGOT, spawnGold);
        ItemStack dm = new ItemStack(Material.DIAMOND, spawnDiamond);
        ItemStack em = new ItemStack(Material.EMERALD, spawnEmerald);
        ItemStack tu = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_tungsten_ore"), spawnTungsten);
        ItemStack chr = LoreMaterial.spawn(LoreMaterial.materialMap.get("mat_chromium_ore"), spawnChromium);
        newItems.add(ir);
        newItems.add(gd);
        newItems.add(dm);
        newItems.add(em);
        newItems.add(tu);
        newItems.add(chr);
        if (initGranary() || initWonder()) {
            for (ItemStack newItem : newItems) {
                dest_inv.addItemStack(newItem);
            }
            this.iron -= spawnIron;
            this.gold -= spawnGold;
            this.diamond -= spawnDiamond;
            this.emerald -= spawnEmerald;
            this.tungsten -= spawnTungsten;
            this.chromium -= spawnChromium;
        } else {
            this.getTown().saveGranaryResources(this.iron, this.gold, this.diamond, this.emerald, this.tungsten, this.chromium);
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
