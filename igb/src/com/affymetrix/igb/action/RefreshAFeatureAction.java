package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.awt.event.ActionEvent;

/**
 *
 * @author hiralv
 */
public abstract class RefreshAFeatureAction extends GenericAction {
	private static final long serialVersionUID = 1L;
	private GenericFeature feature;

	public static RefreshAFeatureAction createRefreshAFeatureAction(final GenericFeature feature) {
		RefreshAFeatureAction refreshAFeature = new RefreshAFeatureAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public String getText() {
				return "Load "+feature.featureName;
			}
		};
		refreshAFeature.setFeature(feature);
		return refreshAFeature;
	}

	private RefreshAFeatureAction(){
		super(null, null);
	}

	private void setFeature(GenericFeature feature) {
		this.feature = feature;
		this.enabled = (feature.getLoadStrategy() != LoadStrategy.NO_LOAD && feature.getLoadStrategy() != LoadStrategy.GENOME);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		GeneralLoadUtils.loadAndDisplayAnnotations(feature);
	}
}
