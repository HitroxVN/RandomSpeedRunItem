package me.HitroxVN.util;

public class TimeUtil {

    public static String format(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds %= 60;

        return minutes + "m " + seconds + "s";
    }
}