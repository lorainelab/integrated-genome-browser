package com.affymetrix.igb.view;

import com.affymetrix.genoviz.bioviews.DragScrollMonitor;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import java.awt.event.MouseEvent;

/**
 *
 * @author hiralv
 */
public class SmartDragScrollMonitor extends DragScrollMonitor {

    private final SeqMapView smv;

    public SmartDragScrollMonitor(SeqMapView smv) {
        super();
        this.smv = smv;
    }

    @Override
    public void mousePressed(MouseEvent evt) {
        if (!(evt instanceof NeoMouseEvent)) {
            return;
        }
        NeoMouseEvent nevt = (NeoMouseEvent) evt;

        GlyphI topgl = null;
        if (!nevt.getItems().isEmpty()) {
            topgl = nevt.getItems().get(nevt.getItems().size() - 1);
        }

        //if(topgl != smv.getAxisGlyph() && topgl != smv.getSequnceGlyph()){
        heardEvent(evt);
        //}
    }
}
