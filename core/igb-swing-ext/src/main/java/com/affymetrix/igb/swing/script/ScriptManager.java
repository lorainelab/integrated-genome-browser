package com.affymetrix.igb.swing.script;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.igb.swing.JRPWidget;
import com.affymetrix.igb.swing.JRPWidgetDecorator;
import com.affymetrix.igb.swing.Operation;
import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.JOptionPane;

public class ScriptManager {

    public final static String SCRIPTING = "scripting";
    private final static String WILDCARD = "*";
    private static final ScriptManager instance = new ScriptManager();
    private List<Operation> operations = new ArrayList<>();
    private Map<String, JRPWidget> widgets = new HashMap<>();
    private boolean mouseDown;
    private InputHandler inputHandler;

    public static interface InputHandler {

        public InputStream getInputStream(String fileName) throws Exception;
    }

    public static ScriptManager getInstance() {
        return instance;
    }

    private ScriptManager() {
        super();
        mouseDown = false;
        long eventMask = AWTEvent.MOUSE_EVENT_MASK;

        Toolkit.getDefaultToolkit().addAWTEventListener(e -> {
            if (e.getID() == MouseEvent.MOUSE_PRESSED) {
                mouseDown = true;
            }
            if (e.getID() == MouseEvent.MOUSE_RELEASED) {
                mouseDown = false;
            }
        }, eventMask);
    }

    public boolean isMouseDown() {
        return mouseDown;
    }

    public void setInputHandler(InputHandler inputHandler) {
        this.inputHandler = inputHandler;
    }

    public void addWidget(JRPWidget widget) {
        if (widgets.get(widget.getId()) != null) {
//			Logger.getLogger(getClass().getName()).log(Level.WARNING, "duplicate id for widget " + widget.getId());
        }
        widgets.put(widget.getId(), widget);
        if (ExtensionPointHandler.getExtensionPoint(JRPWidgetDecorator.class) != null) {
            for (JRPWidgetDecorator decorator : ExtensionPointHandler.getExtensionPoint(JRPWidgetDecorator.class).getExtensionPointImpls()) {
                decorator.widgetAdded(widget);
            }
        }
    }

    public void removeWidget(String id) {
        widgets.remove(id);
    }

    public String getScript(ScriptProcessor scriptProcessor) {
        StringBuilder sb = new StringBuilder();
        sb.append(scriptProcessor.getHeader());
        sb.append("\n");
        for (Operation operation : operations) {
            sb.append(scriptProcessor.getCommand(operation));
            sb.append("\n");
        }
        return sb.toString();
    }

    public JRPWidget getWidget(String id) {
        return widgets.get(id);
    }

    public void clearScript() {
        operations.clear();
    }

    public void recordOperation(Operation operation) {
        if (!operation.getWidget().consecutiveOK()) {
            int lastIndex = operations.size() - 1;
            if (lastIndex >= 0) {
                Operation lastOperation = operations.get(lastIndex);
                if (operation.getId().equals(lastOperation.getId())) {
                    operations.set(lastIndex, operation);
                    return;
                }
            }
        }
        operations.add(operation);
    }

    /**
     * Run text lines in the specified scripting language.
     */
    public void runScriptString(String scriptText, String extension) {
        try {
            ScriptEngine engine = getScriptEngine("x." + extension); // fake file name
            if (engine == null) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "igb script engine is not loaded");
                return;
            }
            engine.eval(scriptText);
        } catch (ScriptException ex) {
            ex.printStackTrace();
        }
    }

    public ScriptEngine getScriptEngine(String fileName) {
        ScriptEngineManager engineMgr = new ScriptEngineManager();
        int pos = fileName.lastIndexOf('.');
        if (pos == -1) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "no extension for file {0}", fileName);
            return null;
        }
        String extension = fileName.substring(pos + 1);
        ScriptEngine engine = engineMgr.getEngineByExtension(extension);
        ScriptProcessor scriptProcessor = ScriptProcessorHolder.getInstance().getScriptProcessor(extension);
        if (scriptProcessor != null) {
            engine = scriptProcessor.getScriptEngineFactory().getScriptEngine();
        }
        return engine;
    }

    public boolean isScript(String fileName) {
        return getScriptEngine(fileName) != null;
    }

    public void runScript(String fileName) {
        if (fileName.equals(WILDCARD)) {
            return;
        }
        try {
            ScriptEngine engine = getScriptEngine(fileName);
            if (engine == null) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "script engine is null for file {0}", fileName);
                return;
            }
            InputStream is;
            try {
                if (inputHandler == null) {
                    is = new FileInputStream(fileName);
                    Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Input handler not set. Using FileInputStream.");
                } else {
                    is = inputHandler.getInputStream(fileName);
                }
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error getting input stream for script file " + fileName, ex);
                return;
            }

            if (is == null) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Input stream null for script file {0}", fileName);
                return;
            }

            Reader reader = new InputStreamReader(is);
            engine.eval(reader);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void pause() {
        JOptionPane.showMessageDialog(null, "script paused ...");
    }

    public void sleep(int seconds) {
        try {
            Thread.sleep(1000 * seconds);
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
    }

    public synchronized void addDecorator(JRPWidgetDecorator decorator) {
        widgets.values().forEach(decorator::widgetAdded);
    }
}
