
package com.affymetrix.igb.tiers;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.igb.shared.ScrollableViewModeGlyph;
import com.affymetrix.igb.shared.TierGlyph;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 *
 * @author hiralv
 */
public class GlyphScroller implements MouseWheelListener, SymSelectionListener{
	AffyLabelledTierMap map;
	ScrollableViewModeGlyph ag;
	
	public GlyphScroller(AffyLabelledTierMap map){
		this.map = map;
	}
	
	public void startscroll(TierGlyph tier){
		if(!(tier.getViewModeGlyph() instanceof ScrollableViewModeGlyph)){
			return;
		}
		
		ag = (ScrollableViewModeGlyph)tier.getViewModeGlyph();
		
		// Flushing, just in case
		map.getLabelMap().getNeoCanvas().removeMouseWheelListener(this);
		GenometryModel.getGenometryModel().removeSymSelectionListener(this);
		
		// Now add listeners
		map.getLabelMap().getNeoCanvas().addMouseWheelListener(this);
		GenometryModel.getGenometryModel().addSymSelectionListener(this);
	}
	
	private void stopscroll(){
		map.getLabelMap().getNeoCanvas().removeMouseWheelListener(this);
		GenometryModel.getGenometryModel().removeSymSelectionListener(this);
		ag = null;
	}
	
	
	public void mouseWheelMoved(MouseWheelEvent e) {
		ag.setOffset(ag.getOffset() + (e.getWheelRotation() * e.getScrollAmount()), map.getView());
		map.updateWidget(true);
	}

	public void symSelectionChanged(SymSelectionEvent evt) {
		stopscroll();
	}
}
