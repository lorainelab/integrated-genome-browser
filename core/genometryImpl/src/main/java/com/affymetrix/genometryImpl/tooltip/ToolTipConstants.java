/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometryImpl.tooltip;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tarun
 */
public class ToolTipConstants {

    public static final String NAME = "name";
    public static final String ID = "id";
    public static final String CHROMOSOME = "chromosome";
    public static final String START = "start";
    public static final String END = "end";
    public static final String LENGTH = "length";
    public static final String AVERAGE_QUALITY = "average quality";
    public static final List<String> BASIC_CAT_KEYS = ImmutableList.of(NAME, ID, CHROMOSOME, START, END, LENGTH, AVERAGE_QUALITY);

    public static final String RESIDUES = "residues";
    public static final String STRAND = "strand";
    public static final String SHOW_MASK = "showMask";
    public static final String SCORES = "scores";
    public static final String FORWARD = "forward";
    public static final String MAPQ = "mapq";
    public static final String FLAGS = "flags";
    public static final List<String> BAM_CAT_KEYS = ImmutableList.of(RESIDUES, STRAND, SHOW_MASK, SCORES, FORWARD, MAPQ, FLAGS);

    public static final String CL = "CL";
    public static final String CIGAR = "cigar";
    public static final String VN = "VN";
    public static final String NH = "NH";
    public static final String XS = "XS";
    public static final String NM = "NM";
    public static final List<String> CIGAR_CAT_KEYS = ImmutableList.of(CL, CIGAR, VN, NH, XS, NM);

    public static final Map<String, List<String>> BASIC_CATEGORY = ImmutableMap.of("Basic Info", BASIC_CAT_KEYS);
    public static final Map<String, List<String>> BAM_CATEGORY = ImmutableMap.of("Bam Info", BAM_CAT_KEYS);
    public static final Map<String, List<String>> CIGAR_CATEGORY = ImmutableMap.of("Cigar", CIGAR_CAT_KEYS);
    public static final String MISC_CATEGORY = "Misc";

    private ToolTipConstants() {

    }

}
