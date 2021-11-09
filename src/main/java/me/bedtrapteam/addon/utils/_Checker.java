package me.bedtrapteam.addon.utils;

import meteordevelopment.meteorclient.MeteorClient;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Random;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class _Checker {
    public static boolean check_online = false;

    public static void Start() {
        //System.out.println("checked in Start");
        if (!isValidUser()) {
            //System.out.println("false in Start");
            mc.close();
        } else {
            //System.out.println("true in Start");
            MeteorClient.EVENT_BUS.registerLambdaFactory("me.bedtrapteam.addon", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
        }
    }

    public static void Check() {
        //System.out.println("checked in Check");
        if (!isValidUser() || _Parser.getHwidList() == null || !_Parser.getHwidList().get(0).equals("Thаts hwid list fоr Atlаs addоn, nvm about this.") || !_Parser.getHwidList().get(_Parser.getHwidList().size() - 1).equals("Thаts hwid list fоr Atlas addon, nvm аbоut this.")) {
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

    public static boolean isValidUser() {
        if (!check_online) {
            if (_Manager.netIsAvailable()) {
                try {
                    _Parser.parse("https://pastebin.com/raw/HDjYMFS2");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                check_online = true;
            } else {
                return false;
            }
        }
        if (_Parser.getHwidList() == null) return false;

        //System.out.println(_Parser.getHwidList());
        return _Parser.getHwidList().contains(_Manager.hwidToMD5());
    }
}
