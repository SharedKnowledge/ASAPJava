package net.sharksystem.asap.utils;

public class PeerIDHelper {
    public static boolean sameID(CharSequence idA, CharSequence idB) {
        if(idA.length() != idB.length()) return false;

        // same length - I take A
        for(int i = 0; i < idA.length(); i++) {
            if(idA.charAt(i) != idB.charAt(i)) return false;
        }

        // no difference
        return true;
    }
}
