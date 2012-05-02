package com.affymetrix.igb.tiers;

import com.affymetrix.igb.Application;
import com.affymetrix.igb.shared.ScrollableViewModeGlyph;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import java.awt.Dimension;
import java.awt.event.*;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JWindow;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author hiralv
 */
public class GlyphScroller {
	private static final int SCROLLBAR_WIDTH = 12;
	private static final int WINDOW_OFFSET = 4;
	private static final int SPEED = 10;
	
	JWindow scroll_window;
	JScrollBar scrollbar;
	Listeners listener;
	
	AffyLabelledTierMap map;
	TierGlyph tier;
	ScrollableViewModeGlyph svmg;
	
	public GlyphScroller(AffyLabelledTierMap map){
		this.map = map;
	}
	
	public void startscroll(TierGlyph tg){
		if(!(tg.getViewModeGlyph() instanceof ScrollableViewModeGlyph)
				|| !((ScrollableViewModeGlyph)tg.getViewModeGlyph()).isScrollingAllowed()){
			return;
		}
		
		// Initialize all variables
		tier = tg;
		svmg = (ScrollableViewModeGlyph)tier.getViewModeGlyph();
		scrollbar = getScrollBar(tier, svmg.getOffset());
		scroll_window = getWindow(scrollbar);

		// Add listeners
		listener = new Listeners();
		addListeners();
		
		// Set scroll window properties
		resizeWindow();
		repositionWindow();
		scroll_window.setVisible(true);
	}

	private void scroll(int i){
		svmg.setOffset(i);
		map.updateWidget(true);
	}
	
	private void stopscroll(){
		// Remove Listeners
		removeListners();
		
		// Dispose window
		scroll_window.dispose();
		
		// Helps with garbage collection
		scrollbar = null;
		svmg = null;
		map = null;
		listener = null;
	}
	
	private void repositionWindow(){
		scroll_window.setLocation(map.getLocationOnScreen().x + Math.min(0, tier.getPixelBox(map.getView()).x) + WINDOW_OFFSET, 
				map.getLocationOnScreen().y + tier.getPixelBox(map.getView()).y + WINDOW_OFFSET);
	}
	
	private void resizeWindow() {
		scroll_window.setPreferredSize(new Dimension(SCROLLBAR_WIDTH, tier.getPixelBox(map.getView()).height - WINDOW_OFFSET));
		scroll_window.pack();
	}
	
	private void addListeners(){
		map.getLabelMap().getNeoCanvas().addMouseWheelListener(listener);
		map.addListSelectionListener(listener);
		scrollbar.addAdjustmentListener(listener);
		Application.getSingleton().getFrame().addComponentListener(listener);
	}
	
	private void removeListners(){
		map.getLabelMap().getNeoCanvas().removeMouseWheelListener(listener);
		map.removeListSelectionListener(listener);
		scrollbar.removeAdjustmentListener(listener);
		Application.getSingleton().getFrame().removeComponentListener(listener);
	}
	
	private static JWindow getWindow(JComponent... components){
		JWindow window = new JWindow(Application.getSingleton().getFrame());
		for(int i=0; i<components.length; i++){
			window.add(components[i]);
		}
		
		return window;
	}
	
	private static JScrollBar getScrollBar(TierGlyph tier, int sb_curr){
		int style_height = (int) tier.getViewModeGlyph().getChildHeight() * tier.getActualSlots()  + 75;
		if(tier.getDirection() != Direction.REVERSE){
			style_height *= -1;
		}
		
		int sb_min = Math.min(1, style_height);
		int sb_max = Math.max(1, style_height);
		sb_max = Math.max(sb_curr, sb_max);
		
		return new JScrollBar(JScrollBar.VERTICAL, sb_curr, 0, sb_min, sb_max);
	}
	
	private class Listeners implements MouseWheelListener, 
			ListSelectionListener, AdjustmentListener, ComponentListener {

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			scrollbar.setValue(svmg.getOffset() + (e.getWheelRotation() * e.getScrollAmount() * SPEED));
		}

		@Override
		public void adjustmentValueChanged(AdjustmentEvent e) {
			scroll(e.getValue());
		}
			
		@Override
		public void valueChanged(ListSelectionEvent e) {
			stopscroll();
		}
		
		@Override
		public void componentMoved(ComponentEvent e) {
			repositionWindow();
		}
		
		public void componentResized(ComponentEvent e) {
			resizeWindow();
			repositionWindow();
		}
		
		public void componentShown(ComponentEvent e) { }
		public void componentHidden(ComponentEvent e) { }
	}
	
}
