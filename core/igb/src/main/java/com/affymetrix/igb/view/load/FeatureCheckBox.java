package com.affymetrix.igb.view.load;

import com.affymetrix.genometry.general.DataSet;
import com.affymetrix.igb.swing.JRPCheckBox;
import com.affymetrix.igb.view.load.FeatureTreeView.FeatureTreeCellEditor.FeatureLoadAction;
import java.awt.Insets;
import java.awt.event.ActionListener;

public class FeatureCheckBox extends JRPCheckBox {

    private static final long serialVersionUID = 1L;
    private static final Insets insets = new Insets(0, 0, 0, 0);
    private boolean featureLoadActionSet;

    public FeatureCheckBox(DataSet gFeature) {
        super(getId(gFeature));
        featureLoadActionSet = false;
    }

    @Override
    public Insets getInsets() {
        return insets;
    }

    @Override
    public void addActionListener(ActionListener l) {
        super.addActionListener(l);
        if (l instanceof FeatureLoadAction) {
            featureLoadActionSet = true;
        }
    }

    public boolean isFeatureLoadActionSet() {
        return featureLoadActionSet;
    }

    private static String getId(DataSet dataSet) {
        String featureName = dataSet.getDataSetName();
        String featureText = dataSet.getDataSetName().substring(featureName.lastIndexOf(FeatureTreeView.path_separator) + 1).replaceAll(" ", "_");
        return "FeatureTreeView_LeafCheckBox_"
                + dataSet.getDataContainer().getDataProvider().getName()
                + "_"
                + dataSet.getDataContainer().getDataProvider().getUrl()
                + "_"
                + featureText;
    }

}
