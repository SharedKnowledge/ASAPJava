package net.sharksystem.asap.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeHelper {
    public static final long TIME_NOT_SET = -1;

    private static String guardNull(long longtime) {
        if(longtime == TIME_NOT_SET) return "not set (yet)";
        else return null;
    }

    public static String long2DateString(long longtime) {
        String ret = guardNull(longtime);
        if(ret != null) return ret;

        Date date = new Date(longtime);
        return DateFormat.getInstance().format(date);
    }

    public static String long2ExactTimeString(long longtime) {
        String ret = guardNull(longtime);
        if(ret != null) return ret;

        Date date = new Date(longtime);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        return df.format(date);
    }
}
