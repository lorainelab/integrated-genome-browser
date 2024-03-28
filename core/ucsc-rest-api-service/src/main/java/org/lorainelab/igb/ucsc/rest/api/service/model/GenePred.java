package org.lorainelab.igb.ucsc.rest.api.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenePred {
    private int bin;
    private String name;
    private String chrom;
    private String strand;
    private int txStart;
    private int txEnd;
    private int cdsStart;
    private int cdsEnd;
    private int exonCount;
    private String exonStarts;
    private String exonEnds;
    private int score;
    private String name2;
    private String cdsStartStat;
    private String cdsEndStat;
    private String exonFrames;
}