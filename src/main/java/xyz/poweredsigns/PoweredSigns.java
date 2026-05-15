package xyz.poweredsigns;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.poweredsigns.config.ModConfig;
import xyz.poweredsigns.utils.SignUtils;

import java.util.ArrayList;
import java.util.List;

public class PoweredSigns implements ModInitializer {
	public static String MODID = "poweredsigns";
	private static final Logger LOGGER = LoggerFactory.getLogger(MODID);
	public static int ticksSinceStartup = 0;
	public static List<String> noPrintPlayers = new ArrayList<>();

	@Override
	public void onInitialize() {
		LOGGER.info("Signs can now be powered.");
		ModConfig.init();
		noPrintPlayers = SignUtils.readToggleSigns();
		ServerTickEvents.END_SERVER_TICK.register(this::onEndTick);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(Commands.literal("togglesigns")
								.executes(this::toggleSigns)

						.then(Commands.argument("value", BoolArgumentType.bool())
								.executes(context ->
										toggleSigns(context, BoolArgumentType.getBool(context, "value"))))));

		FabricLoader.getInstance().getModContainer(MODID).ifPresent(container ->
				ResourceManagerHelper.registerBuiltinResourcePack(
						asId("redstone_signs"), container, ResourcePackActivationType.NORMAL));
	}

	public static Identifier asId(String path) {return Identifier.fromNamespaceAndPath(MODID, path);}

	public void onEndTick(MinecraftServer server) {ticksSinceStartup = server.getTickCount();}

	private int toggleSigns(CommandContext<CommandSourceStack> context, boolean value) {
		CommandSourceStack source = context.getSource();
		if (source.getPlayer() == null) {return 0;}

		String player = source.getPlayer().getName().getString();
		return handleEnvironments(value, source, player);
	}

	private int toggleSigns(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		if (source.getPlayer() == null) {return 0;}

		String player = source.getPlayer().getName().getString();
		boolean disabled = noPrintPlayers.contains(player);
		return handleEnvironments(disabled, source, player);
	}
	private void internalToggleSign(String player) {
		if (!noPrintPlayers.contains(player)) {noPrintPlayers.add(player);}
	}

	private int handleEnvironments(boolean value, CommandSourceStack source, String player) {
		switch (FabricLoader.getInstance().getEnvironmentType()) {
			case CLIENT -> {
				if (value) {
					noPrintPlayers.remove(player);
					source.sendSuccess(() -> Component.translatable("text.poweredsigns.feedback.enabled"), false);
				} else {
					internalToggleSign(player);
					source.sendSuccess(() -> Component.translatable("text.poweredsigns.feedback.disabled"), false);
				}
			}
			case SERVER -> {
				if (value) {
					noPrintPlayers.remove(player);
					source.sendSuccess(() -> Component.literal("§ePowered signs will now send you messages.§r"), false);
				} else {
					internalToggleSign(player);
					source.sendSuccess(() -> Component.literal("§ePowered signs will no longer send you messages.§r"), false);
				}
			}
		}
		return 1;
	}
}

