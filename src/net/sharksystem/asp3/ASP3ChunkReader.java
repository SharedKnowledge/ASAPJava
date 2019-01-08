package net.sharksystem.asp3;

import java.io.DataInputStream;
import java.io.IOException;

class ASP3ChunkReader implements Runnable {
    ASP3Reader reader;
    private final DataInputStream dis;
    private final String peer;
    private final String owner;

    ASP3ChunkReader(ASP3Reader reader, DataInputStream dis, String owner, 
            String peer) {
        this.reader = reader;
        this.dis = dis;
        this.peer = peer;
        this.owner = owner;
    }

    private String getLogStart() {
        StringBuilder b = new StringBuilder();
        b.append("ASP3ChunkReader (");
        b.append(this.owner);
        b.append(") connected to (");
        b.append(this.peer);
        b.append(") ");

        return b.toString();
    }

    @Override
    public void run() {
        //<<<<<<<<<<<<<<<<<<debug
        StringBuilder b = new StringBuilder();
        b.append(this.getLogStart());
        b.append("start reading ");
        System.out.println(b.toString());
        //>>>>>>>>>>>>>>>>>>>debug

        try {
            String chunkUrl = dis.readUTF();
            //<<<<<<<<<<<<<<<<<<debug
            b = new StringBuilder();
            b.append(this.getLogStart());
            b.append("read chunk URL: ");
            b.append(chunkUrl);
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug
            for(;;) {
                // escapes with IOException
                String message = dis.readUTF();
                //<<<<<<<<<<<<<<<<<<debug
                b = new StringBuilder();
                b.append(this.getLogStart());
                b.append("read message: ");
                b.append(message);
                System.out.println(b.toString());
                //>>>>>>>>>>>>>>>>>>>debug

                if(this.reader != null) {
                    this.reader.read(chunkUrl, peer, message);
                }
            }
        } catch (IOException ex) {
            // done
        }
    }
}
