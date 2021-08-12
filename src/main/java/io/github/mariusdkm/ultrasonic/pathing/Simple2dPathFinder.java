package io.github.mariusdkm.ultrasonic.pathing;

import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import xyz.wagyourtail.jsmacros.client.api.classes.PlayerInput;
import xyz.wagyourtail.jsmacros.client.movement.MovementDummy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class Simple2dPathFinder extends BasePathFinder {

    public Simple2dPathFinder(BlockPos start, BlockPos goal, boolean allowSprint) {
        super(start, goal, allowSprint);
    }

    @Override
    public List<CompletableFuture<Node>> calcNode(Node currentNode, int currentScore, Set<Node> closedSet) {
        List<CompletableFuture<Node>> queue = new ArrayList<>();
//        for (int x = currentNode.pos.getX() - 1; x <= currentNode.pos.getX() + 1; x += 1) {
//            for (int z = currentNode.pos.getZ() - 1; z <= currentNode.pos.getZ() + 1; z += 1) {
//                if (currentNode.pos.getX() == x && currentNode.pos.getZ() == z) {
//                    continue;
//                }
//                Node newNode = new Node(new BlockPos(x, currentNode.pos.getY(), z), 0, currentNode.distTravel, currentNode.player);
//                if (!closedSet.contains(newNode) && newNode.isWalkable()) {
//                    newNode.score = calcScore(newNode, currentScore);
//                    newNode.prevNode = currentNode;
////                        LOGGER.info("Node x={}, y={}, z={}, score={}", newNode.pos.getX(), newNode.pos.getY(), newNode.pos.getZ(), newNode.score);
//                    queue.add(newNode);
//                }
//            }
//        }
        return queue;
    }

    private int findBestPath(Node node) {
        int cost;
        boolean directPath = node.player.world.raycast(new RaycastContext(node.player.getCameraPosVec(1.0F).subtract(0, 1, 0),
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
                float yaw = (float) (MovementHelper.calcAngleDegXZ(vecToBlock));
                PlayerInput newInput = new PlayerInput(1.0F, 0.0F, yaw, 0.0F, false, false, allowSprint);
                MovementDummy testSubject = node.player.clone();
                testSubject.applyInput(newInput);
                if (testSubject.horizontalCollision) {
                    // The player collided with a wall, that means on of his velocity vectors is set to 0
                    // --> we loose momentum/speed
                    double diff = testSubject.getPos().squaredDistanceTo(node.player.getPos());
                    // That's why we move in the direction the wall pushes us, which is usually parallel to the wall
                    yaw = (float) (MovementHelper.calcAngleDegXZ(testSubject.getVelocity()));
                    testSubject = node.player.clone();
                    newInput.yaw = yaw;
                    testSubject.applyInput(newInput);
                    // But do we actually travel further with the new yaw?
                    if (diff > testSubject.getPos().squaredDistanceTo(node.player.getPos())) {
                        node.distTravel += diff;
                        yaw = (float) (MovementHelper.calcAngleDegXZ(vecToBlock));
                    } else if (diff == 0.0 && testSubject.getPos().squaredDistanceTo(node.player.getPos()) == 0.0) {
                        return Integer.MAX_VALUE;
                    } else {
                        node.distTravel += testSubject.getPos().squaredDistanceTo(node.player.getPos());
                    }
                } else {
                    node.distTravel = testSubject.getPos().squaredDistanceTo(node.player.getPos());
                }
                // TODO is ot faster to use the testSubject, or to recalc the move?
                newInput.yaw = yaw;
                node.player.applyInput(newInput);

            }
        } else {
            cost = Integer.MAX_VALUE;
        }
        // double maxDistance = node.pos.getSquaredDistance(new Vec3i(node.player.getX(), node.player.getY(), node.p layer.getZ()), true);
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
}
