package org.lorainelab.igb.ucsc.rest.api.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChromosomeData {
    private String downloadTime;
    private long downloadTimeStamp;
    private String genome;
    private String dataTime;
    private long dataTimeStamp;
    private int chromCount;
    private Map<String, Integer> chromosomes;
}