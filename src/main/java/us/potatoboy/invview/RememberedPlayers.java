package us.potatoboy.invview;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Chill Zone player-name memory used only for /view tab completion.
 *
 * It deliberately does NOT restrict command execution to remembered players.
 * InvView still uses Minecraft's GameProfileArgument, so an operator may manually
 * type a valid username that has never joined this server and let the normal
 * profile resolver attempt to resolve it.
 */
public final class RememberedPlayers {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, String> PLAYERS = new LinkedHashMap<>();

    private static Path storePath;

    private RememberedPlayers() {
    }

    public static synchronized void initialize(MinecraftServer server) {
        storePath = FabricLoader.getInstance().getConfigDir().resolve("chill_zone_invview_players.json");
        PLAYERS.clear();

        loadStoredPlayers();
        importVanillaUserCache();

        // SERVER_STARTED normally guarantees PlayerList exists, but keep this guard
        // so a lifecycle/order change can never crash the server during startup.
        if (server.getPlayerList() != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                put(player.getUUID(), player.getScoreboardName());
            }
        }

        save();
        LOGGER.info("InvView Chill Zone loaded {} remembered player name(s) for autocomplete", PLAYERS.size());
    }

    public static synchronized void remember(UUID uuid, String name) {
        if (put(uuid, name)) {
            save();
        }
    }

    private static boolean put(UUID uuid, String name) {
        if (uuid == null || name == null || name.isBlank()) {
            return false;
        }

        String cleanName = name.trim();
        String previous = PLAYERS.put(uuid, cleanName);
        return !cleanName.equals(previous);
    }

    /**
     * Suggest only server-known names and only when they match what staff has typed.
     */
    public static synchronized CompletableFuture<Suggestions> suggest(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {

        String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<>(PLAYERS.values());
        names.sort(String.CASE_INSENSITIVE_ORDER);

        for (String name : names) {
            if (name.toLowerCase(Locale.ROOT).startsWith(typed)) {
                builder.suggest(name);
            }
        }

        return builder.buildFuture();
    }

    private static void loadStoredPlayers() {
        if (storePath == null || !Files.isRegularFile(storePath)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(storePath, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonArray()) {
                return;
            }

            for (JsonElement element : root.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject object = element.getAsJsonObject();
                if (!object.has("uuid") || !object.has("name")) {
                    continue;
                }

                try {
                    UUID uuid = UUID.fromString(object.get("uuid").getAsString());
                    String name = object.get("name").getAsString();
                    put(uuid, name);
                } catch (Exception ignored) {
                    // Ignore one malformed entry rather than losing the rest of the registry.
                }
            }
        } catch (Exception exception) {
            LOGGER.warn("Unable to read Chill Zone InvView remembered-player file", exception);
        }
    }

    /**
     * Bootstrap the Chill Zone list from the vanilla server's existing usercache.json.
     * This is what makes players who joined before this fork existed appear immediately.
     */
    private static void importVanillaUserCache() {
        Path userCache = Path.of("usercache.json");
        if (!Files.isRegularFile(userCache)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(userCache, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonArray()) {
                return;
            }

            for (JsonElement element : root.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject object = element.getAsJsonObject();
                if (!object.has("uuid") || !object.has("name")) {
                    continue;
                }

                try {
                    UUID uuid = UUID.fromString(object.get("uuid").getAsString());
                    String name = object.get("name").getAsString();
                    put(uuid, name);
                } catch (Exception ignored) {
                    // A bad cache entry should not stop server startup.
                }
            }
        } catch (Exception exception) {
            LOGGER.warn("Unable to import vanilla usercache.json for InvView autocomplete", exception);
        }
    }

    private static void save() {
        if (storePath == null) {
            return;
        }

        try {
            Files.createDirectories(storePath.getParent());

            List<Map.Entry<UUID, String>> entries = new ArrayList<>(PLAYERS.entrySet());
            entries.sort(Comparator.comparing(Map.Entry<UUID, String>::getValue, String.CASE_INSENSITIVE_ORDER));

            JsonArray array = new JsonArray();
            for (Map.Entry<UUID, String> entry : entries) {
                JsonObject object = new JsonObject();
                object.addProperty("uuid", entry.getKey().toString());
                object.addProperty("name", entry.getValue());
                array.add(object);
            }

            try (BufferedWriter writer = Files.newBufferedWriter(
                    storePath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {
                GSON.toJson(array, writer);
            }
        } catch (Exception exception) {
            LOGGER.warn("Unable to save Chill Zone InvView remembered-player file", exception);
        }
    }
}
