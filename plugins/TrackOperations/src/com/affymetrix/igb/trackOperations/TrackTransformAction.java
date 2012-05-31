
package com.affymetrix.igb.trackOperations;

import java.util.ArrayList;
import java.util.List;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.TrackFunctionOperationA;

public class TrackTransformAction extends TrackFunctionOperationA {
	private static final long serialVersionUID = 1L;

	public TrackTransformAction(IGBService igbService) {
		super(igbService.getSeqMapView(), null, TrackOperationsTab.BUNDLE.getString("goButton"));
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
