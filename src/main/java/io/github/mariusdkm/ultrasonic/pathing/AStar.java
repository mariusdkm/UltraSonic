package io.github.mariusdkm.ultrasonic.pathing;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import xyz.wagyourtail.jsmacros.client.api.classes.Draw3D;
import xyz.wagyourtail.jsmacros.client.api.library.impl.FHud;
import xyz.wagyourtail.jsmacros.client.movement.MovementDummy;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.PriorityBlockingQueue;

import static io.github.mariusdkm.ultrasonic.api.Pathing.getPath;

public class AStar extends AbstractExecutionThreadService {
    public final BlockPos start;
    public final BlockPos goal;
    private final ClientPlayerEntity player;
    private final Draw3D scoreBlocks;
    private final Simple2dPathFinder simple2DPathFinder;
    private final Adv3dPathFinder adv3dPathFinder;
    public Optional<Node> result = Optional.absent();

    public AStar(ClientPlayerEntity player, BlockPos start, BlockPos goal, boolean allowSprint) throws Exception {
        this.player = player;

        if (!Caches.WALKABLE.get(start)) {
            if (Caches.WALKABLE.get(start.add(0, 1, 0))) {
                this.start = start.add(0, 1, 0);
            } else {
                throw new Exception("Start is not walkable!");
            }
        } else {
            this.start = start;
        }

        if (!Caches.WALKABLE.get(goal)) {
            if (Caches.WALKABLE.get(goal.add(0, 1, 0))) {
                this.goal = goal.add(0, 1, 0);
            } else if (Caches.WALKABLE.get(goal.add(0, -1, 0))) {
                this.goal = goal.add(0, -1, 0);
            } else {
                throw new Exception("Goal is not walkable!");
            }
        } else {
            this.goal = goal;
        }

        this.scoreBlocks = new Draw3D();
        synchronized (FHud.renders) {
            FHud.renders.add(scoreBlocks);
        }

        // TODO Temporary fix since some block updates aren't registered
        Caches.WALKABLE.invalidateAll();

        this.simple2DPathFinder = new Simple2dPathFinder(this.start, this.goal, allowSprint);
        this.adv3dPathFinder = new Adv3dPathFinder(this.start, this.goal, allowSprint);
    }

    @Override
    protected void run() {
        Queue<Node> queue = new PriorityBlockingQueue<>(50, new Node.NodeComparator());
        Set<Node> closedSet = new HashSet<>();

        Node currentNode = new Node(start, 0, 0, new MovementDummy(player));
        queue.add(currentNode);

        boolean routeAvailable = false;
        while (!queue.isEmpty() && isRunning()) {
            do {
                if (queue.isEmpty()) break;
                currentNode = queue.poll();
            } while (closedSet.contains(currentNode));
            closedSet.add(currentNode);

            if (currentNode.score == Integer.MAX_VALUE) {
                // If we reach this, there is no path to the goal
                break;
            }

            for (Draw3D.Line line : scoreBlocks.getLines()) {
                // I hope this doesn't impact the performance to much
                scoreBlocks.removeLine(line);
            }
            for (Node node : getPath(currentNode)) {
                if (node.prevNode != null) {
                    scoreBlocks.addLine(node.pos.getX() + 0.5D, node.pos.getY() + 1, node.pos.getZ() + 0.5D, node.prevNode.pos.getX() + 0.5D, node.prevNode.pos.getY() + 1D, node.prevNode.pos.getZ() + 0.5D, 0xde070a);
                }
            }

            int currentScore = currentNode.score;

            if (currentNode.pos.equals(goal)) {
                // at the end, return path
                routeAvailable = true;
                break;
            }
            List<CompletableFuture<Node>> newNodes = adv3dPathFinder.calcNode(currentNode, currentScore, closedSet);
            /* Check if the nodes already exist in the queue, and removing duplicates that are worse
               Without this, the queue would get very big
             */
            final Node[] queueArray = queue.toArray(new Node[0]);
            queue.addAll(newNodes.stream().parallel().map(CompletableFuture::join).map((Node newNode) -> {
                if (newNode == null) {
                    return null;
                }
                for (Node oldNode : queueArray) {
                    // Check if node already exists in the queue
                    if (oldNode.equals(newNode)) {
                        if (oldNode.score > newNode.score) {
                            queue.remove(oldNode);
                            return newNode;
                        } else {
                            return null;
                        }
                    }
                }
                return newNode;
            }).filter(Objects::nonNull).toList());
        }
        synchronized (FHud.renders) {
            FHud.renders.remove(scoreBlocks);
        }
        result = routeAvailable ? Optional.of(currentNode) : Optional.absent();
    }
}
