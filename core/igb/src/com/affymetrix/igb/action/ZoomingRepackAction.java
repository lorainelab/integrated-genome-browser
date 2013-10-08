/*  Copyright (c) 2012 Genentech, Inc.
 *
 *  Licensed under the Common Public License, Version 1.0 (the "License").
 *  A copy of the license must be included
 *  with any distribution of this source code.
 *  Distributions from Genentech, Inc. place this in the IGB_LICENSE.html file.
 *
 *  The license is also available at
 *  http://www.opensource.org/licenses/CPL
 */
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.tiers.TierLabelManager;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.Action;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 * Size the tiers vertically so that all glyphs in view horizontally will fit.
 * Unlike the "repack" operation this should not shrink or expand the tiers
 * to make it fit in the view.
 * i.e. You may need to scroll afterward.
 * Instead of changing the glyph sizes to fit the panel,
 * this keeps glyph sizes constant.
 * @author Eric Blossom
 */
public class ZoomingRepackAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static final ZoomingRepackAction ACTION = new ZoomingRepackAction();
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ZoomingRepackAction getAction() {
		return ACTION;
	}

	/**
	 * Create an action for the given tiered map.
	 */
	public ZoomingRepackAction() {
		super(BUNDLE.getString("optimizeAll"),"16x16/actions/optimize_stack_depth.png", "22x22/actions/optimize_stack_depth.png");
		putValue(Action.SHORT_DESCRIPTION, BUNDLE.getString("optimizeAllToolTip"));
	}

	/**
	 * Repacks tiers (tracks).
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		SeqMapView gviewer = getSeqMapView();
		TierLabelManager manager = getTierManager();
		List<TierLabelGlyph> theTiers = manager.getAllTierLabels();
		ViewI ourView = gviewer.getSeqMap().getView();
		for (TierLabelGlyph tl : theTiers) {
			TierGlyph t = (TierGlyph) tl.getInfo();
			int a = t.getSlotsNeeded(ourView);
			ITrackStyleExtended style = t.getAnnotStyle();
			TierGlyph.Direction d = t.getDirection();
			switch (d) {
				case REVERSE:
					style.setReverseMaxDepth(a);
					break;
				default:
				case FORWARD:
					style.setForwardMaxDepth(a);
					break;
			}
//			if (t.getTierType() == TierGlyph.TierType.GRAPH) {
//				for(GlyphI g : t.getChildren()){
//					if(!(g instanceof com.affymetrix.igb.shared.GraphGlyph))
//						continue;
//					
//					// So far this has only been tested with annotation depth graphs.
//					com.affymetrix.igb.shared.GraphGlyph gg
//							= (com.affymetrix.igb.shared.GraphGlyph) g;
//					gg.setVisibleMaxY(a);
//				}
//			}
		}
		// Now repack with the newly appointed maxima.
		boolean fullRepack = true, stretchMap = true, fireTierPositionChanged = false;
		gviewer.getSeqMap().repackTheTiers(fullRepack, stretchMap, fireTierPositionChanged);
		gviewer.redoEdgeMatching();
		// Full update doesn't seem to happen.
		// Or, rather, it happens when the user clicks on the map.
	}
}
