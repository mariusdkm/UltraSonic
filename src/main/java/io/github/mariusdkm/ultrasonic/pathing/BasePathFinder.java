package io.github.mariusdkm.ultrasonic.pathing;

import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.Set;

public abstract class BasePathFinder {
    protected final BlockPos start;
    protected final BlockPos goal;
    protected final boolean allowSprint;

    public BasePathFinder(BlockPos start, BlockPos goal, boolean allowSprint) {
        this.start = start;
        this.goal = goal;
        this.allowSprint = allowSprint;
    }

    public abstract Collection<Node> calcNode(Node currentNode, int currentScore, Set<Node> closedSet);
}
