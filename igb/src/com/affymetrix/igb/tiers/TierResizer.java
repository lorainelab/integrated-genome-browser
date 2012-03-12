package com.affymetrix.igb.tiers;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.widget.NeoWidget;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.event.MouseInputAdapter;

/**
 * A class to handle generic resizing the tiers on a labeled tiered map.
 * Tiers are resized by adjusting the border between their labels.
 * So far this is only for vertical resizing
 * and is only used by the TierLabelManager.
 * @author blossome
 */
public class TierResizer extends MouseInputAdapter {

	private static final double RESIZE_THRESHHOLD = 4.0;
	private AffyLabelledTierMap tiermap;
	private TierLabelGlyph lowerGl;
	private TierLabelGlyph upperGl;
	private SeqMapView gviewer = null;
	private double start;
	private double ourFloor, ourCeiling;
	private List<TierLabelGlyph> fixedInterior;
	
	/**
	 * Construct a resizer for the given tiered map.
	 */
	public TierResizer(AffyLabelledTierMap theDataTiers) {
		assert null != theDataTiers;
		this.tiermap = theDataTiers;
	}		

	/**
	 * Determines the scope of resizing.
	 * Given a border between two tiers determine a list of tiers
	 * that will be affected by the resize.
	 * Note that the returned list is of contiguous tiers.
	 *
	 * @param theFirst points to tier just above the border being dragged.
	 * @param theLast points to the tier just below the border being dragged.
	 * @param theList of tiers that might be resized.
	 * @return a maximal (possibly empty) section of theList
	 *         such that the tiers in this list can be resized
	 *         and none of the others can.
	 */
	private List<TierLabelGlyph> pertinentTiers(int theFirst, int theLast,
			List<TierLabelGlyph> theList) {
		assert 0 <= theFirst;
		assert theFirst < theLast;
		assert theLast < theList.size();
		int top = theLast, limit = theLast;
		for (int i = theFirst; 0 <= i; i--) {
			TierLabelGlyph g = theList.get(i);
			if (g.isManuallyResizable()) {
				top = i;
				break;
			}
		}
		for (int i = theLast; i < theList.size(); i++) {
			TierLabelGlyph g = theList.get(i);
			if (g.isManuallyResizable()) {
				limit = i + 1;
				break;
			}
		}
		return theList.subList(top, limit);
	}

	/**
	 * Establish some context and boundaries for the drag.
	 * @param theRegion is a list of contiguous tiers affected by the resize.
	 * @param nevt is the event starting the drag.
	 */
	public void startDrag(List<TierLabelGlyph> theRegion, NeoMouseEvent nevt) {
		this.upperGl = theRegion.get(0);
		this.lowerGl = theRegion.get(theRegion.size()-1);
		
		start = nevt.getCoordY();
	
		// These minimum heights are in coord space.
		// Shouldn't we be dealing in pixels?
		ourCeiling = this.upperGl.getCoordBox().getY()
				+ this.upperGl.getMinimumHeight();
		java.awt.geom.Rectangle2D.Double box = this.lowerGl.getCoordBox();
		ourFloor = box.getY() + box.getHeight() - this.lowerGl.getMinimumHeight();
		
		this.fixedInterior = theRegion.subList(1, theRegion.size()-1);
		for (TierLabelGlyph g: this.fixedInterior) {
			java.awt.geom.Rectangle2D.Double b = g.getCoordBox();
			if (b.getY() <= start) {
				ourCeiling += b.getHeight();
			}
			if (start <= b.getY()) {
				ourFloor -= b.getHeight();
			}
		}	
	}

	@Override
	public void mouseMoved(MouseEvent theEvent) {
		if (theEvent instanceof NeoMouseEvent) {
			NeoMouseEvent nevt = (NeoMouseEvent) theEvent;
			Object src = theEvent.getSource();
			if (src instanceof AffyTieredMap) {
				AffyTieredMap m
						= Application.getSingleton().getMapView().getSeqMap();
				assert m != (AffyTieredMap) src; // This seems odd.
				if (atResizeTop(nevt)) {
					m.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
				} 
				else if (atResizeBottom(nevt)) {
					m.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
				}
			}
			else {
				assert false : "not from a tiered map?";
			}
		}
		else {
			assert false: "not a neo mouse event?";
		}
	}

	@Override
	public void mousePressed(MouseEvent evt) {
		if (evt instanceof NeoMouseEvent) {
			if (null == this.gviewer) {
				this.gviewer = Application.getSingleton().getMapView();
				assert null != this.gviewer;
			}
			NeoMouseEvent nevt = (NeoMouseEvent) evt;
			List<GlyphI> selected_glyphs = nevt.getItems();
			GlyphI topgl = null;
			if (!selected_glyphs.isEmpty()) {
				topgl = selected_glyphs.get(selected_glyphs.size() - 1);
			}
			List<TierLabelGlyph> orderedGlyphs = tiermap.getOrderedTierLabels();
			int index = orderedGlyphs.indexOf(topgl);
			List<TierLabelGlyph> resizeRegion = null;
			if (atResizeTop(nevt)) {
				resizeRegion = pertinentTiers(index - 1, index, orderedGlyphs);
			} else if (atResizeBottom(nevt)) {
				resizeRegion = pertinentTiers(index, index + 1, orderedGlyphs);
			}
			if (null != resizeRegion && 1 < resizeRegion.size()) {
				startDrag(resizeRegion, nevt);
			}
		}
	}

	@Override
	public void mouseDragged(MouseEvent evt) {
		if (evt instanceof NeoMouseEvent) {
			neoMouseDragged((NeoMouseEvent) evt);
		}
	}
	
	/**
	 * Adjust the tiers on either side of the mouse pointer.
	 * This adjustment is going on in scene space rather than pixel space.
	 * That doesn't seem quite right. - elb
	 * @param evt is the drag event.
	 */
	private void neoMouseDragged(NeoMouseEvent nevt) {
		double delta = nevt.getCoordY() - start;

		if (this.upperGl != null && null != this.lowerGl) {
			if (ourCeiling < nevt.getCoordY() && nevt.getCoordY() < ourFloor) {
				double y = this.upperGl.getCoordBox().getY();
				double height = this.upperGl.getCoordBox().getHeight() + delta;
				this.upperGl.resizeHeight(y, height);
				
				// Move the fixed height glyphs in the middle,
				// assuming that the list is sorted top to bottom.
				height = this.upperGl.getCoordBox().getHeight();
				y = this.upperGl.getCoordBox().getY() + height;
				for (TierLabelGlyph g: this.fixedInterior) {
					g.resizeHeight(y, g.getCoordBox().getHeight());
					y += g.getCoordBox().getHeight();
				}
				
				y = this.lowerGl.getCoordBox().getY() + delta;
				height = this.lowerGl.getCoordBox().getHeight() - delta;
				this.lowerGl.resizeHeight(y, height);
				this.gviewer.getSeqMap().updateWidget();
			}
			else { // then we're out of bounds.
				// Ignore it.
				//System.err.println("TierResizer: Out of bounds.");
			}
		}
		else {
//			System.err.println("TierResizer: No upper glyph or no lower glyph.");
		}

		start = nevt.getCoordY();
	}

	/**
	 * Resize the data tiers to match the resized labels.
	 * The data tiers are also repacked if necessary.
	 */
	@Override
	public void mouseReleased(MouseEvent evt) {
		
		boolean needRepacking = (this.upperGl != null && this.lowerGl != null);
		
		if (this.upperGl != null) {
			com.affymetrix.igb.shared.TierGlyph gl = this.upperGl.getReferenceTier();
			gl.setPreferredHeight(
					this.upperGl.getCoordBox().getHeight(),
					this.gviewer.getSeqMap().getView()
					);
		}
		
		if (this.lowerGl != null) {
			com.affymetrix.igb.shared.TierGlyph gl = this.lowerGl.getReferenceTier();
			gl.setPreferredHeight(
					this.lowerGl.getCoordBox().getHeight(),
					this.gviewer.getSeqMap().getView()
					);
		}
		
		if (needRepacking) {
			
			// This is pretty good now. Tiers jump just a bit after resizing.
			// Mostly in one direction. Maybe can get to the bottom of this.
			// The border width of 2 pixels looks suspicious both here
			// and when moving the lower split pane.
			// - elb
			com.affymetrix.igb.tiers.AffyTieredMap m = this.gviewer.getSeqMap();
			com.affymetrix.igb.tiers.AffyLabelledTierMap lm
					= (com.affymetrix.igb.tiers.AffyLabelledTierMap) m;
			boolean full_repack = true, stretch_vertically = true, manual = false;
			lm.repackTheTiers(full_repack, stretch_vertically, manual);
			//lm.repackTiersToLabels();
			// The above repack (either one I think) changes (enlarges) the tier map's bounds.
			// This probably affects the tiers' spacing. - elb 2012-02-21
			
			// Vanilla repack seems to have worse symptoms.
			//m.repack();
			//m.packTiers(true, false, false, true);
			
			// This was also commented out.
			// From the name "kludgeRepackingTheTiers" 
			// it looks like someone tried a specialized repack.
			// Don't know who or how far they got.
			//com.affymetrix.igb.tiers.AffyTieredMap m = this.gviewer.getSeqMap();
			//if (m instanceof com.affymetrix.igb.tiers.AffyLabelledTierMap) {
			//	com.affymetrix.igb.tiers.AffyLabelledTierMap lm
			//			= (com.affymetrix.igb.tiers.AffyLabelledTierMap) m;
			//	lm.kludgeRepackingTheTiers(needRepacking, needRepacking, needRepacking);
			//}
			// The above may not have worked,
			// but it would seem we need something to repack the tiers
			// based on the label glyphs' height and position.
			// Would have thought that's what the last paramater in repackTheTiers was for.

		}

		this.upperGl = null; // helps with garbage collection
		this.lowerGl = null; // helps with garbage collection
	}

	private boolean atResizeTop(NeoMouseEvent nevt) {
		if (nevt == null || nevt.getItems().isEmpty()) {
			return false;
		}
		GlyphI topgl = nevt.getItems().get(nevt.getItems().size() - 1);
		List<TierLabelGlyph> orderedGlyphs = tiermap.getOrderedTierLabels();
		int index = orderedGlyphs.indexOf(topgl);
		LinearTransform trans = ((NeoWidget) nevt.getSource()).getView().getTransform();
		double threshhold = RESIZE_THRESHHOLD / trans.getScaleY();
		return (0 < index
				&&
			(nevt.getCoordY() - topgl.getCoordBox().getY() < threshhold)
				&&
			(((TierLabelGlyph)topgl).isManuallyResizable()
				|| orderedGlyphs.get(index - 1).isManuallyResizable()));
	}

	private boolean atResizeBottom(NeoMouseEvent nevt) {
		if (nevt == null || nevt.getItems().isEmpty()) {
			return false;
		}
		GlyphI topgl = nevt.getItems().get(nevt.getItems().size() - 1);
		List<TierLabelGlyph> orderedGlyphs = tiermap.getOrderedTierLabels();
		int index = orderedGlyphs.indexOf(topgl);
		LinearTransform trans = ((NeoWidget) nevt.getSource()).getView().getTransform();
		double threshhold = RESIZE_THRESHHOLD / trans.getScaleY();
		return (index < orderedGlyphs.size() - 1
				&&
			(topgl.getCoordBox().getY() + topgl.getCoordBox().getHeight() - nevt.getCoordY() < threshhold)
				&&
			(((TierLabelGlyph)topgl).isManuallyResizable()
				|| orderedGlyphs.get(index + 1).isManuallyResizable()));
	}

}