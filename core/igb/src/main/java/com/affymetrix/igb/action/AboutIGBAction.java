package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import static com.affymetrix.igb.IGBConstants.APP_NAME;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
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
public class AboutIGBAction extends GenericAction {

    private static final long serialVersionUID = 1l;
    private static final AboutIGBAction ACTION = new AboutIGBAction();
    private static final Logger logger = LoggerFactory.getLogger(AboutIGBAction.class);

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
        j.setSize(new Dimension(500, 500));
        j.setVisible(true);
    }

    /**
     * Create an HTML-formatted String containing information about IGB.
     *
     * @return String text
     */
    public String makeText() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<head>");
        sb.append("</head>");
        sb.append("<body bgcolor=\"FFFFFF\">");
        sb.append("<h1><center>About Integrated Genome Browser</center></h1>");
        sb.append("<p>");
        sb.append("IGB (pronounced ig-bee) is a fast, flexible, desktop genome browser"
                + " first developed at <a href=\"http://www.affymetrix.com\">Affymetrix</a> for tiling arrays."
                + " IGB is now open source software supported by grants and donations."
                + " To find out more, visit <a href=\"http://www.bioviz.org/igb\">BioViz.org</a>."
        );
        sb.append("</p>");
        sb.append("<p>");
        sb.append("If you use IGB in your research, please cite "
                + "Nicol JW, Helt GA, Blanchard SG Jr, Raja A, Loraine AE. "
                + "<a href=\"http://www.ncbi.nlm.nih.gov/pubmed/19654113\">The Integrated Genome "
                + " Browser: free software for distribution and exploration of"
                + "genome-scale datasets.</a> Bioinformatics. 2009 25(20):2730-1."
        );
        sb.append("</p>");

        String cache_root = com.affymetrix.genometryImpl.util.LocalUrlCacher.getCacheRoot();
        File cache_file = new File(cache_root);
        if (cache_file.exists()) {
            sb.append("<p>");
            sb.append("Cached data are stored in ").append(cache_file.getAbsolutePath());
            sb.append("</p>");
        }
        String data_dir = PreferenceUtils.getAppDataDirectory();
        if (data_dir != null) {
            File data_dir_f = new File(data_dir);
            sb.append("<p>");
            sb.append("Application data stored in ").append(data_dir_f.getAbsolutePath());
            sb.append("</p>");
        }
        sb.append("</body>");
        sb.append("</html>");
        return sb.toString();
    }

}
