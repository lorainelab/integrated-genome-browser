/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.bai;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.comparator.BioSeqComparator;
import com.affymetrix.genometry.parsers.graph.WiggleData;
import static com.affymetrix.genometry.symloader.ProtocolConstants.FILE_PROTOCOL_SCHEME;
import static com.affymetrix.genometry.symloader.ProtocolConstants.FTP_PROTOCOL_SCHEME;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTPS_PROTOCOL_SCHEME;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTP_PROTOCOL_SCHEME;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LoadUtils;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometry.util.LocalUrlCacher;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import htsjdk.samtools.BAMFileSpan;
import htsjdk.samtools.Bin;
import htsjdk.samtools.BinList;
import htsjdk.samtools.BrowseableBAMIndex;
import htsjdk.samtools.Chunk;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pooja Nikhare
 */
public class BaiSymLoader extends SymLoader {

    ArrayList<Chromosomes> chromosomeList = new ArrayList<>();
    SamReader samReader = null;
    SamReader.PrimitiveSamReaderToSamReaderAdapter primitiveSamReader = null;

    String inputFileName = null;
    private StringBuilder output;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(BaiSymLoader.class);

    private static final List<LoadUtils.LoadStrategy> strategyList = new ArrayList<>();
    private static final Pattern field_regex = Pattern.compile("\\s+");  // one or more whitespace

    private static final boolean ensure_unique_id = true;
    
    static {
        strategyList.add(LoadUtils.LoadStrategy.NO_LOAD);
        strategyList.add(LoadUtils.LoadStrategy.VISIBLE);
//		strategyList.add(LoadStrategy.CHROMOSOME);
        strategyList.add(LoadUtils.LoadStrategy.GENOME);
    }

    public BaiSymLoader(URI uri, Optional<URI> indexUri, String featureName, GenomeVersion genomeVersion) {
        super(uri, indexUri, featureName, genomeVersion);
        output = new StringBuilder();
        initializeChromosomes(uri);
    }

    /**
     *
     * @param uri It contains input BAI file location Gets genome structure info
     * - the list of chromosome names and their sizes Iterates through the
     * chromosomes list. It contains sequenceName and sequence length For each
     * chromosome, goes to the BAI file and gets the value for each 16,000 base
     * bin. The output is used to create graph Sym
     */
    private void initializeChromosomes(URI uri) {
        inputFileName = FilenameUtils.getBaseName(uri.getPath()); // -> file

        final SamReaderFactory samReaderFactory = SamReaderFactory.makeDefault().
                setOption(SamReaderFactory.Option.CACHE_FILE_BASED_INDEXES, Boolean.TRUE).
                validationStringency(ValidationStringency.LENIENT);

        InputStream bamFile = null;
        BrowseableBAMIndex browseableIndex = null;
        File inputBAIFile = null;
        File bamFileFromBai = null;
        URI bamFileFromBaiURI = null;
        ArrayList<ChromosomeOutput> finalOutput = new ArrayList<>();
        Map<Integer,Double> idToMeanMap = new HashMap<>();

        /**
         * If there an existing BAM file present in the same location as of BAI
         * location then sequence of chromosome is retrieved from BAM file
         * header
         */
        try {
            if (uri.getPath().contains("bam.bai")) {
                bamFileFromBaiURI = new URI(uri.toString().replace(".bai", ""));
            } else {
                bamFileFromBaiURI = new URI(uri.toString().replace(".bai", ".bam"));
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }

        /**
         * Checks the input scheme to support different file input sources(Local
         * file, URL from Internet)
         */
        String scheme = uri.getScheme().toLowerCase();
        if (StringUtils.equals(scheme, FILE_PROTOCOL_SCHEME)) {
            inputBAIFile = new File(uri);
            bamFileFromBai = new File(bamFileFromBaiURI);
        } else if (StringUtils.equals(scheme, HTTP_PROTOCOL_SCHEME) || StringUtils.equals(scheme, HTTPS_PROTOCOL_SCHEME)) {
            inputBAIFile = LocalUrlCacher.convertURIToFile(uri);
            bamFileFromBai = LocalUrlCacher.convertURIToFile(bamFileFromBaiURI);
        } else if (scheme.startsWith(FTP_PROTOCOL_SCHEME)) {
            inputBAIFile = LocalUrlCacher.convertURIToFile(uri);
            bamFileFromBai = LocalUrlCacher.convertURIToFile(bamFileFromBaiURI);
        } else {
            Logger.getLogger(BaiSymLoader.class.getName()).log(
                    Level.SEVERE, "URL scheme: {0} not recognized", scheme);
        }

        /**
         * Get empty.bai file from Bai project resources folder
         */
        bamFile = findBAMFile();

        if (chromosomeList.isEmpty() && bamFileFromBai != null && bamFileFromBai.exists() && !bamFileFromBai.isDirectory()) {
            primitiveSamReader = (SamReader.PrimitiveSamReaderToSamReaderAdapter) samReaderFactory.open(bamFileFromBai);
            if (primitiveSamReader != null && primitiveSamReader.underlyingReader() != null) {
                SAMFileHeader samFileHeader = primitiveSamReader.underlyingReader().getFileHeader();
                if (samFileHeader != null) {
                    SAMSequenceDictionary samSequenceDictionary = samFileHeader.getSequenceDictionary();
                    if (samSequenceDictionary != null) {
                        List<SAMSequenceRecord> samequenceRecordList = samSequenceDictionary.getSequences();
                        if (samequenceRecordList != null) {
                            for (int i = 0; i < samequenceRecordList.size(); i++) {
                                Chromosomes chromosome = new Chromosomes(samequenceRecordList.get(i).getSequenceName(), samequenceRecordList.get(i).getSequenceLength());
                                chromosomeList.add(chromosome);
                            }
                        }
                    }
                }
            }
        }

        /**
         * Gets genome structure info - the map of chromosome names and their
         * sizes. Add this information to the chromosomeList
         */
        if (chromosomeList.isEmpty()) {
            Map<String, Integer> chromosomesMap = GeneralLoadUtils.getAssemblyInfo();
            chromosomesMap.entrySet().forEach((spe) -> {
                Chromosomes chromosome = new Chromosomes(spe.getKey(), spe.getValue());
                chromosomeList.add(chromosome);
            });
        }

        /**
         * Gets BrowsableIndex for browsing the input BAI file
         */
        samReader = samReaderFactory.open(bamFile, inputBAIFile);
        final SamReader.Indexing indexing = samReader.indexing();
        browseableIndex = indexing.getBrowseableIndexAlt();

        /**
         * Iterate through the chromosomes list
         */
        //output.append("#chrom	start	end	length\n");
        for (int tid = 0; tid < chromosomeList.size(); ++tid) {
            double mean = 0, total = 0, count = 0,currChunk = 0;
            BinList binList = browseableIndex.getBinsOverlapping(tid, 1, chromosomeList.get(tid).getSequenceLength());
            //Calculating the mean of the chunks to scale the graph
            for (final Bin binItem : binList) {
                if (String.valueOf(browseableIndex.getLevelForBin(binItem)).equals("5")) {
                    String firstLocusInBin = String.valueOf(browseableIndex.getFirstLocusInBin(binItem) - 1);
                    String lastLocusInBin = String.valueOf(browseableIndex.getLastLocusInBin(binItem));
                    final BAMFileSpan span = browseableIndex.getSpanOverlapping(binItem);
                    if (span != null && !String.valueOf(span.getFirstOffset()).equals("0")) {
                         if (browseableIndex.getLastLocusInBin(binItem) > chromosomeList.get(tid).getSequenceLength()) {
                            lastLocusInBin = String.valueOf(chromosomeList.get(tid).getSequenceLength());
                        }
                        final List<Chunk> chunks = span.getChunks();
                        currChunk = chunks.get(0).getChunkEnd() - chunks.get(0).getChunkStart();
                        ChromosomeOutput chr = new ChromosomeOutput(tid, chromosomeList.get(tid).getSequenceName(), firstLocusInBin, lastLocusInBin,currChunk);
                        finalOutput.add(chr);
                        total += currChunk;                        
                        count++;
                    }
                }
            }
            if (count > 0) {
                mean = total / count;
                idToMeanMap.put(tid,mean);
            }
        }
        
        for (ChromosomeOutput chr : finalOutput){
            output.append(chr.getChrom()+" "+chr.getStart()+" "+chr.getEnd()+" "+chr.getLength()/idToMeanMap.get(chr.getTid())+"\n");           
        }  
        
    }

    /**
     *
     * @return It returns empty.bam file InputStream from the Bai project
     * resources folder
     */
    public InputStream findBAMFile() {
        InputStream bamFile = BaiSymLoader.class.getClassLoader().getResourceAsStream("empty.bam");
        return bamFile;
    }

    @Override
    public List<BioSeq> getChromosomeList() throws Exception {
        init();
        List<BioSeq> chromosomeList = new ArrayList<>(chrList.keySet());
        Collections.sort(chromosomeList, new BioSeqComparator());

        return chromosomeList;
    }

    @Override
    public List<LoadStrategy> getLoadChoices() {
        return strategyList;
    }

    @Override
    public void init() throws Exception {
        if (this.isInitialized) {
            return;
        }
        if (buildBaiIndex()) {
            super.init();
        }
    }

    /*
    Converts Bed File format string in Input stream and Parses each line
     */
    protected boolean buildBaiIndex() throws Exception {
        Map<String, Integer> chrLength = new HashMap<>();
        Map<String, File> chrFiles = new HashMap<>();

        try {
            logger.info("build index from Bai File");
            try (InputStream is = new ByteArrayInputStream(output.toString().getBytes(StandardCharsets.UTF_8))) {
                if (parseLines(is, chrLength, chrFiles)) {
                    createResults(chrLength, chrFiles);
                    return true;
                }
            }
        } catch (Exception ex) {
            throw ex;
        }
        return false;
    }
    
    @Override
    protected boolean parseLines(InputStream istr, Map<String, Integer> chrLength, Map<String, File> chrFiles) throws Exception {
        BufferedReader br = null;
        BufferedWriter bw = null;
        String line, current_seq_id = null;
        int length = 0;

        Map<String, BufferedWriter> chrs = new HashMap<>();
        /* line format:
         * chrom	start	end	length
         */
        try {

            br = new BufferedReader(new InputStreamReader(istr));
            Thread thread = Thread.currentThread();
            while ((line = br.readLine()) != null && !thread.isInterrupted()) {
                if (line.length() == 0) {
                    continue;
                }

                String[] fields = field_regex.split(line.trim());
                if (fields.length < 4) {
                    throw new IllegalArgumentException("format error: Improper line: " + line);
                }
                current_seq_id = fields[0];
                length = Integer.parseInt(fields[2]);
                if (!chrs.containsKey(current_seq_id)) {
                    addToLists(chrs, current_seq_id, chrFiles, chrLength, ".wig");
                }
                bw = chrs.get(current_seq_id);

                bw.write(line + "\n");
                if (length > chrLength.get(current_seq_id)) {
                    chrLength.put(current_seq_id, length);
                }
            }

            return !thread.isInterrupted();
        } catch (IOException | IllegalArgumentException ex) {
            throw ex;
        } finally {
            for (BufferedWriter b : chrs.values()) {
                try {
                    b.flush();
                } catch (IOException ex) {
                    Logger.getLogger(BaiSymLoader.class.getName()).log(Level.SEVERE, null, ex);
                }
                GeneralUtils.safeClose(b);
            }
            GeneralUtils.safeClose(br);
            GeneralUtils.safeClose(bw);
        }
    }

    @Override
    public List<GraphSym> getRegion(SeqSpan span) throws Exception {
        init();
        return parse(span.getBioSeq(), span.getMin(), span.getMax());
    }

    /**
     * Reads a BED4/ Wiggle-formatted file.
     */
    private List<GraphSym> parse(BioSeq seq, int min, int max) throws Exception {
        FileInputStream fis = null;
        InputStream istr = null;
        try {
            File file = chrList.get(seq);
            
            if (file == null) {
                Logger.getLogger(BaiSymLoader.class.getName()).log(Level.FINE, "Could not find chromosome {0}", seq.getId());
                return Collections.<GraphSym>emptyList();
            }

            final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

            Iterator<String> it = new Iterator<String>() {

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public String next() {
                    try {
                        return br.readLine();
                    } catch (IOException x) {
                        Logger.getLogger(this.getClass().getName()).log(
                                Level.SEVERE, "error reading wig file", x);
                    }
                    return null;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };

            return parse(it, seq, min, max);

        } catch (Exception ex) {
            throw ex;
        } finally {
            GeneralUtils.safeClose(istr);
            GeneralUtils.safeClose(fis);
        }

    }

    private List<GraphSym> parse(Iterator<String> it, BioSeq seq, int min, int max) {
        List<GraphSym> grafs = new ArrayList<>();
        WiggleData current_data = null;
        Map<String, WiggleData> current_datamap = new HashMap<>(); // Map: seq_id -> WiggleData
        String line;

        while ((line = it.next()) != null && !Thread.currentThread().isInterrupted()) {
            if (line.length() == 0) {
                continue;
            }
            String[] fields = field_regex.split(line.trim()); // trim() because lines are allowed to start with whitespace
            parseDataLine(fields, current_data, current_datamap, min, max);
        }
        grafs.addAll(createGraphSyms(genomeVersion, current_datamap, uri.toString(), extension));

        return grafs;
    }

    /**
     * Parse a single line of data (BED4 format).
     *
     * @param fields
     * @param current_data
     * @param current_datamap
     */
    private static void parseDataLine(String[] fields, WiggleData current_data, Map<String, WiggleData> current_datamap, int min, int max) {

        // chrom  start end value
        String seq_id = fields[0];	// chrom
        current_data = current_datamap.get(seq_id);
        if (current_data == null) {
            current_data = new WiggleData(seq_id);
            current_datamap.put(seq_id, current_data);
        }

        int x1 = Integer.parseInt(fields[1]);	// start, or perhaps end
        int x2 = Integer.parseInt(fields[2]);	// start, or perhaps end
        int start = Math.min(x1, x2);
        int width = Math.max(x1, x2) - start;

        if (!checkRange(x1, width, min, max)) {
            return;
        }

        current_data.add(x1, Float.parseFloat(fields[3]), width);
    }

    private static boolean checkRange(int start, int width, int min, int max) {
        //getChromosome && getRegion
        return !(start + width < min || start > max);
    }

    /**
     * Finishes the current data section and creates a list of GraphSym objects.
     */
    private static List<GraphSym> createGraphSyms(GenomeVersion seq_group,
            Map<String, WiggleData> current_datamap, String stream_name, String extension) {
        if (current_datamap == null) {
            return Collections.<GraphSym>emptyList();
        }

        List<GraphSym> grafs = new ArrayList<>(current_datamap.size());

        String graph_id = null;
        if (graph_id == null) {
            graph_id = stream_name;
        }

        // Need iterator because we're removing data on the fly
        Iterator<WiggleData> wiggleDataIterator = current_datamap.values().iterator();
        while (wiggleDataIterator.hasNext()) {
            GraphSym gsym = wiggleDataIterator.next().createGraph(seq_group, graph_id, stream_name);

            if (gsym != null) {
                grafs.add(gsym);
            }
            wiggleDataIterator.remove();	// free up memory now that we've created the graph.
        }

        return grafs;
    }
}

/**
 *
 * Chromosomes class contains sequenceName and SequenceLength
 */
class Chromosomes {

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

class ChromosomeOutput {
   int tid;
    String chrom;
    String start;
    String end;
    Double length;

     public ChromosomeOutput(int tid,String chrom, String start, String end, Double length) {
        this.tid = tid;
        this.chrom = chrom;
        this.start = start;
        this.end = end;
        this.length = length;
    }
     
    public int getTid() {
        return tid;
    }

    public void setTid(int tid) {
        this.tid = tid;
    }

    public String getChrom() {
        return chrom;
    }

    public void setChrom(String chrom) {
        this.chrom = chrom;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }
    
    public Double getLength() {
        return length;
    }

    public void setLength(Double length) {
        this.length = length;
    }
    
}
