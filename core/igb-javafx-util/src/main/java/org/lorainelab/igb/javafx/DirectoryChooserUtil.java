/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.javafx;

import java.io.File;
import java.util.Optional;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.DirectoryChooser; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *
 * @author dfreese
 */
public class DirectoryChooserUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(DirectoryChooserUtil.class); 
    private static final JFXPanel FX_RUNTIME_INITIALIZER = new JFXPanel(); 
    private static final Object LOCK = new Object(); 
    private Optional<String> title; 
    private Optional<File> context; 

    private DirectoryChooserUtil(){
        title = Optional.empty();
        context = Optional.empty(); 
    }
    
    public static DirectoryChooserUtil build(){
        return new DirectoryChooserUtil(); 
    }
    
    public DirectoryChooserUtil setTitle(String title){
       this.title = Optional.of(title); 
       return this;
    }
    
    public DirectoryChooserUtil setContext(File file){ //this is selecting the directory to look into 
        if(file != null && file.exists()){
            if(file.isDirectory()){
                  this.context = Optional.ofNullable(file); 
            }else {
                this.context = Optional.ofNullable(file.getParentFile()); 
            }
        }
          
        return this; 
    }
    
    public final Optional<File> retrieveDirectoryFromFXChooser(){
        synchronized(LOCK){
            final File[] selectedFile = new File[1]; //why do these have to be a constants
            final boolean[] keepWaiting = new boolean[1]; 
            keepWaiting[0] = true; 
            
            Platform.runLater(()-> {
                synchronized(LOCK){
                    final DirectoryChooser directoryChooser = getDirectoryChooser(); 
                    selectedFile[0] = directoryChooser.showDialog(null); 
                    keepWaiting[0] = false; 
                    LOCK.notifyAll();  
                    
                }
            }); 
            do{
                try{
                    LOCK.wait(); 
                }catch(final InterruptedException ex){
                    logger.error(ex.getMessage(),ex);
                }
            }while(keepWaiting[0]); 
            
           return Optional.ofNullable(selectedFile[0]);  
        }
    }
    
    private DirectoryChooser getDirectoryChooser(){
        DirectoryChooser directoryChooser = new DirectoryChooser(); 
        
        if(title.isPresent()){
            directoryChooser.setTitle(title.get());
        }

        if(context.isPresent()){
            directoryChooser.setInitialDirectory(context.get());
        }else{
            directoryChooser.setInitialDirectory(directoryChooser.getInitialDirectory()); // i was thinking if the user didn't select a directory the to use the one they are currently on?
        }
        
        return directoryChooser; 
    }
    
    
}
