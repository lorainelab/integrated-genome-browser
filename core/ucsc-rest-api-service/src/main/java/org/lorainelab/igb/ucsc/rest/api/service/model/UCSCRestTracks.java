package org.lorainelab.igb.ucsc.rest.api.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UCSCRestTracks {
    private String downloadTime;
    private long downloadTimeStamp;
    private String dataTime;
    private long dataTimeStamp;
    private Map<String, Map<String, TrackDetails>> tracks;
}
