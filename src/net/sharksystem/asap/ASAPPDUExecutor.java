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

    public ASAPPDUExecutor(ASAP_PDU_1_0 asapPDU, InputStream is, OutputStream os,
                           EngineSetting engineSetting, ASAP_1_0 protocol) {
        this.asapPDU = asapPDU;
        this.is = is;
        this.os = os;
        this.engineSetting = engineSetting;
        this.protocol = protocol;

        System.out.println("ASAPPDUExecutor created - "
                + "folder: " + engineSetting.folder + " | "
                + "engine: " + engineSetting.engine.getClass().getSimpleName() + " | "
                + "listener: " + engineSetting.listener.getClass().getSimpleName());
    }

    public void run() {
        try {
            switch (asapPDU.getCommand()) {
                case ASAP_1_0.INTEREST_CMD:
                    System.out.println("ASAPPDUExecutor call handleASAPInterest one engine");
                    engineSetting.engine.handleASAPInterest((ASAP_Interest_PDU_1_0) asapPDU, protocol, os);
                    break;
                case ASAP_1_0.OFFER_CMD:
                    System.out.println("ASAPPDUExecutor call handleASAPOffer one engine");
                    engineSetting.engine.handleASAPOffer((ASAP_OfferPDU_1_0) asapPDU, protocol, os);
                    break;
                case ASAP_1_0.ASSIMILATE_CMD:
                    System.out.println("ASAPPDUExecutor call handleASAPAssimilate one engine");
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
    }
}
