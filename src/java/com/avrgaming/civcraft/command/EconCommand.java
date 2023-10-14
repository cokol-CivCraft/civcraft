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
package com.avrgaming.civcraft.command;

import com.avrgaming.civcraft.command.arguments.CivArgumentType;
import com.avrgaming.civcraft.command.arguments.ResidentArgumentType;
import com.avrgaming.civcraft.command.arguments.TownArgumentType;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.EconObject;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class EconCommand extends CommandBase {
    static final CommandDispatcher<CommandSender> dispatcher = new CommandDispatcher<>();
    static final LiteralArgumentBuilder<CommandSender> root = LiteralArgumentBuilder.literal("econ");

    static {
        LiteralArgumentBuilder<CommandSender> debt = LiteralArgumentBuilder.<CommandSender>literal("setdebt").then(
                RequiredArgumentBuilder.<CommandSender, Double>argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(EconCommand::setdebt_cmd));

        LiteralArgumentBuilder<CommandSender> add = LiteralArgumentBuilder.<CommandSender>literal("add").then(
                RequiredArgumentBuilder.<CommandSender, Double>argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(EconCommand::add_cmd));

        LiteralArgumentBuilder<CommandSender> sub = LiteralArgumentBuilder.<CommandSender>literal("sub").then(
                RequiredArgumentBuilder.<CommandSender, Double>argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(EconCommand::sub_cmd));

        LiteralArgumentBuilder<CommandSender> set = LiteralArgumentBuilder.<CommandSender>literal("set").then(
                RequiredArgumentBuilder.<CommandSender, Double>argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(EconCommand::set_cmd));


        root.then(
                LiteralArgumentBuilder.<CommandSender>literal("civ").then(
                        RequiredArgumentBuilder.<CommandSender, Civilization>argument("civ", CivArgumentType.civilization())
                                .then(debt).then(add).then(sub).then(set)));
        root.then(
                LiteralArgumentBuilder.<CommandSender>literal("town").then(
                        RequiredArgumentBuilder.<CommandSender, Town>argument("town", TownArgumentType.town())
                                .then(debt).then(add).then(sub).then(set)));
        root.then(
                LiteralArgumentBuilder.<CommandSender>literal("res").then(
                        RequiredArgumentBuilder.<CommandSender, Resident>argument("res", ResidentArgumentType.resident())
                                .then(debt).then(add).then(sub).then(set)));
        dispatcher.register(root);
    }

    public void clearalldebt_cmd() throws CivException {
        validEcon();

        for (Civilization civ : CivGlobal.getCivs()) {
            civ.getTreasury().setDebt(0);
            try {
                civ.saveNow();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        for (Town town : CivGlobal.getTowns()) {
            town.getTreasury().setDebt(0);
            try {
                town.saveNow();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        for (Resident res : CivGlobal.getResidents()) {
            res.getTreasury().setDebt(0);
            try {
                res.saveNow();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        CivMessage.send(sender, CivSettings.localize.localizedString("cmd_econ_clearedAllDebtSuccess"));
    }

    public static int setdebt_cmd(CommandContext<CommandSender> context) {
        EconObject treasury = getTreasury(context);
        double amount = DoubleArgumentType.getDouble(context, "amount");
        treasury.setDebt(amount);
        saveTarget(context);

        CivMessage.sendSuccess(context.getSource(), CivSettings.localize.localizedString("SetSuccess"));
        return 1;
    }

    protected static EconObject getTreasury(CommandContext<CommandSender> context) {
        try {
            return CivArgumentType.getCiv(context, "civ").getTreasury();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            return TownArgumentType.getTown(context, "town").getTreasury();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            return ResidentArgumentType.getResident(context, "res").getTreasury();
        } catch (IllegalArgumentException ignored) {
        }
        throw new IllegalArgumentException("Treasure not found");
    }

    protected static String getName(CommandContext<CommandSender> context) {
        try {
            return CivArgumentType.getCiv(context, "civ").getName();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            return TownArgumentType.getTown(context, "town").getName();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            return ResidentArgumentType.getResident(context, "res").getName();
        } catch (IllegalArgumentException ignored) {
        }
        throw new IllegalArgumentException("Treasure not found");
    }

    protected static void saveTarget(CommandContext<CommandSender> context) {
        try {
            Civilization civilization = CivArgumentType.getCiv(context, "civ");
            civilization.save();
            return;
        } catch (IllegalArgumentException ignored) {

        }
        try {
            Town town = TownArgumentType.getTown(context, "town");
            town.save();
            return;
        } catch (IllegalArgumentException ignored) {

        }
        try {
            Resident resident = ResidentArgumentType.getResident(context, "res");
            resident.save();
            return;
        } catch (IllegalArgumentException ignored) {

        }
        throw new IllegalArgumentException("Treasure not found");
    }

    private void validEcon() throws CivException {
        // Allow Console commands to manipulate the economy.
        if (sender instanceof ConsoleCommandSender) {
            return;
        }
        if (!getPlayer().isOp()) {
            throw new CivException(CivSettings.localize.localizedString("cmd_MustBeOP"));
        }
    }

    public static Player getPlayer(CommandContext<CommandSender> context) throws CommandFeedback {
        if (!(context.getSource() instanceof Player)) {
            throw new CommandFeedback(CivSettings.localize.localizedString("cmd_MustBePlayer"));
        }
        return (Player) context.getSource();
    }

    public static int add_cmd(CommandContext<CommandSender> context) {
        EconObject treasury = getTreasury(context);
        double amount = DoubleArgumentType.getDouble(context, "amount");
        treasury.deposit(amount);
        saveTarget(context);

        CivMessage.sendSuccess(context.getSource(), CivSettings.localize.localizedString("var_cmd_econ_added", amount, CivSettings.CURRENCY_NAME, getName(context)));
        return 1;
    }

    public static int sub_cmd(CommandContext<CommandSender> context) {
        EconObject treasury = getTreasury(context);
        double amount = DoubleArgumentType.getDouble(context, "amount");
        treasury.withdraw(amount);
        saveTarget(context);

        CivMessage.sendSuccess(context.getSource(), CivSettings.localize.localizedString("var_cmd_econ_Subtracted", amount, CivSettings.CURRENCY_NAME, getName(context)));
        return 1;
    }

    public static int set_cmd(CommandContext<CommandSender> context) {
        EconObject treasury = getTreasury(context);
        double amount = DoubleArgumentType.getDouble(context, "amount");
        treasury.setBalance(amount);

        CivMessage.sendSuccess(context.getSource(), CivSettings.localize.localizedString("var_cmd_econ_set", getName(context), amount, CivSettings.CURRENCY_NAME));
        return 1;
    }

    @Override
    public void init() {
        command = "/econ";
        displayName = CivSettings.localize.localizedString("cmd_econ_Name");

//        register_sub("add", this::add_cmd, CivSettings.localize.localizedString("cmd_econ_addDesc"));
//        register_sub("give", this::give_cmd, CivSettings.localize.localizedString("cmd_econ_giveDesc"));
//        register_sub("set", this::set_cmd, CivSettings.localize.localizedString("cmd_econ_setDesc"));
//        register_sub("sub", this::sub_cmd, CivSettings.localize.localizedString("cmd_econ_subDesc"));

//        register_sub("addtown", this::addtown_cmd, CivSettings.localize.localizedString("cmd_econ_addtownDesc"));
//        register_sub("settown", this::settown_cmd, CivSettings.localize.localizedString("cmd_econ_settownDesc"));
//        register_sub("subtown", this::subtown_cmd, CivSettings.localize.localizedString("cmd_econ_subtownDesc"));
//
//        register_sub("addciv", this::addciv_cmd, CivSettings.localize.localizedString("cmd_econ_addcivDesc"));
//        register_sub("setciv", this::setciv_cmd, CivSettings.localize.localizedString("cmd_econ_setcivDesc"));
//        register_sub("subciv", this::subciv_cmd, CivSettings.localize.localizedString("cmd_econ_subcivDesc"));

//        register_sub("setdebt", this::setdebt_cmd, CivSettings.localize.localizedString("cmd_econ_setdebtDesc"));
//        register_sub("setdebttown", this::setdebttown_cmd, CivSettings.localize.localizedString("cmd_econ_setdebttownDesc"));
//        register_sub("setdebtciv", this::setdebtciv_cmd, CivSettings.localize.localizedString("cmd_econ_setdebtcivDesc"));

        register_sub("clearalldebt", this::clearalldebt_cmd, CivSettings.localize.localizedString("cmd_econ_clearAllDebtDesc"));

    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        StringBuilder builder = new StringBuilder("econ");
        for (String arg : args) {
            builder.append(" ");
            builder.append(arg);
        }
        try {
            return dispatcher.execute(builder.toString(), sender) > 0;
        } catch (CommandSyntaxException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        StringBuilder builder = new StringBuilder("econ");
        for (String arg : args) {
            builder.append(" ");
            builder.append(arg);
        }
        try {
            Suggestions suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(builder.toString(), sender)).get();
            return suggestions.getList().stream().map(Suggestion::getText).collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public void doDefaultAction() throws CivException {
        Player player = getPlayer();
        Resident resident = CivGlobal.getResident(player);

        if (resident == null) {
            return;
        }

        CivMessage.sendSuccess(player, resident.getTreasury().getBalance() + " " + CivSettings.CURRENCY_NAME);

    }

    @Override
    public void showHelp() {
        Player player;
        try {
            player = getPlayer();
        } catch (CivException e) {
            e.printStackTrace();
            return;
        }

        if (!player.isOp()) {
            return;
        }

        showBasicHelp();

    }

    @Override
    public void permissionCheck() {

    }

}
