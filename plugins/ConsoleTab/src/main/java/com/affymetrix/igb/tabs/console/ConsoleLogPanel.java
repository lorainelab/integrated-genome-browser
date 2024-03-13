package com.affymetrix.igb.tabs.console;

import ch.qos.logback.core.FileAppender;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.util.ErrorHandler;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import javax.swing.text.DefaultCaret;
import static org.lorainelab.igb.services.ServiceComponentNameReference.CONSOLE_TAB;
import org.lorainelab.igb.services.window.tabs.IgbTabPanelI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.logging.Level;
import javax.swing.BoxLayout;
import org.lorainelab.igb.services.IgbService;
import org.lorainelab.igb.services.window.tabs.IgbTabPanel;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
/**
 *
 * @author pruthakulkarni
 */
@Component(name = CONSOLE_TAB, service = IgbTabPanelI.class, immediate = true)
public class ConsoleLogPanel extends IgbTabPanel {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleLogPanel.class);
    private static final long serialVersionUID = 1L;
    private static final String IGB_LOGS_FILE_NAME = "igb_logs.txt";
    public static final java.util.ResourceBundle BUNDLE = java.util.ResourceBundle.getBundle("console");
    private static final int TAB_POSITION = 6;
    private static final int MAX_CONSOLE_LENGTH = 1000;
    // Variables declaration - do not modify  
    private javax.swing.JButton copyToClipboardBtn;
    private javax.swing.JButton viewLogs;
    private javax.swing.JButton clearTxtBtn;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea consoleTextArea;
    private javax.swing.JPanel panel;
    // End of variables declaration 
    private IgbService igbService;
    
    public ConsoleLogPanel() {
        super(BUNDLE.getString("consoleTab"), BUNDLE.getString("consoleTab"), "", false, TAB_POSITION);
        initComponents();
        initialiseConsoleLogs();
        initialiseFileLogs();
    }
    private void initComponents() {
        this.setLayout(new BorderLayout());
        
        consoleTextArea = new javax.swing.JTextArea();
        jScrollPane1 = new javax.swing.JScrollPane(consoleTextArea);
        this.add(jScrollPane1, BorderLayout.CENTER);
        copyToClipboardBtn = new javax.swing.JButton();
        viewLogs = new javax.swing.JButton();
        clearTxtBtn = new javax.swing.JButton();
        panel = new javax.swing.JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(copyToClipboardBtn);
        panel.add(viewLogs);
        panel.add(clearTxtBtn);
        
        copyToClipboardBtn.setText("Copy All To Clipboard");
        viewLogs.setText("View Logs");
        clearTxtBtn.setText("Clear");

        this.add(panel, BorderLayout.SOUTH);
        
        copyToClipboardBtn.addActionListener(this::copyToClipboardBtnActionPerformed);
        clearTxtBtn.addActionListener(this::clearTxtBtnActionPerformed);
        viewLogs.addActionListener((event)->{
            String logFile = PreferenceUtils.getAppDataDirectory()+File.separator+IGB_LOGS_FILE_NAME;
            try {
                Desktop.getDesktop().open(new File(logFile));
            } catch (UnsupportedOperationException ex) {
                ErrorHandler.errorPanel("Your platform does not support this action", String.format("Navigate to %s to view the logs", logFile), Level.WARNING);
            } catch (SecurityException ex ){
                ErrorHandler.errorPanel("Read access to the file denied");
            } catch (IOException | NullPointerException | IllegalArgumentException ex){
                ErrorHandler.errorPanel("Unable to open the file", "File doesn't exist / has no associated application to open the file", Level.WARNING);
            }
        });
        
    }
   
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    
    private void copyToClipboardBtnActionPerformed(ActionEvent evt) {
        StringSelection stringSelection = new StringSelection(consoleTextArea.getText());
        Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
        clpbrd.setContents(stringSelection, null);
    }
    
    private void clearTxtBtnActionPerformed(ActionEvent evt){
        consoleTextArea.setText(null);
    }


    private void initialiseConsoleLogs(){
        try {
            consoleTextArea.getDocument().addDocumentListener(new ConsoleLogDocListener(MAX_CONSOLE_LENGTH));
            DefaultCaret caret = (DefaultCaret) consoleTextArea.getCaret();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
            final JTxtAreaOutputStream tout = new JTxtAreaOutputStream(consoleTextArea, System.out);
            System.setOut(new PrintStream(tout, false, "UTF-8"));
            System.setErr(new PrintStream(new JTxtAreaOutputStream(consoleTextArea, System.err), false, "UTF-8"));
            final ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
            final LoggerContext loggerContext = rootLogger.getLoggerContext();
            OutputStreamAppender<ILoggingEvent> outputStreamAppender = new OutputStreamAppender<>();
            outputStreamAppender.setName("OutputStream Appender");
            outputStreamAppender.setContext(loggerContext);
            outputStreamAppender.setOutputStream(tout);
            outputStreamAppender.start();
            rootLogger.addAppender(outputStreamAppender);
        } catch (UnsupportedEncodingException ex) {
            logger.error("Error setting up gui console logger", ex);
        }
    }
    private void initialiseFileLogs() {
        final ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        final LoggerContext loggerContext = rootLogger.getLoggerContext();
        PatternLayoutEncoder ple = new PatternLayoutEncoder();
        ple.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
        ple.setContext(loggerContext);
        ple.start();
        FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        fileAppender.setName("File Appender");
        String logFile = PreferenceUtils.getAppDataDirectory()+File.separator+IGB_LOGS_FILE_NAME;
        fileAppender.setAppend(false);
        fileAppender.setFile(logFile);
        fileAppender.setContext(loggerContext);
        fileAppender.setEncoder(ple);
        fileAppender.start();
        rootLogger.addAppender(fileAppender);
    }
}
