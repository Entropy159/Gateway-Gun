package com.entropy.misc;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.CompletableFuture;

public class SideSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        if (CommandSource.shouldSuggest(builder.getRemainingLowerCase(), "one")) {
            builder.suggest("ONE");
        }
        if (CommandSource.shouldSuggest(builder.getRemainingLowerCase(), "two")) {
            builder.suggest("TWO");
        }
        if (CommandSource.shouldSuggest(builder.getRemainingLowerCase(), "none")) {
            builder.suggest("NONE");
        }
        return builder.buildFuture();
    }
}
