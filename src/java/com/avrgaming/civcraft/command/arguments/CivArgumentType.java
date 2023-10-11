// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.avrgaming.civcraft.command.arguments;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Civilization;
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

public class CivArgumentType implements ArgumentType<Civilization> {
    private static final DynamicCommandExceptionType UNKNOWN_TOWN_EXCEPTION = new DynamicCommandExceptionType(name -> new LiteralMessage(CivSettings.localize.localizedString("var_civGlobal_noCivilization", name)));
    private static final Collection<String> EXAMPLES = Arrays.asList("Greece", "China", "Imperium_Romanum");

    private CivArgumentType() {
    }

    public static CivArgumentType civilization() {
        return new CivArgumentType();
    }

    public static Civilization getCiv(final CommandContext<?> context, final String name) {
        return context.getArgument(name, Civilization.class);
    }

    @Nonnull
    @Override
    public Civilization parse(final StringReader reader) throws CommandSyntaxException {
        String name = reader.readUnquotedString();
        Civilization civilization = CivGlobal.getCiv(name);
        if (civilization == null) {
            throw UNKNOWN_TOWN_EXCEPTION.createWithContext(reader, name);
        }
        return civilization;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        for (Civilization civilization : CivGlobal.getCivs()) {
            if (civilization.getName().startsWith(builder.getRemaining())) {
                builder.suggest(civilization.getName());
            }
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
