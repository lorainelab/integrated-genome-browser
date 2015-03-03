package com.affymetrix.igb.action;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.event.SymSelectionEvent;
import com.affymetrix.genometry.event.SymSelectionListener;
import com.affymetrix.genometry.style.GraphState;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import static com.affymetrix.igb.shared.Selections.graphGlyphs;
import static com.affymetrix.igb.shared.Selections.graphStates;
import com.affymetrix.igb.view.SeqMapView;
import com.lorainelab.igb.genoviz.extensions.GraphGlyph;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import javax.swing.SwingUtilities;

public class FloatTiersAction extends SeqMapViewActionA {

    public static final String COMPONENT_NAME = "FloatTiersAction";
    private static final long serialVersionUID = 1L;

    private static final FloatTiersAction ACTION = new FloatTiersAction();

    private Enabler enabler = new Enabler();

    private class Enabler implements SymSelectionListener {

        @Override
        public void symSelectionChanged(SymSelectionEvent evt) {
            // Only pay attention to selections from the main SeqMapView or its map.
            // Ignore the splice view as well as events coming from this class itself.
            Object src = evt.getSource();
            SeqMapView gviewer = getSeqMapView();
            if (!(src == gviewer || src == gviewer.getSeqMap())) {
                return;
            }

            boolean hasFloater = false;
            boolean hasAnchored = false;
            for (GraphState gs : graphStates) {
                boolean floating = gs.getTierStyle().isFloatTier();
                hasFloater |= floating;
                hasAnchored |= !floating;
            }
            ACTION.setEnabled(hasAnchored);

        }

    }

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
        GenometryModel.getInstance().addSymSelectionListener(ACTION.enabler);
        ACTION.setEnabled(false);
    }

    public static FloatTiersAction getAction() {
        return ACTION;
    }

    public FloatTiersAction() {
        super("Float Track", "16x16/actions/Float.png", "22x22/actions/Float.png");
        this.ordinal = -6006511;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        boolean something_changed = false;
        for (GraphGlyph glyph : graphGlyphs) {
            ITrackStyleExtended style = glyph.getAnnotStyle();
            boolean is_floating = style.isFloatTier();
            if (!is_floating) {
                // figure out correct height
                Rectangle2D.Double coordbox = getSeqMapView().getFloaterGlyph().getFloatCoords(glyph, getTierMap().getView());
                style.setY(coordbox.y);
                style.setHeight(coordbox.height);

                style.setFloatTier(true);
                glyph.getGraphState().setShowLabel(true);

                something_changed = true;
            }

        }
        if (something_changed) {
            updateViewer();
        }
    }

    private void updateViewer() {
        SwingUtilities.invokeLater(() -> {
            SeqMapView v = getSeqMapView();
            GenometryModel m = GenometryModel.getInstance();
            BioSeq s = m.getSelectedSeq();
            v.setAnnotatedSeq(s, true, true);
            v.getSeqMap().packTiers(false, false, true); //Fire event for sort in data management table
            v.postSelections(); // to disable partner.
        });
    }
    
    @Override
    public boolean isToolbarAction() {
        return false;
    }
}
