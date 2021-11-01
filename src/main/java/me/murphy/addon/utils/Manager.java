/*
 * Decompiled with CFR 0.150.
 *
 * Could not load the following classes:
 *  net.minecraft.client.MinecraftClient
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package me.murphy.addon.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Manager {
    public static final Manager INSTANCE = new Manager();
    public static MinecraftClient mc = MinecraftClient.getInstance();
    public static ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    public static Logger LOG = LogManager.getLogger();

    public static void ResetAllInfo() {
        TickUtils.ResetTickCount();
        //LatencyManager.get.ClearInfo();
    }
}

