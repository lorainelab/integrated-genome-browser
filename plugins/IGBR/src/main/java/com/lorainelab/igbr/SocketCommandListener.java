package com.lorainelab.igbr;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class SocketCommandListener implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SocketCommandListener.class);
    private static final int IGBR_PORT = 7084;

    @Override
    public void run() {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(IGBR_PORT);
            while (true) {
                logger.info("Opening IGB command socket");
                Socket connection = socket.accept();
                CommandProcessor commandProcessor = new CommandProcessor(connection);
                commandProcessor.run();
                logger.info("IGB socket connection started");
            }
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            GeneralUtils.closeQuietly(socket);
        }
    }

}
