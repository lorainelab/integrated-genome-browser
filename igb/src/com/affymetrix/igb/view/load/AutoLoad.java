package com.affymetrix.igb.view.load;

import com.affymetrix.genoviz.widget.NeoMap;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Hashtable;
import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JSlider;

/**
 *
 * @author hiralv
 */
public class AutoLoad implements MouseListener, MouseMotionListener {

	private final JSlider zoomer;
	private final JScrollBar scroller;
	private final NeoMap map;
	private boolean was_dragging = false;
	public static final int threshold = 85;

	protected int zoomer_value, scroller_value,prev_zoomer_value, prev_scroller_value;

	public AutoLoad(NeoMap map){
		this.map = map;
		this.zoomer = (JSlider)map.getZoomer(NeoMap.X);
		this.scroller = map.getScroller(NeoMap.X);
		
		this.zoomer.addMouseListener(this);
		this.scroller.addMouseListener(this);
		this.map.addMouseListener(this);
		this.map.addMouseMotionListener(this);

		showAutoLoadRegion();
	}

	private void showAutoLoadRegion(){
		String text = "Autoload Region";
		int threshValue = (AutoLoad.threshold * zoomer.getMaximum()/100);
		Hashtable<Integer, JLabel> table = new Hashtable<Integer, JLabel>();
		JLabel startStop = new JLabel("|");
		startStop.setForeground(Color.RED);
		JLabel def = new JLabel("-");
		def.setForeground(Color.RED);

		table.put(threshValue, startStop);
		for(int i=threshValue+1; i<zoomer.getMaximum(); i++){
			table.put(i, def);
		}
		table.put(zoomer.getMaximum(), startStop);
		
		zoomer.setLabelTable(table);
		zoomer.setPaintLabels(true);
	}

	public void loadData(){
		GeneralLoadView.loadAutoLoadFeatures();
		//GeneralLoadView.getLoadView().loadResiduesInView(false);
	}
	
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	
	public void mouseReleased(MouseEvent e) {
		Object src = e.getSource();

		if (src != map && src != scroller && src != zoomer) {
			return;
		}

		if (src == map && !was_dragging) {
			was_dragging = false;
			return;
		}

		if (src == scroller) {
			scroller_value = scroller.getValue();
			if (zoomer_value < threshold
					|| scroller_value == prev_scroller_value) {
				return;
			}
			prev_scroller_value = scroller_value;
		} else if (src == zoomer){
			zoomer_value = (zoomer.getValue() * 100 / zoomer.getMaximum());
			if (zoomer_value == prev_zoomer_value
					|| zoomer_value < threshold) {
				return;
			}
			prev_zoomer_value = zoomer_value;
		}
		
		loadData();
	}

	public void mouseDragged(MouseEvent e) {
		was_dragging = true;
	}
}
