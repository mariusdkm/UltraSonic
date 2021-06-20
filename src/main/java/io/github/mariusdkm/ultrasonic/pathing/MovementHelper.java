package io.github.mariusdkm.ultrasonic.pathing;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import xyz.wagyourtail.jsmacros.client.movement.MovementDummy;

public class MovementHelper {
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
     * Calculate the number of ticks in the air, when jumping.
     *
     * @param relative how high/low to jump, relative to the start
     * @return number of ticks in the air
     */
    static int ticksToLand(double relative) {
        double height = 0;
        //After 5  Ticks we reach our height point an we always land on our way down. So only start at tick 5 to go back
        //This can be written diffenrently
        for (int t = 0; t < 256; t++) {
            if (height < relative && t > 5)
                return t;
            height += calcJumpMomentum(t);
        }
        return 0;
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
        return world.getBlockState(new BlockPos(MathHelper.floor(pos.x), MathHelper.floor(pos.y) - 1.0D, MathHelper.floor(pos.z))).getBlock().getSlipperiness() * 0.91F;
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
     * TODO More general by not using PlayerEntity?
     *
     * @param player The player, which is going to move
     * @param ticks  Number of ticks the player moves
     * @param dir    X or Z, in which direction to calculate
     * @param sprint Whether to sprint or not
     * @return How far relative the player is going to move.
     * @see net.minecraft.client.network.ClientPlayerEntity#travel(Vec3d)
     */
    static double jumpReach(MovementDummy player, int ticks, char dir, boolean sprint) {

        double reach = 0.0;
        float yaw = player.getYaw(); //Should this become it's own variable?

        //We first have to add the one tick of ground movement.
        double motion = groundTick(player, sprint, true).x;
        reach += motion;
        //We still have ground, bc our Positions hasn't changed yet
        motion *= getGroundInertia(player);

        float trig = (dir == 'Z') ? MathHelper.cos(yaw * 0.017453292F) : -MathHelper.sin(yaw * 0.017453292F); // 0.017453292F = Math.PI / 180.0F

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
     * @param dir     X or Z, in which direction to calculate
     * @param sprint  Whether to sprint or not
     * @param jump    Whether to jump int that tick or not. This gives the "jumpboost"
     * @return Motion after a Tick on the ground
     * @see MovementHelper#jumpReach(MovementDummy, int, char, boolean)
     * @see net.minecraft.entity.player.PlayerEntity#travel(Vec3d)
     */
    static double fwdGroundTick(double motion, float inertia, float yaw, char dir, boolean sprint, boolean jump) {
        float movementFactor = (0.16277136F / (inertia * inertia * inertia));
        float trig = (dir == 'Z') ? MathHelper.cos(yaw * 0.017453292F) : -MathHelper.sin(yaw * 0.017453292F);
        motion += movementFactor * trig * (sprint ? 0.12739 : 0.098) + ((sprint && jump) ? trig * 0.2 : 0); // 0.1274 =  0.98 * 0.13; 0.098 = 0.98 * 0.1
        return motion;
    }
//    static double fwdGroundTick(double motion, float inertia, float yaw, char dir, boolean sprint, boolean jump) {
//        float momentum = motion * inertia * 0.91 + (sprint ? 0.12739 : 0.098) ;
//        float trig = (dir == 'Z') ? MathHelper.cos(yaw * 0.017453292F) : -MathHelper.sin(yaw * 0.017453292F);
//        motion += movementFactor * trig * (sprint ? 0.12739 : 0.098) + ((sprint && jump) ? trig * 0.2 : 0); // 0.1274 =  0.98 * 0.13; 0.098 = 0.98 * 0.1
//        return motion;
//    }


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
     * @see MovementHelper#jumpReach(MovementDummy, int, char, boolean)
     * @see net.minecraft.entity.player.PlayerEntity#travel(Vec3d)
     */
    static Vec3d groundTick(MovementDummy player, boolean sprint, boolean jump) {
        Vec3d newPos = new Vec3d(0.0, 0.0, 0.0);
        newPos.add(fwdGroundTick(player.getVelocity().x, getGroundInertia(player), player.getYaw(), 'X', sprint, jump), 0.0, 0.0);
        newPos.add(0.0, 0.0, fwdGroundTick(player.getVelocity().z, getGroundInertia(player), player.getYaw(), 'Z', sprint, jump));
        return newPos;
    }

//    public static ArrayList<PlayerInput> walkToCorner(MovementDummy player, double goalAngle, int corner) {
//    }

    public static double calcXZAngle(Vec3d vec) {
        return Math.atan2(vec.getZ(), vec.getX()) * 180.0D / Math.PI - 90.0D;
    }

    public static Vec3d vec3dFromBlockPos(BlockPos pos, boolean treatAsBlockPos) {
        if (treatAsBlockPos) {
            return new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        } else {
            return new Vec3d(pos.getX(), pos.getY(), pos.getZ());
        }
    }
}


