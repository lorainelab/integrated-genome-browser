package com.affymetrix.igb.view.load;

import com.affymetrix.genoviz.widget.NeoMap;
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

	public AutoLoad(Adjustable zoomer, NeoMap map){
		this.map = map;
		this.zoomer = zoomer;
		this.scroller = map.getScroller(NeoMap.X);
		
		this.zoomer.addAdjustmentListener(this);
		this.scroller.addAdjustmentListener(this);
		this.map.addMouseListener(this);
		this.map.addMouseMotionListener(this);
	}

	public void adjustmentValueChanged(AdjustmentEvent evt) {
		//    System.out.println("adjustmentValueChanged to: " + evt.getValue());
		Adjustable source = evt.getAdjustable();
		//    System.out.println(source);
		if((source != zoomer && source != scroller) || is_dragging)
			return;
		
		if (source == zoomer) {

			zoomer_value = (source.getValue() * 100/ source.getMaximum());
			if (zoomer_value == prev_zoomer_value ||
					zoomer_value < threshold){
				return;
			}

			prev_zoomer_value = zoomer_value;
		}
		else if (source == scroller) {
			
			scroller_value = source.getValue();
			if (scroller_value == prev_scroller_value ||
					zoomer_value < threshold ||
					scroller.getValueIsAdjusting()){
				return;
			}
			
			prev_scroller_value = scroller_value;

		}

		loadData();
	}

	public void loadData(){
			GeneralLoadView.loadVisibleFeatures();
	}
	
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	
	public void mouseReleased(MouseEvent e) {
		is_dragging = false;
		if(was_dragging && zoomer_value > threshold){
			loadData();
		}
		was_dragging = false;
	}

	public void mouseDragged(MouseEvent e) {
		is_dragging = true;
		was_dragging = true;
	}
}
