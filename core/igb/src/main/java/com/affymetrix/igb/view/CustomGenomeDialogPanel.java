package com.affymetrix.igb.view;

import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.util.FileTracker;
import com.affymetrix.igb.shared.OpenURIAction;
import static com.affymetrix.igb.shared.OpenURIAction.CUSTOM_GENOME_COUNTER;
import static com.affymetrix.igb.shared.OpenURIAction.UNKNOWN_GENOME_PREFIX;
import com.affymetrix.igb.swing.JRPComboBox;
import com.affymetrix.igb.swing.JRPComboBoxWithSingleListener;
import com.affymetrix.igb.util.JComboBoxToolTipRenderer;
import static com.affymetrix.igb.view.SeqGroupView.SELECT_SPECIES;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.jidesoft.editor.DocumentListenerEx;
import org.lorainelab.igb.javafx.FileChooserUtil;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.stage.FileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.swingx.prompt.PromptSupport;
import org.lorainelab.igb.synonymlookup.services.SpeciesSynonymsLookup;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
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
    private javax.swing.JLabel monthLabel;
    private JRPComboBox speciesComboBox;
    private static final String[] months = {"Month", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    private JRPComboBox monthComboBox;
    private javax.swing.JLabel versionLabel;
    private javax.swing.JTextField versionTextField;
    private JComboBoxToolTipRenderer speciesCBRenderer;
    private LocalDate cdate;

    public CustomGenomeDialogPanel() {
        initComponents();
        layoutComponents();      
        PromptSupport.setPrompt(UNKNOWN_GENOME_PREFIX + " " + CUSTOM_GENOME_COUNTER + "     ", versionTextField);
        PromptSupport.setFocusBehavior(PromptSupport.FocusBehavior.HIDE_PROMPT, versionTextField);
    }

    private void initComponents() {
        cdate = LocalDate.now();
        speciesLabel = new javax.swing.JLabel("Species");
        speciesComboBox = new JRPComboBoxWithSingleListener("DataAccess_species");
        monthLabel = new javax.swing.JLabel("Month");
        monthComboBox = new JRPComboBoxWithSingleListener("Month",months);
        versionLabel = new javax.swing.JLabel("Genome Version");
        versionTextField = new javax.swing.JTextField();
        refSeqLabel = new javax.swing.JLabel("Reference Sequence");
        refSeqTextField = new javax.swing.JTextField();
        addComboBoxItems();
        refSeqBrowseButton = new javax.swing.JButton("Choose File\u2026");
        refSeqBrowseButton.addActionListener(this::refSeqBrowseButtonActionPerformed);
    }
    private void addComboBoxItems(){
        final List<String> speciesList = GeneralLoadUtils.getSpeciesList();
        speciesComboBox.addItem(SELECT_SPECIES);
        speciesComboBox.setMaximumSize(new Dimension(speciesComboBox.getPreferredSize().width * 4, speciesComboBox.getPreferredSize().height));
        speciesComboBox.setEnabled(true);
        speciesComboBox.setEditable(false);
        speciesComboBox.setToolTipText("Choose" + " " + SELECT_SPECIES);
        speciesCBRenderer = new JComboBoxToolTipRenderer();
        speciesComboBox.setRenderer(speciesCBRenderer);
        speciesCBRenderer.setToolTipEntry(SELECT_SPECIES, "Choose" + " " + SELECT_SPECIES);
        
        monthComboBox.setMaximumSize(new Dimension(speciesComboBox.getPreferredSize().width * 4, speciesComboBox.getPreferredSize().height));
        monthComboBox.setEnabled(true);
        monthComboBox.setEditable(false); 

        Bundle bundle = FrameworkUtil.getBundle(SeqGroupView.class);
        SpeciesSynonymsLookup speciesSynLookup = null;
        if (bundle != null) {
            BundleContext bundleContext = bundle.getBundleContext();
            ServiceReference<SpeciesSynonymsLookup> serviceReference = bundleContext.getServiceReference(SpeciesSynonymsLookup.class);
            speciesSynLookup = bundleContext.getService(serviceReference);
        }
        for (String speciesName : speciesList) {
                speciesCBRenderer.setToolTipEntry(speciesName, speciesSynLookup.getCommonSpeciesName(speciesName));
                speciesComboBox.addItem(speciesName);
        }
        
        speciesComboBox.addItemListener((ItemEvent e) -> {
            String[] versionSplit = getSpeciesName().split(" ");
            String mon = "";
            if (monthComboBox.getSelectedItem().toString().equalsIgnoreCase("Month")){
                mon = cdate.getMonth().toString().charAt(0) + cdate.getMonth().toString().substring(1,3).toLowerCase();
            }else{
                mon = monthComboBox.getSelectedItem().toString().substring(0,3);
            }
            String versionName =versionSplit[0].charAt(0)+"_"+versionSplit[1]+"_"+mon+"_"+cdate.getYear();
            versionTextField.setText(versionName);
        });
        monthComboBox.addItemListener((ItemEvent e) -> {
            if (!monthComboBox.getSelectedItem().toString().equalsIgnoreCase("Month")) {
                if(!getSpeciesName().equals("Species")){
                    String[] versionSplit = getSpeciesName().split(" ");
                    String versionName = versionSplit[0].charAt(0)+"_"+versionSplit[1]+"_"+monthComboBox.getSelectedItem().toString().substring(0,3)+"_" + cdate.getYear();
                    versionTextField.setText(versionName);
                }
            }
        }); 
    }
    private void layoutComponents() {
        this.setLayout(new MigLayout("fillx", "[]rel[grow]", "[][][]"));
        add(refSeqLabel);
        add(refSeqTextField, "growx");
        add(refSeqBrowseButton, "wrap");
        add(speciesLabel, "");
        add(speciesComboBox, "growx, wrap");
        add(monthLabel);
        add(monthComboBox,"growx,wrap");
        add(versionLabel, "");
        add(versionTextField, "growx, wrap");


        this.addHierarchyListener((HierarchyEvent e) -> {
            Window window = SwingUtilities.getWindowAncestor(CustomGenomeDialogPanel.this);
            if (window instanceof Dialog) {
                Dialog dialog = (Dialog) window;
                if (!dialog.isResizable()) {
                    dialog.setResizable(true);
                }
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(575, 175);
    }

    private void refSeqBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {
        FileTracker fileTracker = FileTracker.DATA_DIR_TRACKER;
        List<String> supportedExtensions = OpenURIAction.getSupportedFiles(FileTypeCategory.Sequence).stream()
                .flatMap(filter -> filter.getExtensions().stream())
                .map(ext -> "*." + ext).collect(Collectors.toList());
        final String extensionInfo = Joiner.on(",").join(supportedExtensions);
        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Sequence file (" + extensionInfo + ")", supportedExtensions);
        Optional<File> selectedRefSeqFile = FileChooserUtil.build()
                .setContext(fileTracker.getFile())
                .setTitle("Choose Reference Sequence File")
                .setFileExtensionFilters(Lists.newArrayList(extensionFilter))
                .retrieveFileFromFxChooser();
        if (selectedRefSeqFile.isPresent()) {
            fileTracker.setFile(selectedRefSeqFile.get().getParentFile());
            try {
                refSeqTextField.setText(selectedRefSeqFile.get().getCanonicalPath());
                if (Strings.isNullOrEmpty(versionTextField.getText())) {
                    versionTextField.setText("Custom Genome " + CUSTOM_GENOME_COUNTER);
                }
            } catch (IOException ex) {
                logger.error("Error reading sequence file", ex);
            }
        }
    }

    public String getSpeciesName() {
        return speciesComboBox.getSelectedItem().toString();
    }

    public String getVersionName() {
        return versionTextField.getText();
    }

    public String getRefSeqFile() {
        return refSeqTextField.getText();
    }
}
