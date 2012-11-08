package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author hiralv
 */
public class AutoScrollAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1l;
	private static AutoScrollAction ACTION = new AutoScrollAction();
	
	private AutoScrollAction(){
		super(BUNDLE.getString("autoScroll"), "toolbarButtonGraphics/media/Play16.gif",
			"toolbarButtonGraphics/media/Play24.gif");
	}
	
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static AutoScrollAction getAction() { 
		return ACTION; 
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		getSeqMapView().getAutoScroll().start(this.getTierMap());
	}
}
