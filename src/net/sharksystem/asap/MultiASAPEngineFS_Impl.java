package net.sharksystem.asap;

import net.sharksystem.asap.protocol.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

public class MultiASAPEngineFS_Impl implements MultiASAPEngineFS, PDUReaderListener {
    private CharSequence owner;
    private final HashMap<CharSequence, EngineSetting> folderMap;
    private final long maxExecutionTime;

    public static MultiASAPEngineFS createMultiEngine(CharSequence rootFolder, long maxExecutionTime,
                                                      ASAPReceivedChunkListener listener) throws ASAPException, IOException {
        return new MultiASAPEngineFS_Impl(rootFolder, maxExecutionTime, listener);
    }

    public static MultiASAPEngineFS createMultiEngine(CharSequence folder, ASAPReceivedChunkListener listener)
            throws ASAPException, IOException {

        return MultiASAPEngineFS_Impl.createMultiEngine(folder, DEFAULT_MAX_PROCESSING_TIME, listener);
    }


    MultiASAPEngineFS_Impl(CharSequence owner, List<ASAPEngineFSSetting> settings, long maxExecutionTime)
            throws ASAPException {

        if(settings == null) throw new ASAPException("no settings at all - makes no sense");
        if(settings.size() == 0) throw new ASAPException("no settings - makes no sense");

        this.owner = owner;
        this.folderMap = new HashMap<>();

        // fill settings
        for(ASAPEngineFSSetting setting : settings) {
            if(setting.folder.toString().contains("/") || setting.folder.toString().contains("\\")) {
                throw new ASAPException("sub folders are not allowed: " + setting.folder);
            }
            folderMap.put(setting.format, new EngineSetting(setting.folder, setting.listener));
        }

        this.maxExecutionTime = maxExecutionTime;
    }

    /**
     * assumed that a number of asap storages are already exists in subdirectories of the
     * root directory. setting list can be created by iterating those storages.
     * @param rootFolderName
     */
    MultiASAPEngineFS_Impl(CharSequence rootFolderName, long maxExecutionTime,
                                  ASAPReceivedChunkListener listener) throws ASAPException, IOException {

        this.owner = ASAPEngine.DEFAULT_OWNER; // probably dummy name
        this.maxExecutionTime = maxExecutionTime;
        this.folderMap = new HashMap<>();

        File rootFolder = new File(rootFolderName.toString());

        if (!rootFolder.isDirectory()) {
            throw new ASAPException("is not directory: " + rootFolderName);
        }

        File[] files = rootFolder.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                String fileName = file.getCanonicalPath();
                ASAPEngine engine = ASAPEngineFS.getExistingASAPEngineFS(fileName);
                EngineSetting setting = new EngineSetting(
                        rootFolderName + "/" + fileName, // folder
                        listener// listener
                        );
                setting.setASAPEngine(engine);
                this.folderMap.put(engine.format, setting);
            }
        }
    }

    public ASAPEngine getEngineByFormat(CharSequence format) throws ASAPException, IOException {
        // get engine
        EngineSetting engineSetting = this.getEngineSettings(format);
        ASAPEngine asapEngine = engineSetting.engine;

        if (asapEngine == null) {
            asapEngine = ASAPEngineFS.getASAPEngine(owner.toString(), engineSetting.folder.toString(), format);
            engineSetting.setASAPEngine(asapEngine); // remember - keep that object
        }
        return asapEngine;
    }

    private EngineSetting getEngineSettings(CharSequence format) throws ASAPException {
        EngineSetting folderAndListener = folderMap.get(format);
        if(folderAndListener == null) throw new ASAPException("no folder for owner / format: " + owner + "/" + format);

        return folderAndListener;
    }

    private Thread managementThread = null;

    @Override
    public void doneReadingPDU() {
        this.managementThread.interrupt();
    }

    public void handleConnection(InputStream is, OutputStream os) throws IOException, ASAPException {
        ASAP_1_0 protocol = new ASAP_Modem_Impl();

        // issue an interest for each owner / format combination
        for(CharSequence format : this.folderMap.keySet()) {
            protocol.interest(this.owner, null, format,null, -1, -1, os, false);
        }

        // start reading / processing loop
        while(true) {
            ASAPPDUReader pduReader = new ASAPPDUReader(protocol, is, this);
            pduReader.start();

            try {
                this.managementThread = Thread.currentThread();
                Thread.sleep(maxExecutionTime);
            } catch (InterruptedException e) {
                // should happen if successfully read something
            }

            // what happened?
            if(pduReader.ioExceptionMessage != null) {
                throw new IOException(pduReader.ioExceptionMessage);
            }

            if(pduReader.asapExceptionMessage != null) {
                System.out.println(this.getLogStart() + pduReader.asapExceptionMessage);
                return;
            }

            if(pduReader.asapPDU == null) {
                System.out.println(this.getLogStart() + "no data received during max execution time");
                os.close();
                is.close();
                System.out.println(this.getLogStart() + "closed streams");
                return;
            }

            // get engine
            EngineSetting engineSetting = this.getEngineSettings(pduReader.asapPDU.getFormat());
            if (engineSetting.engine == null) {
                ASAPEngine asapEngine = ASAPEngineFS.getASAPEngine(
                        owner.toString(),
                        engineSetting.folder.toString(),
                        pduReader.asapPDU.getFormat());

                engineSetting.setASAPEngine(asapEngine);
            }

            // process pdu
            ASAPPDUExecutor executor = new ASAPPDUExecutor(pduReader.asapPDU, is, os, engineSetting, protocol);
            executor.start();

            try {
                Thread.sleep(maxExecutionTime);
            } catch (InterruptedException e) {
                // cannot happen.
                e.printStackTrace();
            }

            if (executor.isAlive()) {
                // declare this a failure
                System.err.println(this.getLogStart() + "process that processes asap pdu takes longer than allowed - close streams");
                try {
                    os.close();
                } catch (IOException e) {
                    // ignore
                }
                try {
                    is.close();
                } catch (IOException e) {
                    // ignore
                }

                throw new ASAPException("process that processes asap pdu takes longer than allowed - closed streams");
            }
        }
    }

    private String getLogStart() {
        return this.getClass().getSimpleName() + ": ";
    }

    private class EngineSetting {
        final CharSequence folder;
        final ASAPReceivedChunkListener listener;
        private ASAPEngine engine;

        EngineSetting(CharSequence folder, ASAPReceivedChunkListener listener) {
            this.folder = folder;
            this.listener = listener;
        }

        void setASAPEngine(ASAPEngine engine) {
            this.engine = engine;
        }
    }

    private class ASAPPDUReader extends Thread {
        private final ASAP_1_0 protocol;
        private final InputStream is;
        private final PDUReaderListener pduReaderListener;
        ASAP_PDU_1_0 asapPDU = null;
        private String ioExceptionMessage = null;
        private String asapExceptionMessage = null;

        ASAPPDUReader(ASAP_1_0 protocol, InputStream is, PDUReaderListener pduReaderListener) {
            this.protocol = protocol;
            this.is = is;
            this.pduReaderListener = pduReaderListener;
        }

        public void run() {
            try {
                this.asapPDU = protocol.readPDU(is);
                this.pduReaderListener.doneReadingPDU();
            } catch (IOException e) {
                this.ioExceptionMessage = e.getLocalizedMessage();
            } catch (ASAPException e) {
                this.asapExceptionMessage = e.getLocalizedMessage();
            }
        }
    }

    private class ASAPPDUExecutor extends Thread {
        private final ASAP_PDU_1_0 asapPDU;
        private final InputStream is;
        private final OutputStream os;
        private final EngineSetting engineSetting;
        private final ASAP_1_0 protocol;

        ASAPPDUExecutor(ASAP_PDU_1_0 asapPDU, InputStream is, OutputStream os, EngineSetting engineSetting,
                        ASAP_1_0 protocol) {
            this.asapPDU = asapPDU;
            this.is = is;
            this.os = os;
            this.engineSetting = engineSetting;
            this.protocol = protocol;
        }

        public void run() {
            try {
                switch (asapPDU.getCommand()) {
                    case ASAP_1_0.INTEREST_CMD:
                        engineSetting.engine.handleASAPInterest((ASAP_Interest_PDU_1_0) asapPDU, protocol, os);
                        break;
                    case ASAP_1_0.OFFER_CMD:
                        engineSetting.engine.handleASAPOffer((ASAP_OfferPDU_1_0) asapPDU, protocol, os);
                        break;
                    case ASAP_1_0.ASSIMILATE_CMD:
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
}
