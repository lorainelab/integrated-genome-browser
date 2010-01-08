package org.bioviz.protannot;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import javax.swing.AbstractAction;
import java.util.Properties;
import java.awt.event.*;
import com.affymetrix.genoviz.event.NeoMouseEvent;

/**
 * Asks ProtAnnot to open a browser window showing info
 * on the currently selected Glyph.
 */
final class OpenBrowserAction extends AbstractAction implements MouseListener {

    private final GenomeView view;
    private String url = null;

    /**
     * Create a OpenBrowserAction.
     */
    OpenBrowserAction(GenomeView view) {
        super("Open Browser");
        setEnabled(false);
        this.view = view;
        view.addMapListener(this);
    }

    /**
     * When this action is executed, open up a new
     * browser window showing info on the selected item.
     * @see     com.affymetrix.genometryImpl.util.GeneralUtils
     */
    public void actionPerformed(ActionEvent ae) {
        if (this.url != null) {
            GeneralUtils.browse(url);
        } else {
            Reporter.report("No URL associated with selected item",
                    null, false, false, true);
        }
    }

    /** MouseListener interface implementation */
    public void mouseClicked(MouseEvent e) {
    }

    /** MouseListener interface implementation */
    public void mouseEntered(MouseEvent e) {
    }

    /** MouseListener interface implementation */
    public void mouseExited(MouseEvent e) {
    }

    /** MouseListener interface implementation */
    public void mouseReleased(MouseEvent e) {
    }

    /**
     * Open link of selected Glyph in the browser
     * @see     com.affymetrix.genoviz.event.NeoMouseEvent
     */
    public void mousePressed(MouseEvent e) {
        if (!(e instanceof NeoMouseEvent)) {
            return;
        }
        Properties[] props = view.getProperties();
        if (props != null && props.length == 1) {
            this.url = build_url(props[0]);
        } else {
            this.url = null;
        }
        setEnabled(this.url != null);
    }

    /**
     * Builds url of selected glyphs
     * @param p Property of the selected glyph
     * @return  String of build url.
     */
    private static String build_url(Properties p) {
        String val = p.getProperty("URL");
        if (val != null) {
            return val;
        }
        val = p.getProperty("interpro_id");
        if (val != null) {
            return "http://www.ebi.ac.uk/interpro/IEntry?ac=" + val;
        }
        val = p.getProperty("exp_ngi");
        if (val != null) {
            if (val.startsWith("gi:")) {
                val = val.substring(3);
            }

            return "http://www.ncbi.nlm.nih.gov:80/entrez/query.fcgi?cmd=Retrieve&db=nucleotide&list_uids=" + val + "&dopt=GenBank";
        } else {
            return null;
        }
    }
}

