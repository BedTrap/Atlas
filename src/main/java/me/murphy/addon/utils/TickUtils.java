/*
 * Decompiled with CFR 0.150.
 *
 * Could not load the following classes:
 *  net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket
 *  net.minecraft.util.math.MathHelper
 */
package me.murphy.addon.utils;

import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.util.math.MathHelper;
public class TickUtils extends Manager {
    private static long tick_count = 0L;
    private static long last_update_time = -1L;
    private static long next_tick_time = -1L;
    public static long last_processed_tick = 0L;
    private static long interval_per_tick = 50L;
    private static long last_world_time = -1L;
    private static long game_joined_time = -1L;

    public static void onWorldTimeUpdate(WorldTimeUpdateS2CPacket packet) {
        if (last_update_time != -1L && last_world_time != -1L) {
            long time_delta = Utils.GetCurTime() - last_update_time;
            long tick_delta = packet.getTime() - last_world_time;
            interval_per_tick = MathHelper.clamp((long)(time_delta / tick_delta), (long)50L, (long)1000L);
        }
        last_update_time = Utils.GetCurTime();
        last_world_time = packet.getTime();
        tick_count = packet.getTime();
    }

    public static void onTick() {
        long cur_time = Utils.GetCurTime();
        if (cur_time < game_joined_time + 4000L) {
            TickUtils.IncrementTickCount();
            return;
        }
        if (next_tick_time == -1L) {
            next_tick_time = cur_time;
        }
        if (cur_time >= next_tick_time) {
            TickUtils.IncrementTickCount();
            while (next_tick_time <= cur_time) {
                next_tick_time += interval_per_tick;
            }
        }
    }

    public static long GetIntervalPerTick() {
        return 50L;
    }

    public static boolean CheckTickChanged() {
        if (tick_count != last_processed_tick) {
            last_processed_tick = tick_count;
            return true;
        }
        return false;
    }

    public static int GetTickCount() {
        return (int)tick_count;
    }

    public static void IncrementTickCount() {
        ++tick_count;
    }

    public static void ResetTickCount() {
        tick_count = 0L;
        interval_per_tick = 50L;
        last_processed_tick = 0L;
        last_update_time = -1L;
        next_tick_time = -1L;
        last_world_time = -1L;
        game_joined_time = Utils.GetCurTime();
    }
}

