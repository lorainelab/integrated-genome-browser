/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uncc.bioinformatics.quickloadbuilder;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.awt.Color;
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
import java.util.Collection;
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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
    
     public HashMap<String, File> searchForSeqFiles() throws IOException{
        File source_archive = new File (this.OutputPath + System.getProperty("file.separator") + SpeciesName ); 
        
        HashMap<String, File> map = new HashMap<String, File>();
        
        File[] SequenceTair = source_archive.listFiles();
        for( File st: SequenceTair){
            try{
                String[] split = GeneralUtils.getUnzippedName( st.getName() ).split("\\.");
                String extension = split[ split.length - 1];
                if( isSequenceFile(extension) ){
                    //this are Sequence files
                    String key = st.getAbsolutePath();
                    map.put( key, st);
                    SequenceFiles.add(st);
                }
            }catch( IndexOutOfBoundsException e ){
                System.err.println("Error with file " + st.getPath() + ". no extension found... continuing with other files...");
            } 
        }
        return map;
    }
    
    
    /**
     * Method to be used by the GUI tool.  This will add a new assembly to the contents.txt file
     * @param new_folder
     * @param description
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public void updateContent( String description ) throws FileNotFoundException, IOException{
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
        ContentsMap.put(SpeciesName , description == null ? "Write species description here" : description );
        //rewrites contents.txt to update changes to descriptions if any
        FileWriter update_contents = new FileWriter( contents , false );
        Set<String> keys = ContentsMap.keySet();
        for( String key : keys){
            update_contents.write( key + "\t" + ContentsMap.get(key) + "\n");//Write species sescription here\n");    
        }
        
        update_contents.close();
    }
    
    File SpeciesDir = null;
    public void createSpeciesDir(){
        SpeciesDir = new File( OutputPath + System.getProperty("file.separator") + SpeciesName );
        if( !SpeciesDir.exists() ){
            SpeciesDir.mkdir();
        }
    }
    /**
     * Convenience method.  it appends the information necesary to load a species from 
     * already set archive location.
     * @param species
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException 
     */
    public HashMap<String,Annotation>  readAnnotationFile( String species ) throws ParserConfigurationException, SAXException, IOException{
        SpeciesName = species;
        File file = new File( OutputPath + System.getProperty("file.separator") + species+ System.getProperty("file.separator")  + ANNOTS_XML_FILE_NAME);
        return readAnnotationFile( file );
    }
    /**
     * Reads the Annotation file xml and puts the information in Annotation class data structure.
     * 
     * TODO move to annotation class
     * 
     * @param file
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException 
     */
    public HashMap<String,Annotation>  readAnnotationFile( File file ) throws ParserConfigurationException, SAXException, IOException{
        
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(file);
      doc.getDocumentElement().normalize();
      NodeList nodeLst = doc.getElementsByTagName(Annotation.FILE_TAG);
      
      HashMap<String, Annotation> map = new HashMap<String, Annotation>();
      
      for (int s = 0; s < nodeLst.getLength(); s++) {
        
        
        Node one_file = nodeLst.item(s);
        
        NamedNodeMap attr = one_file.getAttributes();
        
        Node name_tag = attr.getNamedItem( Annotation.NAME_TAG );
        File annot_file = new File( file.getParentFile().getAbsolutePath() + System.getProperty("file.separator") + name_tag.getNodeValue() );
        Annotation annot = new Annotation( annot_file );
        
        Node node = attr.getNamedItem(Annotation.TITLE_TAG );
        annot.Title = node == null ? annot_file.getName() : node.getNodeValue();
        
        node = attr.getNamedItem(Annotation.DESCRIPTION_TAG );
        annot.Description = node == null ? "" : node.getNodeValue();
        
        node = attr.getNamedItem(Annotation.URL_TAG );
        annot.Url = node == null ? null : URI.create( node.getNodeValue() );
        
        node = attr.getNamedItem(Annotation.LABEL_FIELD_TAG );
        annot.Label = node == null ? "" : node.getNodeValue();
        
        node = attr.getNamedItem(Annotation.LOAD_HINT_TAG );
        annot.LoadHint = node == null ? false : Annotation.isLoadHint( node.getNodeValue() ) ;
        try{
            node = attr.getNamedItem(Annotation.NAME_SIZE );
            annot.NameSize = node == null ? Annotation.NAME_SIZE_DEFAULT_SIZE : Integer.parseInt(node.getNodeValue());
        }catch( NumberFormatException e){
            annot.NameSize = Annotation.NAME_SIZE_DEFAULT_SIZE;
        }
        try{
            node = attr.getNamedItem(Annotation.MAX_DEPTH );
            annot.MaxDepth = node == null ? Annotation.MAX_DEPTH_DEFAULT : Integer.parseInt( node.getNodeValue());
        }catch( NumberFormatException e){
            annot.MaxDepth = Annotation.MAX_DEPTH_DEFAULT;
        }
        
        node = attr.getNamedItem( Annotation.DIRECTION_TYPE );
        annot.DirectionType = node == null ? null : node.getNodeValue();
        
        node = attr.getNamedItem( Annotation.BACKGROUND );
        
        try{
            annot.BackgroundColor = node == null ? null : Color.decode(node.getNodeValue());
        }catch( NumberFormatException e ){
            annot.BackgroundColor = null;
        }
        try{
            node = attr.getNamedItem( Annotation.POSITIVE_STRAND_COLOR );
            annot.PositiveStrandColor = node == null ? null : Color.decode(  node.getNodeValue() );
        }catch(NumberFormatException e){
            annot.PositiveStrandColor = null;
        }
        try{
            node = attr.getNamedItem( Annotation.NEGATIVE_STRAND_COLOR );
            annot.NegativeStrandColor = node == null ? null : Color.decode(node.getNodeValue());
        }catch( NumberFormatException e){
            annot.NegativeStrandColor = null;
        }
        node = attr.getNamedItem( Annotation.VIEW_MOD );
        annot.ViewMode = node == null ? null : node.getNodeValue();
        
        node = attr.getNamedItem( Annotation.CONNECTED );
        annot.Connected = node == null ? null : Boolean.parseBoolean(node.getNodeValue());
        
        node = attr.getNamedItem( Annotation.COLLAPSED );
        annot.Collapsed = node == null ? null : Boolean.parseBoolean(node.getNodeValue());
        
        node = attr.getNamedItem( Annotation.SHOW_2_TRACKS );
        annot.ShowTracks = node == null ? null : Boolean.parseBoolean(node.getNodeValue());
        map.put( annot_file.getPath() , annot);
      }
       return map;
    }
    
    
    /**
     * 
     * Creates annotation file based on the information provided by the hashmap
     * 
     */
    public void createAnnotationsFile(HashMap<String,Annotation> map) throws ParserConfigurationException, FileNotFoundException, IOException, TransformerConfigurationException, TransformerException{
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        Element root = doc.createElement( Annotation.FILES_TAG );
        doc.appendChild(root);
        
        Collection<Annotation> annotations = map.values();

        for( Annotation annot : annotations){
            Element file = doc.createElement(Annotation.FILE_TAG);

            //add attributes
            file.setAttribute(Annotation.NAME_TAG, annot.getName() );
            file.setAttribute(Annotation.TITLE_TAG, annot.Title == null ? " " : annot.Title );
            file.setAttribute(Annotation.DESCRIPTION_TAG, annot.Description == null ? " " : annot.Description );
            file.setAttribute(Annotation.URL_TAG ,  annot.Url == null ? " " : annot.Url.toString() );
            if( annot.BackgroundColor != null ){
                file.setAttribute(Annotation.BACKGROUND, toHex( annot.BackgroundColor) );
            }
            if( annot.PositiveStrandColor != null){
                file.setAttribute(Annotation.POSITIVE_STRAND_COLOR, toHex( annot.PositiveStrandColor) );
            }
            if( annot.NegativeStrandColor != null){
                file.setAttribute(Annotation.NEGATIVE_STRAND_COLOR, toHex( annot.NegativeStrandColor) );
            }
            file.setAttribute(Annotation.CONNECTED, Boolean.toString(annot.Connected));
            file.setAttribute(Annotation.COLLAPSED, Boolean.toString(annot.Collapsed));
            file.setAttribute(Annotation.SHOW_2_TRACKS, Boolean.toString(annot.ShowTracks));
            
            file.setAttribute(Annotation.DIRECTION_TYPE, annot.DirectionType);
            file.setAttribute(Annotation.VIEW_MOD, annot.ViewMode );
            
            file.setAttribute(Annotation.MAX_DEPTH, "" + annot.MaxDepth);
            file.setAttribute(Annotation.NAME_SIZE, "" + annot.NameSize);
            //TODO add the other visual attributes.
            
            if( annot.LoadHint ){
                file.setAttribute(Annotation.LOAD_HINT_TAG, "Whole Sequence");
            }
            file.setAttribute(Annotation.LABEL_FIELD_TAG, annot.Label == null ? "id" : annot.Label );
            
            root.appendChild( file );
            //copy file to the new quickload archive
            //File copy = new File( OutputPath + "/" + SpeciesName + "/" + annot.FAttribute.getName());
            //System.out.println("Copying file: " + copy.getName() );
            //copyFile( annot.FAttribute, copy );
        }
        
        File file = new File( SpeciesDir + "/" + ANNOTS_XML_FILE_NAME );
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource( doc );
        StreamResult result =  new StreamResult(file);
        transformer.transform(source, result);
    }
    public String toHex( Color color){
        return "0x" + Integer.toHexString(color.getRGB());
    }
    /**
     * Generates chromeinfor by extracting the information from the sequence files.
     * @param seq_map
     * @throws IOException
     * @throws SpeciesFolder2BitFileMismatchException
     * @throws Exception 
     */
    public void generateChromeInfo( HashMap<String,File> seq_map) throws IOException, SpeciesFolder2BitFileMismatchException, Exception{
        Collection<File> seq_list = seq_map.values();
        
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

        
        for( File st : seq_list){
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
                //File copy = new File( OutputPath + "/" + SpeciesName + "/" + st.getName() );
                //copyFile( st, copy );
        }
       
        mod_chromeinfo_file.write( mod_chromeinfo );
        mod_chromeinfo_file.close();
    }
    
    
    /**
     * Copies both the annotation files and the sequence files to the new location on the quickload archive.
     * @param annot_map
     * @param seq_map
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public void copyAnnotationSequenceFiles( HashMap<String,Annotation> annot_map, HashMap<String,File> seq_map) throws FileNotFoundException, IOException{
        Collection<Annotation> annotations = annot_map.values();
        for( Annotation annot : annotations){
            File copy = new File( OutputPath + "/" + SpeciesName + "/" + annot.FAttribute.getName());
            System.out.println("Copying file: " + copy.getName() );
            copyFile( annot.FAttribute, copy );
        }
        
        Collection<File> sequences = seq_map.values();
        
        for( File st : sequences ){
            //Copy sequence file to its new quickload location 
            File copy = new File( OutputPath + "/" + SpeciesName + "/" + st.getName() );
            copyFile( st, copy );
        }
    }
    
    
    public List<String> getContentsFile() throws FileNotFoundException, IOException{
        FileInputStream fstream = new FileInputStream(OutputPath + "/" + CONTENTS_TXT_FILE_NAME);
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String strLine;
        List<String> str_list = new ArrayList<String>();
        while ((strLine = br.readLine()) != null)   {
            String[] values = strLine.split("\t");
            if( values.length > 0 ){
                str_list.add( values[0] );
            }
        }
        return str_list;
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

            Element root = doc.createElement( Annotation.FILES_TAG );
            doc.appendChild(root);
            for( File st : AnnotationFiles ){
                Element file = doc.createElement(Annotation.FILE_TAG);

                //add attributes
                file.setAttribute(Annotation.NAME_TAG, st.getName() );
                file.setAttribute(Annotation.TITLE_TAG, st.getName() );
                file.setAttribute(Annotation.DESCRIPTION_TAG, " ");
                file.setAttribute(Annotation.URL_TAG ,  " ");
                file.setAttribute(Annotation.LOAD_HINT_TAG, " ");
                file.setAttribute(Annotation.LABEL_FIELD_TAG, "id");
                root.appendChild( file );
                //copy file to the new quickload archive
                File copy = new File( OutputPath + "/" + SpeciesName + "/" + st.getName() );
                System.out.println("Copying file: " + copy.getName() );
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

            Element root = doc.createElement( Annotation.FILES_TAG );
            doc.appendChild(root);
            
            for( int k = 0; k < AnnotationFiles.size(); k++){//File st : AnnotationFiles ){
                Element file = doc.createElement(Annotation.FILE_TAG);

                //load the gui settings for this file
                String description = Annots[k].get(Annotation.DESCRIPTION_TAG);
                String title = Annots[k].get(Annotation.TITLE_TAG);
                String url = Annots[k].get(Annotation.URL_TAG);
                String load_hint = Annots[k].get(Annotation.LOAD_HINT_TAG);
                String label_field = Annots[k].get(Annotation.LABEL_FIELD_TAG);
                
                //add attributes
                file.setAttribute(Annotation.NAME_TAG, AnnotationFiles.get(k).getName() );
                file.setAttribute(Annotation.TITLE_TAG, title == null ? AnnotationFiles.get(k).getName() : title );
                file.setAttribute(Annotation.DESCRIPTION_TAG, description == null ? " " : description );
                file.setAttribute(Annotation.URL_TAG ,  url == null ? " " : url);
                if( load_hint != null){
                    if(load_hint.equals( CHECKED )){
                        file.setAttribute(Annotation.LOAD_HINT_TAG, "Whole Sequence");
                    }
                }
                file.setAttribute(Annotation.LABEL_FIELD_TAG, label_field == null ? "id" : label_field);
                root.appendChild( file );
                //copy file to the new quickload archive
                File copy = new File( OutputPath + "/" + SpeciesName + "/" + AnnotationFiles.get(k).getName() );
                System.out.println("Copying file: " + copy.getName() );
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
