package com.lorainelab.igbr;

import com.affymetrix.genometryImpl.util.GeneralUtils;
//import com.affymetrix.igb.swing.ScriptManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dcnorris
 */
public class CommandProcessor extends Thread {

    private Socket connection;
    private BufferedReader in;
    private PrintWriter out;

    public CommandProcessor(Socket connection) {
        this.connection = connection;
        try {
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            out = new PrintWriter(connection.getOutputStream(), true);
        } catch (IOException ex) {
            Logger.getLogger(CommandProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void recieveIgbCommand(String igbCommand) {
        if (out != null) {
//           ScriptManager.getInstance().runScriptString(igbCommand, "igb");
            out.println("Command processed");
        }
    }

    @Override
    public void run() {
        try {
            String input;
            while ((input = in.readLine()) != null) {
                recieveIgbCommand(input);
            }
            connection.close();
        } catch (IOException ex) {
            Logger.getLogger(CommandProcessor.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            GeneralUtils.safeClose(in);
            GeneralUtils.safeClose(out);
        }

    }
}
