package org.lorainelab.igb.ucsc.rest.api.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BarChartTypeData {
    private String chrom;
    private int chromStart;
    private int chromEnd;
    private String name;
    private int score;
    private String strand;
    private String name2;
    private int expCount;
    private String expScores;

    public float[] getExpScoresArray(){
        if(Objects.isNull(expScores) || expScores.isEmpty())
            return null;
        String[] expScoresArray = expScores.split(",");
        float[] expScoresFloatArray = new float[expCount];
        for(int i=0; i<expCount; i++){
            expScoresFloatArray[i] = Float.parseFloat(expScoresArray[i]);
        }
        return expScoresFloatArray;
    }
}
