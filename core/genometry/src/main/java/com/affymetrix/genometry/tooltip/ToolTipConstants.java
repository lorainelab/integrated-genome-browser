/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometry.tooltip;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tarun
 */
public class ToolTipConstants {

    public static final String TITLE = "title";
    public static final String NAME = "name";
    public static final String ID = "id";
    public static final String CHROMOSOME = "chromosome";
    public static final String START = "start";
    public static final String END = "end";
    public static final String LENGTH = "length";
    public static final String AVERAGE_QUALITY = "average quality";

    public static final String RESIDUES = "residues";
    public static final String STRAND = "strand";
    public static final String SHOW_MASK = "showMask";
    public static final String SCORE = "score";
    public static final String SCORES = "scores";
    public static final String FORWARD = "forward";
    public static final String MAPQ = "mapQ";
    public static final String FLAGS = "flags";

    public static final String CL = "CL";
    public static final String CIGAR = "CIGAR";
    public static final String VN = "VN";
    public static final String NH = "NH";
    public static final String XS = "XS";
    public static final String NM = "NM";
    public static final String AS = "AS";
    public static final String XM = "XM";
    public static final String XN = "XN";
    public static final String MD = "MD";
    public static final String XO = "XO";
    public static final String YT = "YT";
    public static final String PN = "PN";
    public static final String XG = "XG";

    public static final String DESCRIPTION = "description";
    public static final String CDS_START = "cds start";

    public static final String BLOCK_NUMBER = "block number";
    public static final String CDS_END = "cds end";

    public static final String BAM_FLAG = "raw sam record flag";
    public static final String PAIRED_READ = "paired read";
    public static final String MATE_START = "mate start";
    public static final String PROPER_PAIR_READ = "mapped in proper pair";
    public static final String UNMAPPED_READ = "unmapped";
    public static final String READ_REVERSE_STRAND = "strand";
    public static final String MATE_REVERSE_STRAND = "mate on reverse strand";
    public static final String FIRST_IN_PAIR = "first in pair";
    public static final String SECOND_IN_PAIR = "second in pair";
    public static final String DUPLICATE = "duplicate";
    public static final String SUPPLEMENTARY = "supplementary";
    public static final String FAILED_QC = "failed qc";

    public static final String NA = "na";

    // Constants added while SeqMapView refactoring
    public static final String SEQ_ID = "seq id";
    public static final String METHOD = "method";
    public static final String SOURCE = "source";
    public static String SOURCE_TYPE = "source type";
    public static final String TYPE = "type";
    public static final String MATCH = "match";
    public static final String FEATURE_TYPE = "feature type";
    public static final String FRAME = "frame";
    public static final String DIRECTION = "direction";
    public static final String REVERSE_DIRECTION = "reverse";

    public static final String FILE_NAME = "file name";
    public static final String FILE_TITLE = "file title";
    public static final String FILE_URL = "file URL";
    public static final String URL = "url";
    public static final String SERVER = "server";
    public static final String MIN_SCORE = "min score";
    public static final String MAX_SCORE = "max score";

    //NarrowPeak
    public static final String SIGNAL_VALUE = "signalValue";
    public static final String P_VALUE = "pValue";
    public static final String Q_VALUE = "qValue";
    public static final String PEAK = "peak";

    // BAM
    public static final List<String> BAM_INFO_GRP = Arrays.asList(NAME, ID, CHROMOSOME, START, END, LENGTH, AVERAGE_QUALITY);
    public static final List<String> BAM_LOC_GRP = Arrays.asList(RESIDUES, STRAND, SHOW_MASK, SCORES, FORWARD, MAPQ, FLAGS);
    public static final List<String> BAM_DETAILS_GRP = Arrays.asList(PAIRED_READ, MATE_START, PROPER_PAIR_READ, UNMAPPED_READ, READ_REVERSE_STRAND, MATE_REVERSE_STRAND, FIRST_IN_PAIR,
            SECOND_IN_PAIR, DUPLICATE, SUPPLEMENTARY, FAILED_QC, CIGAR, VN, NH, XS, NM);
    public static final List<String> BAM_IGNORE_LIST = Arrays.asList(CL, BAM_FLAG);
    public static final List<String> BAM_PROP_LIST = new ArrayList<>();
    private static final String BASIC__INFO_CATEGORY = "Basic Info";

    public static final Map<String, List<String>> BAM_INFO_CATEGORY = ImmutableMap.of(BASIC__INFO_CATEGORY, BAM_INFO_GRP);
    public static final Map<String, List<String>> BAM_LOCATION_CATEGORY = ImmutableMap.of("Bam Info", BAM_LOC_GRP);
    public static final Map<String, List<String>> BAM_DETAILS_CATEGORY = ImmutableMap.of("Details", BAM_DETAILS_GRP);
    public static final String MISC_CATEGORY = "Misc";

    // BAM Insertion
    public static final List<String> BAM_INS_INFO_GRP = Arrays.asList(FEATURE_TYPE, LENGTH, RESIDUES, STRAND, START, END, ID);
    public static final List<String> BAM_INS_IGNORE_LIST = Arrays.asList(NAME, SHOW_MASK, SCORES, FORWARD, MAPQ, PAIRED_READ, DUPLICATE, SUPPLEMENTARY, FAILED_QC, CIGAR, VN, XS, NM, AS, XM, XN, MD, XO, YT, PN, XG, CHROMOSOME, AVERAGE_QUALITY);
    public static final List<String> BAM_INS_PROP_LIST = new ArrayList<>();
    public static final Map<String, List<String>> BAM_INS_INFO_CATEGORY = ImmutableMap.of(BASIC__INFO_CATEGORY, BAM_INS_INFO_GRP);

    // BED-14
    public static final List<String> BED14_INFO_GRP = Arrays.asList(TITLE, ID, DESCRIPTION);
    public static final List<String> BED14_LOC_GRP = Arrays.asList(START, END, LENGTH, STRAND, CDS_START, CDS_END, CHROMOSOME);
    public static final List<String> BED14_CIGAR_GRP = Arrays.asList(SCORES, RESIDUES, SHOW_MASK);
    public static final List<String> BED14_IGNORE_LIST = Arrays.asList(CL, NAME);
    public static final List<String> BED14_PROP_LIST = new ArrayList<>();

    public static final Map<String, List<String>> BED14_INFO_CATEGORY = ImmutableMap.of(BASIC__INFO_CATEGORY, BED14_INFO_GRP);
    public static final Map<String, List<String>> BED14_LOCATION_CATEGORY = ImmutableMap.of("BED14 Info", BED14_LOC_GRP);
    public static final Map<String, List<String>> BED14_CIGAR_CATEGORY = ImmutableMap.of("Cigar", BED14_CIGAR_GRP);

    //NarrowPeak+BroadPeak
    public static final List<String> NARROW_PEAK_INFO_GRP = Arrays.asList(TITLE, ID, SCORE, SIGNAL_VALUE, P_VALUE, Q_VALUE, PEAK);
    public static final List<String> NARROW_PEAK_LOC_GRP = Arrays.asList(START, END, LENGTH, STRAND, CHROMOSOME);
    public static final List<String> NARROW_PEAK_CIGAR_GRP = Arrays.asList(SCORES, RESIDUES, SHOW_MASK);
    public static final List<String> NARROW_PEAK_IGNORE_LIST = Arrays.asList(CL, NAME, CDS_START, CDS_END);
    public static final List<String> NARROW_PEAK_PROP_LIST = new ArrayList<>();

    public static final Map<String, List<String>> NARROW_PEAK_INFO_CATEGORY = ImmutableMap.of(BASIC__INFO_CATEGORY, NARROW_PEAK_INFO_GRP);
    public static final Map<String, List<String>> NARROW_PEAK_LOCATION_CATEGORY = ImmutableMap.of("NARROW_PEAK Info", NARROW_PEAK_LOC_GRP);
    public static final Map<String, List<String>> NARROW_PEAK_CIGAR_CATEGORY = ImmutableMap.of("Cigar", NARROW_PEAK_CIGAR_GRP);

    //PSL
    public static final List<String> PSL_INFO_GRP = Arrays.asList(ID, DESCRIPTION);
    public static final List<String> PSL_LOC_GRP = Arrays.asList(START, END, LENGTH, STRAND, CHROMOSOME);
    public static final List<String> PSL_IGNORE_LIST = Arrays.asList(CL, NAME);
    public static final List<String> PSL_PROP_LIST = new ArrayList<>();

    public static final Map<String, List<String>> PSL_INFO_CATEGORY = ImmutableMap.of(BASIC__INFO_CATEGORY, PSL_INFO_GRP);
    public static final Map<String, List<String>> PSL_LOCATION_CATEGORY = ImmutableMap.of("PSL Info", PSL_LOC_GRP);

    // DEFAULT
    public static final List<String> DEFAULT_INFO_GRP = Arrays.asList(NAME, ID, CHROMOSOME, START, END, LENGTH, AVERAGE_QUALITY);
    public static final List<String> DEFAULT_LOC_GRP = Arrays.asList(RESIDUES, STRAND, SHOW_MASK, SCORES, FORWARD, MAPQ, FLAGS);
    public static final List<String> DEFAULT_CIGAR_GRP = Arrays.asList(PAIRED_READ, CIGAR, VN, NH, XS, NM);
    public static final List<String> DEFAULT_IGNORE_LIST = Arrays.asList(CL);
    public static final List<String> DEFAULT_PROP_LIST = new ArrayList<>();

    public static final Map<String, List<String>> DEFAULT_INFO_CATEGORY = ImmutableMap.of(BASIC__INFO_CATEGORY, DEFAULT_INFO_GRP);
    public static final Map<String, List<String>> DEFAULT_LOCATION_CATEGORY = ImmutableMap.of("Bam Info", DEFAULT_LOC_GRP);
    public static final Map<String, List<String>> DEFAULT_CIGAR_CATEGORY = ImmutableMap.of("Cigar", DEFAULT_CIGAR_GRP);

    //GFF
    public static final List<String> GFF_INFO_GRP = Arrays.asList(TITLE, ID, DESCRIPTION);
    public static final List<String> GFF_LOC_GRP = Arrays.asList(START, END, LENGTH, STRAND, CDS_START, CDS_END, CHROMOSOME);
    public static final List<String> GFF_CIGAR_GRP = Arrays.asList(SCORES, RESIDUES, SHOW_MASK);
    public static final List<String> GFF_IGNORE_LIST = Arrays.asList(CL);
    public static final List<String> GFF_PROP_LIST = new ArrayList<>();

    public static final Map<String, List<String>> GFF_INFO_CATEGORY = ImmutableMap.of(BASIC__INFO_CATEGORY, GFF_INFO_GRP);
    public static final Map<String, List<String>> GFF_LOCATION_CATEGORY = ImmutableMap.of("GFF Info", GFF_LOC_GRP);
    public static final Map<String, List<String>> GFF_CIGAR_CATEGORY = ImmutableMap.of("Cigar", GFF_CIGAR_GRP);

    //SamTools Tags
    public static final String CR = "CR";
    public static final String CB = "CB";
    public static final String MI = "MI";
    public static final String UB = "UB";

    static {
        BAM_PROP_LIST.addAll(BAM_INFO_GRP);
        BAM_PROP_LIST.addAll(BAM_LOC_GRP);
        BAM_PROP_LIST.addAll(BAM_DETAILS_GRP);
        
        BAM_INS_PROP_LIST.addAll(BAM_INS_INFO_GRP);

        BED14_PROP_LIST.addAll(BED14_INFO_GRP);
        BED14_PROP_LIST.addAll(BED14_LOC_GRP);
        BED14_PROP_LIST.addAll(BED14_CIGAR_GRP);

        NARROW_PEAK_PROP_LIST.addAll(NARROW_PEAK_INFO_GRP);
        NARROW_PEAK_PROP_LIST.addAll(NARROW_PEAK_LOC_GRP);
        NARROW_PEAK_PROP_LIST.addAll(NARROW_PEAK_CIGAR_GRP);

        PSL_PROP_LIST.addAll(PSL_INFO_GRP);
        PSL_PROP_LIST.addAll(PSL_LOC_GRP);

        DEFAULT_PROP_LIST.addAll(DEFAULT_INFO_GRP);
        DEFAULT_PROP_LIST.addAll(DEFAULT_LOC_GRP);
        DEFAULT_PROP_LIST.addAll(DEFAULT_CIGAR_GRP);

        GFF_PROP_LIST.addAll(GFF_INFO_GRP);
        GFF_PROP_LIST.addAll(GFF_LOC_GRP);
        GFF_PROP_LIST.addAll(GFF_CIGAR_GRP);
    }

    private ToolTipConstants() {

    }

}
