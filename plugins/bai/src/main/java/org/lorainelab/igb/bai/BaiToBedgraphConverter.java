package org.lorainelab.igb.bai;

import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.io.File;
import java.net.URI;
import org.slf4j.LoggerFactory;

import htsjdk.samtools.BAMFileSpan;
import htsjdk.samtools.Bin;
import htsjdk.samtools.BinList;
import htsjdk.samtools.BrowseableBAMIndex;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Sai Charan Reddy Vallapureddy
 * @email  vallapucharan@gmail.com
 */
public class BaiToBedgraphConverter{
    
    ArrayList<Chromosomes> chromosomeList = new ArrayList<>();
    SamReader samReader = null;
    
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
     * Lastly writes to a “bedgraph” file in the same location
     */
    private void initializeChromosomes(URI uri) {
        InputStream bamFile = null;
        BrowseableBAMIndex browseableIndex = null;
        FileWriter writer=null;
        File inputBAIFile = new File(uri);
        
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
        for(int tid=0;tid< chromosomeList.size();++tid)
            {
                int check = 0;
                String prevSequenceName = null, prevFirstLocusInBin =null, prevLastLocusInBin = null;
                double prevFirstOffset = 0;
                double veryFirstOffset = 0;
                    try{
                        writer = new FileWriter(createBedGraphFile(inputBAIFile));
                        BinList binList =browseableIndex.getBinsOverlapping(tid, 1, chromosomeList.get(tid).getSequenceLength());
                        /**
                         * For each chromosome, goes to the BAI file and gets the value for each 16,000 base bin
                         */
                        for(final Bin binItem:binList)
                        {
                            if(String.valueOf(browseableIndex.getLevelForBin(binItem)).equals("5"))
                            {
                                final BAMFileSpan span= browseableIndex.getSpanOverlapping(binItem);
                                if(!String.valueOf(span.getFirstOffset()).equals("0"))
                                {
                                    if(check == 0)
                                        {
                                            check = check + 1;
                                            veryFirstOffset = span.getFirstOffset();
                                            prevSequenceName = chromosomeList.get(tid).getSequenceName();
                                            prevFirstLocusInBin = String.valueOf(browseableIndex.getFirstLocusInBin(binItem));
                                            prevLastLocusInBin = String.valueOf(browseableIndex.getLastLocusInBin(binItem));
                                            prevFirstOffset = span.getFirstOffset();
                                            continue;
                                        }
                                    output.append(prevSequenceName
                                            +  "   " + prevFirstLocusInBin
                                            +  "   " + prevLastLocusInBin
                                            +  "   " + ((span.getFirstOffset() - prevFirstOffset)/1000000) + "\n");
                                    prevSequenceName = chromosomeList.get(tid).getSequenceName();
                                    prevFirstLocusInBin = String.valueOf(browseableIndex.getFirstLocusInBin(binItem));
                                    prevLastLocusInBin = String.valueOf(browseableIndex.getLastLocusInBin(binItem));
                                    prevFirstOffset = span.getFirstOffset();
                                }
                            }
                        }
                    }
                    catch (IOException ex) 
                    {
                        Logger.getLogger(BaiToBedgraphConverter.class.getName()).log(Level.SEVERE, null, ex);
                    }
                check = 0;
            }
        /**
         * Write the output to a bedgraph file
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
     * @param baifile It takes bai file location and creates a bedgraph file in the same location
     * @return It returns newly created bedgraph file.
     */
    public static File createBedGraphFile (File baifile) {
            String path = baifile.getPath();
            path = path.substring(0, path.length() - 3) + "bedgraph";
            File bedGraphFile = new File(path);
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