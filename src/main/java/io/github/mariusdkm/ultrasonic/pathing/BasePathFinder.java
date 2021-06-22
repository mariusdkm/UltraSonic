package io.github.mariusdkm.ultrasonic.pathing;

import net.minecraft.util.math.BlockPos;
import xyz.wagyourtail.jsmacros.client.api.classes.Draw3D;
import xyz.wagyourtail.jsmacros.client.api.library.impl.FHud;

import java.util.Collection;
import java.util.Set;

public abstract class BasePathFinder {
    protected final BlockPos start;
    protected final BlockPos goal;
    protected final boolean allowSprint;
    protected Draw3D scoreBlocks;

    public BasePathFinder(BlockPos start, BlockPos goal, boolean allowSprint) {
        this.start = start;
        this.goal = goal;
        this.allowSprint = allowSprint;

        this.scoreBlocks = new Draw3D();
        synchronized (FHud.renders) {
            FHud.renders.add(scoreBlocks);
        }
    }

    public abstract Collection<Node> calcNode(Node currentNode, int currentScore, Set<Node> closedSet);
}
