package io.github.mariusdkm.ultrasonic.utils;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class MathUtils {
    private static final double INVPI = 1 / Math.PI;
    private static final double TAU = 2 * Math.PI;
    private static final double INVTAU = 1 / TAU;

    public static double calcAngleDegXZ(Vec3d vec) {
        return atan2(vec.getZ(), vec.getX()) * 180 / Math.PI - 90;
    }

    public static double calcAngleRadXZ(Vec3i vec) {
        return atan2(vec.getZ(), vec.getX()) - Math.PI / 2;
    }

    public static double roundRad(Vec3i vec3i, double roundTo, double shift) {
        return Math.round((calcAngleRadXZ(vec3i) + shift) / roundTo) * roundTo - shift;
    }

    public static Vec3d rotatedVec(double len, double rad) {
        // our vec is (len|0|0)
        // x' = xcos(θ) − ysin(θ)
        // y' = xsin(θ) + ycos(θ)
        // return new Vec3d(len * Math.cos(rad), 0, len * Math.sin(rad));
        return new Vec3d(-len * f_sin((float) rad), 0, len * f_cos(rad));
    }

    public static Vec3d vec3dFromBlockPos(BlockPos pos, boolean treatAsBlockPos) {
        if (treatAsBlockPos) {
            return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
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
        Vec3d vecToBlock = vec1.subtract(vec2.getX() + 0.5, 0, vec2.getZ() + 0.5).multiply(-1);
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

    public static double f_cos(double radians) {
        return f_sin(radians + 0.5 * Math.PI);
    }

    /**
     * Made by Qther
     *
     * @param radians input radians
     * @return sinus
     */
    public static double f_sin(double radians) {
        double x = radians - Math.floor((radians + Math.PI) * INVTAU) * TAU;

        x = 4 * x * INVPI * (Math.fma(-Math.abs(x), INVPI, 1));
        return x * Math.fma(0.224, Math.abs(x), 0.776);
    }

    /**
     * Handbook of Mathematical Functions
     * M. Abramowitz and I.A. Stegun, Ed.
     * Source: https://developer.download.nvidia.com/cg/acos.html
     * <p>
     * Absolute error <= 6.7e-5
     **/
    private static double acos(double x) {
        double negate = (x < 0) ? 1 : 0;
        x = Math.abs(x);
        double ret = Math.fma(Math.fma(Math.fma(-0.0187293, x, 0.0742610), x, -0.2121144), x, Math.PI / 2) * (Math.sqrt(1 - x));
        ret = Math.fma(2 * ret, negate, -ret);
        return Math.fma(negate, Math.PI, ret);
    }


    /**
     * Source: https://developer.download.nvidia.com/cg/atan2.html
     **/
    private static double atan2(double y, double x) {
        double t3 = Math.abs(x);
        double t1 = Math.abs(y);
        double t0 = Math.max(t3, t1);
        t1 = Math.min(t3, t1);
        t3 = 1 / t0;
        t3 = t1 * t3;

        double t4 = t3 * t3;
        t0 = -0.013480470;
        t0 = Math.fma(t0, t4, 0.057477314);
        t0 = Math.fma(t0, t4, -0.121239071);
        t0 = Math.fma(t0, t4, 0.195635925);
        t0 = Math.fma(t0, t4, -0.332994597);
        t0 = Math.fma(t0, t4, 0.999995630);
        t3 = t0 * t3;

        t3 = (Math.abs(y) > Math.abs(x)) ? Math.fma(Math.PI, 0.5, -t3) : t3;
        t3 = (x < 0) ? Math.PI - t3 : t3;

        return (y < 0) ? -t3 : t3;
    }
}
