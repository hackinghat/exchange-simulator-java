package com.hackinghat.util;

public class FloatUtil {

    // a == b iff |a-b| < eps
    public static boolean almost_eq_eps(final float a, final float b, final float eps) {
        return Math.abs(a - b) < eps;
    }

    // a < b iff (a-eps)<(b+eps)
    public static boolean almost_lt_eps(final float a, final float b, final float eps) {
        return (a - eps) < (b + eps);
    }

    public static float round(float number, int scale) {
        int pow = 10;
        for (int i = 1; i < scale; i++)
            pow *= 10;
        float tmp = number * pow;
        return ((float) ((int) ((tmp - (int) tmp) >= 0.5f ? tmp + 1 : tmp))) / pow;
    }
}
