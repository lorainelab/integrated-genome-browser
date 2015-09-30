package com.affymetrix.genometry.symloader;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryConstants;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.comparator.BioSeqComparator;
import com.affymetrix.genometry.comparator.SeqSymMinComparator;
import static com.affymetrix.genometry.parsers.BedParser.makeBlockMaxs;
import static com.affymetrix.genometry.parsers.BedParser.makeBlockMins;
import static com.affymetrix.genometry.parsers.BedParser.parseIntArray;
import com.affymetrix.genometry.parsers.TrackLineParser;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleSymWithProps;
import com.affymetrix.genometry.symmetry.impl.UcscBedDetailSym;
import com.affymetrix.genometry.symmetry.impl.UcscBedSym;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.broad.tribble.readers.LineReader;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hiralv
 */
public class BED extends SymLoader implements LineProcessor {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(BED.class);
    private static final int BED_DETAIL_FIELD_COUNT = 14;
    private static final String BED_MIME_TYPE = "text/bed";
    private static final String BED_FILE_EXTENSION = ".bed";
    private static final String BED_DETAIL_TRACKLINE_TYPE = "bedDetail";
    private static final Pattern LINE_REGEX = Pattern.compile("\\s+");
    private static final Pattern TAB_REGEX = Pattern.compile("\\t");
    private static final List<LoadStrategy> strategyList = new ArrayList<>();

    static {
        strategyList.add(LoadStrategy.NO_LOAD);
        strategyList.add(LoadStrategy.VISIBLE);
        strategyList.add(LoadStrategy.GENOME);
    }

    private boolean annotate_seq = true;
    private boolean create_container_annot = false;
    private String trackName = null;
    private final TrackLineParser trackLineParser;
    private String bedFileType;
    private final GenometryModel gmodel;

    public BED(URI uri, Optional<URI> indexUri, String featureName, GenomeVersion genomeVersion) {
        super(uri, indexUri, featureName, genomeVersion);
        gmodel = GenometryModel.getInstance();
        trackLineParser = new TrackLineParser();
        trackName = uri.toString();
        annotate_seq = false;
        create_container_annot = false;
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
        if (buildIndex()) {
            sortCreatedFiles();
            super.init();
        }
    }

    @Override
    public List<BioSeq> getChromosomeList() throws Exception {
        init();
        List<BioSeq> chromosomeList = new ArrayList<>(chrList.keySet());
        Collections.sort(chromosomeList, new BioSeqComparator());
        return chromosomeList;
    }

    @Override
    public List<SeqSymmetry> getGenome() throws Exception {
        init();
        List<BioSeq> allSeq = getChromosomeList();
        List<SeqSymmetry> retList = new ArrayList<>();
        for (BioSeq seq : allSeq) {
            retList.addAll(getChromosome(seq));
        }
        return retList;
    }

    @Override
    public List<SeqSymmetry> getChromosome(BioSeq seq) throws Exception {
        init();
        return parse(seq, seq.getMin(), seq.getMax());
    }

    @Override
    public List<SeqSymmetry> getRegion(SeqSpan span) throws Exception {
        init();
        return parse(span.getBioSeq(), span.getMin(), span.getMax());
    }

    private List<SeqSymmetry> parse(BioSeq seq, int min, int max) throws Exception {
        File file = chrList.get(seq);
        boolean isSorted = chrSort.get(seq);
        try (InputStream istr = new FileInputStream(file)) {
            return parse(istr, isSorted, min, max);
        }
    }

    private List<SeqSymmetry> parse(LineReader lineReader, boolean isSorted, int min, int max) {
        List<SeqSymmetry> symlist = new ArrayList<>();
        try {
            List<String> lines = new ArrayList<>();
            String nextLine;
            while ((nextLine = lineReader.readLine()) != null) {
                lines.add(nextLine);
            }
            lineReader.close();
            symlist = parseLines(lines, min, max, isSorted);

        } catch (IOException ex) {
            logger.error("Error reading lines from Bed File", ex);
        }
        return symlist;
    }

    private List<SeqSymmetry> parse(InputStream istr, boolean isSorted, int min, int max) throws IOException {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(istr))) {
            List<String> lines = CharStreams.readLines(reader);
            return parseLines(lines, min, max, isSorted);
        }
    }

    private List<SeqSymmetry> parseLines(List<String> lines, int min, int max, boolean isSorted) {
        List<SeqSymmetry> symlist = new ArrayList<>();
        Map<BioSeq, Map<String, SeqSymmetry>> seq2types = new HashMap<>();

        for (String line : lines) {
            if (!Strings.isNullOrEmpty(line)) {
                if (isTrackLine(line)) {
                    processTrackLine(line);
                } else if (isBrowserLine(line)) {
                    // currently take no action for browser lines
                } else {
                    if (!parseLine(line, min, max, symlist, seq2types) && isSorted) {
                        break;
                    }
                }
            }
        }
        return symlist;
    }

    private static boolean isTrackLine(String line) {
        return line.startsWith("track");
    }

    private void processTrackLine(String line) {
        String defaultName;
        trackLineParser.parseTrackLine(line);
        String trackLineName = trackLineParser.getCurrentTrackHash().get(TrackLineParser.NAME);
        if (StringUtils.isNotBlank(trackLineName)) {
            defaultName = trackLineName;
            trackLineParser.getCurrentTrackHash().put(TrackLineParser.NAME, defaultName);
        }
        bedFileType = trackLineParser.getCurrentTrackHash().get("type");
    }

    @Override
    public List<? extends SeqSymmetry> processLines(BioSeq seq, final LineReader lineReader) {
        return parse(lineReader, true, 0, Integer.MAX_VALUE);
    }

    private boolean parseLine(
            String line,
            int minimum,
            int maximum,
            List<SeqSymmetry> symlist,
            Map<BioSeq, Map<String, SeqSymmetry>> seq2types) {
        String[] fields = TAB_REGEX.split(line);
        if (fields.length == 1) {
            fields = LINE_REGEX.split(line);
        }
        String detailSymbol = null;
        String detailDescription = null;
        int fieldCount = fields.length;
        boolean isBedDetail = isBedDetailType(bedFileType);

        if (!isBedDetail && fieldCount == BED_DETAIL_FIELD_COUNT) {
            isBedDetail = true;
        }
        if (isBedDetail && fieldCount == BED_DETAIL_FIELD_COUNT) {
            isBedDetail = true;
            detailSymbol = fields[fieldCount - 2];
            detailDescription = fields[fieldCount - 1];
            fieldCount -= 2;
        }

        if (fieldCount < 3) {
            return true;
        }

        String seq_name = null;
        String geneName = null;
        int min;
        int max;
        String itemRgb = "";
        int thick_min = Integer.MIN_VALUE; // Integer.MIN_VALUE signifies that thick_min is not used
        int thick_max = Integer.MIN_VALUE; // Integer.MIN_VALUE signifies that thick_max is not used
        float score = Float.NEGATIVE_INFINITY; // Float.NEGATIVE_INFINITY signifies that score is not used
        boolean forward;
        int[] blockSizes = null;
        int[] blockStarts = null;
        int[] blockMins = null;
        int[] blockMaxs = null;
        boolean includes_bin_field = fieldCount > 6 && (fields[6].startsWith("+") || fields[6].startsWith("-") || fields[6].startsWith("."));
        int findex = 0;
        if (includes_bin_field) {
            findex++;
        }
        seq_name = fields[findex++]; // seq id field

        int beg = Integer.parseInt(fields[findex++]); // start field
        int end = Integer.parseInt(fields[findex++]); // stop field
        if (fieldCount >= 4) {
            geneName = BedUtils.parseName(fields[findex++]);
            if (geneName == null || geneName.length() == 0) {
                geneName = genomeVersion.getName();
            }
        }
        if (fieldCount >= 5) {
            score = BedUtils.parseScore(fields[findex++]);
        } // score field
        if (fieldCount >= 6) {
            forward = !(fields[findex++].equals("-"));
        } else {
            forward = (beg <= end);
        }
        min = Math.min(beg, end);
        max = Math.max(beg, end);

        if (!BedUtils.checkRange(min, max, minimum, maximum)) {
            return max <= maximum;
        }

        BioSeq seq = genomeVersion.getSeq(seq_name);
        if ((seq == null) && (seq_name.indexOf(';') > -1)) {
            // if no seq found, try and split up seq_name by ";", in case it is in format
            //    "seqid;genome_version"
            String seqid = seq_name.substring(0, seq_name.indexOf(';'));
            String version = seq_name.substring(seq_name.indexOf(';') + 1);
            if ((gmodel.getSeqGroup(version) == genomeVersion) || genomeVersion.getName().equals(version)) {
                // for format [chrom_name];[genome_version]
                seq = genomeVersion.getSeq(seqid);
                if (seq != null) {
                    seq_name = seqid;
                }
            } else if ((gmodel.getSeqGroup(seqid) == genomeVersion) || genomeVersion.getName().equals(seqid)) {
                // for format [genome_version];[chrom_name]
                seqid = version;
                seq = genomeVersion.getSeq(seqid);
                if (seq != null) {
                    seq_name = seqid;
                }
            }
        }
        if (seq == null) {
            //System.out.println("seq not recognized, creating new seq: " + seq_name);
            seq = genomeVersion.addSeq(seq_name, 0, uri.toString());
        }

        if (fieldCount >= 8) {
            thick_min = Integer.parseInt(fields[findex++]); // thickStart field
            thick_max = Integer.parseInt(fields[findex++]); // thickEnd field
        }
        if (fieldCount >= 9) {
            itemRgb = fields[findex++];
        } else {
            findex++;
        }
        if (fieldCount >= 12) {
            int blockCount = Integer.parseInt(fields[findex++]); // blockCount field
            blockSizes = parseIntArray(fields[findex++]); // blockSizes field
            if (blockCount != blockSizes.length) {
                System.out.println("WARNING: block count does not agree with block sizes.  Ignoring " + geneName + " on " + seq_name);
                return true;
            }
            blockStarts = parseIntArray(fields[findex++]); // blockStarts field
            if (blockCount != blockStarts.length) {
                System.out.println("WARNING: block size does not agree with block starts.  Ignoring " + geneName + " on " + seq_name);
                return true;
            }
            blockMins = makeBlockMins(min, blockStarts);
            blockMaxs = makeBlockMaxs(blockSizes, blockMins);
        } else {
            /*
             * if no child blocks, make a single child block the same size as the parent
             * Very Inefficient, ideally wouldn't do this
             * But currently need this because of GenericAnnotGlyphFactory use of annotation depth to
             *     determine at what level to connect glyphs -- if just leave blockMins/blockMaxs null (no children),
             *     then factory will create a line container glyph to draw line connecting all the bed annots
             * Maybe a way around this is to adjust depth preference based on overall depth (1 or 2) of bed file?
             */
            blockMins = new int[1];
            blockMins[0] = min;
            blockMaxs = new int[1];
            blockMaxs[0] = max;
        }
        if (max > seq.getLength()) {
            seq.setLength(max);
        }

        SymWithProps bedline_sym;
        bedline_sym = isBedDetail
                ? new UcscBedDetailSym(trackName, seq, min, max, geneName, score, forward, thick_min, thick_max, blockMins, blockMaxs, detailSymbol, detailDescription)
                : new UcscBedSym(trackName, seq, min, max, geneName, score, forward, thick_min, thick_max, blockMins, blockMaxs);
        if (itemRgb != null) {
            java.awt.Color c = null;
            try {
                c = TrackLineParser.reformatColor(itemRgb);
            } catch (Exception e) {
                logger.error("Could not parse a color from String '{}'", itemRgb);
            }
            if (c != null) {
                bedline_sym.setProperty(TrackLineParser.ITEM_RGB, c);
            }
        }
        symlist.add(bedline_sym);
        if (annotate_seq) {
            this.annotationParsed(bedline_sym, seq2types);
        }
        return true;
    }

    private void annotationParsed(SeqSymmetry bedline_sym, Map<BioSeq, Map<String, SeqSymmetry>> seq2types) {
        BioSeq seq = bedline_sym.getSpan(0).getBioSeq();
        if (create_container_annot) {
            String type = trackLineParser.getCurrentTrackHash().get(TrackLineParser.NAME);
            if (type == null) {
                type = trackName;
            }
            Map<String, SeqSymmetry> type2csym = seq2types.get(seq);
            if (type2csym == null) {
                type2csym = new HashMap<>();
                seq2types.put(seq, type2csym);
            }
            SimpleSymWithProps parent_sym = (SimpleSymWithProps) type2csym.get(type);
            if (parent_sym == null) {
                parent_sym = new SimpleSymWithProps();
                parent_sym.addSpan(new SimpleSeqSpan(0, seq.getLength(), seq));
                parent_sym.setProperty("method", type);
                parent_sym.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
                seq.addAnnotation(parent_sym);
                type2csym.put(type, parent_sym);
            }
            parent_sym.addChild(bedline_sym);
        } else {
            seq.addAnnotation(bedline_sym);
        }
    }

    /**
     * Implementing AnnotationWriter interface to write out annotations to an
     * output stream as "BED" format.
     *
     */
    public boolean writeAnnotations(Collection<? extends SeqSymmetry> syms, BioSeq seq, String type, OutputStream outstream) {

        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new BufferedOutputStream(outstream));
            for (SeqSymmetry sym : syms) {
                BedUtils.writeSymmetry(dos, sym, seq);
            }
            dos.flush();
            return true;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return false;
    }

    public void writeSymmetry(SeqSymmetry sym, BioSeq seq, OutputStream os) throws IOException {
        DataOutputStream dos = null;
        if (os instanceof DataOutputStream) {
            dos = (DataOutputStream) os;
        } else {
            dos = new DataOutputStream(os);
        }
        BedUtils.writeSymmetry(dos, sym, seq);
    }

    public Comparator<SeqSymmetry> getComparator(BioSeq seq) {
        return new SeqSymMinComparator(seq);
    }

    public int getMin(SeqSymmetry sym, BioSeq seq) {
        SeqSpan span = sym.getSpan(seq);
        return span.getMin();
    }

    public int getMax(SeqSymmetry sym, BioSeq seq) {
        SeqSpan span = sym.getSpan(seq);
        return span.getMax();
    }

    public String getMimeType() {
        return BED_MIME_TYPE;
    }

    @Override
    protected boolean parseLines(InputStream istr, Map<String, Integer> chrLength, Map<String, File> chrFiles) throws Exception {
        BufferedReader br = null;
        BufferedWriter bw = null;

        Map<String, Boolean> chrTrack = new HashMap<>();
        Map<String, BufferedWriter> chrs = new HashMap<>();
        String line, trackLine = null, seq_name = null;
        String[] fields;
        int lineCounter = 0;

        try {
            Thread thread = Thread.currentThread();
            br = new BufferedReader(new InputStreamReader(istr));
            while ((line = br.readLine()) != null && (!thread.isInterrupted())) {
                lineCounter++;
                if (line.length() == 0) {
                    continue;
                }

                if (!line.startsWith("#") && !isBrowserLine(line)) {// skip comment lines
                    if (isTrackLine(line)) {
                        chrTrack = new HashMap<>();
                        trackLine = line;
                    } else {
                        fields = TAB_REGEX.split(line);
                        if (fields.length == 1) {
                            fields = LINE_REGEX.split(line);
                        }
                        if (fields.length < 3) {
                            logger.warn("Invalid line at {} in BED file", lineCounter);
                            continue;
                        }
                        boolean includes_bin_field = false;

                        if (fields.length > 6) {
                            includes_bin_field = (fields[6].equals("+") || fields[6].equals("-") || fields[6].equals("."));
                        }

                        int findex = 0;
                        if (includes_bin_field) {
                            findex++;
                        }

                        seq_name = fields[findex++]; // seq id field

                        int beg = Integer.parseInt(fields[findex++]); // start field;
                        int end = Integer.parseInt(fields[findex++]); // stop field
                        int max = Math.max(beg, end);

                        if (!chrs.containsKey(seq_name)) {
                            addToLists(chrs, seq_name, chrFiles, chrLength, BED_FILE_EXTENSION);
                        }
                        bw = chrs.get(seq_name);
                        if (!chrTrack.containsKey(seq_name)) {
                            chrTrack.put(seq_name, true);

                            if (trackLine != null) {
                                bw.write(trackLine + "\n");
                            }
                        }
                        bw.write(line + "\n");

                        if (max > chrLength.get(seq_name)) {
                            chrLength.put(seq_name, max);
                        }

                    }
                }
            }

            return !thread.isInterrupted();

        } catch (IOException ex) {
            if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains(TOO_MANY_CONTIGS_EXCEPTION.toLowerCase())) {
                ErrorHandler.errorPanel(GenometryConstants.BUNDLE.getString("tooManyContigsError"));
                return true;
            }
            throw ex;
        } finally {
            chrs.values().forEach(GeneralUtils::safeClose);
            GeneralUtils.safeClose(br);
            GeneralUtils.safeClose(bw);
        }

    }

    private static boolean isBrowserLine(String line) {
        return line.startsWith("browser");
    }

    @Override
    protected void createResults(Map<String, Integer> chrLength, Map<String, File> chrFiles) {
        for (Entry<String, Integer> bioseq : chrLength.entrySet()) {
            String seq_name = bioseq.getKey();
            BioSeq seq = genomeVersion.getSeq(seq_name);
            if ((seq == null) && (seq_name.indexOf(';') > -1)) {
                // if no seq found, try and split up seq_name by ";", in case it is in format
                //    "seqid;genome_version"
                String seqid = seq_name.substring(0, seq_name.indexOf(';'));
                String version = seq_name.substring(seq_name.indexOf(';') + 1);
                //            System.out.println("    seq = " + seqid + ", version = " + version);
                if ((gmodel.getSeqGroup(version) == genomeVersion) || genomeVersion.getName().equals(version)) {
                    // for format [chrom_name];[genome_version]
                    seq = genomeVersion.getSeq(seqid);
                    if (seq != null) {
                        seq_name = seqid;
                    }
                } else if ((gmodel.getSeqGroup(seqid) == genomeVersion) || genomeVersion.getName().equals(seqid)) {
                    // for format [genome_version];[chrom_name]
                    String temp = seqid;
                    seqid = version;
                    version = temp;
                    seq = genomeVersion.getSeq(seqid);
                    if (seq != null) {
                        seq_name = seqid;
                    }
                }
            }
            if (seq == null) {
                //System.out.println("seq not recognized, creating new seq: " + seq_name);
                seq = genomeVersion.addSeq(seq_name, bioseq.getValue(), uri.toString());
            }

            chrList.put(seq, chrFiles.get(seq_name));
        }
    }

    @Override
    public void init(URI uri) {
    }

    @Override
    public SeqSpan getSpan(String line) {
        return null; // not used yet
    }

    @Override
    public boolean processInfoLine(String line, List<String> infoLines) {
        return false; // not used yet
    }

    @Override
    public boolean isMultiThreadOK() {
        return true;
    }

    private boolean isBedDetailType(String bedFileType) {
        return BED_DETAIL_TRACKLINE_TYPE.equals(bedFileType);
    }

}
