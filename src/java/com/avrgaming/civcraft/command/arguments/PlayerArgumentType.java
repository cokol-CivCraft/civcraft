// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.avrgaming.civcraft.command.arguments;

import com.avrgaming.civcraft.config.CivSettings;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class PlayerArgumentType implements ArgumentType<Player> {
    private static final DynamicCommandExceptionType UNKNOWN_PLAYER_EXCEPTION = new DynamicCommandExceptionType(name -> new LiteralMessage(CivSettings.localize.localizedString("var_civGlobal_noPlayer", name)));
    private static final Collection<String> EXAMPLES = Arrays.asList("Notch", "Lucius777_YT", "Player1");

    private PlayerArgumentType() {
    }

    public static PlayerArgumentType player() {
        return new PlayerArgumentType();
    }

    public static Player getPlayer(final CommandContext<?> context, final String name) {
        return context.getArgument(name, Player.class);
    }

    @Nonnull
    @Override
    public Player parse(final StringReader reader) throws CommandSyntaxException {
        String name = reader.readUnquotedString();
        Player player = Bukkit.getPlayerExact(name);
        if (player == null) {
            throw UNKNOWN_PLAYER_EXCEPTION.createWithContext(reader, name);
        }
        return player;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().startsWith(builder.getRemaining())) {
                builder.suggest(player.getName());
            }
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
