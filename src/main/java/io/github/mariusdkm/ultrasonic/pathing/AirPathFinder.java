package io.github.mariusdkm.ultrasonic.pathing;

import io.github.mariusdkm.ultrasonic.api.Pathing;
import io.github.mariusdkm.ultrasonic.utils.MathUtils;
import io.github.mariusdkm.ultrasonic.utils.MovementUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import xyz.wagyourtail.jsmacros.client.access.IChatHud;
import xyz.wagyourtail.jsmacros.client.api.classes.PlayerInput;
import xyz.wagyourtail.jsmacros.client.movement.MovementDummy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static io.github.mariusdkm.ultrasonic.utils.MovementUtils.*;

public class AirPathFinder extends BasePathFinder {

    private final int[] SPRINT_JUMP_RANGE = {4, 5, 6, 6, 6, 6, 6, 7, 7, 7, 8};
    private final BlockPos[] jumpingSphere;

    public AirPathFinder(BlockPos start, BlockPos goal, boolean allowSprint) {
        super(start, goal, allowSprint);
        jumpingSphere = createJumpingSphere();
    }

    private BlockPos[] createJumpingSphere() {
        ArrayList<BlockPos> blocks = new ArrayList<>();
        int maxDepthY = -5;
        for (int y = 2; y >= maxDepthY; y--) {
            double nextXn = 0;
            int sprintRadius = SPRINT_JUMP_RANGE[-y + 2];
            // int walkRadius = sprintRadius / 2;
            forX:
            for (int x = 0; x <= sprintRadius; x++) {
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
                    blocks.add(new BlockPos(x, y, z));
                }
            }
        }
        return blocks.toArray(new BlockPos[0]);
    }

    @Override
    public Queue<CompletableFuture<Node>> calcNode(Node currentNode, Set<Node> closedSet) {
        Queue<CompletableFuture<Node>> queue = new ArrayDeque<>();

        for (BlockPos block : jumpingSphere) {
            queue.add(CompletableFuture.supplyAsync(() -> calcBlock(block, currentNode, closedSet)));
            queue.add(CompletableFuture.supplyAsync(() -> calcBlock(block.rotate(BlockRotation.CLOCKWISE_180), currentNode, closedSet)));

            if (block.getX() != 0 && block.getZ() != 0) {
                // We don't want the points at the axis doubled
                queue.add(CompletableFuture.supplyAsync(() -> calcBlock(block.rotate(BlockRotation.CLOCKWISE_90), currentNode, closedSet)));
                queue.add(CompletableFuture.supplyAsync(() -> calcBlock(block.rotate(BlockRotation.COUNTERCLOCKWISE_90), currentNode, closedSet)));
            }
        }
        return queue;
    }

    private Node calcBlock(BlockPos block, Node currentNode, Set<Node> closedSet) {
        Node newNode = new Node(
                block.add(currentNode.pos),
                currentNode.score,
                currentNode.distTravel,
                currentNode.player);
        if (closedSet.contains(newNode)) {
            return null;
        }
        newNode.score = calcScore(newNode, currentNode, this.allowSprint);
        newNode.prevNode = currentNode;
        return newNode;
    }

    private int calcScore(Node newNode, Node currentNode, boolean sprint) {
        // h = how many tick is it going take to finish
        // h = distanceToGoal / avgSpeed; avgSpeed = distanceFromStart / timeSpend
        // h = distanceToGoal * timeSpend / distanceFromStart
        // avgWalkingSpeed = 0.21585
        // int heuristic = (int) (newNode.pos.getSquaredDistance(goal) / 0.2);

        int movementCost;
        try {
            movementCost = findMove(newNode, currentNode, sprint);
        } catch (Exception e) {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.execute(() -> ((IChatHud) mc.inGameHud.getChatHud()).jsmacros_addMessageBypass(Text.of(e.getMessage()).copy().setStyle(Style.EMPTY.withColor(Formatting.DARK_RED))));
            return Integer.MAX_VALUE;
        }

        if (movementCost == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        int heuristic = calcHeuristic(newNode);

        return newNode.score + movementCost + heuristic;
    }

    private int calcHeuristic(Node newNode) {
        int heuristic;
        double avgSpeed = newNode.distTravel / newNode.player.getInputs().size();
        if (avgSpeed > 0.05) {
            heuristic = (int) (newNode.pos.getSquaredDistance(goal) / avgSpeed);
        } else {
            heuristic = (int) (newNode.pos.getSquaredDistance(goal) / 0.1);
        }
        return heuristic;
    }

    private int findMove(Node newNode, Node currentNode, boolean sprint) {
        if (!newNode.isWalkable()) {
            return Integer.MAX_VALUE;
        }

        Box startArea = createArea(currentNode.player.world, currentNode.pos);
        Box goalArea = createArea(newNode.player.world, newNode.pos);
        double heightDiff = goalArea.getMin(Direction.Axis.Y) - startArea.getMin(Direction.Axis.Y);

        // -2.0695 is the maximum height a player can jump down, while -3.3462 is the max walking down
        if (!Pathing.immuneToDamage && heightDiff < -2.0695) {
            return Integer.MAX_VALUE;
        }

        double reach = MovementUtils.getSprintJumpReach(heightDiff);
        // Create a point, which is the maximum distance away a player could reach
        Vec3d optimalJumpReach = createFocus(newNode.player.world, newNode.pos.subtract(currentNode.pos), currentNode.pos,
                0.8 + reach,
                1.13 + reach,
                1).add(currentNode.pos.getX() + 0.5, goalArea.getMax(Direction.Axis.Y), currentNode.pos.getZ() + 0.5);
        double jumpHeight = 1.2522;
        if (goalArea.getMin(Direction.Axis.Y) - startArea.getMin(Direction.Axis.Y) <= jumpHeight
                && goalArea.intersects(new Vec3d(currentNode.pos.getX() + 0.5, goalArea.getMin(Direction.Axis.Y), currentNode.pos.getZ() + 0.5), optimalJumpReach)) {
            return findJumpMove(newNode, currentNode.pos, startArea, goalArea, sprint);
        }
        return  Integer.MAX_VALUE;
    }

    private int findJumpMove(Node node, BlockPos currentPos, Box startArea, Box goalArea, boolean sprint) {
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
            // Max size of the block
            double max = Math.max(startArea.getXLength(), startArea.getZLength());
            // Diagonal length of the block with half of the player hitbox
            double diagonalLength = Math.sqrt(2 * Math.pow(0.3 + max / 2, 2));
            runFocus = new Vec3d(currentPos.getX() + 0.5, startArea.getMin(Direction.Axis.Y), currentPos.getZ() + 0.5)
                    .add(createFocus(node.player.world, blockToNode, currentPos,
                            0.3 + max,
                            diagonalLength,
                            diagonalLength));

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

        return tryMove(node, currentPos, startArea, goalArea, sprint, runFocus, jumpFocus);
    }

    private int tryMove(Node node, BlockPos currentPos, Box startArea, Box goalArea, boolean sprint, Vec3d runFocus, Vec3d jumpFocus) {
        double heightDiff = goalArea.getMin(Direction.Axis.Y) - startArea.getMin(Direction.Axis.Y);
        int cost = 0;
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

            if (testSubject.horizontalCollision && Math.abs(doObstacleAvoidance(testSubject, prevTestSubject, newInput, jumpFocus, sprint)) <= 2 * Double.MIN_VALUE) {
                // We don't move at all, so something must be wrong
                return Integer.MAX_VALUE;
            }

            prevTestSubject = testSubject.clone();
            boolean successfulJump = MovementUtils.simulateJump(testSubject, jumpFocus, goalArea, sprint, MovementUtils.ticksToLand(heightDiff));
            if (successfulJump) {
                break;
            } else if (!startArea.intersects(prevTestSubject.getBoundingBox())) {
                // TestSubject is outside the startArea, no need to continue
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
