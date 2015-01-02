/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import static com.affymetrix.igb.IGBConstants.APP_NAME;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * Open a window showing information about Integrated Genome Browser.
 *
 * @author aloraine
 */
public class AboutIGBAction extends GenericAction {

    private static final long serialVersionUID = 1l;
    private static final AboutIGBAction ACTION = new AboutIGBAction();

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
        pane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (Desktop.isDesktopSupported()) {
                        try {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        } catch (IOException ex) {
                            Logger.getLogger(AboutIGBAction.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (URISyntaxException ex) {
                            Logger.getLogger(AboutIGBAction.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        });
        pane.setMargin(new Insets(10, 10, 10, 10));
        JFrame j = new JFrame("About Integrated Genome Browser");
        Component add = j.add(pane);
        j.setSize(new Dimension(500,500));
        j.setVisible(true);
    }
    
    /**
     * Create an HTML-formatted String containing information about IGB.
     * @return String text  
     */
    public String makeText() {
        String text = "<html><body bgcolor=\"FFFFFF\"><h1><center>About Integrated Genome Browser</center></h1>"
                + "<p>IGB (pronounced ig-bee) is a fast, flexible, desktop genome browser"
                + " first developed at <a href=\"http://www.affymetrix.com\">Affymetrix</a> for tiling arrays."
                + " IGB is now open source software supported by grants and donations."
                + " To find out more, visit <a href=\"http://www.bioviz.org/igb\">BioViz.org</a>.</p>"
                + "<p>If you use IGB in your research, please cite "
                + "Nicol JW, Helt GA, Blanchard SG Jr, Raja A, Loraine AE. "
                + "<a href=\"http://www.ncbi.nlm.nih.gov/pubmed/19654113\">The Integrated Genome "
                + " Browser: free software for distribution and exploration of"
                + "genome-scale datasets.</a> Bioinformatics. 2009 25(20):2730-1.</p>";
        String cache_root = com.affymetrix.genometryImpl.util.LocalUrlCacher.getCacheRoot();
        File cache_file = new File(cache_root);
        if (cache_file.exists()) {
            text = text + "<p>Cached data are stored in " 
                    + cache_file.getAbsolutePath() + ".</p>";
        }
        String data_dir = PreferenceUtils.getAppDataDirectory();
        if (data_dir != null) {
            File data_dir_f = new File(data_dir);
            text = text + "<p>Application data stored in "
                    + data_dir_f.getAbsolutePath() + ".</p>";
        }
        text = text + "</body></html>";
        return text;
    }

}
