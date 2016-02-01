package com.affymetrix.igb.action;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.general.DataContainer;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.OpenURIAction;
import com.affymetrix.igb.view.CustomGenomeDialogPanel;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.google.common.base.Strings;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.swing.JOptionPane;
import org.lorainelab.igb.menu.api.model.MenuBarParentMenu;
import org.lorainelab.igb.menu.api.model.MenuIcon;
import org.lorainelab.igb.menu.api.model.MenuItem;
import org.lorainelab.igb.synonymlookup.services.GenomeVersionSynonymLookup;
import org.lorainelab.igb.synonymlookup.services.SpeciesSynonymsLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lorainelab.igb.menu.api.MenuBarEntryProvider;

/**
 *
 * @author hiralv
 */
@Component(name = NewGenomeAction.COMPONENT_NAME, immediate = true, provide = {GenericAction.class, MenuBarEntryProvider.class})
public class NewGenomeAction extends OpenURIAction implements MenuBarEntryProvider {

    private static final Logger LOG = LoggerFactory.getLogger(NewGenomeAction.class);
    public static final String COMPONENT_NAME = "NewGenomeAction";
    private static final int FILE_MENU_INDEX = 6;
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

    @Reference
    public void setGenomeVersionSynonymLookup(GenomeVersionSynonymLookup genomeVersionSynonymLookup) {
        this.genomeVersionSynonymLookup = genomeVersionSynonymLookup;
    }

    @Reference
    public void setSpeciesSynLookup(SpeciesSynonymsLookup speciesSynLookup) {
        this.speciesSynLookup = speciesSynLookup;
    }

    @Override
    public Optional<List<MenuItem>> getMenuItems() {
        MenuItem newGenomeMenuItem = new MenuItem(BUNDLE.getString("addNewSpecies"), (Void t) -> {
            actionPerformed(null);
            return t;
        });
        try (InputStream resourceAsStream = NewGenomeAction.class.getClassLoader().getResourceAsStream(NEW_GENOME_ACTION_ICON)) {
            newGenomeMenuItem.setMenuIcon(new MenuIcon(resourceAsStream));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
        newGenomeMenuItem.setWeight(FILE_MENU_INDEX);
        return Optional.of(Arrays.asList(newGenomeMenuItem));
    }
    private static final String NEW_GENOME_ACTION_ICON = "new_genome.png";

    @Override
    public MenuBarParentMenu getMenuExtensionParent() {
        return MenuBarParentMenu.FILE;
    }

}
