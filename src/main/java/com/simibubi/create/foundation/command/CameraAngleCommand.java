package com.simibubi.create.foundation.command;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.simibubi.create.foundation.utility.CameraAngleAnimationService;

import net.createmod.catnip.net.ClientboundSimpleActionPacket;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public class CameraAngleCommand {

	public static ArgumentBuilder<CommandSourceStack, ?> register() {
		return Commands.literal("angle")
				.requires(cs -> cs.hasPermission(2))
				.then(Commands.argument("players", EntityArgument.players())
						.then(Commands.literal("yaw")
								.then(Commands.argument("degrees", FloatArgumentType.floatArg())
										.executes(context -> updateCameraAngle(context, true))
								)
						).then(Commands.literal("pitch")
								.then(Commands.argument("degrees", FloatArgumentType.floatArg())
										.executes(context -> updateCameraAngle(context, false))
								)
						).then(Commands.literal("mode")
								.then(Commands.literal("linear")
										.executes(context -> updateCameraAnimationMode(context, CameraAngleAnimationService.Mode.LINEAR.name()))
										.then(Commands.argument("speed", FloatArgumentType.floatArg(0))
												.executes(context -> updateCameraAnimationMode(context, CameraAngleAnimationService.Mode.LINEAR.name(), FloatArgumentType.getFloat(context, "speed")))
										)
								).then(Commands.literal("exponential")
										.executes(context -> updateCameraAnimationMode(context, CameraAngleAnimationService.Mode.EXPONENTIAL.name()))
										.then(Commands.argument("speed", FloatArgumentType.floatArg(0))
												.executes(context -> updateCameraAnimationMode(context, CameraAngleAnimationService.Mode.EXPONENTIAL.name(), FloatArgumentType.getFloat(context, "speed")))
										)
								)
						)
				);
	}

	private static int updateCameraAngle(CommandContext<CommandSourceStack> ctx, boolean yaw) throws CommandSyntaxException {
		AtomicInteger targets = new AtomicInteger(0);

		float angleTarget = FloatArgumentType.getFloat(ctx, "degrees");
		String optionName = yaw ? "camAngleYawTarget" : "camAnglePitchTarget";

		getPlayersFromContext(ctx).forEach(player -> {
			CatnipServices.NETWORK.sendToPlayer(
					player,
					new ClientboundSimpleActionPacket(optionName, String.valueOf(angleTarget)));
			targets.incrementAndGet();
		});

		return targets.get();
	}

	private static int updateCameraAnimationMode(CommandContext<CommandSourceStack> ctx, String value) throws CommandSyntaxException {
		AtomicInteger targets = new AtomicInteger(0);

		getPlayersFromContext(ctx).forEach(player -> {
			CatnipServices.NETWORK.sendToPlayer(
					player,
					new ClientboundSimpleActionPacket("camAngleFunction", value)
			);
			targets.incrementAndGet();
		});

		return targets.get();
	}

	private static int updateCameraAnimationMode(CommandContext<CommandSourceStack> ctx, String value, float speed) throws CommandSyntaxException {
		AtomicInteger targets = new AtomicInteger(0);

		getPlayersFromContext(ctx).forEach(player -> {
			CatnipServices.NETWORK.sendToPlayer(
					player,
					new ClientboundSimpleActionPacket("camAngleFunction", value + ":" + speed)
			);
			targets.incrementAndGet();
		});

		return targets.get();
	}

	private static Collection<ServerPlayer> getPlayersFromContext(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return EntityArgument.getPlayers(ctx, "players");
	}
}
