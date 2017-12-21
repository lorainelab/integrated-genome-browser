package com.affymetrix.igb.action;

import com.affymetrix.common.CommonUtils;
import static com.affymetrix.common.CommonUtils.APP_NAME;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.common.PreferenceUtils;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.lorainelab.igb.services.window.HtmlHelpProvider;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.event.HyperlinkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Open a window showing information about Integrated Genome Browser.
 *
 * @author aloraine
 */
public class AboutIGBAction extends GenericAction implements HtmlHelpProvider {

    private static final long serialVersionUID = 1L;
    private static final AboutIGBAction ACTION = new AboutIGBAction();
    private static final Logger logger = LoggerFactory.getLogger(AboutIGBAction.class);
    private static final String CACHE_COMMENT = "<!-- cacheInfo -->";
    private static final String DATA_DIR_COMMENT = "<!-- dataDir -->";
    private static final String VERSION_COMMENT = "<!-- igbVersion -->";
    private static final String PERIOD = ".";

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static AboutIGBAction getAction() {
        return ACTION;
    }

    private AboutIGBAction() {
        super(MessageFormat.format(BUNDLE.getString("about"), APP_NAME), null,
                "16x16/actions/about_igb.png",
                "22x22/actions/about_igb.png",
                KeyEvent.VK_A, null, false);
        this.ordinal = 100;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        String text = makeText();
        final JEditorPane pane = new JEditorPane();
        pane.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));
        pane.setEditable(false);
        pane.setText(text);
        pane.addHyperlinkListener(e1 -> {
            if (e1.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(e1.getURL().toURI());
                    } catch (IOException | URISyntaxException ex) {
                        logger.error("Error navigating to hyperlink in about IGB window", ex);
                    }
                }
            }
        });
        pane.setMargin(new Insets(10, 10, 10, 10));
        JFrame j = new JFrame("About Integrated Genome Browser");
        j.add(pane);
        j.setSize(new Dimension(1000, 500));
        j.setVisible(true);
    }

    @Override
    public String getHelpHtml() {
        String htmlText = null;
        try {
            htmlText = Resources.toString(AboutIGBAction.class.getResource("/help/org.lorainelab.igb.AboutIGB.html"), Charsets.UTF_8);
        } catch (IOException ex) {
            logger.error("Help file not found ", ex);
        }
        return htmlText;
    }

    /**
     * Create an HTML-formatted String containing information about IGB.
     *
     * @return String text
     */
    public String makeText() {
        StringBuilder sb = new StringBuilder();
        sb = new StringBuilder(getHelpHtml());

        String cache_root = com.affymetrix.genometry.util.LocalUrlCacher.getCacheRoot();
        File cache_file = new File(cache_root);
        if (cache_file.exists()) {
            StringBuilder cacheInfo = new StringBuilder();
            cacheInfo.append("<p>");
            cacheInfo.append("Downloaded, cached genome data are stored in ").append(cache_file.getAbsolutePath()).append(PERIOD);
            cacheInfo.append("</p>");
            replace(CACHE_COMMENT, cacheInfo.toString(), sb);
        }
        String data_dir = PreferenceUtils.getAppDataDirectory();
        if (data_dir != null) {
            File data_dir_f = new File(data_dir);
            StringBuilder dataDirInfo = new StringBuilder();
            dataDirInfo.append("<p>");
            dataDirInfo.append("Application data and OSGi bundle cache are stored in ").append(data_dir_f.getAbsolutePath()).append(PERIOD);
            dataDirInfo.append("</p>");
            replace(DATA_DIR_COMMENT, dataDirInfo.toString(), sb);
        }
        replace(VERSION_COMMENT, CommonUtils.getInstance().getAppVersion(), sb);
        return sb.toString();
    }

    private void replace(String origStr, String newStr, StringBuilder sb) {
        int index = sb.indexOf(origStr);
        sb.replace(index, index + origStr.length(), newStr);
    }

}
