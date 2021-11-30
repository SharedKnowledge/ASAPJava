package net.sharksystem.utils;

import net.sharksystem.asap.ASAP;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

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
     *
     * @param searchSpace list of possible eras
     * @param fromEra lowest era
     * @param toEra highest era
     * @return list of era which are within from and to and also in search space
     */
    public static Collection<Integer> getErasInRange(Collection<Integer> searchSpace,
                                                     int fromEra, int toEra) {

        Collection<Integer> eras = new ArrayList<>();

        // the only trick is to be aware of the cyclic nature of era numbers
        boolean wrapped = fromEra > toEra; // it reached the era end and started new

        for(Integer era : searchSpace) {
            if(!wrapped) {
                //INIT ---- from-> +++++++++++++ <-to ----- MAX (+ fits)
                if(era >= fromEra && era <= toEra) eras.add(era);
            } else {
                // INIT+++++++++<-to ------ from->++++++MAX
                if(era <= toEra && era >= ASAP.INITIAL_ERA
                    || era >= fromEra && era <= ASAP.MAX_ERA
                ) eras.add(era);
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
            System.out.println("not same length");
            return false;
        }

        if(a.length == 0) {
            System.out.println("len zero");
            return false;
        }

        for(int i = 0; i < a.length; i++) {
            System.out.println(i + ": " + a[i] + " == " + b[i]);
            if(a[i] != b[i]) {
                System.out.println("no longer same");
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
}
