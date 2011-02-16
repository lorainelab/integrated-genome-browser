package com.affymetrix.igb.view.load;

import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.action.LoadSequence;
import java.awt.Adjustable;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JScrollBar;

/**
 *
 * @author hiralv
 */
public class AutoLoad implements AdjustmentListener, MouseListener, MouseMotionListener {

	private final Adjustable zoomer;
	private final JScrollBar scroller;
	private final NeoMap map;
	private boolean is_dragging = false;
	private boolean was_dragging = false;
	private final int threshold = 85;

	protected int zoomer_value, scroller_value,prev_zoomer_value, prev_scroller_value;

	public AutoLoad(NeoMap map){
		this.map = map;
		this.zoomer = map.getZoomer(NeoMap.X);
		this.scroller = map.getScroller(NeoMap.X);
		
		this.zoomer.addAdjustmentListener(this);
		this.scroller.addMouseListener(this);
		this.map.addMouseListener(this);
		this.map.addMouseMotionListener(this);
	}

	public void adjustmentValueChanged(AdjustmentEvent evt) {
		Adjustable source = evt.getAdjustable();

		if (source != zoomer) {
			return;
		}

		zoomer_value = (source.getValue() * 100 / source.getMaximum());
		if (zoomer_value == prev_zoomer_value
				|| zoomer_value < threshold) {
			return;
		}

		prev_zoomer_value = zoomer_value;

		loadData();
	}

	public void loadData(){
		GeneralLoadView.loadAutoLoadFeatures();
		GeneralLoadView.getLoadView().loadResidues(LoadSequence.getPartialAction());
	}
	
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	
	public void mouseReleased(MouseEvent e) {

		if (e.getSource() == map && !was_dragging) {
			was_dragging = false;
			return;
		}
		
		scroller_value = scroller.getValue();
		if (zoomer_value < threshold
				|| scroller_value == prev_scroller_value) {
			return;
		}
		
		prev_scroller_value = scroller_value;
		loadData();
	}

	public void mouseDragged(MouseEvent e) {
		was_dragging = true;
	}
}
