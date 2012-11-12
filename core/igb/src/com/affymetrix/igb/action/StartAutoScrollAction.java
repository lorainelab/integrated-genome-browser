package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author hiralv
 */
public class StartAutoScrollAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1l;
	private static StartAutoScrollAction ACTION = new StartAutoScrollAction();
	
	private StartAutoScrollAction(){
		super(BUNDLE.getString("startAutoScroll"), "toolbarButtonGraphics/media/Play16.gif",
			"toolbarButtonGraphics/media/Play24.gif");
		setEnabled(true);
	}
	
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static StartAutoScrollAction getAction() { 
		return ACTION; 
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		getSeqMapView().getAutoScroll().start(this.getTierMap());
		setEnabled(false);
		StopAutoScrollAction.getAction().setEnabled(true);
	}
}
