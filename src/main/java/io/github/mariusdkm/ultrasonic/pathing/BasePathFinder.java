package io.github.mariusdkm.ultrasonic.pathing;

import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public abstract class BasePathFinder {
    protected final BlockPos start;
    protected final BlockPos goal;
    protected final boolean allowSprint;

    public BasePathFinder(BlockPos start, BlockPos goal, boolean allowSprint) {
        this.start = start;
        this.goal = goal;
        this.allowSprint = allowSprint;
    }

    public abstract List<CompletableFuture<Node>> calcNode(Node currentNode, int currentScore, Set<Node> closedSet);
}
