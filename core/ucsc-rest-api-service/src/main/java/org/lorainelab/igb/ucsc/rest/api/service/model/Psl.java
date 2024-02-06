package org.lorainelab.igb.ucsc.rest.api.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Psl {
    public int bin;
    public int matches;
    public int misMatches;
    public int repMatches;
    public int nCount;
    public int qNumInsert;
    public int qBaseInsert;
    public int tNumInsert;
    public int tBaseInsert;
    public String strand;
    public String qName;
    public int qSize;
    public int qStart;
    public int qEnd;
    public String tName;
    public int tSize;
    public int tStart;
    public int tEnd;
    public int blockCount;
    public String blockSizes;
    public String qStarts;
    public String tStarts;

    public int[] getBlockSizesArray(){
        return Arrays.stream(blockSizes.split(",")).mapToInt(Integer::parseInt).toArray();
    }
    public int[] getQStartsArray(){
        return Arrays.stream(qStarts.split(",")).mapToInt(Integer::parseInt).toArray();
    }
    public int[] getTStartsArray(){
        return Arrays.stream(tStarts.split(",")).mapToInt(Integer::parseInt).toArray();
    }

}
