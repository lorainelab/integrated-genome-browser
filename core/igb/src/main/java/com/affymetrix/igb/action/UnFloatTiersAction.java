package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.igb.shared.GraphGlyph;
import static com.affymetrix.igb.shared.Selections.graphGlyphs;
import static com.affymetrix.igb.shared.Selections.graphStates;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import javax.swing.SwingUtilities;

public class UnFloatTiersAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1L;

    private static final UnFloatTiersAction ACTION = new UnFloatTiersAction();

    private UnFloatTiersAction.Enabler enabler = new UnFloatTiersAction.Enabler();

    private class Enabler implements SymSelectionListener {

        /**
         * React to selection changing by finding out if any floating tiers are
         * selected. This isn't doing it right yet. Need to listen to some other
         * selection?
         *
         * @param evt
         */
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
            ACTION.setEnabled(hasFloater);

        }

    }

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
        GenometryModel.getInstance().addSymSelectionListener(ACTION.enabler);
        ACTION.setEnabled(false);
    }

    public static UnFloatTiersAction getAction() {
        return ACTION;
    }

    public UnFloatTiersAction() {
        super("Anchor Floating Track",
                "16x16/actions/un-Float.png",
                "22x22/actions/un-Float.png");
        this.ordinal = -6006512;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        boolean something_changed = false;
        for (GraphGlyph glyph : graphGlyphs) {
            ITrackStyleExtended style = glyph.getAnnotStyle();
            boolean is_floating = style.isFloatTier();
            if (is_floating) {
                // figure out correct height
                Rectangle2D.Double coordbox = getSeqMapView().getFloaterGlyph().getUnfloatCoords(glyph, getTierMap().getView());
                style.setY(coordbox.y);
                style.setHeight(coordbox.height);

                style.setFloatTier(false);
                glyph.getGraphState().setShowLabel(false);

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
}
