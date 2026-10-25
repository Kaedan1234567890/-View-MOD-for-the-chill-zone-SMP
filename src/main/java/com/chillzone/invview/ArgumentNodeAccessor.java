package com.chillzone.invview;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ArgumentCommandNode.class, remap = false)
public interface ArgumentNodeAccessor<S, T> {
    @Accessor("customSuggestions")
    SuggestionProvider<S> chillzone$getSuggestions();

    @Mutable
    @Accessor("customSuggestions")
    void chillzone$setSuggestions(SuggestionProvider<S> provider);
}
