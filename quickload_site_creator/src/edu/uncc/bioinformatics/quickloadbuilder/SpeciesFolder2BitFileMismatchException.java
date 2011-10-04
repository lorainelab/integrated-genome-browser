/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uncc.bioinformatics.quickloadbuilder;

/**
 *
 * @author jfvillal
 */
class SpeciesFolder2BitFileMismatchException extends Exception {
    String err;
    public SpeciesFolder2BitFileMismatchException(String str ) {
        err = str;
    }

    @Override
    public String getMessage() {
        return err;
    }
    
    
}
