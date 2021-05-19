package net.sharksystem.asap;

import java.util.Random;

public class ASAP {
    public static final int INITIAL_ERA = 0;
    public static final int MIN_ERA = INITIAL_ERA;
    public static final int MAX_ERA = Integer.MAX_VALUE;

    public static int nextEra(int workingEra) {
        if(workingEra == ASAP.MAX_ERA) {
            return ASAP.INITIAL_ERA;
        }
        return workingEra+1;
    }

    public static int previousEra(int workingEra) {
        if(workingEra == ASAP.INITIAL_ERA) {
            return ASAP.MAX_ERA;
        }
        return workingEra-1;
    }

    /**
     * produces a unique id - is made up by current time in millis added with some random digits.
     * @return
     */
    public static String createUniqueID() {
        long now = System.currentTimeMillis();
        long nowUnchanged = now;

        /* now is a 64 bit value. Actually, only 63 bits are relevant because it is positive value.
        Moreover, we are already in the 21. century - could reduce bit even more. We ignore that obvious fact
        at first.

        Calculations: 2^64 = 1,8.. * 10^19; we take a..z and A..Z and 0..9 -> 62 digits
        62^11 = 5,2.. * 10^19 we need 11 digits
         */

        int randomDigits = 3;
        int timeDigits = 11;
        int digits = timeDigits + randomDigits;
        int basis = 62;

        /*
        0..9 --> 0..9
        10..35 -> a..z
        36..61 -> A..Z
         */

        char[] idChars = new char[digits];
        // init with random number
        for(int i = 0;  i < digits; i++) idChars[i] = '0';

        // let's fill it with code time stamp
        int i = 0;
        long rest = 0;
        while(now > 0) {
            rest = now % basis; // rest is number [0,63]
            now /= basis;

            idChars[i++] = ASAP.int2charID((int) rest);
        }

        // set index
        i = timeDigits;

        // random digits
        long rValue = now + nowUnchanged;
        Random random = new Random(rValue);
        //char lastC = ' '; // not value space
        while(i < digits) {
            int r = random.nextInt(basis);
            char c = ASAP.int2charID(r);
            //if(c == lastC) continue;
            idChars[i++] = c;
            rValue = (rValue * r * nowUnchanged) % basis;
            random = new Random(rValue);
            //random.setSeed(rValue);
        }

        return new String(idChars);
    }

    private static char int2charID(int value) {
        // convert value into valid values;
        // 0..9 -> 0..9
        if(value <= 9) {
            return (char)((int)'0' + value);
        } else {
            value -= 10;
            // a..z
            if(value <= 25) {
                return (char)((int)'a' + value);
            } else {
                // A..Z
                value -= 26;
                return (char)((int)'A' + value);
            }
        }
    }
}
