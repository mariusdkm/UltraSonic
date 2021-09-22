package io.github.mariusdkm.ultrasonic.pathing;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import xyz.wagyourtail.jsmacros.client.api.classes.PlayerInput;
import xyz.wagyourtail.jsmacros.client.movement.MovementDummy;

public class MovementHelper {
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
    static double calcJumpMomentum(int t) {
        return -0.0784 * (1 - Math.pow(0.98, t)) / (1 - 0.98) + Math.pow(0.98, t) * 0.42;
    }

    /**
     * Returns the maximum distance a player could the player could theoretically jump
     *
     * @param height how high/low to jump, relative to the start
     * @return number of ticks in the air
     */
    static double findSprintJumpReach(double height) {
        return sprintJump[MathHelper.binarySearch(0, sprintJump.length, (i) -> height > sprintJump[i][0])][1];
    }

    /**
     * Calculate the number of ticks in the air, when jumping.
     *
     * @param height how high/low to jump, relative to the start
     * @return number of ticks in the air
     */
    static int ticksToLand(double height) {
        return MathHelper.binarySearch(0, sprintJump.length, (i) -> height > sprintJump[i][0]) + 6;
    }

    /**
     * Gets the inertia/slipperiness of the block under the player
     *
     * @param player The player to use
     * @return inertia of the specified block
     */
    static float getGroundInertia(MovementDummy player) {
        return getGroundInertia(player.world, player.getPos());
    }

    /**
     * Gets the inertia/slipperiness of the block under the player
     *
     * @param world The world
     * @param pos   the position to get the inertia from
     * @return inertia of the specified block
     */
    static float getGroundInertia(World world, Vec3d pos) {
        // or BlockStateInterface.getBlock(ctx, dest).slipperiness?
        // BlockPos blockPos = this.getVelocityAffectingPos();
        // float t = player.world.getBlockState(blockPos).getBlock().getSlipperiness();
        return world.getBlockState(new BlockPos(MathHelper.fastFloor(pos.x), MathHelper.fastFloor(pos.y) - 1.0D, MathHelper.fastFloor(pos.z))).getBlock().getSlipperiness() * 0.91F;
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
    static double jumpReach(MovementDummy player, int ticks, Direction.Axis axis, boolean sprint) {

        double reach = 0.0;
        float yaw = player.getYaw(); //Should this become it's own variable?

        // We first have to add the one tick of ground movement.
        double motion = groundTick(player, sprint, true).getComponentAlongAxis(axis);
        reach += motion;
        // We still have ground, bc our Positions hasn't changed yet
        motion *= getGroundInertia(player);

        float trig = (axis.getName().equals("z")) ? MathHelper.cos(yaw * 0.017453292F) : -MathHelper.sin(yaw * 0.017453292F); // 0.017453292F = Math.PI / 180.0F

        trig *= ((sprint) ? 0.02548F : 0.0196F); // 0.02548 =  0.98 * (0.02 + 0.02 * 0.3); 0.0196 = 0.98 * 0.02

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
     * @see MovementHelper#jumpReach(MovementDummy, int, Direction.Axis, boolean)
     * @see net.minecraft.entity.player.PlayerEntity#travel(Vec3d)
     */
    static double fwdGroundTick(double motion, float inertia, float yaw, Direction.Axis axis, boolean sprint, boolean jump) {
        float movementFactor = (0.16277136F / inertia / inertia / inertia);
        float trig = (axis.getName().equals("z")) ? MathHelper.cos(yaw * 0.017453292F) : -MathHelper.sin(yaw * 0.017453292F);
        motion += movementFactor * trig * (sprint ? 0.12739 : 0.098) + ((sprint && jump) ? trig * 0.2 : 0); // 0.1274 =  0.98 * 0.13; 0.098 = 0.98 * 0.1
        return motion;
    }
//    static double fwdGroundTick(double motion, float inertia, float yaw, char dir, boolean sprint, boolean jump) {
//        float momentum = motion * inertia * 0.91 + (sprint ? 0.12739 : 0.098) ;
//        float trig = (dir == 'Z') ? MathHelper.cos(yaw * 0.017453292F) : -MathHelper.sin(yaw * 0.017453292F);
//        motion += movementFactor * trig * (sprint ? 0.12739 : 0.098) + ((sprint && jump) ? trig * 0.2 : 0); // 0.1274 =  0.98 * 0.13; 0.098 = 0.98 * 0.1
//        return motion;
//    }

    static Box jumpDistance(MovementDummy player, int ticks, float yaw, boolean sprinting) {
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
     * @see MovementHelper#jumpReach(MovementDummy, int, Direction.Axis, boolean)
     * @see net.minecraft.entity.player.PlayerEntity#travel(Vec3d)
     */
    static Vec3d groundTick(MovementDummy player, boolean sprint, boolean jump) {
        Vec3d newPos = new Vec3d(0.0, 0.0, 0.0);
        newPos.add(fwdGroundTick(player.getVelocity().x, getGroundInertia(player), player.getYaw(), Direction.Axis.X, sprint, jump), 0.0, 0.0);
        newPos.add(0.0, 0.0, fwdGroundTick(player.getVelocity().z, getGroundInertia(player), player.getYaw(), Direction.Axis.Z, sprint, jump));
        return newPos;
    }

//    public static ArrayList<PlayerInput> walkToCorner(MovementDummy player, double goalAngle, int corner) {
//    }

    public static double calcAngleDegXZ(Vec3d vec) {
        return Math.atan2(vec.getZ(), vec.getX()) * 180.0D / Math.PI - 90.0D;
    }

    public static double calcAngleRadXZ(Vec3i vec) {
        return Math.atan2(vec.getZ(), vec.getX()) - Math.PI / 2.0;
    }

    public static double roundRad(Vec3i vec3i, double roundTo, double shift) {
        return Math.round((MovementHelper.calcAngleRadXZ(vec3i) + shift) * 1 / roundTo) * roundTo - shift;
    }

    public static Vec3d rotatedVec(double len, double rad) {
        // our vec is (len|0|0)
        // x' = xcos(θ) − ysin(θ)
        // y' = xsin(θ) + ycos(θ)
        // return new Vec3d(len * Math.cos(rad), 0, len * Math.sin(rad));
        return new Vec3d(-len * Math.sin(rad), 0, len * Math.cos(rad));
    }

    public static Vec3d vec3dFromBlockPos(BlockPos pos, boolean treatAsBlockPos) {
        if (treatAsBlockPos) {
            return new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        } else {
            return new Vec3d(pos.getX(), pos.getY(), pos.getZ());
        }
    }

    /**
     * Calculates the angle relative to the world, which the vector from `vec1` to `vec2` has.
     *
     * @param vec1 First point
     * @param vec2 Block position
     * @return angle
     */
    public static double angleToVec(Vec3d vec1, Vec3i vec2) {
        Vec3d vecToBlock = vec1.subtract(vec2.getX() + 0.5D, 0, vec2.getZ() + 0.5D).multiply(-1);
        return calcAngleDegXZ(vecToBlock);
    }

    /**
     * Calculates the angle relative to the world, which the vector from `vec1` to `vec2` has.
     *
     * @param vec1 First point
     * @param vec2 Second point
     * @return angle
     */
    public static double angleToVec(Vec3d vec1, Vec3d vec2) {
        return calcAngleDegXZ(vec2.subtract(vec1));
    }

    public static double angleBetweenVec(Vec3d vec1, Vec3d vec2) {
        return Math.acos((vec1.getX() * vec2.getX() + vec1.getZ() * vec2.getZ()) / (vec1.horizontalLength() * vec2.horizontalLength()));
    }

    public static boolean isDirectPath(LivingEntity entity, BlockPos startBlock, BlockPos endBlock) {
        // 0.6 = entity.getDimensions(EntityPose.STANDING).width
        Vec3d[][] startCorners = getRaycastCorners(entity.world, startBlock, 0.6);
        Vec3d[][] endCorners = getRaycastCorners(entity.world, endBlock, 0.6);
        boolean shouldBrake = startCorners.length == 6 || endCorners.length == 6;
        for (int i = 0; i < 4; i++) {
            if (raycastHitboxFace(entity, startCorners[i], endCorners[i], false)) {
                return true;
            }
            if (raycastHitboxFace(entity, startCorners[i + 4], endCorners[i + 4], false)) {
                return true;
            }
            if (angleBetweenVec(startCorners[i][1].subtract(startCorners[i][0]), endCorners[i + 4][0].subtract(startCorners[i][0])) >
                    angleBetweenVec(startCorners[i][1].subtract(startCorners[i][0]), endCorners[i + 4][1].subtract(startCorners[i][0]))) {
                if (raycastHitboxFace(entity, startCorners[i], endCorners[i + 4], false)) {
                    return true;
                }
            } else {
                if (raycastHitboxFace(entity, startCorners[i], endCorners[i + 4], true)) {
                    return true;
                }
            }
            if (angleBetweenVec(startCorners[i + 4][1].subtract(startCorners[i + 4][0]), endCorners[i][0].subtract(startCorners[i + 4][0])) >
                    angleBetweenVec(startCorners[i + 4][1].subtract(startCorners[i + 4][0]), endCorners[i][1].subtract(startCorners[i + 4][0]))) {
                if (raycastHitboxFace(entity, startCorners[i + 4], endCorners[i], false)) {
                    return true;
                }
            } else {
                if (raycastHitboxFace(entity, startCorners[i + 4], endCorners[i], true)) {
                    return true;
                }
            }
            if (shouldBrake && i == 1) {
                break;
            }
        }
        return false;
    }

    private static Vec3d[][] getRaycastCorners(World world, BlockPos pos, double width) {
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
                    {new Vec3d(centerX - width, maxY, centerY - width), new Vec3d(centerX + width, maxY, centerY - width)},
                    {new Vec3d(centerX - width, maxY, centerY + width), new Vec3d(centerX + width, maxY, centerY + width)},
                    {}, {},
                    {new Vec3d(centerX - width, maxY, centerY - width), new Vec3d(centerX - width, maxY, centerY + width)},
                    {new Vec3d(centerX + width, maxY, centerY - width), new Vec3d(centerX + width, maxY, centerY + width)}
            };
        } else {
            // The order here is very important
            return new Vec3d[][]{
                    {new Vec3d(minX, maxY, minZ), new Vec3d(minX + width, maxY, minZ)},
                    {new Vec3d(maxX - width, maxY, minZ), new Vec3d(maxX, maxY, minZ)},
                    {new Vec3d(minX, maxY, maxZ), new Vec3d(minX + width, maxY, maxZ)},
                    {new Vec3d(maxX - width, maxY, maxZ), new Vec3d(maxX, maxY, maxZ)},
                    {new Vec3d(minX, maxY, minZ), new Vec3d(minX, maxY, minZ + width)},
                    {new Vec3d(minX, maxY, maxZ - width), new Vec3d(minX, maxY, maxZ)},
                    {new Vec3d(maxX, maxY, minZ), new Vec3d(maxX, maxY, minZ + width)},
                    {new Vec3d(maxX, maxY, maxZ - width), new Vec3d(maxX, maxY, minZ)},
            };
        }
    }

    public static boolean raycastHitboxFace(Entity entity, Vec3d[] start, Vec3d[] end, boolean reverse) {
        double height = 1.8;
        if (angleToVec(start[0], end[0]) == angleToVec(start[0], start[1]) || angleToVec(start[0], end[0]) == angleToVec(start[1], start[0])) {
            // This would mean, that we just check in one line, which is want we want prevent
            return false;
        }
        for (int i = 0; i < 2; i++) {
            if (entity.world.raycast(new RaycastContext(start[i], reverse ? end[1 - i] : end[i], RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.ANY, entity)).getType() != HitResult.Type.MISS) {
                return false;
            }
            if (entity.world.raycast(new RaycastContext(start[i].add(0, height, 0), reverse ? end[1 - i].add(0, height, 0) : end[i].add(0, height, 0), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.ANY, entity)).getType() != HitResult.Type.MISS) {
                return false;
            }
        }
        return true;
    }

    public static boolean simulateJump(MovementDummy testSubject, Vec3d goalFocus, Box goal, boolean allowSprint, int ticks) {
        float yaw = (float) MovementHelper.angleToVec(testSubject.getPos(), goalFocus);
        testSubject.applyInput(new PlayerInput(1.0F, 0.0F, yaw, 0.0F, true, false, allowSprint));
        MovementDummy prevTestSubject;
        // We are jumping, YEET
        for (int i = 0; i < ticks - 1; i++) {
            prevTestSubject = testSubject.clone();

            yaw = (float) MovementHelper.angleToVec(testSubject.getPos(), goalFocus);
            PlayerInput newInput = new PlayerInput(1.0F, 0.0F, yaw, 0.0F, false, false, allowSprint);
            testSubject.applyInput(newInput);

            if (testSubject.horizontalCollision && doObstacleAvoidance(testSubject, prevTestSubject, newInput, goalFocus, allowSprint) == 0.0) {
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
            newInput.yaw = (float) (MovementHelper.calcAngleDegXZ(testSubject.getVelocity()));
            testSubject.applyInput(newInput);

            // But do we actually travel further with the new yaw?
            if (diff > testSubject.getPos().squaredDistanceTo(prevTestSubject.getPos())) {
                // Nope, so do the same as before
                newInput = new PlayerInput(1.0F, 0.0F, MovementHelper.angleToVec(testSubject.getPos(), jumpFocus), 0.0F, false, false, allowSprint);
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
        Vec3d dirVec = rotatedVec(diagonalLen, roundRad(blockToNode, Math.PI / 2, Math.PI / 4));
        if (blockToNode.getX() <= 1 && blockToNode.getZ() <= 1) {
            // It could be that the block we want to walk onto is directly in front of us
            // in that case we just walk towards the block
            return dirVec;
        }
        int dirX = (int) Math.signum(dirVec.getX());
        int dirZ = (int) Math.signum(dirVec.getZ());
        if (!world.getBlockState(sourcePos.add(dirX, 1, dirZ)).isAir() ||
                !world.getBlockState(sourcePos.add(dirX, 2, dirZ)).isAir()) {
            // Round to 90 Deg, so that we move to one of the sides of the diagonal block
            dirVec = rotatedVec(straightLen, roundRad(blockToNode, Math.PI / 2, 0.0));
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
            dirVec = rotatedVec(corneredLen, roundRad(blockToNode, Math.PI / 2, Math.PI / 4));
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

}