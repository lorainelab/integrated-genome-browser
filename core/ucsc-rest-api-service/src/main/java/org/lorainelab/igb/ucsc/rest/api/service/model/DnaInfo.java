package org.lorainelab.igb.ucsc.rest.api.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DnaInfo {
    public String downloadTime;
    public long downloadTimeStamp;
    public String genome;
    public String chrom;
    public int start;
    public int end;
    public String dna;
}