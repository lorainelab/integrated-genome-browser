package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.view.load.GeneralLoadView;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JScrollBar;
import javax.swing.JSlider;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
/**
 *
 * @author hiralv
 */
public class AutoLoadThresholdAction extends GenericAction 
		implements MouseListener, MouseMotionListener, PreferenceChangeListener{
	private static final long serialVersionUID = 1L;
	private static final AutoLoadThresholdAction ACTION = new AutoLoadThresholdAction();

	public static AutoLoadThresholdAction getAction(){
		return ACTION;
	}

	private static final int default_threshold = 70;
	public final static String  PREFS_THRESHOLD = "Threshold Value";
	public final static String  PREFS_AUTOLOAD = "Enable Auto load";
	public final static boolean default_autoload = true;
	private final JSlider zoomer;
	private final JScrollBar scroller;
	private final NeoMap map;
	private boolean was_dragging = false;
	public int threshold = 70;
	private boolean autoLoadEnabled;

	protected int zoomer_value, scroller_value,prev_zoomer_value, prev_scroller_value;
	
	private AutoLoadThresholdAction() {
		//"Set AutoLoad Threshold to Current View"
		super();
		this.map = ((IGB)IGB.getSingleton()).getMapView().getSeqMap();
		this.zoomer = (JSlider)map.getZoomer(NeoMap.X);
		this.scroller = map.getScroller(NeoMap.X);
		
		this.zoomer.addMouseListener(this);
		this.scroller.addMouseListener(this);
		this.map.addMouseListener(this);
		this.map.addMouseMotionListener(this);
		this.zoomer_value = this.zoomer.getValue();
		threshold = PreferenceUtils.getIntParam(PREFS_THRESHOLD, default_threshold);
		autoLoadEnabled = PreferenceUtils.getBooleanParam(PREFS_AUTOLOAD, default_autoload);
		
		PreferenceUtils.getTopNode().addPreferenceChangeListener(this);
	}

	public void loadData(){
		if(!shouldAutoLoad()){
			return;
		}
		GeneralLoadView.loadAutoLoadFeatures();
		//GeneralLoadView.getLoadView().loadResiduesInView(false);
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
		if (src == map && !was_dragging) {
			was_dragging = false;
			return;
		}

		if (src == scroller) {
			scroller_value = scroller.getValue();
			if (scroller_value == prev_scroller_value) {
				return;
			}
			prev_scroller_value = scroller_value;
		} else if (src == zoomer){
			zoomer_value = (zoomer.getValue() * 100 / zoomer.getMaximum());

			if (prev_zoomer_value == zoomer_value) {
				return;
			}
			prev_zoomer_value = zoomer_value;
		}

		loadData();
	}

	public void mapZoomed(){
		update(zoomer);
	}
	
	public void mouseDragged(MouseEvent e) {
		was_dragging = true;
	}

	public boolean shouldAutoLoad(){
		return zoomer_value >= threshold;
	}

	public void preferenceChange(PreferenceChangeEvent pce) {
		if (! pce.getNode().equals(PreferenceUtils.getTopNode())) {
          return;
        }

		if(pce.getKey().equals(PREFS_AUTOLOAD)){
			autoLoadEnabled = PreferenceUtils.getBooleanParam(PREFS_AUTOLOAD, default_autoload);
		}
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		super.actionPerformed(ae);
		threshold = (zoomer.getValue() * 100 / zoomer.getMaximum());
		update(zoomer);
		PreferenceUtils.saveIntParam(PREFS_THRESHOLD, threshold);
		zoomer.repaint();
		loadData();
	}

	@Override
	public String getText() {
		return BUNDLE.getString("setThreshold");
	}

	@Override
	public String getIconPath() {
		return "images/autoload.png";
	}
}
