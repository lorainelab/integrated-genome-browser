package com.affymetrix.igb.action;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.general.DataContainer;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.OpenURIAction;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.affymetrix.igb.view.CustomGenomeDialogPanel;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.google.common.base.Strings;
import com.lorainelab.igb.services.window.menus.IgbMenuItemProvider;
import com.lorainelab.igb.services.window.menus.IgbToolBarParentMenu;
import com.lorainelab.synonymlookup.services.GenomeVersionSynonymLookup;
import com.lorainelab.synonymlookup.services.SpeciesSynonymsLookup;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import javax.swing.JOptionPane;

/**
 *
 * @author hiralv
 */
@Component(name = NewGenomeAction.COMPONENT_NAME, immediate = true, provide = {GenericAction.class, IgbMenuItemProvider.class})
public class NewGenomeAction extends OpenURIAction implements IgbMenuItemProvider {

    public static final String COMPONENT_NAME = "NewGenomeAction";
    private static final int FILE_MENU_INDEX = 3;
    private static final long serialVersionUID = 1L;
    private final int TOOLBAR_INDEX = 3;

    private GenomeVersionSynonymLookup genomeVersionSynonymLookup;
    private SpeciesSynonymsLookup speciesSynLookup;

    public NewGenomeAction() {
        super(BUNDLE.getString("openCustomGenomeMenuTitle"), BUNDLE.getString("openCustomGenomeTooltip"),
                "16x16/actions/new_genome.png", "22x22/actions/new_genome.png",
                KeyEvent.VK_UNDEFINED, null, false);
        this.ordinal = 200;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        CustomGenomeDialogPanel ng = new CustomGenomeDialogPanel();
        triggerCustumGenomeDialogPanel(ng);
    }

    private void triggerCustumGenomeDialogPanel(CustomGenomeDialogPanel ng) {
        int reply = JOptionPane.showConfirmDialog(getSeqMapView(), ng, getText(), JOptionPane.OK_CANCEL_OPTION);
        if (reply == JOptionPane.OK_OPTION) {
            String speciesName = getSpeciesName(ng);
            String genomeVersionName = getGenomeVersionName(ng);
            incrementCustomCounter(speciesName, genomeVersionName);
            GenomeVersion genomeVersion = gmodel.addGenomeVersion(genomeVersionName);
            String refSeqPath = ng.getRefSeqFile();

            if (!Strings.isNullOrEmpty(refSeqPath)) {
                String fileName = getFriendlyName(refSeqPath);
                igbService.openURI(new File(refSeqPath).toURI(), fileName, genomeVersion, speciesName, true);
            } else {
                DataContainer version = GeneralLoadUtils.getLocalFileDataContainer(genomeVersion, speciesName);
//                ServerList.getServerInstance().fireServerInitEvent(version.getgServer(), ResourceStatus.Initialized, false);
            }

            gmodel.setSelectedGenomeVersion(genomeVersion);

        }
    }

    private String getGenomeVersionName(CustomGenomeDialogPanel ng) {
        String versionName;
        if (Strings.isNullOrEmpty(ng.getVersionName())) {
            versionName = UNKNOWN_GENOME_PREFIX + " " + CUSTOM_GENOME_COUNTER;
        } else {
            versionName = ng.getVersionName();
        }
        versionName = genomeVersionSynonymLookup.getPreferredName(versionName);
        return versionName;
    }

    private String getSpeciesName(CustomGenomeDialogPanel ng) {
        String speciesName;
        if (Strings.isNullOrEmpty(ng.getSpeciesName())) {
            speciesName = UNKNOWN_SPECIES_PREFIX + " " + CUSTOM_GENOME_COUNTER;
        } else {
            speciesName = ng.getSpeciesName();
        }
        speciesName = speciesSynLookup.getPreferredName(speciesName);
        return speciesName;
    }

    private void incrementCustomCounter(String speciesName, String versionName) {
        if (speciesName.equals(UNKNOWN_SPECIES_PREFIX + " " + CUSTOM_GENOME_COUNTER) || versionName.equals(UNKNOWN_GENOME_PREFIX + " " + CUSTOM_GENOME_COUNTER)) {
            CUSTOM_GENOME_COUNTER++;
        }
    }

    @Override
    public boolean isToolbarDefault() {
        return true;
    }

    @Override
    public int getToolbarIndex() {
        return TOOLBAR_INDEX;
    }

    @Override
    public IgbToolBarParentMenu getParentMenu() {
        return IgbToolBarParentMenu.FILE;
    }

    @Override
    public JRPMenuItem getMenuItem() {
        JRPMenuItem consoleMenuItem = new JRPMenuItem("showConsole", this, FILE_MENU_INDEX);
        consoleMenuItem.setText(BUNDLE.getString("addNewSpecies"));
        return consoleMenuItem;
    }

    @Override
    public int getMenuItemWeight() {
        return FILE_MENU_INDEX;
    }

    @Reference
    public void setGenomeVersionSynonymLookup(GenomeVersionSynonymLookup genomeVersionSynonymLookup) {
        this.genomeVersionSynonymLookup = genomeVersionSynonymLookup;
    }

    @Reference
    public void setSpeciesSynLookup(SpeciesSynonymsLookup speciesSynLookup) {
        this.speciesSynLookup = speciesSynLookup;
    }

}
