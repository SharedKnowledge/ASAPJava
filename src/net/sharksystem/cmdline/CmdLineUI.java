package net.sharksystem.cmdline;

import net.sharksystem.asap.*;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * @author thsc
 */
public class CmdLineUI {
    // commands
    public static final String CONNECT = "connect";
    public static final String OPEN = "open";
    public static final String EXIT = "exit";
    public static final String LIST = "list";
    public static final String KILL = "kill";
    public static final String SETWAITING = "setwaiting";
    public static final String CREATE_ASAP_ENGINE = "newengine";
    public static final String CREATE_ASAP_STORAGE = "newstorage";
    public static final String CREATE_ASAP_MESSAGE = "newmessage";
    public static final String RESET_ASAP_STORAGES = "resetstorage";

    private final PrintStream consoleOutput;
    private final BufferedReader userInput;

    public static final String TESTS_ROOT_FOLDER = "tests";
    private Map<String, MultiASAPEngineFS> engines = new HashMap();
    private String getStorageKey(String owner, String appName) {
        return owner + ":" + appName;
    }
    private Map<String, ASAPStorage> storages = new HashMap();

    public static void main(String[] args) {
        PrintStream os = System.out;

        os.println("Welcome SN2 version 0.1");
        CmdLineUI userCmd = new CmdLineUI(os, System.in);

        userCmd.printUsage();
        userCmd.runCommandLoop();
    }

    public CmdLineUI(PrintStream os, InputStream is) {
        this.consoleOutput = os;
        this.userInput = new BufferedReader(new InputStreamReader(is));
    }

    public CmdLineUI(PrintStream out) {
        this.consoleOutput = out;
        this.userInput = null;
    }

    public void printUsage() {
        StringBuilder b = new StringBuilder();

        b.append("\n");
        b.append("\n");
        b.append("valid commands:");
        b.append("\n");
        b.append(CONNECT);
        b.append(".. connect to remote engine");
        b.append("\n");
        b.append(OPEN);
        b.append(".. open socket");
        b.append("\n");
        b.append(LIST);
        b.append(".. list open connections");
        b.append("\n");
        b.append(KILL);
        b.append(".. kill an open connection");
        b.append("\n");
        b.append(SETWAITING);
        b.append(".. set waiting period");
        b.append("\n");
        b.append(CREATE_ASAP_ENGINE);
        b.append(".. create new asap engine");
        b.append("\n");
        b.append(CREATE_ASAP_STORAGE);
        b.append(".. create new asap storage");
        b.append("\n");
        b.append(CREATE_ASAP_MESSAGE);
        b.append(".. add message to storage");
        b.append("\n");
        b.append(RESET_ASAP_STORAGES);
        b.append(".. removes all asap storages");
        b.append("\n");
        b.append(EXIT);
        b.append(".. exit");

        this.consoleOutput.println(b.toString());
    }

    public void printUsage(String cmdString, String comment) {
        PrintStream out = this.consoleOutput;

        if(comment == null) comment = " ";
        out.println("malformed command: " + comment);
        out.println("use:");
        switch(cmdString) {
            case CONNECT:
                out.println(CONNECT + " [IP/DNS-Name_remoteHost] remotePort engineName");
                out.println("omitting remote host: localhost is assumed");
                out.println("example: " + CONNECT + " localhost 7070 Bob");
                out.println("example: " + CONNECT + " 7070 Bob");
                out.println("in both cases try to connect to localhost:7070 and let engine Bob handle connection when established");

                break;
            case OPEN:
                out.println(OPEN + " localPort engineName");
                out.println("example: " + OPEN + " 7070 Alice");
                out.println("opens a server socket #7070 and let engine Alice handle connection when established");
                break;
            case LIST:
                out.println("lists all open connections / client and server");
                break;
            case KILL:
                out.println(KILL + " channel name");
                out.println("example: " + KILL + " localhost:7070");
                out.println("kills channel named localhost:7070");
                out.println("channel names are produced by using list");
                out.println(KILL + "all .. kills all open connections");
                break;
            case SETWAITING:
                out.println(SETWAITING + " number of millis to wait between two connection attempts");
                out.println("example: " + KILL + " 1000");
                out.println("set waiting period to one second");
                break;
            case CREATE_ASAP_ENGINE:
                out.println(CREATE_ASAP_ENGINE + " name");
                out.println("example: " + CREATE_ASAP_ENGINE + " Alice");
                out.println("create engine called Alice - data kept under a folder called tests/Alice");
                break;
            case CREATE_ASAP_STORAGE:
                out.println(CREATE_ASAP_STORAGE + " owner appName");
                out.println("example: " + CREATE_ASAP_STORAGE + " Alice chat");
                break;
            case CREATE_ASAP_MESSAGE:
                out.println(CREATE_ASAP_MESSAGE + " owner appName uri message");
                out.println("example: " + CREATE_ASAP_MESSAGE + " Alice chat sn2://abChat HiBob");
                out.println("note: message can only be ONE string. That would not work:");
                out.println("does not work: " + CREATE_ASAP_MESSAGE + " Alice chat sn2://abChat Hi Bob");
                out.println("five parameters instead of four.");
                break;
            case RESET_ASAP_STORAGES:
                out.println(RESET_ASAP_STORAGES);
                out.println("removes all storages");
                break;
        }

        out.println("unknown command: " + cmdString);
    }

    public void runCommandLoop() {

        boolean again = true;
        while(again) {

            try {
                // read user input
                String cmdLineString = userInput.readLine();

                // finish that loop if less than nothing came in
                if(cmdLineString == null) break;

                // trim whitespaces on both sides
                cmdLineString = cmdLineString.trim();

                // extract command
                int spaceIndex = cmdLineString.indexOf(' ');
                spaceIndex = spaceIndex != -1 ? spaceIndex : cmdLineString.length();

                // got command string
                String commandString = cmdLineString.substring(0, spaceIndex);

                // extract parameters string - can be empty
                String parameterString = cmdLineString.substring(spaceIndex);
                parameterString = parameterString.trim();

                // start command loop
                switch(commandString) {
                    case CONNECT:
                        this.doConnect(parameterString); break;
                    case OPEN:
                        this.doOpen(parameterString); break;
                    case LIST:
                        this.doList(); break;
                    case KILL:
                        this.doKill(parameterString); break;
                    case SETWAITING:
                        this.doSetWaiting(parameterString); break;
                    case CREATE_ASAP_ENGINE:
                        this.doCreateASAPMultiEngine(parameterString); break;
                    case CREATE_ASAP_STORAGE:
                        this.doCreateASAPStorage(parameterString); break;
                    case CREATE_ASAP_MESSAGE:
                        this.doCreateASAPMessage(parameterString); break;
                    case RESET_ASAP_STORAGES:
                        this.doResetASAPStorages(); break;
                    case "q": // convenience
                    case EXIT:
                        this.doKill("all");
                        again = false; break; // end loop

                    default: this.consoleOutput.println("unknown command:" +
                            cmdLineString);
                        this.printUsage();
                        break;
                }
            } catch (IOException ex) {
                this.consoleOutput.println("cannot read from input stream");
                System.exit(0);
            }
        }
    }

    private Map<String, TCPChannel> channels = new HashMap<>();
    private long waitPeriod = 1000*30; // 30 seconds

    private void setWaitPeriod(long period) {
        this.waitPeriod = period;
    }

    private void startChannel(String name, TCPChannel channel, String engineName) throws ASAPException {
        channel.setWaitPeriod(this.waitPeriod);
        MultiASAPEngineFS multiASAPEngineFS = this.engines.get(engineName);
        if(multiASAPEngineFS == null) {
            throw new ASAPException("engine does not exist: " + engineName);
        }

        channel.setListener(new TCPChannelCreatedHandler(multiASAPEngineFS));
        channel.start();
        this.channels.put(name, channel);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                           method implementations                                   //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void doConnect(String parameterString) {
        StringTokenizer st = new StringTokenizer(parameterString);

        try {
            String remoteHost = st.nextToken();
            String remotePortString = st.nextToken();
            String engineName = null;
            if(!st.hasMoreTokens()) {
                // no remote host set - shift
                engineName = remotePortString;
                remotePortString = remoteHost;
                remoteHost = "localhost";
            } else {
                engineName = st.nextToken();
            }
            int remotePort = Integer.parseInt(remotePortString);

            String name =  remoteHost + ":" + remotePortString;

            this.startChannel(name,  new TCPChannel(remotePort, false, name), engineName);
        }
        catch(RuntimeException re) {
            this.printUsage(CONNECT, re.getLocalizedMessage());
        } catch (ASAPException e) {
            this.printUsage(CONNECT, e.getLocalizedMessage());
        }
    }

    public void doOpen(String parameterString) {
        StringTokenizer st = new StringTokenizer(parameterString);

        try {
            String portString = st.nextToken();
            String engineName = st.nextToken();

            int port = Integer.parseInt(portString);
            String name =  "server:" + port;

            this.startChannel(name,  new TCPChannel(port, true, name), engineName);
        }
        catch(RuntimeException re) {
            this.printUsage(OPEN, re.getLocalizedMessage());
        } catch (ASAPException e) {
            this.printUsage(OPEN, e.getLocalizedMessage());
        }
    }

    public void doList() {
        System.out.println("connections:");
        for(String connectionName : this.channels.keySet()) {
            System.out.println(connectionName);
        }
        System.out.println("storages:");
        for(String storageName : this.storages.keySet()) {
            System.out.println(storageName);
        }
        System.out.println("engines:");
        for(String engineName : this.engines.keySet()) {
            System.out.println(engineName);
        }
    }

    public void doKill(String parameterString) {
        StringTokenizer st = new StringTokenizer(parameterString);

        try {
            String channelName = st.nextToken();
            if(channelName.equalsIgnoreCase("all")) {
                System.out.println("kill all open channels..");
                for(TCPChannel channel : this.channels.values()) {
                    channel.kill();
                }
                this.channels = new HashMap<>();
                System.out.println(".. done");
            } else {

                TCPChannel channel = this.channels.remove(channelName);
                if (channel == null) {
                    System.err.println("channel does not exist: " + channelName);
                    return;
                }
                System.out.println("kill channel");
                channel.kill();

                System.out.println(".. done");
            }
        }
        catch(RuntimeException e) {
            this.printUsage(KILL, e.getLocalizedMessage());
        }
    }

    public void doSetWaiting(String parameterString) {
        StringTokenizer st = new StringTokenizer(parameterString);

        try {
            String waitingPeriodString = st.nextToken();
            long period = Long.parseLong(waitingPeriodString);
            this.setWaitPeriod(period);
        }
        catch(RuntimeException e) {
            this.printUsage(SETWAITING, e.getLocalizedMessage());
        }
    }

    public void doCreateASAPMultiEngine(String parameterString) {
        StringTokenizer st = new StringTokenizer(parameterString);

        try {
            String engineName = st.nextToken();
            this.engines.put(
                    engineName,
                    MultiASAPEngineFS_Impl.createMultiEngine(engineName,
                            TESTS_ROOT_FOLDER + "/" + engineName,
                            this.waitPeriod, null));
        }
        catch(RuntimeException e) {
            this.printUsage(CREATE_ASAP_ENGINE, e.getLocalizedMessage());
        } catch (IOException | ASAPException e) {
            this.printUsage(CREATE_ASAP_ENGINE, e.getLocalizedMessage());
        }
    }

    public void doCreateASAPStorage(String parameterString) {
        StringTokenizer st = new StringTokenizer(parameterString);

        try {
            String owner = st.nextToken();
            String appName = st.nextToken();
            String appFolderName = TESTS_ROOT_FOLDER + "/" + owner + "/" + appName;
            String format = "sn2://" + appName;

            ASAPStorage storage =
                    ASAPEngineFS.getASAPStorage(owner, appFolderName, format);

            this.storages.put(this.getStorageKey(owner, appName), storage);
        }
        catch(RuntimeException e) {
            this.printUsage(CREATE_ASAP_STORAGE, e.getLocalizedMessage());
        } catch (IOException | ASAPException e) {
            this.printUsage(CREATE_ASAP_STORAGE, e.getLocalizedMessage());
        }
    }

    public void doCreateASAPMessage(String parameterString) {
        StringTokenizer st = new StringTokenizer(parameterString);

        try {
            String owner = st.nextToken();
            String appName = st.nextToken();
            String uri = st.nextToken();
            String message = st.nextToken();

            // first - get storage
            ASAPStorage asapStorage = this.storages.get(this.getStorageKey(owner, appName));
            if(asapStorage == null) {
                System.err.println("storage does not exist: " + this.getStorageKey(owner, appName));
                return;
            }
            asapStorage.add(uri, message);
        }
        catch(RuntimeException e) {
            this.printUsage(CREATE_ASAP_MESSAGE, e.getLocalizedMessage());
        } catch (IOException e) {
            this.printUsage(CREATE_ASAP_MESSAGE, e.getLocalizedMessage());
        }
    }

    public void doResetASAPStorages() {
        ASAPEngineFS.removeFolder("tests");
    }

    public ASAPStorage getStorage(String storageName) {
        return this.storages.get(storageName);
    }
}