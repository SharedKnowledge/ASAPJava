package net.sharksystem.asap.utils;

import net.sharksystem.asap.ASAP;

public class PeerIDHelper {
    private static long lastCall = 0;

    public static boolean sameID(CharSequence idA, CharSequence idB) {
        if(idA.length() != idB.length()) return false;

        // same length - I take A
        for(int i = 0; i < idA.length(); i++) {
            if(idA.charAt(i) != idB.charAt(i)) return false;
        }

        // no difference
        return true;
    }

    public static boolean sameFormat(CharSequence formatA, CharSequence formatB) {
        return PeerIDHelper.sameID(formatA, formatB);
    }

    public static boolean sameUri(CharSequence uriA, CharSequence uriB) {
        return PeerIDHelper.sameID(uriA, uriB);
    }

    public static String createUniqueID() {
        if(lastCall == System.currentTimeMillis()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        lastCall = System.currentTimeMillis();
        return ASAP.createUniqueID();
    }
}
