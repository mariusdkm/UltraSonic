package io.github.mariusdkm.ultrasonic.pathing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Cache {
    public static final Cache INSTANCE = new Cache();
    private final List<BlockPos> WALKABLE = new ArrayList<>();

    public static List<BlockPos> getWalkable() {
        return INSTANCE.WALKABLE;
    }

    public static void addWalkable(BlockPos pos) {
        while (INSTANCE.WALKABLE.size() >= 256) {
            INSTANCE.WALKABLE.remove(0);
        }
	    INSTANCE.WALKABLE.add(pos);
    }

    public static boolean isWalkable(World world, BlockPos pos) {
        return !world.getBlockState(pos).getCollisionShape(world, pos).isEmpty() &&
                world.getBlockState(pos.up()).isAir() &&
                world.getBlockState(pos.up(2)).isAir();
    }
}
