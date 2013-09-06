/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.PreferenceUtils;

import com.affymetrix.igb.tiers.TrackConstants;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.tiers.IGBStateProvider;
import static javax.swing.Action.SELECTED_KEY;

/**
 *
 * @author tkanapar
 */

public class ShowFullFilePathInTrack extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static final ShowFullFilePathInTrack ACTION = new ShowFullFilePathInTrack();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
		PreferenceUtils.saveToPreferences(TrackConstants.PREF_SHOW_FULL_FILE_PATH_IN_TRACK, TrackConstants.default_show_full_file_path_in_track, ACTION);
	}
	
	public static ShowFullFilePathInTrack getAction() {
		return ACTION;
	}

	private ShowFullFilePathInTrack() {
		super(BUNDLE.getString("showFullFilePathInTrack"), "16x16/actions/blank_placeholder.png", null);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		boolean b = (Boolean)getValue(SELECTED_KEY);
		IGBStateProvider.setShowFullFilePathInTrackMark(b);

	}

	@Override
	public boolean isToggle() {
		return true;
	}
}