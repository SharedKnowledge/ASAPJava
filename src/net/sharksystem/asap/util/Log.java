package net.sharksystem.asap.util;

public class Log {
    public static StringBuilder startLog(Object o) {
        StringBuilder sb = new StringBuilder();
        sb.append(o.getClass().getSimpleName());
        sb.append(": ");
        return sb;
    }

    public static void writeLog(Object o, String message) {
        System.out.println(Log.startLog(o) +": " + message);
    }

    public static void writeLogErr(Object o, String message) {
        System.err.println(Log.startLog(o) +": " + message);
    }
}
