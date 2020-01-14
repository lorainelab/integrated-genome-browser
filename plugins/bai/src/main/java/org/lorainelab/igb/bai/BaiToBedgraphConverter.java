package org.lorainelab.igb.bai;

import static com.affymetrix.genometry.symloader.ProtocolConstants.FILE_PROTOCOL_SCHEME;
import static com.affymetrix.genometry.symloader.ProtocolConstants.FTP_PROTOCOL_SCHEME;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTPS_PROTOCOL_SCHEME;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTP_PROTOCOL_SCHEME;
import com.affymetrix.genometry.util.LocalUrlCacher;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.io.File;
import java.net.URI;
import org.slf4j.LoggerFactory;

import htsjdk.samtools.BAMFileSpan;
import htsjdk.samtools.Bin;
import htsjdk.samtools.BinList;
import htsjdk.samtools.BrowseableBAMIndex;
import htsjdk.samtools.Chunk;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.io.FilenameUtils;


/**
 *
 * @author Sai Charan Reddy Vallapureddy
 * @email  vallapucharan@gmail.com
 */
public class BaiToBedgraphConverter{
    
    ArrayList<Chromosomes> chromosomeList = new ArrayList<>();
    SamReader samReader = null;
    File bedGraphFile = null;
    String inputFileName = null;
    
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(BaiToBedgraphConverter.class);
    
    /**
     * 
     * @param uri It contains input BAI file location.
     */
    public BaiToBedgraphConverter(URI uri){
        initializeChromosomes(uri);
    }
    
    /**
     * 
     * @param uri It contains input BAI file location
     * Gets genome structure info - the list of chromosome names and their sizes
     * Iterates through the chromosomes list. It contains sequenceName and sequence length
     * For each chromosome, goes to the BAI file and gets the value for each 16,000 base bin
     * Lastly writes to a temporary “bedgraph” file in the default temp location
     */
    private void initializeChromosomes(URI uri) {
        inputFileName = FilenameUtils.getBaseName(uri.getPath()); // -> file
        InputStream bamFile = null;
        BrowseableBAMIndex browseableIndex = null;
        FileWriter writer=null;
        File inputBAIFile = null;
        
        /**
         * Checks the input scheme to support different file input sources(Local file, URL from Internet)
         */
        String scheme = uri.getScheme().toLowerCase();
        if (StringUtils.equals(scheme, FILE_PROTOCOL_SCHEME)) {
            inputBAIFile = new File(uri);
        }else if (StringUtils.equals(scheme, HTTP_PROTOCOL_SCHEME) || StringUtils.equals(scheme, HTTPS_PROTOCOL_SCHEME)) {
            inputBAIFile = LocalUrlCacher.convertURIToFile(uri);
        }else if (scheme.startsWith(FTP_PROTOCOL_SCHEME)) {
            inputBAIFile = LocalUrlCacher.convertURIToFile(uri);
        }else{
            Logger.getLogger(BaiToBedgraphConverter.class.getName()).log(
                    Level.SEVERE, "URL scheme: {0} not recognized", scheme);
        }
        
        
        /**
         * Get empty.bai file from Bai project resources folder
         */
        bamFile = findBAMFile();
        
        /**
         * Gets genome structure info - the map of chromosome names and their sizes. Add this information to the chromosomeList
         */
        final Map<String, Integer> chromosomesMap = GeneralLoadUtils.getAssemblyInfo();
        chromosomesMap.entrySet().forEach((spe) -> {
             Chromosomes chromosome = new Chromosomes(spe.getKey(), spe.getValue());
             chromosomeList.add(chromosome);
        });
        
        /**
         * Gets BrowsableIndex for browsing the input BAI file
         */
        final SamReaderFactory samReaderFactory = SamReaderFactory.makeDefault().
					setOption(SamReaderFactory.Option.CACHE_FILE_BASED_INDEXES, Boolean.TRUE).
					validationStringency(ValidationStringency.LENIENT);
        samReader = samReaderFactory.open(bamFile,inputBAIFile);
        final SamReader.Indexing indexing = samReader.indexing();
        browseableIndex = indexing.getBrowseableIndexAlt();
        
        /**
         * Iterate through the chromosomes list
         */
        StringBuilder output = new StringBuilder();
        output.append("#chrom	start	end	length\n");
        
        /**
         *  Create a temporary bedgraph file in java.io.dir
         *  Delete the file on exit of IGB
         */
        try {
            writer = new FileWriter(createBedGraphFile(inputBAIFile));
        } catch (IOException ex) {
            Logger.getLogger(BaiToBedgraphConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        for(int tid=0;tid< chromosomeList.size();++tid)
            {
                double mean=0,total=0,count=0;
                BinList binList =browseableIndex.getBinsOverlapping(tid, 1, chromosomeList.get(tid).getSequenceLength());
                //Calculating the mean of the chunks to scale the graph
                for(final Bin binItem:binList)
                    {
                        if(String.valueOf(browseableIndex.getLevelForBin(binItem)).equals("5"))
                        {
                            final BAMFileSpan span= browseableIndex.getSpanOverlapping(binItem);
                            if(!String.valueOf(span.getFirstOffset()).equals("0"))
                            {
                                final List<Chunk> chunks = span.getChunks();
                                total += chunks.get(0).getChunkEnd() - chunks.get(0).getChunkStart();
                                count++;
                            }
                        }
                    }
                if(count>0)
                    mean = total/count;
                
                /**
                 * For each chromosome, goes to the BAI file and gets the value for each 16,000 base bin
                 */
                for(final Bin binItem:binList)
                {
                    if(String.valueOf(browseableIndex.getLevelForBin(binItem)).equals("5"))
                    {
                        String firstLocusInBin = String.valueOf(browseableIndex.getFirstLocusInBin(binItem)-1);
                        String lastLocusInBin = String.valueOf(browseableIndex.getLastLocusInBin(binItem));
                        final BAMFileSpan span= browseableIndex.getSpanOverlapping(binItem);
                        if(!String.valueOf(span.getFirstOffset()).equals("0"))
                        {
                            if(browseableIndex.getLastLocusInBin(binItem) > chromosomeList.get(tid).getSequenceLength())
                                lastLocusInBin = String.valueOf(chromosomeList.get(tid).getSequenceLength());
                            final List<Chunk> chunks = span.getChunks();
                            output.append(chromosomeList.get(tid).getSequenceName()
                                    +  "   " + firstLocusInBin
                                    +  "   " + lastLocusInBin
                                    +  "   " + (chunks.get(0).getChunkEnd() - chunks.get(0).getChunkStart())/mean + "\n");
                        }
                    }
                }
            }
        /**
         * Write the output to a temporary bedgraph file
         */
        try {
            writer.write(output.toString());
        } catch (IOException ex) {
            Logger.getLogger(BaiToBedgraphConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally{
            try {
                writer.close();
            }
            catch (IOException ex) {
                Logger.getLogger(BaiToBedgraphConverter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
     }
    
    
    /**
     * 
     * @return It returns empty.bam file InputStream from the Bai project resources folder
     */
    public InputStream findBAMFile() {
            InputStream bamFile = BaiToBedgraphConverter.class.getClassLoader().getResourceAsStream("empty.bam");
            return bamFile;    
    }
    
    /**
     * 
     * @param baifile It takes bai file location and creates a temporary bedgraph file in the default temp location
     * deletes the temp bedgraph file when IGB is closed
     * @return It returns newly created temporary bedgraph file.
     */
    public File createBedGraphFile (File baifile) throws IOException {
            String tempPath = System.getProperty("java.io.tmpdir");
            inputFileName = inputFileName + ".";
            bedGraphFile = new File(tempPath, inputFileName + "bedgraph");
            if (bedGraphFile.exists()) {
                bedGraphFile.delete();
            }
            try {
                bedGraphFile.createNewFile();
            } catch (IOException ex) {
            }
            bedGraphFile.deleteOnExit();
            return bedGraphFile;    
    }
    
   /**
    * 
    * @return temporary bedgraph file
    */
    public File returnTempBedgraphFile()
    {
        return bedGraphFile;
    }
}

/**
 * 
 * Chromosomes class contains sequenceName and SequenceLength
 */
class Chromosomes{

    public String getSequenceName() {
        return sequenceName;
    }

    public int getSequenceLength() {
        return sequenceLength;
    }

    public Chromosomes(String sequenceName, int sequenceLength) {
        this.sequenceName = sequenceName;
        this.sequenceLength = sequenceLength;
    }
    String sequenceName;
    int sequenceLength;
}