package com.affymetrix.igb.view.load;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.util.ThresholdReader;
import com.affymetrix.igb.view.SeqMapView;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JScrollBar;
import javax.swing.JSlider;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

/**
 *
 * @author hiralv
 */
public class AutoLoadThresholdHandler implements MouseListener, MouseMotionListener, PreferenceChangeListener{
	public final static boolean default_autoload = true;
	private final JSlider zoomer;
	private final JScrollBar scroller;
	private final NeoMap map;
//	private boolean was_dragging = false;
	public int threshold = ThresholdReader.default_threshold;
	private boolean autoLoadEnabled;

	protected int zoomer_value, scroller_value,prev_zoomer_value, prev_scroller_value;
	
	public AutoLoadThresholdHandler(SeqMapView seqMapView) {
		super();
		this.map = seqMapView.getSeqMap();
		this.zoomer = (JSlider)map.getZoomer(NeoMap.X);
		this.scroller = map.getScroller(NeoMap.X);
		
		this.zoomer.addMouseListener(this);
		this.scroller.addMouseListener(this);
		this.map.addMouseListener(this);
		this.map.addMouseMotionListener(this);
		this.zoomer_value = this.zoomer.getValue();
		threshold = PreferenceUtils.getIntParam(PreferenceUtils.PREFS_THRESHOLD, ThresholdReader.default_threshold);
		autoLoadEnabled = PreferenceUtils.getBooleanParam(PreferenceUtils.PREFS_AUTOLOAD, default_autoload);
		
		PreferenceUtils.getTopNode().addPreferenceChangeListener(this);
	}

	public void loadData(){
		if(!shouldAutoLoad()){
			return;
		}
		GeneralLoadView.loadAutoLoadFeatures();
	}
		
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	
	public void mouseReleased(MouseEvent e) {
		if (!autoLoadEnabled) {
			return;
		}

		Object src = e.getSource();

		if (src != map && src != scroller && src != zoomer) {
			return;
		}
		
		update(src);
	}

	private void update(Object src){
//		if (src == map && !was_dragging) {
//			was_dragging = false;
//			return;
//		}

		if (src == scroller) {
			scroller_value = scroller.getValue();
			if (scroller_value == prev_scroller_value) {
				return;
			}
			prev_scroller_value = scroller_value;
		} else if (src == zoomer){
			zoomer_value = ThresholdReader.getInstance().getCurrentThresholdValue();

			if (prev_zoomer_value == zoomer_value) {
				return;
			}
			prev_zoomer_value = zoomer_value;
		}

		loadData();
	}
	
	public void mouseDragged(MouseEvent e) {
//		was_dragging = true;
	}

	public boolean shouldAutoLoad(){
		return ThresholdReader.getInstance().isDetail(threshold);
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent pce) {
		if (! pce.getNode().equals(PreferenceUtils.getTopNode())) {
          return;
        }

		if(pce.getKey().equals(PreferenceUtils.PREFS_AUTOLOAD)){
			autoLoadEnabled = PreferenceUtils.getBooleanParam(PreferenceUtils.PREFS_AUTOLOAD, default_autoload);
		}
		if(pce.getKey().equals(PreferenceUtils.PREFS_THRESHOLD)){
			threshold = ThresholdReader.getInstance().getCurrentThresholdValue();
			//update(zoomer); //No need. It would have been already done.
			loadData();
		}
	}
}
