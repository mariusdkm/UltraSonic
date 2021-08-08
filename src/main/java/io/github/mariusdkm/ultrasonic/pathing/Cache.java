package io.github.mariusdkm.ultrasonic.pathing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
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
        return !world.getBlockState(pos).getCollisionShape(world, pos).isEmpty() && isSafe(world.getBlockState(pos)) &&
                world.getBlockState(pos.up()).getCollisionShape(world, pos).isEmpty() && isSafe(world.getBlockState(pos.up())) &&
                world.getBlockState(pos.up(2)).getCollisionShape(world, pos).isEmpty() && isSafe(world.getBlockState(pos.up(2)));
    }

    public static boolean isSafe(BlockState state) {
        /**
         * Check that the block does not override onEntityCollision like {@link net.minecraft.block.WitherRoseBlock#onEntityCollision}
         **/
        return Arrays.stream(state.getBlock().getClass().getDeclaredMethods()).filter(method ->
                method.getName().equals("method_26180") || method.getName().equals("onEntityCollision")
        ).findFirst().isEmpty();
    }
}
