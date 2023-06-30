package com.avrgaming.civcraft.components;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.components.TradeLevelComponent.Result;

public class TradeShipResults {

    private Result result;
    private double money;
    private int culture;
    private int consumed;

	private List<ItemStack> returnCargo = new LinkedList<ItemStack>();
    
    public TradeShipResults() {
		this.money = 0;
		this.culture = 0;
		this.consumed = 0;
		this.result = Result.UNKNOWN;
	}
    
    public double getMoney() {
        return money;
    }
    
    public void setMoney(Double val) {
        this.money = val;
    }
    
    public int getCulture() {
        return culture;
    }
    
    public void setCulture(Integer val) {
        this.culture = val;
    }

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}

	public int getConsumed() {
		return consumed;
	}

	public void setConsumed(int consumed) {
		this.consumed = consumed;
	}
	
	public void addReturnCargo(ItemStack cargo) {
		this.returnCargo.add(cargo);
	}

	public List<ItemStack> getReturnCargo() {
		return returnCargo;
	}

	public void setReturnCargo(List<ItemStack> returnCargo) {
		this.returnCargo = returnCargo;
	}

}