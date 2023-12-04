package org.lorainelab.igb.Exception;

public class IndexFileNotFoundException extends Exception {
    public IndexFileNotFoundException(){
        super("Could not find index file");
    }
    public IndexFileNotFoundException(String message){
        super(message);
    }
}
