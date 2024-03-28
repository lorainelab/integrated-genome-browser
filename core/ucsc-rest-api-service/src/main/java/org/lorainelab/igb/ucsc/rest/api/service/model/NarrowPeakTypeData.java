package org.lorainelab.igb.ucsc.rest.api.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NarrowPeakTypeData {
    public int bin;
    public String chrom;
    public int chromStart;
    public int chromEnd;
    public String name;
    public String score;
    public String strand;
    public double signalValue;
    public int pValue;
    public double qValue;
    public int peak;
}

