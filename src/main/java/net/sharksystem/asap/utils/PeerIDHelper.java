package net.sharksystem.asap.utils;

import net.sharksystem.asap.ASAP;

public class PeerIDHelper {
    public static boolean sameID(CharSequence idA, CharSequence idB) {
        if(idA == null && idB == null) return true; // both null - consider that same

        if(idA == null) return false; // obviously B is not null - not the same
        if(idB == null) return false; // obviously A is not null - not the same

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
        return ASAP.createUniqueID();
    }
}
