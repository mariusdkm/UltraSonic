package io.github.mariusdkm.ultrasonic.pathing;

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
    private final BlockPos start;
    private final BlockPos goal;
    private final ClientPlayerEntity player;
    private final Draw3D scoreBlocks;
    // private final Simple2dPathFinder simple2DPathFinder;
    private final Adv3dPathFinder adv3dPathFinder;
    private Optional<Node> result = Optional.empty();

    public AStar(ClientPlayerEntity player, BlockPos start, BlockPos goal, boolean allowSprint) throws Exception {
        this.player = player;

        if (!Caches.WALKABLE.get(start)) {
            if (Caches.WALKABLE.get(start.add(0, 1, 0))) {
                this.start = start.add(0, 1, 0);
            } else {
                throw new IllegalStateException("Start is not walkable!");
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
                throw new IllegalStateException("Goal is not walkable!");
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

        // this.simple2DPathFinder = new Simple2dPathFinder(this.start, this.goal, allowSprint);
        this.adv3dPathFinder = new Adv3dPathFinder(this.start, this.goal, allowSprint);
    }

    public BlockPos getGoal() {
        return goal;
    }

    public Optional<Node> getResult() {
        return result;
    }

    @Override
    protected void run() {
        // r^2 * PI should be 75, but a smaller initial Capacity might actually be better
        Queue<Node> queue = new PriorityBlockingQueue<>(50, Node.nodeComparator);
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
                    scoreBlocks.addLine(node.pos.getX() + 0.5, node.pos.getY() + 1, node.pos.getZ() + 0.5,
                            node.prevNode.pos.getX() + 0.5, node.prevNode.pos.getY() + 1, node.prevNode.pos.getZ() + 0.5, 0xde070a);
                }
            }

            if (currentNode.pos.equals(goal)) {
                // at the end, return path
                routeAvailable = true;
                break;
            }

            int currentScore = currentNode.score;

            Queue<CompletableFuture<Node>> newNodes = adv3dPathFinder.calcNode(currentNode, currentScore, closedSet);
            // Check if the nodes already exist in the queue, and removing duplicates that are worse
            // Without this, the queue would get very big
            Node[] queueArray = queue.toArray(new Node[0]);
            newNodes.stream().parallel().map(CompletableFuture::join).forEach(newNode -> {
                if (newNode == null) {
                    return;
                }
                for (Node oldNode : queueArray) {
                    // Check if node already exists in the queue
                    if (oldNode.equals(newNode)) {
                        if (oldNode.score > newNode.score) {
                            queue.remove(oldNode);
                            queue.add(newNode);
                        } else {
                            return;
                        }
                    }
                }
                queue.add(newNode);
            });
        }
        synchronized (FHud.renders) {
            FHud.renders.remove(scoreBlocks);
        }
        result = routeAvailable ? Optional.of(currentNode) : Optional.empty();
    }
}
