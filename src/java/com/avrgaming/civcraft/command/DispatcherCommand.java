package com.avrgaming.civcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public abstract class DispatcherCommand implements TabExecutor {
    public abstract CommandDispatcher<Sender> getDispatcher();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        StringBuilder builder = new StringBuilder(command.getName());
        for (String arg : args) {
            builder.append(" ");
            builder.append(arg);
        }
        try {
            return getDispatcher().execute(builder.toString(), new Sender(sender)) > 0;
        } catch (CommandSyntaxException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
            return false;
        } catch (CommandProblemException e) {
            sender.sendMessage(e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        StringBuilder builder = new StringBuilder(command.getName());
        for (String arg : args) {
            builder.append(" ");
            builder.append(arg);
        }
        try {
            Suggestions suggestions = getDispatcher().getCompletionSuggestions(getDispatcher().parse(builder.toString(), new Sender(sender))).get();
            return suggestions.getList().stream().map(Suggestion::getText).collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
