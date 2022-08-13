package io.github.mariusdkm.ultrasonic.pathing;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.github.mariusdkm.ultrasonic.api.Pathing;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Caches {
    private static final Set<Class<? extends Block>> DAMAGE_BLOCK = Set.of(
            WitherRoseBlock.class,
            AbstractFireBlock.class,
            LavaCauldronBlock.class,
            CactusBlock.class,
            CampfireBlock.class,
            SweetBerryBushBlock.class
    );

    public static final LoadingCache<BlockPos, Boolean> WALKABLE = CacheBuilder.newBuilder()
            .maximumSize(1024)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<>() {
                        @Override
                        public @NotNull Boolean load(@NotNull BlockPos key) {
                            assert MinecraftClient.getInstance().player != null;
                            return isWalkable(MinecraftClient.getInstance().player.world, key);
                        }
                    }
            );

    public static boolean isWalkable(World world, BlockPos pos) {
        return !world.getBlockState(pos).getCollisionShape(world, pos).isEmpty() && isSafe(world.getBlockState(pos), Pathing.immuneToDamage) &&
                world.getBlockState(pos.up()).getCollisionShape(world, pos).isEmpty() && isSafe(world.getBlockState(pos.up()), Pathing.immuneToDamage) &&
                world.getBlockState(pos.up(2)).getCollisionShape(world, pos).isEmpty() && isSafe(world.getBlockState(pos.up(2)), Pathing.immuneToDamage);
    }

    public static boolean isSafe(BlockState state, boolean allowDamage) {
        return state.getFluidState().isEmpty() && (allowDamage || !Caches.DAMAGE_BLOCK.contains(state.getBlock().getClass()));
    }
}
