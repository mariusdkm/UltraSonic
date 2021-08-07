package io.github.mariusdkm.ultrasonic.pathing;

import java.util.stream.IntStream;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import xyz.wagyourtail.jsmacros.client.api.classes.Draw3D;
import xyz.wagyourtail.jsmacros.client.api.classes.PlayerInput;
import xyz.wagyourtail.jsmacros.client.api.library.impl.FHud;
import xyz.wagyourtail.jsmacros.client.movement.MovementDummy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static io.github.mariusdkm.ultrasonic.pathing.MovementHelper.*;

public class Adv3dPathFinder extends BasePathFinder {
    private final Draw3D scoreBlocks;
    private final int[] sprintJumpDist = {4, 5, 5, 6, 6, 6, 7, 7, 7, 8};

    public Adv3dPathFinder(BlockPos start, BlockPos goal, boolean allowSprint) {
        super(start, goal, allowSprint);
        scoreBlocks = new Draw3D();
        synchronized (FHud.renders) {
            FHud.renders.add(scoreBlocks);
        }
    }

    @Override
    public Collection<Node> calcNode(Node currentNode, int currentScore, Set<Node> closedSet) {
        Collection<Node> queue = new HashSet<>();

        int maxDepthY = -3;

        for (int y = 1; y >= maxDepthY; y--) {
            double nextXn = 0;
            int yr = getRadius(y);
            forX:
            for (int x = 0; x <= yr; x++) {
                final double xn = nextXn;
                nextXn = (x + 1.0D) / yr;
                double nextZn = 0;
                for (int z = 0; z <= yr; z++) {
                    final double zn = nextZn;
                    nextZn = (z + 1.0D) / yr;
                    if (x == 0 && z == 0) {
                        continue;
                    }
                    if ((xn * xn) + (zn * zn) > 1) {
                        if (z == 0) {
                            break forX;
                        }
                        break;
                    }

                    // scoreBlocks.addPoint(x + currentNode.pos.getX() + 0.5, y + currentNode.pos.getY() + 0.5, z + currentNode.pos.getZ() + 0.5, 0.5, 0x4287f5);

                    queue.add(calcBlock(new Node(new BlockPos(x + currentNode.pos.getX(), y + currentNode.pos.getY(), z + currentNode.pos.getZ()), currentScore, currentNode.distTravel, currentNode.player), currentNode, closedSet));
                    queue.add(calcBlock(new Node(new BlockPos(-x + currentNode.pos.getX(), y + currentNode.pos.getY(), -z + currentNode.pos.getZ()), currentScore, currentNode.distTravel, currentNode.player), currentNode, closedSet));

                    if (x != 0 && z != 0) {
                        // We don't want the points at the axis doubled
                        queue.add(calcBlock(new Node(new BlockPos(x + currentNode.pos.getX(), y + currentNode.pos.getY(), -z + currentNode.pos.getZ()), currentScore, currentNode.distTravel, currentNode.player), currentNode, closedSet));
                        queue.add(calcBlock(new Node(new BlockPos(-x + currentNode.pos.getX(), y + currentNode.pos.getY(), z + currentNode.pos.getZ()), currentScore, currentNode.distTravel, currentNode.player), currentNode, closedSet));
                    }
                }
            }
        }
        queue.removeAll(Collections.singleton(null));
        return queue;
    }

    private int getRadius(int yDepth) {
        return sprintJumpDist[-yDepth + 1];

    }

    private Node calcBlock(Node newNode, Node currentNode, Set<Node> closedSet) {
        if (!closedSet.contains(newNode) && newNode.isWalkable()) {
            newNode.score = calcScore(newNode, currentNode.pos);
            newNode.prevNode = currentNode;
            // LOGGER.info("Node x={}, y={}, z={}, score={}", newNode.pos.getX(), newNode.pos.getY(), newNode.pos.getZ(), newNode.score);
            return newNode;
        }
        return null;
    }

    private int calcScore(Node newNode, BlockPos currentPos) {
        // h = how many tick is it going take to finish
        // h = distanceToGoal / avgSpeed; avgSpeed = distanceFromStart / timeSpend
        // h = distanceToGoal * timeSpend / distanceFromStart
        // avgWalkingSpeed = 0.21585
//        int heuristic = (int) (newNode.pos.getSquaredDistance(goal) / 0.2);

        int movementCost = 0;
        try {
            movementCost = findMovements(newNode, currentPos);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

        return newNode.score + movementCost + heuristic;
    }

    private int findMovements(Node node, BlockPos currentPos) throws InterruptedException {
        int cost;
        boolean directPath = isDirectPath(node.player, currentPos, node.pos);
        BlockPos blockToNode = node.pos.subtract(currentPos);

        Box startArea = node.player.world.getBlockState(currentPos).getCollisionShape(node.player.world, currentPos).getBoundingBox();
        startArea = new Box(startArea.getMin(Direction.Axis.X) + currentPos.getX(),
                startArea.getMax(Direction.Axis.Y) + currentPos.getY(),
                startArea.getMin(Direction.Axis.Z) + currentPos.getZ(),
                startArea.getMax(Direction.Axis.X) + currentPos.getX(),
                startArea.getMax(Direction.Axis.Y) + currentPos.getY() + 0.5,
                startArea.getMax(Direction.Axis.Z) + currentPos.getZ());
        Box goalArea = node.player.world.getBlockState(node.pos).getCollisionShape(node.player.world, node.pos).getBoundingBox();
        goalArea = new Box(goalArea.getMin(Direction.Axis.X) + node.pos.getX(),
                goalArea.getMax(Direction.Axis.Y) + node.pos.getY(),
                goalArea.getMin(Direction.Axis.Z) + node.pos.getZ(),
                goalArea.getMax(Direction.Axis.X) + node.pos.getX(),
                goalArea.getMax(Direction.Axis.Y) + node.pos.getY() + 0.5,
                goalArea.getMax(Direction.Axis.Z) + node.pos.getZ());

        // The point that is run/walked towards before jumping
        Vec3d runFocus;
        // The point that is run/walked towards while jumping
        Vec3d jumpFocus;
        if (blockToNode.getX() == 0 || blockToNode.getZ() == 0) {
            // The player should just run straight to the goal
            runFocus = new Vec3d(node.pos.getX() + 0.5, startArea.getMin(Direction.Axis.Y), node.pos.getZ() + 0.5);
            jumpFocus = new Vec3d(node.pos.getX() + 0.5, goalArea.getMin(Direction.Axis.Y), node.pos.getZ() + 0.5);
        } else {
            // We use the pos of the last node here, bc the player could still be on the corner of another block
            // The length is outside the block, so that the player can't reach it, without exiting the startArea
            runFocus = new Vec3d(currentPos.getX() + 0.5, startArea.getMin(Direction.Axis.Y), currentPos.getZ() + 0.5)
                    .add(createFocus(node.player.world, blockToNode, currentPos,
                            0.3 + Math.max(startArea.getXLength(), startArea.getZLength()),
                            Math.sqrt(2 * Math.pow(0.3 + Math.max(startArea.getXLength(), startArea.getZLength()) / 2, 2)),
                            Math.sqrt(2 * Math.pow(0.3 + Math.max(startArea.getXLength(), startArea.getZLength()) / 2, 2))));
            // Optimize? Math.sqrt(2 * Math.pow(0.3 + Math.max(startArea.getXLength(), startArea.getZLength()) / 2, 2))

            // The cornered length is outside the block, so that the player can
            jumpFocus = new Vec3d(node.pos.getX() + 0.5, goalArea.getMin(Direction.Axis.Y), node.pos.getZ() + 0.5)
                    .add(createFocus(node.player.world, blockToNode.multiply(-1), node.pos,
                            0.2 + Math.max(goalArea.getXLength(), goalArea.getZLength()),
                            Math.sqrt(2 * Math.pow(0.2 + Math.max(goalArea.getXLength(), goalArea.getZLength()) / 2, 2)),
                            0));
        }

        if (directPath) {
            cost = 0;
            MovementDummy testSubject = node.player.clone();
            MovementDummy prevTestSubject;
            while (!goalArea.intersects(testSubject.getBoundingBox()) || !testSubject.isOnGround()) {
                // Here the player moves towards its jumping position (runFocus),
                // While testing whether the goal could be reached
                if (Thread.interrupted()) {
                    // TODO fix this
                    throw new InterruptedException();
                }
                if (testSubject.getY() < runFocus.getY() && testSubject.getY() < jumpFocus.getY()) {
                    // We fell down
                    return Integer.MAX_VALUE;
                }
                cost += 1;
                prevTestSubject = testSubject.clone();

                float yaw = (float) (MovementHelper.calcAngleDegXZ(runFocus.subtract(testSubject.getPos())));
                PlayerInput newInput = new PlayerInput(1.0F, 0.0F, yaw, 0.0F, false, false, allowSprint);
                testSubject.applyInput(newInput);

                if (testSubject.horizontalCollision && doObstacleAvoidance(testSubject, prevTestSubject, newInput, jumpFocus, allowSprint) == 0.0) {
                    // We don't move at all, so something must be wrong
                    return Integer.MAX_VALUE;
                }

                if (MovementHelper.simulateJump(testSubject.clone(), jumpFocus, goalArea, allowSprint, MovementHelper.ticksToLand(blockToNode.getY()))) {
                    yaw = MovementHelper.angleToVec(testSubject.getPos(), jumpFocus);
                    newInput = new PlayerInput(1.0F, 0.0F, yaw, 0.0F, true, false, allowSprint);
                    testSubject.applyInput(newInput);
                    break;
                } else if (!startArea.intersects(testSubject.getBoundingBox()) || !testSubject.verticalCollision) {
                    // F*ck, we don't have enough momentum or something
                    return Integer.MAX_VALUE;
                    // TODO gain momentum
                }
            }
            // We are jumping, YEET
            while (!goalArea.intersects(testSubject.getBoundingBox()) || !testSubject.isOnGround()) {
                if (testSubject.getY() < runFocus.getY() && testSubject.getY() < jumpFocus.getY()) {
                    // We fell down
                    return Integer.MAX_VALUE;
                }
                if (testSubject.isOnGround() && !goalArea.intersects(testSubject.getBoundingBox())) {
                    // Well, we did some kind of jump, but we didn't land on our desired block
                    return Integer.MAX_VALUE;
                }
                cost += 1;
                prevTestSubject = testSubject.clone();

                float yaw = MovementHelper.angleToVec(testSubject.getPos(), jumpFocus);
                PlayerInput newInput = new PlayerInput(1.0F, 0.0F, yaw, 0.0F, false, false, allowSprint);
                testSubject.applyInput(newInput);

                if (testSubject.horizontalCollision && doObstacleAvoidance(testSubject, prevTestSubject, newInput, jumpFocus, allowSprint) == 0.0) {
                    // We don't move at all, so something must be wrong
                    return Integer.MAX_VALUE;
                }
                // TODO more advanced air travel
            }
            applyInputs(testSubject, node);
        } else {
            cost = Integer.MAX_VALUE;
        }

        return cost;
    }

    private void applyInputs(MovementDummy testSubject, Node node) {
        node.distTravel = 0.0;
        Vec3d prevPos = node.player.getPos();
        int diff = testSubject.getInputs().size() - node.player.getInputs().size();
        for (int i = testSubject.getInputs().size() - diff; i < testSubject.getInputs().size(); i++) {
            node.player.applyInput(testSubject.getInputs().get(i));
            node.distTravel += Math.sqrt(prevPos.squaredDistanceTo(node.player.getPos()));
            prevPos = node.player.getPos();
        }
    }
}