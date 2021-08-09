package io.github.mariusdkm.ultrasonic.mixins;

import io.github.mariusdkm.ultrasonic.pathing.Cache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class MixinClientConnection {
    @Inject(
            at = {@At("HEAD")},
            method = {"handlePacket"},
            cancellable = true
    )
    private static void onPacketReceive(Packet<?> packet, PacketListener listener, CallbackInfo info) {
        if (MinecraftClient.getInstance().player == null) return;
        World world = MinecraftClient.getInstance().player.world;

        if (packet instanceof BlockUpdateS2CPacket) {
            BlockPos pos = ((BlockUpdateS2CPacket) packet).getPos();
            Cache.testWalkable(world, pos);
            Cache.testWalkable(world, pos.down());
            Cache.testWalkable(world, pos.down(2));
        } else if (packet instanceof UnloadChunkS2CPacket ucp) {
            Cache.getWalkable().removeAll(Cache.getWalkable().stream().filter(pos -> pos.getX() << 4 == ucp.getX() && pos.getZ() == ucp.getZ()).toList());
        }
    }
}
