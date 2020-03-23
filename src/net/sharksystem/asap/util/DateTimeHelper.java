package net.sharksystem.asap.util;

import java.text.DateFormat;
import java.util.Date;

public class DateTimeHelper {
    public static final long TIME_NOT_SET = -1;

    public static String long2DateString(long longtime) {
        if(longtime == TIME_NOT_SET) return "not set (yet)";

        Date date = new Date(longtime);
        return DateFormat.getInstance().format(date);
    }
}
