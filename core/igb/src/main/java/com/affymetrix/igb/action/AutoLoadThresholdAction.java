package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.widget.NeoMap;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.util.ThresholdReader;
import java.awt.event.ActionEvent;
import javax.swing.JSlider;
/**
 *
 * @author hiralv
 */
public class AutoLoadThresholdAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static final AutoLoadThresholdAction ACTION
			= new AutoLoadThresholdAction();
	
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}

	public static AutoLoadThresholdAction getAction(){
		return ACTION;
	}

	private AutoLoadThresholdAction() {
		super(BUNDLE.getString("setThreshold"), "16x16/actions/Set_Autoload_threshhold.png",
				"22x22/actions/Set_Autoload_threshhold.png");
		this.ordinal = -4006100;
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		super.actionPerformed(ae);
		PreferenceUtils.saveIntParam(PreferenceUtils.PREFS_THRESHOLD,
				ThresholdReader.getInstance().getCurrentThresholdValue());
		((JSlider)getSeqMapView().getSeqMap().getZoomer(NeoMap.X)).repaint();
	}
}
