package com.affymetrix.igb.action;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.util.List;

import javax.swing.SwingUtilities;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.action.SeqMapViewActionA;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.ViewModeGlyph;

public class FloatTiersAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;

	private static final FloatTiersAction ACTION = new FloatTiersAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static FloatTiersAction getAction() {
		return ACTION;
	}

	public FloatTiersAction() {
		super("Float", null, null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		boolean something_changed = false;
		List<? extends GlyphI> selectedTiers = getSeqMapView().getSelectedTiers();
		for (GlyphI tg : selectedTiers) {
			ViewModeGlyph gl = ((TierGlyph)tg).getViewModeGlyph();
			ITrackStyleExtended style = gl.getAnnotStyle();
			boolean is_floating = style.getFloatTier();
			if (!is_floating) {
				// figure out correct height
				Rectangle2D.Double coordbox = gl.getCoordBox();
				Rectangle pixbox = new Rectangle();
				getSeqMapView().getSeqMap().getView().transformToPixels(coordbox, pixbox);
				style.setY(pixbox.y);
				style.setHeight(pixbox.height);

				style.setFloatTier(true);
				something_changed = true;
			}
		}
		if (something_changed) {
			updateViewer();
		}
	}

	private void updateViewer() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				getSeqMapView().setAnnotatedSeq(GenometryModel.getGenometryModel().getSelectedSeq(), true, true);
			}
		});
	}
}
