package com.affymetrix.genometry.symloader;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.comparator.BioSeqComparator;
import com.affymetrix.genometry.comparator.UcscPslComparator;
import com.affymetrix.genometry.parsers.AnnotationWriter;
import com.affymetrix.genometry.parsers.IndexWriter;
import com.affymetrix.genometry.parsers.TrackLineParser;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.impl.Psl3Sym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetryConverter;
import com.affymetrix.genometry.symmetry.impl.SimpleSymWithProps;
import com.affymetrix.genometry.symmetry.impl.UcscPslSym;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometry.util.SeqUtils;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.broad.tribble.readers.LineReader;

/**
 *
 * @author hiralv
 */
public class PSL extends SymLoader implements AnnotationWriter, IndexWriter, LineProcessor {

    private static final UcscPslComparator comp = new UcscPslComparator();
    static final List<String> psl_pref_list = Arrays.asList("psl");
    static final List<String> link_psl_pref_list = Arrays.asList("link.psl", "bps", "psl");
    static final List<String> psl3_pref_list = Arrays.asList("psl3", "bps", "psl");
    boolean look_for_targets_in_query_group = false;
    boolean create_container_annot = false;
    boolean is_link_psl = false;
    public static final boolean DEBUG = false;
    final static Pattern line_regex = Pattern.compile("\t");
    final static Pattern comma_regex = Pattern.compile(",");
    final static Pattern tagval_regex = Pattern.compile("=");
    final static Pattern non_digit = Pattern.compile("[^0-9-]");
    final TrackLineParser track_line_parser = new TrackLineParser();
    String track_name_prefix = null;
    private static final String newLine = System.getProperty("line.separator");	// system-independent newline

    private final GenomeVersion query_group;
    private final GenomeVersion target_group;
    private final GenomeVersion other_group;
    private final boolean annotate_query;
    private final boolean annotate_target;
    private final boolean annotate_other;

    private static final List<LoadStrategy> strategyList = new ArrayList<>();

    static {
        strategyList.add(LoadStrategy.NO_LOAD);
        strategyList.add(LoadStrategy.VISIBLE);
//		strategyList.add(LoadStrategy.CHROMOSOME);
        strategyList.add(LoadStrategy.GENOME);
    }

    @Override
    public List<LoadStrategy> getLoadChoices() {
        return strategyList;
    }

    public PSL(URI uri, String featureName, GenomeVersion genomeVersion) {
        this(uri, featureName, genomeVersion, null, null, false, false, false);
    }

    public PSL(URI uri, String featureName, GenomeVersion target_group,
            GenomeVersion query_group, GenomeVersion other_group,
            boolean annotate_query, boolean annotate_target,
            boolean annotate_other) {
        super(uri, featureName, target_group);
        this.target_group = target_group;
        this.query_group = query_group;
        this.other_group = other_group;
        this.annotate_query = annotate_query;
        this.annotate_target = annotate_target;
        this.annotate_other = annotate_other;
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

    public void init(URI uri) {
    }

    @Override
    public List<BioSeq> getChromosomeList() throws Exception {
        init();
        List<BioSeq> chromosomeList = new ArrayList<>(chrList.keySet());
        Collections.sort(chromosomeList, new BioSeqComparator());
        return chromosomeList;
    }

    @Override
    public List<UcscPslSym> getGenome() throws Exception {
        init();
        List<BioSeq> allSeq = getChromosomeList();
        List<UcscPslSym> retList = new ArrayList<>();
        for (BioSeq seq : allSeq) {
            retList.addAll(getChromosome(seq));
        }
        return retList;
    }

    @Override
    public List<UcscPslSym> getChromosome(BioSeq seq) throws Exception {
        init();
        return parse(seq, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public List<UcscPslSym> getRegion(SeqSpan span) throws Exception {
        init();
        return parse(span.getBioSeq(), span.getMin(), span.getMax());
    }

    @Override
    protected boolean parseLines(InputStream istr, Map<String, Integer> chrLength, Map<String, File> chrFiles) throws Exception {
        BufferedWriter bw = null;
        BufferedReader br = null;
        Map<String, Boolean> chrTrack = new HashMap<>();
        Map<String, BufferedWriter> chrs = new HashMap<>();
        Map<String, Set<String>> queryTarget = new HashMap<>();
        String trackLine = null;

        if (DEBUG) {
            System.out.println("in PSL.parse(), create_container_annot: " + create_container_annot);
        }

        int line_count = 0, length = 0;
        String line = null, target_seq_id, query_seq_id;
        String[] fields = null;
        boolean in_bottom_of_link_psl = false;

        Thread thread = Thread.currentThread();
        try {
            br = new BufferedReader(new InputStreamReader(istr));

            while ((line = br.readLine()) != null && (!thread.isInterrupted())) {
                line_count++;
                // Ignore psl header lines
                if (line.trim().length() == 0) {
                    continue;
                }
                char firstchar = line.charAt(0);

                if (firstchar == '#'
                        || (firstchar == 'm' && line.startsWith("match\t"))
                        || (firstchar == '-' && line.startsWith("-------"))) {
                    continue;
                }

                if (firstchar == 't' && line.startsWith("track")) {
                    if (is_link_psl) {
                        Map<String, String> track_props = track_line_parser.parseTrackLine(line, track_name_prefix);
                        String track_name = track_props.get(TrackLineParser.NAME);
                        if (track_name != null && track_name.endsWith("probesets")) {
                            in_bottom_of_link_psl = true;
                        }
                    }
                    chrTrack = new HashMap<>();
                    trackLine = line;

                    if (in_bottom_of_link_psl) {
                        for (String seq_id : chrs.keySet()) {
                            bw = chrs.get(seq_id);
                            bw.write(trackLine + "\n");
                            bw.flush();
                        }
                    }
                    continue;
                }

                fields = line_regex.split(line);
                // filtering out header lines (and any other line that doesn't start with a first field of all digits)
                String field0 = fields[0];
                boolean non_digits_present = non_digit.matcher(field0).find(0);
                if (non_digits_present) {
                    continue;
                }

                int findex = 0;

                findex = skipExtraBinField(findex, fields);

                findex += 9;
                query_seq_id = fields[findex];

                findex += 4;
                target_seq_id = fields[findex];

                findex += 1;
                length = Integer.valueOf(fields[findex]);

                if (in_bottom_of_link_psl) {
                    Set<String> seq_ids = queryTarget.get(target_seq_id);

                    if (seq_ids == null) {
                        Logger.getLogger(PSL.class.getName()).log(Level.INFO,
                                "Ignoring orphan target sequence {0} at line {1} "
                                + "in feature {2}",
                                new Object[]{target_seq_id, line_count, featureName});
                        continue;
                    }

                    for (String seq_id : seq_ids) {
                        bw = chrs.get(seq_id);
                        bw.write(line + "\n");
                    }
                    continue;
                }

                addToQueryTarget(queryTarget, query_seq_id, target_seq_id);

                // Ignoring chromosome seqs after last track line. It also ignores
                // orphan chromosome seqs.
                if (!chrs.containsKey(target_seq_id)) {
                    addToLists(chrs, target_seq_id, chrFiles, chrLength, is_link_psl ? ".link.psl" : ".psl");
                }

                bw = chrs.get(target_seq_id);

                if (!chrTrack.containsKey(target_seq_id) && trackLine != null) {
                    chrTrack.put(target_seq_id, true);
                    bw.write(trackLine + "\n");
                }

                bw.write(line + "\n");
                if (length > chrLength.get(target_seq_id)) {
                    chrLength.put(target_seq_id, length);
                }
            }

            return !thread.isInterrupted();
        } catch (Exception e) {
            throw new Exception("Error parsing PSL file\n" + "line count: " + line_count + "\n", e);
        } finally {
            for (BufferedWriter b : chrs.values()) {
                try {
                    b.flush();
                } catch (IOException ex) {
                    Logger.getLogger(PSL.class.getName()).log(Level.SEVERE, null, ex);
                }
                GeneralUtils.safeClose(b);
            }
            GeneralUtils.safeClose(br);
            GeneralUtils.safeClose(bw);
        }
    }

    private static void addToQueryTarget(Map<String, Set<String>> queryTarget, String query_seq_id, String target_seq_id) {

        if (!queryTarget.containsKey(query_seq_id)) {
            queryTarget.put(query_seq_id, new HashSet<>());
        }

        Set<String> set = queryTarget.get(query_seq_id);
        set.add(target_seq_id);
    }

    public void enableSharedQueryTarget(boolean b) {
        look_for_targets_in_query_group = b;
    }

    public void setCreateContainerAnnot(boolean b) {
        create_container_annot = b;
    }

    /**
     * Whether or not to add new seqs from the file to the target
     * GenomeVersion. Normally false; set this to true for "link.psl" files.
     */
    public void setIsLinkPsl(boolean b) {
        is_link_psl = b;
    }

    private List<UcscPslSym> parse(BioSeq seq, int min, int max) throws Exception {
        InputStream istr = null;
        try {
            File file = chrList.get(seq);
            if (file == null) {
                Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Could not find chromosome {0}", seq.getId());
                return Collections.<UcscPslSym>emptyList();
            }
            istr = new FileInputStream(file);
            return parse(istr, uri.toString(), min, max, query_group, target_group, other_group);
        } catch (Exception ex) {
            throw ex;
        } finally {
            GeneralUtils.safeClose(istr);
        }
    }

    private List<UcscPslSym> parse(InputStream istr, String annot_type, int min, int max,
            GenomeVersion query_group, GenomeVersion target_group, GenomeVersion other_group) {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(istr));
        Iterator<String> it = new Iterator<String>() {

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public String next() {
                String line = null;
                try {
                    line = reader.readLine();
                } catch (IOException x) {
                    Logger.getLogger(this.getClass().getName()).log(
                            Level.SEVERE, "error reading psl file", x);
                    line = null;
                }
                return line;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };

        return parse(it, annot_type, min, max, query_group, target_group, other_group);
    }

    @Override
    public List<? extends SeqSymmetry> processLines(BioSeq seq, final LineReader lineReader) {
        Iterator<String> it = new Iterator<String>() {

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public String next() {
                String line = null;
                try {
                    line = lineReader.readLine();
                } catch (IOException x) {
                    Logger.getLogger(this.getClass().getName()).log(
                            Level.SEVERE, "error reading psl file", x);
                    line = null;
                }
                return line;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };

        if (is_link_psl) {
            return tabixParse(it, uri.toString(), 0, Integer.MAX_VALUE, query_group, target_group, other_group);
        }

        return parse(it, uri.toString(), 0, Integer.MAX_VALUE, query_group, target_group, other_group);
    }

    private List<UcscPslSym> tabixParse(
            Iterator<String> it, String annot_type, int min, int max,
            GenomeVersion query_group, GenomeVersion target_group,
            GenomeVersion other_group) {

        if (DEBUG) {
            System.out.println("in PSL.parse(), create_container_annot: " + create_container_annot);
        }
        List<UcscPslSym> results = new ArrayList<>();

        // Make temporary seq groups for any unspecified genomeVersion.
        // These temporary groups do not require synonym matching, because they should
        // only refer to sequences from a single file.
        if (query_group == null) {
            query_group = new GenomeVersion("Query");
            query_group.setUseSynonyms(false);
        }
        if (target_group == null) {
            target_group = new GenomeVersion("Target");
            target_group.setUseSynonyms(false);
        }
        if (other_group == null) {
            other_group = new GenomeVersion("Other");
            other_group.setUseSynonyms(false);
        }

        boolean in_bottom_of_link_psl = false;

        // the three xxx2types Maps accommodate using create_container_annot and psl with track lines.
        Map<BioSeq, Map<String, SimpleSymWithProps>> target2types = new HashMap<>();
        Map<BioSeq, Map<String, SimpleSymWithProps>> query2types = new HashMap<>();
        Map<BioSeq, Map<String, SimpleSymWithProps>> other2types = new HashMap<>();

//		int line_count = 0;
        String line = null;
        @SuppressWarnings("unused")
        int total_annot_count = 0;
        @SuppressWarnings("unused")
        int total_child_count = 0;
        Thread thread = Thread.currentThread();
//		try {
        while ((line = it.next()) != null && (!thread.isInterrupted())) {
//				line_count++;
            // Ignore psl header lines
            if (line.trim().length() == 0) {
                continue;
            }
            char firstchar = line.charAt(0);

            if (firstchar == '#'
                    || (firstchar == 'm' && line.startsWith("match\t"))
                    || (firstchar == '-' && line.startsWith("-------"))) {
                continue;
            }

            if (firstchar == 't' && line.startsWith("track")) {
                // Always parse the track line, but
                // only set the AnnotStyle properties from it
                // if this is NOT a ".link.psl" file.
                track_line_parser.parseTrackLine(line, track_name_prefix);
                if (!is_link_psl) {
                    TrackLineParser.createTrackStyle(track_line_parser.getCurrentTrackHash(), annot_type, extension);
                }
                // You can later get the track properties with getCurrentTrackHash();
                continue;
            }
            String[] fields = line_regex.split(line);
            in_bottom_of_link_psl = false;
            String[] f21 = Arrays.copyOfRange(fields, 0, 21);

            // Main method to determine the symmetry
            UcscPslSym sym = createSym(annot_type, min, max, query_group,
                    target_group, other_group, in_bottom_of_link_psl, f21,
                    target2types, query2types, other2types);

            if (sym == null) {
                continue;
            }

            total_annot_count++;
            total_child_count += sym.getChildCount();
            results.add(sym);

            if (fields.length != 42) {
                continue;
            }

            in_bottom_of_link_psl = true;
            f21 = Arrays.copyOfRange(fields, 21, 42);

            // Main method to determine the symmetry
            createSym(annot_type, min, max, query_group,
                    target_group, other_group, in_bottom_of_link_psl, f21,
                    target2types, query2types, other2types);

            if (DEBUG) {
                if (total_annot_count % 5000 == 0) {
                    System.out.println("current annot count: " + total_annot_count);
                }
            }
        }
//		} catch (Exception e) {
//			StringBuilder sb = new StringBuilder();
//			sb.append("Error parsing PSL file\n");
//			sb.append("line count: ").append(line_count).append("\n");
//			sb.append("child count: ").append(childcount).append("\n");
//			if (block_size_array != null && block_size_array.length != 0) {
//				sb.append("block_size first element: **").append(block_size_array[0]).append("**\n");
//			}
//		}
        if (DEBUG) {
            System.out.println("finished parsing PSL file, annot count: " + total_annot_count
                    + ", child count: " + total_child_count);
        }

        return results;
    }

    /**
     * @param annot_type The method name for the annotation to load from the
     * file, if the track line is missing; if there is a track line in the file,
     * the name from the track line will be used instead.
     * @param query_group An GenomeVersion (or null) to look for query
     * SeqSymmetries in and add SeqSymmetries to. Null is ok; this will cause a
     * temporary GenomeVersion to be created.
     * @param target_group An GenomeVersion (or null) to look for target
     * SeqSymmetries in and add SeqSymmetries to.
     * @param other_group An GenomeVersion (or null) to look for other
     * SeqSymmetries in (in PSL3 format) and add SeqSymmetries to. This
     * parameter is ignored if the file is not in psl3 format.
     */
    private List<UcscPslSym> parse(
            Iterator<String> it, String annot_type, int min, int max,
            GenomeVersion query_group, GenomeVersion target_group,
            GenomeVersion other_group) {

        if (DEBUG) {
            System.out.println("in PSL.parse(), create_container_annot: " + create_container_annot);
        }
        List<UcscPslSym> results = new ArrayList<>();

        // Make temporary seq groups for any unspecified genomeVersion.
        // These temporary groups do not require synonym matching, because they should
        // only refer to sequences from a single file.
        if (query_group == null) {
            query_group = new GenomeVersion("Query");
            query_group.setUseSynonyms(false);
        }
        if (target_group == null) {
            target_group = new GenomeVersion("Target");
            target_group.setUseSynonyms(false);
        }
        if (other_group == null) {
            other_group = new GenomeVersion("Other");
            other_group.setUseSynonyms(false);
        }

        boolean in_bottom_of_link_psl = false;

        // the three xxx2types Maps accommodate using create_container_annot and psl with track lines.
        Map<BioSeq, Map<String, SimpleSymWithProps>> target2types = new HashMap<>();
        Map<BioSeq, Map<String, SimpleSymWithProps>> query2types = new HashMap<>();
        Map<BioSeq, Map<String, SimpleSymWithProps>> other2types = new HashMap<>();

//		int line_count = 0;
        String line = null;
        @SuppressWarnings("unused")
        int total_annot_count = 0;
        @SuppressWarnings("unused")
        int total_child_count = 0;
        Thread thread = Thread.currentThread();
//		try {
        while ((line = it.next()) != null && (!thread.isInterrupted())) {
//				line_count++;
            // Ignore psl header lines
            if (line.trim().length() == 0) {
                continue;
            }
            char firstchar = line.charAt(0);

            if (firstchar == '#'
                    || (firstchar == 'm' && line.startsWith("match\t"))
                    || (firstchar == '-' && line.startsWith("-------"))) {
                continue;
            }

            if (firstchar == 't' && line.startsWith("track")) {
                // Always parse the track line, but
                // only set the AnnotStyle properties from it
                // if this is NOT a ".link.psl" file.
                if (is_link_psl) {
                    Map<String, String> track_props = track_line_parser.parseTrackLine(line, track_name_prefix);
                    String track_name = track_props.get(TrackLineParser.NAME);
                    if (track_name != null && track_name.endsWith("probesets")) {
                        in_bottom_of_link_psl = true;
                    }
                } else {
                    track_line_parser.parseTrackLine(line, track_name_prefix);
                    TrackLineParser.createTrackStyle(track_line_parser.getCurrentTrackHash(), annot_type, extension);
                }
                // You can later get the track properties with getCurrentTrackHash();
                continue;
            }
            String[] fields = line_regex.split(line);

            // Main method to determine the symmetry
            UcscPslSym sym = createSym(annot_type, min, max, query_group,
                    target_group, other_group, in_bottom_of_link_psl, fields,
                    target2types, query2types, other2types);

            if (sym == null) {
                continue;
            }

            total_annot_count++;
            total_child_count += sym.getChildCount();
            results.add(sym);
            if (DEBUG) {
                if (total_annot_count % 5000 == 0) {
                    System.out.println("current annot count: " + total_annot_count);
                }
            }
        }
//		} catch (Exception e) {
//			StringBuilder sb = new StringBuilder();
//			sb.append("Error parsing PSL file\n");
//			sb.append("line count: ").append(line_count).append("\n");
//			sb.append("child count: ").append(childcount).append("\n");
//			if (block_size_array != null && block_size_array.length != 0) {
//				sb.append("block_size first element: **").append(block_size_array[0]).append("**\n");
//			}
//		}
        if (DEBUG) {
            System.out.println("finished parsing PSL file, annot count: " + total_annot_count
                    + ", child count: " + total_child_count);
        }

        return results;
    }

    private UcscPslSym createSym(String annot_type, int min, int max,
            GenomeVersion query_group, GenomeVersion target_group,
            GenomeVersion other_group, boolean in_bottom_of_link_psl, String[] fields,
            Map<BioSeq, Map<String, SimpleSymWithProps>> target2types,
            Map<BioSeq, Map<String, SimpleSymWithProps>> query2types,
            Map<BioSeq, Map<String, SimpleSymWithProps>> other2types) {

        // filtering out header lines (and any other line that doesn't start with a first field of all digits)
        String field0 = fields[0];
        boolean non_digits_present = non_digit.matcher(field0).find(0);
        if (non_digits_present) {
            return null;
        }

        int findex = 0;

        findex = skipExtraBinField(findex, fields);

        int match = Integer.parseInt(fields[findex++]);
        int mismatch = Integer.parseInt(fields[findex++]);
        int repmatch = Integer.parseInt(fields[findex++]);
        int n_count = Integer.parseInt(fields[findex++]);
        int q_gap_count = Integer.parseInt(fields[findex++]);
        int q_gap_bases = Integer.parseInt(fields[findex++]);
        int t_gap_count = Integer.parseInt(fields[findex++]);
        int t_gap_bases = Integer.parseInt(fields[findex++]);
        String strandstring = fields[findex++];
        boolean same_orientation = true;
        boolean qforward = true;
        boolean tforward = true;
        if (strandstring.length() == 1) {
            same_orientation = strandstring.equals("+");
            qforward = (strandstring.charAt(0) == '+');
            tforward = true;
        } else if (strandstring.length() == 2) {
            // need to deal with cases (as mentioned in PSL docs) where
            //    strand field is "++", "+-", "-+", "--"
            //  (where first char indicates strand of query, and second is strand for ? [target??]
            //  for now, just call strand based on them being different,
            //   so "++" OR "--" ==> forward
            //      "+-" OR "-+" ==> reverse
            // current implentation assumes "++", "--", "+-", "-+" are the only possibilities
            same_orientation = (strandstring.equals("++") || strandstring.equals("--"));
            qforward = (strandstring.charAt(0) == '+');
            tforward = (strandstring.charAt(1) == '+');
        } else {
            System.err.println("strand field longer than two characters! ==> " + strandstring);
        }

        String qname = fields[findex++];
        int qsize = Integer.parseInt(fields[findex++]);
        int qmin = Integer.parseInt(fields[findex++]);
        int qmax = Integer.parseInt(fields[findex++]);
        String tname = fields[findex++];
        int tsize = Integer.parseInt(fields[findex++]);
        int tmin = Integer.parseInt(fields[findex++]);
        int tmax = Integer.parseInt(fields[findex++]);
        int maximum = Math.max(tmin, tmax);
        int minimum = Math.min(tmin, tmax);

        if ((maximum < min || minimum > max) && !is_link_psl) {
            return null;
        }

        int blockcount = Integer.parseInt(fields[findex++]);

        String[] block_size_array = comma_regex.split(fields[findex++]);
        String[] q_start_array = comma_regex.split(fields[findex++]);
        String[] t_start_array = comma_regex.split(fields[findex++]);
        int childcount = block_size_array.length;

        // skipping entries that have problems with block_size_array
        if ((block_size_array.length == 0)
                || (block_size_array[0] == null)
                || (block_size_array[0].length() == 0)) {
            System.err.println("PSL found problem with blockSizes list, skipping this line: ");
            System.err.println(fields);
            return null;
        }
        if (blockcount != block_size_array.length) {
            System.err.println("PSL found disagreement over number of blocks, skipping this line: ");
            System.err.println(fields);
            return null;
        }

        // Main method to determine the symmetry
        return determineSym(
                query_group, qname, qsize, target_group, tname, in_bottom_of_link_psl,
                tsize, qforward, tforward, block_size_array, q_start_array, t_start_array,
                annot_type, fields, findex, childcount, other_group, match, mismatch,
                repmatch, n_count, q_gap_count, q_gap_bases, t_gap_count, t_gap_bases,
                same_orientation, qmin, qmax, tmin, tmax, blockcount, annotate_other,
                other2types, annotate_query, query2types, annotate_target, target2types);

    }

    private static int skipExtraBinField(int findex, String[] fields) {
        /*
         *  includes_bin_field is so PSL can serve double duty:
         *  1. for standard PSL files (includes_bin_field = false)
         *  2. for UCSC PSL-like dump from database, where format has extra ushort field at beginning
         *       that is used to speed up indexing in db (includes_bin_field = true)
         */
        //        if (line_count < 3) { System.out.println("# of fields: " + fields.length); }
        // trying to determine if there's an extra bin field at beginning of PSL line...
        //   for normal PSL, orientation field is

        if (fields.length > 9) {
            char firstchar = fields[9].charAt(0);
            if (firstchar == '+' || firstchar == '-') {
                findex++;
            } // skip bin field at beginning if present
        }

        return findex;
    }

    private BioSeq determineSeq(GenomeVersion query_group, String qname, int qsize) {
        BioSeq qseq = query_group.getSeq(qname);
        if (qseq == null) {
            // Doing a new String() here gives a > 4X reduction in
            //    memory requirements!  Possible reason: Regex machinery when it splits a String into
            //    an array of Strings may use same underlying character array, so essentially
            //    end up holding a pointer to a character array containing the whole input file ???
            //
            qseq = query_group.addSeq(qname, qsize, uri.toString());
        }
        if (qseq.getLength() < qsize) {
            qseq.setLength(qsize);
        }
        return qseq;
    }

    private UcscPslSym determineSym(
            GenomeVersion query_group, String qname, int qsize,
            GenomeVersion target_group, String tname, boolean in_bottom_of_link_psl,
            int tsize, boolean qforward, boolean tforward, String[] block_size_array,
            String[] q_start_array, String[] t_start_array, String annot_type,
            String[] fields, int findex, int childcount, GenomeVersion other_group,
            int match, int mismatch, int repmatch, int n_count, int q_gap_count,
            int q_gap_bases, int t_gap_count, int t_gap_bases, boolean same_orientation,
            int qmin, int qmax, int tmin, int tmax, int blockcount, boolean annotate_other,
            Map<BioSeq, Map<String, SimpleSymWithProps>> other2types, boolean annotate_query,
            Map<BioSeq, Map<String, SimpleSymWithProps>> query2types, boolean annotate_target,
            Map<BioSeq, Map<String, SimpleSymWithProps>> target2types)
            throws NumberFormatException {
        BioSeq qseq = determineSeq(query_group, qname, qsize);
        BioSeq tseq = target_group.getSeq(tname);
        boolean shared_query_target = false;
        if (tseq == null) {
            if (look_for_targets_in_query_group && (query_group.getSeq(tname) != null)) {
                tseq = query_group.getSeq(tname);
                shared_query_target = true;
            } else {
                if (look_for_targets_in_query_group && is_link_psl) {
                    // If we are in the bottom section of a ".link.psl" file,
                    // then add sequences only to the query sequence, never the target sequence.
                    if (in_bottom_of_link_psl) {
                        tseq = query_group.addSeq(tname, qsize);
                    } else {
                        tseq = target_group.addSeq(tname, qsize);
                    }
                } else {
                    tseq = target_group.addSeq(tname, qsize);
                }
            }
        }
        if (tseq.getLength() < tsize) {
            tseq.setLength(tsize);
        }
        List<Object> child_arrays = calcChildren(qseq, tseq, qforward, tforward, block_size_array, q_start_array, t_start_array);
        int[] blocksizes = (int[]) child_arrays.get(0);
        int[] qmins = (int[]) child_arrays.get(1);
        int[] tmins = (int[]) child_arrays.get(2);
        String type = track_line_parser.getCurrentTrackHash().get(TrackLineParser.NAME);
        if (type == null) {
            type = annot_type;
        }
        UcscPslSym sym = null;
        // a "+" or "-" in first field after tmins indicates that it's a Psl3 format
        boolean is_psl3 = fields.length > findex && (fields[findex].equals("+") || fields[findex].equals("-"));
        // trying to handle parsing of extended PSL format for three sequence alignment
        //     (putting into a Psl3Sym)
        // extra fields (immediately after tmins), based on Psl3Sym.outputPsl3Format:
        // same_other_orientation  otherseq_id  otherseq_length  other_min other_max omins
        //    (but omins doesn't have weirdness that qmins/tmins does when orientation = "-")
        if (is_psl3) {
            String otherstrand_string = fields[findex++];
            boolean other_same_orientation = otherstrand_string.equals("+");
            String oname = fields[findex++];
            int osize = Integer.parseInt(fields[findex++]);
            int omin = Integer.parseInt(fields[findex++]);
            int omax = Integer.parseInt(fields[findex++]);
            String[] o_min_array = comma_regex.split(fields[findex++]);
            int[] omins = new int[childcount];
            for (int i = 0; i < childcount; i++) {
                omins[i] = Integer.parseInt(o_min_array[i]);
            }
            BioSeq oseq = determineSeq(other_group, oname, osize);

            sym = new Psl3Sym(type, match, mismatch, repmatch, n_count, q_gap_count, q_gap_bases, t_gap_count, t_gap_bases, same_orientation, other_same_orientation, qseq, qmin, qmax, tseq, tmin, tmax, oseq, omin, omax, blockcount, blocksizes, qmins, tmins, omins, in_bottom_of_link_psl);
            annotate(annotate_other, create_container_annot, is_link_psl, other2types, oseq, type, sym, is_psl3, other_group);
        } else {
            String[] target_res_arr = null;
            if (fields.length >= findex + 2) {
                // see if there are two extra fields with residues for each block
                target_res_arr = targetResidues(fields[findex++], tforward);
            }
            sym = new UcscPslSym(type, match, mismatch, repmatch, n_count, q_gap_count, q_gap_bases,
                    t_gap_count, t_gap_bases, same_orientation,
                    qseq, qmin, qmax,
                    tseq, tmin, tmax, target_res_arr,
                    blockcount, blocksizes, qmins, tmins, in_bottom_of_link_psl);
        }

        findExtraTagValues(fields, findex, sym);

        annotate(annotate_query, create_container_annot, is_link_psl, query2types, qseq, type, sym, is_psl3, query_group);
        annotateTarget(annotate_target || (shared_query_target && is_link_psl), create_container_annot, is_link_psl, target2types, tseq, type, sym, is_psl3, in_bottom_of_link_psl, target_group);

        return sym;
    }

    // looking for extra tag-value fields at end of line
    private static void findExtraTagValues(String[] fields, int findex, UcscPslSym sym) {
        if (fields.length > findex) {
            for (int i = findex; i < fields.length; i++) {
                String field = fields[i];
                String[] tagval = tagval_regex.split(field);
                if (tagval.length >= 2) {
                    String tag = tagval[0];
                    String val = tagval[1];
                    sym.setProperty(tag, val);
                }
            }
        }
    }

    private static void annotate(
            boolean annotate, boolean create_container_annot, boolean is_link_psl, Map<BioSeq, Map<String, SimpleSymWithProps>> str2types, BioSeq seq, String type, UcscPslSym sym, boolean is_psl3, GenomeVersion annGroup) {
        if (annotate) {
            if (create_container_annot) {
                createContainerAnnot(str2types, seq, type, sym, is_psl3, is_link_psl);
            } else {
                seq.addAnnotation(sym);
            }
//			annGroup.addToIndex(sym.getName(), sym);
        }
    }

    private static void annotateTarget(
            boolean annotate, boolean create_container_annot, boolean is_link_psl, Map<BioSeq, Map<String, SimpleSymWithProps>> str2types, BioSeq seq, String type, UcscPslSym sym, boolean is_psl3, boolean in_bottom_of_link_psl, GenomeVersion annGroup) {
        if (annotate) {
            // force annotation of target if query and target are shared and file is ".link.psl" format
            if (create_container_annot) {
                createContainerAnnot(str2types, seq, type, sym, is_psl3, is_link_psl);
            } else {
                seq.addAnnotation(sym);
            }
            if (!in_bottom_of_link_psl) {
//				annGroup.addToIndex(sym.getName(), sym);
            }
        }
    }

    private static void createContainerAnnot(
            Map<BioSeq, Map<String, SimpleSymWithProps>> seq2types, BioSeq seq, String type, SeqSymmetry sym, boolean is_psl3, boolean is_link) {
        //    If using a container sym, need to first hash (seq2types) from
        //    seq to another hash (type2csym) of types to container sym
        //    System.out.println("in createContainerAnnot, type: " + type);
        Map<String, SimpleSymWithProps> type2csym = seq2types.get(seq);
        if (type2csym == null) {
            type2csym = new HashMap<>();
            seq2types.put(seq, type2csym);
        }
        SimpleSymWithProps parent_sym = type2csym.get(type);
        if (parent_sym == null) {
            parent_sym = new SimpleSymWithProps();
            parent_sym.addSpan(new SimpleSeqSpan(0, seq.getLength(), seq));
            parent_sym.setProperty("method", type);
            if (is_link) {
                parent_sym.setProperty("preferred_formats", link_psl_pref_list);
            } else if (is_psl3) {
                parent_sym.setProperty("preferred_formats", psl3_pref_list);
            } else {
                parent_sym.setProperty("preferred_formats", psl_pref_list);
            }
            parent_sym.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
            seq.addAnnotation(parent_sym);
            type2csym.put(type, parent_sym);
        }
        parent_sym.addChild(sym);
    }

    private static List<Object> calcChildren(BioSeq qseq, BioSeq tseq, boolean qforward, boolean tforward,
            String[] blocksize_strings,
            String[] qstart_strings, String[] tstart_strings) {
        int childCount = blocksize_strings.length;
        if (qstart_strings.length != childCount || tstart_strings.length != childCount) {
            System.out.println("array counts for block sizes, q starts, and t starts don't agree, "
                    + "skipping children");
            return null;
        }
        int[] blocksizes = new int[childCount];
        int[] qmins = new int[childCount];
        int[] tmins = new int[childCount];

        if (childCount > 0) {
            int qseq_length = qseq.getLength();
            int tseq_length = tseq.getLength();

            if (qforward && tforward) { // query = forward, target = forward
                for (int i = 0; i < childCount; i++) {
                    int match_length = Integer.parseInt(blocksize_strings[i]);
                    int qstart = Integer.parseInt(qstart_strings[i]);
                    int tstart = Integer.parseInt(tstart_strings[i]);
                    blocksizes[i] = match_length;
                    qmins[i] = qstart;
                    tmins[i] = tstart;
                }
            } else if ((!qforward) && (tforward)) { // query = reverse, target = forward
                for (int i = 0; i < childCount; i++) {
                    int string_index = childCount - i - 1;
                    int match_length = Integer.parseInt(blocksize_strings[string_index]);
                    int qstart = qseq_length - Integer.parseInt(qstart_strings[string_index]);
                    int tstart = Integer.parseInt(tstart_strings[string_index]);
                    int qend = qstart - match_length;
                    blocksizes[i] = match_length;
                    qmins[i] = qend;
                    tmins[i] = tstart;
                }
            } else if ((qforward) && (!tforward)) {  // query = forward, target = reverse
                for (int i = 0; i < childCount; i++) {
                    int match_length = Integer.parseInt(blocksize_strings[i]);
                    int qstart = Integer.parseInt(qstart_strings[i]);
                    int tstart = tseq_length - Integer.parseInt(tstart_strings[i]);
                    int tend = tstart - match_length;
                    blocksizes[i] = match_length;
                    qmins[i] = qstart;
                    tmins[i] = tend;
                }
            } else { // query = reverse, target = reverse
                for (int i = 0; i < childCount; i++) {
                    int string_index = childCount - i - 1;
                    int match_length = Integer.parseInt(blocksize_strings[string_index]);
                    int qstart = qseq_length - Integer.parseInt(qstart_strings[string_index]);
                    int tstart = tseq_length - Integer.parseInt(tstart_strings[string_index]);
                    int qend = qstart - match_length;
                    int tend = tstart - match_length;
                    blocksizes[i] = match_length;
                    qmins[i] = qend;
                    tmins[i] = tend;
                }
            }
        }
        List<Object> results = new ArrayList<>(3);
        results.add(blocksizes);
        results.add(qmins);
        results.add(tmins);
        return results;
    }

    private static String[] targetResidues(String residues, boolean tforward) {
        if (tforward) {
            return comma_regex.split(residues);
        }

        String[] residues_array = comma_regex.split(residues);

        if (residues_array == null) {
            return null;
        }

        //Reverse the array for negative strand;
        String temp;
        for (int i = 0; i < residues_array.length / 2; i++) {
            temp = residues_array[i];
            residues_array[i] = residues_array[residues_array.length - i - 1];
            residues_array[residues_array.length - i - 1] = temp;
        }

        return residues_array;
    }

    /**
     * Implementing AnnotationWriter interface to write out annotations to an
     * output stream as "PSL" format
     *
     */
    public boolean writeAnnotations(Collection<? extends SeqSymmetry> syms, BioSeq seq,
            String type, OutputStream outstream) {
        return writeAnnotations(syms, seq, false, type, null, outstream);
    }

    /**
     * This version of the method is able to write out track lines
     *
     */
    public boolean writeAnnotations(Collection<? extends SeqSymmetry> syms, BioSeq seq,
            boolean writeTrackLines, String type,
            String description, OutputStream outstream) {

        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new BufferedOutputStream(outstream));

            if (writeTrackLines) {
                dos.write(trackLine(type, description).getBytes());
            }

            for (SeqSymmetry sym : syms) {

                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                if (!(sym instanceof UcscPslSym)) {
                    int spancount = sym.getSpanCount();
                    if (spancount == 1) {
                        sym = SeqSymmetryConverter.convertToPslSym(sym, type, seq);
                    } else {
                        BioSeq seq2 = SeqUtils.getOtherSeq(sym, seq);
                        sym = SeqSymmetryConverter.convertToPslSym(sym, type, seq2, seq);
                    }
                }
                this.writeSymmetry(sym, seq, dos);
            }
            dos.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    public static String trackLine(String type, String description) {
        String trackLine = "track";
        if (type != null) {
            trackLine += " name=\"" + type + "\"";
        }
        if (description != null) {
            trackLine += " description=\"" + description + "\"";
        }
        trackLine += newLine;
        return trackLine;
    }

    public Comparator<UcscPslSym> getComparator(BioSeq seq) {
        return comp;
    }

    public void writeSymmetry(SeqSymmetry sym, BioSeq seq, OutputStream os) throws IOException {
        DataOutputStream dos = null;
        if (os instanceof DataOutputStream) {
            dos = (DataOutputStream) os;
        } else {
            dos = new DataOutputStream(os);
        }
        ((UcscPslSym) sym).outputPslFormat(dos);
    }

    public int getMin(SeqSymmetry sym, BioSeq seq) {
        return ((UcscPslSym) sym).getTargetMin();
    }

    public int getMax(SeqSymmetry sym, BioSeq seq) {
        return ((UcscPslSym) sym).getTargetMax();
    }

    public List<UcscPslSym> parse(DataInputStream dis, String annot_type, GenomeVersion genomeVersion) {
        return parse(dis, annot_type, Integer.MIN_VALUE, Integer.MAX_VALUE, null, genomeVersion, null);
    }

    public void setTrackNamePrefix(String prefix) {
        track_name_prefix = prefix;
    }

    public String getTrackNamePrefix() {
        return track_name_prefix;
    }

    /**
     * Implementing AnnotationWriter interface to write out annotations to an
     * output stream as "PSL" format
     *
     */
    public String getMimeType() {
        return "text/plain";
    }

    @Override
    public List<String> getFormatPrefList() {
        if (is_link_psl) {
            return PSL.link_psl_pref_list;
        }
        return PSL.psl_pref_list;
    }

    @Override
    public SeqSpan getSpan(String line) {
        return null; // not used yet
    }

    public boolean processInfoLine(String line, List<String> infoLines) {
        return false; // not used yet
    }

    @Override
    public boolean isMultiThreadOK() {
        return true;
    }
}
