/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package quickloadsourcewizard;

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
public class QuickloadSourceCommand {
    
    public static String OUTPUT_PATH = "--output-path";
    public static String INPUT_PATH = "--source-path";
    public static String SPECIES_NAME = "--species-name";
    public static String VALIDATE_SITE = "--validate-site";
    
    public static void main( String[] args) {
        if( args.length < 2){
            System.err.println("Not enough arguments.");
            printUsage();
            System.exit( 1 );   
        }
        String input = null;
        String output = null;
        String species = null;
        String validate_site = null;
        try{
            for( int i = 0; i < args.length ; i ++){
                if( args[i].equals(INPUT_PATH)){
                    input = args[++i];
                }else if( args[i].equals( OUTPUT_PATH)){
                    output = args[++i];
                }else if( args[i].equals(SPECIES_NAME)){
                    species = args[++i];
                }else if( args[i].equals( VALIDATE_SITE)){
                    validate_site = args[++i];
                }

            }
        }catch( IndexOutOfBoundsException e){
            System.err.println("Opps ! a missing argument perhaps ?");
            System.exit(1);
        }
        if( validate_site != null){
            //validate site
            QuickloadSourceValidator validator = new QuickloadSourceValidator( validate_site );
            validator.validate();
        }else{
            //create site
            if( input == null){
                System.err.println("I need at least a source path. (" + INPUT_PATH + ").");
                printUsage();
                System.exit(1);
            }
            if( species == null){
                System.err.println("I need a species name. (" + SPECIES_NAME + ").");
                printUsage();
                System.exit(1);
            }
            if( output == null ){
                output = "./";
            }

            QuickloadSourceCreator creator = new QuickloadSourceCreator();
            creator.setSourcePath(input);
            creator.setOutputPath(output);
            creator.setSpeciesName( species );
            try {

                    creator.generateFiles(null, null);

            } catch (ParserConfigurationException ex) {
                Logger.getLogger(QuickloadSourceCommand.class.getName()).log(Level.SEVERE, null, ex);
            } catch (TransformerConfigurationException ex) {
                Logger.getLogger(QuickloadSourceCommand.class.getName()).log(Level.SEVERE, null, ex);
            } catch (TransformerException ex) {
                Logger.getLogger(QuickloadSourceCommand.class.getName()).log(Level.SEVERE, null, ex);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(QuickloadSourceCommand.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(QuickloadSourceCommand.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(QuickloadSourceCommand.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    static void printUsage(){
        System.out.println(
                "Will create or validate a Quickload website of file path.  Note that it can only "
                + "create or validate in one call, not both.  to validate a site after creating it,"
                + "type the command again with " + VALIDATE_SITE + " option followed by the path"
                + "or URL for the site.\n"
                + "Usage: \n"
                + "Basic use: \n"
                + "java QuickloadSourceCommand "+ INPUT_PATH+" /path/to/source/files \n"
                + INPUT_PATH + " /path/to/source/files\n"
                + "\tPath to sequence and annotations files.\n"
                + "optional commands:\n"
                + OUTPUT_PATH + " /path/to/output/dir  \n"
                + "\tPath where the quickload archive will be placed\n"
                + SPECIES_NAME + " Name_of_Species\n"
                + "\tThe name of the species of the sequence and annotation files\n"
                + VALIDATE_SITE + "http://mysite.com or file:///my/path/to/quickload \n"
                + "\tValidates a site making sure it complies with quickload site specifications." );
    }
}
