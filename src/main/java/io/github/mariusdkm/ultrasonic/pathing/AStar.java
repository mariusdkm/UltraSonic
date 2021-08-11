package io.github.mariusdkm.ultrasonic.pathing;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import xyz.wagyourtail.jsmacros.client.api.classes.Draw3D;
import xyz.wagyourtail.jsmacros.client.api.library.impl.FHud;
import xyz.wagyourtail.jsmacros.client.movement.MovementDummy;

import java.util.Collection;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

import static io.github.mariusdkm.ultrasonic.api.Pathing.getPath;

public class AStar {
    private final ClientPlayerEntity player;
    private final BlockPos start;
    private final BlockPos goal;
    private final Draw3D scoreBlocks;
    private final Simple2dPathFinder simple2DPathFinder;
    private final Adv3dPathFinder adv3dPathFinder;

    public AStar(ClientPlayerEntity player, BlockPos start, BlockPos goal, boolean allowSprint) throws Exception {
        this.player = player;
        this.start = start;
        this.goal = goal;

        if (!Cache.isWalkable(player.world, start)) {
            throw new Exception("Start is not walkable!");
        }
        if (!Cache.isWalkable(player.world, goal)) {
            throw new Exception("Goal is not walkable!");
        }


        this.scoreBlocks = new Draw3D();
        synchronized (FHud.renders) {
            FHud.renders.add(scoreBlocks);
        }

        this.simple2DPathFinder = new Simple2dPathFinder(start, goal, allowSprint);
        this.adv3dPathFinder = new Adv3dPathFinder(start, goal, allowSprint);
    }

    public Node findPath() {
        PriorityQueue<Node> queue = new PriorityQueue<>(new Node.NodeComparator());
        Set<Node> closedSet = new HashSet<>();
        Draw3D closedSetBlocks = new Draw3D();

        synchronized (FHud.renders) {
            FHud.renders.add(closedSetBlocks);
        }

        Node currentNode = new Node(start, 0, 0, new MovementDummy(player));
        queue.add(currentNode);

        boolean routeAvailable = false;
        while (!queue.isEmpty()) {
            do {
                if (queue.isEmpty()) break;
                currentNode = queue.poll();
            } while (closedSet.contains(currentNode));
            closedSet.add(currentNode);

            if (currentNode.score == Integer.MAX_VALUE) {
                // If we reach this, there is no path to the goal
                break;
            }

//            closedSetBlocks.addPoint(new PositionCommon.Pos3D(currentNode.pos.getX() + 0.5D, currentNode.pos.getY() + 0.5D, currentNode.pos.getZ() + 0.5D), 0.5, 0xfcdb03);
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
            Collection<Node> newNodes = adv3dPathFinder.calcNode(currentNode, currentScore, closedSet);
            /* Check if the nodes already exist in the queue, and removing duplicates that are worse
               Without this, the queue would get very big
             */
            OuterLoop:
            for (Object newNode : newNodes.toArray()) {
                // This is basically queue.contains(), but we immediately do smth if it's in the queue
                for (Object oldNode : queue.toArray()) {
                    // Check if node already exists in the queue
                    if (oldNode.equals(newNode)) {
                        if (((Node) oldNode).score > ((Node) newNode).score) {
                            queue.remove(oldNode);
                            queue.add((Node) newNode);
                        }
                        continue OuterLoop;
                    }
                }
                queue.add((Node) newNode);
            }

        }
        synchronized (FHud.renders) {
            FHud.renders.remove(closedSetBlocks);
        }
        if (routeAvailable)
            return currentNode;
        else
            return null;
    }
}
