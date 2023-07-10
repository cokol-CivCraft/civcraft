package com.avrgaming.civcraft.trade;

import com.avrgaming.civcraft.object.Resident;
import org.bukkit.inventory.Inventory;

public class TradeInventoryPair {
    public Inventory inv;
    public Inventory otherInv;
    public Resident resident;
    public Resident otherResident;
    public double coins;
    public double otherCoins;
    public boolean valid;
}
