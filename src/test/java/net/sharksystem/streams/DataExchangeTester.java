package net.sharksystem.streams;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.utils.ASAPSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class DataExchangeTester implements Runnable {
    private final InputStream is;
    private final OutputStream os;
    private final String id;
    private int rounds;
    public boolean synced = false;

    DataExchangeTester(InputStream is, OutputStream os, int rounds, String id) {
        this.is = is;
        this.os = os;
        this.rounds = rounds;
        this.id = id;
    }

    @Override
    public void run() {
        // exchange some example data
        int value = 0;
        try {
            int maxValue = this.rounds - 1;
            System.out.println("Data Exchange Tester - count up to " + maxValue + " | " + id);
            while (this.rounds-- > 0) {
                System.out.println("write data: " + value + " | " + id);
                ASAPSerialization.writeIntegerParameter(value, this.os);
                int retVal = ASAPSerialization.readIntegerParameter(this.is);
                System.out.println("read data: " + retVal + " | " + id);

                if (value != retVal) {
                    System.out.println("data exchange testers are out of sync: " + id);
                    break;
                }
                value++;
            }
            System.out.println("synced: " + id);
            this.synced = true;
            // block
            System.out.println("read again - blocking (?): " + id);
            int retVal = this.is.read();
            System.out.println("back from read after synced: "  + retVal + " | " + id);
        } catch (IOException | ASAPException e) {
            System.out.println("exception data exchange tester - most probably good: " + id + " | "
                    + e.getLocalizedMessage());
        }
    }
}
