package com.affymetrix.igb.external;

import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.igb.swing.JRPButton;
import com.affymetrix.igb.swing.JRPComboBox;
import com.affymetrix.igb.swing.JRPTextField;
import org.lorainelab.igb.igb.services.IgbService;
import java.awt.BorderLayout;
import java.awt.Font;
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

/**
 * View for ENSEMBL annotation
 *
 * @author Ido M. Tamir
 */
public class EnsemblView extends BrowserView {

    private static final long serialVersionUID = 1L;
    public static final String viewName = ExternalViewer.BUNDLE.getString("ensembl");
    private static final String ENSEMBLSETTINGS = "ensemblSettings";
    public static final String ENSEMBLSESSION = "ENSEMBL_WWW_SESSION";
    public static final String ENSEMBLWIDTH = "ENSEMBL_WIDTH";
    private ENSEMBLoader ensemblLoader = new ENSEMBLoader();

    /**
     *
     * @param selector selects foreground
     */
    public EnsemblView(JRPComboBox selector, IgbService igbService, UCSCViewAction ucscViewAction) {
        super(selector, igbService, ucscViewAction);
    }

    @Override
    public JDialog getViewHelper(Window window) {
        Loc loc = getLoc();
        String url = ensemblLoader.url(loc);
        String helper = !"".equals(url) ? "<p>" + ExternalViewer.BUNDLE.getString("ensemblUrlMessage") + ":<a href=" + url + ">" + url + "</a></p>" : "<p>" + ExternalViewer.BUNDLE.getString("ensemblUrlError") + "</p>";
        return new ENSEMBLHelper(window, ExternalViewer.BUNDLE.getString("ensemblCustomize"), helper);
    }

    @Override
    public void initializeCookies() {
        final Preferences ucscSettingsNode = PreferenceUtils.getTopNode().node(ENSEMBLSETTINGS);
        String userId = ucscSettingsNode.get(ENSEMBLSESSION, "");
        setCookie(ENSEMBLSESSION, userId);
    }

    @Override
    public Image getImage(Loc loc, int pixWidth) throws ImageUnavailableException {
        Map<String, String> cookies = new HashMap<>();
        cookies.put(ENSEMBLSESSION, getCookie(ENSEMBLSESSION));
        cookies.put(ENSEMBLWIDTH, Integer.toString(pixWidth));
        return ensemblLoader.getImage(loc, pixWidth, cookies);
    }

    @Override
    public String getViewName() {
        return viewName;
    }

    /**
     * Panel for ENSEMBL Settings: cookie selection
     *
     *
     *
     */
    class ENSEMBLHelper extends JDialog {

        private static final long serialVersionUID = 1L;
        private final JRPButton okButton = new JRPButton("ExternalView_okButton", ExternalViewer.BUNDLE.getString("submit"));
        private final JRPTextField userIdField = new JRPTextField("ExternalView_userId", getCookie(ENSEMBLSESSION), 50);
        private final Font font = okButton.getFont();

        public ENSEMBLHelper(Window window, String string, String helper) {
            super(window, string);
            CookieHandler.setDefault(null);

            this.setLayout(new BorderLayout());
            final JTextPane pane = new JTextPane();
            pane.setContentType("text/html");

            String text = "<h1>" + ExternalViewer.BUNDLE.getString("ensemblCookieHeader") + "</h1>";
            text += "<table><tr><td width='20'/><td>";
            text += "<font face='" + font.getFontName() + "'><p>" + ExternalViewer.BUNDLE.getString("ensemblCookieMessage1") + "</p>";
            text += "<ol><li><p>" + ExternalViewer.BUNDLE.getString("ensemblCookieMessage2") + "</p></li>";
            text += "<li>" + ExternalViewer.BUNDLE.getString("ensemblCookieMessage3") + "</li>";
            text += "<li>" + ExternalViewer.BUNDLE.getString("ensemblCookieMessage4") + "</li></ol></font>";
            text += "</td> <td width='20'/></tr> </table>";
            text += helper;
            pane.setText(text);
            pane.setEditable(false);
            final JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
            panel.add(Box.createHorizontalGlue());
            panel.add(Box.createHorizontalStrut(5));
            panel.add(new JLabel(ExternalViewer.BUNDLE.getString("ensemblCookie") + ": (" + ENSEMBLSESSION + "):"));
            panel.add(Box.createHorizontalStrut(5));
            panel.add(userIdField);
            panel.add(Box.createHorizontalStrut(5));
            panel.add(Box.createHorizontalGlue());
            panel.add(okButton);

            okButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    String userId = userIdField.getText();
                    setCookie(ENSEMBLSESSION, userId);
                    Preferences ucscSettingsNode = PreferenceUtils.getTopNode().node(ENSEMBLSESSION);
                    ucscSettingsNode.put(ENSEMBLSESSION, userId);
                    dispose();
                }
            });
            okButton.setToolTipText(ExternalViewer.BUNDLE.getString("okTT"));

            getContentPane().add("Center", pane);
            getContentPane().add("South", panel);
        }
    }
}
