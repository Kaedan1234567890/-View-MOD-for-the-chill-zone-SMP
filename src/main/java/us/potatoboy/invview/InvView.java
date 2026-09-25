package us.potatoboy.invview;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.logging.LogUtils;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueOutput;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class InvView implements ModInitializer {
    private static MinecraftServer minecraftServer;

    public static boolean isTrinkets = false;
    public static boolean isLuckPerms = false;
    public static boolean isApoli = false;

    @Override
    public void onInitialize() {
        isTrinkets = FabricLoader.getInstance().isModLoaded("trinkets");
        isLuckPerms = FabricLoader.getInstance().isModLoaded("luckperms");
        isApoli = FabricLoader.getInstance().isModLoaded("apoli");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralCommandNode<CommandSourceStack> viewNode = Commands
                    .literal("view")
                    .requires(Permissions.require("invview.command.root", 2))
                    .build();

            LiteralCommandNode<CommandSourceStack> invNode = Commands
                    .literal("inv")
                    .requires(Permissions.require("invview.command.inv", 2))
                    .then(Commands.argument("target", GameProfileArgument.gameProfile())
                            // Chill Zone change: suggestions are ONLY players remembered from this server.
                            // The GameProfileArgument itself is intentionally kept so manually-entered
                            // usernames that have never joined can still be resolved exactly like stock InvView.
                            .suggests(RememberedPlayers::suggest)
                            .executes(ViewCommand::inv))
                    .build();

            LiteralCommandNode<CommandSourceStack> echestNode = Commands
                    .literal("echest")
                    .requires(Permissions.require("invview.command.echest", 2))
                    .then(Commands.argument("target", GameProfileArgument.gameProfile())
                            .suggests(RememberedPlayers::suggest)
                            .executes(ViewCommand::eChest))
                    .build();

            dispatcher.getRoot().addChild(viewNode);
            viewNode.addChild(invNode);
            viewNode.addChild(echestNode);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(this::onLogicalServerStarted);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                RememberedPlayers.remember(handler.player.getUUID(), handler.player.getScoreboardName()));
    }

    private void onLogicalServerStarted(MinecraftServer server) {
        minecraftServer = server;
        RememberedPlayers.initialize(server);
    }

    public static MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }

    // Taken from the original InvView 1.4.21 implementation.
    public static void savePlayerData(ServerPlayer player) {
        File playerDataDir = minecraftServer.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
        try (ProblemReporter.ScopedCollector logging = new ProblemReporter.ScopedCollector(player.problemPath(), LogUtils.getLogger())) {
            TagValueOutput nbtWriteView = TagValueOutput.createWithContext(logging, player.registryAccess());
            player.saveWithoutId(nbtWriteView);
            Path path = playerDataDir.toPath();
            Path path2 = Files.createTempFile(path, player.getStringUUID() + "-", ".dat");
            CompoundTag nbtCompound = nbtWriteView.buildResult();
            NbtIo.writeCompressed(nbtCompound, path2);
            Path path3 = path.resolve(player.getStringUUID() + ".dat");
            Path path4 = path.resolve(player.getStringUUID() + ".dat_old");
            Util.safeReplaceFile(path3, path2, path4);
        } catch (Exception exception) {
            LogUtils.getLogger().warn("Failed to save player data for {}", player.getName().getString());
        }
    }
}
