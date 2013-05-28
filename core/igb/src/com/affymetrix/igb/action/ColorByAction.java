package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.color.ColorProviderI;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;

import com.affymetrix.igb.util.ColorByDialog;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author hiralv
 */
public class ColorByAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static final ColorByAction ACTION = new ColorByAction("colorByAction");
		
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ColorByAction getAction() {
		return ACTION;
	}

	private ColorByAction(String transKey) {
		super(BUNDLE.getString(transKey) , "16x16/actions/blank_placeholder.png", null);
	}
	
	@Override
	public void actionPerformed(java.awt.event.ActionEvent e) {
		super.actionPerformed(e);
		ITrackStyleExtended style = getTierManager().getSelectedTiers().get(0).getAnnotStyle();
		ColorProviderI cp = style.getColorProvider();
		
		ColorByDialog<ColorProviderI> colorByDialog = new ColorByDialog<ColorProviderI>(ColorProviderI.class);
		colorByDialog.setTitle("Color By");
		colorByDialog.setLocationRelativeTo(getSeqMapView());
		colorByDialog.setInitialValue(cp);
		cp = colorByDialog.showDialog();
		
		style.setColorProvider(cp);
		refreshMap(false, false);
		//TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}	
}
