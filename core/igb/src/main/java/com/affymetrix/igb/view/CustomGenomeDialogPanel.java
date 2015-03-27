package com.affymetrix.igb.view;

import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.igb.shared.FileTracker;
import com.affymetrix.igb.shared.OpenURIAction;
import static com.affymetrix.igb.shared.OpenURIAction.CUSTOM_GENOME_COUNTER;
import static com.affymetrix.igb.shared.OpenURIAction.UNKNOWN_GENOME_PREFIX;
import static com.affymetrix.igb.shared.OpenURIAction.UNKNOWN_SPECIES_PREFIX;
import com.affymetrix.igb.swing.JRPFileChooser;
import com.google.common.base.Strings;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.swingx.prompt.PromptSupport;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class CustomGenomeDialogPanel extends JPanel {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CustomGenomeDialogPanel.class);
    private javax.swing.JButton refSeqBrowseButton;
    private javax.swing.JLabel refSeqLabel;
    private javax.swing.JTextField refSeqTextField;
    private javax.swing.JLabel speciesLabel;
    private javax.swing.JTextField speciesTextField;
    private javax.swing.JLabel versionLabel;
    private javax.swing.JTextField versionTextField;

    public CustomGenomeDialogPanel() {
        initComponents();
        layoutComponents();
        PromptSupport.setPrompt(UNKNOWN_SPECIES_PREFIX + " " + CUSTOM_GENOME_COUNTER + "     ", speciesTextField);
        PromptSupport.setPrompt(UNKNOWN_GENOME_PREFIX + " " + CUSTOM_GENOME_COUNTER + "     ", versionTextField);
        PromptSupport.setFocusBehavior(PromptSupport.FocusBehavior.HIDE_PROMPT, speciesTextField);
        PromptSupport.setFocusBehavior(PromptSupport.FocusBehavior.HIDE_PROMPT, versionTextField);
    }

    private void initComponents() {
        speciesLabel = new javax.swing.JLabel("Species");
        speciesTextField = new javax.swing.JTextField();
        versionLabel = new javax.swing.JLabel("Genome Version");
        versionTextField = new javax.swing.JTextField();
        refSeqLabel = new javax.swing.JLabel("Reference Sequence");
        refSeqTextField = new javax.swing.JTextField();
        refSeqBrowseButton = new javax.swing.JButton("...");
        refSeqBrowseButton.addActionListener(this::refSeqBrowseButtonActionPerformed);
    }

    private void layoutComponents() {
        this.setLayout(new MigLayout("fillx", "[]rel[grow]", "[][][]"));
        add(refSeqLabel);
        add(refSeqTextField, "growx");
        add(refSeqBrowseButton, "wrap");
        add(versionLabel, "");
        add(versionTextField, "growx, wrap");
        add(speciesLabel, "");
        add(speciesTextField, "growx");
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(375, 125);
    }

    private void refSeqBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {
        FileTracker fileTracker = FileTracker.DATA_DIR_TRACKER;
        JRPFileChooser chooser = new JRPFileChooser("newGenome", fileTracker.getFile());
        OpenURIAction.getSupportedFiles(FileTypeCategory.Sequence).stream().forEach(chooser::addChoosableFileFilter);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setMultiSelectionEnabled(false);
        int selection = chooser.showOpenDialog(this);
        if (selection != JFileChooser.APPROVE_OPTION) {
            return;
        }
        fileTracker.setFile(chooser.getCurrentDirectory());
        File selectedRefSeqFile = chooser.getSelectedFile();
        if (selectedRefSeqFile != null) {
            try {
                refSeqTextField.setText(selectedRefSeqFile.getCanonicalPath());
                if (Strings.isNullOrEmpty(versionTextField.getText())) {
                    versionTextField.setText("Custom Genome " + CUSTOM_GENOME_COUNTER);
                }
                if (Strings.isNullOrEmpty(speciesTextField.getText())) {
                    speciesTextField.setText("Custom Species " + CUSTOM_GENOME_COUNTER);
                }
            } catch (IOException ex) {
                logger.error("Error reading sequence file", ex);
            }
        }
    }

    public String getSpeciesName() {
        return speciesTextField.getText();
    }

    public String getVersionName() {
        return versionTextField.getText();
    }

    public String getRefSeqFile() {
        return refSeqTextField.getText();
    }
}
