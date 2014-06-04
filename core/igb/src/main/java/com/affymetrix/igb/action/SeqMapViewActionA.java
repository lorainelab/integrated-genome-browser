package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackListProvider;
import com.affymetrix.igb.swing.ScriptManager;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.TierLabelManager;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Superclass of all IGB actions that must refer to a {@link SeqMapView}. These
 * classes are automatically added to a static hash map.
 */
public abstract class SeqMapViewActionA extends GenericAction {

    private static final long serialVersionUID = 1L;
//	protected static final Map<String, SeqMapViewActionA> ACTION_MAP = new HashMap<String, SeqMapViewActionA>();
    protected String id;
    private SeqMapView gviewer;
    private TierLabelManager handler;

    public SeqMapViewActionA(String text, String tooltip, String iconPath, String largeIconPath, int mnemonic) {
        super(text, tooltip, iconPath, largeIconPath, mnemonic);
    }

    public SeqMapViewActionA(String text, String tooltip, String iconPath, String largeIconPath, int mnemonic, Object extraInfo, boolean popup) {
        super(text, tooltip, iconPath, largeIconPath, mnemonic, extraInfo, popup);
    }

    public SeqMapViewActionA(String text, String iconPath, String largeIconPath) {
        super(text, iconPath, largeIconPath);
    }

    public SeqMapViewActionA(String text, int mnemonic) {
        super(text, mnemonic);
    }

    protected SeqMapView getSeqMapView() {
        if (gviewer == null) {
            if (id == null) {
                gviewer = Application.getSingleton().getMapView();
            } else {
                gviewer = (SeqMapView) ScriptManager.getInstance().getWidget(id);
            }
        }
        return gviewer;
    }

    protected TierLabelManager getTierManager() {
        if (handler == null) {
            handler = getSeqMapView().getTierManager();
        }
        return handler;
    }

    protected AffyLabelledTierMap getTierMap() {
        return ((AffyLabelledTierMap) getSeqMapView().getSeqMap());
    }

    protected List<TierGlyph> getTrackList(ActionEvent e) {
        List<TierGlyph> trackList;
        Object src = e.getSource();
        if (src instanceof TrackListProvider) {
            trackList = ((TrackListProvider) src).getTrackList();
        } else {
            trackList = getTierManager().getSelectedTiers();
        }
        return trackList;
    }

    protected AffyTieredMap getLabelMap() {
        return getTierMap().getLabelMap();
    }

    protected void refreshMap(boolean stretch_vertically, boolean stretch_horizonatally) {
        if (getSeqMapView() != null) {
			// if an AnnotatedSeqViewer is being used, ask it to update itself.
            // later this can be made more specific to just update the tiers that changed
            boolean preserve_view_x = !stretch_vertically;
            boolean preserve_view_y = !stretch_horizonatally;
            gviewer.updatePanel(preserve_view_x, preserve_view_y);
        } else {
            // if no AnnotatedSeqViewer (as in simple test programs), update the tiermap itself.
            getTierManager().repackTheTiers(false, stretch_vertically);
        }
    }
}
