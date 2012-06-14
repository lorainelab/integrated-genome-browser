package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.util.ThresholdReader;

import java.awt.event.ActionEvent;
import javax.swing.JSlider;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
/**
 *
 * @author hiralv
 */
public class AutoLoadThresholdAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static final AutoLoadThresholdAction ACTION = new AutoLoadThresholdAction();
	
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}

	public static AutoLoadThresholdAction getAction(){
		return ACTION;
	}

	private AutoLoadThresholdAction() {
		super(BUNDLE.getString("setThreshold"), "16x16/actions/autoload.png", "22x22/actions/autoload.png");
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		super.actionPerformed(ae);
		PreferenceUtils.saveIntParam(PreferenceUtils.PREFS_THRESHOLD, ThresholdReader.getInstance().getCurrentThresholdValue());
		((JSlider)getSeqMapView().getSeqMap().getZoomer(NeoMap.X)).repaint();
	}
}
