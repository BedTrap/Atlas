package me.bedtrapteam.addon.utils;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class _Manager {
    public static String hwidToMD5(){
        String hwid = (System.getenv("os")
            + System.getProperty("os.arch")
            + System.getenv("SystemRoot")
            + System.getenv("HOMEDRIVE")
            + System.getenv("PROCESSOR_LEVEL")
            + System.getenv("PROCESSOR_REVISION")
            + System.getenv("PROCESSOR_IDENTIFIER")
            + System.getenv("PROCESSOR_ARCHITECTURE")
            + System.getenv("PROCESSOR_ARCHITEW6432")
            + System.getenv("NUMBER_OF_PROCESSORS")
            + new File("/").getTotalSpace());
        return md5(hwid);
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            StringBuilder hashtext = new StringBuilder(no.toString(16));
            while (hashtext.length() < 32) {
                hashtext.insert(0, "0");
            }
            return hashtext.toString().toUpperCase();
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean netIsAvailable() {
        try {
            final URL url = new URL("https://pastebin.com/raw/HDjYMFS2");
            final URLConnection conn = url.openConnection();
            conn.connect();
            conn.getInputStream().close();
            return true;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            return false;
        }
    }
}
