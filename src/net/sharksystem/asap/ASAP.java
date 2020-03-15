package net.sharksystem.asap;

public class ASAP {
    public static int nextEra(int workingEra) {
        if(workingEra == Integer.MAX_VALUE) {
            return 0;
        }

        return workingEra+1;
    }

    public static int previousEra(int workingEra) {
        if(workingEra == 0) {
            return Integer.MAX_VALUE;
        }

        return workingEra-1;
    }
}
