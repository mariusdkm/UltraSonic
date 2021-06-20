package io.github.mariusdkm.ultrasonic.api;

import io.github.mariusdkm.ultrasonic.pathing.Simple2DAStar;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import xyz.wagyourtail.jsmacros.core.library.BaseLibrary;
import xyz.wagyourtail.jsmacros.core.library.Library;

@SuppressWarnings("unused")
@Library("Pathing")
public class Pathing extends BaseLibrary {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public Simple2DAStar pathTo(int x, int y, int z, boolean allowSprint) {
        assert mc.player != null;
        return new Simple2DAStar(mc.player, mc.player.getBlockPos().down(), new BlockPos(x, y, z), allowSprint);
    }
}
