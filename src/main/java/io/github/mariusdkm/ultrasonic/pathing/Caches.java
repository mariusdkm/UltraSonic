package io.github.mariusdkm.ultrasonic.pathing;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.util.Arrays;

public class Caches {
    public static final Caches INSTANCE = new Caches();
    private final LoadingCache<BlockPos, Boolean> WALKABLE = CacheBuilder.newBuilder()
        .maximumSize(1024)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build(
            new CacheLoader<BlockPos, Boolean>() {
                @Override
                public Boolean load(BlockPos key) {
                    assert MinecraftClient.getInstance().player != null;
                    return isWalkable(MinecraftClient.getInstance().player.world, key);
                }
            }
        );
    private final Set<Block> UNSAFE = Registry.BLOCK.stream().filter(block -> !isSafe(block)).collect(Collectors.toCollection(HashSet::new));

    public static LoadingCache<BlockPos, Boolean> getWalkable() {
        return INSTANCE.WALKABLE;
    }

    public static boolean testWalkable(World world, BlockPos pos) {
        if (isWalkable(world, pos)) {
            getWalkable().put(pos, true);
            return true;
        } else {
            getWalkable().invalidate(pos);
            getWalkable().put(pos, false);
            return false;
        }
    }

    public static boolean isWalkable(World world, BlockPos pos) {
        return !world.getBlockState(pos).getCollisionShape(world, pos).isEmpty() && isSafe(world.getBlockState(pos)) &&
                world.getBlockState(pos.up()).getCollisionShape(world, pos).isEmpty() && isSafe(world.getBlockState(pos.up())) &&
                world.getBlockState(pos.up(2)).getCollisionShape(world, pos).isEmpty() && isSafe(world.getBlockState(pos.up(2)));
    }

    /**
     * Check that the block does not override onEntityCollision like {@link net.minecraft.block.WitherRoseBlock#onEntityCollision}
     **/
    public static boolean isSafe(Block block) {
        return Arrays.stream(block.getClass().getDeclaredMethods()).filter(method ->
            method.getName().equals("method_26180") || method.getName().equals("onEntityCollision")
        ).findFirst().isEmpty();
    }

    public static boolean isSafe(BlockState state) {
        return state.getFluidState().isEmpty() && !INSTANCE.UNSAFE.contains(state.getBlock()) && isSafe(state.getBlock());
    }
}
