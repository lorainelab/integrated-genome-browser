/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometryImpl.tooltip;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
    public static final String MAPQ = "mapq";
    public static final String FLAGS = "flags";

    public static final String CL = "CL";
    public static final String CIGAR = "cigar";
    public static final String VN = "VN";
    public static final String NH = "NH";
    public static final String XS = "XS";
    public static final String NM = "NM";
    
    public static final String DESCRIPTION = "description";
    public static final String CDS_START = "cds start";
    public static final String CDS_END = "cds end";
    
    
    // BAM
    public static final List<String> BAM_INFO_GRP = Arrays.asList(GENE_NAME, NAME, ID, CHROMOSOME, START, END, LENGTH, AVERAGE_QUALITY);
    public static final List<String> BAM_LOC_GRP = Arrays.asList(RESIDUES, STRAND, SHOW_MASK, SCORES, FORWARD, MAPQ, FLAGS);
    public static final List<String> BAM_CIGAR_GRP = Arrays.asList(CIGAR, VN, NH, XS, NM);
    public static final List<String> BAM_IGNORE_LIST = Arrays.asList(CL);
    
    public static final Map<String, List<String>> BAM_INFO_CATEGORY = ImmutableMap.of("Basic Info", BAM_INFO_GRP);
    public static final Map<String, List<String>> BAM_LOCATION_CATEGORY = ImmutableMap.of("Bam Info", BAM_LOC_GRP);
    public static final Map<String, List<String>> BAM_CIGAR_CATEGORY = ImmutableMap.of("Cigar", BAM_CIGAR_GRP);
    public static final String MISC_CATEGORY = "Misc";
    
    // BED-14
    public static final List<String> BED14_INFO_GRP = Arrays.asList(TITLE, ID, DESCRIPTION);
    public static final List<String> BED14_LOC_GRP = Arrays.asList(START, END, LENGTH, STRAND, CDS_START, CDS_END, CHROMOSOME);
    public static final List<String> BED14_CIGAR_GRP = Arrays.asList(SCORES, RESIDUES, SHOW_MASK);
    public static final List<String> BED14_IGNORE_LIST = Arrays.asList(CL, NAME);
    
    public static final Map<String, List<String>> BED14_INFO_CATEGORY = ImmutableMap.of("Basic Info", BED14_INFO_GRP);
    public static final Map<String, List<String>> BED14_LOCATION_CATEGORY = ImmutableMap.of("BED14 Info", BED14_LOC_GRP);
    public static final Map<String, List<String>> BED14_CIGAR_CATEGORY = ImmutableMap.of("Cigar", BED14_CIGAR_GRP);

    //PSL
    public static final List<String> PSL_INFO_GRP = Arrays.asList(ID, DESCRIPTION);
    public static final List<String> PSL_LOC_GRP = Arrays.asList(START, END, LENGTH, STRAND, CHROMOSOME);
    public static final List<String> PSL_IGNORE_LIST = Arrays.asList(CL, NAME);
    
    public static final Map<String, List<String>> PSL_INFO_CATEGORY = ImmutableMap.of("Basic Info", PSL_INFO_GRP);
    public static final Map<String, List<String>> PSL_LOCATION_CATEGORY = ImmutableMap.of("PSL Info", PSL_LOC_GRP);
    
    private ToolTipConstants() {

    }

}
