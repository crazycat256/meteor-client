/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.joml.Math.floor;

public class MovementUtils {

    public static void TPX(Vec3d pos, Vec3d startPos)  {

        if (mc.player.isSneaking()) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        }

        double distance = startPos.distanceTo(pos);

        int packetsRequired = (int) Math.ceil(Math.abs(distance / 10));
        for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
        }

        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true));
    }
    public static void TPX(Vec3d pos) {
        TPX(pos, mc.player.getPos());
    }

    public static Vec3i Vec3d2Vec3i(Vec3d vec3d) {
        return new Vec3i((int) floor(vec3d.x), (int) floor(vec3d.y), (int) floor(vec3d.z));
    }
}
