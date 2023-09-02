/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.MovementUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class HClipCommand extends Command {
    public HClipCommand() {
        super("hclip", "Lets you clip through blocks horizontally.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("blocks", DoubleArgumentType.doubleArg()).executes(context -> {

            double blocks = context.getArgument("blocks", Double.class);
            Vec3d forward = Vec3d.fromPolar(0, mc.player.getYaw()).normalize();
            int packetsRequired = (int) Math.ceil(Math.abs(blocks / 10));

            if (mc.player.hasVehicle()) {
                Entity vehicle = mc.player.getVehicle();
                for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                    mc.player.networkHandler.sendPacket(new VehicleMoveC2SPacket(mc.player.getVehicle()));
                }

                mc.player.getVehicle().setPosition(vehicle.getX() + forward.x * blocks, vehicle.getY(), vehicle.getZ() + forward.z * blocks);
                mc.player.networkHandler.sendPacket(new VehicleMoveC2SPacket(mc.player.getVehicle()));
            } else {
                Vec3d newPos = mc.player.getPos().add(forward.multiply(blocks));
                MovementUtils.TPX(newPos);
                mc.player.updatePosition(newPos.x, newPos.y, newPos.z);
            }

            return SINGLE_SUCCESS;

        }));
    }
}
