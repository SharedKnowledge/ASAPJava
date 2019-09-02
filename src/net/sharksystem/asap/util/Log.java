package net.sharksystem.asap.util;

public class Log {
    public static StringBuilder startLog(Object o) {
        StringBuilder sb = new StringBuilder();
        sb.append(o.getClass().getSimpleName());
        sb.append(": ");
        return sb;
    }
}
