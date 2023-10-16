// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.avrgaming.civcraft.command.arguments;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.object.Town;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class TownArgumentType implements ArgumentType<Town> {
    private static final DynamicCommandExceptionType UNKNOWN_TOWN_EXCEPTION = new DynamicCommandExceptionType(name -> new LiteralMessage(CivSettings.localize.localizedString("var_civGlobal_noTown", name)));
    private static final Collection<String> EXAMPLES = Arrays.asList("Novosib", "NewTokyo", "Rim");

    private TownArgumentType() {
    }

    public static TownArgumentType town() {
        return new TownArgumentType();
    }

    public static Town getTown(final CommandContext<?> context, final String name) {
        return context.getArgument(name, Town.class);
    }

    @Nonnull
    @Override
    public Town parse(final StringReader reader) throws CommandSyntaxException {
        String name = reader.readUnquotedString();
        Town town = Town.getTown(name);
        if (town == null) {
            throw UNKNOWN_TOWN_EXCEPTION.createWithContext(reader, name);
        }
        return town;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        for (Town town : Town.getTowns()) {
            if (town.getName().startsWith(builder.getRemaining())) {
                builder.suggest(town.getName());
            }
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
