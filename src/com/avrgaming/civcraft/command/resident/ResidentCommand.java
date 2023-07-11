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
package com.avrgaming.civcraft.command.resident;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

public class ResidentCommand extends CommandBase {

    @Override
    public void init() {
        command = "/resident";
        displayName = CivSettings.localize.localizedString("cmd_res_Name");

        register_sub("info", this::info_cmd, CivSettings.localize.localizedString("cmd_res_infoDesc"));
        register_sub("paydebt", this::paydebt_cmd, CivSettings.localize.localizedString("cmd_res_paydebtDesc"));
        register_sub("friend", this::friend_cmd, CivSettings.localize.localizedString("cmd_res_friendDesc"));
        register_sub("toggle", this::toggle_cmd, CivSettings.localize.localizedString("cmd_res_toggleDesc"));
        register_sub("show", this::show_cmd, CivSettings.localize.localizedString("cmd_res_showDesc"));
        register_sub("resetspawn", this::resetspawn_cmd, CivSettings.localize.localizedString("cmd_res_resetspawnDesc"));
        register_sub("exchange", this::exchange_cmd, CivSettings.localize.localizedString("cmd_res_exchangeDesc"));
        register_sub("book", this::book_cmd, CivSettings.localize.localizedString("cmd_res_bookDesc"));
        register_sub("refresh", this::refresh_cmd, CivSettings.localize.localizedString("cmd_res_refreshDesc"));
        register_sub("timezone", this::timezone_cmd, CivSettings.localize.localizedString("cmd_res_timezoneDesc"));
        register_sub("pvptimer", this::pvptimer_cmd, CivSettings.localize.localizedString("cmd_res_pvptimerDesc"));
        register_sub("pt", this::pvptimer_cmd, null); // pvptimer
        register_sub("t", this::timezone_cmd, null); // timezone
        register_sub("f", this::friend_cmd, null); // friend
        register_sub("i", this::info_cmd, null); // info
        register_sub("b", this::book_cmd, null); // book

        //commands.put("switchtown", "[town] - Allows you to instantly change your town to this town, if this town belongs to your civ.");
    }

    @SuppressWarnings("unused")
    public void pt_cmd() {
        try {
            pvptimer_cmd();
        } catch (CivException e) {
            e.printStackTrace();
        }
    }

    public void pvptimer_cmd() throws CivException {
        Resident resident = getResident();

        if (!resident.isProtected()) {
            CivMessage.sendError(sender, CivSettings.localize.localizedString("cmd_res_pvptimerNotActive"));
        }

        resident.setisProtected(false);
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_res_pvptimerSuccess"));
    }

    @SuppressWarnings("unused")
    public void t_cmd() throws CivException {
        Resident resident = getResident();
        if (args.length < 2) {
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_res_timezoneCurrent") + " " + resident.getTimezone());
        } else {
            timezone_cmd();
        }
    }

    public void timezone_cmd() throws CivException {
        Resident resident = getResident();

        if (args.length < 2) {
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_res_timezoneCurrent") + " " + resident.getTimezone());
            return;
        }

        if (args[1].equalsIgnoreCase("list")) {
            CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_res_timezoneHeading"));
            StringBuilder out = new StringBuilder();
            for (String zone : TimeZone.getAvailableIDs()) {
                out.append(zone).append(", ");
            }
            CivMessage.send(sender, out.toString());
            return;
        }

        TimeZone timezone = TimeZone.getTimeZone(args[1]);

        if (timezone.getID().equals("GMT") && !args[1].equalsIgnoreCase("GMT")) {
            CivMessage.send(sender, CivColor.LightGray + CivSettings.localize.localizedString("var_cmd_res_timezonenotFound1", args[1]));
            CivMessage.send(sender, CivColor.LightGray + CivSettings.localize.localizedString("cmd_res_timezoneNotFound3"));
        }

        resident.setTimezone(timezone.getID());
        resident.save();
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_res_timezoneSuccess", timezone.getID()));
    }

    @SuppressWarnings("unused")
    public void refresh_cmd() throws CivException {
        Resident resident = getResident();
        resident.perks.clear();
        resident.loadPerks(getPlayer());
        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_res_refreshSuccess"));
    }


    @SuppressWarnings("unused")
    public void b_cmd() {
        try {
            book_cmd();
        } catch (CivException e) {
            e.printStackTrace();
        }
    }

    public void book_cmd() throws CivException {
        Player player = getPlayer();

        /* Determine if he already has the book. */
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null) {
                continue;
            }

            LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
            if (craftMat == null) {
                continue;
            }

            if (craftMat.getConfigId().equals("mat_tutorial_book")) {
                throw new CivException(CivSettings.localize.localizedString("cmd_res_bookHaveOne"));
            }
        }

        LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterialFromId("mat_tutorial_book");
        ItemStack helpBook = LoreCraftableMaterial.spawn(craftMat);

        HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(helpBook);
        if (leftovers != null && leftovers.size() >= 1) {
            throw new CivException(CivSettings.localize.localizedString("cmd_res_bookInvenFull"));
        }

        CivMessage.sendSuccess(player, CivSettings.localize.localizedString("cmd_res_bookSuccess"));
    }

    /*
     * We need to figure out how to handle debt for the resident when he switches towns.
     * Should we even allow this? idk. Maybe war respawn points is enough?
     */
//	public void switchtown_cmd() throws CivException {
//		Town town = getNamedTown(1);
//		Resident resident = getResident();
//		
//		if (resident.getTown() == town) {
//			throw new CivException("You cannot switch to your own town.");
//		}
//		
//		if (resident.getTown().getMotherCiv() != town.getMotherCiv()) {
//			throw new CivException("You cannot place yourself into a conquered civ's town.");
//		}
//		
//		if (town.getCiv() != resident.getCiv()) {
//			throw new CivException("You cannot switch to a town not in your civ.");
//		}
//		
//		if (town.getMayorGroup().hasMember(resident) && town.getMayorGroup().getMemberCount() <= 1) {
//			throw new CivException("You are the last mayor of the town and cannot leave it.");
//		}
//		
//		resident.getTown().removeResident(resident);
//		try {
//			town.addResident(resident);
//		} catch (AlreadyRegisteredException e) {
//			e.printStackTrace();
//			throw new CivException("You already belong to this town.");
//		}
//		
//	}

    @SuppressWarnings("unused")
    public void exchange_cmd() throws CivException {
        Player player = getPlayer();
        Resident resident = getResident();
        String type = getNamedString(1, CivSettings.localize.localizedString("cmd_res_exchangePrompt"));
        Integer amount = getNamedInteger(2);

        if (amount <= 0) {
            throw new CivException(CivSettings.localize.localizedString("cmd_res_exchangeLessThan0"));
        }

        type = type.toLowerCase();

        Material exchangeID;
        double rate;
        switch (type) {
            case "iron":
                exchangeID = Material.IRON_INGOT;
                rate = CivSettings.iron_rate;
                break;
            case "gold":
                exchangeID = Material.GOLD_INGOT;
                rate = CivSettings.gold_rate;
                break;
            case "diamond":
                exchangeID = Material.DIAMOND;
                rate = CivSettings.diamond_rate;
                break;
            case "emerald":
                exchangeID = Material.EMERALD;
                rate = CivSettings.emerald_rate;
                break;
            default:
                throw new CivException(CivSettings.localize.localizedString("var_cmd_res_exchangeInvalid", type));
        }

        double exchangeRate;
        try {
            exchangeRate = CivSettings.getDouble(CivSettings.civConfig, "global.exchange_rate");
        } catch (InvalidConfiguration e) {
            e.printStackTrace();
            throw new CivException(CivSettings.localize.localizedString("internalException"));
        }

        ItemStack stack = new ItemStack(exchangeID, 1, (short) 0);
        int total = 0;
        for (int i = 0; i < player.getInventory().getContents().length; i++) {
            ItemStack is = player.getInventory().getItem(i);
            if (is == null) {
                continue;
            }

            if (LoreCraftableMaterial.isCustom(is)) {
                continue;
            }

            if (CivGlobal.isBonusGoodie(is)) {
                throw new CivException(CivSettings.localize.localizedString("cmd_res_exchangeNoTradeGoods"));
            }

            if (is.getType() == exchangeID) {
                total += is.getAmount();
                break;
            }
        }

        if (total == 0) {
            throw new CivException(CivSettings.localize.localizedString("cmd_res_exchangeNotEnough") + " " + type);
        }

        if (amount > total) {
            amount = total;
        }

        stack.setAmount(amount);
        player.getInventory().removeItem(stack);
        double coins = amount * rate * exchangeRate;

        resident.getTreasury().deposit(coins);
        CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_cmd_res_exchangeSuccess", amount, type, coins, CivSettings.CURRENCY_NAME));

    }

    @SuppressWarnings("unused")
    public void resetspawn_cmd() throws CivException {
        Player player = getPlayer();
        Location spawn = player.getWorld().getSpawnLocation();
        player.setBedSpawnLocation(spawn, true);
        CivMessage.sendSuccess(player, CivSettings.localize.localizedString("cmd_res_resetspawnSuccess"));
    }

    @SuppressWarnings("unused")
    public void show_cmd() throws CivException {
        if (args.length < 2) {
            throw new CivException(CivSettings.localize.localizedString("cmd_res_showPrompt"));
        }

        Resident resident = getNamedResident(1);

        show(sender, resident);
    }

    @SuppressWarnings("unused")
    public void toggle_cmd() {
        ResidentToggleCommand cmd = new ResidentToggleCommand();
        cmd.onCommand(sender, null, "friend", this.stripArgs(args, 1));
    }

    public void friend_cmd() {
        ResidentFriendCommand cmd = new ResidentFriendCommand();
        cmd.onCommand(sender, null, "friend", this.stripArgs(args, 1));
    }

    @SuppressWarnings("unused")
    public void f_cmd() {
        friend_cmd();
    }

    @SuppressWarnings("unused")
    public void paydebt_cmd() throws CivException {
        Resident resident = getResident();

        if (!resident.getTreasury().inDebt() || !resident.hasTown()) {
            throw new CivException(CivSettings.localize.localizedString("var_cmd_res_paydebtError2"));
        }

        if (!resident.getTreasury().hasEnough(resident.getTreasury().getDebt())) {
            throw new CivException(CivSettings.localize.localizedString("var_cmd_res_paydebtError1", resident.getTreasury().getDebt(), CivSettings.CURRENCY_NAME));
        }


        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_res_paydebtSuccess", resident.getTreasury().getDebt(), CivSettings.CURRENCY_NAME));
        resident.payOffDebt();
    }

    public void info_cmd() throws CivException {
        Resident resident = getResident();
        show(sender, resident);
    }

    @SuppressWarnings("unused")
    public void i_cmd() {
        try {
            info_cmd();
        } catch (CivException e) {
            e.printStackTrace();
        }
    }

    public static void show(CommandSender sender, Resident resident) {
        CivMessage.sendHeading(sender, CivSettings.localize.localizedString("var_Resident", resident.getName()));
        Date lastOnline = new Date(resident.getLastOnline());
        SimpleDateFormat sdf = new SimpleDateFormat("M/dd/yy h:mm:ss a z");
        CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_res_showLastOnline") + " " + CivColor.LightGreen + sdf.format(lastOnline));
        CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Town") + " " + CivColor.LightGreen + resident.getTownString());
        CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Camp") + " " + CivColor.LightGreen + resident.getCampString());

        if (sender.getName().equalsIgnoreCase(resident.getName()) || sender.isOp()) {
            CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_res_showTreasure") + " " + CivColor.LightGreen + resident.getTreasury().getBalance() + " " +
                    CivColor.Green + CivSettings.localize.localizedString("cmd_res_showTaxes") + " " + CivColor.LightGreen + (resident.getPropertyTaxOwed() + resident.getFlatTaxOwed()));
            if (resident.hasTown()) {
                if (resident.getSelectedTown() != null) {
                    CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_res_showSelected") + " " + CivColor.LightGreen + resident.getSelectedTown().getName());
                } else {
                    CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_res_showSelected") + " " + CivColor.LightGreen + resident.getTown().getName());
                }
            }
        }

        if (resident.getTreasury().inDebt()) {
            CivMessage.send(resident, CivColor.Yellow + CivSettings.localize.localizedString("cmd_res_showDebt") + " " + resident.getTreasury().getDebt() + " " + CivSettings.CURRENCY_NAME);
        }

        if (resident.getDaysTilEvict() > 0) {
            CivMessage.send(resident, CivColor.Yellow + CivSettings.localize.localizedString("cmd_res_showEviction") + " " + resident.getDaysTilEvict());
        }

        CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Groups") + " " + resident.getGroupsString());

        try {
            if (resident.isUsesAntiCheat()) {
                CivMessage.send(sender, CivColor.LightGreen + CivSettings.localize.localizedString("cmd_res_showAC1"));
            } else {
                CivMessage.send(sender, CivColor.Rose + CivSettings.localize.localizedString("cmd_res_showAC2"));
            }
        } catch (CivException e) {
            CivMessage.send(sender, CivColor.LightGray + CivSettings.localize.localizedString("cmd_res_showOffline"));
        }
    }

    @Override
    public void doDefaultAction() {
        showHelp();
        //info_cmd();
        //CivMessage.send(sender, CivColor.LightGray+"Subcommands available: See /resident help");
    }

    @Override
    public void showHelp() {
        showBasicHelp();
    }

    @Override
    public void permissionCheck() {

    }

}
