// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.avrgaming.civcraft.command.arguments;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.bukkit.Bukkit;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class ResidentArgumentType implements ArgumentType<Resident> {
    private static final DynamicCommandExceptionType UNKNOWN_RESIDENT_EXCEPTION = new DynamicCommandExceptionType(name -> new LiteralMessage(CivSettings.localize.localizedString("var_civGlobal_noResident", name)));
    private static final Collection<String> EXAMPLES = Arrays.asList("Notch", "Lucius777_YT", "Player1");
    private final boolean online;

    private ResidentArgumentType(boolean online) {
        this.online = online;
    }

    public static ResidentArgumentType resident() {
        return new ResidentArgumentType(true);
    }

    public static ResidentArgumentType offlineResident() { //TODO: я думаю это лучше не использовать
        return new ResidentArgumentType(false);
    }

    public static Resident getResident(final CommandContext<?> context, final String name) {
        return context.getArgument(name, Resident.class);
    }

    @Nonnull
    @Override
    public Resident parse(final StringReader reader) throws CommandSyntaxException {
        String name = reader.readUnquotedString();
        Resident resident = CivGlobal.getResident(name);
        if (resident == null || !Bukkit.getOfflinePlayer(resident.getUUID()).isOnline() && online) {
            throw UNKNOWN_RESIDENT_EXCEPTION.createWithContext(reader, name);
        }
        return resident;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        for (Resident resident : CivGlobal.getResidents()) {
            if (resident.getName().startsWith(builder.getRemaining()) && (Bukkit.getOfflinePlayer(resident.getUUID()).isOnline() || !online)) {
                builder.suggest(resident.getName());
            }
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
