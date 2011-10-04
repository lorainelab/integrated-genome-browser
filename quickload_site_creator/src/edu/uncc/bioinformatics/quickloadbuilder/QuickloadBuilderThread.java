/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uncc.bioinformatics.quickloadbuilder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

/**
 *
 * @author jfvillal
 */
public class QuickloadBuilderThread extends Thread{
    QuickLoadArchiveBuilderView Parent;
    QuickloadSourceCreator Creator;
    boolean isNewArchive;
    /**
     * The worker thread that will created, edit, and move files to create the new, or edit the exisiting
     * quickload archive.
     * 
     * @param parent
     * @param creator
     * @param set 
     */
    public QuickloadBuilderThread( QuickLoadArchiveBuilderView parent, QuickloadSourceCreator creator , boolean set){
        Parent = parent;
        Creator = creator;
        isNewArchive  = set ;
    }
    
    @Override
    public void run(){
        if( this.isNewArchive ){
            newArchive();
        }else{
            modifyExisting();
        }
        newArchive();
        Parent.setFinishToClose();
    }
    public void modifyExisting(){
        
    }
    /*
    new Thread(new Runnable(){

                public void run() {
                    try {
                        for( int i = 0; i <= 100; i++){
                            Thread.sleep( 100 );    
                            ProgressReportPage.getProgressBar().setValue(i);
                        }
                        
                    } catch (InterruptedException ex) {
                        Logger.getLogger(QuickLoadArchiveBuilderView.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    ProgressReportPage.setSuccess( true );
                    FinishButton.setText(CLOSE);
                }
            }).start();
            
    */
    public void newArchive(){
        //create species folder
        Creator.createSpeciesDir();
        try {
            //create annots file
            Parent.mProgressReport.setCurrentTaskText("Creating annotation index file.");
            Creator.createAnnotationsFile( Parent.MAnnotations);
            Parent.mProgressReport.setCurrentTaskProgress(100);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(QuickloadBuilderThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(QuickloadBuilderThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(QuickloadBuilderThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerConfigurationException ex) {
            Logger.getLogger(QuickloadBuilderThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerException ex) {
            Logger.getLogger(QuickloadBuilderThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        Parent.mProgressReport.setGeneralProgress(20);
        
        switch( Parent.getChromeInfoCreationChoice()){
            case SelectSequenceFiles.GENERATE:{
                try {
                    //create sequence file
                    Parent.mProgressReport.setCurrentTaskText("Creating chromosome information from sequence files.");
                    Parent.mProgressReport.setCurrentTaskProgress(0);
                    Creator.generateChromeInfo(Parent.MSequences);
                    Parent.mProgressReport.setCurrentTaskProgress(100);
                } catch (IOException ex) {
                    Logger.getLogger(QuickloadBuilderThread.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SpeciesFolder2BitFileMismatchException ex) {
                    Logger.getLogger(QuickloadBuilderThread.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Exception ex) {
                    Logger.getLogger(QuickloadBuilderThread.class.getName()).log(Level.SEVERE, null, ex);
                }    
                break;
            }
            case SelectSequenceFiles.FROM_URL:{ //this is used both for input by the user or from a list
                //load file and move to new location
                break;
            }
            case SelectSequenceFiles.LOCAL:{
                //move file to new location
                break;
            }
        }
        Parent.mProgressReport.setGeneralProgress(40);
        //copy annot files
        //copy sequence files   
        try {
            Parent.mProgressReport.setCurrentTaskText("Copying annotation and sequence files to new location.");
            Parent.mProgressReport.setCurrentTaskProgress(0);
            Creator.copyAnnotationSequenceFiles(Parent.MAnnotations, Parent.MSequences);
            Parent.mProgressReport.setCurrentTaskProgress(0);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(QuickloadBuilderThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(QuickloadBuilderThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        Parent.mProgressReport.setGeneralProgress(60);
        try {
            //update contenst
            Creator.updateContent( Parent.getSpeciesName() );
        } catch (FileNotFoundException ex) {
            Logger.getLogger(QuickloadBuilderThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(QuickloadBuilderThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        Parent.mProgressReport.setGeneralProgress(100);
        
    }
    
}
