package net.sharksystem.cmdline;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * commands
 * - open
 * - connect
 * - list
 * - kill
 *
 * @author thsc
 */
public class CmdLineUI {
    public static final String CONNECT = "connect";
    public static final String OPEN = "open";
    public static final String EXIT = "exit";
    public static final String LIST = "list";
    public static final String KILL = "kill";

    private final PrintStream consoleOutput;
    private final BufferedReader userInput;

    public static void main(String[] args) {
        PrintStream os = System.out;

        os.println("Welcome SN2 version 0.1");
        CmdLineUI userCmd = new CmdLineUI(os, System.in);

        userCmd.printUsage();
        userCmd.runGame();
    }

    public CmdLineUI(PrintStream os, InputStream is) {
        this.consoleOutput = os;
        this.userInput = new BufferedReader(new InputStreamReader(is));
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
                out.println(CONNECT + " IP/DNS-Name_remoteHost remotePort");
                out.println("example: " + CONNECT + "  localhost 7070");
                out.println("tries to connect to localhost:7070");
                break;
            case OPEN:
                out.println(OPEN + " localPort");
                out.println("example: " + OPEN + " 7070");
                out.println("opens a server socket #7070");
                break;
            case LIST:
                out.println("lists all open connections / client and server");
                break;
            case KILL:
                out.println(KILL + " channel name");
                out.println("example: " + KILL + "localhost:7070");
                out.println("kills channel named localhost:7070");
                out.println("channel names are produced by using list");
                out.println(KILL + "all .. kills all open connections");
                break;
        }

        out.println("unknown command: " + cmdString);
    }

    public void runGame() {

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

    private Map<String, TCPChannel> openConnections = new HashMap<>();

    private void startChannel(String name, TCPChannel channel) {
        channel.setWaitPeriod(1000*30); // 30 seconds
        channel.start();
        this.openConnections.put(name, channel);
    }

    private void doConnect(String parameterString) {
        StringTokenizer st = new StringTokenizer(parameterString);

        try {
            String remoteHost = st.nextToken();
            String remotePortString = st.nextToken();
            int remotePort = Integer.parseInt(remotePortString);

            String name =  remoteHost + ":" + remotePortString;

            this.startChannel(name,  new TCPChannel(remotePort, false, name));
        }
        catch(RuntimeException re) {
            this.printUsage(CONNECT, re.getLocalizedMessage());
        }
    }

    private void doOpen(String parameterString) {
        StringTokenizer st = new StringTokenizer(parameterString);

        try {
            String portString = st.nextToken();

            int port = Integer.parseInt(portString);
            String name =  "server:" + port;

            this.startChannel(name,  new TCPChannel(port, true, name));
        }
        catch(RuntimeException re) {
            this.printUsage(OPEN, re.getLocalizedMessage());
        }
    }

    private void doList() {
        for(String connectionName : this.openConnections.keySet()) {
            System.out.println(connectionName);
        }
    }

    private void doKill(String parameterString) {
        StringTokenizer st = new StringTokenizer(parameterString);

        try {
            String channelName = st.nextToken();
            if(channelName.equalsIgnoreCase("all")) {
                System.out.println("kill all open channels..");
                for(TCPChannel channel : this.openConnections.values()) {
                    channel.kill();
                }
                this.openConnections = new HashMap<>();
                System.out.println(".. done");
            } else {

                TCPChannel channel = this.openConnections.remove(channelName);
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
}

