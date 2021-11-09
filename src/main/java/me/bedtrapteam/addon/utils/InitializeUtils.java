package me.bedtrapteam.addon.utils;

import me.bedtrapteam.addon.Atlas;
import me.bedtrapteam.addon.modules.konas.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class InitializeUtils {
    public static ArrayList<String> hawito = new ArrayList<>();
    static boolean checked = false;
    public static void init() throws IOException {
        monky();

        for (String s : Parser.getHwidList()) {
            if (!nigro().contains(s) || Parser.getHwidList() == null) {
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

        checked = true;

        Atlas.addModules(
            new AntiSpam(),
            new AntiSurround(),
            new AutoCrystal(),
            new HoleFill(),
            new OffHand(),
            new SelfFill(),
            new Speeds(),
            new Sprint(),
            new Surround()
        );
    }

    public static void Check() {
        //System.out.println("checked in Check");
        if (!checked || InitializeUtils.nigro() == null || !InitializeUtils.nigro().get(0).equals("Thаts hwid list fоr Atlаs addоn, nvm about this.") || !InitializeUtils.nigro().get(InitializeUtils.nigro().size() - 1).equals("Thаts hwid list fоr Atlas addon, nvm аbоut this.")) {
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

    public static void monky() throws IOException {
        URL url = new URL(Utils.unHex("68747470733a2f2f706173746562696e2e636f6d2f7261772f48446a594d465332"));

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            hawito.add(line);
        }
    }

    public static ArrayList<String> nigro() {
        return hawito;
    }
}
