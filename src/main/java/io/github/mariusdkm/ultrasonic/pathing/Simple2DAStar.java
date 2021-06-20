package io.github.mariusdkm.ultrasonic.pathing;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import xyz.wagyourtail.jsmacros.client.api.classes.Draw3D;
import xyz.wagyourtail.jsmacros.client.api.classes.PlayerInput;
import xyz.wagyourtail.jsmacros.client.api.library.impl.FHud;
import xyz.wagyourtail.jsmacros.client.api.sharedclasses.PositionCommon;
import xyz.wagyourtail.jsmacros.client.movement.MovementDummy;

import java.util.*;

public class Simple2DAStar {
    private final ClientPlayerEntity player;
    private final World world;
    private final BlockPos start;
    private final BlockPos goal;
    private final boolean allowSprint;
    public Draw3D scoreBlocks;

    public Simple2DAStar(ClientPlayerEntity player, BlockPos start, BlockPos goal, boolean allowSprint) {
        this.player = player;
        this.world = player.getEntityWorld();
        this.start = start;
        this.goal = goal;
        this.allowSprint = allowSprint;

        this.scoreBlocks = new Draw3D();
        synchronized (FHud.renders) {
            FHud.renders.add(scoreBlocks);
        }
    }

    public Node createPath() {
        PriorityQueue<Node> queue = new PriorityQueue<>(new Node.NodeComparator());
        Set<Node> closedSet = new HashSet<>();
        Draw3D closedSetBlocks = new Draw3D();

        synchronized (FHud.renders) {
            FHud.renders.add(closedSetBlocks);
        }

        Node currentNode = new Node(start, 0, 0, new MovementDummy(player));
        queue.add(currentNode);

        int maxScore = 0;
        boolean routeAvailable = false;
        while (!queue.isEmpty()) {
            do {
                if (queue.isEmpty()) break;
                currentNode = queue.poll();
            } while (closedSet.contains(currentNode));

            closedSet.add(currentNode);
//            closedSetBlocks.addPoint(new PositionCommon.Pos3D(currentNode.pos.getX() + 0.5D, currentNode.pos.getY() + 0.5D, currentNode.pos.getZ() + 0.5D), 0.5, 0xfcdb03);
            for (Draw3D.Line line : scoreBlocks.getLines()) {
                // I hope this doesn't impact the performance to much
                scoreBlocks.removeLine(line);
            }
            for (Node node : getPath(currentNode)) {
                if (node.prevNode != null) {
                    scoreBlocks.addLine(node.pos.getX() + 0.5D, node.pos.getY() + 1.5D, node.pos.getZ() + 0.5D, node.prevNode.pos.getX() + 0.5D, node.prevNode.pos.getY() + 1.5D, node.prevNode.pos.getZ() + 0.5D, 0xde070a);
                }
            }

            int currentScore = currentNode.score;

            if (currentNode.pos.equals(goal)) {
                // at the end, return path
                routeAvailable = true;
                break;
            }

            for (int x = currentNode.pos.getX() - 1; x <= currentNode.pos.getX() + 1; x += 1) {
                ZLoop:
                for (int z = currentNode.pos.getZ() - 1; z <= currentNode.pos.getZ() + 1; z += 1) {
                    if (currentNode.pos.getX() == x && currentNode.pos.getZ() == z) {
                        continue;
                    }
                    Node newNode = new Node(new BlockPos(x, currentNode.pos.getY(), z), 0, currentNode.distTravel, currentNode.player);
                    if (!closedSet.contains(newNode) && newNode.isWalkable()) {
                        newNode.score = calcScore(newNode, currentScore);
                        if (newNode.score > maxScore) {
                            maxScore = newNode.score;
                        }
                        newNode.prevNode = currentNode;
//                        LOGGER.info("Node x={}, y={}, z={}, score={}", newNode.pos.getX(), newNode.pos.getY(), newNode.pos.getZ(), newNode.score);
//                        visualizePath(getPath(currentNode));
                        for (Object oldNode : queue.toArray()) {
                            // Check if node already exists in the queue
                            if (oldNode.equals(newNode)) {
                                if (((Node) oldNode).score > newNode.score) {
                                    queue.remove(oldNode);
                                    queue.add(newNode);
                                }
                                continue ZLoop;
                            }
                        }
                        queue.add(newNode);
                    }
                }
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

    private int findBestPath(Node node) {
        int cost;
        boolean directPath = this.world.raycast(new RaycastContext(node.player.getCameraPosVec(1.0F).subtract(0, 1, 0),
                new Vec3d(node.pos.getX() + 0.5, node.pos.getY() + 1.5, node.pos.getZ() + 0.5),
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.ANY, node.player)).getType() == HitResult.Type.MISS;
        Box goal = new Box(node.pos.add(0, 1, 0));
        // ab = B - A [-(node.pos.getX() + 0.5D) = -node.pos.getX() - 0.5D]
        if (directPath) {
            cost = 0;
//            Optional<Vec3d> hit = new Box(node.pos.add(0, 0, 0)).raycast(player.getPos(), MovementHelper.vec3dFromBlockPos(node.pos, true));
//            if(!hit.isPresent()) {
//                LOGGER.info("WTF WHY IS THERE NO RESULT");
//            } else {
//
//            }
            while (!goal.intersects(node.player.getBoundingBox())) {
                cost += 1;
                Vec3d vecToBlock = node.player.getPos().subtract(node.pos.getX() + 0.5D, node.pos.getY() + 0.5D, node.pos.getZ() + 0.5D).multiply(-1);
                float yaw = (float) (MovementHelper.calcXZAngle(vecToBlock));
                PlayerInput newInput = new PlayerInput(1.0F, 0.0F, yaw, 0.0F, false, false, allowSprint);
                MovementDummy testSubject = node.player.clone();
                testSubject.applyInput(newInput);
                if (testSubject.horizontalCollision) {
                    // The player collided with a wall, that means on of his velocity vectors is set to 0
                    // --> we loose momentum/speed
                    double diff = testSubject.getPos().squaredDistanceTo(node.player.getPos());
                    // That's why we move in the direction the wall pushes us, which is usually parallel to the wall
                    yaw = (float) (MovementHelper.calcXZAngle(testSubject.getVelocity()));
                    testSubject = node.player.clone();
                    newInput.yaw = yaw;
                    testSubject.applyInput(newInput);
                    // But do we actually travel further with the new yaw?
                    if (diff > testSubject.getPos().squaredDistanceTo(node.player.getPos())) {
                        node.distTravel += diff;
                        yaw = (float) (MovementHelper.calcXZAngle(vecToBlock));
                    } else if (diff == 0.0 && testSubject.getPos().squaredDistanceTo(node.player.getPos()) == 0.0) {
                        return Integer.MAX_VALUE;
                    } else {
                        node.distTravel += testSubject.getPos().squaredDistanceTo(node.player.getPos());
                    }
                }
                // TODO is ot faster to use the testSubject, or to recalc the move?
                newInput.yaw = yaw;
                node.player.applyInput(newInput);

            }
        } else {
            cost = Integer.MAX_VALUE;
        }
//        double maxDistance = node.pos.getSquaredDistance(new Vec3i(node.player.getX(), node.player.getY(), node.p layer.getZ()), true);
        // Vec3d dirVec = player.getPos().add(player.getVelocity().normalize().multiply(maxDistance));

        return cost;
    }

    private int calcScore(Node newNode, int pastCost) {
        // h = how many tick is it gonna take to finish
        // h = distanceToGoal / avgSpeed; avgSpeed = distanceFromStart / timeSpend
        // h = distanceToGoal * timeSpend / distanceFromStart
        // avgWalkingSpeed = 0.21585
//        int heuristic = (int) (newNode.pos.getSquaredDistance(goal) / 0.2);

        int movementCost = findBestPath(newNode);
        if (movementCost == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        int heuristic;
        double avgSpeed = newNode.distTravel / newNode.player.getInputs().size();
        if (avgSpeed > 0.05) {
            heuristic = (int) (newNode.pos.getSquaredDistance(goal) / avgSpeed);
        } else {
            heuristic = (int) (newNode.pos.getSquaredDistance(goal) / 0.1);
        }

        return movementCost + heuristic + pastCost;
    }

    public List<Node> getPath(Node currentNode) {
        List<Node> path = new ArrayList<>();
        while (currentNode != null) {
            path.add(currentNode);
            currentNode = currentNode.prevNode;
        }
        return path;
    }

    public List<PlayerInput> getInputs(Node currentNode) {
        return currentNode.player.getInputs();
    }

    public void visualizePath(List<Node> path) {
        if (FHud.renders.contains(scoreBlocks)) {
            synchronized (FHud.renders) {
                FHud.renders.remove(scoreBlocks);
            }
        }
        scoreBlocks = new Draw3D();
        synchronized (FHud.renders) {
            FHud.renders.add(scoreBlocks);
        }
        for (Node node : path) {
            scoreBlocks.addPoint(new PositionCommon.Pos3D(node.pos.getX() + 0.5D, node.pos.getY() + 0.5D, node.pos.getZ() + 0.5D), 0.5, 0xde070a);
//            scoreBlocks.addLine(node.pos.getX() + 0.5D, node.pos.getY() + 1.5D, node.pos.getZ() + 0.5D, node.prevNode.pos.getX() + 0.5D, node.prevNode.pos.getY() + 1.5D, node.prevNode.pos.getZ() + 0.5D, 0xde070a);
        }

//        int redValue = (int) (newNode.score > maxScore / 2 ? 1 - 2 * (newNode.score - maxScore / 2) / maxScore : 1.0) * 255;
//        int greenValue = (int) (newNode.score > maxScore / 2 ? 1.0 : 2 * newNode.score / maxScore) * 255;
//        int blueValue = 0;
//        int color = ((redValue & 0xFF) << 16) | ((greenValue & 0xFF) << 8);
//        scoreBlocks.addPoint(new PositionCommon.Pos3D(newNode.pos.getX() + 0.5D, newNode.pos.getY() + 0.5D, newNode.pos.getZ() + 0.5D), 0.5, color);

    }
}
