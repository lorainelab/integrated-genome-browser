package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.view.load.GeneralLoadView;

public class FeatureUnloadAction extends GenericAction {

	private static final long serialVersionUID = 1L;
	private final GenericFeature feature;

	public FeatureUnloadAction(GenericFeature feature) {
		super(null, null, null, null, KeyEvent.VK_UNDEFINED, null, false);
		this.feature = feature;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		String message = "Unchecking " + feature.featureName
				+ " will remove all loaded data. \nDo you want to continue? ";
		if (feature.getMethods().isEmpty() || Application.confirmPanel(message, PreferenceUtils.getTopNode(),
				PreferenceUtils.CONFIRM_BEFORE_DELETE, PreferenceUtils.default_confirm_before_delete)) {
			GeneralLoadView.getLoadView().removeFeature(feature, true);
		}
	}
}
