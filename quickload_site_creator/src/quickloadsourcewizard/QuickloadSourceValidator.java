/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package quickloadsourcewizard;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author jfvillal
 */
public class QuickloadSourceValidator {
    String ArchivePath;
    public QuickloadSourceValidator( String uri ){
        ArchivePath = uri;
    }
    public void validate() {
        //first read contents.
        int species_num = 0;
        URI arch_uri = URI.create(ArchivePath + "/" + QuickloadSourceCreator.CONTENTS_TXT_FILE_NAME);
        URL arch_path;
        try {
            arch_path = arch_uri.toURL();
        } catch (MalformedURLException ex) {
            System.err.println("The path given has syntactical errors");
            return;
        }
        try{
            URLConnection yc = null;
        
            yc = arch_path.openConnection();

            BufferedReader reader = new BufferedReader( new InputStreamReader(
                                                        yc.getInputStream()));
            String c = null;
            System.out.println(QuickloadSourceCreator.CONTENTS_TXT_FILE_NAME + " check passed!");
            while ((c = reader.readLine()) != null) {  //this loop goes over the contents.txt file
                ++species_num;
                String[] vals = c.split("\t");
                if( vals[0].contains(" ")){
                    System.err.println("In contents.txt: Spaces in species name or folder not allowed.  Skipping species " + vals[0]);
                    continue;
                }

                String species = vals[0];
                System.out.println( "Checking species: " +  species );
                //check each directory exists.

                URI species_dir = URI.create(ArchivePath + "/" + species + "/" + QuickloadSourceCreator.ANNOTS_XML_FILE_NAME);
                URL species_path = species_dir.toURL();
                URLConnection sc = species_path.openConnection();
                try {
                        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                        Document doc = docBuilder.parse(sc.getInputStream());
                        doc.getDocumentElement().normalize();

                        NodeList listOfFiles = doc.getElementsByTagName("file");

                        int length = listOfFiles.getLength();
                        for (int s = 0; s < length && (!Thread.currentThread().isInterrupted()); s++) {
                                Node fileNode = listOfFiles.item(s);
                                if (fileNode.getNodeType() != Node.ELEMENT_NODE) {
                                        continue;
                                }
                                Element fileElement = (Element) fileNode;
                                String filename = fileElement.getAttribute("name");
                                //for each name, there should be a file with that name
                                try{
                                    URI annotation_file = URI.create(ArchivePath + "/" + species + "/" + filename);
                                    URL annotation_file_path = annotation_file.toURL();
                                    URLConnection ac = annotation_file_path.openConnection();
                                    BufferedReader file_exists = new BufferedReader( new InputStreamReader(
                                                        ac.getInputStream()));
                                    file_exists.read();
                                    file_exists.close();
                                }catch( FileNotFoundException e ){
                                    System.err.println("Annotation file: " + filename +  " mentioned in " + QuickloadSourceCreator.ANNOTS_XML_FILE_NAME + " does not exists.");
                                }
                                //All other tags cannot be validated, it is up to the user to make
                                //sure they are accurate
                        }
                } catch (Exception e) {
                    System.err.println("Could not read " + QuickloadSourceCreator.ANNOTS_XML_FILE_NAME + " for species " + species_path.toString() + ".  Please add this file according to specifications.\n"
                            + "\tAnother reason for this error can be a misstyped species name. Is " + species + " the right name ?");
                }

                URI modchrome_file = null;
                URL modchrome_path = null;
                URLConnection modchrome_c = null;
                BufferedReader modchrome_reader = null;

                //first try to load genome.txt
                try{
                    modchrome_file = URI.create(ArchivePath + "/" + species + "/" + QuickloadSourceCreator.GENOME_INFO_FILE_NAME);
                    modchrome_path = modchrome_file.toURL();
                    modchrome_c = modchrome_path.openConnection();
                    modchrome_reader = new BufferedReader( new InputStreamReader( modchrome_c.getInputStream()));
                }catch(FileNotFoundException e){
                    //System.out.println("Failed to find " + QuickloadSourceCreator.GENOME_INFO_FILE_NAME + "... looking for " + QuickloadSourceCreator.MOD_CHROM_INFO_FILE_NAME);
                    //then try to load modchrome_info.txt
                    try{
                        modchrome_file = URI.create(ArchivePath + "/" + species + "/" + QuickloadSourceCreator.MOD_CHROM_INFO_FILE_NAME);
                        modchrome_path = modchrome_file.toURL();
                        modchrome_c = modchrome_path.openConnection();
                        modchrome_reader = new BufferedReader( new InputStreamReader( modchrome_c.getInputStream()));
                    }catch(FileNotFoundException m){
                        System.err.println("Failed to find " + QuickloadSourceCreator.MOD_CHROM_INFO_FILE_NAME + ". species folder " 
                                        + species + " needs to have either a " 
                                        + QuickloadSourceCreator.GENOME_INFO_FILE_NAME + " file or a " 
                                        + QuickloadSourceCreator.MOD_CHROM_INFO_FILE_NAME + " file.");
                        continue;
                    }
                }   
                String line = null;
                //if the mod_chrome file is the same as the folder(species) name
                //then we check to make sure there is a mod_chome.2bit file in there
                //and skip all the other sequence names

                //if the modchrome file has a different name than the species 
                //name, then every sequence in the mod_chrome file should have
                //a corresponding sequence file.
                String[] seq_ext = { QuickloadSourceCreator.SEQ_EXT_BNIB, QuickloadSourceCreator.SEQ_EXT_FA, QuickloadSourceCreator.SEQ_EXT_FASTA 
                                    , QuickloadSourceCreator.SEQ_EXT_FASTA + ".gz" , QuickloadSourceCreator.SEQ_EXT_FA + ".gz"};

                boolean chrome_info_equals_species = false;
                try{
                    //if species.2bit file exists.  then we don't have to check that the sequence names 
                    //mentioned in chrome_info.txt exists.
                    URI s_f = URI.create(ArchivePath + "/" + species + "/" + species + ".2bit" );
                    URL s_f_p = s_f.toURL();
                    URLConnection ac = s_f_p.openConnection();
                    BufferedReader file_exists = new BufferedReader( new InputStreamReader( ac.getInputStream()));
                    file_exists.read();
                    file_exists.close();
                    chrome_info_equals_species = true; 
                }catch( FileNotFoundException e ){
                    chrome_info_equals_species = false;
                }catch( IOException ex){
                    ex.printStackTrace();
                }

                if( chrome_info_equals_species ){
                    //that's it, we don't have to test for other sequence file's existance.
                }else{
                    int l = 0;
                    int err = 0;
                    while ((line = modchrome_reader.readLine()) != null) {
                        ++l;
                        try{
                            String[] seq = line.split("\t");
                            boolean one_seq_ext_worked = false;
                            for( int k = 0; k < seq_ext.length ; k++){
                                try{
                                    URI sequence_file = URI.create(ArchivePath + "/" + species + "/" + seq[0] + "." + seq_ext[k]);
                                    URL sequence_file_path = sequence_file.toURL();
                                    URLConnection ac = sequence_file_path.openConnection();
                                    BufferedReader file_exists = new BufferedReader( new InputStreamReader( ac.getInputStream()));
                                    file_exists.read();
                                    file_exists.close();
                                    one_seq_ext_worked = true; 
                                    //if we make it this far, this means one of the sequence extension was found on the archive.
                                    //from modchrome, there is no way to know which estension is used for the sequence file.  and
                                    //if we are validating a webserver, there is no way to list the existing files.  so we will
                                    //just try the posible convinations.
                                    break;
                                }catch( FileNotFoundException e ){
                                    //don't complain until we are sure none of the extensions from seq_ext return a file.
                                    //System.out.println( seq + "." + seq_ext[k] + " did not work.  No worries, I'll try other file extension before complaining about specification compliance");
                                }
                            }
                            if( !one_seq_ext_worked){
                                if( err < 5 ){
                                System.err.println("Sequence file: " + seq[0] +".seq_ext" +  " mentioned in " + QuickloadSourceCreator.MOD_CHROM_INFO_FILE_NAME + " does not exists."
                                        + "\nThere needs to be a sequence file name that matches the sequence mentioned in the sequence file.  The other reason for this error can be a mistyped or missing"
                                        + "\n.2bit file.  a .2bit file should have the same name as the species folder name.");
                                }
                                ++err;
                            }
                            
                        }catch( IndexOutOfBoundsException e){
                            System.out.println("An error happened parsing sequence at line " + l);
                        }
                    }  
                    if( err > 5 ){
                        System.err.println( err + " more sequence file error detected.");
                    }
                }
            }//end while reading contents.txt
            System.out.println("Species checked " + species_num );
        }catch( IOException e){
            System.err.println( QuickloadSourceCreator.CONTENTS_TXT_FILE_NAME + " was not found.  the quickload site path may be mistyped, or it may be missing this file.");
        }
    }
}
