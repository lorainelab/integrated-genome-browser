
package com.affymetrix.igb.tiers;

import com.affymetrix.igb.shared.ScrollableViewModeGlyph;
import com.affymetrix.igb.shared.TierGlyph;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author hiralv
 */
public class GlyphScroller implements MouseWheelListener, ListSelectionListener{
	private static final int SPEED = 10;
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
		map.removeListSelectionListener(this);
		
		// Now add listeners
		map.getLabelMap().getNeoCanvas().addMouseWheelListener(this);
		map.addListSelectionListener(this);
	}
	
	private void stopscroll(){
		map.getLabelMap().getNeoCanvas().removeMouseWheelListener(this);
		map.removeListSelectionListener(this);
		
		// Helps with garbage collection
		ag = null;
		map = null;
	}
	
	
	public void mouseWheelMoved(MouseWheelEvent e) {
		ag.setOffset(ag.getOffset() + (e.getWheelRotation() * e.getScrollAmount() * SPEED));
		map.updateWidget(true);
	}
	
	public void valueChanged(ListSelectionEvent e) {
		stopscroll();
	}
}
