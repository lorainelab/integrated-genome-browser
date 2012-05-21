package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.view.TrackView;
import java.awt.event.ActionEvent;

/**
 *
 * @author hiralv
 */
public class ChangeViewModeAction extends SeqMapViewActionA {

	private static final long serialVersionUID = 1L;
	private final MapViewGlyphFactoryI mode;

	public ChangeViewModeAction(MapViewGlyphFactoryI mode) {
		super(mode.getDisplayName(), null, null);
		this.mode = mode;
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		for(TierGlyph glyph : getTierManager().getSelectedTiers()){
			final RootSeqSymmetry rootSym = (RootSeqSymmetry) glyph.getInfo();
			final ITrackStyleExtended style = glyph.getAnnotStyle();
			final boolean isSeparate = style.getSeparate();
			
			if (isSeparate && !mode.supportsTwoTrack()) {
				style.setSeparate(false);
			}
			
			ITrackStyleExtended comboStyle = (glyph.getViewModeGlyph() instanceof AbstractGraphGlyph) ? ((AbstractGraphGlyph) glyph.getViewModeGlyph()).getGraphState().getComboStyle() : null;
			TrackView.getInstance().changeViewMode(getSeqMapView(), style, mode.getName(), rootSym, comboStyle);
		}

		refreshMap(false, false);
	}
}
