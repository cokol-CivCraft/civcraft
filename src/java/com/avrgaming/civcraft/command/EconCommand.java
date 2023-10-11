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
import com.avrgaming.civcraft.command.arguments.PlayerArgumentType;
import com.avrgaming.civcraft.command.arguments.TownArgumentType;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
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
        root.then(
                LiteralArgumentBuilder.<CommandSender>literal("add").then(
                        RequiredArgumentBuilder.<CommandSender, Player>argument("player", PlayerArgumentType.player()).then(
                                RequiredArgumentBuilder.<CommandSender, Double>argument("amount", DoubleArgumentType.doubleArg(0)).executes(EconCommand::add_cmd))));
        root.then(
                LiteralArgumentBuilder.<CommandSender>literal("setdebtciv").then(
                        RequiredArgumentBuilder.<CommandSender, Civilization>argument("civ", CivArgumentType.civilization()).then(
                                RequiredArgumentBuilder.<CommandSender, Double>argument("amount", DoubleArgumentType.doubleArg(0))
                                        .executes(EconCommand::setdebtciv_cmd))));
        root.then(
                LiteralArgumentBuilder.<CommandSender>literal("setdebttown").then(
                        RequiredArgumentBuilder.<CommandSender, Town>argument("town", TownArgumentType.town()).then(
                                RequiredArgumentBuilder.<CommandSender, Double>argument("amount", DoubleArgumentType.doubleArg(0))
                                        .executes(EconCommand::setdebttown_cmd))));
        dispatcher.register(root);
    }

    private static void validEcon(CommandContext<CommandSender> context) throws CommandFeedback {
        // Allow Console commands to manipulate the economy.
        if (context.getSource() instanceof ConsoleCommandSender) {
            return;
        }
        if (context.getSource() instanceof Player player && !player.isOp()) {
            throw new CommandFeedback(CivSettings.localize.localizedString("cmd_MustBeOP"));
        }
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

    public static int setdebtciv_cmd(CommandContext<CommandSender> context) {
        try {
            validEcon(context);
        } catch (CommandFeedback e) {
            context.getSource().sendMessage(ChatColor.RED + e.getMessage());
            return 0;
        }

        Civilization civ = CivArgumentType.getCiv(context, "civ");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        civ.getTreasury().setDebt(amount);
        civ.save();

        CivMessage.sendSuccess(context.getSource(), CivSettings.localize.localizedString("SetSuccess"));
        return 1;
    }

    public static int setdebttown_cmd(CommandContext<CommandSender> context) {
        try {
            validEcon(context);
        } catch (CommandFeedback e) {
            context.getSource().sendMessage(ChatColor.RED + e.getMessage());
            return 0;
        }

        Town town = TownArgumentType.getTown(context, "town");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        town.getTreasury().setDebt(amount);
        town.save();

        CivMessage.sendSuccess(context.getSource(), CivSettings.localize.localizedString("SetSuccess"));
        return 1;
    }

    public void setdebt_cmd() throws CivException {
        validEcon();

        Resident resident = getNamedResident(1);
        Double amount = getNamedDouble(2);
        resident.getTreasury().setDebt(amount);
        resident.save();

        CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("SetSuccess"));
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
        try {
            validEcon(context);
        } catch (CommandFeedback e) {
            context.getSource().sendMessage(ChatColor.RED + e.getMessage());
            return 0;
        }
        Player to = PlayerArgumentType.getPlayer(context, "player");
        Resident resident = CivGlobal.getResident(to);

        double amount = DoubleArgumentType.getDouble(context, "amount");
        resident.getTreasury().deposit(amount);
        CivMessage.sendSuccess(context.getSource(), CivSettings.localize.localizedString("var_cmd_econ_added", amount, CivSettings.CURRENCY_NAME, to.getName()));
        return 1;
    }

    @Override
    public void init() {
        command = "/econ";
        displayName = CivSettings.localize.localizedString("cmd_econ_Name");

//        register_sub("add", this::add_cmd, CivSettings.localize.localizedString("cmd_econ_addDesc"));
        register_sub("give", this::give_cmd, CivSettings.localize.localizedString("cmd_econ_giveDesc"));
        register_sub("set", this::set_cmd, CivSettings.localize.localizedString("cmd_econ_setDesc"));
        register_sub("sub", this::sub_cmd, CivSettings.localize.localizedString("cmd_econ_subDesc"));

        register_sub("addtown", this::addtown_cmd, CivSettings.localize.localizedString("cmd_econ_addtownDesc"));
        register_sub("settown", this::settown_cmd, CivSettings.localize.localizedString("cmd_econ_settownDesc"));
        register_sub("subtown", this::subtown_cmd, CivSettings.localize.localizedString("cmd_econ_subtownDesc"));

        register_sub("addciv", this::addciv_cmd, CivSettings.localize.localizedString("cmd_econ_addcivDesc"));
        register_sub("setciv", this::setciv_cmd, CivSettings.localize.localizedString("cmd_econ_setcivDesc"));
        register_sub("subciv", this::subciv_cmd, CivSettings.localize.localizedString("cmd_econ_subcivDesc"));

        register_sub("setdebt", this::setdebt_cmd, CivSettings.localize.localizedString("cmd_econ_setdebtDesc"));
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

    public void give_cmd() throws CivException {
        validEcon();

        if (args.length < 3) {
            throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
        }

        Resident resident = getNamedResident(1);

        try {

            double amount = Double.parseDouble(args[2]);
            resident.getTreasury().deposit(amount);
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_give", args[2], CivSettings.CURRENCY_NAME, args[1]));

        } catch (NumberFormatException e) {
            throw new CivException(args[2] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
        }
    }

    public void set_cmd() throws CivException {
        validEcon();

        if (args.length < 3) {
            throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
        }

        Resident resident = getNamedResident(1);

        try {

            double amount = Double.parseDouble(args[2]);
            resident.getTreasury().setBalance(amount);
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_set", args[1], args[2], CivSettings.CURRENCY_NAME));

        } catch (NumberFormatException e) {
            throw new CivException(args[2] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
        }
    }

    public void sub_cmd() throws CivException {
        validEcon();

        if (args.length < 3) {
            throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
        }

        Resident resident = getNamedResident(1);

        try {

            double amount = Double.parseDouble(args[2]);
            resident.getTreasury().withdraw(amount);
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_Subtracted", args[2], CivSettings.CURRENCY_NAME, args[1]));

        } catch (NumberFormatException e) {
            throw new CivException(args[2] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
        }
    }

    public void addtown_cmd() throws CivException {
        validEcon();

        if (args.length < 3) {
            throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
        }

        Town town = getNamedTown(1);

        try {

            double amount = Double.parseDouble(args[2]);
            town.getTreasury().deposit(amount);
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_added", args[2], CivSettings.CURRENCY_NAME, args[1]));

        } catch (NumberFormatException e) {
            throw new CivException(args[2] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
        }
    }

    public void settown_cmd() throws CivException {
        validEcon();

        if (args.length < 3) {
            throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
        }

        Town town = getNamedTown(1);

        try {

            double amount = Double.parseDouble(args[2]);
            town.getTreasury().setBalance(amount);
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_set", args[1], args[2], CivSettings.CURRENCY_NAME));

        } catch (NumberFormatException e) {
            throw new CivException(args[2] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
        }
    }

    public void subtown_cmd() throws CivException {
        validEcon();

        if (args.length < 3) {
            throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
        }

        Town town = getNamedTown(1);

        try {

            double amount = Double.parseDouble(args[2]);
            town.getTreasury().withdraw(amount);
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_Subtracted", args[2], CivSettings.CURRENCY_NAME, args[1]));

        } catch (NumberFormatException e) {
            throw new CivException(args[2] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
        }
    }

    public void addciv_cmd() throws CivException {
        validEcon();

        if (args.length < 3) {
            throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
        }

        Civilization civ = getNamedCiv(1);

        try {

            double amount = Double.parseDouble(args[2]);
            civ.getTreasury().deposit(amount);
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_added", args[2], CivSettings.CURRENCY_NAME, args[1]));

        } catch (NumberFormatException e) {
            throw new CivException(args[2] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
        }
    }

    public void setciv_cmd() throws CivException {
        validEcon();

        if (args.length < 3) {
            throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
        }

        Civilization civ = getNamedCiv(1);

        try {

            double amount = Double.parseDouble(args[2]);
            civ.getTreasury().setBalance(amount);
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_set", args[1], args[2], CivSettings.CURRENCY_NAME));

        } catch (NumberFormatException e) {
            throw new CivException(args[2] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
        }
    }

    public void subciv_cmd() throws CivException {
        validEcon();

        if (args.length < 3) {
            throw new CivException(CivSettings.localize.localizedString("cmd_econ_ProvideNameAndNumberPrompt"));
        }

        Civilization civ = getNamedCiv(1);

        try {

            double amount = Double.parseDouble(args[2]);
            civ.getTreasury().withdraw(amount);
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_econ_Subtracted", args[2], CivSettings.CURRENCY_NAME, args[1]));

        } catch (NumberFormatException e) {
            throw new CivException(args[2] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
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
