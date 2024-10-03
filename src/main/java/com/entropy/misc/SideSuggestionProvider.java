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
    boolean allowNone;
    boolean allowBoth;

    public SideSuggestionProvider(boolean both, boolean none) {
        allowBoth = both;
        allowNone = none;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        if (CommandSource.shouldSuggest(builder.getRemainingLowerCase(), "one")) {
            builder.suggest("ONE");
        }
        if (CommandSource.shouldSuggest(builder.getRemainingLowerCase(), "two")) {
            builder.suggest("TWO");
        }
        if (CommandSource.shouldSuggest(builder.getRemainingLowerCase(), "none") && allowNone) {
            builder.suggest("NONE");
        }
        if (CommandSource.shouldSuggest(builder.getRemainingLowerCase(), "both") && allowBoth) {
            builder.suggest("BOTH");
        }
        return builder.buildFuture();
    }
}
