/*************************************************************************
 * 
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_12_R1.util.HashTreeSet;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.components.NonMemberFeeComponent;
import com.avrgaming.civcraft.components.SignSelectionActionInterface;
import com.avrgaming.civcraft.components.SignSelectionComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigStableHorse;
import com.avrgaming.civcraft.config.ConfigStableItem;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;

import gpl.HorseModifier;
import gpl.HorseModifier.HorseType;
import gpl.HorseModifier.HorseVariant;

public class Stable extends Structure {

	public static Integer FEE_MIN = 5;
	public static Integer FEE_MAX = 100;
	private HashMap<Integer, SignSelectionComponent> signSelectors = new HashMap<Integer, SignSelectionComponent>();
	private BlockCoord horseSpawnCoord;
	private BlockCoord muleSpawnCoord;
	private NonMemberFeeComponent nonMemberFeeComponent;
	
	public HashTreeSet<ChunkCoord> chunks = new HashTreeSet<ChunkCoord>();
	public static Map<ChunkCoord, Stable> stableChunks = new ConcurrentHashMap<ChunkCoord, Stable>();
	
	public Stable(ResultSet rs) throws SQLException, CivException {
		super(rs);
		nonMemberFeeComponent = new NonMemberFeeComponent(this);
		nonMemberFeeComponent.onLoad();
	}

	protected Stable(Location center, String id, Town town) throws CivException {
		super(center, id, town);
		nonMemberFeeComponent = new NonMemberFeeComponent(this);
		nonMemberFeeComponent.onSave();
	}
	
	public void bindStableChunks() {
		for (BlockCoord bcoord : this.structureBlocks.keySet()) {
			ChunkCoord coord = new ChunkCoord(bcoord);
			this.chunks.add(coord);
			stableChunks.put(coord, this);
		}
	}
	
	public void unbindStableChunks() {
		for (ChunkCoord coord : this.chunks) {
			stableChunks.remove(coord);
		}
		this.chunks.clear();
	}
	
	@Override
	public void onComplete() {
		bindStableChunks();
	}
	
	@Override
	public void onLoad() throws CivException {
		bindStableChunks();
	}
	
	@Override
	public void delete() throws SQLException {
		super.delete();
		unbindStableChunks();
	}
	
	public void loadSettings() {
		super.loadSettings();

		SignSelectionComponent horseVender = new SignSelectionComponent();
		SignSelectionComponent muleVender = new SignSelectionComponent();
		SignSelectionComponent itemVender = new SignSelectionComponent();
		
		signSelectors.put(0, horseVender);
		signSelectors.put(1, muleVender);
		signSelectors.put(2, itemVender);

		class buyHorseAction implements SignSelectionActionInterface {
			int horse_id;
			double cost;
			
			public buyHorseAction(int horse_id, double cost) {
				this.horse_id = horse_id;
				this.cost = cost;
			}
			
			@Override
			public void process(Player player) {
				ConfigStableHorse horse = CivSettings.horses.get(horse_id);
				if (horse == null) {
					CivMessage.sendError(player, CivSettings.localize.localizedString("stable_unknownHorse"));
					return;
				}
				if (horse_id >= 5 && !getCiv().hasTechnology("tech_military_science"))
				{
					CivMessage.sendError(player, CivSettings.localize.localizedString("stable_missingTech_MilitaryScience"));
					return;
				}
				
				Resident resident = CivGlobal.getResident(player);
				if (!horse.mule) {
					boolean allow = false;
					for (BonusGoodie goodie : getTown().getBonusGoodies()) {
						if (goodie.getConfigTradeGood().id.equals("good_horses")) {
							allow = true;
							break;
						}
					}
					
					if (!allow) {
						CivMessage.sendError(player, CivSettings.localize.localizedString("stable_townNoHorses"));
						return;
					}
				}
			
				double paid;
				if (resident.getTown() != getTown()) {
					if (!resident.getTreasury().hasEnough(getItemCost(cost))) {
						CivMessage.sendError(player, CivSettings.localize.localizedString("var_config_marketItem_notEnoughCurrency",(getItemCost(cost)+" "+CivSettings.CURRENCY_NAME)));

						return;
					}
					
					
					resident.getTreasury().withdraw(getItemCost(cost));
					getTown().depositTaxed(getFeeToTown(cost));
					CivMessage.send(player, CivColor.Yellow+CivSettings.localize.localizedString("var_taxes_paid",getFeeToTown(cost),CivSettings.CURRENCY_NAME));
					paid = getItemCost(cost);
				} else {
					if (!resident.getTreasury().hasEnough(cost)) {
						CivMessage.sendError(player, CivSettings.localize.localizedString("var_config_marketItem_notEnoughCurrency",(cost+" "+CivSettings.CURRENCY_NAME)));
						return;
					}

					resident.getTreasury().withdraw(cost);
					paid = cost;
				}	

				HorseModifier mod;	
				if (!horse.mule) {			
					mod = HorseModifier.spawn(horseSpawnCoord.getLocation());
					mod.setType(HorseType.NORMAL);
					mod.setTamed(true);
					mod.setSaddled(true);
				} else {
					mod = HorseModifier.spawn(muleSpawnCoord.getLocation());
					mod.setType(HorseType.MULE);
				}
				
				mod.setVariant(HorseVariant.valueOf(horse.variant));
				HorseModifier.setHorseSpeed(mod.getHorse(), horse.speed);
				((Horse)mod.getHorse()).setJumpStrength(horse.jump);
				((Horse)mod.getHorse()).getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(horse.health);
				((Horse)mod.getHorse()).setHealth(horse.health);
				((Horse)mod.getHorse()).setOwner(player);
				((Horse)mod.getHorse()).setCustomName(horse.name);
				((Horse)mod.getHorse()).setCustomNameVisible(true);
				
				CivMessage.send(player, CivColor.LightGreen+CivSettings.localize.localizedString("var_stable_buySuccess",paid,CivSettings.CURRENCY_NAME));
			}
		}
		
		class buyItemAction implements SignSelectionActionInterface {

			int item_id;
			double cost;
			
			public buyItemAction(int item_id, double cost) {
				this.item_id = item_id;
				this.cost = cost;
			}
			
			@Override
			public void process(Player player) {

				Resident resident = CivGlobal.getResident(player);
				if ((item_id >= 417 && item_id <= 419)  && !getCiv().hasTechnology("tech_military_science"))
				{
					CivMessage.sendError(player, CivSettings.localize.localizedString("stable_missingTech_MilitaryScience"));
					return;
				}
				
				double paid;
				if (resident.getTown() != getTown()) {
					if (!resident.getTreasury().hasEnough(getItemCost(cost))) {
						CivMessage.sendError(player, CivSettings.localize.localizedString("var_config_marketItem_notEnoughCurrency",(getItemCost(cost)+" "+CivSettings.CURRENCY_NAME)));
						return;
					}
					
					resident.getTreasury().withdraw(getItemCost(cost));
					CivMessage.send(player, CivColor.Yellow+CivSettings.localize.localizedString("var_taxes_paid",getFeeToTown(cost),CivSettings.CURRENCY_NAME));
					paid = getItemCost(cost);
				} else {
					if (!resident.getTreasury().hasEnough(cost)) {
						CivMessage.sendError(player, CivSettings.localize.localizedString("var_config_marketItem_notEnoughCurrency",(cost+" "+CivSettings.CURRENCY_NAME)));
						return;
					}
					
					resident.getTreasury().withdraw(cost);
					paid = cost;
				}

				HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(ItemManager.createItemStack(item_id, 1));
				if (leftovers.size() > 0) {
					for (ItemStack stack : leftovers.values()) {
						player.getWorld().dropItem(player.getLocation(), stack);
					}
				}
				
				CivMessage.send(player, CivColor.LightGreen+CivSettings.localize.localizedString("var_stable_buySuccess",paid,CivSettings.CURRENCY_NAME));
			}
			
		}
		
		for (ConfigStableItem item : CivSettings.stableItems) {
			SignSelectionComponent comp = signSelectors.get(item.store_id);
			if (comp == null) {
				continue;
			}
			if (item.item_id == 0) {
				comp.addItem(new String[] {CivColor.LightGreen+item.name, CivSettings.localize.localizedString("stable_sign_buyFor"), ""+item.cost, CivSettings.localize.localizedString("Fee:")+this.nonMemberFeeComponent.getFeeString()}, new buyHorseAction(item.horse_id, item.cost));
			} else {
				comp.addItem(new String[] {CivColor.LightGreen+item.name, CivSettings.localize.localizedString("stable_sign_buyFor"), ""+item.cost, CivSettings.localize.localizedString("Fee:")+this.nonMemberFeeComponent.getFeeString()}, new buyItemAction(item.item_id, item.cost));			
			}
		}
	}
	
	private double getItemCost(double cost) {
		return cost + getFeeToTown(cost);
	}
	
	private double getFeeToTown(double cost) {
		return cost*this.nonMemberFeeComponent.getFeeRate();
	}
	
	@Override
	public void processSignAction(Player player, StructureSign sign, PlayerInteractEvent event) {
		SignSelectionComponent signSelection = signSelectors.get(Integer.valueOf(sign.getAction()));
		if (signSelection == null) {
			CivLog.warning("No sign seletor component for with id:"+sign.getAction());
			return;
		}

		switch (sign.getType()) {
		case "prev":
			signSelection.processPrev();
			break;
		case "next":
			signSelection.processNext();
			break;
		case "item":
			signSelection.processAction(player);
			break;
		}
	}
	
	@Override
	public void updateSignText() {
		for (SignSelectionComponent comp : signSelectors.values()) {
			comp.setMessageAllItems(3, CivSettings.localize.localizedString("Fee:")+" "+this.nonMemberFeeComponent.getFeeString());
		}
	}
	
	@Override
	public void onPostBuild(BlockCoord absCoord, SimpleBlock sb) {
		StructureSign structSign;
		int selectorIndex;
		SignSelectionComponent signComp;
		
		switch (sb.command) {
		case "/prev":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());
			structSign = new StructureSign(absCoord, this);
			structSign.setText("\n"+ChatColor.BOLD+ChatColor.UNDERLINE+CivSettings.localize.localizedString("stable_sign_previousUnit"));
			structSign.setDirection(sb.getData());
			structSign.setAction(sb.keyvalues.get("id"));
			structSign.setType("prev");
			structSign.update();
			this.addStructureSign(structSign);
			CivGlobal.addStructureSign(structSign);			
			break;
		case "/item":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());

			structSign = new StructureSign(absCoord, this);
			structSign.setText("");
			structSign.setDirection(sb.getData());
			structSign.setAction(sb.keyvalues.get("id"));
			structSign.setType("item");
			structSign.update();
						
			this.addStructureSign(structSign);
			CivGlobal.addStructureSign(structSign);
						
			selectorIndex = Integer.valueOf(sb.keyvalues.get("id"));
			signComp = signSelectors.get(selectorIndex);
			if (signComp != null) {
				signComp.setActionSignCoord(absCoord);
				signComp.updateActionSign();
			} else {
				CivLog.warning("No sign selector found for id:"+selectorIndex);
			}
			
			break;
		case "/next":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());

			structSign = new StructureSign(absCoord, this);
			structSign.setText("\n"+ChatColor.BOLD+ChatColor.UNDERLINE+CivSettings.localize.localizedString("stable_sign_nextUnit"));
			structSign.setDirection(sb.getData());
			structSign.setType("next");
			structSign.setAction(sb.keyvalues.get("id"));
			structSign.update();
			this.addStructureSign(structSign);
			CivGlobal.addStructureSign(structSign);
			
			break;
		case "/horsespawn":
			this.horseSpawnCoord = absCoord;
			break;
		case "/mulespawn":
			this.muleSpawnCoord = absCoord;
			break;
		}
	}

	public void setNonResidentFee(double d) {
		this.nonMemberFeeComponent.setFeeRate(d);
	}	

}
