package org.lorainelab.igb.ucsc.rest.api.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BigGenePred {
    public String chrom;
    public int chromStart;
    public int chromEnd;
    public String name;
    public int score;
    public String strand;
    public int thickStart;
    public int thickEnd;
    public String reserved;
    public int blockCount;
    public String blockSizes;
    public String chromStarts;
    public String cdsStartStat;
    public String cdsEndStat;
    public String exonFrames;
    public String geneName;
    public Map<String, Object> props;

    List<String> properties = new ArrayList<>(Arrays.asList("chrom", "chromStart", "chromEnd", "score", "strand", "thickStart", "thickEnd",
            "reserved", "blockCount", "blockSizes", "chromStarts", "cdsStartStat", "cdsEndStat", "exonFrames", "geneName"));

    public int[] getBlockSizesArray(){
        return Objects.nonNull(blockSizes) && !blockSizes.isEmpty()
                ? Arrays.stream(blockSizes.split(",")).mapToInt(Integer::parseInt).toArray() : null;
    }

    public int[] getChromStartsArray(){
        return Objects.nonNull(chromStarts) && !chromStarts.isEmpty()
                ? Arrays.stream(chromStarts.split(",")).mapToInt(Integer::parseInt).toArray() : null;
    }

}
