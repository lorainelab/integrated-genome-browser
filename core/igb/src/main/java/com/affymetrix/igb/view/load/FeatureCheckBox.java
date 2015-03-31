package com.affymetrix.igb.view.load;

import com.affymetrix.genometry.general.GenericFeature;
import com.affymetrix.igb.swing.JRPCheckBox;
import com.affymetrix.igb.view.load.FeatureTreeView.FeatureTreeCellEditor.FeatureLoadAction;
import java.awt.Insets;
import java.awt.event.ActionListener;

public class FeatureCheckBox extends JRPCheckBox {

    private static final long serialVersionUID = 1L;
    private static final Insets insets = new Insets(0, 0, 0, 0);
    private boolean featureLoadActionSet;

    public FeatureCheckBox(GenericFeature gFeature) {
        super(getId(gFeature));
        featureLoadActionSet = false;
    }

    @Override
    public Insets getInsets() {
        return insets;
    }

    public void addActionListener(ActionListener l) {
        super.addActionListener(l);
        if (l instanceof FeatureLoadAction) {
            featureLoadActionSet = true;
        }
    }

    public boolean isFeatureLoadActionSet() {
        return featureLoadActionSet;
    }

    private static final String getId(GenericFeature gFeature) {
        String featureName = gFeature.featureName;
        String featureText = gFeature.featureName.substring(featureName.lastIndexOf(FeatureTreeView.path_separator) + 1).replaceAll(" ", "_");
        return "FeatureTreeView_LeafCheckBox_"
                + gFeature.gVersion.gServer.getServerType().getName()
                + "_"
                + gFeature.gVersion.gServer.getURL()
                + "_"
                + featureText;
    }

}
