package net.sharksystem.asap.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Helper {
    public static final String SERIALIZATION_DELIMITER = "|||";

    public static String list2String(List<CharSequence> stringList) {
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        if(stringList != null && !stringList.isEmpty()) {
            for(CharSequence s : stringList) {
                if(s != null) {
                    if(!first) {
                        sb.append(SERIALIZATION_DELIMITER);
                    } else {
                        first = false;
                    }
                    sb.append(s);
                }
            }
        }

        return sb.toString();
    }

    public static List<CharSequence> string2CharSequenceList(String s) {
        StringTokenizer t = new StringTokenizer(s, SERIALIZATION_DELIMITER);
        ArrayList<CharSequence> list = new ArrayList<CharSequence>();

        while(t.hasMoreTokens()) {
            list.add(t.nextToken());
        }

        return list;
    }
}