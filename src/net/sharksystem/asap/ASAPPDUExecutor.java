package net.sharksystem.asap;

import net.sharksystem.asap.protocol.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ASAPPDUExecutor extends Thread {
    private final ASAP_PDU_1_0 asapPDU;
    private final InputStream is;
    private final OutputStream os;
    private final EngineSetting engineSetting;
    private final ASAP_1_0 protocol;
    private final ThreadFinishedListener threadFinishedListener;

    public ASAPPDUExecutor(ASAP_PDU_1_0 asapPDU, InputStream is, OutputStream os,
                           EngineSetting engineSetting, ASAP_1_0 protocol,
                           ThreadFinishedListener threadFinishedListener) {
        this.asapPDU = asapPDU;
        this.is = is;
        this.os = os;
        this.engineSetting = engineSetting;
        this.protocol = protocol;
        this.threadFinishedListener = threadFinishedListener;

        StringBuilder sb = new StringBuilder();
        sb.append("ASAPPDUExecutor created - ");
        sb.append("folder: " + engineSetting.folder + " | ");
        sb.append("engine: " + engineSetting.engine.getClass().getSimpleName() + " | ");
        if(engineSetting.listener != null) {
            sb.append("listener: " + engineSetting.listener.getClass().getSimpleName());
        }

        System.out.println(sb.toString());
    }

    private void finish() {
        if(this.threadFinishedListener != null) {
            this.threadFinishedListener.finished(this);
        }
    }

    public void run() {
        if(engineSetting.engine == null) {
            System.err.println("ASAPPDUExecutor called without engine set - fatal");
            this.finish();
            return;
        }

        System.out.println("ASAPPDUExecutor calls engine: " + engineSetting.engine.getClass().getSimpleName());

        if(asapPDU.getFormat().equalsIgnoreCase(ASAP_1_0.ASAP_MANAGEMENT_FORMAT.toString())) {
            System.out.println("ASAPPDUExecutor got asap management message - ignore");
            this.finish();
            return;
        }

        try {
            switch (asapPDU.getCommand()) {
                case ASAP_1_0.INTEREST_CMD:
                    System.out.println("ASAPPDUExecutor call handleASAPInterest");
                    engineSetting.engine.handleASAPInterest((ASAP_Interest_PDU_1_0) asapPDU, protocol, os);
                    break;
                case ASAP_1_0.OFFER_CMD:
                    System.out.println("ASAPPDUExecutor call handleASAPOffer");
                    engineSetting.engine.handleASAPOffer((ASAP_OfferPDU_1_0) asapPDU, protocol, os);
                    break;
                case ASAP_1_0.ASSIMILATE_CMD:
                    System.out.println("ASAPPDUExecutor call handleASAPAssimilate");
                    engineSetting.engine.handleASAPAssimilate((ASAP_AssimilationPDU_1_0) asapPDU, protocol, is, os,
                            engineSetting.listener);
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
                ex.printStackTrace();
            }
        }
        finally {
            this.finish();
        }
    }
}
