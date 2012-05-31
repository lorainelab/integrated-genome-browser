
package com.affymetrix.igb.trackOperations;

import java.util.ArrayList;
import java.util.List;

import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.osgi.service.SeqMapViewI;
import com.affymetrix.igb.shared.TrackFunctionOperationA;

public class TrackTransformAction extends TrackFunctionOperationA {
	private static final long serialVersionUID = 1L;

	public TrackTransformAction(SeqMapViewI gviewer, Operator operator) {
		super(gviewer, operator);
	}

	@Override
	public void actionPerformed(java.awt.event.ActionEvent e) {
		super.actionPerformed(e);
		List<GlyphI> tiers;
		for (GlyphI glyph : gviewer.getSelectedTiers()) {
			tiers = new ArrayList<GlyphI>();
			tiers.add(glyph);
			addTier(tiers);
		}
	}
}
