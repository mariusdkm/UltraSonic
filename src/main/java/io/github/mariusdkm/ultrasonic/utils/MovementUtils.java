package io.github.mariusdkm.ultrasonic.utils;

import io.github.mariusdkm.ultrasonic.api.Pathing;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import xyz.wagyourtail.jsmacros.client.api.classes.Draw3D;
import xyz.wagyourtail.jsmacros.client.api.classes.PlayerInput;
import xyz.wagyourtail.jsmacros.client.api.library.impl.FHud;
import xyz.wagyourtail.jsmacros.client.movement.MovementDummy;

public class MovementUtils {
    private static final double[][] sprintJump = {
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

    /**
     * Calculates the Y-Momentum, after t-Ticks of jumping.
     *
     * @param t number of ticks after jumping
     * @return Y-Momentum, after t-Ticks
     */
    public static double calcJumpMomentum(int t) {
        return -0.0784 * (1 - Math.pow(0.98, t)) / (1 - 0.98) + Math.pow(0.98, t) * 0.42;
    }

    /**
     * Returns the maximum distance a player could the player could theoretically jump
     *
     * @param height how high/low to jump, relative to the start
     * @return number of ticks in the air
     */
    public static double findSprintJumpReach(double height) {
        return sprintJump[MathHelper.binarySearch(0, sprintJump.length, (i) -> height > sprintJump[i][0])][1];
    }

    /**
     * Calculate the number of ticks in the air, when jumping.
     *
     * @param height how high/low to jump, relative to the start
     * @return number of ticks in the air
     */
    public static int ticksToLand(double height) {
        return MathHelper.binarySearch(0, sprintJump.length, (i) -> height > sprintJump[i][0]) + 6;
    }

    /**
     * Gets the inertia/slipperiness of the block under the player
     *
     * @param player The player to use
     * @return inertia of the specified block
     */
    public static double getGroundInertia(MovementDummy player) {
        return getGroundInertia(player.world, player.getPos());
    }

    /**
     * Gets the inertia/slipperiness of the block under the player
     *
     * @param world The world
     * @param pos   the position to get the inertia from
     * @return inertia of the specified block
     */
    public static double getGroundInertia(World world, Vec3d pos) {
        // or BlockStateInterface.getBlock(ctx, dest).slipperiness?
        // BlockPos blockPos = this.getVelocityAffectingPos();
        // float t = player.world.getBlockState(blockPos).getBlock().getSlipperiness();
        return world.getBlockState(new BlockPos(MathHelper.fastFloor(pos.x), MathHelper.fastFloor(pos.y) - 1, MathHelper.fastFloor(pos.z))).getBlock().getSlipperiness() * 0.91;
    }

    /**
     * Calculates how far the player would travel, when jumping in that tick.<br>
     * Only walking/sprinting forward is supported, not strafe.<br>
     *
     * <p>
     * Watch out, when landing, the last tick is still air movement, but you already collide with the block in front of you.<br>
     * So you should always calculate one tick less for the reach.
     * </p>
     *
     * <a href="https://www.mcpk.wiki/wiki/Movement_Physics"> Great information, about the movement physics</a><br>
     *
     * <b>dir must be capitalized</b><br>
     * <p>
     *
     * @param player The player, which is going to move
     * @param ticks  Number of ticks the player moves
     * @param axis   X or Z, in which direction to calculate
     * @param sprint Whether to sprint or not
     * @return How far relative the player is going to move.
     * @see net.minecraft.client.network.ClientPlayerEntity#travel(Vec3d)
     */
    public static double jumpReach(MovementDummy player, int ticks, Direction.Axis axis, boolean sprint) {
        double reach = 0.0;
        double yaw = player.getYaw(); //Should this become its own variable?

        // We first have to add the one tick of ground movement.
        double motion = groundTick(player, sprint, true).getComponentAlongAxis(axis);
        reach += motion;
        // We still have ground, bc our Positions hasn't changed yet
        motion *= getGroundInertia(player);

        double trig = (axis.getName().equals("z")) ? MathUtils.f_cos(yaw * 0.017453292) : -MathUtils.f_sin(yaw * 0.017453292); // 0.017453292F = Math.PI / 180.0F

        trig *= ((sprint) ? 0.02548 : 0.0196); // 0.02548 =  0.98 * (0.02 + 0.02 * 0.3); 0.0196 = 0.98 * 0.02

        //We start counting at the second tick, since we already calculated the 1.
        for (int tick = 1; tick < ticks; tick++) {
            motion += trig;
            reach += motion;
            motion *= 0.91; //drag
        }
        return reach;
    }

    /**
     * Calculates the momentum of the player after one tick on the ground.<br>
     * Only walking/sprinting forward is supported, not strafe.<br>
     * If jumping and sprinting the player receives a boost.<br>
     *
     * <b>dir must be capitalized</b><br>
     * <p>
     *
     * @param motion  The motion of the player, in which to be calculated(either X or Z)
     * @param inertia The inertia of the block the player is currently standing on
     * @param yaw     The Yaw of the player
     * @param axis    X or Z, in which direction to calculate
     * @param sprint  Whether to sprint or not
     * @param jump    Whether to jump int that tick or not. This gives the "jumpboost"
     * @return Motion after a Tick on the ground
     * @see MovementUtils#jumpReach(MovementDummy, int, Direction.Axis, boolean)
     * @see net.minecraft.entity.player.PlayerEntity#travel(Vec3d)
     */
    public static double fwdGroundTick(double motion, double inertia, double yaw, Direction.Axis axis, boolean sprint, boolean jump) {
        double movementFactor = (0.16277136 / inertia / inertia / inertia);
        double trig = (axis.getName().equals("z")) ? MathUtils.f_cos(yaw * 0.017453292) : -MathUtils.f_sin(yaw * 0.017453292);
        motion += movementFactor * trig * (sprint ? 0.12739 : 0.098) + ((sprint && jump) ? trig * 0.2 : 0); // 0.1274 =  0.98 * 0.13; 0.098 = 0.98 * 0.1
        return motion;
    }
//    public static double fwdGroundTick(double motion, float inertia, float yaw, char dir, boolean sprint, boolean jump) {
//        float momentum = motion * inertia * 0.91 + (sprint ? 0.12739 : 0.098) ;
//        float trig = (dir == 'Z') ? MathHelper.cos(yaw * 0.017453292F) : -MathHelper.sin(yaw * 0.017453292F);
//        motion += movementFactor * trig * (sprint ? 0.12739 : 0.098) + ((sprint && jump) ? trig * 0.2 : 0); // 0.1274 =  0.98 * 0.13; 0.098 = 0.98 * 0.1
//        return motion;
//    }

    public static Box jumpDistance(MovementDummy player, int ticks, double yaw, boolean sprinting) {
        MovementDummy testSubject = player.clone();
        testSubject.applyInput(new PlayerInput(1, 0, yaw, 0, true, false, sprinting));
        for (int i = 0; i < ticks - 2; i++) {
            testSubject.applyInput(new PlayerInput(1, 0, yaw, 0, false, false, sprinting));
        }
        return testSubject.getBoundingBox();
    }


    /**
     * Calculates the momentum of the player after one tick on the ground.<br>
     * Only walking/sprinting forward is supported, not strafe.<br>
     * If jumping and sprinting the player receives a boost.<br>
     *
     * <b>dir must be capitalized</b><br>
     * <p>
     *
     * @param player The player, which is going to move
     * @param sprint Whether to sprint or not
     * @param jump   Whether to jump int that tick or not. This gives the "jumpboost"
     * @return Motion after a Tick on the ground
     * @see MovementUtils#jumpReach(MovementDummy, int, Direction.Axis, boolean)
     * @see net.minecraft.entity.player.PlayerEntity#travel(Vec3d)
     */
    public static Vec3d groundTick(MovementDummy player, boolean sprint, boolean jump) {
        Vec3d newPos = new Vec3d(0.0, 0.0, 0.0);
        newPos.add(fwdGroundTick(player.getVelocity().x, getGroundInertia(player), player.getYaw(), Direction.Axis.X, sprint, jump), 0.0, 0.0);
        newPos.add(0.0, 0.0, fwdGroundTick(player.getVelocity().z, getGroundInertia(player), player.getYaw(), Direction.Axis.Z, sprint, jump));
        return newPos;
    }

    /**
     * This function checks whether it's theoretically possible to get to the endBlock without hitting a block.
     * How this works is it basically it raycasts the hitbox face of the player
     * from every corner of the start bock to the end block.
     * If at least one of these raycasts work it's assumed that the path is theoretically passable by the player
     *
     * @param entity     Entity with world access
     * @param startBlock Block to start raycasting from
     * @param endBlock   Block to raycast to
     * @return Whether it's theoretically possible to get to the endBlock without hitting a block
     */
    public static boolean isDirectPath(LivingEntity entity, BlockPos startBlock, BlockPos endBlock) {
        // 0.6 = entity.getDimensions(EntityPose.STANDING).width
        Vec3d[][] startCorners = getRaycastCorners(entity.world, startBlock, 0.6);
        Vec3d[][] endCorners = getRaycastCorners(entity.world, endBlock, 0.6);
        // When the block is so small, that the only rays are from its corners
        boolean smallBlock = startCorners.length == 6 || endCorners.length == 6;
        for (int i = 0; i < 4; i++) {
            if (raycastHitboxFace(entity, startCorners[i], endCorners[i], false)) {
                return true;
            }
            if (raycastHitboxFace(entity, startCorners[i + 4], endCorners[i + 4], false)) {
                return true;
            }
            // Since we also raycast diagonally we don't want the rays to cross each other and instead run "parallel" to each other
            if (MathUtils.angleBetweenVec(startCorners[i][2].subtract(startCorners[i][0]), endCorners[i + 4][0].subtract(startCorners[i][0])) >
                    MathUtils.angleBetweenVec(startCorners[i][2].subtract(startCorners[i][0]), endCorners[i + 4][2].subtract(startCorners[i][0]))) {
                if (raycastHitboxFace(entity, startCorners[i], endCorners[i + 4], false)) {
                    return true;
                }
            } else {
                if (raycastHitboxFace(entity, startCorners[i], endCorners[i + 4], true)) {
                    return true;
                }
            }
            if (MathUtils.angleBetweenVec(startCorners[i + 4][2].subtract(startCorners[i + 4][0]), endCorners[i][0].subtract(startCorners[i + 4][0])) >
                    MathUtils.angleBetweenVec(startCorners[i + 4][2].subtract(startCorners[i + 4][0]), endCorners[i][2].subtract(startCorners[i + 4][0]))) {
                if (raycastHitboxFace(entity, startCorners[i + 4], endCorners[i], false)) {
                    return true;
                }
            } else {
                if (raycastHitboxFace(entity, startCorners[i + 4], endCorners[i], true)) {
                    return true;
                }
            }
            if (smallBlock && i == 1) {
                break;
            }
        }
        return false;
    }

    public static boolean raycastHitboxFace(Entity entity, Vec3d[] start, Vec3d[] end, boolean reverse) {
        double height = 1.8;
        if (Math.abs(MathUtils.angleToVec(start[0], end[0]) - MathUtils.angleToVec(start[0], start[2])) < Double.MIN_VALUE * 2
                || Math.abs(MathUtils.angleToVec(start[0], end[0]) - MathUtils.angleToVec(start[2], start[0])) < Double.MIN_VALUE * 2) {
            // This would mean, that we just check in one line, but we instead want to raycast the hitbox face of the player
            return false;
        }
        // Code for debugging
//        for (int i = 0; i < 3; i++) {
//            Pathing.pathBlocks.addLine(start[i].x, start[i].y, start[i].z, (reverse ? end[2 - i] : end[i]).x, (reverse ? end[2 - i] : end[i]).y, (reverse ? end[2 - i] : end[i]).z, 0xfc0303);
//        }
        for (int i = 0; i < 3; i++) {
            // TODO: Better name
            RaycastContext feetCast = new RaycastContext(start[i], reverse ? end[2 - i] : end[i], RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.ANY, entity);
            if (entity.world.raycast(feetCast).getType() != HitResult.Type.MISS) {
                return false;
            }
            RaycastContext headCast = new RaycastContext(start[i].add(0, height, 0), reverse ? end[2 - i].add(0, height, 0) : end[i].add(0, height, 0),
                    RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.ANY, entity);
            if (entity.world.raycast(headCast).getType() != HitResult.Type.MISS) {
                return false;
            }
        }
        return true;
    }

    public static boolean simulateJump(MovementDummy testSubject, Vec3d goalFocus, Box goal, boolean allowSprint, int ticks) {
        double yaw = MathUtils.angleToVec(testSubject.getPos(), goalFocus);
        testSubject.applyInput(new PlayerInput(1, 0, yaw, 0, true, false, allowSprint));
        MovementDummy prevTestSubject;
        // We are jumping, YEET
        for (int i = 0; i < ticks - 1; i++) {
            prevTestSubject = testSubject.clone();

            yaw = MathUtils.angleToVec(testSubject.getPos(), goalFocus);
            PlayerInput newInput = new PlayerInput(1, 0, yaw, 0, false, false, allowSprint);
            testSubject.applyInput(newInput);

            if (testSubject.horizontalCollision && Math.abs(doObstacleAvoidance(testSubject, prevTestSubject, newInput, goalFocus, allowSprint)) < Double.MIN_VALUE * 2) {
                // We don't move at all, so something must be wrong
                return false;
            } else if (testSubject.verticalCollision && goal.intersects(testSubject.getBoundingBox())) {
                return true;
            }
            // TODO advanced air travel
        }
        return false;
    }

    public static double doObstacleAvoidance(MovementDummy testSubject, MovementDummy prevTestSubject, PlayerInput newInput, Vec3d jumpFocus, boolean allowSprint) {
        if (testSubject.horizontalCollision) {
            // The player collided with a wall, that means on of his velocity vectors is set to 0
            // --> we loose momentum/speed
            double diff = testSubject.getPos().squaredDistanceTo(prevTestSubject.getPos());
            testSubject = prevTestSubject.clone();

            // That's why we move in the direction the wall pushes us, which is usually parallel to the wall
            newInput.yaw = (float) (MathUtils.calcAngleDegXZ(testSubject.getVelocity()));
            testSubject.applyInput(newInput);

            // But do we actually travel further with the new yaw?
            if (diff > testSubject.getPos().squaredDistanceTo(prevTestSubject.getPos())) {
                // Nope, so do the same as before
                testSubject.applyInput(newInput);
                return diff;
            } else {
                // Yes, this is actually better
                return testSubject.getPos().squaredDistanceTo(prevTestSubject.getPos());
            }
        }
        return 0;
    }

    public static Vec3d createFocus(World world, Vec3i blockToNode, BlockPos sourcePos, double straightLen, double diagonalLen, double corneredLen) {
        Vec3d dirVec;
        if (blockToNode.getX() == 0 || blockToNode.getZ() == 0) {
            dirVec = MathUtils.rotatedVec(diagonalLen, MathUtils.roundRad(blockToNode, Math.PI / 2, 0));
        } else {
            dirVec = MathUtils.rotatedVec(diagonalLen, MathUtils.roundRad(blockToNode, Math.PI / 2, Math.PI / 4));
        }
        if (Math.abs(blockToNode.getX()) <= 1 && Math.abs(blockToNode.getZ()) <= 1) {
            // It could be that the block we want to walk onto is directly in front of us
            // in that case we just walk towards the block
            return dirVec;
        }
        int dirX = (int) Math.signum(dirVec.getX());
        int dirZ = (int) Math.signum(dirVec.getZ());
        if (!world.getBlockState(sourcePos.add(dirX, 1, dirZ)).isAir() ||
                !world.getBlockState(sourcePos.add(dirX, 2, dirZ)).isAir()) {
            // Round to 90 Deg, so that we move to one of the sides of the diagonal block
            dirVec = MathUtils.rotatedVec(straightLen, MathUtils.roundRad(blockToNode, Math.PI / 2, 0.0));
        }
        if (!world.getBlockState(sourcePos.add(dirX, 1, 0)).isAir() ||
                !world.getBlockState(sourcePos.add(dirX, 2, 0)).isAir()) {
            dirVec = new Vec3d(0, 0, (0 > dirZ) ? -straightLen : straightLen);
        }
        if (!world.getBlockState(sourcePos.add(0, 1, dirZ)).isAir() ||
                !world.getBlockState(sourcePos.add(0, 2, dirZ)).isAir()) {
            dirVec = new Vec3d((0 > dirX) ? -straightLen : straightLen, 0, 0);
        }
        if (dirVec.getX() == 0 && dirVec.getZ() == 0) {
            // There are blocks on either side of our focus
            dirVec = MathUtils.rotatedVec(corneredLen, MathUtils.roundRad(blockToNode, Math.PI / 2, Math.PI / 4));
        }
        return dirVec;
    }

    public static Box createArea(World world, BlockPos pos) {
        Box goalArea = world.getBlockState(pos).getCollisionShape(world, pos).getBoundingBox();
        return new Box(goalArea.getMin(Direction.Axis.X) + pos.getX(),
                goalArea.getMax(Direction.Axis.Y) + pos.getY(),
                goalArea.getMin(Direction.Axis.Z) + pos.getZ(),
                goalArea.getMax(Direction.Axis.X) + pos.getX(),
                goalArea.getMax(Direction.Axis.Y) + pos.getY() + 0.5,
                goalArea.getMax(Direction.Axis.Z) + pos.getZ());
    }

    private static void debugCode(LivingEntity entity, BlockPos startBlock, BlockPos endBlock) {
        if (FHud.renders.contains(Pathing.pathBlocks)) {
            synchronized (FHud.renders) {
                FHud.renders.remove(Pathing.pathBlocks);
            }
        }
        Pathing.pathBlocks = new Draw3D();
        synchronized (FHud.renders) {
            FHud.renders.add(Pathing.pathBlocks);
        }
        Vec3d[][] startCorners = getRaycastCorners(entity.world, startBlock, 0.6);
        Vec3d[][] endCorners = getRaycastCorners(entity.world, endBlock, 0.6);
        // When the block is so small, that the only rays are from its corners
        boolean smallBlock = startCorners.length == 6 || endCorners.length == 6;
        for (int i = 0; i < 4; i++) {
            raycastHitboxFace(entity, startCorners[i], endCorners[i], false);
            raycastHitboxFace(entity, startCorners[i + 4], endCorners[i + 4], false);
            // Since we also raycast diagonally we don't want the rays to cross each other and instead run "parallel" to each other
            raycastHitboxFace(entity, startCorners[i], endCorners[i + 4], !(MathUtils.angleBetweenVec(startCorners[i][2].subtract(startCorners[i][0]),
                    endCorners[i + 4][0].subtract(startCorners[i][0])) > MathUtils.angleBetweenVec(startCorners[i][2].subtract(startCorners[i][0]),
                    endCorners[i + 4][2].subtract(startCorners[i][0]))));
            raycastHitboxFace(entity, startCorners[i + 4], endCorners[i], !(MathUtils.angleBetweenVec(startCorners[i + 4][2].subtract(startCorners[i + 4][0]),
                    endCorners[i][0].subtract(startCorners[i + 4][0])) > MathUtils.angleBetweenVec(startCorners[i + 4][2].subtract(startCorners[i + 4][0]),
                    endCorners[i][2].subtract(startCorners[i + 4][0]))));
            if (smallBlock && i == 1) {
                break;
            }
        }
    }

    // Warum ist hier die width als einziges ein Parameter und sonst nicht?
    private static Vec3d[][] getRaycastCorners(World world, BlockPos pos, double width) {
        double halfWidth = width / 2;
        // This is kinda special, since we want the hitbox not to reach over the corners
        VoxelShape shape = world.getBlockState(pos).getCollisionShape(world, pos);
        double maxX = shape.getMax(Direction.Axis.X) + pos.getX();
        double maxY = shape.getMax(Direction.Axis.Y) + pos.getY();
        double maxZ = shape.getMax(Direction.Axis.Z) + pos.getZ();
        double minX = shape.getMin(Direction.Axis.X) + pos.getX();
        double minZ = shape.getMin(Direction.Axis.Z) + pos.getZ();
        if (maxX - minX <= width && maxZ - minZ <= width) {
            // The block is so small, we just return the middle
            double centerX = shape.getMin(Direction.Axis.X) + shape.getMax(Direction.Axis.X) / 2 + pos.getX();
            double centerY = shape.getMin(Direction.Axis.Y) + shape.getMax(Direction.Axis.Y) / 2 + pos.getZ();
            return new Vec3d[][]{
                    {new Vec3d(centerX - halfWidth, maxY, centerY - halfWidth), new Vec3d(centerX, maxY, centerY - halfWidth), new Vec3d(centerX + halfWidth, maxY, centerY - halfWidth)},
                    {new Vec3d(centerX - halfWidth, maxY, centerY + halfWidth), new Vec3d(centerX, maxY, centerY + halfWidth), new Vec3d(centerX + halfWidth, maxY, centerY + halfWidth)},
                    {}, {},
                    {new Vec3d(centerX - halfWidth, maxY, centerY - halfWidth), new Vec3d(centerX - halfWidth, maxY, centerY), new Vec3d(centerX - halfWidth, maxY, centerY + halfWidth)},
                    {new Vec3d(centerX + halfWidth, maxY, centerY - halfWidth), new Vec3d(centerX + halfWidth, maxY, centerY), new Vec3d(centerX + halfWidth, maxY, centerY + halfWidth)}
            };
        } else {
            // The order here is very important
            // This is just pain, pls help
            return new Vec3d[][]{
                    {new Vec3d(minX, maxY, minZ), new Vec3d(minX + halfWidth, maxY, minZ), new Vec3d(minX + width, maxY, minZ)},
                    {new Vec3d(maxX - width, maxY, minZ), new Vec3d(maxX - halfWidth, maxY, minZ), new Vec3d(maxX, maxY, minZ)},
                    {new Vec3d(minX, maxY, maxZ), new Vec3d(minX + halfWidth, maxY, maxZ), new Vec3d(minX + width, maxY, maxZ)},
                    {new Vec3d(maxX - width, maxY, maxZ), new Vec3d(maxX - halfWidth, maxY, maxZ), new Vec3d(maxX, maxY, maxZ)},
                    {new Vec3d(minX, maxY, minZ), new Vec3d(minX, maxY, minZ + halfWidth), new Vec3d(minX, maxY, minZ + width)},
                    {new Vec3d(minX, maxY, maxZ - width), new Vec3d(minX, maxY, maxZ - halfWidth), new Vec3d(minX, maxY, maxZ)},
                    {new Vec3d(maxX, maxY, minZ), new Vec3d(maxX, maxY, minZ + halfWidth), new Vec3d(maxX, maxY, minZ + width)},
                    {new Vec3d(maxX, maxY, maxZ - width), new Vec3d(maxX, maxY, maxZ - halfWidth), new Vec3d(maxX, maxY, maxZ)},
            };
        }
    }
}
