package me.bedtrapteam.addon.utils;

import java.io.IOException;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class _Checker {
    public static boolean check_online = false;

    public static void Authentication() {
        if (!isValidUser()) {
            mc.close();
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
        return _Parser.getHwidList().contains(_Manager.hwidToMD5());
    }
}
