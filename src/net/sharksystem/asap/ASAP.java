package net.sharksystem.asap;

public class ASAP {
    public static final int INITIAL_ERA = 0;
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
}
