package com.avrgaming.civcraft.object;

import com.avrgaming.civcraft.main.CivGlobal;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class VaultEconObject extends EconObject {

    private final UUID uuid;
    private boolean shouldUpdate = false;

    public VaultEconObject(SQLObject holder, UUID uuid) {
        super(holder);

        this.uuid = uuid;
    }

    private Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public double getBalance() {
        Player player = getPlayer();
        Economy econ = CivGlobal.getEconomy();

        if (player != null && econ != null) {
            if (shouldUpdate) {
                setBalance(super.getBalance(), false);
                shouldUpdate = false;
            }
            return Math.floor(econ.getBalance(player));
        }
        shouldUpdate = true;

        return super.getBalance();
    }


    public void setBalance(double amount) {
        this.setBalance(amount, true);
    }

    public void setBalance(double amount, boolean save) {
        Player player = getPlayer();
        Economy econ = CivGlobal.getEconomy();

        if (player == null || econ == null) {
            super.setBalance(amount, save);
            shouldUpdate = true;
            return;
        }

        amount = amount < 0 ? 0 : Math.floor(amount);

        double current = econ.getBalance(player);
        if (amount > current) econ.depositPlayer(player, amount - current);
        else econ.withdrawPlayer(player, current - amount);

        if (save) {
            holder.save();
        }
    }

    public void deposit(double amount) {
        this.deposit(amount, true);
    }

    public void deposit(double amount, boolean save) {
        Player player = getPlayer();
        Economy econ = CivGlobal.getEconomy();

        if (player == null || econ == null) {
            super.deposit(amount, save);
            shouldUpdate = true;
            return;
        }

        if (shouldUpdate) {
            setBalance(super.getBalance(), false);
            shouldUpdate = false;
        }

        if (amount < 0) return;

        econ.depositPlayer(player, Math.floor(amount));

        if (save) {
            holder.save();
        }
    }

    public void withdraw(double amount) {
        this.withdraw(amount, true);
    }

    public void withdraw(double amount, boolean save) {
        Player player = getPlayer();
        Economy econ = CivGlobal.getEconomy();

        if (player == null || econ == null) {
            super.withdraw(amount, save);
            shouldUpdate = true;
            return;
        }

        if (shouldUpdate) {
            setBalance(super.getBalance(), false);
            shouldUpdate = false;
        }

        if (amount < 0) return;
        amount = Math.floor(amount);

		/*
		 * Update the principal we use to calculate interest,
		 * if our current balance dips below the principal,
		 * then we subtract from the principal.
		 */
        synchronized(principalAmount) {
            if (principalAmount > 0) {
                double currentBalance = this.getBalance();
                double diff = currentBalance - principalAmount;
                diff -= amount;

                if (diff < 0) {
                    principalAmount -= (-diff);
                }
            }
        }

        econ.withdrawPlayer(player, amount);

        if (save) {
            holder.save();
        }



//		EconomyResponse resp;
//		resp = CivGlobal.econ.withdrawPlayer(getEconomyName(), amount);
//		if (resp.type == EconomyResponse.ResponseType.FAILURE) {
//			throw new EconomyException(resp.errorMessage);
//		}
    }

    public boolean hasEnough(double amount) {
        Player player = getPlayer();
        Economy econ = CivGlobal.getEconomy();
        if (player == null || econ == null) return super.hasEnough(amount);

        return econ.has(player, Math.floor(amount));
        //	return CivGlobal.econ.has(getEconomyName(), amount);
    }

    public boolean payTo(EconObject objToPay, double amount) {
        if (!this.hasEnough(amount)) {
            return false;
        } else {
            this.withdraw(amount);
            objToPay.deposit(amount);
            return true;
        }
    }

    public double payToCreditor(EconObject objToPay, double amount) {
        double total = 0;

        if (this.hasEnough(amount)) {
            this.withdraw(amount);
            objToPay.deposit(amount);
            return amount;
        }

		/* Do not have enough to pay, pay what we can and put the rest into debt. */
        this.debt += amount - this.getBalance();
        objToPay.deposit(this.getBalance());
        this.withdraw(this.getBalance());

        return total;
    }
}
