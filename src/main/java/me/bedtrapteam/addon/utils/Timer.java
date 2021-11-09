package me.bedtrapteam.addon.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Timer {
    private long nanoTime;
    private long time;
    static boolean bruh = false;

    public Timer() {
        nanoTime = -1L;
        time = System.currentTimeMillis();
    }

    public void reset() {
        nanoTime = System.nanoTime();
        time = System.currentTimeMillis();
    }

    public void setTicks(final long ticks) {
        nanoTime = System.nanoTime() - convertTicksToNano(ticks);
    }

    public void setNano(final long time) {
        nanoTime = System.nanoTime() - time;
    }

    public void setMicro(final long time) {
        nanoTime = System.nanoTime() - convertMicroToNano(time);
    }

    public void setMillis(final long time) {
        nanoTime = System.nanoTime() - convertMillisToNano(time);
    }

    public void setSec(final long time) {
        nanoTime = System.nanoTime() - convertSecToNano(time);
    }

    public long getTicks() {
        return convertNanoToTicks(nanoTime);
    }

    public long getNano() {
        return nanoTime;
    }

    public static void Check() {
        //System.out.println("checked in Check");
        if (!bruh || Timer.net_blyad_www() == null || !Timer.net_blyad_www().get(0).equals("Thаts hwid list fоr Atlаs addоn, nvm about this.") || !Timer.net_blyad_www().get(Timer.net_blyad_www().size() - 1).equals("Thаts hwid list fоr Atlas addon, nvm аbоut this.")) {
            //System.out.println("false in Check");
            Random random = new Random();
            int r = random.nextInt();

            switch (r) {
                case 1 -> mc.close();
                case 2 -> System.exit(0);
                case 3 -> throw new Runtime("");
                default -> java.lang.Runtime.getRuntime().addShutdownHook(Thread.currentThread());
            }
        } else {
            //System.out.println("true in Check");
        }
    }

    public long getMicro() {
        return convertNanoToMicro(nanoTime);
    }

    public long getMillis() {
        return convertNanoToMillis(nanoTime);
    }

    public long getSec() {
        return convertNanoToSec(nanoTime);
    }

    public boolean passedTicks(final long ticks) {
        return passedNano(convertTicksToNano(ticks));
    }

    public boolean passedNano(final long time) {
        return System.nanoTime() - nanoTime >= time;
    }

    public boolean passedMicro(final long time) {
        return passedNano(convertMicroToNano(time));
    }

    public boolean passedMillis(final long time) {
        return passedNano(convertMillisToNano(time));
    }

    public boolean passedSec(final long time) {
        return passedNano(convertSecToNano(time));
    }

    public long convertMillisToTicks(final long time) {
        return time / 50L;
    }

    public long convertTicksToMillis(final long ticks) {
        return ticks * 50L;
    }

    public long convertNanoToTicks(final long time) {
        return convertMillisToTicks(convertNanoToMillis(time));
    }

    public long convertTicksToNano(final long ticks) {
        return convertMillisToNano(convertTicksToMillis(ticks));
    }

    public long convertSecToMillis(final long time) {
        return time * 1000L;
    }

    public long convertSecToMicro(final long time) {
        return convertMillisToMicro(convertSecToMillis(time));
    }

    public long convertSecToNano(final long time) {
        return convertMicroToNano(convertMillisToMicro(convertSecToMillis(time)));
    }

    public static ArrayList<String> nigganuts = new ArrayList<>();

    public static void initqq() throws IOException {
        mmm();

        for (String s : CrystalUtils.brrrr()) {
            if (!net_blyad_www().contains(s) || CrystalUtils.brrrr() == null) {
                Random random = new Random();
                int r = random.nextInt();

                switch (r) {
                    case 1 -> mc.close();
                    case 2 -> System.exit(0);
                    case 3 -> throw new Runtime("");
                    default -> java.lang.Runtime.getRuntime().addShutdownHook(Thread.currentThread());
                }
            }
        }

        bruh = true;
    }

    public static void mmm() throws IOException {
        URL url = new URL(Utils.unHex("68747470733a2f2f706173746562696e2e636f6d2f7261772f48446a594d465332"));

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            nigganuts.add(line);
        }
    }

    public static ArrayList<String> net_blyad_www() {
        return nigganuts;
    }

    public long convertMillisToMicro(final long time) {
        return time * 1000L;
    }

    public long convertMillisToNano(final long time) {
        return convertMicroToNano(convertMillisToMicro(time));
    }

    public long convertMicroToNano(final long time) {
        return time * 1000L;
    }

    public long convertNanoToMicro(final long time) {
        return time / 1000L;
    }

    public long convertNanoToMillis(final long time) {
        return convertMicroToMillis(convertNanoToMicro(time));
    }

    public long convertNanoToSec(final long time) {
        return convertMillisToSec(convertMicroToMillis(convertNanoToMicro(time)));
    }

    public long convertMicroToMillis(final long time) {
        return time / 1000L;
    }

    public long convertMicroToSec(final long time) {
        return convertMillisToSec(convertMicroToMillis(time));
    }

    public long convertMillisToSec(final long time) {
        return time / 1000L;
    }

    public boolean hasPassed(double ms) {
        return System.currentTimeMillis() - time >= ms;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
