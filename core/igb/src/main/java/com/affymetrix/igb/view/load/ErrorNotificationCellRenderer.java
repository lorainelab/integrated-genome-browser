package com.affymetrix.igb.view.load;

import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genoviz.swing.JTextButtonCellRenderer;
import java.awt.event.ActionEvent;
import java.util.logging.Level;
import javax.swing.Icon;

/**
 *
 * @author dcnorris
 */
public class ErrorNotificationCellRenderer extends JTextButtonCellRenderer {

    private static final long serialVersionUID = 1L;
    private final String title;
    private final String message;

    public ErrorNotificationCellRenderer(String title, String message, Icon icon) {
        super(icon);
        this.title = title;
        this.message = message;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ErrorHandler.errorPanel(title, message, Level.WARNING);
        fireEditingCanceled();
    }
}
