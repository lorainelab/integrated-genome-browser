package com.affymetrix.igb.view;

import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.util.FileTracker;
import com.affymetrix.igb.shared.OpenURIAction;
import static com.affymetrix.igb.shared.OpenURIAction.CUSTOM_GENOME_COUNTER;
import static com.affymetrix.igb.shared.OpenURIAction.UNKNOWN_GENOME_PREFIX;
import com.affymetrix.igb.swing.JRPComboBox;
import com.affymetrix.igb.swing.JRPComboBoxWithSingleListener;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.lorainelab.igb.javafx.FileChooserUtil;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.stage.FileChooser;
import javax.swing.BorderFactory;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
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
    private final String defaultVersion = "Genus_species__MMM_YYYY";

    public CustomGenomeDialogPanel() {
        initComponents();
        layoutComponents();      
        PromptSupport.setPrompt(UNKNOWN_GENOME_PREFIX + " " + CUSTOM_GENOME_COUNTER + "     ", versionTextField);
        PromptSupport.setPrompt("Enter genus", genusTextField);
        PromptSupport.setPrompt("Enter species", speciesTextField);
        PromptSupport.setPrompt("Enter strain/cultivar/accession", varietyTextField);
        PromptSupport.setPrompt("Enter year", yearTextField);
        PromptSupport.setPrompt("Enter URL or choose a local file", refSeqTextField);
        PromptSupport.setFocusBehavior(PromptSupport.FocusBehavior.HIDE_PROMPT, versionTextField);
    }

    private void initComponents() {
        genusLabel = new javax.swing.JLabel("Genus (optional)");
        genusTextField = new javax.swing.JTextField();
        
        speciesLabel = new javax.swing.JLabel("Species (optional)");
        speciesTextField = new javax.swing.JTextField();
        
        monthLabel = new javax.swing.JLabel("Genome release date (optional)");
        monthComboBox = new JRPComboBoxWithSingleListener("Month",months);        
        yearTextField = new javax.swing.JTextField();
        
        varietyLabel = new javax.swing.JLabel("Variety (optional)");
        varietyTextField = new javax.swing.JTextField();
        
        versionLabel = new javax.swing.JLabel("Version");
        versionTextField = new javax.swing.JTextField();
//        versionTextField.setEditable(false);
        
        refSeqLabel = new javax.swing.JLabel("<html>Reference Sequence <font color='red'>*</font></html> ");
        refSeqTextField = new javax.swing.JTextField();
        validateTextFields();
        refSeqBrowseButton = new javax.swing.JButton("Browse\u2026");
        refSeqBrowseButton.addActionListener(this::refSeqBrowseButtonActionPerformed);
    }
    private void validateTextFields(){
        final String version[] = defaultVersion.split("_");
        monthComboBox.setEnabled(true);
//        monthComboBox.setEditable(false); 
        Border defaultB = genusTextField.getBorder();
        genusTextField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = ((JTextField)input).getText();
                if(text.isEmpty()){
                    version[0] = "Genus";
                    versionTextField.setText(joinVersionValues(version));
                    return true;
                }else{
                    genusTextField.setText(text.toLowerCase());
                }
                versionTextField.setText("");
                try{
                    version[0]=text.toUpperCase().charAt(0)+"";
                    versionTextField.setText(joinVersionValues(version));
                    return true;      
                }catch(Exception e){
                    return true;
                }
            } 
        });
        speciesTextField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = ((JTextField)input).getText();
                if(text.isEmpty()){
                     version[1] = "species";
                     versionTextField.setText(joinVersionValues(version));
                     return true;
                }else{
                    speciesTextField.setText(text.toLowerCase());
                }
                versionTextField.setText("");               
                try{
                    version[1] = text.toLowerCase();
                    versionTextField.setText(joinVersionValues(version));
                    return true;      
                }catch(Exception e){
                    return true;
                }
            } 
        });
        varietyTextField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = ((JTextField)input).getText();
                versionTextField.setText("");
                version[2] = "";
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
            }else{
                version[3]="MMM";
            }
             versionTextField.setText(joinVersionValues(version));
        });
         yearTextField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = ((JTextField)input).getText();
                if(text.isEmpty()){
                     version[4] = "YYYY";
                     versionTextField.setText(joinVersionValues(version));
                     return true;
                }
                versionTextField.setText("");               
                try{
                    if(text.matches("^[0-9]+$")|| text.isEmpty()){
                        if(text.length()==4){
                            yearTextField.setBorder(defaultB);
                            version[4] = text;
                            versionTextField.setText(joinVersionValues(version));
                        }else{ 
                            if(text.length()>4){
                                version[4]=text.substring(0,4);
                                versionTextField.setText(joinVersionValues(version));
                            }else{
                                throw new Exception("Error");
                            }
                        }
                    }else{                        
                        throw new Exception("Error");
                    }
                    return true;      
                }catch(Exception e){
                    return true;
                }
            } 
        });
        
    }
    private String joinVersionValues(String version[]){
        String vers = String.join("_",version);
        vers = vers.replace("__","_");
        if(vers.equalsIgnoreCase("Genus_species_MMM_YYYY"))
            return "Custom Genome " + CUSTOM_GENOME_COUNTER;
        return vers;
    }
    private void layoutComponents() {
        this.setLayout(new MigLayout("fillx"));
        
        JPanel referencePane = new JPanel();
        referencePane.setLayout(new MigLayout("fillx","[]rel[grow][]"));
        referencePane.add(refSeqLabel);
        referencePane.add(refSeqTextField,"growx");
        referencePane.add(refSeqBrowseButton);
        add(referencePane,"growx,wrap");
        
        JPanel optionalPane = new JPanel();
        optionalPane.setLayout(new MigLayout("fillx,insets 0 20 0 20","[]rel[grow]",""));
        Border border = BorderFactory.createTitledBorder("Genome Details (all fields are optional)");
        optionalPane.setBorder(border);
        optionalPane.add(genusLabel, "");
        optionalPane.add(genusTextField, "growx,wrap");
        optionalPane.add(speciesLabel, "");
        optionalPane.add(speciesTextField, "growx,wrap");
        optionalPane.add(varietyLabel,"");
        optionalPane.add(varietyTextField, "growx,wrap");
        optionalPane.add(monthLabel,"");
        optionalPane.add(monthComboBox,"split 2,growx");
        optionalPane.add(yearTextField,"growx,wrap");
       
        add(optionalPane,"growx,wrap,gapright 50");
        add(new JLabel("<html><font color='red'>* </font>Required field</html>"),"growx,wrap");
        add(new JLabel("If you choose to include the optional details, IGB will display the details in the species and genome menus."),"growx,wrap");
   


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
    private void refSeqBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {
        FileTracker fileTracker = FileTracker.DATA_DIR_TRACKER;
        List<String> supportedExtensions = OpenURIAction.getSupportedFiles(FileTypeCategory.Sequence).stream()
                .flatMap(filter -> filter.getExtensions().stream())
                .map(ext -> "*." + ext).collect(Collectors.toList());
        final String extensionInfo = Joiner.on(",").join(supportedExtensions);
        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Sequence file ()", supportedExtensions);
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
        String speciesName= "";
        if(!(genusTextField.getText().isEmpty() || speciesTextField.getText().isEmpty())){
            speciesName = genusTextField.getText().substring(0,1).toUpperCase()+genusTextField.getText().substring(1);
            speciesName = speciesName+" "+speciesTextField.getText().toLowerCase();
            return speciesName;
        }else if(!speciesName.isEmpty() || !varietyTextField.getText().isEmpty()){
            return speciesName+" "+varietyTextField.getText().toLowerCase();
        }else{
            return speciesName;  
        }
    }

    public String getVersionName() {
        return versionTextField.getText();
    }

    public String getRefSeqFile() {
        return refSeqTextField.getText();
    }
}
