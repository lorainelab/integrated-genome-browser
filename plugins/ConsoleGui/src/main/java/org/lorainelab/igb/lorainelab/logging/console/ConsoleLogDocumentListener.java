package org.lorainelab.igb.logging.console;

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleLogDocumentListener implements DocumentListener {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleLogDocumentListener.class);

    private final int maximumLines;

    public ConsoleLogDocumentListener(int maximumLines) {
        this.maximumLines = maximumLines;
    }

    @Override
    public void insertUpdate(final DocumentEvent e) {
        SwingUtilities.invokeLater(() -> removeLines(e));
    }

    private void removeLines(DocumentEvent e) {
        Document document = e.getDocument();
        Element root = document.getDefaultRootElement();

        while (root.getElementCount() > maximumLines) {
            removeFromStart(document, root);
        }
    }

    private void removeFromStart(Document document, Element root) {
        Element line = root.getElement(0);
        int end = line.getEndOffset();
        try {
            document.remove(0, end);
        } catch (BadLocationException ex) {
            logger.error("Error trimming console logger text", ex);
        }

    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        //do nothing
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        //do nothing
    }

}
