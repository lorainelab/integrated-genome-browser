package org.lorainelab.igb.ucsc.rest.api.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lorainelab.igb.ucsc.rest.api.service.model.BedTrackTypeData.PROPERTIES;
import static org.lorainelab.igb.ucsc.rest.api.service.model.TrackDataDetails.BED_FORMATS;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaData {
    public String downloadTime;
    public long downloadTimeStamp;
    public String genome;
    public String track;
    public String dataTime;
    public long dataTimeStamp;
    public List<ColumnType> columnTypes;
    private String shortLabel;
    private String type;
    private String longLabel;
    private long itemCount;
    private String parent;
    private String colorByStrand;
    private String visibility;
    private String priority;
    private String subGroups;

    public Map<String, String> getFeaturePropsMap() {
        Map<String, String> featureProps =  new HashMap<>();
        if(BED_FORMATS.contains(type.split(" ")[0])){
            StringBuilder stringBuilder = new StringBuilder();
            columnTypes.stream().filter(columnType -> !PROPERTIES.contains(columnType.name)).forEach(columnType -> {
                stringBuilder.append(columnType.name);
                stringBuilder.append(":");
                stringBuilder.append(columnType.jsonType);
                stringBuilder.append(",");
            });
            if(!stringBuilder.isEmpty()){
                featureProps.put("genome", genome);
                featureProps.put("track", track);
                featureProps.put("props", stringBuilder.substring(0, stringBuilder.length()-1));
            }
        }
        return featureProps;
    }
}