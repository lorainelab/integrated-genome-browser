package com.lorainelab.igbr;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dcnorris
 */
public class SocketCommandListener {
    
    private static final int IGBR_PORT = 7084;

    public void start() throws IOException {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(IGBR_PORT);
            while (true) {
                Socket connection = socket.accept();
                CommandProcessor commandProcessor = new CommandProcessor(connection);
                commandProcessor.start();
                Logger.getLogger(SocketCommandListener.class.getName()).log(Level.INFO, "IGB socket connection started");
            }
        } catch (IOException ex) {
            Logger.getLogger(SocketCommandListener.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

   

}
