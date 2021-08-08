package io.github.mariusdkm.ultrasonic.pathing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.BlockPos;

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

    // TODO: Block Event Listeners to invalidate walkable cache
}
