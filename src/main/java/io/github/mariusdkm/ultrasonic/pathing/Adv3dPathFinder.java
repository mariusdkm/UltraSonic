package io.github.mariusdkm.ultrasonic.pathing;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.*;
import xyz.wagyourtail.jsmacros.client.access.IChatHud;
import xyz.wagyourtail.jsmacros.client.api.classes.PlayerInput;
import xyz.wagyourtail.jsmacros.client.movement.MovementDummy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static io.github.mariusdkm.ultrasonic.pathing.MovementHelper.*;

public class Adv3dPathFinder extends BasePathFinder {
    private final int[] sprintJumpDist = {4, 5, 6, 6, 6, 6, 6, 7, 7, 7, 8};
    private final double[][] sprintJump = {
            {1.2522, 3.4548},
            {1.1767, 3.7399},
            {1.0244, 4.0248},
            {0.7967, 4.3095},
            {0.4952, 4.5941},
            {0.1212, 4.8786},
            {-0.3235, 5.1629},
            {-0.8378, 5.4472},
            {-1.4203, 5.7313},
            {-2.0694, 6.0154},
            {-2.7841, 6.2993},
            {-3.5628, 6.5832},
            {-4.4043, 6.8670},
            {-5.3074, 7.1508},
            {-6.2709, 7.4345},
            {-7.2935, 7.7181},
            {-8.3740, 8.0017},
            {-9.5113, 8.2852},
            {-10.7043, 8.5687},
            {-11.9518, 8.8522},
            {-13.2528, 9.1357},
            {-14.6061, 9.4191},
            {-16.0108, 9.7025},
            {-17.4658, 9.9858},
            {-18.9701, 10.269},
            {-20.5226, 10.552}};

    public Adv3dPathFinder(BlockPos start, BlockPos goal, boolean allowSprint) {
        super(start, goal, allowSprint);
    }

    @Override
    public List<CompletableFuture<Node>> calcNode(Node currentNode, int currentScore, Set<Node> closedSet) {
        List<CompletableFuture<Node>> queue = new ArrayList<>();

        int maxDepthY = -4;
        for (int y = 2; y >= maxDepthY; y--) {
            double nextXn = 0;
            int sprintRadius = sprintJumpDist[-y + 2];
            // int walkRadius = sprintRadius / 2;
            forX:
            for (int x = 0; x <= sprintRadius; x++) {
                final double xn = nextXn;
                nextXn = (x + 1.0D) / sprintRadius;
                double nextZn = 0;
                for (int z = 0; z <= sprintRadius; z++) {
                    final double zn = nextZn;
                    nextZn = (z + 1.0D) / sprintRadius;
                    if (x == 0 && z == 0) {
                        continue;
                    }
                    if ((xn * xn) + (zn * zn) > 1) {
                        if (z == 0) {
                            break forX;
                        }
                        break;
                    }

                    int finalX = x;
                    int finalZ = z;
                    int finalY = y;
                    queue.add(CompletableFuture.supplyAsync(() -> calcBlock(new Node(new BlockPos(finalX + currentNode.pos.getX(), finalY + currentNode.pos.getY(), finalZ + currentNode.pos.getZ()), currentScore, currentNode.distTravel, currentNode.player), currentNode, closedSet, true)));
                    queue.add(CompletableFuture.supplyAsync(() -> calcBlock(new Node(new BlockPos(-finalX + currentNode.pos.getX(), finalY + currentNode.pos.getY(), -finalZ + currentNode.pos.getZ()), currentScore, currentNode.distTravel, currentNode.player), currentNode, closedSet, true)));

                    if (x != 0 && z != 0) {
                        // We don't want the points at the axis doubled
                        queue.add(CompletableFuture.supplyAsync(() -> calcBlock(new Node(new BlockPos(finalX + currentNode.pos.getX(), finalY + currentNode.pos.getY(), -finalZ + currentNode.pos.getZ()), currentScore, currentNode.distTravel, currentNode.player), currentNode, closedSet, true)));
                        queue.add(CompletableFuture.supplyAsync(() -> calcBlock(new Node(new BlockPos(-finalX + currentNode.pos.getX(), finalY + currentNode.pos.getY(), finalZ + currentNode.pos.getZ()), currentScore, currentNode.distTravel, currentNode.player), currentNode, closedSet, true)));
                    }

//                    if ((x * x) + (z * z) < walkRadius) {
//                        queue.add(CompletableFuture.supplyAsync(() -> calcBlock(new Node(new BlockPos(finalX + currentNode.pos.getX(), finalY + currentNode.pos.getY(), finalZ + currentNode.pos.getZ()), currentScore, currentNode.distTravel, currentNode.player), currentNode, closedSet, false)));
//                        queue.add(CompletableFuture.supplyAsync(() -> calcBlock(new Node(new BlockPos(-finalX + currentNode.pos.getX(), finalY + currentNode.pos.getY(), -finalZ + currentNode.pos.getZ()), currentScore, currentNode.distTravel, currentNode.player), currentNode, closedSet, false)));
//
//                        if (x != 0 && z != 0) {
//                            // We don't want the points at the axis doubled
//                            queue.add(CompletableFuture.supplyAsync(() -> calcBlock(new Node(new BlockPos(finalX + currentNode.pos.getX(), finalY + currentNode.pos.getY(), -finalZ + currentNode.pos.getZ()), currentScore, currentNode.distTravel, currentNode.player), currentNode, closedSet, false)));
//                            queue.add(CompletableFuture.supplyAsync(() -> calcBlock(new Node(new BlockPos(-finalX + currentNode.pos.getX(), finalY + currentNode.pos.getY(), finalZ + currentNode.pos.getZ()), currentScore, currentNode.distTravel, currentNode.player), currentNode, closedSet, false)));
//                        }
//                    }
                }
            }
        }
        return queue;
    }

    private Node calcBlock(Node newNode, Node currentNode, Set<Node> closedSet, boolean sprint) {
        if (!closedSet.contains(newNode) && newNode.isWalkable()) {
            Box startArea = createArea(currentNode.player.world, currentNode.pos);
            Box goalArea = createArea(newNode.player.world, newNode.pos);
            double reach = findClosestReach(goalArea.getMin(Direction.Axis.Y) - startArea.getMin(Direction.Axis.Y));
            Vec3d optimalJumpReach = createFocus(newNode.player.world, newNode.pos.subtract(currentNode.pos), currentNode.pos,
                    0.8 + reach,
                    1.13 + reach,
                    1).add(currentNode.pos.getX() + 0.5, goalArea.getMax(Direction.Axis.Y), currentNode.pos.getZ() + 0.5);
            if (goalArea.getMin(Direction.Axis.Y) - startArea.getMin(Direction.Axis.Y) <= 1.2522 && goalArea.intersects(new Vec3d(currentNode.pos.getX() + 0.5, goalArea.getMin(Direction.Axis.Y), currentNode.pos.getZ() + 0.5), optimalJumpReach)) {
                newNode.score = calcScore(newNode, currentNode.pos, startArea, goalArea, sprint);
                newNode.prevNode = currentNode;
                return newNode;
            }
            return null;
        }
        return null;
    }

    private double findClosestReach(double height) {
        return sprintJump[MathHelper.binarySearch(0, sprintJump.length, (i) -> height > sprintJump[i][0])][1];
    }

    private int calcScore(Node newNode, BlockPos currentPos, Box startArea, Box goalArea, boolean sprint) {
        // h = how many tick is it going take to finish
        // h = distanceToGoal / avgSpeed; avgSpeed = distanceFromStart / timeSpend
        // h = distanceToGoal * timeSpend / distanceFromStart
        // avgWalkingSpeed = 0.21585
        // int heuristic = (int) (newNode.pos.getSquaredDistance(goal) / 0.2);

        int movementCost;
        try {
            movementCost = findMovements(newNode, currentPos, startArea, goalArea, sprint);
        } catch (Exception e) {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.execute(() -> ((IChatHud) mc.inGameHud.getChatHud()).jsmacros_addMessageBypass(new LiteralText(e.getMessage()).setStyle(Style.EMPTY.withColor(Formatting.DARK_RED))));
            return Integer.MAX_VALUE;
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

    private int findMovements(Node node, BlockPos currentPos, Box startArea, Box goalArea, boolean sprint) throws Exception {
        int cost;
        boolean directPath = isDirectPath(node.player, currentPos, node.pos);
        BlockPos blockToNode = node.pos.subtract(currentPos);


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
            double max = Math.max(startArea.getXLength(), startArea.getZLength());
            double sqrt = Math.sqrt(2 * Math.pow(0.3 + max / 2, 2));
            runFocus = new Vec3d(currentPos.getX() + 0.5, startArea.getMin(Direction.Axis.Y), currentPos.getZ() + 0.5)
                    .add(createFocus(node.player.world, blockToNode, currentPos,
                            0.3 + max,
                            sqrt,
                            sqrt));

            // The cornered length is outside the block, so that the player can
            jumpFocus = new Vec3d(node.pos.getX() + 0.5, goalArea.getMin(Direction.Axis.Y), node.pos.getZ() + 0.5)
                    .add(createFocus(node.player.world, blockToNode.multiply(-1), node.pos,
                            0.2 + Math.min(goalArea.getXLength(), goalArea.getZLength()),
                            Math.sqrt(2 * Math.pow(0.2 + Math.min(goalArea.getXLength(), goalArea.getZLength()) / 2, 2)),
                            0));
        }

        if (directPath) {
            cost = 0;
            MovementDummy testSubject = node.player.clone();
            MovementDummy prevTestSubject;
            while (!goalArea.intersects(testSubject.getBoundingBox()) || !testSubject.isOnGround()) {
                // Here the player moves towards its jumping position (runFocus),
                // While testing whether the goal could be reached
                if (cost > 200) {
                    throw new Exception("Cost is to high from " + currentPos + " - " + node.pos.toString());
                }
                if (testSubject.getY() < runFocus.getY() && testSubject.getY() < jumpFocus.getY()) {
                    // We fell down
                    return Integer.MAX_VALUE;
                }
                cost += 1;
                prevTestSubject = testSubject.clone();

                float yaw = (float) (MovementHelper.calcAngleDegXZ(runFocus.subtract(testSubject.getPos())));
                PlayerInput newInput = new PlayerInput(1.0F, 0.0F, yaw, 0.0F, false, false, sprint);
                testSubject.applyInput(newInput);

                if (testSubject.horizontalCollision && doObstacleAvoidance(testSubject, prevTestSubject, newInput, jumpFocus, sprint) == 0.0) {
                    // We don't move at all, so something must be wrong
                    return Integer.MAX_VALUE;
                }

                if (MovementHelper.simulateJump(testSubject.clone(), jumpFocus, goalArea, sprint, MovementHelper.ticksToLand(blockToNode.getY()))) {
                    yaw = (float) MovementHelper.angleToVec(testSubject.getPos(), jumpFocus);
                    newInput = new PlayerInput(1.0F, 0.0F, yaw, 0.0F, true, false, sprint);
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

                float yaw = (float) MovementHelper.angleToVec(testSubject.getPos(), jumpFocus);
                PlayerInput newInput = new PlayerInput(1.0F, 0.0F, yaw, 0.0F, false, false, sprint);
                testSubject.applyInput(newInput);

                if (testSubject.horizontalCollision && doObstacleAvoidance(testSubject, prevTestSubject, newInput, jumpFocus, sprint) == 0.0) {
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