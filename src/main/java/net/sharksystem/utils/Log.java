package net.sharksystem.utils;

import java.io.PrintStream;

public class Log {
    private static PrintStream outStream = System.out;
    private static PrintStream errStream = System.err;

    public static void setOutStream(PrintStream outStream) {
        Log.outStream = outStream;
    }

    public static void setErrStream(PrintStream errStream) {
        Log.errStream = errStream;
    }

    public static StringBuilder startLog(Object o, String parameter) {
        return startLog(o.getClass(), parameter);
    }

    public static StringBuilder startLog(Object o) {
        return startLog(o, null);
    }

    public static StringBuilder startLog(Class c) {
        return startLog(c, null);
    }

    public static StringBuilder startLog(Class c, String parameter) {
            StringBuilder sb = new StringBuilder();
        sb.append(c.getSimpleName());
        if(parameter != null && parameter.length() > 0) {
            sb.append("(");
            sb.append(parameter);
            sb.append(")");
        }
        sb.append(": ");
        return sb;
    }

    public static void writeLog(Object o, String parameter, String message) {
        Log.outStream.println(Log.startLog(o, parameter) + message);
    }

    public static void writeLog(Object o, CharSequence parameter, String message) {
        Log.outStream.println(Log.startLog(o, String.valueOf(parameter)) + message);
    }

    public static void writeLog(Object o, String message) {
        writeLog(o, null, message);
    }

    public static void writeLog(Class c, String parameter, String message) {
        Log.outStream.println(Log.startLog(c, parameter) + message);
    }

    public static void writeLog(Class c, String message) {
        writeLog(c, null, message);
    }

    public static void writeLogErr(Object o, String parameter, String message) {
        Log.errStream.println(Log.startLog(o, parameter) + message);
    }

    public static void writeLogErr(Object o, CharSequence parameter, String message) {
        Log.errStream.println(Log.startLog(o, String.valueOf(parameter)) + message);
    }

    public static void writeLogErr(Object o, String message) {
        writeLogErr(o, null, message);
    }

    public static void writeLogErr(Class c, String parameter, String message) {
        Log.errStream.println(Log.startLog(c, parameter) + message);
    }

    public static void writeLogErr(Class c, String message) {
        writeLogErr(c, null, message);
    }
}
