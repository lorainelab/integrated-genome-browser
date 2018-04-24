package com.affymetrix.igb.general;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.common.PreferenceUtils;
import com.google.common.collect.Lists;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.FileTracker;
import com.affymetrix.genometry.util.GeneralUtils; 
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.swing.JRPButton;
import com.affymetrix.igb.swing.JRPTextField;
import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.HeadlessException;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import javafx.stage.FileChooser;
import javax.swing.GroupLayout;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import org.lorainelab.igb.javafx.FileChooserUtil;
import org.lorainelab.igb.synonymlookup.services.ChromosomeSynonymLookup;
import org.lorainelab.igb.synonymlookup.services.GenomeVersionSynonymLookup;
import org.lorainelab.igb.synonymlookup.services.SynonymLookupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(name = SynonymsControlPanel.COMPONENT_NAME, immediate = true, provide = SynonymsControlPanel.class)
public class SynonymsControlPanel {

    public static final String COMPONENT_NAME = "SynonymsControlPanel";
    private static final Logger logger = LoggerFactory.getLogger(SynonymsControlPanel.class);
    private static final String PREF_VSYN_FILE_URL = "Version Synonyms File URL";
    private static final String PREF_CSYN_FILE_URL = "Chromosome Synonyms File URL";
    private JPanel panel;
    private GenomeVersionSynonymLookup genomeVersionSynonymLookup;
    private ChromosomeSynonymLookup chrSynLookup;

    public SynonymsControlPanel() {

    }

    @Activate
    public void activate() {
        panel = initSynonymsPanel();
    }

    public JPanel getPanel() {
        return panel;
    }

    
    protected static File getSelectedFile() throws HeadlessException {
        // IGBF-1185: Provide File chooser UI in native OS file chooser style and 
        // allow user to select only text file. 
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Text files","*.txt",".TXT", ".Txt");
        Optional<File> selectedFile = FileChooserUtil.build()
                .setContext(FileTracker.DATA_DIR_TRACKER.getFile())
                .setTitle("Choose File")
                .setFileExtensionFilters(Lists.newArrayList(extFilter))
                .retrieveFileFromFxChooser();
        
        if (selectedFile.isPresent() && selectedFile.get()!= null) {
            FileTracker.DATA_DIR_TRACKER.setFile(selectedFile.get());
            return selectedFile.get();
        }
        return null;
    }

    private JPanel initSynonymsPanel() {
        final JPanel synonymsPanel = new JPanel();
        final GroupLayout layout = new GroupLayout(synonymsPanel);
        final JLabel vsynonymsLabel = new JLabel("Version Synonyms File");
        final JLabel csynonymsLabel = new JLabel("Chromosome Synonyms File");
        final JRPTextField vsynonymFile = new JRPTextField("DataLoadPrefsView_vsynonymFile", PreferenceUtils.getLocationsNode().get(PREF_VSYN_FILE_URL, ""));
        final JRPTextField csynonymFile = new JRPTextField("DataLoadPrefsView_csynonymFile", PreferenceUtils.getLocationsNode().get(PREF_CSYN_FILE_URL, ""));
        final JRPButton vopenFile = new JRPButton("DataLoadPrefsView_vopenFile", "\u2026");
        final JRPButton copenFile = new JRPButton("DataLoadPrefsView_copenFile", "\u2026");
        
        final ActionListener vlistener = e -> {
            if (e.getSource() == vopenFile) {
                File selectedFile = getSelectedFile();
                try {
                    if (selectedFile != null){
                        vsynonymFile.setText(selectedFile.getCanonicalPath());
                        
                        // IGBF-1187: Display messgae to restart IGB when version synonym file is selected
                        // and user has already selected spacies. If user sets synonym file
                        // and then selectes spacies, then there is no need to restart IGB.
                        String speciesName = GeneralLoadView.getLoadView().getSelectedSpecies();
                        GenomeVersion loadGroup = GenometryModel.getInstance().getSelectedGenomeVersion();
                        if (speciesName!= null && loadGroup!= null) {
                            String[] options = {"Quit IGB", "No, don't quit"};
                            if (JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(
                            PreferencesPanel.getSingleton(), 
                            "To start using your Personal Synonyms, quit and re-start IGB. \n" +
                            "Do you want to quit now?", "IGB Restart",
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                            options, options[1])) {
                                try {
                                    ((IGB) IGB.getInstance()).defaultCloseOperations();
                                    System.exit(0);
                                } catch (Exception ex) {
                                    com.affymetrix.genoviz.util.ErrorHandler.errorPanel("ERROR", "Error clearing preferences", ex);
                                }
                            }
                        }
                    }
                } catch (IOException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }

            if (vsynonymFile.getText().isEmpty() || loadSynonymFile(genomeVersionSynonymLookup, vsynonymFile)) {
                PreferenceUtils.getLocationsNode().put(PREF_VSYN_FILE_URL, vsynonymFile.getText());
            } else {
                ErrorHandler.errorPanel(
                        "Unable to Load Version Synonyms",
                        "Unable to load personal synonyms from " + vsynonymFile.getText() + ".", Level.SEVERE);
            }
        };

        final ActionListener clistener = e -> {
            if (e.getSource() == copenFile) {
                File selectedFile = getSelectedFile();
                try {
                    if (selectedFile != null) {
                        csynonymFile.setText(selectedFile.getCanonicalPath());
                        
                        // IGBF-1187: Display messgae to restart IGB when chromosome file is selected
                        // and user has already selected spacies. If user sets chromosome file
                        // and then selectes spacies, then there is no need to restart IGB.
                        String speciesName = GeneralLoadView.getLoadView().getSelectedSpecies();
                        GenomeVersion loadGroup = GenometryModel.getInstance().getSelectedGenomeVersion();
                        if (speciesName!= null && loadGroup!= null) {
                            String[] options = {"Quit IGB", "No, don't quit"};
                            if (JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(
                              PreferencesPanel.getSingleton(), 
                              "To start using your Personal Synonyms, quit and re-start IGB. \n" +
                              "Do you want to quit now?", "IGB Restart",
                              JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                              options, options[1])) {
                                try {
                                    ((IGB) IGB.getInstance()).defaultCloseOperations();
                                    System.exit(0);
                                } catch (Exception ex) {
                                    com.affymetrix.genoviz.util.ErrorHandler.errorPanel("ERROR", "Error clearing preferences", ex);
                                }
                            }
                        }
                    }
                } catch (IOException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }

            if (csynonymFile.getText().isEmpty() || loadSynonymFile(chrSynLookup, csynonymFile)) {
                PreferenceUtils.getLocationsNode().put(PREF_CSYN_FILE_URL, csynonymFile.getText());
            } else {
                ErrorHandler.errorPanel(
                        "Unable to Load Chromosome Synonyms",
                        "Unable to load personal synonyms from " + csynonymFile.getText() + ".", Level.SEVERE);
            }
        };

        vopenFile.setToolTipText("Open Local File");
        vopenFile.addActionListener(vlistener);
        vsynonymFile.addActionListener(vlistener);

        copenFile.setToolTipText("Open Local File");
        copenFile.addActionListener(clistener);
        csynonymFile.addActionListener(clistener);

        synonymsPanel.setLayout(layout);
        synonymsPanel.setBorder(new TitledBorder("Personal Synonyms"));
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(layout.createParallelGroup(LEADING).addGroup(layout.createSequentialGroup().addComponent(vsynonymsLabel).addComponent(vsynonymFile).addComponent(vopenFile)).addGroup(layout.createSequentialGroup().addComponent(csynonymsLabel).addComponent(csynonymFile).addComponent(copenFile)));

        layout.setVerticalGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(BASELINE).addComponent(vsynonymsLabel).addComponent(vsynonymFile).addComponent(vopenFile)).addGroup(layout.createParallelGroup(BASELINE).addComponent(csynonymsLabel).addComponent(csynonymFile).addComponent(copenFile)));

        /*
         * Load the synonym file from preferences on startup
         */
        loadSynonymFile(genomeVersionSynonymLookup, vsynonymFile);
        loadSynonymFile(chrSynLookup, csynonymFile);

        return synonymsPanel;
    }

    private static boolean loadSynonymFile(SynonymLookupService lookup, JRPTextField synonymFile) {
        File file = new File(synonymFile.getText());

        if (!file.isFile() || !file.canRead()) {
            return false;
        }

        FileInputStream fis = null;
        try {
            synonymFile.setText(file.getCanonicalPath());
            fis = new FileInputStream(file);
            lookup.loadSynonyms(fis);
        } catch (Throwable ex) {
            logger.warn(ex.getMessage(), ex);
            return false;
        } finally {
            GeneralUtils.safeClose(fis);
        }

        return true;
    }

    @Reference
    public void setGenomeVersionSynonymLookup(GenomeVersionSynonymLookup genomeVersionSynonymLookup) {
        this.genomeVersionSynonymLookup = genomeVersionSynonymLookup;
    }

    @Reference
    public void setChrSynLookup(ChromosomeSynonymLookup chrSynLookup) {
        this.chrSynLookup = chrSynLookup;
    }
}
