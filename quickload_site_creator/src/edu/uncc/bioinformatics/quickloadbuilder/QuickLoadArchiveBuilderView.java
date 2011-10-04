/*
 * QuickLoadServerGUILookAndFeelPrototypeView.java
 */

package edu.uncc.bioinformatics.quickloadbuilder;

import java.awt.CardLayout;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import javax.swing.BoxLayout;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import org.xml.sax.SAXException;


/**
 * The application's main frame.
 */
public class QuickLoadArchiveBuilderView extends FrameView {
    public final String FINISH = "Finish";
    public final String CLOSE = "Close";
    public final String ABORT = "Abort";
    
    public final String WELCOME = "WelcomeScreen";
    public final String EDIT_CREATE = "EditCreate";
    public final String SELECT_ANNOTATION_FILES = "AddAnnotationFiles";
    public final String ANNOTATION_FILES_ATTRIBUTES = "AnnotationFilesAttributes";
    public final String COMMIT_CHANGES = "CommitChanges";
    public final String QUICKLOAD_ROOT_NEW = "QuickloadRootNew" ;
    public final String QUICKLOAD_ROOT_EXISTING = "QuickloadRooExisting" ;
    public final String SELECT_SEQUENCE_FILES = "SequenceFiles";
    public final String GENUS_SPECIES = "GenusSpecies";
    
    public final String PROGRESS_REPORT = "ProgressReport";
    
    ProgressReport mProgressReport;
    
    /** All the faces used with the wizard **/
    WelcomePanel mWelcome;
    CreateEditValidate mCreateEditValidate;
    QuickloadServerRoot mQuickloadServerRoot;
    QuickloadServerRootExisting mQuickloadServerRootExisting;
    SelectAnnotationFiles mSelectAnnotationFiles;
    AnnotationFiles mAnnotationFiles;
    CommitPage mCommitPage;
    SelectSequenceFiles mSelectSequenceFiles;
    GenusSpeciesName mGenusSpeciesName;
    
    public String  getSpeciesName(){
        return mGenusSpeciesName.getSpeciesDescription();
    }
    
    QuickloadSourceCreator Creator;
    
    /**Variables used to generate the Quickload archive **/
    /**
     * Stores the Annotation Files.
     */
    public HashMap<String, Annotation > MAnnotations;
    public HashMap<String, File> MSequences;
    
    /**
     * 
     * Stores if the quickload archive will be created from scratch.  If false, 
     * the program will add the files to an existing quickload site.
     * 
     */
    boolean CreateQuickloadArchive;
    public boolean getCreateQuickloadArchive(){
        return this.CreateQuickloadArchive;
    }
    public void setCreateQuickloadArchive( boolean set ){
        this.CreateQuickloadArchive = set;
    }
    public QuickLoadArchiveBuilderView(SingleFrameApplication app) {
        super(app);

        MAnnotations = new HashMap<String, Annotation>();
        MSequences = new HashMap<String, File>();
        initComponents();

        CreateQuickloadArchive = true;
        
        TheContainer.setLayout(new CardLayout() );
        
        mWelcome = new WelcomePanel(this);
        mCreateEditValidate = new CreateEditValidate(this );
        mQuickloadServerRoot = new QuickloadServerRoot(this);
        mQuickloadServerRootExisting = new QuickloadServerRootExisting(this);
        mSelectAnnotationFiles = new SelectAnnotationFiles(this );
        mAnnotationFiles = new AnnotationFiles(this);
        mCommitPage = new CommitPage(this);
        mSelectSequenceFiles= new SelectSequenceFiles(this );
        mGenusSpeciesName = new GenusSpeciesName( this );
        
        TheContainer.add( mWelcome , WELCOME);
        TheContainer.add( mCreateEditValidate, EDIT_CREATE);
        TheContainer.add( mQuickloadServerRoot, QUICKLOAD_ROOT_NEW );
        TheContainer.add( mQuickloadServerRootExisting, QUICKLOAD_ROOT_EXISTING );
        TheContainer.add( mSelectAnnotationFiles, SELECT_ANNOTATION_FILES );
        TheContainer.add( mAnnotationFiles , ANNOTATION_FILES_ATTRIBUTES);
        TheContainer.add( mCommitPage, COMMIT_CHANGES );
        TheContainer.add( mSelectSequenceFiles, SELECT_SEQUENCE_FILES );
        TheContainer.add( mGenusSpeciesName , GENUS_SPECIES) ;
        
        
        mProgressReport = new ProgressReport( this );
        TheContainer.add( mProgressReport , PROGRESS_REPORT) ;
        
        
        State = WELCOME;
        
        
        /**
         * Debug: use the lines below to jump to a specific card.
         */
        /*
         State = SELECT_SEQUENCE_FILES;
         CardLayout layout = (CardLayout) TheContainer.getLayout();
         layout.show( TheContainer, State);    
        */
        FinishButton.setText(CLOSE);
        BackButton.setEnabled(false);
    }

    
    
    
    final TableModel AnnotationsTableModel = new TableModel(){
        
        TableModelListener ner;
        public int getRowCount() {
            return MAnnotations.size();
        }

        public int getColumnCount() {
            return 1;
        }

        public String getColumnName(int columnIndex) {
            return "Annotation Files";
        }

        public Class<?> getColumnClass(int columnIndex) {
            return String.class;//File.class;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return ((Annotation) MAnnotations.values().toArray()[rowIndex]).FAttribute.getName();
        }
        //no used
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            //File f = (File) aValue;
            //AnnotationFileMap.put( f.getPath(), f );
        }

        public void addTableModelListener(TableModelListener l) {
            ner = l;
        }

        public void removeTableModelListener(TableModelListener l) {
            ner = null;
        }
        
    };
    
    final TableModel SequencesTableModel = new TableModel(){
        
        TableModelListener ner;
        public int getRowCount() {
            return MSequences.size();
        }

        public int getColumnCount() {
            return 1;
        }

        public String getColumnName(int columnIndex) {
            return "Sequence Files";
        }

        public Class<?> getColumnClass(int columnIndex) {
            return String.class;//File.class;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return ((File) MSequences.values().toArray()[rowIndex]).getName();
        }
        //no used
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            //File f = (File) aValue;
            //AnnotationFileMap.put( f.getPath(), f );
        }

        public void addTableModelListener(TableModelListener l) {
            ner = l;
        }

        public void removeTableModelListener(TableModelListener l) {
            ner = null;
        }
        
    };
    
    
    

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        NextButton = new javax.swing.JButton();
        FinishButton = new javax.swing.JButton();
        BackButton = new javax.swing.JButton();
        TheContainer = new javax.swing.JPanel();

        mainPanel.setName("mainPanel"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(edu.uncc.bioinformatics.quickloadbuilder.QuickLoadArchiveBuilderApp.class).getContext().getResourceMap(QuickLoadArchiveBuilderView.class);
        NextButton.setText(resourceMap.getString("NextButton.text")); // NOI18N
        NextButton.setName("NextButton"); // NOI18N
        NextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NextButtonActionPerformed(evt);
            }
        });

        FinishButton.setText(resourceMap.getString("FinishButton.text")); // NOI18N
        FinishButton.setName("FinishButton"); // NOI18N
        FinishButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FinishButtonActionPerformed(evt);
            }
        });

        BackButton.setText(resourceMap.getString("BackButton.text")); // NOI18N
        BackButton.setName("BackButton"); // NOI18N
        BackButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BackButtonActionPerformed(evt);
            }
        });

        TheContainer.setName("TheContainer"); // NOI18N

        javax.swing.GroupLayout TheContainerLayout = new javax.swing.GroupLayout(TheContainer);
        TheContainer.setLayout(TheContainerLayout);
        TheContainerLayout.setHorizontalGroup(
            TheContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 750, Short.MAX_VALUE)
        );
        TheContainerLayout.setVerticalGroup(
            TheContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 492, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                        .addComponent(BackButton)
                        .addGap(18, 18, 18)
                        .addComponent(NextButton)
                        .addGap(18, 18, 18)
                        .addComponent(FinishButton))
                    .addComponent(TheContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(TheContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(FinishButton)
                    .addComponent(NextButton)
                    .addComponent(BackButton))
                .addContainerGap())
        );

        setComponent(mainPanel);
    }// </editor-fold>//GEN-END:initComponents

    
    static String State;
    private void NextButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NextButtonActionPerformed
        CardLayout layout = (CardLayout) TheContainer.getLayout();
        HashMap<String, Annotation> map;
        if( State.equals( WELCOME )){
            State = EDIT_CREATE;
            layout.show( TheContainer, State);    
            BackButton.setEnabled(true);
        }else if(State.equals(EDIT_CREATE) ){
            if( this.CreateQuickloadArchive){
                State = QUICKLOAD_ROOT_NEW;
                this.mQuickloadServerRoot.updateNextButton();
            }else{
                State = QUICKLOAD_ROOT_EXISTING;
                this.mQuickloadServerRootExisting.updateControls();
            }
            //set weather we are creating a new archive or building a new one.
            this.CreateQuickloadArchive = mCreateEditValidate.isCreateNewArchive();
            
            
            
            layout.show( TheContainer, State);    
        }else if(State.equals(QUICKLOAD_ROOT_NEW) || State.equals(QUICKLOAD_ROOT_EXISTING)  ){
            
            Creator = new QuickloadSourceCreator();
            if( State.equals(QUICKLOAD_ROOT_NEW)){
                Creator.setOutputPath( mQuickloadServerRoot.getArchivePath() );
                mGenusSpeciesName.setCreateOrEditArchive(true);
                
            }else{
                Creator.setOutputPath(mQuickloadServerRootExisting.getArchivePath() );
                try {
                    mGenusSpeciesName.setCreateOrEditArchive(false);
                    mGenusSpeciesName.setExistingConents( Creator.getContentsFile());
                }catch( FileNotFoundException e){
                    e.printStackTrace();
                } catch (IOException ex) {
                        Logger.getLogger(QuickLoadArchiveBuilderView.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            this.mGenusSpeciesName.validateFields();
            
            State = GENUS_SPECIES;
            layout.show( TheContainer, State);
            
        }else if(State.equals(GENUS_SPECIES)){
            State = SELECT_ANNOTATION_FILES;
            
            String species = mGenusSpeciesName.getSpeciesSelected();
            Creator.setSpeciesName(species );
            //if editing an existing archive, populate table with files from the archive.
            if( !this.CreateQuickloadArchive ){
                try {
                    
                    MAnnotations = Creator.readAnnotationFile( species );
                    
                    mAnnotationFiles.loadAnnotOnUIFromMap(0);
                    mSelectAnnotationFiles.updateGUI();
                } catch (ParserConfigurationException ex) {
                    Logger.getLogger(QuickLoadArchiveBuilderView.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SAXException ex) {
                    Logger.getLogger(QuickLoadArchiveBuilderView.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(QuickLoadArchiveBuilderView.class.getName()).log(Level.SEVERE, null, ex);
                }
            }else{
                //create folder
                Creator.setSpeciesName(species);
                //all changes are commited at the end.
                //Creator.createSpeciesDir();
            }
            
            layout.show( TheContainer, State);    
        }else if(State.equals(SELECT_ANNOTATION_FILES) ){
            State = ANNOTATION_FILES_ATTRIBUTES;
            layout.show( TheContainer, State);  
            
            this.mAnnotationFiles.updateTable();
            //After this point, all information is optional
        }else if(State.equals(ANNOTATION_FILES_ATTRIBUTES) ){
            State = SELECT_SEQUENCE_FILES;
            
            //if editing existing archive
            if(!this.CreateQuickloadArchive){
                try {
                    this.MSequences = Creator.searchForSeqFiles();
                    mSelectSequenceFiles.updateGUI();
                } catch (IOException ex) {
                    Logger.getLogger(QuickLoadArchiveBuilderView.class.getName()).log(Level.SEVERE, null, ex);
                }
            }//else nothing
                
            layout.show( TheContainer, State);    
        }else if(State.equals( SELECT_SEQUENCE_FILES )){
            State = COMMIT_CHANGES;
            layout.show( TheContainer, State);    
            NextButton.setEnabled(false);
            FinishButton.setText(FINISH);
        }
        
    }//GEN-LAST:event_NextButtonActionPerformed

    
    public boolean isCreateNewArchive(){
        return mCreateEditValidate.isCreateNewArchive();
    }
    
    private void FinishButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FinishButtonActionPerformed
        if( FinishButton.getText().equals(FINISH)){
            CardLayout layout = (CardLayout) TheContainer.getLayout();
            System.out.println("Commit");
            //take all the information and create the quickload archive.
            //do this on a seaparate thread.
            //when that thread is done, it should update PROGRESS_REPORT 
            //and set the finish button to close.
            int mod_chrome_creation_optin = mSelectSequenceFiles.getMochromeCreationOption();
            //temporal code for prototype
            
            QuickloadBuilderThread worker = new QuickloadBuilderThread(this, Creator, this.isCreateNewArchive());
            worker.start();
            
            
            State = PROGRESS_REPORT;
            layout.show( TheContainer, State);    
            NextButton.setEnabled(false);
            FinishButton.setText(ABORT);
        }else if(FinishButton.getText().equals(ABORT)){
            System.out.println("Abort");
            CardLayout layout = (CardLayout) TheContainer.getLayout();
            mProgressReport.setSuccess(false);  
            FinishButton.setText(CLOSE);
        }else{
            System.out.println("Exit");
            this.getApplication().exit();
        }
    }//GEN-LAST:event_FinishButtonActionPerformed

    private void BackButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BackButtonActionPerformed
          CardLayout layout = (CardLayout) TheContainer.getLayout();
        if( State.equals(  EDIT_CREATE )){
            State = WELCOME;
            layout.show( TheContainer, State);    
            BackButton.setEnabled(false);
        }else if(State.equals(QUICKLOAD_ROOT_NEW) || State.equals(QUICKLOAD_ROOT_EXISTING) ){
            State = EDIT_CREATE;
            layout.show( TheContainer, State);    
        }else if(State.equals(GENUS_SPECIES)){
            if( this.CreateQuickloadArchive ){
                State = QUICKLOAD_ROOT_NEW;
            }else{
                State = QUICKLOAD_ROOT_EXISTING;
            }
            layout.show( TheContainer, State);    
        }else if(State.equals(SELECT_ANNOTATION_FILES) ){
            State = GENUS_SPECIES;
            layout.show( TheContainer, State);    
            
        }else if(State.equals(ANNOTATION_FILES_ATTRIBUTES) ){
            State = SELECT_ANNOTATION_FILES;
            layout.show( TheContainer, State);    
            //At this point all information is required (going backwards)
        }else if(State.equals( SELECT_SEQUENCE_FILES )){
            State = ANNOTATION_FILES_ATTRIBUTES;
            layout.show( TheContainer, ANNOTATION_FILES_ATTRIBUTES);    
            
        }else if(State.equals(COMMIT_CHANGES) ){
            State = SELECT_SEQUENCE_FILES;
            layout.show( TheContainer, State);    
            NextButton.setEnabled(true);
            FinishButton.setText(CLOSE);
        }
    }//GEN-LAST:event_BackButtonActionPerformed

    public void disableNext(){
        NextButton.setEnabled( false);
    }
    public void enableNext(){
        NextButton.setEnabled( true );
    }
    public void enableBack(){
        BackButton.setEnabled(true);
    }
    public void disableBack(){
        BackButton.setEnabled(false);
    }
    public void setFinishToClose(){
        FinishButton.setText(CLOSE);
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton BackButton;
    private javax.swing.JButton FinishButton;
    private javax.swing.JButton NextButton;
    private javax.swing.JPanel TheContainer;
    private javax.swing.JPanel mainPanel;
    // End of variables declaration//GEN-END:variables

    //private final Timer messageTimer;
    //private final Timer busyIconTimer;
    //private final Icon idleIcon;
    //private final Icon[] busyIcons = new Icon[15];
    //private int busyIconIndex = 0;

    private JDialog aboutBox;

    int getChromeInfoCreationChoice() {
        return mSelectSequenceFiles.getMochromeCreationOption();
    }
}
