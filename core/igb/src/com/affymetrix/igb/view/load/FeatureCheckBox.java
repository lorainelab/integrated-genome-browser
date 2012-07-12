package com.affymetrix.igb.view.load;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genoviz.swing.recordplayback.JRPCheckBox;
import com.affymetrix.igb.action.FeatureLoadAction;
import com.affymetrix.igb.action.FeatureUnloadAction;

public class FeatureCheckBox extends JRPCheckBox {
	private static final long serialVersionUID = 1L;
	private static final Insets insets = new Insets(0, 0, 0, 0);
	private final GenericFeature feature;
	private final FeatureLoadAction loadAction;
	private final FeatureUnloadAction unloadAction;

	public FeatureCheckBox(GenericFeature gFeature) {
		super(getId(gFeature));
		this.feature = gFeature;
		this.loadAction = new FeatureLoadAction(feature);
		this.unloadAction = new FeatureUnloadAction(feature);
		this.addActionListener(
			new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (FeatureCheckBox.this.isSelected()) {
						unloadAction.actionPerformed(e);
					}
					else {
						loadAction.actionPerformed(e);
					}
				}
			}
		);
	}
	@Override
	public Insets getInsets() {
		return insets;
	}

	private static final String getId(GenericFeature gFeature) {
		String featureName = gFeature.featureName;
		String featureText = gFeature.featureName.substring(featureName.lastIndexOf(FeatureTreeView.path_separator) + 1).replaceAll(" ", "_");
		return "FeatureTreeView_LeafCheckBox_" + 
			gFeature.gVersion.gServer.serverType.getName() +
			"_" + 
			gFeature.gVersion.gServer.URL +
			"_" +
			featureText;
	}
	
}
