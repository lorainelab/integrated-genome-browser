
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.tiers.TrackStyle;
import java.awt.event.ActionEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
/**
 *
 * @author hiralv
 */
public class DrawCollapseControlAction extends GenericAction {
	private static final long serialVersionUID = 1L;
	private static final DrawCollapseControlAction ACTION = new DrawCollapseControlAction();

	public static DrawCollapseControlAction getAction() {
		return ACTION;
	}

	private DrawCollapseControlAction() {
		super(BUNDLE.getString("drawCollapseControl"), null, null);
		this.putValue(SELECTED_KEY, TrackStyle.getDrawCollapseState());
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		boolean b = !TrackStyle.getDrawCollapseState();
		TrackStyle.setDrawCollapseControl(b);
		this.putValue(SELECTED_KEY, b);
		((IGB) IGB.getSingleton()).getMapView().getSeqMap().updateWidget();
	}

	@Override
	public boolean isToggle() {
		return true;
	}
}
