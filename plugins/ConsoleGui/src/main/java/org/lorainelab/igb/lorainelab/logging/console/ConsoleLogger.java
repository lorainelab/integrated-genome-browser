package org.lorainelab.igb.logging.console;

import aQute.bnd.annotation.component.Component;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(name = ConsoleLogger.COMPONENT_NAME, immediate = true, provide = ConsoleLogger.class)
public class ConsoleLogger extends JFrame {

    public static final String COMPONENT_NAME = "ConsoleLogger";
    private static final Logger logger = LoggerFactory.getLogger(ConsoleLogger.class);
    private static final int MAX_CONSOLE_LENGTH = 1000;
    private MigLayout layout;
    private JPanel panel;
    private JTextArea consoleTextArea;
    private JButton closeBtn;
    private JButton copyToClipboardBtn;
    private JScrollPane scrollPane;

    public ConsoleLogger() {
        initializeFrame();
        initializeLayout();
        initComponents();
        initializePanelComponents();
        this.add(panel);
        setupLogging();
    }

    private void initializeFrame() {
        setName("IGB Console");
        setTitle("IGB Console");
        setSize(800, 600);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
    }

    private void initializeLayout() {
        layout = new MigLayout("fill", "", "[grow 98,fill][grow 2,fill]");
    }

    private void initComponents() {
        panel = new JPanel(layout);
        consoleTextArea = new JTextArea();
        closeBtn = new JButton("Close");
        closeBtn.addActionListener(evt -> setVisible(false));
        copyToClipboardBtn = new JButton("Copy To Clipboard");
        copyToClipboardBtn.addActionListener(this::copyToClipboardBtnActionPerformed);
        scrollPane = new JScrollPane();
        scrollPane.setViewportView(consoleTextArea);
    }

    private void initializePanelComponents() {
        panel.add(scrollPane, "grow, wrap");
        panel.add(copyToClipboardBtn, "bottom, right, split");
        panel.add(closeBtn, "bottom, right");
    }

    private void setupLogging() {
        try {
            consoleTextArea.getDocument().addDocumentListener(new ConsoleLogDocumentListener(MAX_CONSOLE_LENGTH));
            DefaultCaret caret = (DefaultCaret) consoleTextArea.getCaret();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
            final JTextAreaOutputStream tout = new JTextAreaOutputStream(consoleTextArea, System.out);
            System.setOut(new PrintStream(tout, false, "UTF-8"));
            System.setErr(new PrintStream(new JTextAreaOutputStream(consoleTextArea, System.err), false, "UTF-8"));
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

    private void copyToClipboardBtnActionPerformed(ActionEvent evt) {
        StringSelection stringSelection = new StringSelection(consoleTextArea.getText());
        Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
        clpbrd.setContents(stringSelection, null);
    }

    public void showConsole() {

        java.awt.EventQueue.invokeLater(() -> {
            setVisible(true);
        });

    }

    public static void main(String[] args) {
        ConsoleLogger c = new ConsoleLogger();
        c.setVisible(true);
    }

}
