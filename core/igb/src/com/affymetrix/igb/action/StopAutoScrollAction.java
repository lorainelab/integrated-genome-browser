package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author hiralv
 */
public class StopAutoScrollAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1l;
	private static StopAutoScrollAction ACTION = new StopAutoScrollAction();
	
	private StopAutoScrollAction(){
		super(BUNDLE.getString("stopAutoScroll"), "16x16/actions/autoscroll_stop.png",
			"22x22/actions/autoscroll_stop.png");
		setEnabled(false);
	}
	
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static StopAutoScrollAction getAction() { 
		return ACTION; 
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		getSeqMapView().getAutoScroll().stop();
		setEnabled(false);
		StartAutoScrollAction.getAction().setEnabled(true);
		getSeqMapView().getAutoLoadAction().loadData();
	}
}
