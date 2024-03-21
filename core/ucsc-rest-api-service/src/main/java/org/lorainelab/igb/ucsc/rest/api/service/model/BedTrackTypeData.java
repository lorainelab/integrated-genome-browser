package org.lorainelab.igb.ucsc.rest.api.service.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BedTrackTypeData {

    private String chrom;
    private int chromStart;
    private int chromEnd;
    private String name;
    private int score;
    private String strand;
    private int thickStart;
    private int thickEnd;
    private String reserved;
    private int blockCount;
    private String blockSizes;
    private String chromStarts;
    private String cdsStartStat;
    private String cdsEndStat;
    private String exonFrames;
    private String geneName;
    private Map<String, Object> props;

    public static List<String> PROPERTIES = new ArrayList<>(Arrays.asList("chrom", "chromStart", "chromEnd", "name", "score", "strand", "thickStart", "thickEnd",
            "reserved", "blockCount", "blockSizes", "chromStarts", "cdsStartStat", "cdsEndStat", "exonFrames", "geneName"));

    public int[] getBlockSizesArray(){
        return Objects.nonNull(blockSizes) && !blockSizes.isEmpty()
                ? Arrays.stream(blockSizes.split(",")).mapToInt(Integer::parseInt).toArray() : null;
    }

    public int[] getChromStartsArray(){
        return Objects.nonNull(chromStarts) && !chromStarts.isEmpty()
                ? Arrays.stream(chromStarts.split(",")).mapToInt(Integer::parseInt).toArray() : null;
    }

    public void setProps(String jsonString) {
        Gson gson = new Gson();
        Map<String, Object> jsonMap = gson.fromJson(jsonString, new TypeToken<Map<String, Object>>() {
        }.getType());
        this.props = new Hashtable<>();
        jsonMap.keySet().forEach(key -> {
            if(!PROPERTIES.contains(key))
                props.put(key, jsonMap.get(key));
        });
    }
}
