package me.bedtrapteam.addon.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

public class _Parser {
    public static ArrayList<String> hwid = new ArrayList<>();

    public static void parse(String link) throws IOException {
        URL url = new URL(link);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            hwid.add(line);
        }
    }

    public static ArrayList<String> getHwidList() {
        return hwid;
    }
}
