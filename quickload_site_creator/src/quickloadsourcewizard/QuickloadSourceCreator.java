/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package quickloadsourcewizard;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author jfvillal
 */
public class QuickloadSourceCreator {
    /**
     * Specifies the path to the input sequence and annotation files.
     */
    String SourcePath;
    /**
     * Specifies the output path for the generated quickload archive
     */
    String OutputPath;
    
    /**
     * The name of the species ( the directory that will be created to put
     * the annotations and sequence files.  
     */
    String SpeciesName;
    
    static final String CHECKED = "checked";
    
    //tag names used by annot.xml standard
    static final String ANNOT_FILES = "files";
    static final String ANNOT_FILE  = "file";
    static final String ANNOT_NAME = "name";
    static final String ANNOT_TITLE = "title";
    static final String ANNOT_DESCRIPTION = "description";
    static final String ANNOT_URL = "url";
    static final String ANNOT_SERVER_URL = "serverURL";
    static final String ANNOT_LOAD_HINT = "load_hint";
    static final String ANNOT_LABEL_FIELD = "label_field";
    
    //file names used by quickload standard
    static final String ANNOTS_XML_FILE_NAME = "annots.xml";
    static final String CONTENTS_TXT_FILE_NAME = "contents.txt";
    static final String MOD_CHROM_INFO_FILE_NAME = "mod_chromInfo.txt";
    static final String GENOME_INFO_FILE_NAME = "genome.txt";
    
    //Sequence files extensions
    static final String SEQ_EXT_FASTA = "fasta";
    static final String SEQ_EXT_BNIB = "bnib";
    static final String SEQ_EXT_FA = "fa";
    static final String SEQ_EXT_2BIT = "2bit";
    
    //Annotation files extensions
    static final String ANNOT_EXT_BED = "bed";
    
    static boolean isSequenceFile( String extension ){
        boolean ans = extension.equals(SEQ_EXT_FASTA);
        ans = ans || extension.equals(SEQ_EXT_BNIB);
        ans = ans || extension.equals(SEQ_EXT_FA);
        ans = ans || extension.equals(SEQ_EXT_2BIT);
        return ans;
    }
    static boolean isAnnotationFile( String extension){
        boolean ans = extension.equals(ANNOT_EXT_BED);
        return ans;
    }
    
    void setSourcePath(String source_path) {
        SourcePath = source_path;
    }

    public QuickloadSourceCreator(){
        AnnotationFiles = new ArrayList<File>();
        SequenceFiles = new ArrayList<File>();
    }
    
    ArrayList<File> AnnotationFiles;
    List<File> SequenceFiles;
    
    public void searchInputForAnnotSeqFiles() throws IOException{
        File SourceFile = new File( SourcePath );
        if( !SourceFile.exists()){
            throw new IOException();
        }
        if( !SourceFile.isDirectory()){
            throw new IOException("I only work with directory paths at the moment... Sorry");
        }else{
            File[] SequenceTair = SourceFile.listFiles();
            for( File st: SequenceTair){
                try{
                    String[] split = GeneralUtils.getUnzippedName( st.getName() ).split("\\.");
                    String extension = split[ split.length - 1];
                    if( isSequenceFile(extension) ){
                        //this are Sequence files
                        SequenceFiles.add(st);
                    }else if( isAnnotationFile(extension) ){
                       //this are Annotation files
                        AnnotationFiles.add(st);
                    }
                    
                }catch( IndexOutOfBoundsException e ){
                    System.err.println("Error with file " + st.getPath() + ". no extension found... continuing with other files...");
                } 
            }
        }
    }
    
    /**
     * this method will create the contents.txt, the annot.xml, and the chromeInfo.txt files
     * It is intended for a command line use, so many of the tags for annot.xml and 
     * the descriptions on contents.txt are filled with place holders.
     * @param Annots is an array that holds maps.  each map is for one of the files in annots.xml.  
     * @throws ParserConfigurationException
     * @throws TransformerConfigurationException
     * @throws TransformerException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws Exception 
     */
    void generateFiles(String species_description, Map<String,String>[] Annots) throws 
                               ParserConfigurationException, TransformerConfigurationException
                            , TransformerException, FileNotFoundException, IOException, Exception {
        //load the files into a map.
        
        searchInputForAnnotSeqFiles();
        
        //create species directory for our new species.
        File SpeciesDir = new File( OutputPath + "/" + SpeciesName );
        if( !SpeciesDir.exists() ){
            SpeciesDir.mkdir();
        }

        //create annotations xml file   
        if(Annots == null ){//this is to support the current command line tool
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            Element root = doc.createElement( ANNOT_FILES );
            doc.appendChild(root);
            for( File st : AnnotationFiles ){
                Element file = doc.createElement(ANNOT_FILE);

                //add attributes
                file.setAttribute(ANNOT_NAME, st.getName() );
                file.setAttribute(ANNOT_TITLE, st.getName() );
                file.setAttribute(ANNOT_DESCRIPTION, " ");
                file.setAttribute(ANNOT_URL ,  " ");
                file.setAttribute(ANNOT_LOAD_HINT, " ");
                file.setAttribute(ANNOT_LABEL_FIELD, "id");
                root.appendChild( file );
                //copy file to the new quickload archive
                File copy = new File( OutputPath + "/" + SpeciesName + "/" + st.getName() );
                copyFile( st, copy );

            }

            File file = new File( SpeciesDir + "/" + ANNOTS_XML_FILE_NAME );
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource( doc );
            StreamResult result =  new StreamResult(file);
            transformer.transform(source, result);
        }else{ //this takes the additional arguments submitted by a GUI application
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            Element root = doc.createElement( ANNOT_FILES );
            doc.appendChild(root);
            
            for( int k = 0; k < AnnotationFiles.size(); k++){//File st : AnnotationFiles ){
                Element file = doc.createElement(ANNOT_FILE);

                //load the gui settings for this file
                String description = Annots[k].get(ANNOT_DESCRIPTION);
                String title = Annots[k].get(ANNOT_TITLE);
                String url = Annots[k].get(ANNOT_URL);
                String load_hint = Annots[k].get(ANNOT_LOAD_HINT);
                String label_field = Annots[k].get(ANNOT_LABEL_FIELD);
                
                //add attributes
                file.setAttribute(ANNOT_NAME, AnnotationFiles.get(k).getName() );
                file.setAttribute(ANNOT_TITLE, title == null ? AnnotationFiles.get(k).getName() : title );
                file.setAttribute(ANNOT_DESCRIPTION, description == null ? " " : description );
                file.setAttribute(ANNOT_URL ,  url == null ? " " : url);
                if( load_hint != null){
                    if(load_hint.equals( CHECKED )){
                        file.setAttribute(ANNOT_LOAD_HINT, "Whole Sequence");
                    }
                }
                file.setAttribute(ANNOT_LABEL_FIELD, label_field == null ? "id" : label_field);
                root.appendChild( file );
                //copy file to the new quickload archive
                File copy = new File( OutputPath + "/" + SpeciesName + "/" + AnnotationFiles.get(k).getName() );
                copyFile( AnnotationFiles.get(k), copy );
            }

            File file = new File( SpeciesDir + "/" + ANNOTS_XML_FILE_NAME );
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource( doc );
            StreamResult result =  new StreamResult(file);
            transformer.transform(source, result);
            
        }
        File contents = new File(OutputPath + "/" + CONTENTS_TXT_FILE_NAME);
        /**
         * Every time the user runs this routine, we will overwrite the exiting
         * files for the species.  However, this routine only works on one species
         * and a script or a GUI is needed to write multiple spcies to a quickload
         * directory.
         * 
         * Because of this, the routine will be "nice" with the contents file and
         * will make sure not to delete existing species in the quickload archive
         * and not to write the same species twice.
         */
        
        //put contents into put, so that we can update descriptions on species 
        //that had being put into quickload server before (enables updates)
        Map<String,String> ContentsMap = new HashMap<String,String>();
        if( new File(OutputPath + "/" + CONTENTS_TXT_FILE_NAME).exists() ){
            FileInputStream fstream = new FileInputStream(OutputPath + "/" + CONTENTS_TXT_FILE_NAME);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            while ((strLine = br.readLine()) != null)   {
                String[] values = strLine.split("\t");
                if( values.length >= 2){
                    ContentsMap.put(values[0], values[1]);
                }else if (values.length == 1 ){
                    ContentsMap.put(values[0], "Write species sescription here");
                }//else don't pass this line since it is empty
            }
        }
        ContentsMap.put(SpeciesName, species_description == null ? "Write species description here" : species_description );
        //rewrites contents.txt to update changes to descriptions if any
        FileWriter update_contents = new FileWriter( contents , false );
        Set<String> keys = ContentsMap.keySet();
        for( String key : keys){
            update_contents.write( key + "\t" + ContentsMap.get(key) + "\n");//Write species sescription here\n");    
        }
        
        update_contents.close();
        
        //Now lets go over the sequence files ...
        FileWriter mod_chromeinfo_file = new FileWriter( OutputPath + "/" + SpeciesName + "/" + MOD_CHROM_INFO_FILE_NAME, false );
        String mod_chromeinfo = "";
        Collections.sort( SequenceFiles );
        /**
         * if the species name and thethe 2bit sequence 
         * are the same, then skip (don't add) the other sequence files if
         * present.
         */
        boolean only_2bit = false;
        int two_bit = 0;
        String two_bit_file = "";
        for( File st : SequenceFiles ){
            String[] val = st.getName().split("\\.");
            if( val[0].equals( SpeciesName ) && val[1].equals( SEQ_EXT_2BIT ) ){
                //only allow 2bit format sequence and skip all the rest
                only_2bit = true;
                break;
            }
            if( val[1].equals(SEQ_EXT_2BIT)){
                two_bit_file = st.getName();
                ++two_bit;
            }

        }
        if( two_bit > 0 && !only_2bit){
            String err = "For Species: " + SpeciesName + " and 2bit file " + two_bit_file + "\n" +
                            "Fatal:  the species folder name and the 2bit file name must be equal.  Please make sure these two are the same.";
            System.err.println(err);
            throw new SpeciesFolder2BitFileMismatchException(err);
        }

        for( File st : SequenceFiles ){
            String[] val = GeneralUtils.getUnzippedName(st.getName()).split("\\.");
            //index 1 should be the extension
            //index 0 should be the sequence name used by IGB.
            //try to get the sequence size out of the file
            String ext = val[val.length -1 ];

            /*if( only_2bit &&  ext.equals( SEQ_EXT_2BIT) ){
                //skip this sequence if 2bit sequence has the same name
                //as the species name
                continue;
            }*/

               FileTypeHandler file_handler;
                
               FileTypeHolder holder = FileTypeHolder.getInstance();
                
                file_handler = holder.getFileTypeHandler( ext );
                
                AnnotatedSeqGroup g = new AnnotatedSeqGroup("group");
                
                SymLoader symloader =file_handler.createSymLoader(URI.create("file://" + st.getPath()), st.getName(), g ); 
                
                List<BioSeq> list = symloader.getChromosomeList();
                /**
                 * Most sequence files contain only one sequence, but some newer file
                 * standards can contain multiple sequences.  For the mod_chrominfo file
                 * we want to show the total nuber of sequences in each file
                 */
                
                //add this sequence file to the sequence file list only if either
                //this is a 2bit file genome and we are currently processing that file (ignore other fasta, fa, or binib files
                //or if this genome does not have a 2bit file.
                if( (only_2bit &&  ext.equals( SEQ_EXT_2BIT)) || !only_2bit ){
                    for( BioSeq sec : list ){
                
                        //String m = sec.getID();
                        mod_chromeinfo += sec.getID() + "\t" + sec.getLength() + "\n";
                    }
                }
                
                //Copy sequence file to its new quickload location 
                File copy = new File( OutputPath + "/" + SpeciesName + "/" + st.getName() );
                copyFile( st, copy );
        }
        mod_chromeinfo_file.write( mod_chromeinfo );
        mod_chromeinfo_file.close();
        
    }
    
    void setOutputPath(String output_path) {
        OutputPath = output_path;
    }
    

    void setSpeciesName(String species) {
        SpeciesName = species;
    }
    /**
     * Copies source file to destination file.
     * @param source source file
     * @param dest destination file
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static void copyFile( File source, File dest) throws FileNotFoundException, IOException{
        InputStream in = new FileInputStream(source);
        OutputStream out = new FileOutputStream(dest);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0){
        out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
    
}
