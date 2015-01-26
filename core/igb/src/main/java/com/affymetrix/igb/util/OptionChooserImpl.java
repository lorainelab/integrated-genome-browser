package com.affymetrix.igb.util;

import com.affymetrix.genometry.AnnotatedSeqGroup;
import com.affymetrix.genometry.GenometryModel;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.OpenURIAction;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;

/**
 *
 * @author hiralv
 */
public class OptionChooserImpl extends OptionChooser {

    private static final long serialVersionUID = 1L;

    private static final String SELECT_SPECIES = BUNDLE.getString("speciesCap");

    @Override
    protected void speciesCBActionPerformedA(java.awt.event.ActionEvent evt) {
        populateVersionCB();
        if (getSpeciesCB().getSelectedIndex() == 0) {
            getSpeciesCB().setEditable(true);
        } else {
            getSpeciesCB().setEditable(false);
        }
        getVersionCB().setSelectedIndex(0);
    }

    @Override
    protected void versionCBActionPerformedA(java.awt.event.ActionEvent evt) {
        if (getVersionCB().getSelectedIndex() == 0) {
            getVersionCB().setEditable(true);
        } else {
            getVersionCB().setEditable(false);
        }
    }

    @Override
    protected void loadAsSeqCBActionPerformedA(java.awt.event.ActionEvent evt) {
        // Do nothing
    }

    public void refreshSpeciesList() {
        getSpeciesCB().removeAllItems();
        getSpeciesCB().addItem(OpenURIAction.UNKNOWN_SPECIES_PREFIX + " " + OpenURIAction.unknown_group_count);
        for (String species : GeneralLoadUtils.getSpeciesList()) {
            getSpeciesCB().addItem(species);
        }

        String speciesName = GeneralLoadView.getLoadView().getSelectedSpecies();

        if (!SELECT_SPECIES.equals(speciesName)) {
            getSpeciesCB().setSelectedItem(speciesName);
        } else {
            getSpeciesCB().setSelectedIndex(0);
        }

        AnnotatedSeqGroup group = GenometryModel.getInstance().getSelectedSeqGroup();
        if (group != null) {
            getVersionCB().setSelectedItem(group.getID());
        } else {
            getVersionCB().setSelectedIndex(0);
        }
    }

    private void populateVersionCB() {
        String speciesName = (String) getSpeciesCB().getSelectedItem();
        getVersionCB().removeAllItems();
        getVersionCB().addItem(OpenURIAction.UNKNOWN_GENOME_PREFIX + " " + OpenURIAction.unknown_group_count);
        for (String version : GeneralLoadUtils.getGenericVersions(speciesName)) {
            getVersionCB().addItem(version);
        }
    }

}
