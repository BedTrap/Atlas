package me.bedtrapteam.addon.utils;

import net.minecraft.util.math.MathHelper;

import java.util.Arrays;

public class TickRateUtils {
    private static final float[] tickRates = new float[20];
    private static int nextIndex = 0;
    private static long timeLastTimeUpdate;

    public static void reset() {
        nextIndex = 0;
        timeLastTimeUpdate = -1L;
        Arrays.fill(tickRates, 0.0F);
    }

    public static float getTickRate() {
        float numTicks = 0.0F;
        float sumTickRates = 0.0F;
        for (float tickRate : tickRates) {
            if (tickRate > 0.0F) {
                sumTickRates += tickRate;
                numTicks += 1.0F;
            }
        }
        return MathHelper.clamp(sumTickRates / numTicks, 0.0F, 20.0F);
    }

    public static float getMinTickRate() {
        float minTick = 20.0F;
        for (float tickRate : tickRates) {
            if (tickRate > 0.0F) {
                if (tickRate < minTick) {
                    minTick = tickRate;
                }
            }
        }
        return MathHelper.clamp(minTick, 0.0F, 20.0F);
    }

    public static float getLatestTickRate() {
        try {
            return MathHelper.clamp(tickRates[tickRates.length - 1], 0.0F, 20.0F);
        } catch (Exception e) {
            e.printStackTrace();
            return 20.0F;
        }
    }

    public static void onTimeUpdate() {
        if (timeLastTimeUpdate != -1L) {
            float timeElapsed = (float) (System.currentTimeMillis() - timeLastTimeUpdate) / 1000.0F;
            tickRates[(nextIndex % tickRates.length)] = MathHelper.clamp(20.0F / timeElapsed, 0.0F, 20.0F);
            nextIndex += 1;
        }
        timeLastTimeUpdate = System.currentTimeMillis();
    }
}
