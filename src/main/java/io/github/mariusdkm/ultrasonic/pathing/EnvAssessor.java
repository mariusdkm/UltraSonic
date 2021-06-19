package io.github.mariusdkm.ultrasonic.pathing;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import xyz.wagyourtail.jsmacros.client.movement.MovementDummy;

import java.util.HashMap;

public class EnvAssessor {
    private MovementDummy dummy;
    private BlockPos goal;
    private HashMap<BlockPos, Double> eval;

    public EnvAssessor(MovementDummy dummy, BlockPos goal) {
        this.dummy = dummy;
        this.goal = goal;
        this.eval = new HashMap<>();
    }

    public HashMap<BlockPos, Double> assessEnviroment(int radius) {
        for(int x = -radius; x < radius; x++) {
            for(int z = -radius; z < radius; z++) {
                BlockPos pos = new BlockPos(dummy.getX() + x, dummy.getY(), dummy.getZ() + z);
                Block block = dummy.world.getBlockState(pos).getBlock();
                double value = 0;
                if (block != Blocks.AIR) {
                    value = (Math.sqrt(goal.getSquaredDistance(pos)) + Math.sqrt(x * x + z * z));
                }
                this.eval.put(pos.toImmutable(), value);
            }
        }
        return eval;
    }
}
