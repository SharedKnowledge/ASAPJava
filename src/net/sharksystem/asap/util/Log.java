package net.sharksystem.asap.util;

public class Log {
    public static StringBuilder startLog(Object o) {
        return Log.startLog(o.getClass());
    }

    public static StringBuilder startLog(Class c) {
            StringBuilder sb = new StringBuilder();
        sb.append(c.getSimpleName());
        sb.append(": ");
        return sb;
    }

    public static void writeLog(Object o, String message) {
        System.out.println(Log.startLog(o) + message);
    }

    public static void writeLog(Class c, String message) {
        System.out.println(Log.startLog(c) + message);
    }

    public static void writeLogErr(Object o, String message) {
        System.err.println(Log.startLog(o) + message);
    }

    public static void writeLogErr(Class c, String message) {
        System.err.println(Log.startLog(c) + message);
    }
}
