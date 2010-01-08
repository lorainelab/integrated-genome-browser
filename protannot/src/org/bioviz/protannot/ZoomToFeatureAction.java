package org.bioviz.protannot;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

/**
 * Asks ProtAnnot to center on the location of the 
 * currently selected Glyph.
 */
final class ZoomToFeatureAction extends AbstractAction implements MouseListener {

    private final GenomeView view;

    /**
     * Create a new ZoomToFeatureAction.
     * @param   view    
     * @see             GenomeView
     */
    ZoomToFeatureAction(GenomeView view) {
        super("Zoom to Selected");
        setEnabled(false);
        this.view = view;
        view.addMapListener(this);
    }

    /**
     * When this action is executed, tell our display
     * instance to zoom in on the selected item.
     * @param   ae
     */
    public void actionPerformed(ActionEvent ae) {
        this.view.zoomToSelection();
    }

    /** MouseListener interface implementation */
    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    /**
     * Action to be performed when mouse button is pressed
     * @param e
     */
    public void mousePressed(MouseEvent e) {
        if (!(e instanceof NeoMouseEvent)) {
            return;
        }
        List<GlyphI> selected = view.getSelected();
        setEnabled(selected != null && !selected.isEmpty());
    }
}
