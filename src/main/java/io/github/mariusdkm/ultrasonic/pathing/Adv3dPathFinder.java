package io.github.mariusdkm.ultrasonic.pathing;

import io.github.mariusdkm.ultrasonic.api.Pathing;
import io.github.mariusdkm.ultrasonic.utils.MathUtils;
import io.github.mariusdkm.ultrasonic.utils.MovementUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import xyz.wagyourtail.jsmacros.client.access.IChatHud;
import xyz.wagyourtail.jsmacros.client.api.classes.PlayerInput;
import xyz.wagyourtail.jsmacros.client.movement.MovementDummy;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static io.github.mariusdkm.ultrasonic.utils.MovementUtils.*;

public class Adv3dPathFinder extends BasePathFinder {
    private final int[] sprintJumpDist = {4, 5, 6, 6, 6, 6, 6, 7, 7, 7, 8};

    public Adv3dPathFinder(BlockPos start, BlockPos goal, boolean allowSprint) {
        super(start, goal, allowSprint);
    }

    @Override
    public Queue<CompletableFuture<Node>> calcNode(Node currentNode, int currentScore, Set<Node> closedSet) {
        Queue<CompletableFuture<Node>> queue = new ArrayDeque<>();

        int maxDepthY = -5;
        for (int y = 2; y >= maxDepthY; y--) {
            double nextXn = 0;
            int sprintRadius = sprintJumpDist[-y + 2];
            // int walkRadius = sprintRadius / 2;
            forX: for (int x = 0; x <= sprintRadius; x++) {
                double xn = nextXn;
                nextXn = (x + 1.0) / sprintRadius;
                double nextZn = 0;
                for (int z = 0; z <= sprintRadius; z++) {
                    if (x == 0 && z == 0) {
                        continue;
                    }
                    double zn = nextZn;
                    nextZn = (z + 1.0) / sprintRadius;
                    if ((xn * xn) + (zn * zn) > 1) {
                        if (z == 0) {
                            break forX;
                        }
                        break;
                    }

                    final int finalX = x;
                    final int finalZ = z;
                    final int finalY = y;
                    queue.add(CompletableFuture.supplyAsync(() -> calcBlock(new Node(
                            new BlockPos(finalX + currentNode.pos.getX(), finalY + currentNode.pos.getY(),finalZ + currentNode.pos.getZ()),
                            currentScore, currentNode.distTravel, currentNode.player), currentNode, closedSet, true)));
                    queue.add(CompletableFuture.supplyAsync(() -> calcBlock(new Node(
                            new BlockPos(-finalX + currentNode.pos.getX(), finalY + currentNode.pos.getY(),-finalZ + currentNode.pos.getZ()),
                            currentScore, currentNode.distTravel, currentNode.player), currentNode, closedSet, true)));

                    if (x != 0 && z != 0) {
                        // We don't want the points at the axis doubled
                        queue.add(CompletableFuture.supplyAsync(() -> calcBlock(new Node(
                                new BlockPos(finalX + currentNode.pos.getX(), finalY + currentNode.pos.getY(),-finalZ + currentNode.pos.getZ()),
                                currentScore, currentNode.distTravel, currentNode.player), currentNode, closedSet, true)));
                        queue.add(CompletableFuture.supplyAsync(() -> calcBlock(new Node(
                                new BlockPos(-finalX + currentNode.pos.getX(), finalY + currentNode.pos.getY(),finalZ + currentNode.pos.getZ()),
                                currentScore, currentNode.distTravel, currentNode.player), currentNode, closedSet, true)));
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
            double heightDiff = goalArea.getMin(Direction.Axis.Y) - startArea.getMin(Direction.Axis.Y);
            // -2.0695 is the maximum height a player can jump down, while -3.3462 is the max walking down
            if (Pathing.immuneToDamage || heightDiff > -2.0695) {
                double reach = MovementUtils.findSprintJumpReach(heightDiff);
                Vec3d optimalJumpReach = createFocus(newNode.player.world, newNode.pos.subtract(currentNode.pos), currentNode.pos,
                        0.8 + reach,
                        1.13 + reach,
                        1).add(currentNode.pos.getX() + 0.5, goalArea.getMax(Direction.Axis.Y), currentNode.pos.getZ() + 0.5);
                double jumpHeight = 1.2522;
                if (goalArea.getMin(Direction.Axis.Y) - startArea.getMin(Direction.Axis.Y) <= jumpHeight
                        && goalArea.intersects(new Vec3d(currentNode.pos.getX() + 0.5, goalArea.getMin(Direction.Axis.Y), currentNode.pos.getZ() + 0.5), optimalJumpReach)) {
                    newNode.score = calcScore(newNode, currentNode.pos, startArea, goalArea, sprint);
                    newNode.prevNode = currentNode;
                    return newNode;
                }
            }
            return null;
        }
        return null;
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

    private int findMovements(Node node, BlockPos currentPos, Box startArea, Box goalArea, boolean sprint) {
        int cost;
        boolean directPath = isDirectPath(node.player, currentPos, node.pos);
        BlockPos blockToNode = node.pos.subtract(currentPos);
        double heightDiff = goalArea.getMin(Direction.Axis.Y) - startArea.getMin(Direction.Axis.Y);

        // The point that is run/walked towards before jumping
        Vec3d runFocus;
        // The point that is run/walked towards while jumping
        Vec3d jumpFocus;
        if (blockToNode.getX() == 0 || blockToNode.getZ() == 0) {
            // The player should just run straight to the goal
            runFocus = new Vec3d(node.pos.getX() + 0.5, startArea.getMin(Direction.Axis.Y), node.pos.getZ() + 0.5);
            jumpFocus = new Vec3d(node.pos.getX() + 0.5, goalArea.getMin(Direction.Axis.Y), node.pos.getZ() + 0.5);
        } else {
            // We use the pos of the last node here, because the player could still be on the corner of another block
            // The length is outside the block, so that the player can't reach it, without exiting the startArea
            double max = Math.max(startArea.getXLength(), startArea.getZLength());
            // Root of what?
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

        if (!directPath) {
            return Integer.MAX_VALUE;
        }

        cost = 0;
        MovementDummy testSubject = node.player.clone();
        // Note: when cloning the testSubject many attribute like verticalCollision aren't copied
        MovementDummy prevTestSubject;
        while (!goalArea.intersects(testSubject.getBoundingBox()) || !testSubject.isOnGround()) {
            // Here the player moves towards its jumping position (runFocus),
            // While testing whether the goal could be reached
            if (cost > 200) {
                throw new IllegalStateException("Cost is to high from " + currentPos + " - " + node.pos.toString());
            }
            if (testSubject.getY() < runFocus.getY() && testSubject.getY() < jumpFocus.getY()) {
                // We fell down
                return Integer.MAX_VALUE;
            }
            cost++;
            prevTestSubject = testSubject.clone();

            float yaw = (float) (MathUtils.calcAngleDegXZ(runFocus.subtract(testSubject.getPos())));
            PlayerInput newInput = new PlayerInput(1.0F, 0.0F, yaw, 0.0F, false, false, sprint);
            testSubject.applyInput(newInput);

            if (testSubject.horizontalCollision && Math.abs(doObstacleAvoidance(testSubject, prevTestSubject, newInput, jumpFocus, sprint)) < 2 * Double.MIN_VALUE) {
                // We don't move at all, so something must be wrong
                return Integer.MAX_VALUE;
            }

            prevTestSubject = testSubject.clone();
            if (MovementUtils.simulateJump(testSubject, jumpFocus, goalArea, sprint, MovementUtils.ticksToLand(heightDiff))) {
                break;
            } else if (!startArea.intersects(prevTestSubject.getBoundingBox())) {
                // We only want to check jumps from the startArea
                return Integer.MAX_VALUE;
            } else {
                testSubject = prevTestSubject.clone();
            }
        }
        return applyInputs(testSubject, node);
    }

    private int applyInputs(MovementDummy testSubject, Node node) {
        Vec3d prevPos = node.player.getPos();
        int amountOfNewInputs = testSubject.getInputs().size() - node.player.getInputs().size();
        for (int i = testSubject.getInputs().size() - amountOfNewInputs; i < testSubject.getInputs().size(); i++) {
            node.player.applyInput(testSubject.getInputs().get(i));
            node.distTravel += Math.sqrt(prevPos.squaredDistanceTo(node.player.getPos()));
            prevPos = node.player.getPos();
        }
        return amountOfNewInputs;
    }
}
