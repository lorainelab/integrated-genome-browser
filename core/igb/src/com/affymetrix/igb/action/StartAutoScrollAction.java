package com.affymetrix.igb.action;

import java.awt.geom.Rectangle2D;
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
		super(BUNDLE.getString("startAutoScroll"), "16x16/actions/autoscroll.png",
			"22x22/actions/autoscroll.png");
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
		start();
	}
	
	public void start(){
		// Calculate start, end and bases per pixels
		Rectangle2D.Double cbox = getTierMap().getViewBounds();
		int bases_in_view = (int) cbox.width;
		int start_pos = (int) cbox.x;
		int end_pos = getSeqMapView().getViewSeq().getLength();
		int pixel_width = getTierMap().getView().getPixelBox().width;
		int bases_per_pix = bases_in_view / pixel_width;
		
		// as_bases_per_pix *should* be a float, or else should simply
		// use the current resolution without asking the user,
		// but since it is an integer, we have to set the minimum value as 1
		if (bases_per_pix < 1) {
			bases_per_pix = 1;
		}
		
		getSeqMapView().getAutoScroll().configure(bases_per_pix, start_pos, end_pos);
		getSeqMapView().getAutoScroll().start(this.getTierMap());
		setEnabled(false);
		StopAutoScrollAction.getAction().setEnabled(true);
	}
}
