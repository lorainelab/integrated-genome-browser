package com.lorainelab.igbr;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.google.common.io.Closeables;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dcnorris
 */
public class SocketCommandListener implements Runnable {

    private static final int IGBR_PORT = 7084;

    @Override
    public void run() {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(IGBR_PORT);
            while (true) {
                Logger.getLogger(SocketCommandListener.class.getName()).log(Level.INFO, "Opening IGB command socket");
                Socket connection = socket.accept();
                CommandProcessor commandProcessor = new CommandProcessor(connection);
                commandProcessor.run();
                Logger.getLogger(SocketCommandListener.class.getName()).log(Level.INFO, "IGB socket connection started");
            }
        } catch (IOException ex) {
            Logger.getLogger(SocketCommandListener.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            GeneralUtils.closeQuietly(socket);
        }
    }

}
