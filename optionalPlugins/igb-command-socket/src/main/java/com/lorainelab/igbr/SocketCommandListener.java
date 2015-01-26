package com.lorainelab.igbr;

import com.affymetrix.genometry.util.GeneralUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author dcnorris
 */
public class SocketCommandListener implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SocketCommandListener.class);
    private static final int IGBR_PORT = 7084;
    private Boolean stop = false;
    private ServerSocket socket = null;

    public void setStop(Boolean stop) {
        this.stop = stop;
        if (stop) {
            try {
                logger.info("Closing IGB command socket");
                socket.close();
            } catch (IOException ex) {
                //do nothing
            }
        }
    }

    @Override
    public void run() {
        try {
            socket = new ServerSocket(IGBR_PORT);
            while (!stop) {
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
