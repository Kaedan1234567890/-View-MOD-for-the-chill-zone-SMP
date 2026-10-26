package com.chillzone.invview;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class ChillZoneInvView implements ModInitializer {
    private final Map<String, String> historicalNames = new ConcurrentHashMap<>();
    private final Path registry = FabricLoader.getInstance().getConfigDir().resolve("chillzone-invview/players.tsv");

    @Override public void onInitialize() {
        readRegistry();
        importOldRegistry();
        importUserCache();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            // UUID key keeps names up to date if a player renames themselves.
            historicalNames.put(player.getUUID().toString(), player.getGameProfile().name());
            saveRegistry();
        });
        // The command tree is complete after server startup; registration callbacks may
        // run before InvView adds its /view nodes, so patch the finished tree instead.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var dispatcher = server.getCommands().getDispatcher();
            install(dispatcher, "inv");
            install(dispatcher, "echest");
        });
    }

    @SuppressWarnings("unchecked")
    private void install(CommandDispatcher<CommandSourceStack> dispatcher, String subcommand) {
        CommandNode<CommandSourceStack> root = dispatcher.getRoot().getChild("view");
        if (root == null) return;
        CommandNode<CommandSourceStack> action = root.getChild(subcommand);
        if (action == null) return;
        CommandNode<CommandSourceStack> target = action.getChild("target");
        if (!(target instanceof ArgumentCommandNode<?, ?> argument)) return;
        ArgumentNodeAccessor<CommandSourceStack, ?> accessor = (ArgumentNodeAccessor<CommandSourceStack, ?>) argument;
        SuggestionProvider<CommandSourceStack> original = accessor.chillzone$getSuggestions();
        accessor.chillzone$setSuggestions((context, builder) -> mergeSuggestions(original, context, builder));
    }

    private CompletableFuture<Suggestions> mergeSuggestions(SuggestionProvider<CommandSourceStack> original,
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        // The original provider is retained to preserve vanilla selectors (@s, @a, @p, @r, @n) and current online player names.
        CompletableFuture<Suggestions> vanilla = original == null
                ? net.minecraft.commands.arguments.GameProfileArgument.gameProfile().listSuggestions(context, builder.createOffset(builder.getStart()))
                : original.getSuggestions(context, builder.createOffset(builder.getStart()));
        SuggestionsBuilder rememberedBuilder = builder.createOffset(builder.getStart());
        String typed = rememberedBuilder.getRemaining().toLowerCase(Locale.ROOT);
        historicalNames.values().stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(typed))
                .forEach(rememberedBuilder::suggest);
        Suggestions remembered = rememberedBuilder.build();
        return vanilla.thenApply(suggestions -> Suggestions.merge(builder.getInput(), List.of(suggestions, remembered)));
    }

    private synchronized void readRegistry() {
        if (!Files.isRegularFile(registry)) return;
        try {
            for (String line : Files.readAllLines(registry, StandardCharsets.UTF_8)) {
                String[] fields = line.split("\\t", 2);
                if (fields.length == 2 && fields[0].matches("[a-fA-F0-9-]{36}") && fields[1].matches("[A-Za-z0-9_]{1,16}"))
                    historicalNames.put(fields[0], fields[1]);
            }
        } catch (IOException ex) { System.err.println("[ChillZoneInvView] Could not read remembered players: " + ex); }
    }

    // Upgrade path from Chill Zone InvView 1.4.21 (Minecraft 26.2).
    // Never delete the original JSON; it remains available for rollback.
    private synchronized void importOldRegistry() {
        importJsonPlayerArray(FabricLoader.getInstance().getConfigDir().resolve("chill_zone_invview_players.json"));
    }

    // Recover already known offline player names on fresh installations.
    private synchronized void importUserCache() {
        importJsonPlayerArray(Path.of("usercache.json"));
    }

    private synchronized void importJsonPlayerArray(Path path) {
        if (!Files.isRegularFile(path)) return;
        int before = historicalNames.size();
        try (java.io.Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            com.google.gson.JsonElement root = com.google.gson.JsonParser.parseReader(reader);
            if (!root.isJsonArray()) return;
            for (com.google.gson.JsonElement element : root.getAsJsonArray()) {
                if (!element.isJsonObject()) continue;
                com.google.gson.JsonObject item = element.getAsJsonObject();
                if (!item.has("uuid") || !item.has("name")) continue;
                String uuid = item.get("uuid").getAsString();
                String name = item.get("name").getAsString();
                if (uuid.matches("[a-fA-F0-9-]{36}") && name.matches("[A-Za-z0-9_]{1,16}"))
                    historicalNames.putIfAbsent(uuid, name);
            }
        } catch (Exception ex) {
            System.err.println("[ChillZoneInvView] Could not import player history from " + path + ": " + ex);
        }
        if (historicalNames.size() != before) saveRegistry();
    }

    private synchronized void saveRegistry() {
        try {
            Files.createDirectories(registry.getParent());
            Path temp = registry.resolveSibling("players.tsv.tmp");
            List<String> rows = new ArrayList<>();
            historicalNames.forEach((uuid, name) -> rows.add(uuid + "\t" + name));
            rows.sort(String::compareTo);
            Files.write(temp, rows, StandardCharsets.UTF_8);
            try { Files.move(temp, registry, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(temp, registry, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException ex) { System.err.println("[ChillZoneInvView] Could not save remembered players: " + ex); }
    }
}
