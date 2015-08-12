package com.lorainelab.igbr;

import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.igb.swing.script.ScriptManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javax.script.ScriptContext.ENGINE_SCOPE;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class CommandProcessor implements Runnable {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CommandProcessor.class);
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
            //TODO when time allows, I think this code probably could just use
            // ScriptEngine engine = engineMgr.getEngineByExtension("igb");...rather than use this singleton
            ScriptEngine scriptEngine = ScriptManager.getInstance().getScriptEngine("x." + "igb");
            SimpleScriptContext c = new SimpleScriptContext();
            c.setAttribute(ScriptManager.FILENAME, System.getProperty("user.home"), ENGINE_SCOPE);
            try {
                scriptEngine.eval(igbCommand, c);
            } catch (ScriptException ex) {
                logger.error(ex.getMessage(), ex);
            }
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
        } catch (SocketException ex) {
            //do nothing
        } catch (IOException ex) {
            Logger.getLogger(CommandProcessor.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            GeneralUtils.safeClose(in);
            GeneralUtils.safeClose(out);
        }

    }
}
