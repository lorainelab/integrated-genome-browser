package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.swing.JRPButton;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public abstract class HelpActionA extends GenericAction {

    private static final long serialVersionUID = 1L;
    private static final String HELP_WINDOW_NAME = " Help Window";
    private static final Logger ourLogger
            = Logger.getLogger(HelpActionA.class.getPackage().getName());

    public HelpActionA(String text, String tooltip, String iconPath, String largeIconPath, int mnemonic, Object extraInfo, boolean popup) {
        super(text, tooltip, iconPath, largeIconPath, mnemonic, extraInfo, popup);
    }

    protected void showHelp(final JPanel parentPanel, String s) {
        JEditorPane text = new JEditorPane();
        text.setContentType("text/html");
        text.setText(s);
        text.setEditable(false);
        text.setCaretPosition(0); // force a scroll to the top
        JScrollPane scroller = new JScrollPane(text);
        scroller.setPreferredSize(new java.awt.Dimension(300, 400));

        JFrame frameAncestor = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, parentPanel);
        final JDialog dialog = new JDialog(frameAncestor, BUNDLE.getString("helpMenu"), true);
        dialog.getContentPane().add(scroller, "Center");
        Action close_action = new GenericAction("OK", null, null) {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                super.actionPerformed(e);
                dialog.dispose();
            }
        };
        JRPButton close = new JRPButton("HelpActionA_close", close_action);
        Box button_box = new Box(BoxLayout.X_AXIS);
        button_box.add(Box.createHorizontalGlue());
        button_box.add(close);
        button_box.add(Box.createHorizontalGlue());
        dialog.getContentPane().add(button_box, "South");
        dialog.pack();
        dialog.setLocationRelativeTo(parentPanel);
        Rectangle pos = PreferenceUtils.retrieveWindowLocation(parentPanel.getClass().getName() + HELP_WINDOW_NAME, new Rectangle(400, 400));
        if (pos != null) {
            PreferenceUtils.setWindowSize(dialog, pos);
        }
        dialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent evt) {
                PreferenceUtils.saveWindowLocation(dialog, parentPanel.getClass().getName() + HELP_WINDOW_NAME);
                dialog.dispose();
            }
        });

        dialog.setVisible(true);
    }

    /**
     * Gives help text explaining the function of this panel. If no help is
     * available, should return null rather than an empty String. The help text
     * should describe what effect changes in the panel will have, how to make
     * the changes (if it isn't obvious), and whether the changes are expected
     * to take effect immediately or only after a re-start.
     *
     * @param parentPanel the parent of the panel to display text for
     * @param panel the panel to display text for
     */
    protected void showHelpForPanel(JPanel parentPanel, JPanel panel) {
        StringBuilder builder = new StringBuilder();
        char buffer[] = new char[4096];
        InputStream stream = null;
        Reader reader = null;

        try {
            stream = this.getClass().getResourceAsStream(
                    "/help/" + panel.getClass().getName() + ".html");
            reader = new InputStreamReader(stream, "UTF-8");

            for (int read = reader.read(buffer, 0, buffer.length); read > 0; read = reader
                    .read(buffer, 0, buffer.length)) {
                builder.append(buffer);
            }
            String text = builder.toString();
            showHelp(parentPanel, text);

        } catch (UnsupportedEncodingException ex) {
            ourLogger.log(Level.SEVERE,
                    "UTF-8 is not a supported encoding?!", ex);
        } catch (IOException ex) {
            ourLogger.log(Level.SEVERE,
                    "Unable to load help file for "
                    + this.getClass().getName(), ex);
        } finally {
            GeneralUtils.safeClose(reader);
            GeneralUtils.safeClose(stream);
        }
    }
}
