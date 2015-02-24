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

    public static final String GENE_NAME = "gene name";
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

    public static final String DESCRIPTION = "description";
    public static final String CDS_START = "cds start";
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
    public static final String TYPE = "type";
    public static final String MATCH = "match";
    public static final String FEATURE_TYPE = "feature type";
    public static final String DIRECTION = "direction";
    public static final String REVERSE_DIRECTION = "reverse";

    // BAM
    public static final List<String> BAM_INFO_GRP = Arrays.asList(GENE_NAME, NAME, ID, CHROMOSOME, START, END, LENGTH, AVERAGE_QUALITY);
    public static final List<String> BAM_LOC_GRP = Arrays.asList(RESIDUES, STRAND, SHOW_MASK, SCORES, FORWARD, MAPQ, FLAGS);
    public static final List<String> BAM_DETAILS_GRP = Arrays.asList(PAIRED_READ, MATE_START, PROPER_PAIR_READ, UNMAPPED_READ, READ_REVERSE_STRAND, MATE_REVERSE_STRAND, FIRST_IN_PAIR,
            SECOND_IN_PAIR, DUPLICATE, SUPPLEMENTARY, FAILED_QC, CIGAR, VN, NH, XS, NM);
    public static final List<String> BAM_IGNORE_LIST = Arrays.asList(CL, BAM_FLAG);
    public static final List<String> BAM_PROP_LIST = new ArrayList<>();

    public static final Map<String, List<String>> BAM_INFO_CATEGORY = ImmutableMap.of("Basic Info", BAM_INFO_GRP);
    public static final Map<String, List<String>> BAM_LOCATION_CATEGORY = ImmutableMap.of("Bam Info", BAM_LOC_GRP);
    public static final Map<String, List<String>> BAM_DETAILS_CATEGORY = ImmutableMap.of("Details", BAM_DETAILS_GRP);
    public static final String MISC_CATEGORY = "Misc";

    // BED-14
    public static final List<String> BED14_INFO_GRP = Arrays.asList(TITLE, ID, DESCRIPTION);
    public static final List<String> BED14_LOC_GRP = Arrays.asList(START, END, LENGTH, STRAND, CDS_START, CDS_END, CHROMOSOME);
    public static final List<String> BED14_CIGAR_GRP = Arrays.asList(SCORES, RESIDUES, SHOW_MASK);
    public static final List<String> BED14_IGNORE_LIST = Arrays.asList(CL, NAME);
    public static final List<String> BED14_PROP_LIST = new ArrayList<>();

    public static final Map<String, List<String>> BED14_INFO_CATEGORY = ImmutableMap.of("Basic Info", BED14_INFO_GRP);
    public static final Map<String, List<String>> BED14_LOCATION_CATEGORY = ImmutableMap.of("BED14 Info", BED14_LOC_GRP);
    public static final Map<String, List<String>> BED14_CIGAR_CATEGORY = ImmutableMap.of("Cigar", BED14_CIGAR_GRP);

    //PSL
    public static final List<String> PSL_INFO_GRP = Arrays.asList(ID, DESCRIPTION);
    public static final List<String> PSL_LOC_GRP = Arrays.asList(START, END, LENGTH, STRAND, CHROMOSOME);
    public static final List<String> PSL_IGNORE_LIST = Arrays.asList(CL, NAME);
    public static final List<String> PSL_PROP_LIST = new ArrayList<>();

    public static final Map<String, List<String>> PSL_INFO_CATEGORY = ImmutableMap.of("Basic Info", PSL_INFO_GRP);
    public static final Map<String, List<String>> PSL_LOCATION_CATEGORY = ImmutableMap.of("PSL Info", PSL_LOC_GRP);

    // DEFAULT
    public static final List<String> DEFAULT_INFO_GRP = Arrays.asList(GENE_NAME, NAME, ID, CHROMOSOME, START, END, LENGTH, AVERAGE_QUALITY);
    public static final List<String> DEFAULT_LOC_GRP = Arrays.asList(RESIDUES, STRAND, SHOW_MASK, SCORES, FORWARD, MAPQ, FLAGS);
    public static final List<String> DEFAULT_CIGAR_GRP = Arrays.asList(PAIRED_READ, CIGAR, VN, NH, XS, NM);
    public static final List<String> DEFAULT_IGNORE_LIST = Arrays.asList(CL);
    public static final List<String> DEFAULT_PROP_LIST = new ArrayList<>();

    public static final Map<String, List<String>> DEFAULT_INFO_CATEGORY = ImmutableMap.of("Basic Info", DEFAULT_INFO_GRP);
    public static final Map<String, List<String>> DEFAULT_LOCATION_CATEGORY = ImmutableMap.of("Bam Info", DEFAULT_LOC_GRP);
    public static final Map<String, List<String>> DEFAULT_CIGAR_CATEGORY = ImmutableMap.of("Cigar", DEFAULT_CIGAR_GRP);

    //GFF
    public static final List<String> GFF_INFO_GRP = Arrays.asList(TITLE, ID, DESCRIPTION);
    public static final List<String> GFF_LOC_GRP = Arrays.asList(START, END, LENGTH, STRAND, CDS_START, CDS_END, CHROMOSOME);
    public static final List<String> GFF_CIGAR_GRP = Arrays.asList(SCORES, RESIDUES, SHOW_MASK);
    public static final List<String> GFF_IGNORE_LIST = Arrays.asList(CL);
    public static final List<String> GFF_PROP_LIST = new ArrayList<>();

    public static final Map<String, List<String>> GFF_INFO_CATEGORY = ImmutableMap.of("Basic Info", GFF_INFO_GRP);
    public static final Map<String, List<String>> GFF_LOCATION_CATEGORY = ImmutableMap.of("GFF Info", GFF_LOC_GRP);
    public static final Map<String, List<String>> GFF_CIGAR_CATEGORY = ImmutableMap.of("Cigar", GFF_CIGAR_GRP);

    static {
        BAM_PROP_LIST.addAll(BAM_INFO_GRP);
        BAM_PROP_LIST.addAll(BAM_LOC_GRP);
        BAM_PROP_LIST.addAll(BAM_DETAILS_GRP);

        BED14_PROP_LIST.addAll(BED14_INFO_GRP);
        BED14_PROP_LIST.addAll(BED14_LOC_GRP);
        BED14_PROP_LIST.addAll(BED14_CIGAR_GRP);

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
