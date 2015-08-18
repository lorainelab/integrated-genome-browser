package com.affymetrix.igb.general;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.FileTracker;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.igb.swing.JRPButton;
import com.affymetrix.igb.swing.JRPTextField;
import com.lorainelab.igb.synonymlookup.services.ChromosomeSynonymLookup;
import com.lorainelab.igb.synonymlookup.services.DefaultSynonymLookup;
import com.lorainelab.igb.synonymlookup.services.SynonymLookupService;
import java.awt.HeadlessException;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import javax.swing.GroupLayout;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import javax.swing.JFileChooser;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.DIRECTORIES_ONLY;
import static javax.swing.JFileChooser.FILES_AND_DIRECTORIES;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
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
    private DefaultSynonymLookup defSynLookup;
    private ChromosomeSynonymLookup chrSynLookup;

    public SynonymsControlPanel() {
        panel = initSynonymsPanel();
    }

    public JPanel getPanel() {
        return panel;
    }

    protected static File fileChooser(int mode) throws HeadlessException {
        JFileChooser chooser = new JFileChooser();

        chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());
        chooser.setFileSelectionMode(mode);
        chooser.setDialogTitle("Choose " + (mode == DIRECTORIES_ONLY ? "Directory" : "File"));
        chooser.setAcceptAllFileFilterUsed(mode != DIRECTORIES_ONLY);
        chooser.rescanCurrentDirectory();

        if (chooser.showOpenDialog(null) != APPROVE_OPTION) {
            return null;
        }

        return chooser.getSelectedFile();
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
                File file = fileChooser(FILES_AND_DIRECTORIES);
                try {
                    if (file != null) {
                        vsynonymFile.setText(file.getCanonicalPath());

                    }
                } catch (IOException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }

            if (vsynonymFile.getText().isEmpty() || loadSynonymFile(defSynLookup, vsynonymFile)) {
                PreferenceUtils.getLocationsNode().put(PREF_VSYN_FILE_URL, vsynonymFile.getText());
            } else {
                ErrorHandler.errorPanel(
                        "Unable to Load Version Synonyms",
                        "Unable to load personal synonyms from " + vsynonymFile.getText() + ".", Level.SEVERE);
            }
        };

        final ActionListener clistener = e -> {
            if (e.getSource() == copenFile) {
                File file = fileChooser(FILES_AND_DIRECTORIES);
                try {
                    if (file != null) {
                        csynonymFile.setText(file.getCanonicalPath());

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

        vopenFile.setToolTipText("Open Local Directory");
        vopenFile.addActionListener(vlistener);
        vsynonymFile.addActionListener(vlistener);

        copenFile.setToolTipText("Open Local Directory");
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
        loadSynonymFile(defSynLookup, vsynonymFile);
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
        } catch (IOException ex) {
            return false;
        } finally {
            GeneralUtils.safeClose(fis);
        }

        return true;
    }

    @Reference
    public void setDefSynLookup(DefaultSynonymLookup defSynLookup) {
        this.defSynLookup = defSynLookup;
    }

    @Reference
    public void setChrSynLookup(ChromosomeSynonymLookup chrSynLookup) {
        this.chrSynLookup = chrSynLookup;
    }
}
