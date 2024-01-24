package org.lorainelab.igb.ucsc.rest.api.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenePred {
    public int bin;
    public String name;
    public String chrom;
    public String strand;
    public int txStart;
    public int txEnd;
    public int cdsStart;
    public int cdsEnd;
    public int exonCount;
    public String exonStarts;
    public String exonEnds;
    public int score;
    public String name2;
    public String cdsStartStat;
    public String cdsEndStat;
    public String exonFrames;
}