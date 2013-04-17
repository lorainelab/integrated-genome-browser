
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.tiers.TrackStyle;
import java.awt.event.ActionEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.tiers.IGBStateProvider;
/**
 *
 * @author hiralv
 */
public class ShowLockedTrackIconAction extends GenericAction {
	private static final long serialVersionUID = 1L;
	private static final ShowLockedTrackIconAction ACTION = new ShowLockedTrackIconAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ShowLockedTrackIconAction getAction() {
		return ACTION;
	}

	private ShowLockedTrackIconAction() {
		super(BUNDLE.getString("showLockedTrackIcon"), "16x16/actions/blank_placeholder.png", null);
		this.putValue(SELECTED_KEY, IGBStateProvider.getShowLockIcon());
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		boolean b = (Boolean)getValue(SELECTED_KEY);
		IGBStateProvider.setShowLockIcon(b);
		((IGB) IGB.getSingleton()).getMapView().getSeqMap().repackTheTiers(true, true);
	}

	@Override
	public boolean isToggle() {
		return true;
	}
}
