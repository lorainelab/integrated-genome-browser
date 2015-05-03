package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.general.DataSet;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.awt.event.ActionEvent;

/**
 *
 * @author hiralv
 */
public class RefreshAFeatureAction extends GenericAction {

    private static final long serialVersionUID = 1L;
    private DataSet feature;

    public static RefreshAFeatureAction createRefreshAFeatureAction(final DataSet feature) {
        final String text = "Load " + feature.getDataSetName();
        RefreshAFeatureAction refreshAFeature = new RefreshAFeatureAction(text);
        refreshAFeature.setFeature(feature);
        return refreshAFeature;
    }

    private RefreshAFeatureAction(String text) {
        super(text, "toolbarButtonGraphics/general/Refresh16.gif", null);
    }

    private void setFeature(DataSet feature) {
        this.feature = feature;
        this.enabled = (feature.getLoadStrategy() != LoadStrategy.NO_LOAD && feature.getLoadStrategy() != LoadStrategy.GENOME);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        GeneralLoadUtils.loadAndDisplayAnnotations(feature);
    }
}
