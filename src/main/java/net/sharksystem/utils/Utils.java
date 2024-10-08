package net.sharksystem.utils;

import net.sharksystem.asap.ASAP;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class Utils {
    public static String url2FileName(String url) {
        // escape:
        /*
        see https://en.wikipedia.org/wiki/Percent-encoding
        \ - %5C, / - %2F, : - %3A, ? - %3F," - %22,< - %3C,> - %3E,| - %7C
        */

        if(url == null) return null; // to be safe

        String newString = url.replace("\\", "%5C");
        newString = newString.replace("/", "%2F");
        newString = newString.replace(":", "%3A");
        newString = newString.replace("?", "%3F");
        newString = newString.replace("\"", "%22");
        newString = newString.replace("<", "%3C");
        newString = newString.replace(">", "%3E");
        newString = newString.replace("|", "%7C");

        return newString;
    }

    /**
     *
     * @param rootFolder
     * @return collection of integer values depicting era present in that folder
     */
    public static Collection<Integer> getErasInFolder(String rootFolder) {
        Collection<Integer> eras = new HashSet<>();
        File dir = new File(rootFolder);
        String[] dirEntries = dir.list();
        if (dirEntries != null) {
            for (String fileName : dirEntries) {
                // era folder?
                try {
                    int era = Integer.parseInt(fileName);
                    // It is an era folder
                    eras.add(era);
                } catch (NumberFormatException e) {
                    // no number - no problem - go ahead!
                }
            }
        }
        return eras;
    }

    /**
     * Return true if both collections contain same number of elements and for each element in a there is an
     * identical element in b (String.compareTo())
     * in a has the "same"
     * @param a
     * @param b
     * @return
     */
    public static boolean sameContent(Collection<CharSequence> a, Collection<CharSequence> b) {
        if(a == null && b == null) return true;
        if(a == null) return false; // b is not null
        if(b == null) return false; // a is not null
        // bot not null
        if(a.size() != b.size()) return false;

        for(CharSequence aElement : a) {
            String aString = aElement.toString();
            boolean found = false;
            Iterator<CharSequence> bIterator = b.iterator();
            while(bIterator.hasNext() && !found) {
                String bString = bIterator.next().toString();
                if(bString.compareTo(aString) == 0) found = true;
            }
            if(!found) return false;
        }

        return true;
    }

    public static boolean compareArrays(byte[] a, byte[] b) {
        if(a.length != b.length) {
            Log.writeLog(net.sharksystem.utils.Utils.class, "not same length");
            return false;
        }

        if(a.length == 0) {
            Log.writeLog(net.sharksystem.utils.Utils.class, "len zero");
            return false;
        }

        for(int i = 0; i < a.length; i++) {
            Log.writeLog(net.sharksystem.utils.Utils.class, i + ": " + a[i] + " == " + b[i]);
            if(a[i] != b[i]) {
                Log.writeLog(net.sharksystem.utils.Utils.class, "no longer same");
                return false;
            }
        }

        return true;
    }

    public static String byteArray2String(byte[] b) {
        if(b == null) return "null";

        StringBuilder sb = new StringBuilder();
        sb.append("len == ");
        sb.append(b.length);
        sb.append("; content == (");
        for(int i = 0; i < b.length; i++) {
            if(i != 0) sb.append(", ");
            sb.append(b);
        }
        sb.append(")");

        return sb.toString();
    }

    public static String calendar2String(Calendar calendarObject) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z");
        return dateFormat.format(calendarObject.getTime());
    }

    public static String getStringByStringList(List<String> list) {
        if(list == null || list.isEmpty()) return "empty list";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        boolean first = true;
        for(String o : list) {
            if(first) first = false;
            else sb.append(", ");
            sb.append(i++);
            sb.append(": ");
            sb.append(o);
        }
        return sb.toString();
    }
}
