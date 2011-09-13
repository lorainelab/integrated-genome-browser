/*
 * QuickloadSourceWizardView.java
 */

package quickloadsourcewizard;

import java.awt.Color;
import java.awt.event.InputMethodEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputMethodListener;
import javax.swing.BorderFactory;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.border.Border;

/**
 * The application's main frame.
 */
public class QuickloadSourceWizardView extends FrameView {

    public QuickloadSourceWizardView(SingleFrameApplication app) {
        super(app);

        initComponents();
        
        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String)(evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer)(evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });
    }
    
    public boolean validateOutput(){
        if( InputValid && OutputValid ){
            return true;
        }else{
            //show dialog
            if( !InputValid && ! OutputValid){
                JOptionPane.showMessageDialog( null, "The input and output paths are invalid.");
            }else if( !InputValid){
                JOptionPane.showMessageDialog( null, "The input path is invalid.");
            }else
            if( !OutputValid){
                JOptionPane.showMessageDialog( null, "The output path is invalid.");
            }
            return false;
        }
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = QuickloadSourceWizardApp.getApplication().getMainFrame();
            aboutBox = new QuickloadSourceWizardAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        QuickloadSourceWizardApp.getApplication().show(aboutBox);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        create_sources = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        OutputPath = new javax.swing.JTextField();
        OutputPathBrowse = new javax.swing.JButton();
        AdvanceCreateButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        InputPath = new javax.swing.JTextField();
        InputPathBrowse = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        SpeciesName = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        jLabel3 = new javax.swing.JLabel();
        SpeciesDescriptionTextField = new javax.swing.JTextField();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();

        mainPanel.setName("mainPanel"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(quickloadsourcewizard.QuickloadSourceWizardApp.class).getContext().getResourceMap(QuickloadSourceWizardView.class);
        create_sources.setText(resourceMap.getString("create_sources.text")); // NOI18N
        create_sources.setName("create_sources"); // NOI18N
        create_sources.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                create_sourcesActionPerformed(evt);
            }
        });

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N

        OutputPath.setText(resourceMap.getString("OutputPath.text")); // NOI18N
        OutputPath.setName("OutputPath"); // NOI18N
        OutputPath.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                OutputPathCaretUpdate(evt);
            }
        });

        OutputPathBrowse.setText(resourceMap.getString("OutputPathBrowse.text")); // NOI18N
        OutputPathBrowse.setName("OutputPathBrowse"); // NOI18N
        OutputPathBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OutputPathBrowseActionPerformed(evt);
            }
        });

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(quickloadsourcewizard.QuickloadSourceWizardApp.class).getContext().getActionMap(QuickloadSourceWizardView.class, this);
        AdvanceCreateButton.setAction(actionMap.get("showAttributesDialog")); // NOI18N
        AdvanceCreateButton.setText(resourceMap.getString("AdvanceCreateButton.text")); // NOI18N
        AdvanceCreateButton.setName("AdvanceCreateButton"); // NOI18N
        AdvanceCreateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AdvanceCreateButtonActionPerformed(evt);
            }
        });

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        InputPath.setText(resourceMap.getString("InputPath.text")); // NOI18N
        InputPath.setName("InputPath"); // NOI18N
        InputPath.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                InputPathCaretUpdate(evt);
            }
        });
        InputPath.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                InputPathActionPerformed(evt);
            }
        });
        InputPath.addInputMethodListener(new java.awt.event.InputMethodListener() {
            public void caretPositionChanged(java.awt.event.InputMethodEvent evt) {
                InputPathCaretPositionChanged(evt);
            }
            public void inputMethodTextChanged(java.awt.event.InputMethodEvent evt) {
                InputPathInputMethodTextChanged(evt);
            }
        });

        InputPathBrowse.setAction(actionMap.get("browseInputPath")); // NOI18N
        InputPathBrowse.setText(resourceMap.getString("InputPathBrowse.text")); // NOI18N
        InputPathBrowse.setName("InputPathBrowse"); // NOI18N
        InputPathBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                InputPathBrowseActionPerformed(evt);
            }
        });

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        SpeciesName.setText(resourceMap.getString("SpeciesName.text")); // NOI18N
        SpeciesName.setName("SpeciesName"); // NOI18N

        jSeparator1.setName("jSeparator1"); // NOI18N

        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N

        jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
        jLabel6.setName("jLabel6"); // NOI18N

        jSeparator2.setName("jSeparator2"); // NOI18N

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N

        SpeciesDescriptionTextField.setText(resourceMap.getString("SpeciesDescriptionTextField.text")); // NOI18N
        SpeciesDescriptionTextField.setName("SpeciesDescriptionTextField"); // NOI18N

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 816, Short.MAX_VALUE)
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel1)
                                    .addComponent(jLabel2))
                                .addGap(21, 21, 21)
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(InputPath)
                                    .addComponent(SpeciesName, javax.swing.GroupLayout.DEFAULT_SIZE, 342, Short.MAX_VALUE)
                                    .addComponent(SpeciesDescriptionTextField, javax.swing.GroupLayout.Alignment.LEADING))
                                .addGap(18, 18, 18)
                                .addComponent(InputPathBrowse))
                            .addComponent(jLabel5))
                        .addContainerGap(38, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                        .addComponent(AdvanceCreateButton)
                        .addGap(65, 65, 65)
                        .addComponent(create_sources)
                        .addContainerGap())
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 817, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(37, Short.MAX_VALUE))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addContainerGap(796, Short.MAX_VALUE))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 169, Short.MAX_VALUE)
                        .addComponent(OutputPath, javax.swing.GroupLayout.PREFERRED_SIZE, 345, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(OutputPathBrowse)
                        .addGap(64, 64, 64))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addContainerGap(715, Short.MAX_VALUE))))
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(InputPath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(InputPathBrowse))
                .addGap(18, 18, 18)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel2)
                    .addComponent(SpeciesName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(28, 28, 28)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(SpeciesDescriptionTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(38, 38, 38)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(OutputPath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(OutputPathBrowse))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 48, Short.MAX_VALUE)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(AdvanceCreateButton)
                    .addComponent(create_sources))
                .addGap(24, 24, 24))
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 866, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 682, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    /*
     * This button will go through the files and create the quickload site.
     * The first implementation will overwrite the quickload site with the new 
     * information.
     * 
     * The second implementation will ensure appending to a site, 
     */
    private void create_sourcesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_create_sourcesActionPerformed
        if( validateOutput() ){
            String input = InputPath.getText();
            String output = OutputPath.getText();
            String species = SpeciesName.getText();

            QuickloadSourceCreator creator = new QuickloadSourceCreator();
            creator.setOutputPath(output);
            creator.setSourcePath(input);
            creator.setSpeciesName(species);
            try {
                //basic file creation with place holders
                creator.generateFiles(null, null);

            } catch (ParserConfigurationException ex) {
                Logger.getLogger(QuickloadSourceWizardView.class.getName()).log(Level.SEVERE, null, ex);
            } catch (TransformerConfigurationException ex) {
                Logger.getLogger(QuickloadSourceWizardView.class.getName()).log(Level.SEVERE, null, ex);
            } catch (TransformerException ex) {
                Logger.getLogger(QuickloadSourceWizardView.class.getName()).log(Level.SEVERE, null, ex);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(QuickloadSourceWizardView.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(QuickloadSourceWizardView.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(QuickloadSourceWizardView.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_create_sourcesActionPerformed
    @Action
    private void OutputPathBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OutputPathBrowseActionPerformed
        //show file browser with option to select multiple files.
        
        JFileChooser file_chooser = new JFileChooser(System.getProperty( "user.home" ) );
        File from_text_field = new File(OutputPath.getText());
        if( from_text_field.exists()){
            file_chooser.setCurrentDirectory( from_text_field );
        }
        file_chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = file_chooser.showOpenDialog( InputPath );
        
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = file_chooser.getSelectedFile();
            OutputPath.setText(file.getPath() );
            //This is where a real application would open the file.
            
        } 
        
    }//GEN-LAST:event_OutputPathBrowseActionPerformed
    
    @Action
    private void InputPathBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_InputPathBrowseActionPerformed
        
        JFileChooser file_chooser = new JFileChooser(System.getProperty( "user.home" ) );
        
        File from_text_field = new File(InputPath.getText());
        if( from_text_field.exists()){
            file_chooser.setCurrentDirectory( from_text_field );
        }
        
        file_chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = file_chooser.showOpenDialog( InputPath );
        
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = file_chooser.getSelectedFile();
            InputPath.setText(file.getPath() );
            //This is where a real application would open the file.
            
        } 
        
    }//GEN-LAST:event_InputPathBrowseActionPerformed

    private void AdvanceCreateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AdvanceCreateButtonActionPerformed
        if( validateOutput() ){
            String input = InputPath.getText();
            String output = OutputPath.getText();
            String species = SpeciesName.getText();
            String species_description = this.SpeciesDescriptionTextField.getText();

            QuickloadSourceCreator creator = new QuickloadSourceCreator();
            creator.setOutputPath(output);
            creator.setSourcePath(input);
            creator.setSpeciesName(species);
            try {
                creator.searchInputForAnnotSeqFiles();

                //if ( mAttributesDialog == null) {
                    JFrame mainFrame = QuickloadSourceWizardApp.getApplication().getMainFrame();
                    mAttributesDialog = new AttributesDialog(mainFrame, true, creator
                                                            , species_description
                                                            , input, output, species);
                    mAttributesDialog.setLocationRelativeTo(mainFrame);
                //}
                QuickloadSourceWizardApp.getApplication().show( mAttributesDialog );




            } catch (IOException ex) {
                Logger.getLogger(QuickloadSourceWizardView.class.getName()).log(Level.SEVERE, null, ex);
                //show dialog with an error message.
            }
        }
    }//GEN-LAST:event_AdvanceCreateButtonActionPerformed

    private void InputPathCaretPositionChanged(java.awt.event.InputMethodEvent evt) {//GEN-FIRST:event_InputPathCaretPositionChanged
     
    }//GEN-LAST:event_InputPathCaretPositionChanged

    private void InputPathInputMethodTextChanged(java.awt.event.InputMethodEvent evt) {//GEN-FIRST:event_InputPathInputMethodTextChanged
        
    }//GEN-LAST:event_InputPathInputMethodTextChanged

    private void InputPathActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_InputPathActionPerformed

    }//GEN-LAST:event_InputPathActionPerformed

    private void InputPathCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_InputPathCaretUpdate

        if( new File( InputPath.getText() ).exists() &&  new File( InputPath.getText() ).isDirectory()){
            InputValid = true;
            InputPath.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.green) );
        }else{
            InputValid = false;
            InputPath.setBorder( BorderFactory.createMatteBorder( 1, 1, 1, 1, Color.red));
        }
    }//GEN-LAST:event_InputPathCaretUpdate

    private void OutputPathCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_OutputPathCaretUpdate

        if( new File( OutputPath.getText() ).exists() &&  new File( OutputPath.getText() ).isDirectory()){
            OutputValid = true;
            OutputPath.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.green) );
        }else{
            OutputValid = false;
            OutputPath.setBorder( BorderFactory.createMatteBorder( 1, 1, 1, 1, Color.red));
        }
    }//GEN-LAST:event_OutputPathCaretUpdate

    boolean InputValid;
    boolean OutputValid;
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton AdvanceCreateButton;
    private javax.swing.JTextField InputPath;
    private javax.swing.JButton InputPathBrowse;
    private javax.swing.JTextField OutputPath;
    private javax.swing.JButton OutputPathBrowse;
    private javax.swing.JTextField SpeciesDescriptionTextField;
    private javax.swing.JTextField SpeciesName;
    private javax.swing.JButton create_sources;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;
    private AttributesDialog mAttributesDialog;
}
