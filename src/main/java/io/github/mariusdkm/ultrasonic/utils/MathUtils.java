package io.github.mariusdkm.ultrasonic.utils;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class MathUtils {
    private static final float PI = 3.141592653589793f;
    private static final float TAU = 2.0f * PI;

    public static double calcAngleDegXZ(Vec3d vec) {
        return atan2((float) vec.getZ(), (float) vec.getX()) * 180.0D / Math.PI - 90.0D;
    }

    public static double calcAngleRadXZ(Vec3i vec) {
        return atan2(vec.getZ(), vec.getX()) - Math.PI / 2.0;
    }

    public static double roundRad(Vec3i vec3i, double roundTo, double shift) {
        return Math.round((calcAngleRadXZ(vec3i) + shift) * 1 / roundTo) * roundTo - shift;
    }

    public static Vec3d rotatedVec(double len, double rad) {
        // our vec is (len|0|0)
        // x' = xcos(θ) − ysin(θ)
        // y' = xsin(θ) + ycos(θ)
        // return new Vec3d(len * Math.cos(rad), 0, len * Math.sin(rad));
        return new Vec3d(-len * f_sin((float) rad), 0, len * f_cos((float) rad));
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
        return acos((float) ((vec1.getX() * vec2.getX() + vec1.getZ() * vec2.getZ()) / (vec1.horizontalLength() * vec2.horizontalLength())));
    }

    public static float f_cos(float radians) {
        return f_sin(radians + 0.5f * PI);
    }

    /**
     * Made by Qther
     *
     * @param radians input radians
     * @return sinus
     */
    public static float f_sin(float radians) {
        float x = radians - (int) (radians / TAU) * TAU;
        if (x > PI) {
            x -= TAU;
        }
        x = 4.0f * x / PI * (1 - Math.abs(x) / PI);

        return 0.224f * (x * Math.abs(x) - x) + x;
    }

    /**
     * Handbook of Mathematical Functions
     * M. Abramowitz and I.A. Stegun, Ed.
     * Source: https://developer.download.nvidia.com/cg/acos.html
     * <p>
     * Absolute error <= 6.7e-5
     **/
    private static float acos(float x) {
        float negate = Math.signum(x);
        x = Math.abs(x);
        float ret = -0.0187293f;
        ret = ret * x;
        ret = ret + 0.0742610f;
        ret = ret * x;
        ret = ret - 0.2121144f;
        ret = ret * x;
        ret = ret + 1.5707288f;
        ret = ret * MathHelper.sqrt(1.0f - x);
        ret = ret - 2 * negate * ret;
        return negate * PI + ret;
    }


    /**
     * Source: https://developer.download.nvidia.com/cg/atan2.html
     **/
    private static float atan2(float y, float x) {
        float t0, t1, t3, t4;

        t3 = Math.abs(x);
        t1 = Math.abs(y);
        t0 = Math.max(t3, t1);
        t1 = Math.min(t3, t1);
        t3 = 1f / t0;
        t3 = t1 * t3;

        t4 = t3 * t3;
        t0 = -0.013480470f;
        t0 = t0 * t4 + 0.057477314f;
        t0 = t0 * t4 - 0.121239071f;
        t0 = t0 * t4 + 0.195635925f;
        t0 = t0 * t4 - 0.332994597f;
        t0 = t0 * t4 + 0.999995630f;
        t3 = t0 * t3;

        t3 = (Math.abs(y) > Math.abs(x)) ? 1.570796327f - t3 : t3;
        t3 = (x < 0) ? 3.141592654f - t3 : t3;
        t3 = (y < 0) ? -t3 : t3;

        return t3;
    }
}
