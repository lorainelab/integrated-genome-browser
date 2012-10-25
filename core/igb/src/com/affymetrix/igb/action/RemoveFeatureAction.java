package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import com.affymetrix.igb.view.load.GeneralLoadView;
import static com.affymetrix.igb.shared.Selections.*;

/**
 *
 * @author hiralv
 */
public class RemoveFeatureAction extends SeqMapViewActionA {
	
	private static final long serialVersionUID = 1L;
	private static final RemoveFeatureAction ACTION = new RemoveFeatureAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static RemoveFeatureAction getAction() {
		return ACTION;
	}

	protected RemoveFeatureAction() {
		super(IGBConstants.BUNDLE.getString("deleteFeatureAction"), null,
				"16x16/actions/delete_track.png",
				"22x22/actions/delete_track.png", KeyEvent.VK_UNDEFINED);
		this.ordinal = -9007300;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		String message = "Really remove all selected data set ?";
		if (Application.confirmPanel(message, PreferenceUtils.getTopNode(),
						PreferenceUtils.CONFIRM_BEFORE_DELETE, PreferenceUtils.default_confirm_before_delete)) {
			for (ITrackStyleExtended style : allStyles) {
				if(style.getFeature() != null){
					GeneralLoadView.getLoadView().removeFeature(style.getFeature(), true);
				}
			}
		}
		getSeqMapView().dataRemoved();	// refresh
	}
}
