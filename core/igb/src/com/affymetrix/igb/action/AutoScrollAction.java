package com.affymetrix.igb.action;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.view.SeqMapView;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author hiralv
 */
public class AutoScrollAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static AutoScrollAction ACTION = new AutoScrollAction();
	protected ActionListener map_auto_scroller = null;
	protected Timer swing_timer = null;
	
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
		start();
	}
	
	private void start() {
		final SeqMapView seqMapView = IGB.getSingleton().getMapView();
		int bases_per_pixel = ConfigureScrollAction.getAction().get_bases_per_pix(); 
		int pix_to_scroll = ConfigureScrollAction.getAction().get_pix_to_scroll();
		int timer_interval = ConfigureScrollAction.getAction().get_time_interval();
		final int start_coord = ConfigureScrollAction.getAction().get_start_pos();
		final int end_coord = ConfigureScrollAction.getAction().get_end_pos();
		final boolean cycle = true;
		
		double pix_per_coord = 1.0 / bases_per_pixel;
		final double coords_to_scroll = pix_to_scroll / pix_per_coord;

		seqMapView.getSeqMap().zoom(NeoAbstractWidget.X, pix_per_coord);
		seqMapView.getSeqMap().scroll(NeoAbstractWidget.X, start_coord);

		if (map_auto_scroller == null) {
			map_auto_scroller = new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent evt) {
					Rectangle2D.Double vbox = seqMapView.getSeqMap().getViewBounds();
					int scrollpos = (int) (vbox.x + coords_to_scroll);
					if ((scrollpos + vbox.width) > end_coord) {
						if (cycle) {
							seqMapView.getSeqMap().scroll(NeoAbstractWidget.X, start_coord);
							seqMapView.getSeqMap().updateWidget();
						} else {
							stop();
						}
					} else {
						seqMapView.getSeqMap().scroll(NeoAbstractWidget.X, scrollpos);
						seqMapView.getSeqMap().updateWidget();
					}
				}
			};

			swing_timer = new javax.swing.Timer(timer_interval, map_auto_scroller);
			swing_timer.start();
			// Other options:
			//    java.util.Timer ??
			//    com.affymetrix.genoviz.util.NeoTimerEventClock ??
		} else {
			stop();
		}
	}
	
	public void stop() {
		// end of sequence reached, so stop scrolling
		if(swing_timer != null){
			swing_timer.stop();
			swing_timer = null;
		}
		map_auto_scroller = null;
	}
}
