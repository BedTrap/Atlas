package me.bedtrapteam.addon.utils;

import meteordevelopment.meteorclient.systems.modules.Module;

import static me.bedtrapteam.addon.utils.enchansed.Block2Utils.mc;

public class TimerManager {
    private static Module currentModule;
    private static int priority;
    private static float timerSpeed;
    private static boolean active = false;

    public static void updateTimer(Module module, int priority, float timerSpeed) {
        if (module == currentModule) {
            TimerManager.priority = priority;
            TimerManager.timerSpeed = timerSpeed;
            TimerManager.active = true;
        } else if (priority > TimerManager.priority || !TimerManager.active) {
            TimerManager.currentModule = module;
            TimerManager.priority = priority;
            TimerManager.timerSpeed = timerSpeed;
            TimerManager.active = true;
        }
    }

    public static void resetTimer(Module module) {
        if (TimerManager.currentModule == module) {
            active = false;
        }
    }

    public static float getTimerSpeed() {
        if (!mc.isOnThread()) active = false;
        return active ? timerSpeed : 1F;
    }
}
