package com.affymetrix.igb.external;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.swing.JRPButton;
import com.affymetrix.igb.swing.JRPComboBox;
import com.affymetrix.igb.swing.JRPTextField;
import com.affymetrix.igb.service.api.IGBService;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.CookieHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.Font;

/**
 * Subclass for UCSC view
 *
 * @author Ido M. Tamir
 */
public class UCSCView extends BrowserView {

    private static final long serialVersionUID = 1L;
    public static final String viewName = "UCSC";
    private static final String UCSCSETTINGSNODE = "ucscSettings";
    public static final String UCSCUSERID = "hguid";

    /**
     *
     * @param selector for selection foreground
     */
    public UCSCView(JRPComboBox selector, IGBService igbService, UCSCViewAction ucscViewAction_) {
        super(selector, igbService, ucscViewAction_);
    }

    @Override
    public JDialog getViewHelper(Window window) {
        return new UCSCHelper(window, ExternalViewer.BUNDLE.getString("UCSCCustomize"));
    }

    @Override
    public void initializeCookies() {
        final Preferences ucscSettingsNode = PreferenceUtils.getTopNode().node(UCSCSETTINGSNODE);
        String userId = ucscSettingsNode.get(UCSCUSERID, "");
        setCookie(UCSCUSERID, userId);
    }

    @Override
    public Image getImage(Loc loc, int pixWidth) throws ImageUnavailableException {
        Map<String, String> cookies = new HashMap<>();
        cookies.put(UCSCUSERID, getCookie(UCSCUSERID));
        return new UCSCLoader().getImage(loc, pixWidth, cookies);
    }

    @Override
    public String getViewName() {
        return viewName;
    }

    /**
     * Panel for UCSC Settings: hguid selection Shows the UCSC plot for the
     * current region.
     *
     *
     */
    public class UCSCHelper extends JDialog {

        private static final long serialVersionUID = 1L;
        private final JRPButton okButton = new JRPButton("UCSCView_okButton", ExternalViewer.BUNDLE.getString("submit"));
        private final JRPButton ucscInfo = new JRPButton("UCSCView_ucscInfo", ExternalViewer.BUNDLE.getString("UCSCinfo"));
        private final JRPTextField userIdField = new JRPTextField("UCSCView_userIdField", getCookie(UCSCUSERID), 15);
        private final Font font = okButton.getFont();

        public UCSCHelper(Window window, String string) {
            super(window, string);
            CookieHandler.setDefault(null);

            this.setLayout(new BorderLayout());
            final JTextPane pane = new JTextPane();
            pane.setContentType("text/html");

            String text = "<h1>Setting the UCSC user id</h1>";
            text += "<table><tr><td width='20'/><td>";
            text += "<font face='" + font.getFontName() + "'><p>If you have already customized the USCS view in a web browser, IGB can use those settings. Follow the instructions below to link your settings to IGB.</p>";
            text += "<ol><li><p>Obtain your user id by clicking on the \"UCSC info\" button.</p><p>Or open <a href=\"http://genome.ucsc.edu/cgi-bin/cartDump\">http://genome.ucsc.edu/cgi-bin/cartDump</a> in your browser</p></li>";
            text += "<li>Then scroll down in the opened window and copy the value of hguid into the \"UCSC user id\" field.</li>";
            text += "<li>Click the submit button.</li>";
            text += "<li>Your IGB UCSC View is now synchronized with your browser track configuration.</br>";
            text += "The settings in your browser now change the view.</li></ol></font>";
            text += "</td> <td width='20'/></tr> </table>";
            pane.setText(text);
            pane.setEditable(false);
            final JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
            panel.add(ucscInfo);
            panel.add(Box.createHorizontalGlue());
            panel.add(Box.createHorizontalStrut(5));
            panel.add(new JLabel(ExternalViewer.BUNDLE.getString("ucscUserId") + ":"));
            panel.add(Box.createHorizontalStrut(5));
            panel.add(userIdField);
            panel.add(Box.createHorizontalStrut(5));
            panel.add(Box.createHorizontalGlue());
            panel.add(okButton);

            okButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    String userId = userIdField.getText();
                    setCookie(UCSCUSERID, userId);
                    Preferences ucscSettingsNode = PreferenceUtils.getTopNode().node(UCSCSETTINGSNODE);
                    ucscSettingsNode.put(UCSCUSERID, userId);
                    dispose();
                }
            });
            okButton.setToolTipText(ExternalViewer.BUNDLE.getString("ucscUserIdTT"));
            ucscInfo.addActionListener(e -> GeneralUtils.browse("http://genome.ucsc.edu/cgi-bin/cartDump"));
            ucscInfo.setToolTipText("<html>" + ExternalViewer.BUNDLE.getString("ucscTT1") + "</br>" + ExternalViewer.BUNDLE.getString("ucscTT2") + "</br>" + ExternalViewer.BUNDLE.getString("ucscTT3") + "</html>");
            getContentPane().add("Center", pane);
            getContentPane().add("South", panel);
        }
    }
}
