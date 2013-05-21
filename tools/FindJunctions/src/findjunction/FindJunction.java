/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package findjunction;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.symloader.BAM;
import com.affymetrix.genometryImpl.symloader.TwoBit;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

/**
 *
 * @author Anuj
 */
public class FindJunction {

    /**
     * Class used to find the Junctions of a given BAM File using a threshold value of intron length and writes down to an output BED file using the File path
     * given in command line using various options as given in the code.
     * 
     */
    private static final int DEFAULT_THRESHOLD = 5; // maybe make all caps
    public static boolean DEBUG = false;
    
    public static void main(String[] args)throws FileNotFoundException,IOException, URISyntaxException, Exception {
        FindJunction fJ = new FindJunction();
        File directory = new File(".");
        String home = System.getProperty("user.home");
        int threshold = DEFAULT_THRESHOLD;
        boolean twoTracks = false;
        boolean uniqueness = false;
        boolean topHatStyleFlanking = false;
        String input = args[args.length-1];
        String last_argument_message = "Last argument should be a comma-separated list of one or more BAM files";
        if(input.lastIndexOf(".") < 0){
            System.err.println(last_argument_message);
            System.exit(1);   
        }
        if(!(input.substring(input.lastIndexOf(".")+1, input.length()).equals("bam"))){
            System.err.println(last_argument_message);
            System.exit(1);
        }
        String output = "";
        String twoBit = "";
        String thresh = getArg("-n", args);
        String unique = getArg("-u", args);
        String topHatFlanking = getArg("-t", args);
        String debug_option = getArg("-d",args);
        if (debug_option != null) {
            DEBUG = true;
        }
        
        if(thresh != null)
            threshold = Integer.parseInt(thresh);
        if(unique != null)
            uniqueness = true;
        if(topHatFlanking != null)
            topHatStyleFlanking = true;
        output = getArg("-o", args);
        if((getArg("-s", args) != null) && (getArg("-b" , args) != null)){
            System.err.println("Both -s and -b cannot be given together");
            return;
        }
        if((getArg("-s", args) == null) && (getArg("-b", args) == null)){
            System.err.println("Provide either -s or -b option to decide strands");
            return;
        }
        twoBit = getArg("-b", args);
        if(getArg("-s", args) != null){
            twoTracks = true;
            twoBit = null;
        }        
        fJ.init(input, output, threshold, twoTracks, twoBit, uniqueness, topHatStyleFlanking);
    }
    
    /* getArg is the method used to return the appropriate argument from the list of arguments 
     * using the option variable which is the first argument.
     */ 
    public static String getArg(String label, String[] args) {
        String to_return = null;
        boolean got_it = false;
        if (label != null && args != null) {
            for (String item : args) {
                if (got_it) {
                    to_return = item;
                    break;
                }
                if (item.equals(label)) {
                    got_it = true;
                }
            }
        }
        if (got_it && to_return == null) {
            to_return = "true";
        }
        return to_return;
    }
    
    /* This is the method where the control of the program gets started and all of the variables like input file output file and 2bit files gets assigned
     * and then calls the method to find the Junctions. 
     */
    public void init(String input, String output, int threshold, boolean twoTracks, String twoBit, boolean uniqueness, boolean topHatStyleFlanking) throws URISyntaxException, Exception{
        if(DEBUG)
            System.err.println("Initial Heap Memory: "+Runtime.getRuntime().freeMemory());
        URI inputURI, twoBitURI = null; 
        
        inputURI = relativeToAbsolute(input);
        if(twoBit != null){
            twoBitURI = relativeToAbsolute(twoBit);    
        }
        
        if(output != null){
            output = getAbsoluteFile(output).getAbsolutePath();
        }
        
        InputStream isreader = FindJunction.class.getResourceAsStream("/chromosomes.txt");
        SynonymLookup.getChromosomeLookup().loadSynonyms(isreader, true) ;
        GeneralUtils.safeClose(isreader);
        
        convertBAMToBed(inputURI , output, threshold, twoTracks, twoBitURI, uniqueness, topHatStyleFlanking);        
    }
    
    /* This method is used to convert the given file path from relative to absolute.
     */
    private URI relativeToAbsolute(String path) throws URISyntaxException{
        if(!(path.startsWith("file:") && !(path.startsWith("http:")) && !(path.startsWith("ftp:")))){
            return getAbsoluteFile(path).toURI();
        }
        return new URI(path);
    }
    
    /*Returns the File object at given path
     */
    private File getAbsoluteFile(String path){
        return new File(path).getAbsoluteFile();
    }
    
    //Takes BAM file in the given path as an input and filters it with the Simple Filter Class
    public void convertBAMToBed(URI input , String output, int threshold, boolean twoTracks, URI twoBit, boolean uniqueness, boolean topHatStyleFlanking) throws URISyntaxException, Exception{
        TwoBit twoBitFile = null;
        AnnotatedSeqGroup group = new AnnotatedSeqGroup("Find Junctions");
        String fileName = input.toString().substring(input.toString().lastIndexOf("/")+1, input.toString().lastIndexOf("."));
        
        BAM bam = new BAM(input, fileName, group);
        if(twoBit != null){
            twoBitFile = new TwoBit(twoBit, twoBit.toString(), group);
        }
      
        DataOutputStream dos;
        if(output != null){    
            dos = new DataOutputStream(new FileOutputStream(output));
        }
        else{
            dos = new DataOutputStream(System.out);
        } 
        
        WriteJunctionsThread writeJunction = new WriteJunctionsThread(bam, threshold, twoTracks, uniqueness, topHatStyleFlanking, dos, DEBUG);
        for(BioSeq bioSeq : bam.getChromosomeList()){
            SeqSpan currentSpan = new SimpleMutableSeqSpan(bioSeq.getMin(), bioSeq.getMax(), bioSeq);
            if(twoBitFile != null){
                BioSeq.addResiduesToComposition(bioSeq, twoBitFile.getRegionResidues(currentSpan), currentSpan);
            }
            writeJunction.run(bioSeq);
            bioSeq.setComposition(null);
        }
        
        GeneralUtils.safeClose(dos);
    }    
}