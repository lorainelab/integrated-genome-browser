
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
public class GlyphScroller {
	private static final int SPEED = 10;
	MouseWheelAndListListener listener;
	AffyLabelledTierMap map;
	ScrollableViewModeGlyph ag;
	
	public GlyphScroller(AffyLabelledTierMap map){
		this.map = map;
	}
	
	public void startscroll(TierGlyph tier){
		if(!(tier.getViewModeGlyph() instanceof ScrollableViewModeGlyph)
				|| !((ScrollableViewModeGlyph)tier.getViewModeGlyph()).isScrollingAllowed()){
			return;
		}
		
		listener = new MouseWheelAndListListener();
		ag = (ScrollableViewModeGlyph)tier.getViewModeGlyph();
		
		// Add listeners
		map.getLabelMap().getNeoCanvas().addMouseWheelListener(listener);
		map.addListSelectionListener(listener);
	}
	
	private void scroll(int i){
		ag.setOffset(ag.getOffset() + (i * SPEED));
		map.updateWidget(true);
	}
	
	private void stopscroll(){
		map.getLabelMap().getNeoCanvas().removeMouseWheelListener(listener);
		map.removeListSelectionListener(listener);
		
		// Helps with garbage collection
		ag = null;
		map = null;
		listener = null;
	}
	
	private class MouseWheelAndListListener implements MouseWheelListener, ListSelectionListener {

		public void mouseWheelMoved(MouseWheelEvent e) {
			scroll(e.getWheelRotation() * e.getScrollAmount());
		}

		public void valueChanged(ListSelectionEvent e) {
			stopscroll();
		}
	}
	
}
