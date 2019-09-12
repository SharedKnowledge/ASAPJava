package net.sharksystem.asap;

import net.sharksystem.asap.protocol.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ASAPPDUExecutor extends Thread {
    private final ASAP_PDU_1_0 asapPDU;
    private final InputStream is;
    private final OutputStream os;
    private final ASAPEngine engine;
    private final ASAP_1_0 protocol;
    private final ASAPChunkReceivedListener listener;
    private final ThreadFinishedListener threadFinishedListener;

    public ASAPPDUExecutor(ASAP_PDU_1_0 asapPDU, InputStream is, OutputStream os,
                           ASAPEngine engine, ASAP_1_0 protocol,
                           ASAPChunkReceivedListener listener,
                           ThreadFinishedListener threadFinishedListener) {
        this.asapPDU = asapPDU;
        this.is = is;
        this.os = os;
        this.engine = engine;
        this.protocol = protocol;
        this.listener = listener;
        this.threadFinishedListener = threadFinishedListener;

        if(listener != null) {
            System.out.println("ASAPPDUExecutor created: "
                    + "engine: " + engine.getClass().getSimpleName() + " | "
                    + "listener: " + listener.getClass().getSimpleName());
        } else {
            System.out.println("ASAPPDUExecutor created: "
                    + "engine: " + engine.getClass().getSimpleName() + " | "
                    + "no chunk received listener: ");
        }
    }

    private String getLogStart() {
        return this.getClass().getSimpleName() + ": ";
    }

    public void run() {
        try {
            String engineClass = engine.getClass().getSimpleName();
            switch (asapPDU.getCommand()) {
                case ASAP_1_0.INTEREST_CMD:
                    System.out.println(this.getLogStart() + "call " + engineClass + ".handleASAPInterest()");
                    engine.handleASAPInterest((ASAP_Interest_PDU_1_0) asapPDU, protocol, os);
                    System.out.println(this.getLogStart() + "done " + engineClass + ".handleASAPInterest()");
                    break;
                case ASAP_1_0.OFFER_CMD:
                    System.out.println(this.getLogStart() + "call " + engineClass + ".handleASAPOffer()");
                    engine.handleASAPOffer((ASAP_OfferPDU_1_0) asapPDU, protocol, os);
                    System.out.println(this.getLogStart() + "done " + engineClass + ".handleASAPOffer()");
                    break;
                case ASAP_1_0.ASSIMILATE_CMD:
                    System.out.println(this.getLogStart() + "call " + engineClass + ".handleASAPAssimilate()");
                    engine.handleASAPAssimilate((ASAP_AssimilationPDU_1_0) asapPDU, protocol, is, os,
                            listener);
                    System.out.println(this.getLogStart() + "done " + engineClass + ".handleASAPAssimilate()");
                    break;

                default:
                    System.err.println(
                            this.getClass().getSimpleName() + ": " + "unknown ASAP command: " + asapPDU.getCommand());
            }
        }
        catch(IOException | ASAPException e) {
            System.err.println("Exception while processing ASAP PDU - close streams" + e.getLocalizedMessage());
            try {
                os.close(); // more important to close than input stream - do it first
                is.close();
            } catch (IOException ex) {
                System.err.println("could not close streams - ignore probably already closed: " + e.getLocalizedMessage());
            }
        }

        if(this.threadFinishedListener != null) {
            this.threadFinishedListener.finished(this);
        }
    }
}
