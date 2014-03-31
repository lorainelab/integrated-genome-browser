/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.tiers.IGBStateProvider;
import com.affymetrix.igb.shared.TrackConstants;
import java.awt.event.ActionEvent;
import static javax.swing.Action.SELECTED_KEY;

/**
 *
 * @author tkanapar
 */
public class ShowFilterMarkAction  extends SeqMapViewActionA {

	private static final long serialVersionUID = 1;
	private static final ShowFilterMarkAction ACTION = new ShowFilterMarkAction();

	static {
		GenericActionHolder.getInstance().addGenericAction(ACTION);
		PreferenceUtils.saveToPreferences(TrackConstants.PREF_SHOW_FILTER_MARK, TrackConstants.default_show_filter_mark, ACTION);
	}
	
	public static ShowFilterMarkAction getAction() {
		return ACTION;
	}
	
	private ShowFilterMarkAction() {
		super(BUNDLE.getString("showFilterMark"), "16x16/actions/blank_placeholder.png", null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		boolean b = (Boolean)getValue(SELECTED_KEY);
		IGBStateProvider.setShowFilterMark(b);
		getSeqMapView().getSeqMap().updateWidget();

	}
}
