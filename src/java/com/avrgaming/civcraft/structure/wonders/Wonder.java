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
package com.avrgaming.civcraft.structure.wonders;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuff;
import com.avrgaming.civcraft.config.ConfigWonderBuff;
import com.avrgaming.civcraft.database.SQLController;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TradeGood;
import com.avrgaming.civcraft.structure.MetaStructure;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.util.BlockCoord;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.UUID;

public abstract class Wonder extends MetaStructure {
    private ConfigWonderBuff wonderBuffs = null;
    protected BlockCoord tradeOutpostTower = null;
    protected TradeGood good = null;
    protected BonusGoodie goodie = null;

    public Wonder(UUID uuid, NBTTagCompound nbt) throws SQLException, CivException {
        super(uuid, nbt);
    }

    public Wonder(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    public void loadSettings() {
        wonderBuffs = CivSettings.wonderBuffs.get(this.getConfigId());

        if (this.isComplete() && this.isActive()) {
            this.addWonderBuffsToTown();
        }
    }

    @Override
    public void save() {
        SQLUpdate.add(this);
    }


    @Override
    public void delete() throws SQLException {
        super.delete();

        if (this.wonderBuffs != null) {
            for (ConfigBuff buff : this.wonderBuffs.buffs) {
                this.getTown().getBuffManager().removeBuff(buff.id);
            }
        }

        SQLController.deleteNamedObject(this, TABLE_NAME);
        CivGlobal.removeWonder(this);
    }

    public static boolean isWonderAvailable(String configId) {
        if (CivGlobal.isCasualMode()) {
            return true;
        }

        for (Wonder wonder : CivGlobal.getWonders()) {
            if (!wonder.getConfigId().equals(configId)) {
                continue;
            }
            if (wonder.isNationalWonder()) {
                return true;
            }
            if (wonder.isComplete()) {
                return false;
            }
        }

        return true;
    }


    @Override
    public void processUndo() throws CivException {
        this.undoFromTemplate();

        CivMessage.global(CivSettings.localize.localizedString("var_wonder_undo_broadcast", (ChatColor.GREEN + this.getDisplayName() + ChatColor.WHITE), this.getTown().getName(), this.getTown().getCiv().getName()));

        double refund = this.getCost();
        this.getTown().depositDirect(refund);
        CivMessage.sendTown(getTown(), CivSettings.localize.localizedString("var_structure_undo_refund", this.getTown().getName(), refund, CivSettings.CURRENCY_NAME));

        this.unbindStructureBlocks();

        try {
            delete();
            getTown().removeWonder(this);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CivException(CivSettings.localize.localizedString("internalDatabaseException"));
        }
    }

    @Override
    public void build(Player player, Location centerLoc, Template tpl) throws Exception {

        // We take the player's current position and make it the 'center' by moving the center location
        // to the 'corner' of the structure.
        Location savedLocation = centerLoc.clone();

        centerLoc = this.repositionCenter(centerLoc, tpl.dir(), tpl.size_x, tpl.size_z);
        Block centerBlock = centerLoc.getBlock();
        // Before we place the blocks, give our build function a chance to work on it

        this.setTotalBlockCount(tpl.size_x * tpl.size_y * tpl.size_z);
        // Save the template x,y,z for later. This lets us know our own dimensions.
        // this is saved in the db so it remains valid even if the template changes.
        this.setTemplateName(tpl.getFilepath());
        this.setTemplateX(tpl.size_x);
        this.setTemplateY(tpl.size_y);
        this.setTemplateZ(tpl.size_z);
        this.setCorner(new BlockCoord(centerLoc));

        checkBlockPermissionsAndRestrictions(player, centerBlock, tpl.size_x, tpl.size_y, tpl.size_z, savedLocation);
        this.runOnBuildStart(centerLoc, tpl);

        // Setup undo information
        getTown().lastBuildableBuilt = this;
        tpl.saveUndoTemplate(this.getCorner().toString(), this.getTown().getName(), centerLoc);
        tpl.buildScaffolding(centerLoc);

        // Player's center was converted to this building's corner, save it as such.
        this.startBuildTask(tpl, centerLoc);

        this.save();
        CivGlobal.addWonder(this);
        CivMessage.global(CivSettings.localize.localizedString("var_wonder_startedByCiv", this.getCiv().getName(), this.getDisplayName(), this.getTown().getName()));
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 1.25f, 0.75f);
        }
    }


    @Override
    protected void runOnBuildStart(Location centerLoc, Template tpl) {
    }

    public void onDestroy() {
        if (CivGlobal.isCasualMode()) {
            return;
        }
        //can be overriden in subclasses.
        CivMessage.global(CivSettings.localize.localizedString("var_wonder_destroyed", this.getDisplayName(), this.getTown().getName()));
        try {
            this.getTown().removeWonder(this);
            this.fancyDestroyStructureBlocks();
            this.unbindStructureBlocks();
            this.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addWonderBuffsToTown() {

        if (this.wonderBuffs == null) {
            return;
        }

        for (ConfigBuff buff : this.wonderBuffs.buffs) {
            try {
                this.getTown().getBuffManager().addBuff("wonder:" + this.getDisplayName() + ":" + this.getCorner() + ":" + buff.id,
                        buff.id, this.getDisplayName());
            } catch (CivException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onComplete() {
        addWonderBuffsToTown();
    }

    public void build_trade_outpost_tower() throws CivException {
    }

    public void setWonderTower(BlockCoord absCoord) {
        tradeOutpostTower = absCoord;
    }

    public ConfigWonderBuff getWonderBuffs() {
        return wonderBuffs;
    }


    public void setWonderBuffs(ConfigWonderBuff wonderBuffs) {
        this.wonderBuffs = wonderBuffs;
    }

    @Override
    public void onLoad() {
    }

    @Override
    public void onUnload() {
    }

    protected void addBuffToTown(Town town, String id) {
        try {
            town.getBuffManager().addBuff(id, id, this.getDisplayName() + " in " + this.getTown().getName());
        } catch (CivException e) {
            e.printStackTrace();
        }
    }

    protected void addBuffToCiv(Civilization civ, String id) {
        for (Town town : civ.getTowns()) {
            addBuffToTown(town, id);
        }
    }

    protected void removeBuffFromTown(Town town, String id) {
        town.getBuffManager().removeBuff(id);
    }

    protected void removeBuffFromCiv(Civilization civ, String id) {
        for (Town town : civ.getTowns()) {
            removeBuffFromTown(town, id);
        }
    }

    protected abstract void removeBuffs();

    protected abstract void addBuffs();

    public void processCoinsFromCulture() {
        int cultureCount = 0;
        for (Town town : this.getCiv().getTowns()) {
            cultureCount += town.getCultureChunks().size();
        }

        double coinsPerCulture = Double.parseDouble(CivSettings.buffs.get("buff_colossus_coins_from_culture").value);

        double total = coinsPerCulture * cultureCount;
        this.getCiv().getTreasury().deposit(total);

        CivMessage.sendCiv(this.getCiv(), ChatColor.GREEN + CivSettings.localize.localizedString("var_colossus_generatedCoins", (String.valueOf(ChatColor.YELLOW) + total + ChatColor.GREEN), CivSettings.CURRENCY_NAME, cultureCount));
    }

    public void processCoinsFromColosseum() {
        int townCount = 0;
        for (Civilization civ : Civilization.getCivs()) {
            townCount += civ.getTownCount();
        }
        double coinsPerTown = Double.parseDouble(CivSettings.buffs.get("buff_colosseum_coins_from_towns").value);

        double total = coinsPerTown * townCount;
        this.getCiv().getTreasury().deposit(total);

        CivMessage.sendCiv(this.getCiv(), ChatColor.GREEN + CivSettings.localize.localizedString("var_colosseum_generatedCoins", (String.valueOf(ChatColor.YELLOW) + total + ChatColor.GREEN), CivSettings.CURRENCY_NAME, townCount));
    }
}
