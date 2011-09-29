
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.tiers.TrackStyle;
import java.awt.event.ActionEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
/**
 *
 * @author hiralv
 */
public class DrawCollapseControlAction extends GenericAction {
	private static final long serialVersionUID = 1L;
	final SeqMapView map_view;
	
	public DrawCollapseControlAction(SeqMapView map_view){
		super();
		this.putValue(SELECTED_KEY, TrackStyle.getDrawCollapseState());
		this.map_view = map_view;
	}
	
	@Override
	public String getText() {
		return BUNDLE.getString("drawCollapseControl");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		boolean b = !TrackStyle.getDrawCollapseState();
		TrackStyle.setDrawCollapseControl(b);
		this.putValue(SELECTED_KEY, b);
		map_view.getSeqMap().updateWidget();
	}
	
}
