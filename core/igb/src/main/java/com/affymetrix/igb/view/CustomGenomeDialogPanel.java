package com.affymetrix.igb.view;

import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.util.FileTracker;
import com.affymetrix.igb.shared.OpenURIAction;
import static com.affymetrix.igb.shared.OpenURIAction.CUSTOM_GENOME_COUNTER;
import static com.affymetrix.igb.shared.OpenURIAction.UNKNOWN_GENOME_PREFIX;
import com.affymetrix.igb.swing.JRPComboBox;
import com.affymetrix.igb.swing.JRPComboBoxWithSingleListener;
import com.affymetrix.igb.util.JComboBoxToolTipRenderer;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.awt.Color;
import org.lorainelab.igb.javafx.FileChooserUtil;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.stage.FileChooser;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
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
    private javax.swing.JLabel genusLabel;
    private javax.swing.JTextField genusTextField;
    private javax.swing.JLabel speciesLabel;
    private javax.swing.JTextField speciesTextField;
    private javax.swing.JLabel varietyLabel;
    private javax.swing.JTextField varietyTextField;
    private javax.swing.JTextField yearTextField;
    private javax.swing.JLabel monthLabel;
    private static final String[] months = {"Month", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    private JRPComboBox monthComboBox;
    private javax.swing.JLabel versionLabel;
    private javax.swing.JTextField versionTextField;
    private JComboBoxToolTipRenderer speciesCBRenderer;
    private LocalDate cdate;
    private String defaultVersion = "Genus_species__MMM_YYYY";

    public CustomGenomeDialogPanel() {
        initComponents();
        layoutComponents();      
        PromptSupport.setPrompt(UNKNOWN_GENOME_PREFIX + " " + CUSTOM_GENOME_COUNTER + "     ", versionTextField);
        PromptSupport.setPrompt("Enter Genus", genusTextField);
        PromptSupport.setPrompt("Enter Species", speciesTextField);
        PromptSupport.setPrompt("(Optional)-strain/cultivar/accession", varietyTextField);
        PromptSupport.setPrompt("Enter Year", yearTextField);
        PromptSupport.setFocusBehavior(PromptSupport.FocusBehavior.HIDE_PROMPT, versionTextField);
    }

    private void initComponents() {
        cdate = LocalDate.now();
        genusLabel = new javax.swing.JLabel("Genus");
        genusTextField = new javax.swing.JTextField();
        
        speciesLabel = new javax.swing.JLabel("Species");
        speciesTextField = new javax.swing.JTextField();
        
        monthLabel = new javax.swing.JLabel("Month/Year");
        monthComboBox = new JRPComboBoxWithSingleListener("Month",months);
        
        speciesLabel = new javax.swing.JLabel("Species");
        speciesTextField = new javax.swing.JTextField();
        yearTextField = new javax.swing.JTextField();
        
        varietyLabel = new javax.swing.JLabel("Variety");
        varietyTextField = new javax.swing.JTextField();
        
        versionLabel = new javax.swing.JLabel("Version");
        versionTextField = new javax.swing.JTextField();
        versionTextField.setEditable(false);
        
        refSeqLabel = new javax.swing.JLabel("Reference Sequence");
        refSeqTextField = new javax.swing.JTextField();
        validateTextFields();
        refSeqBrowseButton = new javax.swing.JButton("Choose File\u2026");
        refSeqBrowseButton.addActionListener(this::refSeqBrowseButtonActionPerformed);
    }
    private void validateTextFields(){
        final String version[] = defaultVersion.split("_");
        monthComboBox.setEnabled(true);
        monthComboBox.setEditable(false); 
        Border defaultB = genusTextField.getBorder();
        genusTextField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = ((JTextField)input).getText();
                genusTextField.setText(text.substring(0,1).toUpperCase()+text.substring(1).toLowerCase());
                try{
                    if(text.matches("^[a-zA-z]+$")){                        
                        genusTextField.setBorder(defaultB);
                        version[0]=text.toUpperCase().charAt(0)+"";
                        versionTextField.setText(joinVersionValues(version));
                    }else{
                        versionTextField.setText("");
                        throw new Exception("Error");
                    }
                    return true;      
                }catch(Exception e){
                    Border border = new LineBorder(Color.red,2,true);
                    genusTextField.setBorder(border);
                    return true;
                }
            } 
        });
        speciesTextField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = ((JTextField)input).getText();
                try{
                    if(text.matches("^[a-zA-z]+$")){                        
                        speciesTextField.setBorder(defaultB);
                        version[1] = text.toLowerCase();
                        versionTextField.setText(joinVersionValues(version));
                    }else{
                        versionTextField.setText("");
                        throw new Exception("Error");
                    }
                    return true;      
                }catch(Exception e){
                    Border border = new LineBorder(Color.red,2,true);
                    speciesTextField.setBorder(border);
                    return true;
                }
            } 
        });
        varietyTextField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = ((JTextField)input).getText();
                try{                     
                    version[2] = text.toLowerCase();
                    versionTextField.setText(joinVersionValues(version));
                    return true;      
                }catch(Exception e){
                    return true;
                }
            } 
        });
        monthComboBox.addItemListener((ItemEvent e) -> {
            String monthSelected = monthComboBox.getSelectedItem().toString();
            if (!monthSelected.equalsIgnoreCase("Month")) {
                version[3] = monthSelected.substring(0,3);
                versionTextField.setText(joinVersionValues(version));
            }
        });
         yearTextField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = ((JTextField)input).getText();
                try{
                    if(text.matches("^[0-9]{4}+$")){                        
                        yearTextField.setBorder(defaultB);
                        version[4] = text;
                        versionTextField.setText(joinVersionValues(version));
                    }else{
                        versionTextField.setText("");
                        throw new Exception("Error");
                    }
                    return true;      
                }catch(Exception e){
                    Border border = new LineBorder(Color.red,2,true);
                    yearTextField.setBorder(border);
                    return true;
                }
            } 
        });
        
    }
    private String joinVersionValues(String version[]){
        String vers = String.join("_",version);
        vers = vers.replace("__","_");
        return vers;
    }
    private void layoutComponents() {
        this.setLayout(new MigLayout("fillx", "[]rel[grow]", "[][][]"));
        add(refSeqLabel);
        add(refSeqTextField, "growx");
        add(refSeqBrowseButton, "wrap");
        add(genusLabel, "");
        add(genusTextField, "growx, wrap");
        add(speciesLabel, "");
        add(speciesTextField, "growx, wrap");
        add(varietyLabel,"");
        add(varietyTextField, "growx, wrap");
        add(monthLabel,"");
        add(monthComboBox,"split 2");
        add(yearTextField,"growx,wrap");
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
        return new Dimension(575, 210);
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
    public String getGenusName(){
        return genusTextField.getText().substring(0,1).toUpperCase()+genusTextField.getText().substring(1);
    }

    public String getSpeciesName() {
        String specieseName= "";
        if(!(genusTextField.getText().isEmpty() || speciesTextField.getText().isEmpty() || varietyTextField.getText().isEmpty())){
            specieseName = genusTextField.getText().substring(0,1).toUpperCase()+genusTextField.getText().substring(1);
            specieseName = specieseName+" "+speciesTextField.getText().toLowerCase() + " "+varietyTextField.getText().toLowerCase();
            return specieseName;
        }else{
          return speciesTextField.getText();   
        }      
    }

    public String getVersionName() {
        return versionTextField.getText();
    }

    public String getRefSeqFile() {
        return refSeqTextField.getText();
    }
}
